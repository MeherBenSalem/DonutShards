# Configuration

DonutShards creates `config.yml`, `messages.yml`, `database.yml`, `rewards.yml`, `zones.yml`, `shop.yml`, `gui.yml`, and `anti-abuse.yml`. Packaged defaults contain comments and no credentials. Monetary values are whole shards stored as signed 64-bit integers and are validated against `maximum-balance`.

MiniMessage is accepted for presentation fields. Materials, sounds, worlds, modes, durations, slots, ranges, and permissions must be valid for the running server. Keep `schema-version` intact. Invalid configuration prevents the affected startup from becoming ready instead of silently selecting dangerous values.
