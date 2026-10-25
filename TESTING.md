# Testing

- `./gradlew build` runs the unit tests (Minecraft-free rules in `core/`, the SQLite ledger, YAML loading and merging).
- `./gradlew compileMatrix` compiles the sources against the Paper 1.21.1, 26.1.2, 26.2 and 26.3 APIs (the release
  jar itself is compiled against 1.20.1).
- `tools/qa/run_matrix.sh` runs the in-game checks on real Paper and Folia servers with a real game client; see
  [CONTRIBUTING.md](CONTRIBUTING.md#testing-in-game). The latest results are in [docs/QA-2.0.0.md](docs/QA-2.0.0.md).

No server result may be claimed until that server has actually been launched and exercised.
