package webSocketClient;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;


import java.net.URI;
import java.util.Map;

public class socketClient extends WebSocketClient {
    public socketClient(URI serverUri, Draft draft) {
        super(serverUri, draft);
    }

    public socketClient(URI serverURI) {
        super(serverURI);
    }

    public socketClient(URI serverUri, Draft draft,Map<String, String> httpHeaders, int connectTimeout) {
        super(serverUri, draft,httpHeaders,connectTimeout);
    }
    public void sendMessage(String mex){
            send(mex);
    }

    @Override
    public void onOpen(ServerHandshake handshakes) {
        //send("Hello, it is me. Mario :)");
        System.out.println("opened connection");
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
    }

    @Override
    public void onMessage(String message) {
        System.out.println("received from server: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        System.out.println(
                "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: "
                        + reason);
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        // if the error is fatal then onClose will be called additionally
    }

}