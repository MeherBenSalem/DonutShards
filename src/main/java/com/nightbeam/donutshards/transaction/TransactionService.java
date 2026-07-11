package com.nightbeam.donutshards.transaction;
import com.nightbeam.donutshards.model.*;
import com.nightbeam.donutshards.scheduler.SchedulerService;
import java.util.*;
import java.util.concurrent.*;

public final class TransactionService {
    private final TransactionRepository repository; private final SchedulerService scheduler; private final ConcurrentMap<UUID,Long> cache=new ConcurrentHashMap<>();
    public TransactionService(TransactionRepository repository,SchedulerService scheduler){this.repository=repository;this.scheduler=scheduler;}
    private <T> CompletionStage<T> submit(Callable<T> work){var future=new CompletableFuture<T>();scheduler.async(()->{try{future.complete(work.call());}catch(Throwable e){future.completeExceptionally(e);}});return future;}
    public CompletionStage<Long> balance(UUID id){return submit(()->{var value=repository.balance(id);cache.put(id,value);return value;});}
    public OptionalLong cached(UUID id){var value=cache.get(id);return value==null?OptionalLong.empty():OptionalLong.of(value);}
    public CompletionStage<TransactionResult> add(UUID id,long amount,MutationContext ctx){if(amount<=0)return CompletableFuture.completedFuture(TransactionResult.failure("invalid_amount"));return mutate(id,null,amount,ctx);}
    public CompletionStage<TransactionResult> remove(UUID id,long amount,MutationContext ctx){if(amount<=0)return CompletableFuture.completedFuture(TransactionResult.failure("invalid_amount"));return mutate(id,null,-amount,ctx);}
    public CompletionStage<TransactionResult> set(UUID id,long value,MutationContext ctx){if(value<0)return CompletableFuture.completedFuture(TransactionResult.failure("invalid_amount"));return mutate(id,value,0,ctx);}
    private CompletionStage<TransactionResult> mutate(UUID id,Long absolute,long delta,MutationContext ctx){return submit(()->{var result=repository.mutate(id,absolute,delta,ctx);if(result.success())cache.put(id,result.transaction().newBalance());return result;});}
    public CompletionStage<TransferResult> transfer(UUID from,UUID to,long amount,int tax,MutationContext ctx){return submit(()->{var result=repository.transfer(from,to,amount,tax,ctx);if(result.success()){cache.remove(from);cache.remove(to);}return result;});}
    public CompletionStage<Optional<ShardTransaction>> transaction(UUID id){return submit(()->repository.find(id));}
    public CompletionStage<PlayerStatistics> statistics(UUID id){return submit(()->repository.statistics(id));}
    public void forget(UUID id){cache.remove(id);}
}
