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
// This test class is used for testing the Sample Plugin 2 class.
//
//==============================================================================
package mil.dod.th.ose.core.impl.ccomm.link;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.link.LinkFrame;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.link.LinkLayerAttributes;
import mil.dod.th.core.ccomm.link.LinkLayerProxy;
import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.test.EventAdminVerifier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestLinkLayerImpl
{
    private LinkLayerImpl m_SUT;
    private LinkLayerProxy m_LinkLayerProxy;
    private FactoryRegistry<?> m_FactReg;
    private FactoryInternal m_LinkLayerFactoryInternal;
    private ConfigurationAdmin m_ConfigurationAdmin;
    private EventAdmin m_EventAdmin;
    private UUID m_Uuid = UUID.randomUUID();
    private String m_Name = "name";
    private String m_Pid = "pid1";
    private String m_BaseType = "baseType";
    private LinkLayerCapabilities m_Caps;
    private PowerManagerInternal m_PowManInternal;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new LinkLayerImpl();
        
        //mocks
        m_LinkLayerProxy = mock(LinkLayerProxy.class, withSettings().extraInterfaces(FactoryObjectProxy.class));
        m_FactReg = mock(FactoryRegistry.class);
        m_LinkLayerFactoryInternal = mock(FactoryInternal.class);
        m_ConfigurationAdmin = ConfigurationAdminMocker.createMockConfigAdmin();
        m_EventAdmin = mock(EventAdmin.class);
        m_Caps = mock(LinkLayerCapabilities.class);
        m_PowManInternal = mock(PowerManagerInternal.class);
        
        when(m_LinkLayerFactoryInternal.getLinkLayerCapabilities()).thenReturn(m_Caps);
        doReturn(LinkLayer.class.getName()).when(m_LinkLayerFactoryInternal).getProductType();
        
        m_SUT.initialize(m_FactReg, m_LinkLayerProxy, m_LinkLayerFactoryInternal, m_ConfigurationAdmin, m_EventAdmin, 
                m_PowManInternal, m_Uuid, m_Name, m_Pid, m_BaseType);       
    }
    
    /** 
    * Test perform BIT for a link layer.
    */
    @Test
    public void testPerformBit() throws CCommException
    {
        //test default
        assertThat(m_SUT.isPerformingBit(), is(false));

        //mock behavior
        when(m_LinkLayerProxy.onPerformBit()).thenReturn(LinkStatus.OK);
        doReturn(LinkLayerProxy.class.getName()).when(m_LinkLayerFactoryInternal).getProductType();
        when(m_Caps.isPerformBITSupported()).thenReturn(true);
        
        //perform the built in test
        LinkStatus status = m_SUT.performBit();
        assertThat(status, is(LinkStatus.OK));

        //verify
        verify(m_LinkLayerProxy).onPerformBit();
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_STATUS_CHANGED);
        assertThat((LinkLayerImpl)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is(m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is(m_Name));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.OK));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_Pid));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
    }

    /** 
     * Test perform BIT for a link layer.
     * Verify if an exception is thrown the status is {@link LinkStatus#LOST}.
     */
    @Test
    public void testPerformBitSupportedButException() throws CCommException
    {
        //test default
        assertThat(m_SUT.isPerformingBit(), is(false));

        //mock behavior
        when(m_LinkLayerProxy.onPerformBit()).
            thenThrow(new CCommException(FormatProblem.ADDRESS_MISMATCH));
        doReturn(LinkLayerProxy.class.getName()).when(m_LinkLayerFactoryInternal).getProductType();
        when(m_Caps.isPerformBITSupported()).thenReturn(true);
        
        //perform the built in test
        LinkStatus status = m_SUT.performBit();
        assertThat(status, is(LinkStatus.LOST));

        //verify
        verify(m_LinkLayerProxy).onPerformBit();
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_STATUS_CHANGED);
        assertThat((LinkLayerImpl)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is(m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is(m_Name));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_Pid));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
    }
     
   /** 
    * Test perform BIT for a link layer when BIT isn't supported.
    */
    @Test
    public void testPerformBITException()
    {
        //mock behavior
        when(m_LinkLayerFactoryInternal.getLinkLayerCapabilities()).thenReturn(m_Caps);
        when(m_Caps.isPerformBITSupported()).thenReturn(false);

        //perform the built in test
        try
        {
            m_SUT.performBit();
            fail("Expected exception because BIT is not supported");
        }
        catch (CCommException e)
        {
            //expected exception
        }

        //verify
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }

     /** 
     * Test set and get LinkStatus for a link layer.
     */
    @Test
    public void testLinkStatus()
    {
        m_SUT.setStatus(LinkStatus.LOST);
        assertThat(m_SUT.getLinkStatus(), is(LinkStatus.LOST));

        m_SUT.setStatus(LinkStatus.OK);
        assertThat(m_SUT.getLinkStatus(), is(LinkStatus.OK));
    }

    /**
     * Test that the link layer can post an event and all necessary properties are present.
     */
    @Test
    public void testPostEvent() throws CCommException, IOException, IllegalArgumentException, IllegalStateException, 
        FactoryException, ConfigurationException, AssetException
    {
        m_SUT.internalSetName("test-link-layer");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("prop1", 2);
        props.put("prop2", "test");
        m_SUT.postEvent("blah", props);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, "blah");
        assertThat((LinkLayer)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is((LinkLayer)m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("test-link-layer"));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_SUT.getPid()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
    }

    /**
     * Verify event posting properties when a link layer receives data.
     */
    @Test
    public void testPostReceiveEvent() throws CCommException, IOException, IllegalArgumentException, 
        IllegalStateException, FactoryException, ConfigurationException, AssetException
    {
        final String AddressType = Address.class.toString();
        
        Address sourceAddress = mock(Address.class);       
        Map<String, Object> addressEventProps = new HashMap<String, Object>();
        addressEventProps.put("source." + Address.EVENT_PROP_ADDRESS_SUFFIX, sourceAddress);
        addressEventProps.put("source." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX, AddressType);
        addressEventProps.put("source." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX, "test-source");
        when(sourceAddress.getEventProperties("source.")).thenReturn(addressEventProps);
        when(sourceAddress.getName()).thenReturn("test-source");
        
        Address destAddress = mock(Address.class);
        addressEventProps = new HashMap<String, Object>();
        addressEventProps.put("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX, destAddress);
        addressEventProps.put("dest." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX, AddressType);
        addressEventProps.put("dest." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX, "test-dest");
        when(destAddress.getEventProperties("dest.")).thenReturn(addressEventProps);
        when(destAddress.getName()).thenReturn("test-dest");
        LinkFrame frame = mock(LinkFrame.class);
        
        m_SUT.internalSetName("test-link-layer");
        m_SUT.postReceiveEvent(sourceAddress, destAddress, frame);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_DATA_RECEIVED);
        assertThat((LinkLayer)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is((LinkLayer)m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("test-link-layer"));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_SUT.getPid()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
        assertThat((LinkFrame)event.getProperty(LinkLayer.EVENT_PROP_LINK_FRAME), is(frame));
        
        assertThat((Address)event.getProperty("source." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(sourceAddress));
        assertThat((String)event.getProperty("source." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX), 
                is(sourceAddress.getName()));
        assertThat((String)event.getProperty("source." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX), is(AddressType));
        
        assertThat((Address)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(destAddress));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX), 
                is(destAddress.getName()));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX), is(AddressType));
        
        // verify no error if the addrs are empty values
        m_SUT.postReceiveEvent(null, destAddress, frame);
        m_SUT.postReceiveEvent(sourceAddress, null, frame);
    }
    
    /**
     * Verify exception if frame is null.
     */
    @Test
    public void testNullFrame()
    {
        //mocks
        Address sourceAddress = mock(Address.class); 
        Address destAddress = mock(Address.class);
        
        //act
        try
        {
            m_SUT.postReceiveEvent(sourceAddress, destAddress, null);
            fail("Expecting exception");
        }
        catch (NullPointerException e)
        {
            
        }
    }
    
    /**
     * Verify send called on layer proxy.
     * Verify event has correct props.
     */
    @Test
    public void testSend() throws CCommException, IOException, IllegalArgumentException, IllegalStateException,
        FactoryException, ConfigurationException, AssetException
    {
        //mocks
        Address destAddress = mock(Address.class);
        LinkFrame frame = mock(LinkFrame.class);
        
        final String addressType = Address.class.toString();
        
        Map<String, Object> addressEventProps = new HashMap<String, Object>();
        addressEventProps.put("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX, destAddress);
        addressEventProps.put("dest." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX, addressType);
        addressEventProps.put("dest." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX, "test-dest");
        when(destAddress.getEventProperties("dest.")).thenReturn(addressEventProps);
        when(destAddress.getName()).thenReturn("test-dest");
        
        //need to activate so that isActivated returns true
        m_SUT.activateLayer();
        m_SUT.send(frame, destAddress);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //one for activate, last is send
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_DATA_SENT);
        assertThat((LinkLayer)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is((LinkLayer)m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is(m_Name));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_Pid));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
        assertThat((LinkFrame)event.getProperty(LinkLayer.EVENT_PROP_LINK_FRAME), is(frame));

        assertThat((Address)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(destAddress));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX), 
                is(destAddress.getName()));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX), is(addressType));
        
        verify(m_LinkLayerProxy, times(1)).send(frame, destAddress);
    }
    
    /**
     * Verify that frame values are checked before invocation of send to the proxy.
     */
    @Test
    public void testSendNullFrame() throws CCommException
    {
        Address addr = mock(Address.class);
        
        //null for the frame
        try
        {
            m_SUT.send(null, addr);
            fail("Expecting exception");
        }
        catch (NullPointerException e)
        {
        }
    }
    
    /**
     * Verify that the link layer must be activated before being able to send.
     */
    @Test
    public void testSendNotActivated()
    {
        //mocks
        Address destAddress = mock(Address.class);
        LinkFrame frame = mock(LinkFrame.class);
        
        //act
        try
        {
            m_SUT.send(frame, destAddress);
            fail("Expected exception because layer is not activated.");
        }
        catch (CCommException e)
        {
            //expected
        }
    }
    
    /**
     * Verify send called on layer proxy. Even if dest addr is null.
     * Verify event still posted and addr props are non-existent.
     */
    @Test
    public void testSendNullAddr() throws CCommException, IOException, IllegalArgumentException, IllegalStateException,
        FactoryException, ConfigurationException, AssetException
    {
        //mocks
        Address destAddress = null;
        LinkFrame frame = mock(LinkFrame.class);

        //need to activate so that isActivated returns true
        m_SUT.activateLayer();
        m_SUT.send(frame, destAddress);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //one for activate, last is send
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_DATA_SENT);
        assertThat((LinkLayer)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is((LinkLayer)m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is(m_Name));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_Pid));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
        assertThat((LinkFrame)event.getProperty(LinkLayer.EVENT_PROP_LINK_FRAME), is(frame));

        assertThat((Address)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(nullValue()));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX), 
                is(nullValue()));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX), is(nullValue()));
        
        verify(m_LinkLayerProxy, times(1)).send(frame, destAddress);
    }
    
    /**
     * Verify getting the static mtu value.
     */
    @Test
    public void testGetStaticMtu()
    {
        when(m_LinkLayerFactoryInternal.getCapabilities()).thenReturn(m_Caps);
        when(m_Caps.isStaticMtu()).thenReturn(true);
        when(m_Caps.getMtu()).thenReturn(2);
        
        assertThat(m_SUT.getMtu(), is(2));
    }
    
    /**
     * Verify getting the dynamic mtu value.
     */
    @Test
    public void testGetDynamicMtu()
    {
        when(m_LinkLayerFactoryInternal.getCapabilities()).thenReturn(m_Caps);
        when(m_Caps.isSetStaticMtu()).thenReturn(false);
        when(m_LinkLayerProxy.getDynamicMtu()).thenReturn(5);
        
        assertThat(m_SUT.getMtu(), is(5));
    }
    
    /**
     * Verify event when layer is activated.
     */
    @Test
    public void testOnActivateEvent()
    {
        m_SUT.activateLayer();
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //one for activate, last is send
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_ACTIVATED);
        assertThat((LinkLayer)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is((LinkLayer)m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is(m_Name));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_Pid));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
    }
    
    /**
     * Verify event when layer is deactivated.
     */
    @Test
    public void testOnDeactivateEvent()
    {
        m_SUT.deactivateLayer();
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //one for activate, last is send
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, LinkLayer.TOPIC_DEACTIVATED);
        assertThat((LinkLayer)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is((LinkLayer)m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is(m_Name));
        assertThat((LinkStatus)event.getProperty(LinkLayer.EVENT_PROP_LINK_STATUS), is(LinkStatus.LOST));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_Pid));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
    }
    
    /**
     * Verify link layer address available call upon proxy.
     */
    @Test
    public void testAddressAvailable()
    {
        Address addr = mock(Address.class);
        m_SUT.isAvailable(addr);
        
        verify(m_LinkLayerProxy).isAvailable(addr);
    }
    
    /**
     * Verify {@link LinkLayerImpl#getConfig()} will return an object with the default properties and set properties.
     */
    @Test
    public void testGetConfig() throws Exception
    {
        Dictionary<String, Object> table = new Hashtable<>();
        Configuration config = ConfigurationAdminMocker.addMockConfiguration(m_SUT);
        when(config.getProperties()).thenReturn(table);
        
        // verify defaults, ignore physical link name as it is required
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(0));
        assertThat(m_SUT.getConfig().retries(), is(2));
        
        // test overrides
        table.put(LinkLayerAttributes.CONFIG_PROP_PHYSICAL_LINK_NAME, "blah");
        table.put(LinkLayerAttributes.CONFIG_PROP_RETRIES, 3);
        table.put(LinkLayerAttributes.CONFIG_PROP_READ_TIMEOUT_MS, 500);
        assertThat(m_SUT.getConfig().physicalLinkName(), is("blah"));
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(500));
        assertThat(m_SUT.getConfig().retries(), is(3));        
    }
    
    /**
     * Verify the registry is called to delete the object.
     */
    @Test
    public void testDelete() throws Exception
    {
        m_SUT.delete();
        verify(m_FactReg).delete(m_SUT);
    }
    
    /**
     * Verify the registry is not called if link layer is active.
     */
    @Test
    public void testDelete_Activated() throws Exception
    {
        m_SUT.activateLayer();
        
        try
        {
            m_SUT.delete();
            fail("Should fail to delete as link layer is activated");
        }
        catch (IllegalStateException e)
        {
            
        }
        verify(m_FactReg, never()).delete(Mockito.any(FactoryObjectInternal.class));
    }
}
