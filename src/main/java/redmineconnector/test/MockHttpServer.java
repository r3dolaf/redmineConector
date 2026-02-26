package redmineconnector.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple HTTP Mock Server for testing HttpDataService.
 * Simulates Redmine API responses without real HTTP calls.
 * 
 * Usage:
 * MockHttpServer server = new MockHttpServer(8080);
 * server.addResponse("GET", "/issues.json", "{\"issues\":[...]}");
 * server.start();
 * // ... run tests ...
 * server.stop();
 */
public class MockHttpServer {

    private final int port;
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private volatile boolean running = false;
    private final Map<String, String> responses = new HashMap<>();
    private final Map<String, Integer> statusCodes = new HashMap<>();

    public MockHttpServer(int port) {
        this.port = port;
    }

    /**
     * Add a mock response for a specific endpoint.
     * 
     * @param method       HTTP method (GET, POST, PUT, DELETE)
     * @param path         URL path (e.g., "/issues.json")
     * @param responseBody JSON response body
     */
    public void addResponse(String method, String path, String responseBody) {
        String key = method + " " + path;
        responses.put(key, responseBody);
        statusCodes.put(key, 200); // Default OK
    }

    /**
     * Add a mock response with custom status code.
     */
    public void addResponse(String method, String path, int statusCode, String responseBody) {
        String key = method + " " + path;
        responses.put(key, responseBody);
        statusCodes.put(key, statusCode);
    }

    /**
     * Start the mock HTTP server.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleRequest(clientSocket);
                } catch (IOException e) {
                    if (running) {
                        System.err.println("MockServer error: " + e.getMessage());
                    }
                }
            }
        });
    }

    /**
     * Stop the mock HTTP server.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executor != null) {
                executor.shutdownNow();
            }
        } catch (IOException e) {
            System.err.println("Error stopping MockServer: " + e.getMessage());
        }
    }

    private void handleRequest(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                OutputStream output = socket.getOutputStream()) {

            // Read request line
            String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }

            // Parse method and path
            String[] parts = requestLine.split(" ");
            if (parts.length < 2) {
                sendResponse(output, 400, "Bad Request");
                return;
            }

            String method = parts[0];
            String fullPath = parts[1];

            // Extract path without query string
            String path = fullPath.split("\\?")[0];

            // Read headers (skip for simplicity, but consume them)
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                // Skip headers
            }

            // Find matching response
            String key = method + " " + path;
            String responseBody = responses.get(key);
            Integer statusCode = statusCodes.getOrDefault(key, 404);

            if (responseBody != null) {
                sendResponse(output, statusCode, responseBody);
            } else {
                // Try wildcard matching for parameterized paths
                String wildcardKey = findWildcardMatch(method, path);
                if (wildcardKey != null) {
                    responseBody = responses.get(wildcardKey);
                    statusCode = statusCodes.getOrDefault(wildcardKey, 200);
                    sendResponse(output, statusCode, responseBody);
                } else {
                    sendResponse(output, 404, "{\"error\":\"Not Found\"}");
                }
            }

        } catch (IOException e) {
            System.err.println("Error handling request: " + e.getMessage());
        }
    }

    private String findWildcardMatch(String method, String path) {
        // Support patterns like "/issues/{id}.json"
        for (String key : responses.keySet()) {
            String[] keyParts = key.split(" ");
            if (keyParts.length == 2 && keyParts[0].equals(method)) {
                String pattern = keyParts[1];
                if (matchesPattern(path, pattern)) {
                    return key;
                }
            }
        }
        return null;
    }

    private boolean matchesPattern(String path, String pattern) {
        // Simple wildcard matching: /issues/*.json matches /issues/123.json
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", "\\d+");
            return path.matches(regex);
        }
        return false;
    }

    private void sendResponse(OutputStream output, int statusCode, String body) throws IOException {
        String statusText = getStatusText(statusCode);
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);

        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");
        response.append("Content-Type: application/json; charset=UTF-8\r\n");
        response.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        response.append("Connection: close\r\n");
        response.append("\r\n");

        output.write(response.toString().getBytes(StandardCharsets.UTF_8));
        output.write(bodyBytes);
        output.flush();
    }

    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200:
                return "OK";
            case 201:
                return "Created";
            case 204:
                return "No Content";
            case 400:
                return "Bad Request";
            case 404:
                return "Not Found";
            case 500:
                return "Internal Server Error";
            default:
                return "Unknown";
        }
    }

    /**
     * Get the server URL.
     */
    public String getUrl() {
        return "http://localhost:" + port;
    }
}
