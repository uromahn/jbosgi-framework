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
import static org.jboss.osgi.framework.internal.FrameworkMessages.MESSAGES;
import static org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION;
import static org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT;
import static org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT_BOOT;
import static org.osgi.framework.Constants.FRAMEWORK_BUNDLE_PARENT_EXT;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jboss.modules.filter.MultiplePathFilterBuilder;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.osgi.framework.spi.AbstractIntegrationService;
import org.jboss.osgi.framework.spi.FrameworkBuilder;
import org.jboss.osgi.framework.spi.IntegrationServices;
import org.jboss.osgi.framework.spi.SystemPathsPlugin;
import org.jboss.osgi.metadata.spi.ElementParser;

/**
 * A plugin manages the Framework's system packages.
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Aug-2009
 */
final class DefaultSystemPathsPlugin extends AbstractIntegrationService<SystemPathsPlugin> implements SystemPathsPlugin {

    private final FrameworkBuilder frameworkBuilder;
    // The derived combination of all system packages
    private Set<String> systemPackages = new LinkedHashSet<String>();
    // The boot delegation packages
    private Set<String> bootDelegationPackages = new LinkedHashSet<String>();
    // The framework packages
    private Set<String> frameworkPackages = new LinkedHashSet<String>();

    private Set<String> cachedBootDelegationPaths;
    private PathFilter cachedBootDelegationFilter;
    private Set<String> cachedFrameworkPaths;
    private PathFilter cachedFrameworkFilter;
    private Set<String> cachedSystemPaths;
    private PathFilter cachedSystemFilter;

    DefaultSystemPathsPlugin(FrameworkBuilder frameworkBuilder) {
        super(IntegrationServices.SYSTEM_PATHS_PLUGIN);
        this.frameworkBuilder = frameworkBuilder;
    }

    @Override
    protected void addServiceDependencies(ServiceBuilder<SystemPathsPlugin> builder) {
        builder.setInitialMode(Mode.ON_DEMAND);
    }

    @Override
    public void start(StartContext context) throws StartException {

        // Initialize the framework packages
        frameworkPackages.addAll(Arrays.asList(SystemPathsPlugin.DEFAULT_FRAMEWORK_PACKAGES));

        String systemPackagesProp = (String) frameworkBuilder.getProperty(FRAMEWORK_SYSTEMPACKAGES);
        if (systemPackagesProp != null) {
            systemPackages.addAll(packagesAsList(systemPackagesProp));
        } else {
            // The default system packages
            systemPackages.addAll(Arrays.asList(SystemPathsPlugin.DEFAULT_SYSTEM_PACKAGES));
            systemPackages.addAll(frameworkPackages);
        }

        // Add the extra system packages
        String extraPackages = (String) frameworkBuilder.getProperty(FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        if (extraPackages != null)
            systemPackages.addAll(packagesAsList(extraPackages));

        // Initialize the boot delegation package names
        String bootDelegationProp = (String) frameworkBuilder.getProperty(FRAMEWORK_BOOTDELEGATION);
        if (bootDelegationProp != null) {
            String[] packageNames = bootDelegationProp.split(",");
            for (String packageName : packageNames) {
                bootDelegationPackages.add(packageName);
            }
        } else {
            bootDelegationPackages.add("sun.*");
            bootDelegationPackages.add("com.sun.*");
        }
    }

    @Override
    public SystemPathsPlugin getValue() {
        return this;
    }

    @Override
    public Set<String> getBootDelegationPackages() {
        assertInitialized();
        return Collections.unmodifiableSet(bootDelegationPackages);
    }

    @Override
    public PathFilter getBootDelegationFilter() {
        assertInitialized();
        if (cachedBootDelegationFilter == null) {
            MultiplePathFilterBuilder builder = PathFilters.multiplePathFilterBuilder(false);
            for (String packageName : getBootDelegationPackages()) {
                if (packageName.equals("*")) {
                    if (doFrameworkPackageDelegation()) {
                        builder.addFilter(PathFilters.acceptAll(), true);
                    } else {
                        builder.addFilter(PathFilters.all(PathFilters.acceptAll(), PathFilters.not(getFrameworkFilter())), true);
                    }
                } else if (packageName.endsWith(".*")) {
                    packageName = packageName.substring(0, packageName.length() - 2);
                    builder.addFilter(PathFilters.isChildOf(packageName.replace('.', '/')), true);
                } else {
                    builder.addFilter(PathFilters.is(packageName.replace('.', '/')), true);
                }
            }
            cachedBootDelegationFilter = builder.create();
            LOGGER.tracef("BootDelegationFilter: %s", cachedBootDelegationFilter);
        }
        return cachedBootDelegationFilter;
    }

    @Override
    public Set<String> getBootDelegationPaths() {
        assertInitialized();
        if (cachedBootDelegationPaths == null) {
            Set<String> result = new LinkedHashSet<String>();
            boolean hasBootDelegationWildcards = false;
            for (String packageName : getBootDelegationPackages()) {
                if (packageName.endsWith("*")) {
                    hasBootDelegationWildcards = true;
                    break;
                }
            }
            if (hasBootDelegationWildcards == true) {
                PathFilter bootDelegationFilter = getBootDelegationFilter();
                for (String path : JDKPaths.JDK) {
                    if (bootDelegationFilter.accept(path)) {
                        result.add(path);
                    }
                }
            } else {
                for (String packageName : getBootDelegationPackages()) {
                    result.add(packageName.replace('.', '/'));
                }
            }
            cachedBootDelegationPaths = Collections.unmodifiableSet(result);
            LOGGER.tracef("BootDelegationPaths: %s", cachedBootDelegationPaths);
        }
        return cachedBootDelegationPaths;
    }

    @Override
    public Set<String> getSystemPackages() {
        assertInitialized();
        return Collections.unmodifiableSet(systemPackages);
    }

    @Override
    public PathFilter getSystemFilter() {
        assertInitialized();
        if (cachedSystemFilter == null) {
            MultiplePathFilterBuilder builder = PathFilters.multiplePathFilterBuilder(false);
            Set<String> paths = new LinkedHashSet<String>();
            for (String packageSpec : getSystemPackages()) {
                int index = packageSpec.indexOf(';');
                if (index > 0) {
                    packageSpec = packageSpec.substring(0, index);
                }
                String path = packageSpec.replace('.', '/');
                paths.add(path);
            }
            builder.addFilter(PathFilters.in(paths), true);
            cachedSystemFilter = builder.create();
            LOGGER.debugf("SystemFilter: %s", cachedSystemFilter);
        }
        return cachedSystemFilter;
    }

    @Override
    public Set<String> getSystemPaths() {
        assertInitialized();
        if (cachedSystemPaths == null) {
            Set<String> result = new LinkedHashSet<String>();
            for (String packageSpec : getSystemPackages()) {
                int index = packageSpec.indexOf(';');
                if (index > 0) {
                    packageSpec = packageSpec.substring(0, index);
                }
                String path = packageSpec.replace('.', '/');
                result.add(path);
            }
            cachedSystemPaths = Collections.unmodifiableSet(result);
            LOGGER.debugf("SystemPaths: %s", cachedSystemPaths);
        }
        return cachedSystemPaths;
    }

    private Set<String> getFrameworkPackages() {
        return Collections.unmodifiableSet(frameworkPackages);
    }

    private Set<String> getFrameworkPaths() {
        if (cachedFrameworkPaths == null) {
            Set<String> paths = new LinkedHashSet<String>();
            for (String packageSpec : getFrameworkPackages()) {
                int index = packageSpec.indexOf(';');
                if (index > 0) {
                    packageSpec = packageSpec.substring(0, index);
                }
                paths.add(packageSpec.replace('.', '/'));
            }
            cachedFrameworkPaths = Collections.unmodifiableSet(paths);
            LOGGER.debugf("FrameworkPaths: %s", cachedFrameworkPaths);
        }
        return cachedFrameworkPaths;
    }

    private PathFilter getFrameworkFilter() {
        assertInitialized();
        if (cachedFrameworkFilter == null) {
            cachedFrameworkFilter = PathFilters.in(getFrameworkPaths());
            LOGGER.debugf("FrameworkFilter: %s", cachedFrameworkFilter);
        }
        return cachedFrameworkFilter;
    }

    private boolean doFrameworkPackageDelegation() {
        String property = (String) frameworkBuilder.getProperty(FRAMEWORK_BUNDLE_PARENT);
        if (property == null) {
            property = FRAMEWORK_BUNDLE_PARENT_BOOT;
        }
        boolean allBootDelegation = getBootDelegationPackages().contains("*");
        return !(allBootDelegation && (FRAMEWORK_BUNDLE_PARENT_BOOT.equals(property) || FRAMEWORK_BUNDLE_PARENT_EXT.equals(property)));
    }

    private List<String> packagesAsList(String sysPackages) {
    	return ElementParser.parseDelimitedString(sysPackages, ',');
    }

    private void assertInitialized() {
        if (systemPackages.isEmpty())
            throw MESSAGES.illegalStateSystemPathsNotInitialized();
    }
}