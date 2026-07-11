# Installation

1. Run a supported Paper or Folia server with the Java version required by that server.
2. Place the DonutShards release JAR in `plugins/` and start the server.
3. Confirm the console reports the detected platform and successful database migration.
4. Stop the server before changing database type. Configure `database.yml`, then restart.

SQLite needs no external service. For MariaDB/MySQL, create an empty database and a least-privilege user that may create/alter tables and read/write rows. Do not use a root account. Back up the database and configuration before upgrades.
