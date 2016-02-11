package org.red5.demos.chat;

import org.red5.logging.Red5LoggerFactory;
import org.red5.net.websocket.WebSocketPlugin;
import org.red5.net.websocket.WebSocketScopeManager;
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Main application entry point for the chat application.
 * 
 * @author Paul Gregoire
 */
public class Application extends MultiThreadedApplicationAdapter implements ApplicationContextAware {

    private static Logger log = Red5LoggerFactory.getLogger(Application.class, "chat");

    @SuppressWarnings("unused")
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean appStart(IScope scope) {
        log.info("Chat starting");
        // add our application to enable websocket support
        WebSocketScopeManager manager = ((WebSocketPlugin) PluginRegistry.getPlugin("WebSocketPlugin")).getManager();
        manager.addApplication(scope);
        return super.appStart(scope);
    }

    @Override
    public void appStop(IScope scope) {
        log.info("Chat stopping");
        // remove our app
        WebSocketScopeManager manager = ((WebSocketPlugin) PluginRegistry.getPlugin("WebSocketPlugin")).getManager();
        manager.removeApplication(scope);
        super.appStop(scope);
    }

}
