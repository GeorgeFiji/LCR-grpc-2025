# LCR Leader Election Algorithm - gRPC Implementation

**Author:** George Fong, Jash Bhavsar, Pranav Kumar and Lavinia Siliva

**Package:** GeorgeFiji  
**Technology:** Java 21 + gRPC 1.57.2 (OkHttp) + Protocol Buffers

## Overview

This project implements the **LCR (LeCann-Chang-Roberts) Leader Election Protocol** using Java and gRPC. 

- **Ring Topology:** Nodes arranged in a unidirectional ring (each node knows only its successor)
- **Leader Election:** The node with the highest ID is elected as leader
- **Communication:** gRPC-based message passing with OkHttp transport
- **Automatic Registration:** Nodes automatically register with PeerRegister on startup

## Algorithm Description

### How LCR Works:

1. Any node can initiate an election by sending its ID clockwise around the ring
2. When a node receives an `ELECTION(id)` message:
   - If `id > myId`: Forward the message to the next node
   - If `id < myId`: Drop the message (don't forward it)
   - If `id == myId`: Declare myself as leader and send `LEADER(myId)` announcement
3. The `LEADER` message circulates once around the ring so all nodes learn who the leader is

### Example:
Consider 4 nodes with IDs: **5, 11, 2, 7**
- All nodes send `ELECTION(selfId)` concurrently
- `ELECTION(5)` is dropped when it reaches Node 11 (11 > 5)
- `ELECTION(2)` is dropped when it reaches Node 5 (5 > 2)
- `ELECTION(7)` is dropped when it reaches Node 11 (11 > 7)
- Only `ELECTION(11)` completes the full ring
- Node 11 declares itself leader and sends `LEADER(11)` around the ring

## Architecture

### Components:

1. **PeerRegister** (Port 50099)
   - Central registration service using gRPC
   - Manages node registration
   - Automatically configures ring topology when nodes join
   
2. **Node** (Ports 50001-50008, etc.)
   - Each node runs on port 50000 + nodeId
   - Automatically registers with PeerRegister on startup
   - Implements LCR election algorithm
   - Provides user interface to trigger elections

3. **NodeServiceImpl**
   - gRPC service implementation
   - Handles `ELECTION` and `LEADER` messages
   - Manages ring connections

## Prerequisites

✅ **Java JDK 21** (⚠️ **IMPORTANT:** Java 22 and 24 have compatibility issues with gRPC!)  
✅ **Maven 3.9+**  
✅ **Windows PowerShell**

### Why Java 21?

Java 22 and 24 introduced breaking changes in their network stack that cause `UnsupportedAddressTypeException` with gRPC on Windows. Java 21 is the latest stable version that works correctly with gRPC.

**To install Java 21:** Download from [Oracle Java 21](https://www.oracle.com/java/technologies/downloads/#java21) or [Adoptium OpenJDK 21](https://adoptium.net/temurin/releases/?version=21)

## Quick Start (5 Minutes)

### Step 1: Set Java 21 Environment

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"
$env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH
java -version  # Should show Java 21
```

### Step 2: Build the Project

```powershell
mvn clean package -DskipTests
```

✅ **Expected:** `BUILD SUCCESS` message

### Step 2: Open 9 Terminal Windows

You need **9 terminals**:
- 1 for PeerRegister
- 8 for Nodes

**Tip:** Use VS Code's split terminal feature (Ctrl+Shift+5)

### Step 3: Start PeerRegister (Terminal 1)

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.PeerRegister
```

✅ **Expected Output:**
```
PeerRegister running on port 50099
Ready to accept node registrations
```

### Step 4: Start 8 Nodes (Terminals 2-9)

**⚠️ Important:** Wait **2 seconds** between starting each node to allow proper registration

**Use this command for each node (change the number at the end):**

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 1
```

```powershell
# Terminal 2 - Node 1
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 1

# Terminal 3 - Node 2
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 2

# Terminal 4 - Node 3
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 3

# Terminal 5 - Node 4
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 4

# Terminal 6 - Node 5
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 5

# Terminal 7 - Node 6
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 6

# Terminal 8 - Node 7
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 7

# Terminal 9 - Node 8
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 8
```

✅ **Expected Output (Each Node):**
```
Node X started on port 5000X
Node X registered with PeerRegister successfully
Node X: Connected to next node Y
Node X: Enter 'election' to start election or 'exit' to quit:
```

✅ **Expected Output (PeerRegister):**
```
Registered Node 1. Total nodes: 1
Registered Node 2. Total nodes: 2
Setting up ring topology for 2 nodes...
Connected Node 1 -> Node 2
Connected Node 2 -> Node 1
Ring topology complete!
...
```

### Step 5: Run an Election

In **ANY** node terminal, type:
```
election
```

✅ **Expected Result:** Node with highest ID (Node 8) will be elected leader!

## Project Structure

```
CS324 A2/
├── pom.xml                          # Maven configuration
├── src/
│   └── main/
│       ├── java/
│       │   └── CS324_A2/
│       │       ├── Node.java              # Node main class
│       │       ├── NodeServiceImpl.java   # gRPC service implementation
│       │       └── PeerRegister.java      # Registration service
│       └── proto/
│           └── node.proto                 # Protocol Buffer definitions
├── target/
│   ├── classes/                     # Compiled classes
│   └── generated-sources/           # Auto-generated gRPC code (GeorgeFiji package)
└── README_GRPC.md                   # This file
```

## Building the Project

### Step 1: Clean and Build

```powershell
mvn clean package -DskipTests
```

Expected output:
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~4 seconds
```

### Step 2: Verify Build

Check that `target/classes/` contains:
- `CS324_A2/*.class` files
- `GeorgeFiji/*.class` files (generated gRPC code)

## Running the System

### Method 1: Manual Setup (Recommended for Learning)

You need **9 terminal windows**: 1 for PeerRegister + 8 for Nodes

#### Step 1: Start PeerRegister (Terminal 1)

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.PeerRegister
```

Expected output:
```
PeerRegister running on port 50099
Ready to accept node registrations
```

#### Step 2: Start Nodes (Terminals 2-9)

**Important:** Wait 2 seconds between starting each node to allow proper registration.

```powershell
# Terminal 2 - Node 1
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 1

# Terminal 3 - Node 2
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 2

# Terminal 4 - Node 3
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 3

# Terminal 5 - Node 4
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 4

# Terminal 6 - Node 5
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 5

# Terminal 7 - Node 6
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 6

# Terminal 8 - Node 7
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 7

# Terminal 9 - Node 8
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH; java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 8
```

Expected output for each node:
```
Node X started on port 5000X
Node X registered with PeerRegister successfully
Node X: Connected to next node Y
Node X: Enter 'election' to start election or 'exit' to quit:
```

Expected output in PeerRegister terminal:
```
Registered Node 1. Total nodes: 1
Registered Node 2. Total nodes: 2
Setting up ring topology for 2 nodes...
Connected Node 1 -> Node 2
Connected Node 2 -> Node 1
Ring topology complete!
Registered Node 3. Total nodes: 3
Setting up ring topology for 3 nodes...
...
```

#### Step 3: Start an Election

In **ANY** node terminal, type:
```
election
```

### Method 2: Using Custom Node IDs

You can use any unique integer IDs:

```powershell
# Example with different IDs (remember to set Java 21 environment first!)
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH

java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 5
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 11
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 2
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 7
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 100
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 50
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 25
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 75
```

The node with **ID 100** will be elected leader.

## What Happens During Election?

### Example: Nodes with IDs 1, 2, 3, 4, 5, 6, 7, 8

When you type `election` in **Node 5's terminal**:

**Node 5 (initiator):**
```
Node 5: Starting election...
```

**Node 6 (next in ring):**
```
Node 6: Received ELECTION(5)
Node 6: Forwarding ELECTION(5)
```

**Node 7:**
```
Node 7: Received ELECTION(5)
Node 7: Forwarding ELECTION(5)
```

**Node 8 (highest ID):**
```
Node 8: Received ELECTION(5)
Node 8: Forwarding ELECTION(5)  ← Forwards because 8 > 5
...
Node 8: Received ELECTION(8)    ← Gets its own ID back

=== Node 8 is LEADER! ===       ← Declares victory!
```

**All Other Nodes:**
```
Node 1: Leader is Node 8
Node 2: Leader is Node 8
Node 3: Leader is Node 8
...
Node 7: Leader is Node 8
```

### Why Node 8 Wins?
- Node 8 has the **highest ID**
- IDs smaller than 8 get dropped
- Only `ELECTION(8)` completes the full ring
- Node 8 recognizes its own ID and becomes leader

## Understanding the Ring Topology

Nodes are connected in the **order they register**, not by their IDs.

Example registration order: 1, 2, 3, 4, 5, 6, 7, 8

Ring structure:
```
1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 1
```

If you register in order: 5, 11, 2, 7, 1, 3, 4, 6

Ring structure:
```
5 → 11 → 2 → 7 → 1 → 3 → 4 → 6 → 5
```

## Commands

Once a node is running, you can enter:

- **`election`** - Start a leader election from this node
- **`exit`** - Shut down this node and exit

## Commands

Once nodes are running:
- **`election`** - Start a leader election
- **`exit`** - Shut down the node

## Troubleshooting

| Problem | Solution |
|---------|----------|
| ❌ "UnsupportedAddressTypeException" | ✅ **Use Java 21!** Java 22/24 have network compatibility issues |
| ❌ "Node X failed to register" | ✅ Start PeerRegister first; Check Java 21 is being used |
| ❌ "Node X is already registered" | ✅ Each node needs a unique ID |
| ❌ "Cannot start election - no next node" | ✅ Wait for "Ring topology complete!" in PeerRegister |
| ❌ "invalid target release: 21" | ✅ Verify `JAVA_HOME` points to JDK 21 |
| ❌ Maven build fails | ✅ Run `mvn clean package -DskipTests` with Java 21 |
| ⚠️ Build warnings (generated code) | ✅ Safe to ignore |

## Common Issues

### No Response After Typing "election"
- Check all nodes are running
- Verify ring topology was established
- Look for error messages in PeerRegister terminal

### Node Won't Start
```powershell
# Check if port is already in use
netstat -ano | findstr "5000X"

# If port is busy, kill the process or use different node ID
```

## Advanced: Using Custom Node IDs

You can use **any unique integer IDs**:

```powershell
# Set Java 21 environment first
$env:JAVA_HOME="C:\Program Files\Java\jdk-21"; $env:PATH="C:\Program Files\Java\jdk-21\bin;" + $env:PATH

# Example: Random IDs
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 5
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 11
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 2
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 7
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 100
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 50
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 25
java -cp target/a2-election-algorithm-1.0-SNAPSHOT-jar-with-dependencies.jar CS324_A2.Node 75
```

**Result:** Node with ID **100** will be elected leader!

## Testing Scenarios

| Scenario | Test Case | Expected Winner |
|----------|-----------|-----------------|
| 🔢 Sequential | IDs: 1, 2, 3, 4, 5, 6, 7, 8 | Node 8 |
| 🎲 Random | IDs: 5, 11, 2, 7, 100, 3, 42, 99 | Node 100 |
| 🔄 Multiple Elections | Run election twice | Same leader both times |
| ⚡ Concurrent | Start election from multiple nodes | Highest ID still wins |

## Protocol Buffer Definition

Located in `src/main/proto/node.proto`:

```protobuf
syntax = "proto3";
option java_package = "GeorgeFiji";

service NodeService {
  rpc SendElection(MessageRequest) returns (MessageResponse);
  rpc SendLeader(MessageRequest) returns (MessageResponse);
  rpc SetNext(MessageRequest) returns (MessageResponse);
}

service PeerRegisterService {
  rpc RegisterNode(RegisterRequest) returns (MessageResponse);
}
```

## Key Points

⚠️ **Start PeerRegister FIRST** (before any nodes)  
⏱️ **Wait 2 seconds** between starting each node  
📊 **Minimum 2 nodes** required for ring formation  
🔢 **Node IDs must be unique** integers  
🔌 **Ports:** PeerRegister=50099, Nodes=50000+nodeId

## Technical Details

| Component | Details |
|-----------|---------|
| **Communication** | gRPC (synchronous blocking stubs) |
| **Transport** | OkHttp (client), Netty (server) |
| **Serialization** | Protocol Buffers |
| **Package Name** | GeorgeFiji (custom) |
| **Java Version** | 21 LTS ⚠️ **Required** |
| **gRPC Version** | 1.57.2 |
| **Protobuf Version** | 3.21.12 |

### Why OkHttp Transport?

Java 22 and 24 introduced breaking changes in `java.nio.channels` that cause `UnsupportedAddressTypeException` with Netty transport on Windows. Using OkHttp for client channels avoids this issue while maintaining full gRPC compatibility.

## Project Files

```
CS324 A2/
├── pom.xml                          Maven configuration
├── README.md                        This file
├── src/main/
│   ├── java/CS324_A2/
│   │   ├── Node.java               Main node class
│   │   ├── NodeServiceImpl.java   Election algorithm
│   │   └── PeerRegister.java      Registration service
│   └── proto/
│       └── node.proto              Protocol definitions (GeorgeFiji)
└── target/
    ├── classes/                    Compiled classes
    └── generated-sources/          Auto-generated gRPC code
```

## Success Checklist

- [ ] PeerRegister running on port 50099
- [ ] 8 nodes started successfully
- [ ] "Ring topology complete!" message appears
- [ ] Typed `election` in a node terminal
- [ ] Node with highest ID declared leader
- [ ] All nodes received leader announcement

## Important Notes About Java Versions ⚠️

### Java 21 is Required!

This project **MUST** use Java 21. Java 22 and 24 have breaking changes that cause runtime failures:

**Error with Java 22/24:**
```
java.nio.channels.UnsupportedAddressTypeException
    at java.base/sun.nio.ch.Net.checkAddress(Net.java:135)
```

**Why this happens:**
- Java 22+ changed how network addresses are handled
- gRPC's Netty transport attempts to use Unix domain sockets
- Unix domain sockets are not supported on Windows
- OkHttp transport + Java 21 is the working solution

**To verify your Java version:**
```powershell
java -version
# Should show: java version "21.0.x"
```

**To set Java 21 permanently (System-wide):**
1. Open System Properties → Environment Variables
2. Set `JAVA_HOME` = `C:\Program Files\Java\jdk-21`
3. Update `PATH` to include `%JAVA_HOME%\bin` at the beginning
4. Restart all terminals

## Resources

- **LCR Algorithm:** LeLann-Chang-Roberts Leader Election
- **gRPC Documentation:** https://grpc.io/
- **Protocol Buffers:** https://protobuf.dev/
- **Java 21 Download:** https://www.oracle.com/java/technologies/downloads/#java21

---

**Assignment:** CS324 Assignment 2  
**Author:** George Fong  
**Implementation:** Java 21 + gRPC 1.57.2 (OkHttp) + Protocol Buffers  
**Package:** GeorgeFiji
