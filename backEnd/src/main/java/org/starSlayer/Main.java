package org.starSlayer;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.abs;

public class Main {
    public static final String databaseUrl = "jdbc:sqlite:" + "database.db";
    public static chatHandler chatHandler = new chatHandler();

    public static HttpServer httpServer;

    public static void main(String[] args) {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/api/chatSend", new chatSendHandler());
        httpServer.createContext("/api/chatUpdate", new chatUpdateHandler());
        httpServer.createContext("/api/login", new chatSendHandler());
        httpServer.setExecutor(null);
        httpServer.start();
        chatHandler.login("test", "test");
        chatHandler.login("test", "incorrect ' OR 1=1");

    }

    public static class LoginHandler implements HttpHandler {
        /**
         * Data should be held
         * {username : String, password : String}
         * @param exchange the exchange containing the request from the
         *                 client and used to send the response
         * @throws IOException
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, 0);
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            String username = object.get("username").getAsString();
            String password = object.get("password").getAsString();
            CompletableFuture<Boolean> login = chatHandler.login(username, password);
            login.thenAccept(success -> {
                try {
                    if (success) {
                        String key = UUID.randomUUID().toString();
                        key = Base64.getEncoder().encodeToString(key.getBytes());
                        try (Connection connection = connect()){
                            PreparedStatement statement = connection.prepareStatement("INSERT INTO sessions (session_key, username,timestamp) VALUES (?, ?,?)");
                            statement.setString(1, key);
                            statement.setString(2, username);
                            statement.setInt(3, LocalTime.now().getHour());
                            statement.execute();
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        exchange.sendResponseHeaders(200, 0);
                        exchange.getResponseBody().write(key.getBytes());
                        exchange.getResponseBody().close();
                    } else {
                        exchange.sendResponseHeaders(401, 0);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static class chatUpdateHandler implements HttpHandler {


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, 0);
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();


        }
    }

    public static Connection connect() {
        try {
            var conn = DriverManager.getConnection(databaseUrl);
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, displayName TEXT, password TEXT)").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, message TEXT, timestamp INTEGER, uuid TEXT)").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS sessions (session_key TEXT PRIMARY KEY, username TEXT, timestamp INTEGER)").execute();
            IO.println("Connection to SQLite has been established.");
            return conn;
        } catch (IllegalStateException _) {
            throw new IllegalStateException("Failed to connect to SQLite using URL: " + databaseUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to SQLite using URL: " + databaseUrl, e);
        }
    }


    public static class chatSendHandler implements HttpHandler {
        /**
         * Data should be held
         * {
         * message : String,
         * username : String,
         * sessionKey : String
         * }
         * @param exchange the exchange containing the request from the
         *                 client and used to send the response
         * @throws IOException
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, 0);
            }
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(body).getAsJsonObject();
            String message = object.get("message").getAsString();
            String username = object.get("username").getAsString();
            String sessionKey = object.get("sessionKey").getAsString();
            if (chatHandler.isSessionValid(sessionKey, username)) {
                String uuid = UUID.randomUUID().toString();
                try (Connection connection = connect()){
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
                    exchange.sendResponseHeaders(200, 0);
                    exchange.getResponseBody().write(json.toString().getBytes());
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                exchange.sendResponseHeaders(401, 0);
                exchange.getResponseBody().write("Invalid session key".getBytes());
                exchange.getResponseBody().close();
                return;
            }
            exchange.sendResponseHeaders(405, 0);
            exchange.getResponseBody().close();
        }


    }

    public static class chatHandler {

        public List<User> activeUsers = new ArrayList<>();
        public int timeoutTime = 4;

        public static boolean isSessionValid(String sessionKey,String username) {
            try (Connection connection = connect()) {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM sessions WHERE session_key = ? AND username = ?");
                statement.setString(1, sessionKey);
                statement.setString(2, username);
                statement.execute();
                try (var rs = statement.executeQuery()) {
                    int seconds = statement.getResultSet().getInt("timestamp");
                    Instant instant = Instant.ofEpochSecond(seconds);
                    boolean isLasting = instant.isAfter(Instant.now().plus(4,ChronoUnit.HOURS));
                    if (!isLasting) {
                        PreparedStatement stmt = connection.prepareStatement("DELETE FROM sessions WHERE session_key = ? AND username = ?");
                        stmt.setString(1, sessionKey);
                        stmt.setString(2, username);
                        stmt.execute();
                        return false;
                    }
                    return rs.next();
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }


        public CompletableFuture<Boolean> login(String username, String password) {
            User user = new User(username);
            AtomicBoolean isNotDropped = new AtomicBoolean(user.isRegistered());
            CompletableFuture<Boolean> future = user.login(password);
            future.thenAccept(success -> {
                if (success) {
                    activeUsers.add(user);
                    isNotDropped.set(true);
                    IO.println("Login successful, " + username);
                } else {
                    isNotDropped.set(false);
                    IO.println("Login failed, " + username);
                }
            });
            return future;
        }


    }

    public record User(String username) {

        public CompletableFuture<Boolean> login(String password) {
                if (isRegistered()) {
                    if (isPasswordCorrect(password)) {
                        return CompletableFuture.completedFuture(true);
                    }
                } else {
                    register(password);
                }
                return CompletableFuture.completedFuture(false);
            }

            public boolean setDisplayName(String displayName) {
                try {
                    Connection conn = connect();
                    assert conn != null;
                    PreparedStatement statement = conn.prepareStatement("UPDATE users SET displayName = ? WHERE username = ?");
                    statement.setString(1, displayName);
                    statement.setString(2, username);
                    statement.execute();
                    conn.close();
                    return true;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public String displayName() {

                try {
                    Connection conn = connect();
                    assert conn != null;
                    PreparedStatement preparedStatement = conn.prepareStatement("SELECT displayName FROM users WHERE username = ?");
                    preparedStatement.setString(1, username);
                    preparedStatement.execute();
                    if (preparedStatement.getResultSet().next()) {
                        return preparedStatement.getResultSet().getString("displayName");
                    }
                    conn.close();
                } catch (Exception _) {
                }
                return username;
            }

            public boolean isRegistered() {
                try {
                    Connection conn = connect();
                    assert conn != null;
                    PreparedStatement statement = conn.prepareStatement("SELECT 1 FROM users WHERE username = ?");
                    statement.setString(1, username);
                    statement.execute();
                    try (var rs = statement.executeQuery()) {
                        if (rs.next()) {
                            conn.close();
                            return true;
                        }
                    }
                    conn.close();
                } catch (Exception _) {
                }
                return false;
            }

            public boolean isPasswordCorrect(String password) {
                try {
                    Connection conn = connect();
                    assert conn != null;
                    PreparedStatement statement = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
                    statement.setString(1, username);
                    statement.setString(2, password);
                    statement.execute();
                    try (var rs = statement.executeQuery()) {
                        return rs.next();
                    } catch (Exception _) {
                        return false;
                    }
                } catch (Exception _) {
                }
                return false;
            }

            public JsonObject toJson() {
                JsonObject json = new JsonObject();
                json.addProperty("username", username);
                json.addProperty("displayName", displayName());
                return json;
            }

            public void register(String password) {
                try {
                    Connection conn = connect();
                    assert conn != null;
                    if (isRegistered()) {
                        IO.println("User already registered");
                        return;
                    }
                    PreparedStatement statement = conn.prepareStatement("INSERT INTO users (username,displayName, password) VALUES (?, ?,?)");
                    statement.setString(1, username);
                    statement.setString(2, username);
                    statement.setString(3, password);
                    statement.execute();
                    conn.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

        }
}
