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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.core.WebSocketComponents;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;
import org.eclipse.jetty.websocket.server.JettyWebSocketServerContainer;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.sensinact.prototype.SensiNactSessionManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletAsyncSupported;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

@Component(service = Servlet.class, scope = ServiceScope.PROTOTYPE)
@HttpWhiteboardServletPattern("/ws")
@HttpWhiteboardServletAsyncSupported
public class WebSocketJettyRegistrar extends JettyWebSocketServlet {

    private static final long serialVersionUID = 1L;

    @Reference
    SensiNactSessionManager sessionManager;

    private final AtomicBoolean initCalled = new AtomicBoolean(false);
    private final CountDownLatch initComplete = new CountDownLatch(1);

    @Override
    public void init() throws ServletException {
        // Deliberately block initialization. This is needed as there is
        // no Jetty context on the thread until a request occurs
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        // Check to see if init needs calling
        if (initCalled.getAndSet(true)) {
            // Someone else is responsible, wait until they are done
            try {
                initComplete.await();
            } catch (InterruptedException e) {
                throw new ServletException(e);
            }
        } else {
            // Initialise now
            try {
                ServletContext servletContext = getServletContext();
                ServletContextHandler contextHandler = ServletContextHandler.getServletContextHandler(servletContext,
                        "Jetty WebSocket init");
                WebSocketServerComponents.ensureWebSocketComponents(contextHandler.getServer(), servletContext);
                JettyWebSocketServerContainer.ensureContainer(servletContext);
                super.init();
            } finally {
                // Tell other callers that they can stop waiting
                initComplete.countDown();
            }
        }
        // Normal service resumes
        super.service(req, res);
    }

    @Override
    protected void configure(JettyWebSocketServletFactory factory) {
        System.out.println("***************** CONFIGURE");
        factory.register(WebSocketEndpoint.class);
    }
}
