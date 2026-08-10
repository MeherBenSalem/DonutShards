# Graph Report - DonutShards  (2026-08-10)

## Corpus Check
- 53 files · ~6,666 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 399 nodes · 755 edges · 22 communities (16 shown, 6 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 82 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `dbd2555e`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Event
- SchedulerService
- ShopService
- TransactionService
- TransactionRepository
- ZoneService
- TransactionType
- CommandRouter
- AfkZoneEnterEvent
- README.md
- DonutShards
- DonutShards
- Test
- Changelog
- gradlew
- Patch Notes — 1.1.0
- CONFIGURATION.md
- FOLIA_COMPATIBILITY.md
- LICENSE.md
- TESTING.md

## God Nodes (most connected - your core abstractions)
1. `ZoneService` - 34 edges
2. `TransactionService` - 27 edges
3. `MutationContext` - 24 edges
4. `SchedulerService` - 23 edges
5. `ShopService` - 19 edges
6. `CommandRouter` - 18 edges
7. `TransactionRepository` - 18 edges
8. `DefaultDonutShardsApi` - 17 edges
9. `DatabaseManager` - 17 edges
10. `RewardService` - 16 edges

## Surprising Connections (you probably didn't know these)
- `DonutShardsPlugin` --references--> `DatabaseManager`  [EXTRACTED]
  src/main/java/com/nightbeam/donutshards/DonutShardsPlugin.java → src/main/java/com/nightbeam/donutshards/database/DatabaseManager.java
- `DonutShardsPlugin` --references--> `SchedulerService`  [EXTRACTED]
  src/main/java/com/nightbeam/donutshards/DonutShardsPlugin.java → src/main/java/com/nightbeam/donutshards/scheduler/SchedulerService.java
- `DonutShardsPlugin` --references--> `ZoneService`  [EXTRACTED]
  src/main/java/com/nightbeam/donutshards/DonutShardsPlugin.java → src/main/java/com/nightbeam/donutshards/zone/ZoneService.java
- `DefaultDonutShardsApi` --references--> `ZoneService`  [EXTRACTED]
  src/main/java/com/nightbeam/donutshards/api/DefaultDonutShardsApi.java → src/main/java/com/nightbeam/donutshards/zone/ZoneService.java
- `CommandRouter` --references--> `SchedulerService`  [EXTRACTED]
  src/main/java/com/nightbeam/donutshards/command/CommandRouter.java → src/main/java/com/nightbeam/donutshards/scheduler/SchedulerService.java

## Import Cycles
- None detected.

## Communities (22 total, 6 thin omitted)

### Community 0 - "Event"
Cohesion: 0.05
Nodes (13): Cancellable, Event, AfkZoneLeaveEvent, HandlerList, Player, HandlerList, ShardBalanceChangeEvent, HandlerList (+5 more)

### Community 1 - "SchedulerService"
Cohesion: 0.06
Nodes (17): FunctionalInterface, Override, Server, AbstractModernScheduler, Entity, Location, Plugin, FoliaSchedulerService (+9 more)

### Community 2 - "ShopService"
Cohesion: 0.07
Nodes (22): Public API, Component, Inventory, InventoryClickEvent, InventoryDragEvent, InventoryHolder, JavaPlugin, MiniMessage (+14 more)

### Community 3 - "TransactionService"
Cohesion: 0.08
Nodes (7): AntiCheatFlagProvider, DefaultDonutShardsApi, DonutShardsApi, MutationContext, PlayerStatistics, TransactionResult, TransactionService

### Community 4 - "TransactionRepository"
Cohesion: 0.09
Nodes (16): AfterEach, BeforeEach, HikariDataSource, DatabaseConfig, Type, MARIADB, SQLITE, DatabaseManager (+8 more)

### Community 5 - "ZoneService"
Cohesion: 0.10
Nodes (12): Listener, PlayerMoveEvent, AfkZone, EventHandler, PlayerJoinEvent, PlayerQuitEvent, ZoneListener, Location (+4 more)

### Community 6 - "TransactionType"
Cohesion: 0.10
Nodes (13): ResultSet, ShardTransaction, TransactionType, ADMIN_GIVE, ADMIN_SET, ADMIN_TAKE, API, MIGRATION (+5 more)

### Community 7 - "CommandRouter"
Cohesion: 0.25
Nodes (6): Command, CommandExecutor, CommandRouter, CommandSender, Player, TabCompleter

### Community 8 - "AfkZoneEnterEvent"
Cohesion: 0.27
Nodes (3): AfkZoneEnterEvent, HandlerList, Player

### Community 9 - "README.md"
Cohesion: 0.20
Nodes (6): Commands, Installation, Permissions, Compatibility, DonutShards, Install

### Community 10 - "DonutShards"
Cohesion: 0.22
Nodes (8): Commands, Compatibility, Current features, DonutShards, Important release status, Installation, License, Support and bug reports

### Community 11 - "DonutShards"
Cohesion: 0.22
Nodes (8): Commands, DonutShards, Highlights, License, Release status, Reporting issues, Setup, Supported platforms

### Community 13 - "Changelog"
Cohesion: 0.50
Nodes (3): 1.0.0, 1.1.0, Changelog

### Community 14 - "gradlew"
Cohesion: 0.83
Nodes (3): gradlew script, die(), warn()

## Knowledge Gaps
- **39 isolated node(s):** `SQLITE`, `MARIADB`, `REWARD`, `SHOP_PURCHASE`, `ADMIN_GIVE` (+34 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TransactionService` connect `TransactionService` to `SchedulerService`, `ShopService`, `TransactionRepository`, `TransactionType`, `CommandRouter`?**
  _High betweenness centrality (0.200) - this node is a cross-community bridge._
- **Why does `ZoneService` connect `ZoneService` to `SchedulerService`, `ShopService`, `TransactionService`, `CommandRouter`?**
  _High betweenness centrality (0.166) - this node is a cross-community bridge._
- **Why does `SchedulerService` connect `SchedulerService` to `ShopService`, `TransactionService`, `CommandRouter`?**
  _High betweenness centrality (0.150) - this node is a cross-community bridge._
- **Are the 4 inferred relationships involving `MutationContext` (e.g. with `.admin()` and `.shards()`) actually correct?**
  _`MutationContext` has 4 INFERRED edges - model-reasoned connections that need verification._
- **What connects `SQLITE`, `MARIADB`, `REWARD` to the rest of the system?**
  _39 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Event` be split into smaller, more focused modules?**
  _Cohesion score 0.050505050505050504 - nodes in this community are weakly interconnected._
- **Should `SchedulerService` be split into smaller, more focused modules?**
  _Cohesion score 0.05505279034690799 - nodes in this community are weakly interconnected._