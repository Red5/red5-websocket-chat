package org.red5.demos.chat;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;
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

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean appStart(IScope scope) {
        log.info("Chat starting");
        return super.appStart(scope);
    }

    @Override
    public void appStop(IScope scope) {
        log.info("Chat stopping");
        super.appStop(scope);
    }

}
