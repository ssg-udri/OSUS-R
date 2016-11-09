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
package mil.dod.th.ose.core.impl.ccomm.transport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.impl.ccomm.TransportLayerRegistryCallback;
import mil.dod.th.ose.core.impl.ccomm.transport.data.TransportLayerFactoryObjectDataManager;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.metatype.ObjectClassDefinition;


/**
 * Test class for the {@link TransportLayerServiceProxy}.
 * @author allenchl
 *
 */
public class TestTransportLayerServiceProxy
{
    private TransportLayerServiceProxy m_SUT;
    private ComponentFactory m_ComponentFactory;
    private ComponentInfo<TransportLayerInternal> m_ComponentInfo;
    
    @Mock private FactoryServiceContext<TransportLayerInternal> factoryServiceContext;
    @Mock private TransportLayerFactoryObjectDataManager transportLayerFactoryObjectDataManager;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        m_ComponentFactory = mock(ComponentFactory.class);
        m_ComponentInfo = ComponentFactoryMocker.
                mockSingleComponent(TransportLayerInternal.class, m_ComponentFactory);
        
        m_SUT = new TransportLayerServiceProxy();
        
        m_SUT.setTransportLayerFactory(m_ComponentFactory);
        m_SUT.setTransportLayerFactoryObjectDataManager(transportLayerFactoryObjectDataManager);
    }
    
    /**
     * Verify proxy is initialized correctly.
     */
    @Test
    public void testInitializeProxy() throws FactoryException
    {
        TransportLayerProxy mockProxy = mock(TransportLayerProxy.class);
        Map<String, Object> map = new HashMap<>();
        TransportLayerInternal internalObj = mock(TransportLayerInternal.class);
        
        m_SUT.initializeProxy(internalObj, mockProxy, map);
        
        verify(mockProxy).initialize(internalObj, map);
    }
    
    /**
     * Verify that an internal object can be created.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateComponentInstance()
    {
        ComponentInstance instance = m_SUT.createFactoryObjectInternal(Mockito.any(FactoryInternal.class));
        
        verify(m_ComponentFactory).newInstance(Mockito.any(Dictionary.class));
        
        assertThat(instance, is(m_ComponentInfo.getInstance()));
    }
    
    /**
     * Verify there are no additional service attr defs.
     */
    @Test
    public void testGetExtendedServiceAttributeDefinitions()
    {
        FactoryInternal factory = mock(FactoryInternal.class);
        assertThat(m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, factory,
                ObjectClassDefinition.ALL).length, is(0));
    }
    
    /**
     * Verify property capability type is returned.
     */
    @Test
    public void testGetCapType()
    {
        assertThat(m_SUT.getCapabilityType().getName(), is(TransportLayerCapabilities.class.getName()));
    }
    
    /**
     * Verify proper base type is returned.
     */
    @Test
    public void testGetBaseType()
    {
        assertThat(m_SUT.getBaseType().getName(), is(TransportLayer.class.getName()));
    }
    
    /**
     * Verify dictionary with factory type is returned for additional registration properties.
     */
    @Test
    public void testGetAdditionalServiceRegProps()
    {
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, TransportLayerFactory.class);
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(null).equals(props), is(true));
    }
    
    /**
     * Verify that set factory object data manager is returned.
     */
    @Test
    public void testGetDataManager()
    {
        assertThat((TransportLayerFactoryObjectDataManager)m_SUT.getDataManager(), 
                is(transportLayerFactoryObjectDataManager));
    }
    
    /**
     * Verify that callback is created.
     */
    @Test
    public void testCreateCallback()
    {
        assertThat(m_SUT.createCallback(factoryServiceContext).getClass().getName(), 
                is(TransportLayerRegistryCallback.class.getName()));
    }
    
    /**
     * Verify that transport layers are shutdown when a factory is removed.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testOnRemoveFactory()
    {
        //product type
        String productType1 = "a";
        
        //mock behavior
        FactoryRegistry reg = mock(FactoryRegistry.class);
        FactoryServiceContext context = mock(FactoryServiceContext.class);
        when(context.getRegistry()).thenReturn(reg);
        
        //create a transport object to return
        FactoryInternal fact1 = mock(FactoryInternal.class);
        when(fact1.getProductType()).thenReturn(productType1);
        TransportLayerInternal trans1 = mock(TransportLayerInternal.class);
        when(trans1.getFactory()).thenReturn(fact1);
        TransportLayerInternal trans2 = mock(TransportLayerInternal.class);
        when(trans2.getFactory()).thenReturn(fact1);
        
        Set<TransportLayerInternal> transports = new HashSet<>();
        transports.add(trans1);
        transports.add(trans2);
        when(reg.getObjectsByProductType(productType1)).thenReturn(transports);
        
        m_SUT.onRemoveFactory(context, fact1);
        
        verify(trans1).shutdown();
        verify(trans2).shutdown();
    }
}
