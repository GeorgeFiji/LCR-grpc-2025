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

public class PeerRegister extends PeerRegisterServiceGrpc.PeerRegisterServiceImplBase {
    private final List<Integer> registeredNodes = new ArrayList<>();

    @Override
    public void registerNode(RegisterRequest request, StreamObserver<MessageResponse> responseObserver) {
        int nodeId = request.getNodeId();
        
        synchronized (this) {
            if (registeredNodes.contains(nodeId)) {
                System.out.println("Node " + nodeId + " is already registered");
                responseObserver.onError(new Exception("Node already registered"));
                return;
            }
            
            registeredNodes.add(nodeId);
            System.out.println("Registered Node " + nodeId + ". Total nodes: " + registeredNodes.size());
            
            responseObserver.onNext(MessageResponse.newBuilder().setAck(1).build());
            responseObserver.onCompleted();
            
            if (registeredNodes.size() >= 2) {
                setupRingTopology();
            }
        }
    }

    private void setupRingTopology() {
        System.out.println("Setting up ring topology for " + registeredNodes.size() + " nodes...");
        for (int i = 0; i < registeredNodes.size(); i++) {
            int currentNode = registeredNodes.get(i);
            int nextNode = registeredNodes.get((i + 1) % registeredNodes.size());
            
            try {
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress("localhost", 50000 + currentNode)
                        .usePlaintext()
                        .build();
                
                NodeServiceGrpc.NodeServiceBlockingStub stub = NodeServiceGrpc.newBlockingStub(channel);
                
                MessageRequest request = MessageRequest.newBuilder()
                        .setOrigin(0)
                        .setMessage(nextNode)
                        .build();
                
                stub.setNext(request);
                System.out.println("Connected Node " + currentNode + " -> Node " + nextNode);
                channel.shutdown();
            } catch (Exception e) {
                System.err.println("Failed to connect Node " + currentNode + " to Node " + nextNode + ": " + e.getMessage());
            }
        }
        System.out.println("Ring topology complete!");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(50099)
                .addService(new PeerRegister())
                .build()
                .start();
        
        System.out.println("PeerRegister running on port 50099");
        System.out.println("Ready to accept node registrations");
        
        server.awaitTermination();
    }
}
