/*********************************************************************
* Copyright (c) 2021 Kentyou and others
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/

package org.eclipse.sensinact.gateway.core.security.test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.sensinact.gateway.common.primitive.Describable;
import org.eclipse.sensinact.gateway.core.Core;
import org.eclipse.sensinact.gateway.core.ServiceProvider;
import org.eclipse.sensinact.gateway.core.Session;
import org.eclipse.sensinact.gateway.core.security.Authentication;
import org.eclipse.sensinact.gateway.core.security.Credentials;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.test.common.annotation.InjectService;
import org.osgi.test.junit5.context.BundleContextExtension;
import org.osgi.test.junit5.context.InstalledBundleExtension;
import org.osgi.test.junit5.service.ServiceExtension;

/**
 *
 * @author <a href="mailto:christophe.munilla@cea.fr">Christophe Munilla</a>
 */
@ExtendWith(BundleContextExtension.class)
@ExtendWith(InstalledBundleExtension.class)
@ExtendWith(ServiceExtension.class)
@Disabled
public class TestSecurityPattern{
	// ********************************************************************//
	// NESTED DECLARATIONS //
	// ********************************************************************//

	// ********************************************************************//
	// ABSTRACT DECLARATIONS //
	// ********************************************************************//

	// ********************************************************************//
	// STATIC DECLARATIONS //
	// ********************************************************************//

	private static final String SLIDERS_DEFAULT = "[\"slider01\",\"slider02\",\"slider11\"]";
	private static final String SLIDERS_PROP = "org.eclipse.sensinact.simulated.sliders";
	private static final String GUI_ENABLED = "org.eclipse.sensinact.simulated.gui.enabled";

	// ********************************************************************//
	// INSTANCE DECLARATIONS //
	// ********************************************************************//

	Method getDescription = null;
	public TestSecurityPattern() throws Exception {
		super();
		getDescription = Describable.class.getDeclaredMethod("getDescription");
	}

	public boolean isExcluded(String fileName) {
		switch(fileName) {
		case "org.apache.felix.framework.security.jar":
			return true;
		default:
			break;
		}
		return false;
	}

	protected void doInit(Map<String, String> configuration) {
		configuration.put("org.osgi.framework.system.packages.extra",
			"org.eclipse.sensinact.gateway.test," + 
			"com.sun.net.httpserver," + 
			"javax.mail," + 
			"javax.mail.internet," + 
			"javax.microedition.io," +
			"javax.management.modelmbean," + 
			"javax.management.remote,"	+
			"javax.persistence," +
			"junit.framework," + 
			"junit.textui," + 
			"org.w3c.dom," + 
			"org.xml.sax," + 
			"org.xml.sax.helpers," + 
			"sun.misc,"+ 
			"sun.security.action");

		configuration.put("org.eclipse.sensinact.simulated.gui.enabled", "false");

		configuration.put("org.eclipse.sensinact.gateway.security.jks.filename", "target/felix/bundle/keystore.jks");
		configuration.put("org.eclipse.sensinact.gateway.security.jks.password", "sensiNact_team");

		configuration.put("org.eclipse.sensinact.gateway.security.database", 
				new File("../sensinact-security-core/src/test/resources/sensinact.sqlite").getAbsolutePath());

    	configuration.put("felix.auto.start.1",  
           "file:target/felix/bundle/org.osgi.service.component.jar "+ 
           "file:target/felix/bundle/org.osgi.service.cm.jar "+  
           "file:target/felix/bundle/org.osgi.service.metatype.jar "+  
           "file:target/felix/bundle/org.osgi.namespace.extender.jar "+  
           "file:target/felix/bundle/org.osgi.util.promise.jar "+  
           "file:target/felix/bundle/org.osgi.util.function.jar "+  
           "file:target/felix/bundle/org.osgi.util.pushstream.jar "+
           "file:target/felix/bundle/org.osgi.service.log.jar "  +
           "file:target/felix/bundle/org.apache.felix.log.jar " + 
           "file:target/felix/bundle/org.apache.felix.scr.jar " +
           "file:target/felix/bundle/org.apache.felix.fileinstall.jar " +
           "file:target/felix/bundle/org.apache.felix.configadmin.jar " + 
           "file:target/felix/bundle/org.apache.felix.framework.security.jar ");
    	
        configuration.put("felix.auto.install.2",  
    	    "file:target/felix/bundle/slf4j-api.jar "+
			"file:target/felix/bundle/sensinact-utils.jar "+ 
			"file:target/felix/bundle/sensinact-datastore-api.jar "+
			"file:target/felix/bundle/sensinact-sqlite-connector.jar "+
			"file:target/felix/bundle/sensinact-common.jar "+
			"file:target/felix/bundle/sensinact-framework-extension.jar "+
	    	"file:target/felix/bundle/sensinact-security-keybuilder.jar "+	
			"file:target/felix/bundle/sensinact-security-core.jar "+
	    	"file:target/felix/bundle/slf4j-simple.jar");

		configuration.put("felix.auto.start.2", 
			"file:target/felix/bundle/sensinact-test-configuration.jar "+
			"file:target/felix/bundle/sensinact-signature-validator.jar " +
			"file:target/felix/bundle/org.apache.felix.http.servlet-api.jar " +
			"file:target/felix/bundle/org.apache.felix.http.api.jar " +
			"file:target/felix/bundle/org.apache.felix.http.jetty.jar " +
			"file:target/felix/bundle/org.apache.aries.javax.jax.rs-api.jar");

		configuration.put("felix.auto.start.3",
			"file:target/felix/bundle/sensinact-core.jar " + 
		    "file:target/felix/bundle/sensinact-generic.jar ");

		configuration.put("felix.auto.start.4", "file:target/felix/bundle/slider.jar ");       
		configuration.put(SLIDERS_PROP, SLIDERS_DEFAULT);

		configuration.put("felix.log.level", "4");
	}

	@Test
	public void testSecurityAccessWithPattern(@InjectService Core core) throws Throwable {
		// slider[0-9]{2} - authenticated access level
		// slider[0-9]{2}/admin - admin authenticated access level
		// cea user is admin on slider[0-9]{2}

		// slider0[0-9] - authenticated access level
		// slider0[0-9]/cursor - authenticated access level
		// fake user is authenticated on slider0[0-9]

		// slider1[0-9] - authenticated access level
		// slider1[0-9]/cursor - authenticated access level
		// fake2 user is authenticated on slider1[0-9]

		Session session = core.getAnonymousSession();
		assertNotNull(session);

		Set<ServiceProvider> providers = session.serviceProviders();
		System.out.println("====================================>>>>>");
		System.out.println(providers);
		System.out.println("====================================>>>>>");
		assertTrue(providers.isEmpty());

		// ******************************************************
		// admin
		// the admin user is suppose to see every thing
		// service providers and services
		Authentication<Credentials> credentials = new Credentials("cea", "sensiNact_team");

		session = core.getSession(credentials);

		assertNotNull(session);

		providers = session.serviceProviders();
		assertEquals(3, providers.size());
		Iterator<ServiceProvider> iterator = providers.iterator();

		while (iterator.hasNext()) {

			ServiceProvider serviceProvider = iterator.next();
			assertEquals(2, serviceProvider.getServices().size());
			System.out.println(serviceProvider.getDescription().getJSON());
		}

		// *************************************
		// fake
		// the fake user is suppose to see only two service providers
		// and only the cursor service for each one
		credentials = new Credentials("fake", "fake");

		session = core.getSession(credentials);

		assertNotNull(session);
		
		assertNotNull(session);

		providers = session.serviceProviders();

		assertEquals(2, providers.size());
		iterator = providers.iterator();

		while (iterator.hasNext()) {
			ServiceProvider serviceProvider = iterator.next();
			assertEquals(1, serviceProvider.getServices().size());
			System.out.println(serviceProvider.getDescription().getJSON());
		}

		// ***************************************
		// fake2
		// the fake2 user is suppose to see only one service provider
		// and only its cursor service
		credentials = new Credentials("fake2", "fake2");

		session = core.getSession(credentials);

		assertNotNull(session);
		assertNotNull(session);

		providers = session.serviceProviders();
		assertEquals(1, providers.size());
		iterator = providers.iterator();

		while (iterator.hasNext()) {
			ServiceProvider serviceProvider = iterator.next();
			assertEquals(1, serviceProvider.getServices().size());
			System.out.println(serviceProvider.getDescription().getJSON());
		}
	}
}
