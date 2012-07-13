/*
 * #%L
 * JBossOSGi Framework Core
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
package org.jboss.osgi.framework.internal;

import org.osgi.framework.BundleContext;

/**
 * The system {@link BundleContext}.
 *
 * @author thomas.diesler@jboss.com
 * @since 29-Jun-2010
 */
final class SystemBundleContext extends AbstractBundleContext {

    SystemBundleContext(SystemBundleState bundle) {
        super(bundle);
    }

    @Override
    SystemBundleState getBundleState() {
        return (SystemBundleState) super.getBundleState();
    }
}
