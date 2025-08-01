# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Red5 WebSocket chat application demo that demonstrates real-time chat functionality using WebSockets with a Red5 server. The application provides both WebSocket and Flash/Shared Object communication channels.

## Build Commands

- **Build**: `mvn package` - Builds the application and creates a WAR file
- **Default goal**: `package` - Maven's default goal is set to package

## Architecture

### Core Components

1. **Application.java** (`src/main/java/org/red5/demos/chat/Application.java`):
   - Main Red5 application entry point extending `MultiThreadedApplicationAdapter`
   - Handles application lifecycle (start/stop)
   - Spring-aware component

2. **WebSocketChatDataListener.java** (`src/main/java/org/red5/demos/chat/WebSocketChatDataListener.java`):
   - Primary WebSocket message handler extending `WebSocketDataListener`
   - Manages WebSocket connections in a HashSet
   - Routes messages between WebSocket clients and shared objects
   - Handles both JSON and plain text messages
   - Uses "chat" protocol identifier

3. **Router.java** (`src/main/java/org/red5/demos/chat/Router.java`):
   - Bridges WebSocket messages to Red5 shared objects
   - Enables cross-platform communication (WebSocket ↔ Flash clients)

### Configuration

- **Spring Configuration**: `src/main/webapp/WEB-INF/red5-web.xml`
   - Defines WebSocket scope and listeners
   - Wires components together (Application, Router, WebSocketChatDataListener)
- **Properties**: `src/main/webapp/WEB-INF/red5-web.properties`
- **Web Configuration**: Requires WebSocketFilter servlet configuration for WebSocket support

### Message Flow

1. WebSocket clients connect and are added to connections set
2. Messages received via `onWSMessage()` are:
   - Parsed (JSON detection and parsing)
   - Broadcast to all WebSocket connections on same path via `sendToAll()`
   - Routed to shared objects via Router for Flash client compatibility
3. Path-based routing enables multiple chat rooms

## Deployment

The application packages as a WAR file that should be deployed to the Red5 server's webapps directory. Default port is 5080.

## Dependencies

- Red5 Server 2.0.19
- Spring Framework 6.2.8 (beans, context, context-support)
- Apache MINA 2.0.23
- Gson 2.13.1 for JSON parsing
- Java 17 (updated from Java 11)

## Key Design Patterns

- Uses Spring dependency injection for component wiring
- Dual communication channels (WebSocket + Shared Objects) for cross-platform support
- Path-based message routing for multi-room functionality
- Protocol-based message filtering