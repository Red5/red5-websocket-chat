package org.red5.demos.chat;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedTransferQueue;

import org.red5.server.adapter.ApplicationLifecycle;
import org.red5.server.api.scope.IScope;
import org.red5.server.net.sse.SSEService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Router for chat messages between WebSockets without any SharedObject usage.
 * 
 * @author Paul Gregoire
 */
public class Router {

    private static Logger log = LoggerFactory.getLogger(Router.class);

    private Application app;

    private WebSocketChatDataListener wsListener;

    private SSEService sseService;

    // messages are stored in a concurrent map to allow for thread-safe access
    // this is not strictly necessary for the router, but it can be useful if you want
    // entries are keyed by the path and the value is the message queue
    private ConcurrentMap<String, LinkedTransferQueue<String>> messages = new ConcurrentHashMap<>();

    private Thread routeFuture;

    /**
     * Default constructor.
     */
    public Router() {
        log.debug("Router initialized");
    }

    /**
     * Routes a message on a given path to the associated shared object.
     * 
     * @param path shared object path / name
     * @param message string
     */
    public void route(String path, String message) {
        log.debug("Route to WebSocket: {} with {}", path, message);
        // ensure the message queue exists for the path
        if (messages.computeIfAbsent(path, k -> new LinkedTransferQueue<>()).offer(message)) {
            log.debug("Message added to new queue for path: {}", path);
        } else if (messages.get(path).offer(message)) {
            log.debug("Message added to existing queue for path: {}", path);
        }
    }

    /**
     * Routes a message on a given scope to the associated websocket connections.
     * 
     * @param scope application or room scope
     * @param message string
     */
    public void route(IScope scope, String message) {
        // scope.path = /default scope.name = chat
        String path = scope.getContextPath();
        log.debug("Route to WebSocket: {} with {}", path, message);
        if (messages.get(path) == null) {
            log.warn("Message queue for path: {} did not exist", path);
        }
        if (wsListener != null) {
            wsListener.sendToAll(scope.getContextPath(), message);
        }
        if (sseService != null) {
            sseService.broadcastToScope(scope, message);
        }
    }

    /**
     * Get the chat shared object for a given path.
     * 
     * @param path path / name
     * @return the message queue for the path or null if its not available
     */
    private LinkedTransferQueue<String> getMessageQueue(String path) {
        // get the application level scope
        IScope appScope = app.getScope();
        // resolve the path given to an existing scope
        IScope scope = ScopeUtils.resolveScope(appScope, path);
        if (scope == null) {
            // attempt to create the missing scope for the given path
            if (!appScope.createChildScope(path)) {
                log.warn("Scope creation failed for {}", path);
                return null;
            }
            scope = ScopeUtils.resolveScope(appScope, path);
        }
        // get the message queue for the scope
        return messages.computeIfAbsent(scope.getContextPath(), k -> new LinkedTransferQueue<>());
    }

    public void setApp(Application app) {
        log.debug("Setting application: {}", app);
        this.app = app;
        final IScope appScope = app.getScope();
        sseService = (SSEService) appScope.getServiceHandler(SSEService.BEAN_NAME);
        if (sseService == null) {
            log.warn("SSE service was null, SSE will not be available");            
        } else {
            log.info("SSE service found in application scope: {}", appScope.getName());
        }
        final String contextPath = appScope.getContextPath();
        this.app.addListener(new ApplicationLifecycle() {

            @Override
            public void appStop(IScope scope) {
                if (wsListener != null) {
                    wsListener.stop();
                }
                // stop routing thread
                if (routeFuture != null && routeFuture.isAlive()) {
                    log.info("Stopping routing thread for application: {}", app.getName());
                    routeFuture.interrupt();
                }
                log.info("Application stopped, clearing message queues");
                // clear all message queues
                LinkedTransferQueue<String> queue = getMessageQueue(contextPath);
                if (queue != null) {
                    queue.clear();
                    log.debug("Cleared message queue for path: {}", contextPath);
                    if (messages.remove(contextPath) != null) {
                        log.debug("Removed message queue for path: {}", contextPath);
                    }
                }
            }

        });
        routeFuture = Thread.ofVirtual().start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    log.info("Router thread is running for application: {}", app.getName());
                    // Here you can implement any routing logic if needed
                    LinkedTransferQueue<String> queue = getMessageQueue(contextPath);
                    String message = queue.take();
                    if (message != null) {
                        log.info("Routing message: {}", message);
                        // Here you can implement the logic to route the message
                        if (wsListener != null) {
                            wsListener.sendToAll(contextPath, message);
                        }
                        if (sseService != null) {
                            sseService.broadcastToScope(appScope, message);
                        }
                    }                       
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    public void setWsListener(WebSocketChatDataListener wsListener) {
        this.wsListener = wsListener;
    }

}
