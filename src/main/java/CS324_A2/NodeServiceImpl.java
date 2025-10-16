package CS324_A2;

import GeorgeFiji.NodeProto.MessageRequest;
import GeorgeFiji.NodeProto.MessageResponse;
import GeorgeFiji.NodeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

/**
 * NodeServiceImpl implements the LCR Leader Election Protocol.
 * 
 * LCR Protocol Rules:
 * 1. Each node sends ELECTION(ownId) clockwise around the ring
 * 2. Upon receiving ELECTION(id):
 *    - If id == myId: Declare victory, send LEADER(myId)
 *    - If id > myId: Forward the message
 *    - If id < myId: Drop the message
 * 3. LEADER message circulates once so all nodes learn the result
 */
public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase {
    private final int nodeId;                          // This node's unique ID
    private ManagedChannel nextNodeChannel;             // Channel to successor in ring
    private NodeServiceGrpc.NodeServiceBlockingStub nextNodeStub;  // Stub for RPC calls
    private boolean isLeader = false;                   // True if this node won election
    private boolean leaderAnnounced = false;            // Prevents duplicate announcements

    /**
     * Constructor: Creates service implementation for the given node ID.
     */
    public NodeServiceImpl(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * setNext: Called by PeerRegister to configure this node's successor in the ring.
     * Establishes the unidirectional link for the ring topology.
     * 
     * @param request Contains the next node's ID
     * @param responseObserver Used to send acknowledgment back to PeerRegister
     */
    @Override
    public void setNext(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        int nextNodeId = request.getMessage();
        
        // Create gRPC channel to the next node in the ring
        // Uses 127.0.0.1 for explicit IPv4, port = 50000 + nextNodeId
        this.nextNodeChannel = ManagedChannelBuilder
                .forAddress("127.0.0.1", 50000 + nextNodeId)
                .usePlaintext()  // No TLS for local testing
                .build();
        
        // Create blocking stub for synchronous RPC calls to next node
        this.nextNodeStub = NodeServiceGrpc.newBlockingStub(nextNodeChannel);
        System.out.println("Node " + nodeId + ": Connected to next node " + nextNodeId);
        
        // Send acknowledgment back to PeerRegister
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    /**
     * sendElection: Handles incoming ELECTION messages (core of LCR algorithm).
     * 
     * LCR Logic:
     * 1. If candidateId == myId: I've won! Declare leadership and announce
     * 2. If candidateId > myId: Forward the message (larger ID might win)
     * 3. If candidateId < myId: Drop the message (this ID can't win)
     * 
     * @param request Contains the candidate ID circulating around the ring
     * @param responseObserver Used to send acknowledgment
     */
    @Override
    public void sendElection(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        int candidateId = request.getMessage();
        System.out.println("Node " + nodeId + ": Received ELECTION(" + candidateId + ")");
        
        // CASE 1: This is my own ID coming back - I'm the leader!
        if (candidateId == nodeId) {
            isLeader = true;
            System.out.println("\n=== Node " + nodeId + " is LEADER! ===\n");
            
            // Send LEADER announcement around the ring once
            if (nextNodeStub != null) {
                nextNodeStub.sendLeader(MessageRequest.newBuilder()
                        .setOrigin(nodeId)
                        .setMessage(nodeId)
                        .build());
            }
        } 
        // CASE 2: Candidate ID is larger than mine - forward it
        else if (candidateId > nodeId) {
            System.out.println("Node " + nodeId + ": Forwarding ELECTION(" + candidateId + ")");
            if (nextNodeStub != null) {
                nextNodeStub.sendElection(MessageRequest.newBuilder()
                        .setOrigin(request.getOrigin())
                        .setMessage(candidateId)
                        .build());
            }
        } 
        // CASE 3: Candidate ID is smaller than mine - drop it
        else {
            System.out.println("Node " + nodeId + ": Dropped ELECTION(" + candidateId + ")");
            // Do nothing - message is not forwarded
        }
        
        // Send acknowledgment back to sender
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    /**
     * sendLeader: Handles the LEADER announcement message.
     * The LEADER message circulates once around the ring so all nodes learn the result.
     * The leaderAnnounced flag prevents infinite circulation.
     * 
     * @param request Contains the winner's ID
     * @param responseObserver Used to send acknowledgment
     */
    @Override
    public void sendLeader(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        // Only process this announcement once
        if (!leaderAnnounced) {
            leaderAnnounced = true;
            System.out.println("Node " + nodeId + ": Leader is Node " + request.getMessage());
            
            // Forward the announcement to next node (unless I'm the leader)
            // The leader node doesn't forward to prevent infinite loop
            if (!isLeader && nextNodeStub != null) {
                nextNodeStub.sendLeader(request);
            }
        }
        
        // Send acknowledgment
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    /**
     * startElection: Initiates a new leader election from this node.
     * Sends ELECTION(myId) to the next node in the ring.
     * 
     * Called in two scenarios:
     * 1. Automatically when ring topology is established (all nodes participate concurrently)
     * 2. Manually when user types "election" command (can trigger a new election)
     */
    public void startElection() {
        System.out.println("\nNode " + nodeId + ": Starting election...\n");
        
        // Reset flags for new election
        leaderAnnounced = false;
        isLeader = false;
        
        if (nextNodeStub != null) {
            // Send my ID as a candidate around the ring
            nextNodeStub.sendElection(MessageRequest.newBuilder()
                    .setOrigin(nodeId)
                    .setMessage(nodeId)  // My ID is the candidate
                    .build());
        } else {
            // Ring not yet configured - cannot start election
            System.err.println("Node " + nodeId + ": Cannot start election - no next node");
        }
    }

    /**
     * TriggerElection: Called by PeerRegister to make this node start election.
     * This is used when one node initiates election and PeerRegister broadcasts to all nodes.
     * 
     * @param request Contains trigger message (not used)
     * @param responseObserver Used to send acknowledgment
     */
    @Override
    public void triggerElection(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        System.out.println("Node " + nodeId + ": Received trigger to start election from PeerRegister");
        
        // Start election in a separate thread to avoid blocking the RPC
        new Thread(() -> {
            try {
                Thread.sleep(100); // Small stagger to avoid all nodes sending at exact same time
                startElection();
            } catch (Exception e) {
                System.err.println("Node " + nodeId + ": Failed to start election: " + e.getMessage());
            }
        }).start();
        
        // Send acknowledgment
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    /**
     * shutdown: Cleanly closes the gRPC channel to the next node.
     * Called when the node is shutting down.
     */
    public void shutdown() {
        if (nextNodeChannel != null) nextNodeChannel.shutdown();
    }
}
