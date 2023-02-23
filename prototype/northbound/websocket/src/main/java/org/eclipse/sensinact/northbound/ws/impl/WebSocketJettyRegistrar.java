/*********************************************************************
* Copyright (c) 2022 Contributors to the Eclipse Foundation.
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

import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.sensinact.prototype.SensiNactSessionManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import jakarta.servlet.Servlet;

@Component(service = Servlet.class, scope = ServiceScope.PROTOTYPE)
@HttpWhiteboardServletPattern("/ws")
@HttpWhiteboardServletAsyncSupported
public class WebSocketJettyRegistrar extends JettyWebSocketServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    SensiNactSessionManager sessionManager;

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        System.out.println("***************** CONFIGURE");
        factory.register(WebSocketEndpoint.class);
    }
}
