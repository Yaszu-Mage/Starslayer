package org.starSlayer;

import com.google.gson.JsonObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CompletableFuture;

import static org.starSlayer.Main.connect;

public class User {
    public String username;

    public User(String username) {
        this.username = username;
    }
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
