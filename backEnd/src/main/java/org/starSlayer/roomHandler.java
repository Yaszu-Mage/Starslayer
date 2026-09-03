package org.starSlayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

import static org.starSlayer.Main.connect;

public class roomHandler {
    private static final ArrayList<Room> activeRooms = new ArrayList<>();
    public ArrayList<Room> getRooms() {
        return activeRooms;
    }
    public ArrayList<WebSocket> getUsers(Room room) {
        return room.users;
    }
    public void addUser(Room room, WebSocket user) {
        room.users.add(user);
    }
    public static void addRoom(Room room) throws SQLException {
        activeRooms.add(room);
        try (Connection conn = connect()) {
            conn.prepareStatement("INSERT INTO rooms (name,password,description,maxUsers,isPrivate) VALUES (?,?,?,?,?)").execute();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static Room getUsersRoom(WebSocket user) {
        for (Room room : activeRooms) {
            if (room.users.contains(user)) {
                return room;
            }
        }
        return null;
    }



    public static Room createRoom(String name, String password,String description, int maxUsers, boolean isPrivate,WebSocket user) throws SQLException {
        Room room = new Room(name, password,description, maxUsers, isPrivate);
        addRoom(room);
        if (user != null) {
            room.addUser(user);
        }
        return room;
    }

    public static Room getRoom(String name) {
        for (Room room : activeRooms) {
            if (room.name.equals(name)) {
                return room;
            }
        }
        return null;
    }

    public static void removeRoom(Room room) {
        activeRooms.remove(room);
        room.delete();
        room = null;
    }
    public static class Room {
        private final ArrayList<WebSocket> users = new ArrayList<>();
        public String name;
        public String description;
        public String password;
        public int maxUsers;
        public boolean isPrivate;
        public int userCount() {
            return users.size();
        }
        public boolean isFull() {
            if (maxUsers == -1) {
                return false;
            }
            return userCount() >= maxUsers;
        }

        public Room(String name,String password,String description, int maxUsers, boolean isPrivate) {
            this.name = name;
            this.password = password;
            this.description = description;
            this.maxUsers = maxUsers;
            this.isPrivate = isPrivate;
        }

        HttpClient httpClient = HttpClient.newHttpClient();
        public void sendMessage(String message) {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String messageTxt = json.get("message").getAsString();
            try (Connection conn = connect()) {
                PreparedStatement stmt = conn.prepareStatement("INSERT INTO messages (username,message,timestamp,uuid,room) VALUES (?,?,?,?,?)");
                stmt.setString(1, json.get("username").getAsString());
                stmt.setString(2, messageTxt);
                stmt.setLong(3, Instant.now().getEpochSecond());
                UUID uuid = UUID.randomUUID();
                stmt.setString(4, uuid.toString());
                stmt.setString(5, name);
                stmt.execute();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            for (WebSocket user : users) {
                user.send(messageTxt);
            }
        }

        public void addUser(WebSocket user) {
            if (!isFull()) {
                users.add(user);
                return;
            }
            JsonObject json = new JsonObject();
            json.addProperty("type", "error");
            json.addProperty("message", "Room is full");
            user.send(json.toString());
        }

        public void removeUser(WebSocket user) {
            users.remove(user);
        }

        public void delete() {
            activeRooms.remove(this);
            this.users.clear();
            this.name = null;
        }




    }



}
