package org.jboss.test.osgi.container.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.Map;

import org.jboss.modules.ModuleIdentifier;
import org.jboss.osgi.container.bundle.AbstractBundle;
import org.jboss.osgi.container.bundle.BundleManager;
import org.jboss.osgi.container.bundle.HostBundleRevision;
import org.jboss.osgi.container.bundle.ModuleManager;
import org.jboss.osgi.container.bundle.Revision;
import org.jboss.osgi.resolver.XModule;
import org.jboss.osgi.resolver.spi.AbstractModuleBuilder;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.Version;


@Ignore("FIXME: javadoc, license, intension, new ModuleHolder(b, null)")
public class ModuleManagerTestCase
{
   @Test
   public void testGetUnrevisionedModule() throws Exception
   {
      class MyModuleManager extends ModuleManager
      {
         public MyModuleManager()
         {
            super(mock(BundleManager.class));
         }
         
         @SuppressWarnings({ "rawtypes", "unchecked" })
         AbstractBundle createModule(int revision) throws Exception
         {
            ModuleIdentifier mi = new ModuleIdentifier("test", "test", "0.0.0-rev" + revision);
            Field mf = ModuleManager.class.getDeclaredField("modules");
            mf.setAccessible(true);
            Map modules = (Map)mf.get(this);

            AbstractBundle b = mock(AbstractBundle.class);
            ModuleManager.ModuleHolder holder = new ModuleManager.ModuleHolder(b, null);
            modules.put(mi, holder);
            return b;
         }
      };

      MyModuleManager mm = new MyModuleManager();
      Object ex1 = mm.createModule(0);
      Object ex2 = mm.createModule(1);
      Object ex3 = mm.createModule(3);
      
      assertEquals("Should return the module with the highest revision",
            ex3, mm.getBundleState(new ModuleIdentifier("test", "test", "0.0.0")));

      assertEquals(ex1, mm.getBundleState(new ModuleIdentifier("test", "test", "0.0.0-rev0")));
      assertEquals(ex2, mm.getBundleState(new ModuleIdentifier("test", "test", "0.0.0-rev1")));
      assertNull(mm.getBundleState(new ModuleIdentifier("test", "test", "0.0.0-rev2")));
   }

   @Test
   public void testGetModuleIdentifier() throws Exception
   {
      HostBundleRevision br = Mockito.mock(HostBundleRevision.class);
      Mockito.when(br.getRevision()).thenReturn(42);

      XModule xm = new AbstractModuleBuilder().createModule(123, "myModule", Version.emptyVersion);
      xm.addAttachment(Revision.class, br);

      ModuleIdentifier id = ModuleManager.getModuleIdentifier(xm);
      assertEquals("module:jbosgi[123]:myModule:0.0.0-rev42", id.toURL().toString());
      ModuleIdentifier id2 = ModuleManager.getModuleIdentifier(xm);
      assertSame(id, id2);

      HostBundleRevision br2 = Mockito.mock(HostBundleRevision.class);
      Mockito.when(br2.getRevision()).thenReturn(43);

      XModule xm2 = new AbstractModuleBuilder().createModule(123, "myModule", Version.emptyVersion);
      xm2.addAttachment(Revision.class, br2);
      ModuleIdentifier id3 = ModuleManager.getModuleIdentifier(xm2);
      String a = id.toURL().toString();
      String b = id3.toURL().toString();
      Assert.assertTrue("Should be different: " + a + " and " + b, !a.equals(b));
   }
}
