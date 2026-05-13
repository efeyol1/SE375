import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LAB10_Server {
    private static final int SERVER_PORT = 9876;
    private static final int BUFFER_SIZE = 1024;
    private static final int FILE_COUNT = 8;
    private static final String DATA_DIRECTORY = "SE375_LAB_DATA";

    private static final int STATUS_OK = 1;
    private static final int STATUS_ERROR = -1;
    private static final int STATUS_EXIT = 0;

    private static final Map<String, DistrictStats> results = new HashMap<>();

    private static class DistrictStats {
        long totalPassengers;
        long totalTrips;

        DistrictStats(long totalPassengers, long totalTrips) {
            this.totalPassengers = totalPassengers;
            this.totalTrips = totalTrips;
        }

        double averagePassengersPerTrip() {
            if (totalTrips == 0) {
                return 0.0;
            }
            return (double) totalPassengers / totalTrips;
        }
    }

    private static class FileProcessor extends Thread {
        private final String fileName;

        FileProcessor(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void run() {
            processFile(fileName);
        }
    }

    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("Server is running on port " + SERVER_PORT);

            FileProcessor[] processors = new FileProcessor[FILE_COUNT];

            for (int i = 0; i < FILE_COUNT; i++) {
                System.out.println("Waiting for a packet...");
                DatagramPacket packet = receivePacket(socket);
                String fileName = packetToString(packet);

                System.out.println("Received packet from: " + packet.getAddress()
                        + " : " + packet.getPort()
                        + ", Message: " + fileName + ". Assigning thread...");

                processors[i] = new FileProcessor(fileName);
                processors[i].start();
            }

            for (FileProcessor processor : processors) {
                processor.join();
            }

            System.out.println();
            System.out.println("Processing done! Waiting for a query...");

            boolean running = true;
            while (running) {
                DatagramPacket queryPacket = receivePacket(socket);
                String query = packetToString(queryPacket).trim();

                if (query.equalsIgnoreCase("EXIT")) {
                    System.out.println("Received EXIT. Exiting...");
                    byte[] exitReply = createMessageResponse(STATUS_EXIT, "Exiting...");
                    sendReply(socket, queryPacket, exitReply);
                    running = false;
                } else {
                    System.out.println("Received query: " + query);

                    byte[] reply;
                    DistrictStats stats = results.get(query);
                    if (stats == null) {
                        reply = createMessageResponse(STATUS_ERROR, "District doesn't exist!");
                    } else {
                        reply = createStatsResponse(stats);
                    }

                    sendReply(socket, queryPacket, reply);
                    System.out.println("Reply sent to client. Awaiting for query...");
                    System.out.println();
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Server interrupted.");
        }
    }

    private static DatagramPacket receivePacket(DatagramSocket socket) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        return packet;
    }

    private static String packetToString(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
    }

    private static void sendReply(DatagramSocket socket, DatagramPacket request, byte[] reply) throws IOException {
        DatagramPacket replyPacket = new DatagramPacket(
                reply,
                reply.length,
                request.getAddress(),
                request.getPort()
        );
        socket.send(replyPacket);
    }

    private static byte[] createStatsResponse(DistrictStats stats) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES + Long.BYTES + Double.BYTES);
        buffer.putInt(STATUS_OK);
        buffer.putLong(stats.totalPassengers);
        buffer.putLong(stats.totalTrips);
        buffer.putDouble(stats.averagePassengersPerTrip());
        return buffer.array();
    }

    private static byte[] createMessageResponse(int status, String message) {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + messageBytes.length);
        buffer.putInt(status);
        buffer.put(messageBytes);
        return buffer.array();
    }

    private static void processFile(String fileName) {
        File file = new File(DATA_DIRECTORY, fileName);
        String districtName = removeCsvExtension(fileName);

        long totalPassengers = 0;
        long totalTrips = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");
                int trips = Integer.parseInt(values[2].trim());
                int passengers = Integer.parseInt(values[3].trim());

                totalTrips += trips;
                totalPassengers += passengers;
            }

            synchronized (results) {
                results.put(districtName, new DistrictStats(totalPassengers, totalTrips));
            }
        } catch (IOException e) {
            System.out.println("Could not read " + fileName + ": " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Invalid numeric value in " + fileName + ": " + e.getMessage());
        }
    }

    private static String removeCsvExtension(String fileName) {
        if (fileName.toLowerCase().endsWith(".csv")) {
            return fileName.substring(0, fileName.length() - 4);
        }
        return fileName;
    }
}
