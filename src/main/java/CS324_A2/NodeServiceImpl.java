package CS324_A2;

import GeorgeFiji.NodeProto.MessageRequest;
import GeorgeFiji.NodeProto.MessageResponse;
import GeorgeFiji.NodeServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase {
    private final int nodeId;
    private ManagedChannel nextNodeChannel;
    private NodeServiceGrpc.NodeServiceBlockingStub nextNodeStub;
    private boolean isLeader = false;
    private boolean leaderAnnounced = false;

    public NodeServiceImpl(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void setNext(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        int nextNodeId = request.getMessage();
        this.nextNodeChannel = ManagedChannelBuilder
                .forAddress("localhost", 50000 + nextNodeId)
                .usePlaintext()
                .build();
        this.nextNodeStub = NodeServiceGrpc.newBlockingStub(nextNodeChannel);
        System.out.println("Node " + nodeId + ": Connected to next node " + nextNodeId);
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendElection(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        int candidateId = request.getMessage();
        System.out.println("Node " + nodeId + ": Received ELECTION(" + candidateId + ")");
        
        if (candidateId == nodeId) {
            isLeader = true;
            System.out.println("\n=== Node " + nodeId + " is LEADER! ===\n");
            if (nextNodeStub != null) {
                nextNodeStub.sendLeader(MessageRequest.newBuilder()
                        .setOrigin(nodeId)
                        .setMessage(nodeId)
                        .build());
            }
        } else if (candidateId > nodeId) {
            System.out.println("Node " + nodeId + ": Forwarding ELECTION(" + candidateId + ")");
            if (nextNodeStub != null) {
                nextNodeStub.sendElection(MessageRequest.newBuilder()
                        .setOrigin(request.getOrigin())
                        .setMessage(candidateId)
                        .build());
            }
        } else {
            System.out.println("Node " + nodeId + ": Dropped ELECTION(" + candidateId + ")");
        }
        
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    @Override
    public void sendLeader(MessageRequest request, StreamObserver<MessageResponse> responseObserver) {
        if (!leaderAnnounced) {
            leaderAnnounced = true;
            System.out.println("Node " + nodeId + ": Leader is Node " + request.getMessage());
            
            if (!isLeader && nextNodeStub != null) {
                nextNodeStub.sendLeader(request);
            }
        }
        responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
        responseObserver.onCompleted();
    }

    public void startElection() {
        System.out.println("\nNode " + nodeId + ": Starting election...\n");
        leaderAnnounced = false;
        isLeader = false;
        
        if (nextNodeStub != null) {
            nextNodeStub.sendElection(MessageRequest.newBuilder()
                    .setOrigin(nodeId)
                    .setMessage(nodeId)
                    .build());
        } else {
            System.err.println("Node " + nodeId + ": Cannot start election - no next node");
        }
    }

    public void shutdown() {
        if (nextNodeChannel != null) nextNodeChannel.shutdown();
    }
}
