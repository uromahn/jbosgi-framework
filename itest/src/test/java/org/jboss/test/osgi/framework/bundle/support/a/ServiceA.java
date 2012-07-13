/*
 * #%L
 * JBossOSGi Framework iTest
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
package org.jboss.test.osgi.framework.bundle.support.a;

import org.jboss.test.osgi.framework.bundle.support.b.ServiceB;
import org.jboss.test.osgi.framework.bundle.support.x.ServiceX;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

/**
 * ServiceA has a dependency on ServiceB, both have a dependency on SomePojo
 * 
 * @author thomas.diesler@jboss.com
 * @since 02-Mar-2009
 */
public class ServiceA {

    ServiceA(BundleContext context) {
        ServiceTracker tracker = new ServiceTracker(context, ServiceB.class.getName(), null) {

            @Override
            public Object addingService(ServiceReference sref) {
                ServiceB serviceB = (ServiceB) super.addingService(sref);
                serviceB.doStuffInB(new ServiceX("hello"));
                return serviceB;
            }
        };
        tracker.open();
    }
}
