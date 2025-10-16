package CS324_A2;

import GeorgeFiji.NodeProto.RegisterRequest;
import GeorgeFiji.PeerRegisterServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.Scanner;

public class Node {
    private final int nodeId;
    private final Server server;
    private final NodeServiceImpl serviceImpl;
    private ManagedChannel registerChannel;

    public Node(int nodeId) throws IOException {
        this.nodeId = nodeId;
        this.serviceImpl = new NodeServiceImpl(nodeId);
        this.server = ServerBuilder.forPort(50000 + nodeId).addService(serviceImpl).build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Node " + nodeId + " started on port " + (50000 + nodeId));
        registerWithPeerRegister();
    }

    private void registerWithPeerRegister() {
        try {
            Thread.sleep(500);
            registerChannel = ManagedChannelBuilder
                    .forAddress("localhost", 50099)
                    .usePlaintext()
                    .build();
            
            PeerRegisterServiceGrpc.PeerRegisterServiceBlockingStub stub = 
                    PeerRegisterServiceGrpc.newBlockingStub(registerChannel);
            
            RegisterRequest request = RegisterRequest.newBuilder()
                    .setNodeId(nodeId)
                    .setPort(50000 + nodeId)
                    .build();
            
            stub.registerNode(request);
            System.out.println("Node " + nodeId + " registered with PeerRegister successfully");
        } catch (Exception e) {
            System.err.println("Node " + nodeId + " failed to register: " + e.getMessage());
        }
    }

    public void handleUserInput() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("Node " + nodeId + ": Enter 'election' to start election or 'exit' to quit:");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("election")) {
                serviceImpl.startElection();
            } else if (input.equalsIgnoreCase("exit")) {
                server.shutdown();
                serviceImpl.shutdown();
                if (registerChannel != null) registerChannel.shutdown();
                scanner.close();
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java CS324_A2.Node <nodeId>");
            System.exit(1);
        }
        try {
            Node node = new Node(Integer.parseInt(args[0]));
            node.start();
            node.handleUserInput();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
