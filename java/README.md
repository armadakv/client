# Armada Java client

This directory contains the migrated JVM SDK for the Armada client monorepo.

The JVM build now uses Armada naming throughout:

- `armada-java-grpc`
- `armada-java-core`
- `armada-java-spring-data`
- `armada-java-test`

The published coordinates are now rooted at `io.armadakv`, with `armada-java-*` artifact names for the JVM modules. The hand-written Java sources live under `io.armadakv.*`, while generated protobuf stubs follow the upstream `armada.v1`/`regatta.v1` contract shape from `api/proto`.

## Local checks

```bash
./java/gradlew check -x test
./java/gradlew test
```
