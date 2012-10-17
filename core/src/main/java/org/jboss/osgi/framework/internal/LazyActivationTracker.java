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

import static org.jboss.osgi.framework.internal.FrameworkLogger.LOGGER;

import java.util.Stack;

/**
 * A {@link ThreadLocal} of bundles that need to get started caused by lazy activation.
 *
 * @author thomas.diesler@jboss.com
 * @since 02-May-2011
 */
final class LazyActivationTracker {

    private final static ThreadLocal<Stack<HostBundleState>> stackAssociation = new ThreadLocal<Stack<HostBundleState>>();
    private final static ThreadLocal<HostBundleState> initiatorAssociation = new ThreadLocal<HostBundleState>();

    static void startTracking(HostBundleState hostBundle, String className) {
        LOGGER.tracef("startTracking %s from: %s", className, hostBundle);
        initiatorAssociation.set(hostBundle);
    }

    static void processLoadedClass(Class<?> loadedClass) {
        assert initiatorAssociation.get() != null : "No activation initiator";
        LOGGER.tracef("processLoadedClass: %s", loadedClass.getName());
        processActivationStack();
    }

    static void preDefineClass(HostBundleState hostBundle, String className) {
        LOGGER.tracef("preDefineClass %s from: %s", className, hostBundle);
        addDefinedClass(hostBundle, className);
    }

    static void postDefineClass(AbstractBundleState hostBundle, Class<?> definedClass) {
        LOGGER.tracef("postDefineClass %s from: %s", definedClass.getName(), hostBundle);
        if (initiatorAssociation.get() == null) {
            processActivationStack();
        }
    }

    static void stopTracking(AbstractBundleState hostBundle, String className) {
        LOGGER.tracef("stopTracking %s from: %s", className, hostBundle);
        initiatorAssociation.remove();
        stackAssociation.remove();
    }

    private static void addDefinedClass(HostBundleState hostBundle, String className) {
        if (hostBundle.awaitLazyActivation() && hostBundle.isAlreadyStarting() == false) {
            Stack<HostBundleState> stack = stackAssociation.get();
            if (stack == null) {
                stack = new Stack<HostBundleState>();
                stackAssociation.set(stack);
            }
            if (stack.contains(hostBundle) == false) {
                LOGGER.tracef("addDefinedClass %s from: %s", className, hostBundle);
                stack.push(hostBundle);
            }
        }
    }

    private static void processActivationStack() {
        Stack<HostBundleState> stack = stackAssociation.get();
        if (stack != null) {
            LOGGER.tracef("processActivationStack: %s", stack);
            while (stack.isEmpty() == false) {
                HostBundleState hostBundle = stack.pop();
                if (hostBundle.awaitLazyActivation()) {
                    try {
                        hostBundle.activateLazily();
                    } catch (Throwable th) {
                        LOGGER.errorCannotActivateBundleLazily(th, hostBundle);
                    }
                }
            }
        }
    }
}
