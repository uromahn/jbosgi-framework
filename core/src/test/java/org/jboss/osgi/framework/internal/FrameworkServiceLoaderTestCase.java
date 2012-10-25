package org.jboss.osgi.framework.internal;
/*
 * #%L
 * JBossOSGi Framework
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Iterator;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.junit.Test;

/**
 * Test whether we can load a service through the framework module
 *
 * @author Thomas.Diesler@jboss.com
 * @since 10-Jan-2011
 */
public class FrameworkServiceLoaderTestCase extends AbstractFrameworkTest {

    @Test
    public void testServiceLoaderFails() throws Exception {

        // The {@link ModularURLStreamHandlerFactory} follows a pattern similar to this.
        SystemBundleState systemBundle = getBundleManager().getSystemBundle();
        ModuleManagerPlugin plugin = getFrameworkState().getModuleManagerPlugin();
        Module frameworkModule = plugin.loadModule(systemBundle.getModuleIdentifier());
        assertNotNull("Framework module not null", frameworkModule);

        // Test resource access
        ModuleClassLoader classLoader = frameworkModule.getClassLoader();
        URL resource = classLoader.getResource("META-INF/services/" + URLStreamHandlerFactory.class.getName());
        assertNull("Resource URL null", resource);

        // Test ServiceLoader access
        Iterator<URLStreamHandlerFactory> iterator = frameworkModule.loadService(URLStreamHandlerFactory.class).iterator();
        assertFalse("No more URLStreamHandlerFactory", iterator.hasNext());
    }
}
