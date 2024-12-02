import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class DAS {
    private static int port;
    private static int number;
    private static DatagramSocket socket;
    private static List<Integer> receivedNumbers = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java DAS.java <port> <number>");
            return;
        }

        try {
            port = Integer.parseInt(args[0]);
            number = Integer.parseInt(args[1]);
            initialize();
        } catch (NumberFormatException e) {
            System.err.println("Invalid parameters. Both <port> and <number> must be integers.");
        }
    }

    private static void initialize() {
        try {
            socket = new DatagramSocket(port);
            System.out.println("Running in master mode on port " + port);
            runMaster();
        } catch (SocketException e) {
            System.out.println("Running in slave mode. Sending number " + number + " to port " + port);
            runSlave();
        }
    }

    private static void runMaster() {
        receivedNumbers.add(number);
        byte[] buffer = new byte[256];
        try {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                int receivedValue = Integer.parseInt(message.trim());


                if (receivedValue == 0) {
                    computeAndBroadcastAverage();
                } else if (receivedValue == -1) {
                    System.out.println("Received -1. Terminating master.");
                    broadcastMessage("-1");
                    socket.close();
                    break;
                } else {
                    System.out.println("Received: " + receivedValue);
                    receivedNumbers.add(receivedValue);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runSlave() {
        try (DatagramSocket slaveSocket = new DatagramSocket()) {
            InetAddress masterAddress = InetAddress.getByName("localhost");
            String message = String.valueOf(number);

            DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), masterAddress, port);
            slaveSocket.send(packet);
            System.out.println("Sent number " + number + " to master.");
            slaveSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void computeAndBroadcastAverage() throws IOException {
        int sum = receivedNumbers.stream().mapToInt(Integer::intValue).sum();
        int count = receivedNumbers.size();
        int average = count == 0 ? 0 : sum / count;
        System.out.println("Average: " + average);
        broadcastMessage(String.valueOf(average));
    }

    private static void broadcastMessage(String message) throws IOException {
        InetAddress broadcastAddress = InetAddress.getByName("172.23.127.255");
        DatagramPacket packet = new DatagramPacket(message.getBytes(), message.length(), broadcastAddress, port);
        socket.send(packet);
        System.out.println("Broadcasted message: " + message);
    }
}