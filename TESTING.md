# Testing

Run `./gradlew clean test shadowJar release`. Unit tests use temporary SQLite databases and verify balance bounds, insufficient funds, tax rounding, transfer idempotency, and zone membership.

Manual release matrix:

- Paper and Folia 1.20.1 on Java 21.
- Paper and Folia 26.2 on Java 25.
- Paper 26.3 (there is no Folia 26.3) on Java 25.
- Verify startup/migrations, commands, permissions, concurrent transfers, reward delivery, shop double-click rejection, reconnect behavior, reload generation replacement, and clean shutdown.
- Inspect logs for region ownership violations and scan source/JAR for forbidden scheduler APIs and credentials.

No real-server smoke result may be claimed until that server has actually been launched and exercised.
