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
package mil.dod.th.ose.datastream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.StreamProfileFactory;
import mil.dod.th.core.datastream.StreamProfileProxy;
import mil.dod.th.core.datastream.capability.StreamProfileCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * @author jmiller
 *
 */
public class TestStreamProfileServiceProxy
{
    private StreamProfileServiceProxy m_SUT;
    
    @Mock private ComponentFactory m_ComponentFactory;
    @Mock private FactoryServiceContext<StreamProfileInternal> m_FactoryServiceContext;
    @Mock private FactoryRegistry<StreamProfileInternal> m_FactoryRegistry;
    @Mock private FactoryInternal m_Factory;
    @Mock private DataStreamServiceImpl m_DataStreamService;
    @Mock private StreamProfileConfigListener m_ConfigListener;
    
    private ComponentInfo<StreamProfileInternal> m_ComponentInfo;
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        m_ComponentInfo = ComponentFactoryMocker.mockSingleComponent(StreamProfileInternal.class, m_ComponentFactory);
        
        m_SUT = new StreamProfileServiceProxy();
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setStreamProfileFactory(m_ComponentFactory);
        m_SUT.setConfigListener(m_ConfigListener);
    }
    
    @Test
    public void testInitializeProxy() throws FactoryException
    {
        StreamProfileProxy mockProxy = mock(StreamProfileProxy.class);
        StreamProfileInternal internalObj = mock(StreamProfileInternal.class);
        
        Map<String, Object> map = new HashMap<>();
        m_SUT.initializeProxy(internalObj,  mockProxy, map);
        
        verify(mockProxy).initialize(internalObj, map);       
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateComponentInstance()
    {
        ComponentInstance instance = m_SUT.createFactoryObjectInternal(m_Factory);
        verify(m_ComponentFactory).newInstance(Mockito.any(Dictionary.class));
        assertThat(instance, is(m_ComponentInfo.getInstance()));
    }
    
    /**
     * Verify there are no additional service attribute definitions.
     */
    @Test
    public void testGetExtendedServiceAttributeDefinitions()
    {
        assertThat(m_SUT.getExtendedServiceAttributeDefinitions(m_FactoryServiceContext, m_Factory, 
                ObjectClassDefinition.ALL).length, is(0));
    }
    
    /**
     * Verify that the correct capability type is returned.
     */
    @Test
    public void testGetCapabilityType()
    {
        assertThat(m_SUT.getCapabilityType().getName(), is(StreamProfileCapabilities.class.getName()));
    }
    
    /**
     * Verify that the proper base type is returned.
     */
    @Test
    public void testGetBaseType()
    {
        assertThat(m_SUT.getBaseType().getName(), is(StreamProfile.class.getName()));
    }
    
    /**
     * Verify dictionary with factory type is returned for additional registration properties.
     */
    @Test
    public void testGetAdditionalServiceRegistrationProps()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, StreamProfileFactory.class);
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(null).equals(props), is(true));
    }
    
    /**
     * Verify that a configuration listener is registered for the factory being added.
     */
    @Test
    public void testBeforeAddFactory() throws FactoryException
    {
        String factoryPid = "some.factory.pid";
        when(m_Factory.getPid()).thenReturn(factoryPid);
        
        m_SUT.beforeAddFactory(m_FactoryServiceContext, m_Factory);
        
        verify(m_ConfigListener).registerConfigListener(factoryPid);
    }
    
    /**
     * Verify that the configuration listener for the factory being removed is unregistered.
     */
    @Test
    public void testOnRemoveFactory()
    {
        String factoryPid = "some.factory.pid";
        when(m_Factory.getPid()).thenReturn(factoryPid);
        
        m_SUT.onRemoveFactory(m_FactoryServiceContext, m_Factory);
        
        verify(m_ConfigListener).unregisterConfigListener("some.factory.pid");
    }
    
    /**
     * Verify that callback is created.
     */
    @Test
    public void testCreateCallback()
    {
        assertThat(m_SUT.createCallback(m_FactoryServiceContext).getClass().getName(),
                is(StreamProfileRegistryCallback.class.getName()));
    }
}
