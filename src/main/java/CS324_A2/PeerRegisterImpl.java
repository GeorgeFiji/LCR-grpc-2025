//Author: George Fong
package CS324_A2;

import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class PeerRegisterImpl extends UnicastRemoteObject implements PeerRegister {
    public Registry registry;
    private final ArrayList<Integer> peers;
    private boolean electionInProgress;
    private final Object electionLock;

    protected PeerRegisterImpl() throws RemoteException {
        this.peers = new ArrayList<>();
        this.registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
        this.electionInProgress = false;
        this.electionLock = new Object();
    }

    @Override
    public synchronized void register(int nodeId) throws RemoteException, NotBoundException {
        synchronized (electionLock) {
            if (electionInProgress) {
                System.out.println("Node " + nodeId + " blocked - Election is currently in progress");
                throw new RemoteException("Cannot register Node " + nodeId + " - Election is currently in progress. Please wait for the election to complete.");
            }
        }

        System.out.println("Attempting to register Node " + nodeId);

        synchronized (electionLock) {
            if (peers.contains(nodeId)) {
                throw new RemoteException("Node " + nodeId + " is already registered.");
            }

            peers.add(nodeId);
            System.out.println("Current peers: " + peers);

            setupRingTopology();

            System.out.println("Node " + nodeId + " registered successfully.");
        }
    }

    @Override
    public void notifyElectionStarted(int nodeId) throws RemoteException {
        synchronized (electionLock) {
            if (!electionInProgress) {
                electionInProgress = true;
                System.out.println("PeerRegister: Election started by Node " + nodeId + " - Registration blocked");
            }
        }
    }

    @Override
    public void notifyElectionEnded(int nodeId) throws RemoteException {
        synchronized (electionLock) {
            if (electionInProgress) {
                electionInProgress = false;
                System.out.println("PeerRegister: Election ended - Registration now allowed");
            }
        }
    }

    private void setupRingTopology() throws RemoteException, NotBoundException {
        if (peers.size() < 2) {
            return;
        }
        System.out.println("Setting up ring topology for peers: " + peers);
        for (int i = 0; i < peers.size(); i++) {
            int currentNodeId = peers.get(i);
            int nextNodeId = peers.get((i + 1) % peers.size());
            NodeInterface currentNodeInterface = (NodeInterface) registry.lookup("Node" + currentNodeId);
            NodeInterface nextNodeInterface = (NodeInterface) registry.lookup("Node" + nextNodeId);
            currentNodeInterface.setNextNode(nextNodeInterface);
            System.out.println("Connected Node " + currentNodeId + " -> Node " + nextNodeId);
        }
        System.out.println("Ring topology setup complete.");
    }

    public static void main(String[] args) throws RemoteException {
        System.setProperty("java.rmi.server.hostname", "localhost");
        try {
            // Create an in-process RMI registry so clients can lookup PeerRegister without external rmiregistry
            Registry registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            PeerRegisterImpl peerRegister = new PeerRegisterImpl();
            registry.bind("PeerRegister", peerRegister);
            peerRegister.registry = registry;
            System.out.println("Peer register node running on port " + Registry.REGISTRY_PORT + " (in-process registry) ...");
            System.out.println("Ready to accept node registrations.");
            Thread.currentThread().join();
        } catch (AlreadyBoundException e) {
            System.err.println("Peer register is already bound.");
        } catch (InterruptedException e) {
            System.err.println("Interrupted while waiting.");
        }
    }
}
