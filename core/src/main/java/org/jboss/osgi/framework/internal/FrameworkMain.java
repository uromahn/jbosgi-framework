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

import java.util.Map;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.osgi.framework.spi.FrameworkBuilderFactory;
import org.jboss.osgi.framework.spi.FrameworkBuilder;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * An impementation of an OSGi {@link FrameworkFactory}
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Aug-2009
 */
public class FrameworkMain implements FrameworkFactory {

    /**
     * The main entry point to the Framework
     */
    public static void main(String[] args) throws Exception {
        FrameworkMain factory = new FrameworkMain();
        Framework framework = factory.newFramework(null);
        framework.start();
    }

    @Override
    public Framework newFramework(Map<String, String> props) {
        FrameworkBuilder builder = FrameworkBuilderFactory.create(props, Mode.ACTIVE);
        return builder.createFramework();
    }
}