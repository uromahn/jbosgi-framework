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

import static org.jboss.osgi.framework.internal.FrameworkMessages.MESSAGES;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BundleLifecyclePlugin;
import org.jboss.osgi.framework.spi.FutureServiceValue;
import org.jboss.osgi.vfs.AbstractVFS;
import org.jboss.osgi.vfs.VFSUtils;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The base of all {@link BundleContext} implementations.
 *
 * @author thomas.diesler@jboss.com
 * @since 29-Jun-2010
 */
abstract class AbstractBundleContext implements BundleContext {

    private final AbstractBundleState bundleState;
    private boolean destroyed;

    AbstractBundleContext(AbstractBundleState bundleState) {
        assert bundleState != null : "Null bundleState";
        this.bundleState = bundleState;
    }

    /**
     * Assert that the given context is an instance of AbstractBundleContext
     */
    static AbstractBundleContext assertBundleContext(BundleContext context) {
        assert context != null : "Null context";
        assert context instanceof AbstractBundleContext : "Not an AbstractBundleContext: " + context;
        return (AbstractBundleContext) context;
    }

    void destroy() {
        destroyed = true;
    }

    AbstractBundleState getBundleState() {
        return bundleState;
    }

    BundleManagerPlugin getBundleManager() {
        return bundleState.getBundleManager();
    }

    FrameworkState getFrameworkState() {
        return bundleState.getFrameworkState();
    }

    @Override
    public String getProperty(String key) {
        checkValidBundleContext();
        getBundleManager().assertFrameworkCreated();
        Object value = getBundleManager().getProperty(key);
        return (value instanceof String ? (String) value : null);
    }

    @Override
    public Bundle getBundle() {
        checkValidBundleContext();
        return bundleState;
    }

    @Override
    public Bundle getBundle(String location) {
        checkValidBundleContext();
        return getBundleManager().getBundleByLocation(location);
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        return installBundleInternal(location, null);
    }

    @Override
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return installBundleInternal(location, input);
    }

    /*
     * This is the entry point for all bundle deployments.
     *
     * #1 Construct the {@link VirtualFile} from the given parameters
     *
     * #2 Create a Bundle {@link Deployment}
     *
     * #3 Deploy the Bundle through the {@link BundleInstallPlugin}
     *
     * The {@link BundleInstallPlugin} is the integration point for JBossAS.
     *
     * The {@link DefaultBundleInstallPlugin} simply delegates to {@link BundleManager#installBundle(ServiceTarget,Deployment)} In
     * JBossAS however, the {@link BundleInstallPlugin} delegates to the management API that feeds the Bundle deployment
     * through the DeploymentUnitProcessor chain.
     */
    private Bundle installBundleInternal(String location, InputStream input) throws BundleException {
        checkValidBundleContext();

        Deployment dep;
        VirtualFile rootFile = null;
        FrameworkState frameworkState = getFrameworkState();
        try {
            if (input != null) {
                try {
                    rootFile = AbstractVFS.toVirtualFile(input);
                } catch (IOException ex) {
                    throw MESSAGES.cannotObtainVirtualFile(ex);
                }
            }

            // Try location as URL
            if (rootFile == null) {
                try {
                    URL url = new URL(location);
                    rootFile = AbstractVFS.toVirtualFile(url.openStream());
                } catch (IOException ex) {
                    // Ignore, not a valid URL
                }
            }

            // Try location as File
            if (rootFile == null) {
                try {
                    File file = new File(location);
                    if (file.exists())
                        rootFile = AbstractVFS.toVirtualFile(file.toURI());
                } catch (IOException ex) {
                    throw MESSAGES.cannotObtainVirtualFileForLocation(ex, location);
                }
            }

            if (rootFile == null)
                throw MESSAGES.cannotObtainVirtualFileForLocation(null, location);

            DeploymentFactoryPlugin deploymentPlugin = frameworkState.getDeploymentFactoryPlugin();
            dep = deploymentPlugin.createDeployment(location, rootFile);

            return installBundle(dep);

        } catch (RuntimeException rte) {
            VFSUtils.safeClose(rootFile);
            throw rte;
        } catch (BundleException ex) {
            VFSUtils.safeClose(rootFile);
            throw ex;
        }
    }

    Bundle installBundle(Deployment deployment) throws BundleException {
        checkValidBundleContext();

        BundleLifecyclePlugin lifecyclePlugin = bundleState.getCoreServices().getBundleLifecyclePlugin();
        lifecyclePlugin.install(deployment, DefaultBundleLifecycleHandler.INSTANCE);

        ServiceName serviceName = deployment.getAttachment(ServiceName.class);
        FrameworkState frameworkState = getFrameworkState();
        BundleManagerPlugin bundleManager = frameworkState.getBundleManager();
        ServiceContainer serviceContainer = bundleManager.getServiceContainer();

        @SuppressWarnings("unchecked")
        ServiceController<UserBundleState> controller = (ServiceController<UserBundleState>) serviceContainer.getService(serviceName);
        FutureServiceValue<UserBundleState> future = new FutureServiceValue<UserBundleState>(controller);
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof BundleException) {
                throw (BundleException) cause;
            }
            throw MESSAGES.cannotInstallBundleFromDeployment(ex, deployment);
        }
    }

    @Override
    public Bundle getBundle(long id) {
        checkValidBundleContext();
        return getBundleManager().getBundleById(id);
    }

    @Override
    public Bundle[] getBundles() {
        checkValidBundleContext();
        List<Bundle> result = new ArrayList<Bundle>();
        for (Bundle bundle : getBundleManager().getBundles())
            result.add(bundle);
        return result.toArray(new Bundle[result.size()]);
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        getFrameworkEventsPlugin().addServiceListener(bundleState, listener, filter);
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        try {
            getFrameworkEventsPlugin().addServiceListener(bundleState, listener, null);
        } catch (InvalidSyntaxException ex) {
            // ignore
        }
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        getFrameworkEventsPlugin().removeServiceListener(bundleState, listener);
    }

    @Override
    public void addBundleListener(BundleListener listener) {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        getFrameworkEventsPlugin().addBundleListener(bundleState, listener);
    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        getFrameworkEventsPlugin().removeBundleListener(bundleState, listener);
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        getFrameworkEventsPlugin().addFrameworkListener(bundleState, listener);
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        checkValidBundleContext();
        if (listener == null)
            throw MESSAGES.illegalArgumentNull("listener");
        getFrameworkEventsPlugin().removeFrameworkListener(bundleState, listener);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary properties) {
        checkValidBundleContext();
        return registerService(new String[] { clazz }, service, properties);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ServiceRegistration<?> registerService(String[] classNames, Object service, Dictionary properties) {
        if (classNames == null || classNames.length == 0)
            throw MESSAGES.illegalArgumentNull("classNames");
        if (service == null)
            throw MESSAGES.illegalArgumentNull("service");
        checkValidBundleContext();
        ServiceManagerPlugin serviceManager = getFrameworkState().getServiceManagerPlugin();
        ServiceState serviceState = serviceManager.registerService(bundleState, classNames, service, properties);
        return serviceState.getRegistration();
    }

    @Override
    public ServiceReference<?> getServiceReference(String className) {
        if (className == null)
            throw MESSAGES.illegalArgumentNull("className");
        checkValidBundleContext();
        ServiceManagerPlugin serviceManager = getFrameworkState().getServiceManagerPlugin();
        ServiceState serviceState = serviceManager.getServiceReference(bundleState, className);
        return (serviceState != null ? new ServiceReferenceWrapper(serviceState) : null);
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String className, String filter) throws InvalidSyntaxException {
        checkValidBundleContext();
        ServiceManagerPlugin serviceManager = getFrameworkState().getServiceManagerPlugin();
        List<ServiceState> srefs = serviceManager.getServiceReferences(bundleState, className, filter, true);
        if (srefs.isEmpty())
            return null;

        List<ServiceReference<?>> result = new ArrayList<ServiceReference<?>>();
        for (ServiceState serviceState : srefs)
            result.add(serviceState.getReference());

        return result.toArray(new ServiceReference[result.size()]);
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String className, String filter) throws InvalidSyntaxException {
        checkValidBundleContext();
        ServiceManagerPlugin serviceManager = getFrameworkState().getServiceManagerPlugin();
        List<ServiceState> srefs = serviceManager.getServiceReferences(bundleState, className, filter, false);
        if (srefs.isEmpty())
            return null;

        List<ServiceReference<?>> result = new ArrayList<ServiceReference<?>>();
        for (ServiceState serviceState : srefs)
            result.add(serviceState.getReference());

        return result.toArray(new ServiceReference[result.size()]);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> S getService(ServiceReference<S> sref) {
        if (sref == null)
            throw MESSAGES.illegalArgumentNull("sref");
        checkValidBundleContext();
        ServiceState serviceState = ServiceState.assertServiceState(sref);
        ServiceManagerPlugin serviceManager = getFrameworkState().getServiceManagerPlugin();
        S service = (S) serviceManager.getService(bundleState, serviceState);
        return service;
    }

    @Override
    public boolean ungetService(ServiceReference<?> sref) {
        if (sref == null)
            throw MESSAGES.illegalArgumentNull("sref");
        checkValidBundleContext();
        ServiceState serviceState = ServiceState.assertServiceState(sref);
        return getServiceManager().ungetService(bundleState, serviceState);
    }

    @Override
    public File getDataFile(String filename) {
        checkValidBundleContext();
        BundleStoragePlugin storagePlugin = getFrameworkState().getBundleStoragePlugin();
        return storagePlugin.getDataFile(bundleState.getBundleId(), filename);
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        checkValidBundleContext();
        return FrameworkUtil.createFilter(filter);
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        // [TODO] R5 BundleContext.registerService 
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        // [TODO] R5 BundleContext.getServiceReference 
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        // [TODO] R5 BundleContext.getServiceReferences 
        throw new UnsupportedOperationException();
    }

    void checkValidBundleContext() {
        if (destroyed == true)
            throw MESSAGES.illegalStateInvalidBundleContext(bundleState);
    }

    private ServiceManagerPlugin getServiceManager() {
        return getFrameworkState().getServiceManagerPlugin();
    }

    private FrameworkEventsPlugin getFrameworkEventsPlugin() {
        return getFrameworkState().getFrameworkEventsPlugin();
    }

    @Override
    public String toString() {
        return "BundleContext[" + bundleState + "]";
    }
}
