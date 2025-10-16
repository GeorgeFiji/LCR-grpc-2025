package CS324_A2;

import GeorgeFiji.NodeProto.RegisterRequest;
import GeorgeFiji.NodeProto.MessageRequest;
import GeorgeFiji.PeerRegisterServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.Scanner;

/**
 * Node represents a process in the LCR Leader Election ring.
 * Each node has a unique ID and can participate in leader election.
 * Nodes automatically register with PeerRegister on startup.
 */
public class Node {
    private final int nodeId;                    // Unique identifier for this node
    private final Server server;                  // gRPC server to receive messages
    private final NodeServiceImpl serviceImpl;    // Service implementation for LCR protocol
    private ManagedChannel registerChannel;       // Channel to communicate with PeerRegister

    /**
     * Constructor: Creates a new node with the specified ID.
     * @param nodeId Unique integer identifier for this node
     */
    public Node(int nodeId) throws IOException {
        this.nodeId = nodeId;
        this.serviceImpl = new NodeServiceImpl(nodeId);
        // Each node runs on port 50000 + nodeId (e.g., Node 1 on port 50001)
        this.server = ServerBuilder.forPort(50000 + nodeId).addService(serviceImpl).build();
    }

    /**
     * Starts the node's gRPC server and registers with PeerRegister.
     */
    public void start() throws IOException {
        server.start();
        System.out.println("Node " + nodeId + " started on port " + (50000 + nodeId));
        registerWithPeerRegister();
    }

    /**
     * Registers this node with the PeerRegister service.
     * PeerRegister will add this node to the ring topology.
     * Uses OkHttp transport (via ManagedChannelBuilder) for Java 21 compatibility.
     */
    private void registerWithPeerRegister() {
        try {
            // Brief delay to ensure server is ready
            Thread.sleep(500);
            
            // Create channel to PeerRegister (running on port 50099)
            // Uses 127.0.0.1 instead of localhost for explicit IPv4
            registerChannel = ManagedChannelBuilder
                    .forAddress("127.0.0.1", 50099)
                    .usePlaintext()  // No TLS encryption for local testing
                    .build();
            
            // Create blocking stub for synchronous RPC calls
            PeerRegisterServiceGrpc.PeerRegisterServiceBlockingStub stub = 
                    PeerRegisterServiceGrpc.newBlockingStub(registerChannel);
            
            // Build registration request with this node's ID and port
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setNodeId(nodeId)
                    .setPort(50000 + nodeId)
                    .build();
            
            // Send registration request to PeerRegister
            stub.registerNode(request);
            System.out.println("Node " + nodeId + " registered with PeerRegister successfully");
        } catch (Exception e) {
            System.err.println("Node " + nodeId + " failed to register: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handles user commands for initiating elections or shutting down the node.
     * Commands:
     *   - "election": Triggers election in ALL registered nodes via PeerRegister
     *   - "exit": Cleanly shuts down the node and exits
     */
    public void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Node " + nodeId + ": Enter 'election' to start election or 'exit' to quit:");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("election")) {
                // Broadcast election start to ALL registered nodes via PeerRegister
                System.out.println("Node " + nodeId + ": Requesting PeerRegister to start election in all nodes...");
                try {
                    PeerRegisterServiceGrpc.PeerRegisterServiceBlockingStub registerStub = 
                        PeerRegisterServiceGrpc.newBlockingStub(registerChannel);
                    registerStub.broadcastElectionStart(MessageRequest.newBuilder()
                            .setOrigin(nodeId)
                            .setMessage(0)
                            .build());
                    System.out.println("Node " + nodeId + ": Election broadcast sent to PeerRegister");
                } catch (Exception e) {
                    System.err.println("Node " + nodeId + ": Failed to broadcast election: " + e.getMessage());
                }
            } else if (input.equalsIgnoreCase("exit")) {
                // Clean shutdown: close server, service, and channels
                server.shutdown();
                serviceImpl.shutdown();
                if (registerChannel != null) registerChannel.shutdown();
                scanner.close();
                System.exit(0);
            }
        }
    }

    /**
     * Main entry point: Creates and starts a node with the given ID.
     * Usage: java CS324_A2.Node <nodeId>
     * Example: java CS324_A2.Node 5
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java CS324_A2.Node <nodeId>");
            System.exit(1);
        }
        try {
            // Parse node ID from command line argument
            Node node = new Node(Integer.parseInt(args[0]));
            node.start();           // Start gRPC server and register
            node.handleUserInput(); // Wait for user commands
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
