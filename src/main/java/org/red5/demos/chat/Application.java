package org.red5.demos.chat;

import org.red5.net.websocket.WebSocketPlugin;
import org.red5.net.websocket.WebSocketScopeManager;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Main application entry point for the chat application.
 * 
 * @author Paul Gregoire
 */
public class Application extends MultiThreadedApplicationAdapter implements ApplicationContextAware {

    // Updated SLF4J logger makes it difficult to use the logger with a context, it is currently buggy in Red5
    //private static Logger log = Red5LoggerFactory.getLogger(Application.class, "chat");
    private static Logger log = LoggerFactory.getLogger(Application.class); // use this for now instead

    @SuppressWarnings("unused")
    private ApplicationContext applicationContext;

    private Thread startupThread;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean appStart(IScope scope) {
        log.info("Chat starting on scope: {} path: {}", scope.getName(), scope.getContextPath());
        if (startupThread != null && startupThread.isAlive()) {
            log.warn("Startup thread is already running, skipping appStart");
            return false;
        }
        final Application app = this;
        startupThread = Thread.ofVirtual().start(new Runnable() {
            @Override
            public void run() {
                String path = scope.getContextPath();
                try {
                    while (!Red5.isPluginsReady()) {
                        log.info("Waiting for plugins to be ready...");
                        Thread.sleep(1000L);
                    }
                    log.info("Plugins are ready, proceeding with chat application startup");
                    do {
                        WebSocketPlugin webSocketPlugin = (WebSocketPlugin) PluginRegistry.getPlugin(WebSocketPlugin.NAME);
                        if (webSocketPlugin == null) {
                            log.warn("WebSocketPlugin is null, cannot start chat application");
                            Thread.sleep(100L);
                            continue;
                        }
                        WebSocketScopeManager webSocketScopeManager = webSocketPlugin.getManager(path);
                        if (webSocketScopeManager == null) {
                            log.warn("WebSocketScopeManager is null, cannot start chat application");
                            Thread.sleep(100L);
                            continue;
                        }
                        // Initialize the router
                        Router router = new Router();
                        router.setApp(app);
                        scope.setAttribute("router", router);
                        log.info("Router set in scope: {}", scope.getName());
                        // Register the WebSocketChatDataListener
                        WebSocketChatDataListener chatListener = new WebSocketChatDataListener();
                        chatListener.setRouter(router);
                        scope.setAttribute("chatListener", chatListener);
                        log.info("Chat listener set in scope: {}", scope.getName());
                        // Start the WebSocketChatDataListener
                        webSocketScopeManager.addListener(chatListener, path);
                        log.info("WebSocketChatDataListener added to WebSocket scope: {}", path);
                        break; // exit the loop if everything is set up correctly
                    } while(true);
                } catch (Exception e) {
                    log.error("Error occurred while starting chat application", e);
                }
            }
        });
        return super.appStart(scope);
    }

    @Override
    public void appStop(IScope scope) {
        log.info("Chat stopping");
        super.appStop(scope);
    }

    public void messageTransmit(String message) {
        log.info("Message transmitted: {}", message);
    }

}
