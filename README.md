Chat application
================

Red5 WebSocket chat application example.

The example <i>index.html</i> defaults to using a WebSocket connection to localhost on port 8081. This means that the localhost and port must be configured in the <i>red5/conf/jee-container.xml</i> file.

```xml
<bean id="webSocketTransport" class="org.red5.net.websocket.WebSocketTransport">
    <property name="addresses">
        <list>
            <value>localhost:8081</value>
        </list>
    </property>
</bean>
```

Build the application from the command line with

```bash
mvn package
```

Deploy your application by copying the war file into your <i>red5/webapps</i> directory.

After deploy is complete, go to http://localhost:5080/chat/ in your browser (open two tabs if you want to chat back and forth on the same computer).

Pre-compiled WAR
----------------
You can find [compiled artifacts via Maven](http://mvnrepository.com/artifact/org.red5.demos/chat)

[Direct Download](https://oss.sonatype.org/content/repositories/releases/org/red5/demos/chat/1.1/chat-1.1.war)
