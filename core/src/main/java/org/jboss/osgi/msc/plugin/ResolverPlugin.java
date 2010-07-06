/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.osgi.msc.plugin;

// $Id$

import java.util.List;

import org.jboss.osgi.msc.bundle.AbstractBundle;
import org.osgi.framework.BundleException;

/**
 * The resolver plugin.
 * 
 * @author thomas.diesler@jboss.com
 * @since 06-Jul-2009
 */
public interface ResolverPlugin extends Plugin 
{
   /**
    * Add a bundle to the resolver.
    * @param bundle the bundle
    * @return The resBundle associated with the added bundle.
    */
   void addBundle(AbstractBundle bundle);
   
   /**
    * Remove a bundle from the resolver.
    * @param bundle the bundle
    * @return The resBundle associated with the removed bundle.
    */
   void removeBundle(AbstractBundle bundle);
   
   /**
    * Resolve the given bundle.
    * @param bundles the bundles to resolve
    * @throws BundleException If the bundle could not get resolved
    */
   void resolve(AbstractBundle bundle) throws BundleException;
   
   /**
    * Resolve the given list of bundles.
    * @param bundles the bundles to resolve
    * @return The list of resolved bundles in the resolve order or an empty list
    */
   List<AbstractBundle> resolve(List<AbstractBundle> bundles);
}