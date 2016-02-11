package org.red5.demos.chat;

import java.util.List;
import java.util.Map;

import org.red5.logging.Red5LoggerFactory;
import org.red5.server.api.IAttributeStore;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.so.ISharedObject;
import org.red5.server.api.so.ISharedObjectBase;
import org.red5.server.api.so.ISharedObjectListener;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;

/**
 * Router for chat messages between WebSockets and an associated SharedObject.
 * 
 * @author Paul Gregoire
 */
public class Router {

    private static Logger log = Red5LoggerFactory.getLogger(Router.class, "chat");

    private Application app;

    private WebSocketChatDataListener wsListener;

    /**
     * Routes a message on a given path to the associated shared object.
     * 
     * @param path
     * @param message
     */
    public void route(String path, String message) {
        log.debug("Route to Shared Object: {} with {}", path, message);
        // get the shared object
        ISharedObject so = getSharedObject(path);
        if (so != null) {
            // set the message attribute
            so.setAttribute("message", message);
        } else {
            log.warn("Shared object was not available for path: {}", path);
        }
    }

    /**
     * Routes a message on a given scope to the associated websocket connections.
     * 
     * @param scope
     * @param message
     */
    public void route(IScope scope, String message) {
        // scope.path = /default scope.name = chat
        String path = scope.getContextPath();
        log.debug("Route to WebSocket: {} with {}", path, message);
        if (getSharedObject(path) == null) {
            log.warn("Shared object for path: {} did not exist", path);
        }
        if (wsListener != null) {
            wsListener.sendToAll(scope.getContextPath(), message);
        }
    }

    /**
     * Get the chat shared object for a given path.
     * 
     * @param path
     * @return the shared object for the path or null if its not available
     */
    private ISharedObject getSharedObject(String path) {
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
        // get the shared object
        ISharedObject so = app.getSharedObject(scope, "chat");
        if (so == null) {
            if (!app.createSharedObject(scope, "chat", false)) {
                log.warn("Chat SO creation failed");
                return null;
            }
            // get the newly created shared object
            so = app.getSharedObject(scope, "chat");
        }
        // ensure the so is acquired and our listener has been added
        if (!so.isAcquired()) {
            // acquire the so to prevent it being removed unexpectedly
            so.acquire(); // TODO in a "real" world implementation, this would need to be paired with a call to release when the so is no longer needed
            // add a listener for detecting sync on the so
            so.addSharedObjectListener(new SOListener(this, scope, path));
        }
        return so;
    }

    public void setApp(Application app) {
        this.app = app;
    }

    public void setWsListener(WebSocketChatDataListener wsListener) {
        this.wsListener = wsListener;
    }

    private final class SOListener implements ISharedObjectListener {

        private final Router router;

        private final IScope scope;

        private final String path;

        SOListener(Router router, IScope scope, String path) {
            log.debug("ctor - path: {} scope: {}", path, scope);
            this.router = router;
            this.scope = scope;
            this.path = path;
        }

        public void onSharedObjectClear(ISharedObjectBase so) {
            log.debug("onSharedObjectClear path: {}", path);
        }

        public void onSharedObjectConnect(ISharedObjectBase so) {
            log.debug("onSharedObjectConnect path: {}", path);
        }

        public void onSharedObjectDelete(ISharedObjectBase so, String key) {
            log.debug("onSharedObjectDelete path: {} key: {}", path, key);
        }

        public void onSharedObjectDisconnect(ISharedObjectBase so) {
            log.debug("onSharedObjectDisconnect path: {}", path);
        }

        public void onSharedObjectSend(ISharedObjectBase so, String method, List<?> attributes) {
            log.debug("onSharedObjectSend path: {} - method: {} {}", path, method, attributes);
        }

        public void onSharedObjectUpdate(ISharedObjectBase so, IAttributeStore attributes) {
            log.debug("onSharedObjectUpdate path: {} - {}", path, attributes);
        }

        public void onSharedObjectUpdate(ISharedObjectBase so, Map<String, Object> attributes) {
            log.debug("onSharedObjectUpdate path: {} - {}", path, attributes);
        }

        public void onSharedObjectUpdate(ISharedObjectBase so, String key, Object value) {
            log.debug("onSharedObjectUpdate path: {} - {} = {}", path, key, value);
            // route to the websockets if we have an RTMP connection as the originator, otherwise websockets will get duplicate messages
            if (Red5.getConnectionLocal() != null) {
                router.route(scope, value.toString());
            }
        }

    }

}
