/*
 * RED5 Open Source Flash Server - https://github.com/red5
 * 
 * Copyright 2006-2015 by respective authors (see below). All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.red5.net.websocket;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.red5.net.websocket.model.WSMessage;
import org.red5.server.plugin.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocketHandler
 * 
 * <pre>
 * IoHandlerAdapter for webSocket
 * </pre>
 * 
 * @author Toda Takahiko
 * @author Paul Gregoire
 */
public class WebSocketHandler extends IoHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        log.trace("Message received on session: {}  {}", session.getId(), message);
        if (message instanceof WSMessage) {
            WebSocketConnection conn = (WebSocketConnection) session.getAttribute(Constants.CONNECTION);
            if (conn != null) {
                conn.receive((WSMessage) message);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        log.trace("Message sent on session: {}  {}", session.getId(), String.valueOf(message));
        log.trace("Session read: {} write: {}", session.getReadBytes(), session.getWrittenBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        log.trace("Session closed");
        // remove connection from scope
        WebSocketConnection conn = (WebSocketConnection) session.removeAttribute(Constants.CONNECTION);
        if (conn != null) {
            // remove from the manager
            WebSocketPlugin plugin = (WebSocketPlugin) PluginRegistry.getPlugin("WebSocketPlugin");
            if (plugin != null) {
                String path = conn.getPath();
                if (path != null) {
                    WebSocketScopeManager manager = (WebSocketScopeManager) session.removeAttribute(Constants.MANAGER);
                    if (manager == null) {
                        manager = plugin.getManager(path);
                    }
                    if (manager != null) {
                        manager.removeConnection(conn);
                    } else {
                        log.debug("WebSocket manager was not found");
                    }
                } else {
                    log.debug("WebSocket connection path was null");
                }
            } else {
                log.debug("WebSocket plugin was not found");
            }
        } else {
            log.debug("WebSocket connection was null");
        }
        super.sessionClosed(session);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        log.error("exception", cause);
    }

}
