import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class LAB10_Client {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 9876;
    private static final int BUFFER_SIZE = 1024;

    private static final int STATUS_OK = 1;
    private static final int STATUS_EXIT = 0;

    private static final String[] FILE_NAMES = {
            "Bornova.csv",
            "Balcova.csv",
            "Gaziemir.csv",
            "Karabaglar.csv",
            "Karsiyaka.csv",
            "Konak.csv",
            "Seferihisar.csv",
            "Urla.csv"
    };

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);

            for (String fileName : FILE_NAMES) {
                sendMessage(socket, serverAddress, fileName);
            }

            boolean running = true;
            while (running) {
                System.out.print("Send a district name to server to see stats (type 'EXIT' to quit): ");
                String query = scanner.nextLine().trim();

                sendMessage(socket, serverAddress, query);
                DatagramPacket reply = receiveReply(socket);

                ByteBuffer buffer = ByteBuffer.wrap(reply.getData(), 0, reply.getLength());
                int status = buffer.getInt();

                if (status == STATUS_OK) {
                    long totalPassengers = buffer.getLong();
                    long totalTrips = buffer.getLong();
                    double averagePassengersPerTrip = buffer.getDouble();

                    System.out.println("Here are the stats for the district " + query + ":");
                    System.out.println("Total number of passengers: " + totalPassengers);
                    System.out.println("Total number of trips: " + totalTrips);
                    System.out.printf("Average number of passengers per trip: %.2f%n", averagePassengersPerTrip);
                    System.out.println();
                } else {
                    String message = new String(
                            reply.getData(),
                            Integer.BYTES,
                            reply.getLength() - Integer.BYTES,
                            StandardCharsets.UTF_8
                    );

                    System.out.println(message);
                    if (status == STATUS_EXIT) {
                        running = false;
                    }
                    System.out.println();
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static void sendMessage(DatagramSocket socket, InetAddress serverAddress, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
        socket.send(packet);
    }

    private static DatagramPacket receiveReply(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }
}
