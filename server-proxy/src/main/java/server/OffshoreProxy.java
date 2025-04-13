package server;

import java.io.*;
import java.net.*;

public class OffshoreProxy {

    private static final int SERVER_PORT = 9090;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
        System.out.println("🏝️ OffshoreProxy started on port " + SERVER_PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleRequest(clientSocket)).start();
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream clientOutput = clientSocket.getOutputStream()
        ) {
            String requestLine = clientReader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            System.out.println("🔍 Incoming request: " + requestLine);
            boolean isConnect = requestLine.startsWith("CONNECT");

            String host = null;
            int port = isConnect ? 443 : 80;

            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append(requestLine).append("\r\n");

            String line;
            while ((line = clientReader.readLine()) != null && !line.isEmpty()) {
                headerBuilder.append(line).append("\r\n");
                if (line.toLowerCase().startsWith("host:")) {
                    String hostLine = line.split(":", 2)[1].trim();
                    if (hostLine.contains(":")) {
                        String[] parts = hostLine.split(":");
                        host = parts[0];
                        port = Integer.parseInt(parts[1]);
                    } else {
                        host = hostLine;
                    }
                }
            }
            headerBuilder.append("\r\n");

            // For CONNECT, extract host from request line
            if (isConnect && host == null) {
                String[] parts = requestLine.split(" ");
                if (parts.length >= 2) {
                    String[] hostParts = parts[1].split(":");
                    host = hostParts[0];
                    port = Integer.parseInt(hostParts[1]);
                }
            }

            if (host == null) {
                System.err.println("❌ Host not found, dropping connection.");
                return;
            }

            Socket targetSocket = new Socket(host, port);
            System.out.println("🌐 Connected to target: " + host + ":" + port);

            InputStream targetIn = targetSocket.getInputStream();
            OutputStream targetOut = targetSocket.getOutputStream();

            if (isConnect) {
                // Respond 200 OK to ShipProxy
                clientOutput.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
                clientOutput.flush();
            } else {
                // Forward the full HTTP request
                targetOut.write(headerBuilder.toString().getBytes());
                targetOut.flush();
            }

            Thread t1 = new Thread(() -> {
                try {
                    streamPipe(clientSocket.getInputStream(), targetOut);
                } catch (IOException e) {
                    System.err.println("❌ Error piping client → target: " + e.getMessage());
                }
            });

            Thread t2 = new Thread(() -> {
                try {
                    streamPipe(targetIn, clientOutput);
                } catch (IOException e) {
                    System.err.println("❌ Error piping target → client: " + e.getMessage());
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            System.out.println("✅ Finished handling " + requestLine);
            targetSocket.close();
            clientSocket.close();

        } catch (IOException | InterruptedException e) {
            System.err.println("❌ OffshoreProxy error: " + e.getMessage());
        }
    }

    private static void streamPipe(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
            out.flush();
        }
    }
}
