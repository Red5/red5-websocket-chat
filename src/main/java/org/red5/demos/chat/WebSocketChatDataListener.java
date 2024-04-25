package org.red5.demos.chat;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.red5.net.websocket.WSConstants;
import org.red5.net.websocket.WebSocketConnection;
import org.red5.net.websocket.listener.WebSocketDataListener;
import org.red5.net.websocket.model.WSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Handler / router for chat data.
 * 
 * @author Paul Gregoire
 */
public class WebSocketChatDataListener extends WebSocketDataListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketChatDataListener.class);

    {
        setProtocol("chat");
    }

    private Router router;

    private Set<WebSocketConnection> connections = new HashSet<>();

    @Override
    public void onWSConnect(WebSocketConnection conn) {
        log.info("Connect: {}", conn);
        Optional<List<String>> header = Optional.ofNullable(conn.getHeaders().get(WSConstants.WS_HEADER_PROTOCOL));
        if (header.isPresent()) {
            log.debug("Protocol header exists");
            String protocol = header.get().get(0);
            if (protocol.contains("chat")) {
                log.debug("Chat enabled");
                conn.setProtocol(protocol);
            } else {
                log.info("Chat is not in the connections protocol list");
            }
        }
        connections.add(conn);
    }

    @Override
    public void onWSDisconnect(WebSocketConnection conn) {
        log.info("Disconnect: {}", conn);
        connections.remove(conn);
    }

    @Override
    public void onWSMessage(WSMessage message) {
        // if its protocol doesn't match then skip the message
        if (!protocol.equals(message.getConnection().getProtocol())) {
            log.debug("Skipping message due to protocol mismatch");
            return;
        }
        // get the connection path for routing
        String path = message.getConnection().getPath();
        log.debug("WebSocket connection path: {}", path);
        // assume we have text
        String msg = new String(message.getPayload().array()).trim();
        log.info("onWSMessage: {}\n{}", msg, message.getConnection());
        // do a quick hacky json check
        if (msg.indexOf('{') != -1 && msg.indexOf(':') != -1) {
            log.info("JSON encoded text message");
            // channelName == roomid in most cases
            try {
                JsonObject obj = JsonParser.parseString(msg).getAsJsonObject();
                log.debug("Parsed - keys: {}\ncontent: {}", obj.keySet(), obj);
                msg = obj.toString();
                // send to all websocket connections matching this connections path
                sendToAll(path, msg);
                // send to the shared object matching this connections path
                router.route(path, msg);
            } catch (Exception e) {
                log.warn("Exception parsing JSON", e);
            }
        } else {
            log.info("Standard text message");
            // send to all websocket connections matching this connections path
            sendToAll(path, msg);
            // send to the shared object matching this connections path
            router.route(path, msg);
        }
    }

    /**
     * Send message to all connected connections.
     * 
     * @param path routable path / name
     * @param message string
     */
    public void sendToAll(String path, String message) {
        for (WebSocketConnection conn : connections) {
            if (path.equals(conn.getPath())) {
                try {
                    conn.send(message);
                } catch (Exception e) {
                    log.warn("Exception sending message", e);
                }
            } else {
                log.trace("Path did not match for message {} != {}", path, conn.getPath());
            }
        }
    }

    public void setRouter(Router router) {
        this.router = router;
        this.router.setWsListener(this);
    }

    @Override
    public void stop() {
        connections.forEach(wc -> wc.close());
        connections.clear();
    }

}
