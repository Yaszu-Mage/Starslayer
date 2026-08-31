import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public static final String databaseUrl = "jdbc:sqlite:" + Path.of("database.db").toAbsolutePath();
public static chatHandler chatHandler = new chatHandler();


public HttpServer httpServer;

void main() {
    try {
        start();
    } catch (IOException e) {
        e.printStackTrace();
    }
}

public void start() throws IOException {
    httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
    httpServer.createContext("/api/chatSend", new chatSendHandler());
    httpServer.createContext("/api/chatUpdate", new chatUpdateHandler());
    httpServer.setExecutor(null);
    httpServer.start();
    chatHandler.login("test", "test");
    chatHandler.login("test", "incorrect");
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
        IO.println("Connection to SQLite has been established.");
        return conn;
    } catch (IllegalStateException _) {
        throw new IllegalStateException("Failed to connect to SQLite using URL: " + databaseUrl);
    } catch (SQLException e) {
        throw new IllegalStateException("Failed to connect to SQLite using URL: " + databaseUrl, e);
    }
}


public static class chatSendHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

    }
}

public static class chatHandler {

    public List<User> activeUsers = new ArrayList<>();


    public void save() {
        JsonArray json = new JsonArray();
        for (User user : activeUsers) {
            json.add(user.toJson());
        }
    }


    public boolean login(String username, String password) {
        User user = new User(username);
        AtomicBoolean isNotDropped = new AtomicBoolean(user.isRegistered());
        CompletableFuture<Boolean> future = user.login(password);
        future.thenAccept(success -> {
            if (success) {
                activeUsers.add(user);
                isNotDropped.set(true);
            } else {
                isNotDropped.set(false);
                IO.println("Login failed, " + username);
            }
        });
        return isNotDropped.get();
    }


}

public static class User {
    private final String username;


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

    public String getUsername() {
        return username;
    }

    public boolean isRegistered() {
        try {
            Connection conn = connect();
            assert conn != null;
            PreparedStatement statement = conn.prepareStatement("SELECT * FROM users WHERE username = ?");
            statement.setString(1, username);
            statement.execute();
            conn.close();
            return statement.getResultSet().next();
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
            conn.close();
            return statement.getResultSet().next();
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
