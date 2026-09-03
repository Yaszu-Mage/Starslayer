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
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static java.lang.Math.abs;

public class Main {
    public static final String databaseUrl = "jdbc:sqlite:" + "database.db";
    public static chatHandler chatHandler = new chatHandler();
    public static rateLimiter rateLimiter = new rateLimiter(10, 1000);
    public static HttpServer httpServer;
    public static webSocketServer webSocketServer = new webSocketServer(8081);
    public static profanityFilter profanityFilterInstance = new profanityFilter();

    static void main(String[] args) {
        try {
            start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/api/login", new LoginHandler());
        httpServer.setExecutor(null);
        httpServer.start();
        chatHandler.login("baller","baller");
        webSocketServer.start();
        roomHandler.createRoom("general","","",-1,false,null);
        IO.println(profanityFilterInstance.isProfane("f4ck"));
    }




    public static class LoginHandler implements HttpHandler {
        /**
         * Data should be held
         * {username : String, password : String}
         * @param exchange the exchange containing the request from the
         *                 client and used to send the response
         */
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                exchange.sendResponseHeaders(405, 0);
            }
            if (!rateLimiter.isAllowed(exchange.getRemoteAddress())) {
                exchange.sendResponseHeaders(405, 0);
                return;
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
                        chatHandler.activeUsers.put(key, new User(username));
                    } else {
                        exchange.sendResponseHeaders(401, 0);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }



    public static Connection connect() {
        try {
            var conn = DriverManager.getConnection(databaseUrl);
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, displayName TEXT, password TEXT)").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS rooms (name TEXT PRIMARY KEY, isPrivate INT, description TEXT, password TEXT)").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS messages (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, message TEXT, timestamp INTEGER, uuid TEXT, room TEXT)").execute();
            conn.prepareStatement("CREATE TABLE IF NOT EXISTS sessions (session_key TEXT PRIMARY KEY, username TEXT, timestamp INTEGER)").execute();
            IO.println("Connection to SQLite has been established.");
            return conn;
        } catch (IllegalStateException _) {
            throw new IllegalStateException("Failed to connect to SQLite using URL: " + databaseUrl);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to SQLite using URL: " + databaseUrl, e);
        }
    }







}
