/*********************************************************************
* Copyright (c) 2023 Contributors to the Eclipse Foundation.
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   Kentyou - initial implementation
**********************************************************************/
package org.eclipse.sensinact.northbound.ws.impl;

import java.io.IOException;

import org.eclipse.sensinact.northbound.ws.dto.WsQueryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Websocket endpoint
 */
@ServerEndpoint("/ws")
public class WebSocketEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEndpoint.class);

    @OnOpen
    public void open(Session session, EndpointConfig conf) {
        logger.info("On open: " + session + " / " + conf);
    }

    @OnMessage
    public void onMessage(final Session session, final WsQueryDTO root) {
        try {
            logger.debug("Got query for URI={}", root.uri);
            session.getBasicRemote().sendText("Hello for " + root.uri);
        } catch (IOException e) {
            logger.error("Error replying to client");
        }
    }
}
