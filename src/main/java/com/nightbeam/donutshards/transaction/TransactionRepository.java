package com.nightbeam.donutshards.transaction;

import com.nightbeam.donutshards.database.DatabaseManager;
import com.nightbeam.donutshards.model.*;
import com.nightbeam.donutshards.util.JsonMetadata;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class TransactionRepository {
    private final DatabaseManager database;
    private final String serverId;
    private final long maximumBalance;
    public TransactionRepository(DatabaseManager database, String serverId, long maximumBalance) { this.database=database; this.serverId=serverId; this.maximumBalance=maximumBalance; }
    public long balance(UUID player) throws SQLException { try (var c=database.connection()) { ensure(c, player); return balance(c, player, false); } }
    public Optional<ShardTransaction> find(UUID id) throws SQLException {
        try (var c=database.connection(); var ps=c.prepareStatement("SELECT * FROM transactions WHERE id=?")) { ps.setString(1,id.toString()); try(var rs=ps.executeQuery()){ return rs.next()?Optional.of(map(rs)):Optional.empty(); } }
    }
    public PlayerStatistics statistics(UUID player) throws SQLException {
        try(var c=database.connection()){ensure(c,player);try(var ps=c.prepareStatement("SELECT * FROM players WHERE uuid=?")){ps.setString(1,player.toString());try(var rs=ps.executeQuery()){rs.next();return new PlayerStatistics(player,rs.getLong("balance"),rs.getLong("total_earned"),rs.getLong("total_spent"),rs.getLong("afk_seconds"),rs.getInt("reward_streak"));}}}
    }
    public List<LeaderboardEntry> topBalances(int limit) throws SQLException {
        var capped = Math.max(1, Math.min(limit, 100));
        var entries = new ArrayList<LeaderboardEntry>();
        try (var c = database.connection(); var ps = c.prepareStatement("SELECT uuid, balance FROM players ORDER BY balance DESC LIMIT ?")) {
            ps.setInt(1, capped);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(UUID.fromString(rs.getString("uuid")), rs.getLong("balance")));
                }
            }
        }
        return entries;
    }
    public TransactionResult mutate(UUID player, Long absolute, long delta, MutationContext context) throws SQLException {
        try { return database.transaction(c -> {
            try {
                ensure(c,player); var existing=findByKey(c,context.idempotencyKey()); if(existing.isPresent()) return TransactionResult.success(existing.get());
                var before=balance(c,player,true); var after=absolute==null?Math.addExact(before,delta):absolute;
                if(after<0||after>maximumBalance)return TransactionResult.failure(after<0?"insufficient_balance":"balance_limit");
                var actual=Math.subtractExact(after,before); update(c,player,after,actual); var tx=create(c,UUID.randomUUID(),UUID.randomUUID(),player,null,context.type(),actual,before,after,context); return TransactionResult.success(tx);
            } catch(SQLException|ArithmeticException e){throw new StorageFailure(e);}
        }); } catch(StorageFailure e){throw (SQLException)e.getCause();}
    }
    public TransferResult transfer(UUID from, UUID to, long amount, int taxBps, MutationContext context) throws SQLException {
        if(from.equals(to)||amount<=0)return TransferResult.failure("invalid_transfer");
        try{return database.transaction(c->{try{
            var old=findTransferByKey(c,context.idempotencyKey());if(old!=null)return old;
            ensure(c,from);ensure(c,to);var ordered=new UUID[]{from,to};Arrays.sort(ordered,Comparator.comparing(UUID::toString));var balances=new HashMap<UUID,Long>();for(var id:ordered)balances.put(id,balance(c,id,true));
            var tax=Math.floorDiv(Math.addExact(Math.multiplyExact(amount,(long)taxBps),9999L),10000L);var debit=Math.addExact(amount,tax);var fromAfter=Math.subtractExact(balances.get(from),debit);var toAfter=Math.addExact(balances.get(to),amount);
            if(fromAfter<0)return TransferResult.failure("insufficient_balance");if(toAfter>maximumBalance)return TransferResult.failure("balance_limit");
            update(c,from,fromAfter,-debit);update(c,to,toAfter,amount);var correlation=UUID.randomUUID();
            create(c,UUID.randomUUID(),correlation,from,to,TransactionType.PLAYER_TRANSFER,-debit,balances.get(from),fromAfter,context);
            var received=new MutationContext(TransactionType.PLAYER_TRANSFER,context.source(),context.actor(),context.idempotencyKey()+":recipient",Map.of("tax",Long.toString(tax)));
            create(c,UUID.randomUUID(),correlation,to,from,TransactionType.PLAYER_TRANSFER,amount,balances.get(to),toAfter,received);
            return new TransferResult(true,"ok",correlation,amount,tax);
        }catch(SQLException|ArithmeticException e){throw new StorageFailure(e);}});}catch(StorageFailure e){throw (SQLException)e.getCause();}
    }
    private TransferResult findTransferByKey(Connection c,String key)throws SQLException{var found=findByKey(c,key);return found.map(t->new TransferResult(true,"ok",t.correlationId(),Math.abs(t.amount()),0)).orElse(null);}
    private void ensure(Connection c,UUID id)throws SQLException{try(var ps=c.prepareStatement("INSERT INTO players(uuid,balance,total_earned,total_spent) SELECT ?,0,0,0 WHERE NOT EXISTS (SELECT 1 FROM players WHERE uuid=?)")){ps.setString(1,id.toString());ps.setString(2,id.toString());ps.executeUpdate();}}
    private long balance(Connection c,UUID id,boolean lock)throws SQLException{var sql="SELECT balance FROM players WHERE uuid=?"+(lock&&database.type()!=com.nightbeam.donutshards.database.DatabaseConfig.Type.SQLITE?" FOR UPDATE":"");try(var ps=c.prepareStatement(sql)){ps.setString(1,id.toString());try(var rs=ps.executeQuery()){if(!rs.next())throw new SQLException("missing player");return rs.getLong(1);}}}
    private void update(Connection c,UUID id,long balance,long delta)throws SQLException{try(var ps=c.prepareStatement("UPDATE players SET balance=?, total_earned=total_earned+?, total_spent=total_spent+?, version=version+1 WHERE uuid=?")){ps.setLong(1,balance);ps.setLong(2,Math.max(0,delta));ps.setLong(3,delta==Long.MIN_VALUE?Long.MAX_VALUE:Math.max(0,-delta));ps.setString(4,id.toString());ps.executeUpdate();}}
    private Optional<ShardTransaction> findByKey(Connection c,String key)throws SQLException{try(var ps=c.prepareStatement("SELECT * FROM transactions WHERE idempotency_key=?")){ps.setString(1,key);try(var rs=ps.executeQuery()){return rs.next()?Optional.of(map(rs)):Optional.empty();}}}
    private ShardTransaction create(Connection c,UUID id,UUID correlation,UUID player,UUID related,TransactionType type,long amount,long before,long after,MutationContext ctx)throws SQLException{var now=Instant.now();try(var ps=c.prepareStatement("INSERT INTO transactions(id,correlation_id,player_uuid,related_uuid,type,amount,previous_balance,new_balance,source,created_at,server_id,administrator_uuid,metadata,idempotency_key,rolled_back) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,0)")){var i=1;ps.setString(i++,id.toString());ps.setString(i++,correlation.toString());ps.setString(i++,player.toString());ps.setString(i++,related==null?null:related.toString());ps.setString(i++,type.name());ps.setLong(i++,amount);ps.setLong(i++,before);ps.setLong(i++,after);ps.setString(i++,ctx.source());ps.setLong(i++,now.toEpochMilli());ps.setString(i++,serverId);ps.setString(i++,ctx.actor()==null?null:ctx.actor().toString());ps.setString(i++,JsonMetadata.encode(ctx.metadata()));ps.setString(i,ctx.idempotencyKey());ps.executeUpdate();}return new ShardTransaction(id,correlation,player,related,type,amount,before,after,ctx.source(),now,serverId,ctx.actor(),false);}
    private ShardTransaction map(ResultSet r)throws SQLException{return new ShardTransaction(UUID.fromString(r.getString("id")),UUID.fromString(r.getString("correlation_id")),UUID.fromString(r.getString("player_uuid")),r.getString("related_uuid")==null?null:UUID.fromString(r.getString("related_uuid")),TransactionType.valueOf(r.getString("type")),r.getLong("amount"),r.getLong("previous_balance"),r.getLong("new_balance"),r.getString("source"),Instant.ofEpochMilli(r.getLong("created_at")),r.getString("server_id"),r.getString("administrator_uuid")==null?null:UUID.fromString(r.getString("administrator_uuid")),r.getBoolean("rolled_back"));}
    private static final class StorageFailure extends RuntimeException{StorageFailure(Throwable cause){super(cause);}}
}
