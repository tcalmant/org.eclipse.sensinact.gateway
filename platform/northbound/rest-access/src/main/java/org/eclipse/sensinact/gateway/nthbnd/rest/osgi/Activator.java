/*
 * Copyright (c) 2017 CEA.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    CEA - initial API and implementation
 */
package org.eclipse.sensinact.gateway.nthbnd.rest.osgi;

import org.apache.felix.http.api.ExtHttpService;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.eclipse.sensinact.gateway.common.bundle.AbstractActivator;
import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.common.execution.Executable;
import org.eclipse.sensinact.gateway.nthbnd.endpoint.NorthboundMediator;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.RestAccessConstants;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.http.CorsFilter;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.http.HttpEndpoint;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.http.HttpLoginEndpoint;
import org.eclipse.sensinact.gateway.nthbnd.rest.internal.ws.WebSocketConnectionFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.http.HttpContext;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @see AbstractActivator
 */
public class Activator extends AbstractActivator<NorthboundMediator> {
    private static final ClassLoader getJettyBundleClassLoader(BundleContext context) {
        Bundle[] bundles = context.getBundles();
        int index = 0;
        int length = bundles == null ? 0 : bundles.length;

        ClassLoader loader = null;

        for (; index < length; index++) {
            if ("org.apache.felix.http.jetty".equals(bundles[index].getSymbolicName())) {
                BundleWiring wiring = bundles[index].adapt(BundleWiring.class);
                loader = wiring.getClassLoader();
                break;
            }
        }
        return loader;
    }

    private CorsFilter corsFilter = null;
    private boolean corsHeader = false;

    /**
     * @inheritDoc
     * @see org.eclipse.sensinact.gateway.common.bundle.AbstractActivator#
     * doStart()
     */
    public void doStart() throws Exception {
        this.corsHeader = Boolean.valueOf((String) super.mediator.getProperty(RestAccessConstants.CORS_HEADER));

        mediator.attachOnServiceAppearing(ExtHttpService.class, null, new Executable<ExtHttpService, Void>() {
            /**
             * @inheritDoc
             *
             * @see org.eclipse.sensinact.gateway.common.execution.Executable#execute(java.lang.Object)
             */
            public Void execute(ExtHttpService service) {
                if (Activator.this.corsHeader) {
                    Activator.this.corsFilter = new CorsFilter(mediator);
                    try {
                        service.registerFilter(corsFilter, ".*", null, 0, null);
                        Activator.this.mediator.info("CORS filter registered");
                    } catch (Exception e) {
                        mediator.error(e);
                    }
                }
                Dictionary<String, Object> params = new Hashtable<String, Object>();
                params.put(Mediator.class.getCanonicalName(), Activator.this.mediator);
                try {
                    HttpContext context = service.createDefaultHttpContext();
                    service.registerServlet(RestAccessConstants.LOGIN_ENDPOINT, new HttpLoginEndpoint(mediator), params, context);
                    Activator.this.mediator.info(String.format("%s servlet registered", RestAccessConstants.LOGIN_ENDPOINT));
                    params = new Hashtable<String, Object>();
                    params.put(Mediator.class.getCanonicalName(), Activator.this.mediator);

                    context = service.createDefaultHttpContext();
                    service.registerServlet(RestAccessConstants.HTTP_ROOT, new HttpEndpoint(mediator), params, context);
                    Activator.this.mediator.info(String.format("%s servlet registered", RestAccessConstants.HTTP_ROOT));

                    params = new Hashtable<String, Object>();
                    params.put(Mediator.class.getCanonicalName(), Activator.this.mediator);

                    final WebSocketConnectionFactory sessionPool = new WebSocketConnectionFactory(Activator.this.mediator);
                    //define the current thread classloader to avoid ServiceLoader error
                    ClassLoader current = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(Activator.getJettyBundleClassLoader(mediator.getContext()));
                    try {
                        service.registerServlet(RestAccessConstants.WS_ROOT, new WebSocketServlet() {
                            @Override
                            public void configure(WebSocketServletFactory factory) {
                                factory.getPolicy().setIdleTimeout(1000 * 3600);
                                factory.setCreator(sessionPool);
                            }

                            ;
                        }, params, context);

                    } finally {
                        Thread.currentThread().setContextClassLoader(current);
                    }
                    mediator.info(String.format("%s servlet registered", RestAccessConstants.WS_ROOT));
                } catch (Exception e) {
                    mediator.error(e);
                }
                return null;
            }
        });
    }

    /**
     * @inheritDoc
     * @see org.eclipse.sensinact.gateway.common.bundle.AbstractActivator#
     * doStop()
     */
    public void doStop() throws Exception {
        mediator.callServices(ExtHttpService.class, new Executable<ExtHttpService, Void>() {
            @Override
            public Void execute(ExtHttpService service) throws Exception {
                try {
                    service.unregister(RestAccessConstants.HTTP_ROOT);

                } catch (Exception e) {
                    mediator.error(e);
                }
                try {
                    service.unregister(RestAccessConstants.WS_ROOT);

                } catch (Exception e) {
                    mediator.error(e);
                }
                try {
                    service.unregisterFilter(corsFilter);

                } catch (Exception e) {
                    mediator.error(e);
                }
                return null;
            }
        });
    }

    /**
     * @inheritDoc
     * @see AbstractActivator#
     * doInstantiate(org.osgi.framework.BundleContext, int, java.io.FileOutputStream)
     */
    @Override
    public NorthboundMediator doInstantiate(BundleContext context) {
        NorthboundMediator mediator = new NorthboundMediator(context);
        return mediator;
    }
}
