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


import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CCommException.FormatProblem;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.ccomm.transport.TransportLayerAttributes;
import mil.dod.th.core.ccomm.transport.TransportLayerProxy;
import mil.dod.th.core.ccomm.transport.TransportPacket;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.core.ConfigurationAdminMocker;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.test.EventAdminVerifier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * @author dhumeniuk
 *
 */
public class TestTransportLayerImpl
{
    private TransportLayerImpl m_SUT;
    private ConfigurationAdmin m_ConfigurationAdmin;
    private UUID m_Uuid = UUID.randomUUID();
    private String m_Name = "name";
    private String m_Pid = "pid1";
    private String m_BaseType = "baseType";

    @Mock(extraInterfaces = FactoryObjectProxy.class)
    private TransportLayerProxy m_TransportLayerProxy;

    @Mock
    private FactoryRegistry<?> m_FactReg;

    @Mock
    private FactoryInternal m_TransportLayerFactoryInternal;

    @Mock
    private EventAdmin m_EventAdmin;

    @Mock
    private TransportLayerCapabilities m_Caps;

    @Mock
    private PowerManagerInternal m_PowManInternal;

    @Mock
    private WakeLock m_WakeLock;

    @Before
    public void setUp() throws IOException, InvalidSyntaxException, ConfigurationException, 
        IllegalArgumentException, IllegalStateException, FactoryException, AssetException, ClassNotFoundException
    {
        m_SUT = new TransportLayerImpl();
        
        //mocks
        MockitoAnnotations.initMocks(this);
        m_ConfigurationAdmin = ConfigurationAdminMocker.createMockConfigAdmin();

        when(m_TransportLayerFactoryInternal.getCapabilities()).thenReturn(m_Caps);
        when(m_TransportLayerFactoryInternal.getTransportLayerCapabilities()).thenReturn(m_Caps);
        doReturn(TransportLayer.class.getName()).when(m_TransportLayerFactoryInternal).getProductType();
        when(m_PowManInternal.createWakeLock(m_TransportLayerProxy.getClass(), m_SUT, "coreFactoryObject")).thenReturn(
                m_WakeLock);
        when(m_PowManInternal.createWakeLock(m_TransportLayerProxy.getClass(), m_SUT, "coreTransLayer")).thenReturn(
                m_WakeLock);
        when(m_PowManInternal.createWakeLock(m_TransportLayerProxy.getClass(), m_SUT, "coreTransLayerRecv")).thenReturn(
                m_WakeLock);
    }

    private void initTransportLayer(boolean isConnectionOriented)
    {
        when(m_Caps.isConnectionOriented()).thenReturn(isConnectionOriented);

        m_SUT.initialize(m_FactReg, m_TransportLayerProxy, m_TransportLayerFactoryInternal, m_ConfigurationAdmin, 
                m_EventAdmin, m_PowManInternal, m_Uuid, m_Name, m_Pid, m_BaseType);

        verify(m_PowManInternal).createWakeLock(m_TransportLayerProxy.getClass(), m_SUT, "coreTransLayer");
        verify(m_PowManInternal).createWakeLock(m_TransportLayerProxy.getClass(), m_SUT, "coreTransLayerRecv");
    }

    /**
     * Verify that the end receive method will post an event with all the properties set.
     */
    @Test
    public void testPostDataRecieved() 
        throws IllegalArgumentException, IllegalStateException, FactoryException, 
        IOException, ConfigurationException, AssetException
    {
        initTransportLayer(false);

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

        m_SUT.internalSetName("transport");
        m_SUT.beginReceiving();
        verify(m_WakeLock).activate();
        TransportPacket pkt = mock(TransportPacket.class);
        m_SUT.endReceiving(pkt, sourceAddress, destAddress);
        verify(m_WakeLock).cancel();
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, TransportLayer.TOPIC_PACKET_RECEIVED);
        
        // verify all properties are set per standard
        assertThat((Address)event.getProperty("source." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(sourceAddress));
        assertThat((String)event.getProperty("source." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX), is(AddressType));
        assertThat((String)event.getProperty("source." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX), is("test-source"));
        
        assertThat((Address)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_SUFFIX), is(destAddress));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_TYPE_SUFFIX), is(AddressType));
        assertThat((String)event.getProperty("dest." + Address.EVENT_PROP_ADDRESS_NAME_SUFFIX), is("test-dest"));
        
        assertThat((TransportLayerImpl)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is(m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("transport"));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_SUT.getPid()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
        assertThat((TransportPacket)event.getProperty(TransportLayer.EVENT_PROP_PACKET), is(pkt));
    }
    
    @Test
    public void testEndReceivingNullAddresses()
    {
        initTransportLayer(false);

        TransportPacket pkt = mock(TransportPacket.class);
        
        m_SUT.internalSetName("transport");
        m_SUT.beginReceiving();
        verify(m_WakeLock).activate();
        m_SUT.endReceiving(pkt, null, null);
        verify(m_WakeLock).cancel();
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, TransportLayer.TOPIC_PACKET_RECEIVED);
        
        //verify all properties are set correctly
        assertThat(event.getPropertyNames().length, is(8));
        assertThat((TransportLayerImpl)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ), is(m_SUT));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_TYPE), 
                is(m_SUT.getFactory().getProductType()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME), is("transport"));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_PID), is(m_SUT.getPid()));
        assertThat((String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE), 
                is(m_BaseType));
        assertThat((TransportPacket)event.getProperty(TransportLayer.EVENT_PROP_PACKET), is(pkt));
    }
    
    /**
     * Verify if the packet is null that an exception is thrown.
     */
    @Test
    public void testEndReceivingExceptionNullPacket()
    {
        initTransportLayer(false);

        Address sourceAddress = mock(Address.class);
        Address destAddress = mock(Address.class);
        try
        {
            m_SUT.beginReceiving();
            verify(m_WakeLock).activate();
            m_SUT.endReceiving(null, sourceAddress, destAddress);
            fail("expecting exception");
        }
        catch (NullPointerException e)
        {
            verify(m_WakeLock, never()).cancel();
        }
    }
    
    /**
     * Verify that a transport layer is able to attempt to send a byte buffer to a specific address.
     */
    @Test
    public void testSendByteBufferToAddress() throws CCommException
    {
        initTransportLayer(false);

        TransportPacket pkt = mock(TransportPacket.class);
        ByteBuffer value = mock(ByteBuffer.class);
        when(pkt.getPayload()).thenReturn(value);
        Address addr = mock(Address.class);
        
        //act
        m_SUT.send(pkt, addr);
        
        //verify
        verify(m_WakeLock).activate();
        verify(m_TransportLayerProxy).send(value, addr);
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testSendByteBufferToAddressInvalid() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        TransportPacket pkt = mock(TransportPacket.class);
        Address addr = mock(Address.class);
        
        //act
        try
        {
            m_SUT.send(pkt, addr);
            fail("Expected IllegalStateException not thrown.");
        }
        catch (IllegalStateException e)
        {
        }
        
        //verify
        assertThat(m_SUT.isTransmitting(), is(false));
        verify(m_WakeLock, never()).activate();
        verify(m_WakeLock, never()).cancel();
    }

    /**
     * Verify that a transport layer is able to attempt to send a byte buffer to a connection oriented transport layer.
     */
    @Test
    public void testSendByteBuffer() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        TransportPacket pkt = mock(TransportPacket.class);
        ByteBuffer value = mock(ByteBuffer.class);
        when(pkt.getPayload()).thenReturn(value);
        
        //act
        m_SUT.send(pkt);
        
        //verify
        verify(m_WakeLock).activate();
        verify(m_TransportLayerProxy).send(value);
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testSendByteBufferInvalid() throws CCommException
    {
        initTransportLayer(false);

        TransportPacket pkt = mock(TransportPacket.class);
        
        //act
        try
        {
            m_SUT.send(pkt);
            fail("Expected IllegalStateException not thrown.");
        }
        catch (IllegalStateException e)
        {
        }
        
        //verify
        assertThat(m_SUT.isTransmitting(), is(false));
        verify(m_WakeLock, never()).activate();
        verify(m_WakeLock, never()).cancel();
    }

    @Test
    public void testSendByteBufferToAddressException() throws CCommException
    {
        initTransportLayer(false);

        TransportPacket pkt = mock(TransportPacket.class);
        ByteBuffer value = mock(ByteBuffer.class);
        when(pkt.getPayload()).thenReturn(value);
        Address addr = mock(Address.class);
        
        doThrow(new CCommException(FormatProblem.TIMEOUT)).when(m_TransportLayerProxy).send(value, addr);
        
        //act
        try
        {
            m_SUT.send(pkt, addr);
            fail("Expected CCommException not thrown.");
        }
        catch (CCommException e)
        {
        }
        
        //verify
        assertThat(m_SUT.isTransmitting(), is(false));
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testSendByteBufferException() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        TransportPacket pkt = mock(TransportPacket.class);
        ByteBuffer value = mock(ByteBuffer.class);
        when(pkt.getPayload()).thenReturn(value);
       
        doThrow(new CCommException(FormatProblem.TIMEOUT)).when(m_TransportLayerProxy).send(value);
        
        //act
        try
        {
            m_SUT.send(pkt);
            fail("Expected CCommException not thrown.");
        }
        catch (CCommException e)
        {
        }
        
        // verify
        assertThat(m_SUT.isTransmitting(), is(false));
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify ability to connect a transport layer.
     */
    @Test
    public void testConnect() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        Address address = mock(Address.class);
        
        //act
        m_SUT.connect(address);
        
        //verify
        verify(m_WakeLock).activate();
        verify(m_TransportLayerProxy).connect(address);
        verify(m_WakeLock).cancel();

        try
        {
            doThrow(new IllegalStateException("Already connected")).when(m_TransportLayerProxy).connect(address);

            m_SUT.connect(address);
            fail("Expected IllegalStateException not thrown.");
        }
        catch (IllegalStateException e)
        {
            //verify
        }
    }
    
    @Test
    public void testConnectInvalid() throws CCommException
    {
        initTransportLayer(false);

        Address address = mock(Address.class);
        
        //act
        try
        {
            m_SUT.connect(address);
            fail("Expected IllegalStateException not thrown");
        }
        catch (IllegalStateException e)
        {
            //verify
            verify(m_WakeLock, never()).activate();
            verify(m_WakeLock, never()).cancel();
        }
    }
    
    @Test
    public void testConnectException() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        Address address = mock(Address.class);
        doThrow(new CCommException(FormatProblem.INACTIVE)).when(m_TransportLayerProxy).connect(address);
               
        //act
        try
        {
            m_SUT.connect(address);
            fail("Expected CCommException not thrown");
        }
        catch (CCommException e)
        {
            //verify
            verify(m_WakeLock).activate();
            verify(m_WakeLock).cancel();
        }
    }
    
    /**
     * Verify ability to disconnect a transport layer.
     */
    @Test
    public void testDisconnect() throws IllegalStateException, CCommException
    {
        initTransportLayer(true); // connection oriented

        //act
        m_SUT.disconnect();
        
        //verify
        verify(m_WakeLock).activate();
        verify(m_TransportLayerProxy).disconnect();
        verify(m_WakeLock).cancel();

        try
        {
            doThrow(new IllegalStateException("Not connected")).when(m_TransportLayerProxy).disconnect();

            m_SUT.disconnect();
            fail("Expected IllegalStateException not thrown.");
        }
        catch (IllegalStateException e)
        {
            //verify
            verify(m_WakeLock, times(2)).activate();
            verify(m_WakeLock, times(2)).cancel();
        }
    }
    
    @Test
    public void testDisonnectInvalid() throws CCommException
    {
        initTransportLayer(false);

        //act
        try
        {
            m_SUT.disconnect();
            fail("Expected IllegalStateException not thrown");
        }
        catch (IllegalStateException e)
        {
            //verify
            verify(m_WakeLock, never()).activate();
            verify(m_WakeLock, never()).cancel();
        }
    }
    
    @Test
    public void testDisconnectException() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        doThrow(new CCommException(FormatProblem.INACTIVE)).when(m_TransportLayerProxy).disconnect();
        
        //act
        try
        {
            m_SUT.disconnect();
            fail("Expected CCommException not thrown");
        }
        catch (CCommException e)
        {
            //verify
            verify(m_WakeLock).activate();
            verify(m_WakeLock).cancel();
        }
    }
    
    /**
     * Verify ability to shutdown a transport layer deactivates link layer.
     */
    @Test
    public void testShutdown() throws CCommException
    {
        initTransportLayer(false);

        //setup
        LinkLayer link = mock(LinkLayer.class);
        m_SUT.setLinkLayer(link);
        
        //act
        m_SUT.shutdown();
        
        //verify
        verify(m_TransportLayerProxy).onShutdown();
        verify(link).deactivateLayer();
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify ability to shutdown a transport layer that has no link layer
     */
    @Test
    public void testShutdownNoLink() throws CCommException
    {
        initTransportLayer(false);

        //setup assert
        assertThat(m_SUT.getLinkLayer(), is((LinkLayer)null));
        
        //act
        m_SUT.shutdown();
        
        //verify, on shutdown is called last
        verify(m_WakeLock).activate();
        verify(m_TransportLayerProxy).onShutdown();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testShutdownConnected() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        //mock
        when(m_TransportLayerProxy.isConnected()).thenReturn(true);
        
        //act
        m_SUT.shutdown();
        
        //verify
        verify(m_WakeLock, times(2)).activate();
        verify(m_TransportLayerProxy).disconnect();
        verify(m_TransportLayerProxy).onShutdown();
        verify(m_WakeLock, times(2)).activate();
    }
    
    @Test
    public void testShutdownDisconnected() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        //mock
        when(m_TransportLayerProxy.isConnected()).thenReturn(false);
        
        //act
        m_SUT.shutdown();
        
        //verify
        verify(m_TransportLayerProxy, never()).disconnect();
        verify(m_WakeLock).activate();
        verify(m_TransportLayerProxy).onShutdown();
        verify(m_WakeLock).cancel();
    }
    
    /**
     * Verify begin receiving call sets receiving flag to true.
     */
    @Test
    public void testBeginReceiving()
    {
        initTransportLayer(false);

        m_SUT.beginReceiving();
        assertThat(m_SUT.isReceiving(), is(true));
        verify(m_WakeLock).activate();
        verify(m_WakeLock, never()).cancel();
    }
    
    /**
     * Verify call to proxy to find out if an address is available.
     */
    @Test
    public void testIsAddressAvailable()
    {
        initTransportLayer(false);

        Address addr = mock(Address.class);
        m_SUT.isAvailable(addr);
        verify(m_TransportLayerProxy).isAvailable(addr);
    }
    
    @Test
    public void testIsAddressAvailableConnectionOriented()
    {
        initTransportLayer(true); // connection oriented

        Address addr = mock(Address.class);
        m_SUT.isAvailable(addr);
        verify(m_TransportLayerProxy, never()).isAvailable(addr);
        verify(m_TransportLayerProxy).isConnected();
    }
    
    /**
     * Verify call to proxy to find out is the transport layer is connected.
     */
    @Test
    public void testIsConnected() throws CCommException
    {
        initTransportLayer(true); // connection oriented

        Address address = mock(Address.class);
        when(m_TransportLayerProxy.isConnected()).thenReturn(true);

        // act
        m_SUT.connect(address);
        
        // verify
        assertThat(m_SUT.isConnected(), is(true));
    }
    
    @Test
    public void testIsConnectedConnectionLess()
    {
        initTransportLayer(false);

        // act
        boolean result = m_SUT.isConnected();
        
        // verify
        verify(m_TransportLayerProxy, never()).isConnected();
        assertThat(result, is(false));
    }
    
    @Test
    public void testIsTransmitting() throws IllegalStateException, CCommException
    {
        initTransportLayer(true); // connection oriented

        TransportPacket pkt = mock(TransportPacket.class);
        m_SUT.send(pkt);
        
        assertThat(m_SUT.isTransmitting(), is(false));
    }
    
    /**
     * Verify {@link TransportLayerImpl#getConfig()} will return an object with the default properties and set 
     * properties.
     */
    @Test
    public void testGetConfig() throws Exception
    {
        initTransportLayer(false);

        Dictionary<String, Object> table = new Hashtable<>();
        Configuration config = ConfigurationAdminMocker.addMockConfiguration(m_SUT);
        when(config.getProperties()).thenReturn(table);
        
        // verify defaults
        assertThat(m_SUT.getConfig().linkLayerName(), is(""));
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(0));
        
        // test overrides
        table.put(TransportLayerAttributes.CONFIG_PROP_LINK_LAYER_NAME, "blah");
        table.put(TransportLayerAttributes.CONFIG_PROP_READ_TIMEOUT_MS, 500);
        assertThat(m_SUT.getConfig().linkLayerName(), is("blah"));
        assertThat(m_SUT.getConfig().readTimeoutMs(), is(500));
    }
    
    /**
     * Verify the registry is called to delete the object if the link layer is not active.
     */
    @Test
    public void testDelete_LinkLayerDeactivated() throws Exception
    {
        initTransportLayer(false);
        
        LinkLayer linkLayer = mock(LinkLayer.class);
        m_SUT.setLinkLayer(linkLayer);
        when(linkLayer.isActivated()).thenReturn(false);
        
        m_SUT.delete();
        
        verify(m_FactReg).delete(m_SUT);
        verify(m_TransportLayerProxy).onShutdown();
        verify(m_PowManInternal, times(3)).deleteWakeLock(m_WakeLock);
    }
    
    /**
     * Verify the transport layer is not removed if link layer is active.
     */
    @Test
    public void testDelete_LinkLayerActivated() throws Exception
    {
        initTransportLayer(false);
        
        LinkLayer linkLayer = mock(LinkLayer.class);
        m_SUT.setLinkLayer(linkLayer);
        when(linkLayer.isActivated()).thenReturn(true);
        
        try
        {
            m_SUT.delete();
            fail("Should fail to delete as link layer is activated");
        }
        catch (IllegalStateException e)
        {
            
        }
        verify(m_FactReg, never()).delete(Mockito.any(FactoryObjectInternal.class));
        verify(m_PowManInternal, never()).deleteWakeLock(m_WakeLock);
    }
}
