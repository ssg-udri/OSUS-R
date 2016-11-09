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
package mil.dod.th.ose.core.impl.ccomm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectDataManager;
import mil.dod.th.ose.core.impl.ccomm.data.AddressFactoryObjectDataManager;
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

public class TestAddressServiceProxy
{
    private AddressServiceProxy m_SUT;
    private ComponentFactory m_ComponentFactory;
    private ComponentInfo<AddressInternal> m_ComponentInfo;
    
    @Mock private FactoryServiceContext<AddressInternal> factoryServiceContext;
    @Mock private FactoryInternal factory;
    @Mock private AddressManagerServiceImpl addrMgrSvc;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        m_ComponentFactory = mock(ComponentFactory.class);
        m_ComponentInfo = ComponentFactoryMocker.mockSingleComponent(AddressInternal.class, m_ComponentFactory);
        
        m_SUT = new AddressServiceProxy();
        
        m_SUT.setAddressFactory(m_ComponentFactory);
    }
    
    /**
     * Verify proxy is initialized correctly.
     */
    @Test
    public void testInitializeProxy() throws FactoryException
    {
        AddressProxy mockProxy = mock(AddressProxy.class);
        Map<String, Object> map = new HashMap<>();
        AddressInternal internalObj = mock(AddressInternal.class);
        
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
        assertThat(m_SUT.getExtendedServiceAttributeDefinitions(factoryServiceContext, factory, 
                ObjectClassDefinition.ALL).length, is(0));
    }
    
    /**
     * Verify property capability type is returned.
     */
    @Test
    public void testGetCapType()
    {
        assertThat(m_SUT.getCapabilityType().getName(), is(AddressCapabilities.class.getName()));
    }
    
    /**
     * Verify proper base type is returned.
     */
    @Test
    public void testGetBaseType()
    {
        assertThat(m_SUT.getBaseType().getName(), is(Address.class.getName()));
    }
    
    /**
     * Verify proper data manager is returned.
     */
    @Test
    public void testGetDataManager()
    {
        AddressFactoryObjectDataManager dataManager = mock(AddressFactoryObjectDataManager.class);
        m_SUT.setAddressFactoryObjectDataManager(dataManager);
        assertThat(m_SUT.getDataManager(), is((FactoryObjectDataManager)dataManager));
    }
    
    /**
     * Verify that callback is created.
     */
    @Test
    public void testCreateCallback()
    {
        assertThat(m_SUT.createCallback(factoryServiceContext).getClass().getName(), 
                is(AddressRegistryCallback.class.getName()));
    }
    
    /**
     * Verify dictionary with factory type is returned for additional registration properties.
     */
    @Test
    public void testGetAdditionalServiceRegProps()
    {
        when(factory.getAddressCapabilities()).thenReturn(new AddressCapabilities().withPrefix("test-prefix"));
        
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(FactoryDescriptor.FACTORY_TYPE_SERVICE_PROPERTY, AddressFactory.class);
        props.put(AddressFactory.ADDRESS_FACTORY_PREFIX_SERVICE_PROPERTY, "test-prefix");
        assertThat(m_SUT.getAdditionalFactoryRegistrationProps(factory).equals(props), is(true));
    }
    
    /**
     * Verify that factories must be unique.
     */
    @Test
    public void testOnAddFactory() throws Exception
    {
        // basic mocking that always applies
        AddressFactory existingFactory = mock(AddressFactory.class);
        when(factoryServiceContext.getDirectoryService()).thenReturn(addrMgrSvc);
        when(addrMgrSvc.getFactoryByPrefix("dup-prefix")).thenReturn(existingFactory);
        
        // replay with new prefix, should be all good
        when(factory.getAddressCapabilities()).thenReturn(new AddressCapabilities().withPrefix("new-prefix"));
        m_SUT.beforeAddFactory(factoryServiceContext, factory);
        
        // replay with duplicate prefix
        when(factory.getAddressCapabilities()).thenReturn(new AddressCapabilities().withPrefix("dup-prefix"));
        
        try
        {
            m_SUT.beforeAddFactory(factoryServiceContext, factory);
            fail("Expecting exception as prefix is already defined");
        }
        catch (FactoryException e)
        {
            
        }
    }
}
