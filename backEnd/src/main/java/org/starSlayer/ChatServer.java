package org.starSlayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.starSlayer.Main.connect;

public class ChatServer extends WebSocketServer {

    public ChatServer(int port) {
        super(new java.net.InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, org.java_websocket.handshake.ClientHandshake handshake) {
        System.out.println("New connection from " + conn.getRemoteSocketAddress());

    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Closed connection to " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);
    }
    /**
     * Data should be held
     * {
     * message : String,
     * username : String,
     * sessionKey : String
     * @throws IOException
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message from " + conn.getRemoteSocketAddress() + ": " + message);
        // Broadcast the message to all connected clients
        JsonObject information = JsonParser.parseString(message).getAsJsonObject();
        String key = information.get("key").getAsString();
        String username = information.get("username").getAsString();
        String messageText = information.get("message").getAsString();
            if (chatHandler.isSessionValid(key, username)) {
                String uuid = UUID.randomUUID().toString();
                try (Connection connection = connect()) {
                    PreparedStatement stmt = connection.prepareStatement("INSERT INTO messages (username, message, timestamp,uuid) VALUES (?, ?, ?,?)");
                    stmt.setString(1, username);
                    stmt.setString(2, message);
                    stmt.setLong(3, Instant.EPOCH.getEpochSecond());
                    stmt.setString(4, uuid);
                    stmt.execute();
                    JsonArray json = new JsonArray();
                    json.add(uuid);
                    json.add(Instant.EPOCH.getEpochSecond());
                    json.add(message);
                    conn.send(json.toString());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("An error occurred on connection " + (conn != null ? conn.getRemoteSocketAddress() : "unknown") + ":" + ex);
    }

    @Override
    public void onStart() {
        System.out.println("Server started successfully");
    }
}
