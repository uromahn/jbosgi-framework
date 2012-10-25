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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collection;

import org.jboss.modules.ClassSpec;
import org.jboss.modules.PackageSpec;
import org.jboss.modules.Resource;
import org.jboss.modules.ResourceLoader;
import org.jboss.osgi.framework.spi.VirtualFileResourceLoader;
import org.jboss.osgi.metadata.OSGiManifestBuilder;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.framework.subA.SimpleActivator;
import org.jboss.test.osgi.framework.subA.SimpleService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the bundle content loader.
 *
 * @author thomas.diesler@jboss.com
 * @since 29-Apr-2010
 */
public class VirtualFileResourceLoaderTestCase {

    private static VirtualFile rootFile;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "simple-bundle");
        archive.addClasses(SimpleService.class, SimpleActivator.class);
        archive.addAsResource("logging.properties");
        archive.setManifest(new Asset() {
            public InputStream openStream() {
                OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
                builder.addBundleManifestVersion(2);
                builder.addBundleSymbolicName(archive.getName());
                builder.addBundleActivator(SimpleActivator.class);
                builder.addExportPackages(SimpleService.class);
                return builder.openStream();
            }
        });
        rootFile = OSGiTestHelper.toVirtualFile(archive);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        rootFile.close();
    }

    @Test
    public void testClassSpec() throws Exception {
        ResourceLoader loader = new VirtualFileResourceLoader(rootFile);
        String fileName = SimpleActivator.class.getName();
        fileName = fileName.replace('.', '/') + ".class";
        ClassSpec result = loader.getClassSpec(fileName);
        assertNotNull("ClassSpec not null", result);
    }

    @Test
    public void testPackageSpec() throws Exception {
        ResourceLoader loader = new VirtualFileResourceLoader(rootFile);
        PackageSpec result = loader.getPackageSpec(SimpleActivator.class.getPackage().getName());
        assertNotNull("PackageSpec not null", result);
    }

    @Test
    public void testResource() throws Exception {
        ResourceLoader loader = new VirtualFileResourceLoader(rootFile);
        Resource result = loader.getResource("META-INF/MANIFEST.MF");
        assertNotNull("Resource not null", result);

        result = loader.getResource("/META-INF/MANIFEST.MF");
        assertNotNull("Resource not null", result);

        result = loader.getResource("logging.properties");
        assertNotNull("Resource not null", result);

        result = loader.getResource("/logging.properties");
        assertNotNull("Resource not null", result);
    }

    @Test
    public void testPaths() throws Exception {
        VirtualFileResourceLoader loader = new VirtualFileResourceLoader(rootFile);
        Collection<String> paths = loader.getPaths();
        assertNotNull("Resource not null", paths);
        assertEquals(3, paths.size());
        assertTrue(paths.contains("org/jboss/test/osgi/framework/subA"));
        assertTrue(paths.contains("META-INF"));
        assertTrue(paths.contains(""));
    }
}