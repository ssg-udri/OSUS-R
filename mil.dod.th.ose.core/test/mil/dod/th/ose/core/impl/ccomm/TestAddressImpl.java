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
// This test class is used to test the Address class.
//
//==============================================================================
package mil.dod.th.ose.core.impl.ccomm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressProxy;
import mil.dod.th.core.ccomm.capability.AddressCapabilities;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;


public class TestAddressImpl
{
    private static UUID OBJ_UUID = UUID.randomUUID();
    private static String OBJ_NAME = "Address1";
    private static String OBJ_PID = "Address1Config";
    private static final String OBJ_BASETYPE = "ObjBaseType";

    private AddressImpl m_SUT;
    private EventAdmin m_EventAdmin;
    private ConfigurationAdmin m_ConfigAdmin;
    @SuppressWarnings("rawtypes")
    private FactoryRegistry m_FactoryRegistry;
    private AddressProxy m_Proxy;
    private FactoryInternal m_Factory;
    private AddressCapabilities m_AddressCapabilities;
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new AddressImpl();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_ConfigAdmin = mock(ConfigurationAdmin.class);
        m_FactoryRegistry = mock(FactoryRegistry.class);
        m_Proxy = mock(AddressProxy.class);
        when(m_Proxy.getAddressDescriptionSuffix()).thenReturn("testSuffix");
        m_Factory = mock(FactoryInternal.class);
        m_AddressCapabilities = new AddressCapabilities().withPrefix("testPrefix");
        when(m_Factory.getAddressCapabilities()).thenReturn(m_AddressCapabilities);
        when(m_Factory.getProductType()).thenReturn("product-type");
        PowerManagerInternal powerManager = mock(PowerManagerInternal.class);

        m_SUT.initialize(m_FactoryRegistry, m_Proxy, m_Factory, 
                m_ConfigAdmin, m_EventAdmin, powerManager, OBJ_UUID, OBJ_NAME, OBJ_PID, OBJ_BASETYPE);
        m_SUT.postCreation();
    }
    
    /**
     * Verify ability to get the address factory.
     */
    @Test
    public void testGetFactory()
    {
        assertThat(m_SUT.getFactory(), is(m_Factory));
    }

    /**
     * Verify ability to get an address's PID.
     */
    @Test
    public void testGetPid()
    {
        m_SUT.setPid("pidtest");
        assertThat(m_SUT.getPid(), is("pidtest"));
    }

    /**
     * Verify {@link AddressImpl#getDescription()} returns the prefix and suffix with : in the middle.
     */
    @Test
    public void testGetDescription() throws FactoryException
    {
        assertThat(m_SUT.getDescription(), is("testPrefix:testSuffix"));
    }
    
    /**
     * Verify that toString just calls {@link AddressImpl#getDescription()}.
     */
    @Test
    public void testToString() throws IllegalArgumentException, IllegalStateException, FactoryException
    {
        assertThat(m_SUT.toString(), is(m_SUT.getDescription()));
    }

    /**
     * Verify expected property values are contained within the call to get an address's event properties.
     */
    @Test
    public void testGetEventProperties() throws IllegalArgumentException, FactoryException
    {
        m_SUT.internalSetName("test");
        
        Map<String, Object> props = m_SUT.getEventProperties("test");
        
        assertThat(props, hasEntry("test" + Address.EVENT_PROP_ADDRESS_SUFFIX, (Object)m_SUT));
        assertThat(props, hasEntry("test" + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX, 
                (Object)m_SUT.getFactory().getProductType()));
        assertThat(props, hasEntry("test" + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX, (Object)"test"));
        assertThat(props, hasEntry("test" + Address.EVENT_PROP_MESSAGE_ADDRESS_SUFFIX, 
                (Object)m_SUT.getDescription()));
    }

    /**
     * Verify that the associated proxy method is called.
     */
    @Test
    public void testEqualProperties()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        m_SUT.equalProperties(properties);
        verify(m_Proxy).equalProperties(properties);
    }
}
