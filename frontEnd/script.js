const wsUri = "wss://socket.yaszu.xyz:8081";



const ws = new WebSocket(wsUri);

ws.addEventListener("open", () => {
    console.log("Connected to WebSocket server");
});