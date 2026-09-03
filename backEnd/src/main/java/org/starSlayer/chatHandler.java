package org.starSlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.starSlayer.Main.connect;

public class chatHandler {
        public static HashMap<String, User> activeUsers = new HashMap<>();
        public static boolean isSessionValid(String sessionKey,String username) {
            try (Connection connection = connect()) {
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM sessions WHERE session_key = ? AND username = ?");
                statement.setString(1, sessionKey);
                statement.setString(2, username);
                try (var rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        return false;
                    }
                    long seconds = rs.getLong("timestamp");
                    Instant instant = Instant.ofEpochSecond(seconds);
                    boolean isExpired = instant.isBefore(Instant.now().minus(4, ChronoUnit.HOURS));
                    if (isExpired) {
                        PreparedStatement stmt = connection.prepareStatement("DELETE FROM sessions WHERE session_key = ? AND username = ?");
                        stmt.setString(1, sessionKey);
                        stmt.setString(2, username);
                        stmt.execute();
                        return false;
                    }
                    return true;
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
