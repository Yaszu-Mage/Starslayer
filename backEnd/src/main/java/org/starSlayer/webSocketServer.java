package org.starSlayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static org.starSlayer.Main.connect;

public class webSocketServer extends WebSocketServer {

    public webSocketServer(int port) {
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

    roomHandler roomHandler = new roomHandler();


    enum UpdateType {
        ChatMessage,
        RoomCreation,
        RoomRemoval,
        RoomJoin,
        RoomLeave,
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
        JsonObject information = JsonParser.parseString(message).getAsJsonObject();
        UpdateType updateType = UpdateType.valueOf(information.get("updatetype").getAsString());
        String key = information.get("key").getAsString();
        String username = information.get("username").getAsString();
        if (chatHandler.isSessionValid(key, username)) {


            switch (updateType) {
                case RoomCreation -> {
                    information.get("roomName").getAsString();
                    String description = information.get("roomDescription").getAsString();
                    String password = information.get("roomPassword").getAsString();


                }
                case UpdateType.ChatMessage -> {
                    String roomName = information.get("roomName").getAsString() != null ? information.get("roomName").getAsString() : "general";
                    roomHandler.Room room = roomHandler.getRoom(roomName);
                    room.sendMessage(message);
                }
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
