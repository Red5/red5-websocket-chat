# Chat application

Red5 chat application example.

The example `index.html` defaults to using a WebSocket connection; SSE is also supported. Change the host and port entries to match your server as needed.

## Add the WebSocket filter servlet to webapps that require WebSocket support

```xml
    <filter>
        <filter-name>WebSocketFilter</filter-name>
        <filter-class>org.red5.net.websocket.server.WsFilter</filter-class>
        <async-supported>true</async-supported>
    </filter>
    <filter-mapping>
        <filter-name>WebSocketFilter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher>
    </filter-mapping>
```

## Add the SSE servlet to webapps that require SSE support

```xml
    <servlet>
        <servlet-name>sse</servlet-name>
        <servlet-class>org.red5.server.net.sse.SSEServlet</servlet-class>
        <load-on-startup>-1</load-on-startup>
        <async-supported>true</async-supported>
        <!-- use CORS internally if no other CORS is enabled -->
        <init-param>
            <param-name>cors.enabled</param-name>
            <param-value>false</param-value>
        </init-param>
    </servlet>   
    <servlet-mapping>
        <servlet-name>sse</servlet-name>
        <url-pattern>/events</url-pattern>
        <url-pattern>/events/*</url-pattern>
    </servlet-mapping>
```

## Build and Deploy

Build the application from the command line with

```sh
mvn package
```

If you are using Red5 Pro, you will need to modify the WebSocket connection URL to include the appropriate capabilities parameter:

```javascript
    /*
        if we're hitting a red5pro server, ensure the port is 5080 and wsonly is true (for websocket only mode)
        ?capabilities=1  : WebSocket Connection only
        ?capabilities=3  : RTCConnection
    */
    var socket = new WebSocket(protocol + '://' + window.location.hostname + ':' + port + '/chat/?capabilities=1', 'chat');
    // std red5
    //var socket = new WebSocket(protocol + '://' + window.location.hostname + ':' + port + '/chat', 'chat');
```

Deploy your application by copying the war file into your `red5/webapps` directory. If the war file does not deploy withing a few minutes, this may indicate the war deployer bean is not created or running; a work-around is to expand the war contents into the webapps directory manually and restart Red5.

After deploy is complete, go to http://localhost:5080/chat/ in your browser (open two tabs if you want to chat back and forth on the same computer).

## Pre-compiled WAR

You can find [compiled artifacts via Maven](http://mvnrepository.com/artifact/org.red5.demos/chat)

[Direct Download](https://oss.sonatype.org/content/repositories/releases/org/red5/demos/chat/2.0.23/chat-2.0.23.war)
