# MiniCache

A Redis-inspired in-memory key-value cache server built from scratch in Java. MiniCache implements a TCP server with support for multiple data types, TTL-based key expiry, snapshot persistence, and concurrent client handling — all without any external frameworks.

**Live Server:** `thomas.proxy.rlwy.net:53543`

---

## Features

- **TCP Server** — raw socket server handling multiple concurrent clients via thread-per-connection model
- **4 Data Types** — Strings, Lists, Sets, and Hashes
- **20+ Commands** — full command set similar to Redis
- **TTL / Key Expiry** — lazy expiration on access + active expiration via background thread
- **Snapshot Persistence** — RDB-style JSON snapshots with atomic writes and TTL-aware reload on restart
- **Dockerized** — multi-stage Docker build deployed to Railway cloud

---

## Architecture

```
┌─────────────────────────────────────────────┐
│                  Client                      │
│         (TCP connection on port 6380)        │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│               Server.java                    │
│  • Accepts connections in a loop             │
│  • Spawns a new thread per client            │
│  • Runs background expiration thread         │
│  • Runs background persistence thread        │
│  • Registers JVM shutdown hook               │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│            ClientHandler.java                │
│  • Dedicated thread per client               │
│  • Reads commands line by line               │
│  • Passes to CommandProcessor                │
│  • Writes responses back to client           │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           CommandProcessor.java              │
│  • Parses raw command strings                │
│  • Routes to correct store method            │
│  • Returns formatted response strings        │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│             KeyValueStore.java               │
│  • ConcurrentHashMap for thread safety       │
│  • Separate maps for each data type          │
│  • Lazy + active TTL expiration              │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│           PersistenceManager.java            │
│  • Serializes store to JSON (Gson)           │
│  • Atomic writes via temp file + rename      │
│  • Filters expired keys on reload            │
└─────────────────────────────────────────────┘
```

---

## Commands

### String Commands
| Command | Description | Example |
|---|---|---|
| `SET key value` | Store a string value | `SET name Abhas` |
| `GET key` | Retrieve a value | `GET name` |
| `DEL key` | Delete a key | `DEL name` |
| `EXISTS key` | Check if key exists | `EXISTS name` |
| `PING` | Test connection | `PING` |

### TTL Commands
| Command | Description | Example |
|---|---|---|
| `EXPIRE key seconds` | Set expiry on a key | `EXPIRE name 60` |
| `TTL key` | Get remaining TTL (-1 = no expiry, -2 = not found) | `TTL name` |

### List Commands
| Command | Description | Example |
|---|---|---|
| `LPUSH key value` | Push to front of list | `LPUSH queue task1` |
| `RPUSH key value` | Push to back of list | `RPUSH queue task2` |
| `LRANGE key start stop` | Get range of elements (0 -1 = all) | `LRANGE queue 0 -1` |
| `LLEN key` | Length of list | `LLEN queue` |

### Set Commands
| Command | Description | Example |
|---|---|---|
| `SADD key member` | Add member to set | `SADD langs java` |
| `SMEMBERS key` | Get all members | `SMEMBERS langs` |
| `SISMEMBER key member` | Check membership | `SISMEMBER langs java` |
| `SCARD key` | Size of set | `SCARD langs` |

### Hash Commands
| Command | Description | Example |
|---|---|---|
| `HSET key field value` | Set a hash field | `HSET user name Abhas` |
| `HGET key field` | Get a hash field | `HGET user name` |
| `HGETALL key` | Get all fields and values | `HGETALL user` |
| `HLEN key` | Number of fields | `HLEN user` |

---

## Connect to Live Server

The server is deployed on Railway. Connect using PowerShell:

```powershell
$client = New-Object System.Net.Sockets.TcpClient("thomas.proxy.rlwy.net", 53543)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter($stream)
$reader = New-Object System.IO.StreamReader($stream)
$writer.AutoFlush = $true

Write-Host $reader.ReadLine()       # welcome message

$writer.WriteLine("PING")
Write-Host $reader.ReadLine()       # PONG

$writer.WriteLine("SET name Abhas")
Write-Host $reader.ReadLine()       # OK

$writer.WriteLine("GET name")
Write-Host $reader.ReadLine()       # Abhas

$writer.WriteLine("EXIT")
$client.Close()
```

Or using telnet:
```bash
telnet thomas.proxy.rlwy.net 53543
```

---

## Run Locally

### Prerequisites
- Java 17+
- Maven 3.9+

### Build and Run

```bash
# Clone the repository
git clone https://github.com/ABHAS-RGB/Minicache.git
cd Minicache

# Build
mvn clean package

# Run as TCP server (default port 6380)
java -jar target/minicache.jar server

# Run as CLI (interactive mode)
java -jar target/minicache.jar cli

# Run on a custom port
java -jar target/minicache.jar server 7000
```

### Run with Docker

```bash
# Build image
docker build -t minicache .

# Run container
docker run -p 6380:6380 minicache
```

---

## Design Decisions & Interview Talking Points

### Thread-per-connection concurrency
Each client gets a dedicated thread. Simple and effective for moderate load.
Tradeoff: doesn't scale to 10,000+ connections (each thread ~1MB stack). Production systems use async I/O (Java NIO / Netty) to solve this — a natural future improvement.

### ConcurrentHashMap for thread safety
All data maps use `ConcurrentHashMap` instead of `HashMap`. Allows safe concurrent reads/writes from multiple client threads without manual locking on every operation.

### Dual expiration strategy (same as Redis)
- **Lazy expiration** — check on every access (GET/EXISTS/TTL). Zero CPU cost for keys that are regularly accessed.
- **Active expiration** — background thread scans every 1 second. Catches keys that are set and never accessed again, preventing memory leaks.

### Atomic persistence writes
Never write directly to `dump.json`. Instead: write to `dump.json.tmp` → rename to `dump.json`. Rename is atomic on most filesystems — if the process crashes mid-write, the previous snapshot is still intact.

### TTL-aware reload
On restart, expired keys are filtered out before loading into memory. A key with 300s TTL that was set 200s before shutdown correctly loads with 100s remaining.

---

## Project Structure

```
src/main/java/com/minicache/
├── Main.java                          # Entry point (cli / server modes)
├── command/
│   └── CommandProcessor.java          # Parses and routes commands
├── server/
│   ├── Server.java                    # TCP server + background threads
│   └── ClientHandler.java             # Per-client connection handler
├── store/
│   └── KeyValueStore.java             # Core in-memory data store
└── persistence/
    └── PersistenceManager.java        # Snapshot save/load
```

---

## Tech Stack

- **Language:** Java 17
- **Build:** Maven
- **Serialization:** Gson
- **Deployment:** Docker + Railway
- **Concurrency:** Java threads, ConcurrentHashMap

---

## Roadmap

- [ ] Thread pool instead of unbounded thread-per-connection
- [ ] RESP protocol compatibility (connect with real Redis clients)
- [ ] Replication (leader-follower)
- [ ] Pub/Sub messaging
- [ ] AOF (Append-Only File) persistence mode

---

*Built as a systems programming project to understand how in-memory databases work under the hood.*
