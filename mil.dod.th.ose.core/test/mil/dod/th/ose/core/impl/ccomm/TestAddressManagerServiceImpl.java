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
//
// DESCRIPTION:
// This test is used to test the Addresss Lookup class.
//
//==============================================================================
package mil.dod.th.ose.core.impl.ccomm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressFactory;
import mil.dod.th.core.ccomm.AddressTranslator;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.FactoryServiceContext;
import mil.dod.th.ose.core.factory.api.FactoryServiceProxy;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.FactoryObjectDataManagerMocker;
import mil.dod.th.ose.core.impl.ccomm.data.AddressFactoryObjectDataManager;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.ComponentFactoryMocker.ComponentInfo;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.common.collect.ImmutableMap;

/**
 * This class tests the AddressManagerServiceImpl.
 * 
 * @author fwebber
 */
public class TestAddressManagerServiceImpl
{
    private static final String PRODUCT_TYPE = "product-type";
    
    private AddressManagerServiceImpl m_SUT;
    private EventAdmin m_EventAdmin;
    private BundleContext m_Context;
    private AddressFactoryObjectDataManager m_AddressFactoryObjectDataManager;
    private FactoryInternal m_Factory;
    private FactoryRegistry<AddressInternal> m_Registry;
    private Set<AddressInternal> m_RegistryObjects;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_EvtHandlerServReg;
    private FactoryServiceProxy<AddressInternal> m_FactoryServiceProxy;
    private AddressTranslatorManager m_Manager;
    private FactoryServiceContext<AddressInternal> m_FactoryServiceContext;
    
    private AddressTranslator m_Translator;
    
    @Mock private ComponentFactory serviceContextFactory;
    @SuppressWarnings("rawtypes")
    private FactoryServiceContext m_ServiceContext;

    @SuppressWarnings("rawtypes")
    private ComponentInfo<FactoryServiceContext> m_ServiceContextComp;

    @Mock private PowerManager m_PowerManager;
    @Mock private WakeLock m_WakeLock;

    @SuppressWarnings({"unchecked"})
    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        m_EventAdmin = mock(EventAdmin.class);
        m_Context = mock(BundleContext.class);
        m_Manager = mock(AddressTranslatorManager.class);
        m_Factory = mock(FactoryInternal.class);
        m_AddressFactoryObjectDataManager = mock(AddressFactoryObjectDataManager.class);
        m_Registry = mock(FactoryRegistry.class);
        m_FactoryServiceContext = mock(FactoryServiceContext.class);
        
        m_SUT = new AddressManagerServiceImpl();
        
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setAddressTranslatorManager(m_Manager);
        m_SUT.setPowerManager(m_PowerManager);

        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "coreAddrManagerService")).thenReturn(m_WakeLock);

        m_RegistryObjects = new HashSet<AddressInternal>();
        when(m_Registry.getObjects()).thenReturn(m_RegistryObjects);
                
        when(m_AddressFactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("name");
        
        m_FactoryServiceProxy = mock(FactoryServiceProxy.class);
        m_SUT.setFactoryServiceProxy(m_FactoryServiceProxy);

        Bundle bundle = mock(Bundle.class);
        when(m_Context.getBundle()).thenReturn(bundle);
        m_EvtHandlerServReg = mock(ServiceRegistration.class);
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class),
            Mockito.any(Dictionary.class))).thenReturn(m_EvtHandlerServReg);
        
        when(m_Factory.getAddressCapabilities()).thenReturn(new AddressCapabilities().withPrefix("Base"));
        when(m_Factory.getProductType()).thenReturn(PRODUCT_TYPE);
        
        mockTranslatorForFactory(m_Factory);
        
        final AddressInternal mockAddress = mock(AddressInternal.class);
        when(mockAddress.getFactory()).thenReturn(m_Factory);
        when(m_Registry.createNewObject(eq(m_Factory), Mockito.any(String.class),
            Mockito.any(Map.class))).thenAnswer(new Answer<AddressInternal>()
            {
                @Override
                public AddressInternal answer(InvocationOnMock invocation) throws Throwable
                {
                    Object param = invocation.getArguments()[1];
                    if (param != null)
                    {
                        when(mockAddress.getName()).thenReturn(param.toString());
                    }
                    m_RegistryObjects.add(mockAddress);
                    return mockAddress;
                }
            });
        doAnswer(new Answer<AddressInternal>()
        {
            @Override
            public AddressInternal answer(InvocationOnMock invocation) throws Throwable
            {
                m_RegistryObjects.remove(mockAddress);
                return mockAddress;
            }
        }).when(m_Registry).removeObject(Mockito.any(UUID.class));
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                m_Registry.removeObject(mockAddress.getUuid());
                return null;
            }
        }).when(m_Registry).delete(Mockito.any(AddressInternal.class));
        
        FactoryObjectDataManagerMocker.createMockFactoryObjectDataManager(m_AddressFactoryObjectDataManager);
        
        m_SUT.setFactoryServiceContextFactory(serviceContextFactory);
        m_ServiceContextComp = 
                ComponentFactoryMocker.mockSingleComponent(FactoryServiceContext.class, serviceContextFactory);
        m_ServiceContext = m_ServiceContextComp.getObject();
        when(m_ServiceContext.getFactories()).thenReturn(ImmutableMap.builder().put(PRODUCT_TYPE, m_Factory).build());
        when(m_ServiceContext.getRegistry()).thenReturn(m_Registry);
        
        m_SUT.activate(m_Context);
    }
    
    /**
     * Verify registry and service context are initialized properly.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testActivation() throws Exception
    {
        verify(serviceContextFactory).newInstance(null);
        verify(m_ServiceContext).initialize(m_Context, m_FactoryServiceProxy, m_SUT);
    }
    
    /**
     * Make sure {@link FactoryServiceContext} is disposed.
     */
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();
        
        verify(m_ServiceContextComp.getInstance()).dispose();
        verify(m_WakeLock, times(1)).delete();
    }
    
    @Test
    public void testCreateFromDescription() throws CCommException
    {
        // This should fail because ExampleAddress has not been registered
        try
        {
            m_SUT.getOrCreateAddress("Example:Param1=null");
            fail("Address does not exist!");
        }
        catch(CCommException e)
        {
            verify(m_WakeLock, never()).activate();
        }
        
        // This should not fail because BaseAddress has been registered
        Address b   = m_SUT.getOrCreateAddress("Base:noparams=null");
        assertThat(b, not(nullValue()));
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Test that the helper service is passed to the factory during the create operation.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateAddressProperties() throws FactoryException, CCommException, InterruptedException, 
        IOException, IllegalArgumentException, FactoryObjectInformationException, ClassNotFoundException
    {
        // replay
        m_SUT.getOrCreateAddress("Base:blah");
        
        verify(m_Manager).getAddressTranslator(m_Factory.getProductType());
        
        // verify service helper is passed
        verify(m_WakeLock).activate();
        verify(m_Registry).createNewObject(eq(m_Factory), anyString(), Mockito.any(Map.class));
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify that flush will remove all addresses (even if one throws an exception).
     */
    @Test
    public void testFlush() throws Exception
    {
        // Create addresses to flush
        Address addr1 = m_SUT.getOrCreateAddress("Base:addr1");
        Address addr2 = m_SUT.getOrCreateAddress("Base:addr2");
        Address addr3 = m_SUT.getOrCreateAddress("Base:addr3");
        Address addr4 = m_SUT.getOrCreateAddress("Base:addr4");
        Address addr5 = m_SUT.getOrCreateAddress("Base:addr5");

        doThrow(new IllegalStateException()).when(addr3).delete();
        
        m_SUT.flush();
        
        verify(addr1).delete();
        verify(addr2).delete();
        verify(addr3).delete();
        verify(addr4).delete();
        verify(addr5).delete();
    }

    /**
     * Verify creating an address fails if the address is incomplete.
     */
    @Test
    public void testCreateFromDescriptionIncomplete() throws CCommException
    {
        // This should fail, base is registered, but addr is incomplete
        try
        {
            m_SUT.getOrCreateAddress("Base:");
            fail("Expected exception");
        }
        catch(IllegalArgumentException e)
        {
            //expected because the address is not complete
        }
    }

    /**
     * Verify address already exists queries error if the address description passed is incomplete.
     */
    @Test
    public void testAddressAlreadyExist() throws CCommException
    {
        //create valid addr
        Address address = m_SUT.getOrCreateAddress("Base:things");
        
        // mock address to return true for matched props
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop", "things");
        when(m_FactoryServiceContext.getRegistry()).thenReturn(m_Registry);
        when(m_FactoryServiceContext.getRegistry().getObjectsByProductType(anyString())).thenReturn(m_RegistryObjects);
        when(address.equalProperties(props)).thenReturn(true);
        
        // This should be true as the address exists
        assertThat(m_SUT.checkAddressAlreadyExists("Base:things"), is(true));
        
        //verify
        verify(m_Translator, times(2)).getAddressPropsFromString("things");
    }
    
    /**
     * Verify invalid input causes an exception.
     */
    @Test
    public void testInvalidAddressDescription() throws CCommException
    {
        try
        {
            m_SUT.getOrCreateAddress(null);
            fail("Expecting exception as address is null");
        }
        catch (NullPointerException e)
        {
            verify(m_WakeLock, never()).activate();
        }
        
        try
        {
            m_SUT.getOrCreateAddress("no-colon-so-no-prefix");
            fail("Expecting exception as address is missing colon");
        }
        catch (IllegalArgumentException e)
        {
            verify(m_WakeLock, never()).activate();
        }
        
        try
        {
            m_SUT.getOrCreateAddress("prefix-only:");
            fail("Expecting exception as address is missing suffix");
        }
        catch (IllegalArgumentException e)
        {
            verify(m_WakeLock, never()).activate();
        }
    }
    
    /**
     * This test ensures that the behavior of getAddressDescriptiveStrings is working as intended.
     * Specifically, that means:
     * - No exception thrown, only an empty list returned, when no addresses have been registered;
     * - Added addresses are correctly returned;
     * - Removed addresses are correctly ... no longer returned.
     */
    @Test
    public void getAddressDescriptiveStrings() throws IllegalArgumentException, IllegalStateException, IOException,
        ConfigurationException, FactoryException, AssetException, CCommException
    {
        // No exception thrown on empty list, and that list is empty
        List<String> emptyList  = m_SUT.getAddressDescriptiveStrings();
        assertThat(emptyList.size(), is(0));
        
        Address address1= m_SUT.getOrCreateAddress("Base:abc");
        
        // Now try and get the address
        List<String> oneElementList = m_SUT.getAddressDescriptiveStrings();
        assertThat(oneElementList.size(), is(1));
        
        // Now let's delete the address
        m_Registry.delete((FactoryObjectInternal)address1);
        List<String> alsoEmpty  = m_SUT.getAddressDescriptiveStrings();
        assertThat(alsoEmpty.size(), is(0));       
    }
    
    /**
     * Verify that if an address is not already existent that it is created.  Conversely, if already created, not 
     * created again.
     */
    @Test
    public void testGetOrCreateAddress() throws CCommException, IllegalArgumentException, InterruptedException,
        FactoryException, FactoryObjectInformationException, ClassNotFoundException
    {
        Address a = m_SUT.getOrCreateAddress("Base:a.b.c");
        m_RegistryObjects.add((AddressInternal) a);
        
        // mock address created previously to be equal to the next one being created
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("prop", "a.b.c");
        when(a.equalProperties(properties)).thenReturn(true);
        when(m_FactoryServiceContext.getRegistry()).thenReturn(m_Registry);
        when(m_FactoryServiceContext.getRegistry().getObjectsByProductType(anyString())).thenReturn(m_RegistryObjects);
        m_SUT.getOrCreateAddress(m_Factory.getProductType(), "old-addr", properties);
        
        // verify a new address is not created
        verify(m_WakeLock).activate();
        verify(m_Registry, times(1)).createNewObject(eq(m_Factory), anyString(), eq(properties));
        verify(m_WakeLock).cancel();
        
        // now mock to not be equal to the address
        when(a.equalProperties(properties)).thenReturn(false);
        m_SUT.getOrCreateAddress(m_Factory.getProductType(), "new-addr", properties);
        
        // verify a new address is created
        verify(m_WakeLock, times(2)).activate();
        verify(m_Registry, times(2)).createNewObject(eq(m_Factory), anyString(), eq(properties));
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /**
     * Verify that if an address with multiple parts delimited by ':' is not already existent that it is created.  
     * Conversely, if already created, not created again.
     */
    @Test
    public void testGetOrCreateAddressWithMultipleParts() 
        throws CCommException, IllegalArgumentException, InterruptedException,
        FactoryException, FactoryObjectInformationException, ClassNotFoundException
    {
        Address a = m_SUT.getOrCreateAddress("Base:a.b.c:1234");
        
        // mock address created previously to be equal to the next one being created
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("prop", "a.b.c:1234");
        when(a.equalProperties(properties)).thenReturn(true);
        when(m_FactoryServiceContext.getRegistry()).thenReturn(m_Registry);
        when(m_FactoryServiceContext.getRegistry().getObjectsByProductType(anyString())).thenReturn(m_RegistryObjects);
        m_SUT.getOrCreateAddress(m_Factory.getProductType(), "old-addr", properties);
        
        // verify a new address is not created
        verify(m_WakeLock).activate();
        verify(m_Registry, times(1)).createNewObject(eq(m_Factory), anyString(), eq(properties));
        verify(m_WakeLock).cancel();
        
        // now mock to not be equal to the address
        when(a.equalProperties(properties)).thenReturn(false);
        m_SUT.getOrCreateAddress(m_Factory.getProductType(), "new-addr", properties);
        
        // verify a new address is created
        verify(m_WakeLock, times(2)).activate();
        verify(m_Registry, times(2)).createNewObject(eq(m_Factory), anyString(), eq(properties));
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /*
     * Test that address name is correctly set if provided at creation.
     */
    @Test
    public void testCreateWithName() throws CCommException, IllegalArgumentException,
        FactoryObjectInformationException, FactoryException, ClassNotFoundException
    {
        when(m_AddressFactoryObjectDataManager.getName(Mockito.any(UUID.class))).thenReturn("creationAddressName");
        m_SUT.getOrCreateAddress("Base:abc", "creationAddressName");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop", "abc");
        verify(m_Registry).createNewObject(m_Factory, "creationAddressName", props);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify that the service can 'print deep' to a print stream.
     */
    @Test
    public void testPrintDeep() throws CCommException
    {
        PrintStream printStream = mock(PrintStream.class);
        
        //create test address and call printDeep
        Address a = m_SUT.getOrCreateAddress("Base:test", "the-name");
        m_SUT.printDeep(printStream);
        
        //verify print stream prints address type and address object
        verify(printStream).
            format("%s: %s (%s)%n", PRODUCT_TYPE, a.toString(), "the-name");
    }
    
    /**
     * Verify that a created address can be verified by descriptive string and the address object itself.
     */
    @Test
    public void testcheckAddressAlreadyExists() throws CCommException, FactoryException, IOException, AssetException,
            IllegalArgumentException
    {
        Address a = m_SUT.getOrCreateAddress("Base:blah");
        Map<String, Object> props = new HashMap<>();
        props.put("prop", "blah");
        when(m_FactoryServiceContext.getRegistry()).thenReturn(m_Registry);
        when(m_FactoryServiceContext.getRegistry().getObjectsByProductType(anyString())).thenReturn(m_RegistryObjects);
        when(a.equalProperties(props)).thenReturn(true);
        
        //verify address is created, by the address's descriptive string
        assertThat(m_SUT.checkAddressAlreadyExists("Base:blah"), is(true));
    }
    
    /**
     * Verify no exception if the check for if an address exists is passed a unknown address string.
     */
    @Test
    public void testcheckAddressAlreadyExistsUnknown()
    {
        assertThat(m_SUT.checkAddressAlreadyExists("Base:jack-Jill"), is(false));
    }
    
    /**
     * Verify no exception if the check for if an address exists is passed an address string, for which there is no 
     * factory.
     */
    @Test
    public void testcheckAddressAlreadyExistsNoFactory()
    {
        assertThat(m_SUT.checkAddressAlreadyExists("Monkey:jack-Jill"), is(false));
    }
    
    /**
     * Verify no exception, but false, if the check for if an address exists is passed an address string which is 
     * no formatted correctly.
     */
    @Test
    public void testcheckAddressAlreadyExistsBadFormat() throws CCommException
    {
        String addrString = "Base:jack-Jill";
        when(m_Translator.getAddressPropsFromString(addrString.split(":")[1])).
            thenThrow(new CCommException(FormatProblem.ADDRESS_MISMATCH));
        //act
        assertThat(m_SUT.checkAddressAlreadyExists(addrString), is(false));
    }
    
    /**
     * Mock out translator service.
     */
    @SuppressWarnings("rawtypes")
    private void mockTranslatorForFactory(final AddressFactory factory) throws CCommException
    {
        //ran multiple times for different factories, don't reset translator
        //just re-use, and VERIFY interactions
        if (m_Translator == null)
        {
            m_Translator = mock(AddressTranslator.class);
        }
        when(m_Translator.getAddressPropsFromString(anyString())).thenAnswer(new Answer<Map>()
        {
            @Override
            public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable
            {
                String description = (String)invocation.getArguments()[0];
                Map<String, Object> props = new HashMap<>();
                props.put("prop", description);
                return props;
            }
        });
        when(m_Manager.getAddressTranslator(factory.getProductType())).
            thenReturn(m_Translator);
    }
}
