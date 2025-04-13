package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ShipProxy {

    private static final String SERVER_HOST = "host.docker.internal"; // Use "localhost" if both run outside Docker
    private static final int SERVER_PORT = 9090;

    private static final ExecutorService requestQueue = Executors.newCachedThreadPool();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        System.out.println("🚢 ShipProxy (Client) started, listening on port 8080...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            requestQueue.submit(() -> handleRequest(clientSocket));
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try {
            BufferedReader browserReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String firstLine = browserReader.readLine();

            if (firstLine == null) return;

            System.out.println("📨 Received from browser: " + firstLine);

            boolean isConnect = firstLine.startsWith("CONNECT");

            // Connect to OffshoreProxy
            Socket offshoreSocket = new Socket(SERVER_HOST, SERVER_PORT);

            BufferedWriter offshoreWriter = new BufferedWriter(new OutputStreamWriter(offshoreSocket.getOutputStream()));
            offshoreWriter.write(firstLine + "\r\n");

            // Read and forward headers
            String line;
            while (!(line = browserReader.readLine()).isEmpty()) {
                offshoreWriter.write(line + "\r\n");
            }
            offshoreWriter.write("\r\n");
            offshoreWriter.flush();

            if (isConnect) {
                // Send 200 OK to browser
                OutputStream browserOut = clientSocket.getOutputStream();
                browserOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                browserOut.flush();
            }

            // Relay data in both directions
            relay(clientSocket, offshoreSocket);

        } catch (IOException e) {
            System.err.println("❌ Proxy error: " + e.getMessage());
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }

    private static void relay(Socket socketA, Socket socketB) {
        Thread t1 = new Thread(() -> forward(socketA, socketB));
        Thread t2 = new Thread(() -> forward(socketB, socketA));
        t1.start();
        t2.start();
    }

    private static void forward(Socket from, Socket to) {
        try (InputStream in = from.getInputStream(); OutputStream out = to.getOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        } catch (IOException ignored) {
        } finally {
            try {
                from.close();
            } catch (IOException ignored) {}
            try {
                to.close();
            } catch (IOException ignored) {}
        }
    }
}
