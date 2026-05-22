# GlobalLock

GlobalLock is a distributed coordination service starter written in dependency-free Java. It is designed to look and feel like the early foundation of a production coordination system you might discuss in a systems interview: leases, fencing tokens, heartbeats, expiration, metrics, and a clean HTTP surface.

## Why this project is strong

- Implements time-bound leases instead of naive mutexes
- Issues monotonically increasing fencing tokens to prevent stale writers
- Supports lease acquire, renew, release, and inspection APIs
- Tracks expirations and contention metrics
- Uses a background reaper to evict expired leases
- Ships with runnable scripts and test coverage without external dependencies

## Architecture

```text
Clients
  |
  v
HTTP API Server
  |
  v
LeaseService
  |
  +--> Lease Registry (ConcurrentHashMap)
  +--> Fencing Token Sequencer
  +--> Expiration Reaper
  +--> Metrics Registry
```

## API

### `POST /v1/leases/acquire`

```json
{
  "resource": "payments-ledger",
  "ownerId": "worker-a",
  "ttlMillis": 15000
}
```

### `POST /v1/leases/renew`

```json
{
  "resource": "payments-ledger",
  "ownerId": "worker-a",
  "ttlMillis": 15000,
  "token": 1
}
```

### `POST /v1/leases/release`

```json
{
  "resource": "payments-ledger",
  "ownerId": "worker-a",
  "token": 1
}
```

### `GET /v1/leases/{resource}`

Returns the current holder, token, and expiration metadata for a resource.

### `GET /v1/leases`

Returns all active leases.

### `GET /metrics`

Exposes counters and gauges in JSON form.

### `GET /health`

Basic liveness and server metadata.

## Run

```bash
cd /Users/jeshwinwilliam/Documents/Playground/globallock
./scripts/build.sh
./scripts/run.sh
```

Server defaults:

- Port: `8081`
- Reaper interval: `1000ms`
- Default TTL: `15000ms`

## Test

```bash
cd /Users/jeshwinwilliam/Documents/Playground/globallock
./scripts/test.sh
```

## Example

```bash
curl -s -X POST http://localhost:8081/v1/leases/acquire \
  -H "Content-Type: application/json" \
  -d '{"resource":"payments-ledger","ownerId":"worker-a","ttlMillis":5000}'
```

## Roadmap

- Write-ahead log for recovery
- Raft-style replicated state machine
- Membership changes and quorum reads
- Watch APIs for leader changes
- gRPC and client SDKs
- OpenTelemetry integration

