//==============================================================================
// This software is part of the Open Standard for Unattended Sensors (OSUS)
// reference implementation (OSUS-R).
//
// To the extent possible under law, the author(s) have dedicated all copyright
// and related and neighboring rights to this software to the public domain
// worldwide. This software is distributed without any warranty.
//
// You should have received a copy of the CC0 Public Domain Dedication along
// with this software. If not, see
// <http://creativecommons.org/publicdomain/zero/1.0/>.
//==============================================================================
package mil.dod.th.ose.core.impl.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.sql.Driver;
import java.util.Map;

import javax.jdo.PersistenceManagerFactory;

import mil.dod.th.ose.utils.BundleService;
import mil.dod.th.ose.utils.ClassService;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.datanucleus.plugin.OSGiPluginRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * @author jconn
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceManagerFactoryCreatorImpl.class)
public class TestPersistenceManagerFactoryCreatorImpl
{
    interface TestClass {}
    
    private PersistenceManagerFactoryCreatorImpl m_SUT;
    
    private Driver m_Driver;
    
    private JDOPersistenceManagerFactory m_PersistenceManagerFactory;

    private ClassLoader m_ClassLoader;

    private Bundle m_Bundle;
    
    @Before
    public void setUp()
        throws Exception
    {
        m_SUT = new PersistenceManagerFactoryCreatorImpl();
        m_Driver = PowerMockito.mock(Driver.class);
        m_SUT.setDriver(m_Driver);
        m_PersistenceManagerFactory = PowerMockito.mock(JDOPersistenceManagerFactory.class);
        
        ClassService classService = mock(ClassService.class);
        m_SUT.setClassService(classService);
        m_ClassLoader = mock(ClassLoader.class);
        when(classService.getClassLoader(TestClass.class)).thenReturn(m_ClassLoader);
        
        BundleService bundleService = mock(BundleService.class);
        m_SUT.setBundleService(bundleService);
        m_Bundle = mock(Bundle.class);
        when(bundleService.getBundle(OSGiPluginRegistry.class)).thenReturn(m_Bundle);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    /**
     * Verify factory is created with the correct properties.
     */
    public void testCreatePersistenceManagerFactory() throws Exception
    {
        ArgumentCaptor<Map> propsCapture = ArgumentCaptor.forClass(Map.class);
        PowerMockito.whenNew(JDOPersistenceManagerFactory.class).withArguments(propsCapture.capture())
            .thenReturn(m_PersistenceManagerFactory);
        
        m_SUT.activate();
        when(m_Bundle.getState()).thenReturn(Bundle.ACTIVE);
        
        PersistenceManagerFactory factory = m_SUT.createPersistenceManagerFactory(TestClass.class, "test");
        assertThat(factory, is((PersistenceManagerFactory)m_PersistenceManagerFactory));
        
        Map<String, Object> props = propsCapture.getValue();
        assertThat(props, hasEntry("javax.jdo.PersistenceManagerFactoryClass", 
                (Object)"org.datanucleus.api.jdo.JDOPersistenceManagerFactory"));
        assertThat(props, hasEntry("datanucleus.ConnectionUserName", (Object)"THOSEAdmin"));
        assertThat(props, hasEntry("datanucleus.ConnectionDriverName", (Object)m_Driver.getClass().getName()));
        assertThat(props, hasEntry("datanucleus.mapping", (Object)"h2"));
        assertThat(props, hasEntry("datanucleus.schema.autoCreateAll", (Object)"true"));
        assertThat(props, hasEntry("datanucleus.schema.validateTables", (Object)"false"));
        assertThat(props, hasEntry("datanucleus.schema.validateConstraints", (Object)"false"));
        assertThat(props, hasEntry("datanucleus.classLoaderResolverName", (Object)"datanucleus"));
        assertThat(props, hasEntry("datanucleus.storeManagerType", (Object)"rdbms"));
        assertThat(props, hasEntry("datanucleus.primaryClassLoader", (Object)m_ClassLoader)); 
        assertThat(props, hasEntry("datanucleus.plugin.pluginRegistryClassName", 
                (Object)OSGiPluginRegistry.class.getName()));
        assertThat(props, hasEntry("datanucleus.ConnectionURL", (Object)"test"));
    }
    
    @SuppressWarnings("rawtypes")
    @Test
    /**
     * Verify if the DataNucleus bundle is resolved that activation fails.
     */
    public void testResolvedBundleFails() throws Exception
    {
        ArgumentCaptor<Map> propsCapture = ArgumentCaptor.forClass(Map.class);
        PowerMockito.whenNew(JDOPersistenceManagerFactory.class).withArguments(propsCapture.capture())
            .thenReturn(m_PersistenceManagerFactory);
        
        m_SUT.activate();
        when(m_Bundle.getState()).thenReturn(Bundle.RESOLVED);
        
        try
        {
            m_SUT.createPersistenceManagerFactory(TestClass.class, "test");
            fail("Expecting exception as bundle is only in the resolved state, not active");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
}
