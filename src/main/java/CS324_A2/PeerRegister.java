package CS324_A2;

import GeorgeFiji.NodeProto.MessageRequest;
import GeorgeFiji.NodeProto.RegisterRequest;
import GeorgeFiji.NodeProto.MessageResponse;
import GeorgeFiji.PeerRegisterServiceGrpc;
import GeorgeFiji.NodeServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PeerRegister manages node registration and ring topology construction.
 * 
 * Responsibilities:
 * 1. Accept node registrations via gRPC
 * 2. Maintain list of registered nodes
 * 3. Automatically configure the ring topology when nodes join
 * 4. Connect each node to its successor (unidirectional ring)
 * 
 * The ring is formed based on registration order, not node IDs.
 * Example: If nodes register as 5, 11, 2, 7, the ring is: 5→11→2→7→5
 */
public class PeerRegister extends PeerRegisterServiceGrpc.PeerRegisterServiceImplBase {
    // List of node IDs in registration order (forms the ring sequence)
    private final List<Integer> registeredNodes = new ArrayList<>();

    /**
     * registerNode: Handles node registration requests.
     * 
     * Process:
     * 1. Validate that node ID is unique
     * 2. Add node to registration list
     * 3. If >= 2 nodes are registered, reconfigure the ring topology
     * 
     * The ring is rebuilt every time a new node joins to include the new member.
     * 
     * @param request Contains the node's ID and port
     * @param responseObserver Used to send acknowledgment or error
     */
    @Override
    public void registerNode(RegisterRequest request, StreamObserver<MessageResponse> responseObserver) {
        int nodeId = request.getNodeId();
        
        // Synchronize to prevent race conditions during registration
        synchronized (this) {
            // Check if node ID is already registered
            if (registeredNodes.contains(nodeId)) {
                System.out.println("Node " + nodeId + " is already registered");
                responseObserver.onError(new Exception("Node already registered"));
                return;
            }
            
            // Add new node to the registration list (in order of arrival)
            registeredNodes.add(nodeId);
            System.out.println("Registered Node " + nodeId + ". Total nodes: " + registeredNodes.size());
            
            // Send acknowledgment to the registering node
            responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
            responseObserver.onCompleted();
            
            // Configure ring if we have at least 2 nodes
            if (registeredNodes.size() >= 2) {
                setupRingTopology();
            }
        }
    }

    /**
     * setupRingTopology: Configures the unidirectional ring topology.
     * 
     * Creates connections: Node[i] → Node[(i+1) % N]
     * The modulo operator ensures the last node connects back to the first node,
     * forming a closed ring.
     * 
     * Example with 4 nodes (IDs: 5, 11, 2, 7 in registration order):
     *   5 → 11 → 2 → 7 → 5 (back to start)
     * 
     * Each node is told who its successor is via the setNext() RPC call.
     */
    private void setupRingTopology() {
        System.out.println("Setting up ring topology for " + registeredNodes.size() + " nodes...");
        
        // Loop through all registered nodes
        for (int i = 0; i < registeredNodes.size(); i++) {
            int currentNode = registeredNodes.get(i);
            // Calculate next node using modulo for circular topology
            int nextNode = registeredNodes.get((i + 1) % registeredNodes.size());
            
            try {
                // Create temporary channel to the current node
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress("localhost", 50000 + currentNode)
                        .usePlaintext()
                        .build();
                
                // Create stub to call the current node's setNext() method
                NodeServiceGrpc.NodeServiceBlockingStub stub = NodeServiceGrpc.newBlockingStub(channel);
                
                // Build request containing the next node's ID
                MessageRequest request = MessageRequest.newBuilder()
                        .setOrigin(0)           // Origin=0 indicates PeerRegister
                        .setMessage(nextNode)   // Tell current node who its successor is
                        .build();
                
                // Call setNext() on the current node
                stub.setNext(request);
                System.out.println("Connected Node " + currentNode + " -> Node " + nextNode);
                
                // Close the temporary channel
                channel.shutdown();
            } catch (Exception e) {
                System.err.println("Failed to connect Node " + currentNode + " to Node " + nextNode + ": " + e.getMessage());
            }
        }
        System.out.println("Ring topology complete!");
    }

    /**
     * broadcastElectionStart: Called when a node initiates election.
     * Triggers ALL registered nodes to start election concurrently.
     * 
     * @param request Contains the originating node ID
     * @param responseObserver Used to send acknowledgment
     */
    @Override
    public void broadcastElectionStart(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        int originNode = request.getOrigin();
        System.out.println("\n=== PeerRegister: Broadcasting election start to all " + registeredNodes.size() + " nodes ===");
        System.out.println("    (initiated by Node " + originNode + ")\n");
        
        // Trigger election on ALL registered nodes
        for (int nodeId : registeredNodes) {
            try {
                // Create temporary channel to the node
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress("127.0.0.1", 50000 + nodeId)
                        .usePlaintext()
                        .build();
                
                // Create stub to call the node's triggerElection() method
                NodeServiceGrpc.NodeServiceBlockingStub stub = NodeServiceGrpc.newBlockingStub(channel);
                
                // Call triggerElection() on the node
                stub.triggerElection(MessageRequest.newBuilder()
                        .setOrigin(0)  // Origin=0 indicates PeerRegister
                        .setMessage(0)
                        .build());
                
                System.out.println("PeerRegister: Triggered election on Node " + nodeId);
                
                // Close the temporary channel
                channel.shutdown();
            } catch (Exception e) {
                System.err.println("PeerRegister: Failed to trigger election on Node " + nodeId + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n=== PeerRegister: Election broadcast complete ===\n");
        
        // Send acknowledgment
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    /**
     * Main entry point: Starts the PeerRegister gRPC server.
     * 
     * The server listens on port 50099 for node registrations.
     * This must be started BEFORE any nodes are launched.
     * 
     * Usage: java CS324_A2.PeerRegister
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // Create and start gRPC server on port 50099
        Server server = ServerBuilder.forPort(50099)
                .addService(new PeerRegister())  // Register the service
                .build()
                .start();
        
        System.out.println("PeerRegister running on port 50099");
        System.out.println("Ready to accept node registrations");
        
        // Keep server running until terminated
        server.awaitTermination();
    }
}
