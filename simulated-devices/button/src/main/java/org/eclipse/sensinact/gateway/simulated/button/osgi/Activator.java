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
package org.eclipse.sensinact.gateway.simulated.button.osgi;

import org.eclipse.sensinact.gateway.common.bundle.AbstractActivator;
import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.generic.ExtModelConfiguration;
import org.eclipse.sensinact.gateway.generic.ExtModelInstanceBuilder;
import org.eclipse.sensinact.gateway.generic.local.LocalProtocolStackEndpoint;
import org.eclipse.sensinact.gateway.simulated.button.api.ButtonSetterItf;
import org.eclipse.sensinact.gateway.simulated.button.internal.ButtonAdapter;
import org.eclipse.sensinact.gateway.simulated.button.internal.ButtonGUI;
import org.eclipse.sensinact.gateway.simulated.button.internal.ButtonPacket;
import org.eclipse.sensinact.gateway.simulated.button.internal.ButtonSetter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

public class Activator extends AbstractActivator<Mediator> {
    private static final String GUI_ENABLED = "org.eclipse.sensinact.simulated.gui.enabled";
    private LocalProtocolStackEndpoint<ButtonPacket> connector;
    private ExtModelConfiguration manager;
    private ButtonSetterItf buttonPanel;
    private JFrame jFrame;
    private ServiceRegistration buttonRegistration;

    public void doStart() throws Exception {
        if (manager == null) {
            manager = new ExtModelInstanceBuilder(super.mediator, ButtonPacket.class).withStartAtInitializationTime(true).<ExtModelConfiguration>buildConfiguration("button-resource.xml", Collections.<String, String>emptyMap());
        }
        if (connector == null) {
            connector = new LocalProtocolStackEndpoint<ButtonPacket>(super.mediator);
        }
        connector.connect(manager);

        if (mediator.getContext().getProperty(GUI_ENABLED).equals("true")) {
            buttonPanel = new ButtonGUI(mediator, new ButtonAdapter(connector));
            jFrame = new JFrame();
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    createAndShowGUI((JLabel) buttonPanel);
                }
            });
        } else {
            buttonPanel = new ButtonSetter(new ButtonAdapter(connector));
            buttonRegistration = mediator.getContext().registerService(ButtonSetterItf.class, buttonPanel, null);
        }
    }

    public void doStop() throws Exception {
        connector.stop();
        buttonPanel.stop();
        if (jFrame != null) {
            jFrame.dispose();
        }
        if (buttonRegistration != null) {
            buttonRegistration.unregister();
        }
    }

    public Mediator doInstantiate(BundleContext context) {
        return new Mediator(context);
    }

    private void createAndShowGUI(JLabel gui) {
        jFrame.add(gui, BorderLayout.CENTER);
        jFrame.setSize(136, 156);
        jFrame.setTitle("button");
        jFrame.setVisible(true);
    }
}
