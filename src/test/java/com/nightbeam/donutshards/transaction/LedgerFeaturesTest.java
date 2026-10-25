package com.nightbeam.donutshards.transaction;

import com.nightbeam.donutshards.database.DatabaseConfig;
import com.nightbeam.donutshards.database.DatabaseManager;
import com.nightbeam.donutshards.model.MutationContext;
import com.nightbeam.donutshards.model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerFeaturesTest {
    @TempDir
    Path dir;
    private DatabaseManager db;
    private TransactionRepository repo;

    @BeforeEach
    void setup() throws Exception {
        db = new DatabaseManager(new DatabaseConfig(DatabaseConfig.Type.SQLITE, "test.db", "", 0, "", "", "", 1, 5000), dir);
        db.migrate();
        repo = new TransactionRepository(db, "test", 1_000_000);
    }

    @AfterEach
    void close() {
        db.close();
    }

    private MutationContext ctx(String key) {
        return new MutationContext(TransactionType.SHOP_PURCHASE, "shop:deal", null, key, Map.of());
    }

    @Test
    void limitedPurchasesStopAtTheLimitAndChargeOnlyWhenAllowed() throws Exception {
        var id = UUID.randomUUID();
        repo.mutate(id, null, 100, new MutationContext(TransactionType.API, "seed", null, "seed", Map.of()));
        var resets = System.currentTimeMillis() + 60_000;
        assertThat(repo.limitedRemove(id, 30, ctx("p1"), "deal:1:x:" + id, 2, resets).success()).isTrue();
        assertThat(repo.limitedRemove(id, 30, ctx("p2"), "deal:1:x:" + id, 2, resets).success()).isTrue();
        var third = repo.limitedRemove(id, 30, ctx("p3"), "deal:1:x:" + id, 2, resets);
        assertThat(third.success()).isFalse();
        assertThat(third.reason()).isEqualTo("limit_reached");
        assertThat(repo.balance(id)).isEqualTo(40);
        assertThat(repo.limitCounts(List.of("deal:1:x:" + id, "deal:1:y:" + id), System.currentTimeMillis()))
                .containsEntry("deal:1:x:" + id, 2L).containsEntry("deal:1:y:" + id, 0L);
    }

    @Test
    void aFailedPaymentDoesNotUseTheLimit() throws Exception {
        var id = UUID.randomUUID();
        var resets = System.currentTimeMillis() + 60_000;
        var broke = repo.limitedRemove(id, 30, ctx("p1"), "deal:1:x:" + id, 1, resets);
        assertThat(broke.reason()).isEqualTo("insufficient_balance");
        assertThat(repo.limitCounts(List.of("deal:1:x:" + id), System.currentTimeMillis())).containsEntry("deal:1:x:" + id, 0L);
    }

    @Test
    void expiredLimitsStartOver() throws Exception {
        var id = UUID.randomUUID();
        repo.mutate(id, null, 100, new MutationContext(TransactionType.API, "seed", null, "seed", Map.of()));
        var past = System.currentTimeMillis() - 1;
        assertThat(repo.limitedRemove(id, 10, ctx("p1"), "scope", 1, past).success()).isTrue();
        assertThat(repo.limitedRemove(id, 10, ctx("p2"), "scope", 1, System.currentTimeMillis() + 60_000).success()).isTrue();
    }

    @Test
    void historyIsNewestFirstAndPaged() throws Exception {
        var id = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) {
            repo.mutate(id, null, i, new MutationContext(TransactionType.REWARD, "reward:" + i, null, "k" + i, Map.of()));
            Thread.sleep(3);
        }
        var page = repo.history(id, 2, 0);
        assertThat(page).extracting(t -> t.source()).containsExactly("reward:5", "reward:4");
        assertThat(repo.history(id, 2, 4)).extracting(t -> t.source()).containsExactly("reward:1");
        assertThat(repo.history(UUID.randomUUID(), 5, 0)).isEmpty();
    }

    @Test
    void touchStoresTheNameForLeaderboards() throws Exception {
        var id = UUID.randomUUID();
        repo.touch(id, "Builder");
        repo.mutate(id, null, 10, new MutationContext(TransactionType.API, "seed", null, "seed", Map.of()));
        assertThat(repo.topBalances(1).get(0).name()).isEqualTo("Builder");
    }
}
