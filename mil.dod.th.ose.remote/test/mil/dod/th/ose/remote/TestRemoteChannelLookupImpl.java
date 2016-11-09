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
package mil.dod.th.ose.remote;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace.EncryptionInfoMessageType;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.GetEncryptionTypeResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.api.RemoteSettings.EncryptionMode;
import mil.dod.th.ose.remote.proto.PersistSystemChannel.SocketChannelType;
import mil.dod.th.ose.remote.proto.PersistSystemChannel.SystemChannels;
import mil.dod.th.ose.remote.proto.PersistSystemChannel.TransportChannelType;
import mil.dod.th.ose.remote.RemoteChannelLookupImpl.EventHandlerImpl;
import mil.dod.th.ose.remote.transport.TransportChannelImpl;
import mil.dod.th.ose.test.ComponentFactoryMocker;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentFactory;
import org.osgi.service.component.ComponentInstance;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Test that the remote channel look up service is able to sync and manage various {@link RemoteChannel}s for multiple 
 * {@link mil.dod.th.core.system.TerraHarvestSystem} IDs. 
 * @author Dave Humeniuk
 *
 */
public class TestRemoteChannelLookupImpl
{
    private RemoteChannelLookupImpl m_SUT;
    private ComponentFactory m_ClientSocketChannelFactory;
    private ComponentFactory m_ServerSocketChannelFactory;
    private ComponentFactory m_TransportChannelFactory;
    private ComponentInstance m_ClientSocketChannelInstance;
    private SocketChannel m_ClientSocketChannel;
    private ComponentInstance m_ServerSocketChannelInstance;
    private SocketChannel m_ServerSocketChannel;
    private ComponentInstance m_TransportChannelInstance;
    private TransportChannel m_TransportChannel;
    private PersistentDataStore m_PersistentDataStore;
    private EventAdmin m_EventAdmin;
    private MessageRouterInternal m_RouterInternal;
    private TerraHarvestSystem m_ThSystem;
    private RemoteSettings m_RemoteSettings;
    
    private int m_SystemId = 5;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new RemoteChannelLookupImpl();
        
        m_EventAdmin = mock(EventAdmin.class);
        m_RouterInternal = mock(MessageRouterInternal.class);
        m_ThSystem = mock(TerraHarvestSystem.class);
        m_RemoteSettings = mock(RemoteSettings.class);
        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageRouter(m_RouterInternal);
        m_SUT.setTerraHarvestSystem(m_ThSystem);
        m_SUT.setRemoteSettings(m_RemoteSettings);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        
        // mock component factories
        m_ClientSocketChannelFactory = mock(ComponentFactory.class);
        m_ClientSocketChannelInstance = mock(ComponentInstance.class);
        
        when(m_ThSystem.getId()).thenReturn(m_SystemId);
        when(m_RemoteSettings.getEncryptionMode()).thenReturn(EncryptionMode.AES_ECDH_ECDSA);
        
        m_ClientSocketChannel = mock(ClientSocketChannel.class);
        when(m_ClientSocketChannelFactory.newInstance(Mockito.any(Dictionary.class)))
            .thenReturn(m_ClientSocketChannelInstance);
        when(m_ClientSocketChannelInstance.getInstance()).thenReturn(m_ClientSocketChannel);
        m_SUT.setClientSocketChannelFactory(m_ClientSocketChannelFactory);
        
        m_ServerSocketChannelFactory = mock(ComponentFactory.class);
        m_ServerSocketChannelInstance = mock(ComponentInstance.class);
        
        m_ServerSocketChannel = mock(ServerSocketChannel.class);
        when(m_ServerSocketChannelFactory.newInstance(Mockito.any(Dictionary.class)))
            .thenReturn(m_ServerSocketChannelInstance);
        when(m_ServerSocketChannelInstance.getInstance()).thenReturn(m_ServerSocketChannel);
        m_SUT.setServerSocketChannelFactory(m_ServerSocketChannelFactory);
        
        m_TransportChannelFactory = mock(ComponentFactory.class);
        m_TransportChannelInstance = mock(ComponentInstance.class);
        m_TransportChannel = mock(TransportChannel.class);
        when(m_TransportChannelFactory.newInstance(Mockito.any(Dictionary.class)))
            .thenReturn(m_TransportChannelInstance);
        when(m_TransportChannelInstance.getInstance()).thenReturn(m_TransportChannel);
        m_SUT.setTransportChannelFactory(m_TransportChannelFactory);
        
        // mock out other service binding
        m_PersistentDataStore = mock(PersistentDataStore.class);
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
    }
    
    /**
     * Verify that this will create a new server socket channel, no need to check lookup though as it won't be there 
     * yet.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testNewServerSocketChannel()
    {
        // mock the input
        Socket socket = mock(Socket.class);
        
        // run the method to test
        SocketChannel channel = m_SUT.newServerSocketChannel(socket);
        
        // should be the socket channel that gets created by the factory
        ArgumentCaptor<Dictionary> dictionaryCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ServerSocketChannelFactory).newInstance(dictionaryCaptor.capture());
        assertThat(channel, is(m_ServerSocketChannel));
        // verify socket is created with proper properties
        assertThat((Socket)dictionaryCaptor.getValue().get(ServerSocketChannel.SOCKET_PROP_KEY), is(socket));
    }

    /**
     * Verify syncing a channel will allow it to be retrieved later.
     * 
     * Verify syncing channel to a new id is handled correctly (should move channel)
     */
    @Test
    public void testSyncChannel()
    {
        // mock channel used for input
        RemoteChannel channel = mock(RemoteChannel.class);
        
        int systemId = 100;
        
        // sync channel
        m_SUT.syncChannel(channel, systemId);
        
        // verify channel is the same as one sent in
        assertThat(m_SUT.getChannel(systemId), is(channel));
        
        // sync to a different system id, shouldn't be available at the old one anymore
        int otherSystemId = 200;
        m_SUT.syncChannel(channel, otherSystemId);
        
        // verify no longer associated with old one, but is with new one
        assertThat(m_SUT.getChannels(systemId).size(), is(0));
        assertThat(m_SUT.getChannels(otherSystemId).size(), is(1));
        assertThat(m_SUT.getChannel(otherSystemId), is(channel));
        
        // sync to same channel again
        m_SUT.syncChannel(channel, otherSystemId);
        
        // verify same amount of channels in list
        assertThat(m_SUT.getChannels(otherSystemId).size(), is(1));
    }
    
    /**
     * Verify that the syncChannel methods persist the channel when appropriate.
     */
    @Test
    public void testSyncChannelPersist()
    {
        //Mock the needed remote channels.
        RemoteChannel persistedChannel = mock(RemoteChannel.class);
        RemoteChannel nonPersistedChannel = mock(RemoteChannel.class);
        RemoteChannel alwaysPersistedChannel = mock(RemoteChannel.class);
        
        //Define remote system IDs
        Integer systemId1 = 75;
        Integer systemId2 = 150;
        Integer systemId3 = 225;
        
        //Sync channels
        m_SUT.syncChannel(persistedChannel, systemId1, true);
        m_SUT.syncChannel(nonPersistedChannel, systemId2, false);
        m_SUT.syncChannel(alwaysPersistedChannel, systemId3);
        
        //Verify that all channels are added to the remote lookup regardless of whether or not they should be persisted.
        assertThat(m_SUT.getChannel(systemId1), is(persistedChannel));
        assertThat(m_SUT.getChannel(systemId2), is(nonPersistedChannel));
        assertThat(m_SUT.getChannel(systemId3), is(alwaysPersistedChannel));
        
        //Verify that only remote channels with system ID 1 and 3 are persisted.
        verify(m_PersistentDataStore).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId1.toString()), Mockito.any(byte[].class));
        verify(m_PersistentDataStore, never()).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId2.toString()), Mockito.any(byte[].class));
        verify(m_PersistentDataStore).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId3.toString()), Mockito.any(byte[].class));
    }

    /**
     * Verify syncing a client socket will allow it to be retrieved later.
     * 
     * Verify channel is not created if already in lookup
     * 
     * Verify syncing channel to a new id is handled correctly (should move channel)
     * 
     * Verify new channel is persisted in the list of system channels.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSyncClientSocketChannel() throws IllegalArgumentException, PersistenceFailedException, 
        InvalidProtocolBufferException
    {
        // mock the socket props used for syncing, have socket channel match with it
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        stubAsExistingSocketChannel(m_ClientSocketChannel);
        
        int systemId = 300;
        
        SocketChannel channel = m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        // should be the socket channel that gets created by the factory
        assertThat((SocketChannel)m_SUT.getChannel(systemId), is(channel));
        ArgumentCaptor<Dictionary> dictionaryCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ClientSocketChannelFactory).newInstance(dictionaryCaptor.capture());
        assertThat(channel, is(m_ClientSocketChannel));
        // verify transport channel is created with proper properties
        assertThat((String)dictionaryCaptor.getValue().get(ClientSocketChannel.HOST_PROP_KEY), is("host"));
        assertThat((Integer)dictionaryCaptor.getValue().get(ClientSocketChannel.PORT_PROP_KEY), is(100));
        
        //verify that the channel added topic is created
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getTopic(), is(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(300));
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), notNullValue());
        
        // syncing again should not create a new channel
        m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        channel = (SocketChannel)m_SUT.getChannel(systemId);
        verify(m_ClientSocketChannelFactory, times(1)).newInstance(Mockito.any(Dictionary.class)); // still holding at 1
        assertThat(channel, is(m_ClientSocketChannel));
        
        // sync to a different system id, shouldn't be available at the old one anymore
        int otherSystemId = 400;
        m_SUT.syncClientSocketChannel("host", 100, otherSystemId);
        
        try
        {
            m_SUT.getChannel(systemId);
            fail("Expecting exception since channel is no longer associated with this id");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        channel = (SocketChannel)m_SUT.getChannel(otherSystemId);
        verify(m_ClientSocketChannelFactory, times(1)).newInstance(Mockito.any(Dictionary.class)); // still holding at 1
        assertThat(channel, is(m_ClientSocketChannel));
        
        ArgumentCaptor<TerraHarvestMessage> messageCaptor = ArgumentCaptor.forClass(TerraHarvestMessage.class);
        verify(channel, times(3)).queueMessage(messageCaptor.capture());
        verify(m_ThSystem, times(3)).getId();
        verify(m_RemoteSettings, times(3)).getEncryptionMode();
        
        for (TerraHarvestMessage message : messageCaptor.getAllValues())
        {
            assertThat(message.getSourceId(), is(m_SystemId));
            assertThat(message.getDestId(), anyOf(equalTo(systemId), equalTo(otherSystemId)));
            TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
            assertThat(payload.getNamespace(), is(Namespace.EncryptionInfo));
            EncryptionInfoNamespace namespace = EncryptionInfoNamespace.parseFrom(payload.getNamespaceMessage());
            assertThat(namespace.getType(), is(EncryptionInfoMessageType.GetEncryptionTypeResponse));
            GetEncryptionTypeResponseData data = GetEncryptionTypeResponseData.parseFrom(namespace.getData());
            assertThat(data.getType(), is(EncryptType.AES_ECDH_ECDSA));
        }
    }
    
    /**
     * Verify that the syncClientSocketChannel methods persist the channel when appropriate.
     */
    @Test
    public void testSyncClientSocketChannelPersist()
    {
        //Mock the needed socket channels.
        SocketChannel persistedChannel = mock(SocketChannel.class);
        SocketChannel nonPersistedChannel = mock(SocketChannel.class);
        SocketChannel alwaysPersistedChannel = mock(SocketChannel.class);
        
        when(m_ClientSocketChannelInstance.getInstance()).thenReturn(persistedChannel, nonPersistedChannel, 
                alwaysPersistedChannel);
        
        stubSocketChannel(persistedChannel, "host1", 1000);
        stubSocketChannel(nonPersistedChannel, "host2", 1005);
        stubSocketChannel(alwaysPersistedChannel, "host3", 1010);

        //Define remote system IDs
        Integer systemId1 = 75;
        Integer systemId2 = 150;
        Integer systemId3 = 225;
        
        //Sync the socket channels
        SocketChannel returned1 = m_SUT.syncClientSocketChannel("host1", 1000, systemId1, true);
        SocketChannel returned2 = m_SUT.syncClientSocketChannel("host2", 1005, systemId2, false);
        SocketChannel returned3 = m_SUT.syncClientSocketChannel("host3", 1010, systemId3);
        
        //Verify that the appropriate channels are returned by the sync methods.
        assertThat(returned1, is(persistedChannel));
        assertThat(returned2, is(nonPersistedChannel));
        assertThat(returned3, is(alwaysPersistedChannel));
        
        //Verify that all channels are added to the remote lookup regardless of whether or not they should be persisted.
        assertThat((SocketChannel)m_SUT.getChannel(systemId1), is(persistedChannel));
        assertThat((SocketChannel)m_SUT.getChannel(systemId2), is(nonPersistedChannel));
        assertThat((SocketChannel)m_SUT.getChannel(systemId3), is(alwaysPersistedChannel));
        
        //Verify that only sockets channels with system ID 1 and 3 are persisted.
        verify(m_PersistentDataStore).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId1.toString()), Mockito.any(byte[].class));
        verify(m_PersistentDataStore, never()).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId2.toString()), Mockito.any(byte[].class));
        verify(m_PersistentDataStore).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId3.toString()), Mockito.any(byte[].class));
    }
    
    /**
     * Verify syncing a transport will allow it to be retrieved later.
     * 
     * Verify channel is not created if already in lookup
     * 
     * Verify syncing channel to a new id is handled correctly (should move channel)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testSyncTransportChannel()
    {
        // mock the transport layer and address used for syncing, have transport channel match with it
        stubTransportChannel(m_TransportChannel, "tl-1", "tl:1.2.3", "tl:1.2.4");
        stubAsExistingTransportChannel(m_TransportChannel);
        
        int systemId = 300;
        
        TransportChannel channel = m_SUT.syncTransportChannel("tl-1", "tl:1.2.3", "tl:1.2.4", systemId);
        
        // should be the transport channel that gets created by the factory
        assertThat((TransportChannel)m_SUT.getChannel(systemId), is(channel));
        ArgumentCaptor<Dictionary> dictionaryCaptor = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_TransportChannelFactory).newInstance(dictionaryCaptor.capture());
        assertThat(channel, is(m_TransportChannel));
        // verify transport channel is created with proper properties
        assertThat((String)dictionaryCaptor.getValue().get(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY), 
                is("tl-1"));
        assertThat((String)dictionaryCaptor.getValue().get(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY), 
                is("tl:1.2.3"));
        assertThat((String)dictionaryCaptor.getValue().get(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY), 
                is("tl:1.2.4"));
        
        //verify that the channel added topic is created
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getTopic(), is(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED));
        assertThat((Integer)event.getProperty(RemoteConstants.EVENT_PROP_SYS_ID), is(300));
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), notNullValue());
                
        // syncing again should not create a new channel
        m_SUT.syncTransportChannel("tl-1", "tl:1.2.3", "tl:1.2.4", systemId);
        
        channel = (TransportChannel)m_SUT.getChannel(systemId);
        verify(m_TransportChannelFactory, times(1)).newInstance(Mockito.any(Dictionary.class)); // still holding at 1
        assertThat(channel, is(m_TransportChannel));
        
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        // sync to a different system id, shouldn't be available at the old one anymore
        int otherSystemId = 400;
        m_SUT.syncTransportChannel("tl-1", "tl:1.2.3", "tl:1.2.4", otherSystemId);
        
        try
        {
            m_SUT.getChannel(systemId);
            fail("Expecting exception since channel is no longer associated with this id");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        channel = (TransportChannel)m_SUT.getChannel(otherSystemId);
        verify(m_TransportChannelFactory, times(1)).newInstance(Mockito.any(Dictionary.class)); // still holding at 1
        assertThat(channel, is(m_TransportChannel));
    }
    
    /**
     * Verify that the syncTransportChannel methods persist the channel when appropriate.
     */
    @Test
    public void testSyncTransportChannelPersist()
    {
        //Mock the needed transport channels.
        TransportChannel persistedChannel = mock(TransportChannel.class);
        TransportChannel nonPersistedChannel = mock(TransportChannel.class);
        TransportChannel alwaysPersistedChannel = mock(TransportChannel.class);
        
        when(m_TransportChannelInstance.getInstance()).thenReturn(persistedChannel, nonPersistedChannel, 
                alwaysPersistedChannel);
        
        stubTransportChannel(persistedChannel, "tl-1", "tl:1.2.3", "tl:1.2.4");
        stubTransportChannel(nonPersistedChannel, "t2-1", "t2:1.2.5", "t2:1.2.6");
        stubTransportChannel(alwaysPersistedChannel, "t3-1", "t3:1.2.7", "t3:1.2.8");
        
        //Define remote system IDs
        Integer systemId1 = 75;
        Integer systemId2 = 150;
        Integer systemId3 = 225;
        
        //Sync the transport channels
        TransportChannel returned1 = m_SUT.syncTransportChannel("t1-1", "t1:1.2.3", "t1:1.2.4", systemId1, true);
        TransportChannel returned2 = m_SUT.syncTransportChannel("t2-1", "t2:1.2.5", "t2:1.2.6", systemId2, false);
        TransportChannel returned3 = m_SUT.syncTransportChannel("t3-1", "t3:1.2.7", "t3:1.2.8", systemId3);
        
        //Verify that the appropriate channels are returned by the sync methods.
        assertThat(returned1, is(persistedChannel));
        assertThat(returned2, is(nonPersistedChannel));
        assertThat(returned3, is(alwaysPersistedChannel));
        
        //Verify that all channels are added to the remote lookup regardless of whether or not they should be persisted.
        assertThat((TransportChannel)m_SUT.getChannel(systemId1), is(persistedChannel));
        assertThat((TransportChannel)m_SUT.getChannel(systemId2), is(nonPersistedChannel));
        assertThat((TransportChannel)m_SUT.getChannel(systemId3), is(alwaysPersistedChannel));
        
        //Verify that only transport channels with system ID 1 and 3 are persisted.
        verify(m_PersistentDataStore).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId1.toString()), Mockito.any(byte[].class));
        verify(m_PersistentDataStore, never()).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId2.toString()), Mockito.any(byte[].class));
        verify(m_PersistentDataStore).persist(Mockito.any(Class.class), Mockito.any(UUID.class), 
                eq(systemId3.toString()), Mockito.any(byte[].class));
    }
    
    /**
     * Verify removing a channel will remove channel from list and dispose of instance.
     */
    @Test
    public void testRemoveServerSocketChannel()
    {
        // mock the sockets for input
        Socket socket1 = mock(Socket.class);
        
        int systemId = 100;
        
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        
        // create channels
        SocketChannel serverChannel = m_SUT.newServerSocketChannel(socket1);
        SocketChannel clientChannel = m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        // remove 1 channel, verify channel is disposed, then verify channel is still available
        // should be false since channel was never synced
        assertThat(m_SUT.removeChannel(serverChannel), is(false));
        verify(m_ServerSocketChannelInstance).dispose();
        assertThat((SocketChannel)m_SUT.getChannel(systemId), is(clientChannel));
        
        // remove other channel
        assertThat(m_SUT.removeChannel(clientChannel), is(true)); // should be true since it was synced to system id
        // instance should be disposed for each removed channel
        verify(m_ClientSocketChannelInstance).dispose();
        
        // should be no channels available now
        assertThat(m_SUT.getChannels(systemId).size(), is(0));
        
        // try removing a channel that is no longer managed at all
        assertThat(m_SUT.removeChannel(serverChannel), is(false));
    }

    /**
     * Verify getSocketChannel returns a channel if added only (not synced).

     * Verify null is returned for a socket channel not in the lookup.
     */
    @Test
    public void testGetSocketChannel()
    {
        // mock the sockets
        Socket socket1 = mock(Socket.class);
        
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        stubTransportChannel(m_TransportChannel, "test", "local", "remote");
        
        // replay
        m_SUT.newServerSocketChannel(socket1);
        m_SUT.syncClientSocketChannel("host", 100, 1);
        // sync transport channel just to make sure those are ignored later
        m_SUT.syncTransportChannel("test", "local", "remote", 2);
        
        // mock channels to be completely different
        stubSocketChannel(m_ServerSocketChannel, "test-host1", 1000);
        stubSocketChannel(m_ClientSocketChannel, "test-host2", 2000);
        
        // verify channel is returned for the given socket
        assertThat(m_SUT.getSocketChannel("test-host1", 1000), is(m_ServerSocketChannel));
        assertThat(m_SUT.getSocketChannel("test-host2", 2000), is(m_ClientSocketChannel));
        
        // mock channels so port is the same
        stubSocketChannel(m_ServerSocketChannel, "test-host1", 1000);
        stubSocketChannel(m_ClientSocketChannel, "test-host2", 1000);
        
        // verify channel is returned for the given socket
        assertThat(m_SUT.getSocketChannel("test-host1", 1000), is(m_ServerSocketChannel));
        assertThat(m_SUT.getSocketChannel("test-host2", 1000), is(m_ClientSocketChannel));
        
        // mock channels so hosts are the same
        stubSocketChannel(m_ServerSocketChannel, "test-host1", 1000);
        stubSocketChannel(m_ClientSocketChannel, "test-host1", 2000);
        
        // verify channel is returned for the given socket
        assertThat(m_SUT.getSocketChannel("test-host1", 1000), is(m_ServerSocketChannel));
        assertThat(m_SUT.getSocketChannel("test-host1", 2000), is(m_ClientSocketChannel));
        
        // verify null returned if not found
        assertThat(m_SUT.getSocketChannel("test-host-blah", 5000), is(nullValue()));
    }
    
    /**
     * Verify getTransportChannel returns a channel if added only (not synced).

     * Verify null is returned for transport channel not in the lookup.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetTransportChannel()
    {
        // mock factory specially to create unique instances for both calls
        ComponentInstance transportChannelInstance1 = mock(ComponentInstance.class);
        TransportChannel channel1 = mock(TransportChannel.class);
        when(transportChannelInstance1.getInstance()).thenReturn(channel1);
        ComponentInstance transportChannelInstance2 = mock(ComponentInstance.class);
        TransportChannel channel2 = mock(TransportChannel.class);
        when(transportChannelInstance2.getInstance()).thenReturn(channel2);
        when(m_TransportChannelFactory.newInstance(Mockito.any(Dictionary.class)))
            .thenReturn(transportChannelInstance1, transportChannelInstance2);
        
        stubTransportChannel(channel1, "test", "local", "remote");
        stubTransportChannel(channel2, "test2", "local2", "remote2");
        
        // replay
        m_SUT.syncTransportChannel("test", "local", "remote", 2);
        m_SUT.syncTransportChannel("test2", "local2", "remote2", 2);
        // sync socket channel just to make sure those are ignored later
        Socket socket = mock(Socket.class);
        m_SUT.newServerSocketChannel(socket);
        
        // mock channels to be completely different
        when(channel1.getTransportLayerName()).thenReturn("tl1");
        when(channel1.getLocalMessageAddress()).thenReturn("local1");
        when(channel1.getRemoteMessageAddress()).thenReturn("remote1");
        when(channel2.getTransportLayerName()).thenReturn("tl2");
        when(channel2.getLocalMessageAddress()).thenReturn("local2");
        when(channel2.getRemoteMessageAddress()).thenReturn("remote2");
        
        // verify channel is returned for the given transport info
        assertThat(m_SUT.getTransportChannel("tl1", "local1", "remote1"), is(channel1));
        assertThat(m_SUT.getTransportChannel("tl2", "local2", "remote2"), is(channel2));
        
        // mock channels so layers are the same
        when(channel1.getTransportLayerName()).thenReturn("tl-same");
        when(channel1.getLocalMessageAddress()).thenReturn("local1");
        when(channel1.getRemoteMessageAddress()).thenReturn("remote1");
        when(channel2.getTransportLayerName()).thenReturn("tl-same");
        when(channel2.getLocalMessageAddress()).thenReturn("local2");
        when(channel2.getRemoteMessageAddress()).thenReturn("remote2");
        
        // verify channel is returned for the given transport info to show differentiates by more than transport layers
        assertThat(m_SUT.getTransportChannel("tl-same", "local1", "remote1"), is(channel1));
        assertThat(m_SUT.getTransportChannel("tl-same", "local2", "remote2"), is(channel2));
        
        // verify null returned if not found
        assertThat(m_SUT.getTransportChannel("bad-tl", "foo", "bar"), is(nullValue()));
    }
    
    /**
     * Verify get channel call will return a channel if there are multiple channels for a given id. 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetChannel()
    {
        // first verify exception if invalid channel is given
        try
        {
            m_SUT.getChannel(2);
            fail("Expecting exception since channel is no longer associated with this id");
        }
        catch (IllegalArgumentException e)
        {
            
        }

        // will add each channel with this id
        int systemId = 100;
        
        // mock generic channels and 1 transport channel
        RemoteChannel channel1 = mock(RemoteChannel.class);
        RemoteChannel channel2 = mock(RemoteChannel.class);
        
        stubTransportChannel(m_TransportChannel, "tl-3", "tl3:4-5", "tl3:4-6");
        
        // sync all to the same system id
        m_SUT.syncChannel(channel1, systemId);
        m_SUT.syncChannel(channel2, systemId);
        m_SUT.syncTransportChannel("tl-3", "tl3:4-5", "tl3:4-6", systemId);
        
        // transport layer should be created since it is new
        verify(m_TransportChannelFactory, times(1)).newInstance(Mockito.any(Dictionary.class));
        
        assertThat(m_SUT.getChannel(systemId), is(notNullValue()));
    }
    
    /**
     * Verify the get channel system id will return the correct system id associate with the channel.
     */
    @Test
    public void testGetChannelSystemId()
    {
        // will add each channel with this id
        int systemId = 100;
        int systemId2 = 128;
        int systemId3 = 256;
        
        // mock generic channels
        RemoteChannel channel1 = mock(RemoteChannel.class);
        RemoteChannel channel2 = mock(RemoteChannel.class);
        RemoteChannel channel3 = mock(RemoteChannel.class);
        RemoteChannel channel4 = mock(RemoteChannel.class);
        RemoteChannel channel5 = mock(RemoteChannel.class);
        RemoteChannel channel6 = mock(RemoteChannel.class);
        
        
        // sync all the system id with the channel
        m_SUT.syncChannel(channel1, systemId);
        m_SUT.syncChannel(channel3, systemId);
        m_SUT.syncChannel(channel4, systemId2);
        m_SUT.syncChannel(channel5, systemId3);
        m_SUT.syncChannel(channel6, systemId3);
        
        //Verify the correct system ID is returned for the channel.
        assertThat(m_SUT.getChannelSystemId(channel1), is(100));
        assertThat(m_SUT.getChannelSystemId(channel3), is(100));
        assertThat(m_SUT.getChannelSystemId(channel4), is(128));
        assertThat(m_SUT.getChannelSystemId(channel5), is(256));
        assertThat(m_SUT.getChannelSystemId(channel6), is(256));
        
        //Verify an illegal argument exception is thrown for a channel that isn't associated with a system.
        try
        {
            m_SUT.getChannelSystemId(channel2);
            fail("Expecting exception since channel not associated with a system.");
        }
        catch (IllegalArgumentException e)
        {
            assertThat(e.getMessage(), is("Specified channel is not associated with a system!"));
        }
    }
    
    /**
     * Verify that a list of channels is returned if multiple channels to one controller are registered.
     */
    @Test
    public void testGetChannels()
    {
        // add channels with these ids
        int systemIdA = 897;
        int systemIdB = 1417;
        int systemIdC = 321;
        
        // mock socket factory to create a different channel for each call to new instance
        RemoteChannel socketChannel1 = mock(SocketChannel.class);
        RemoteChannel socketChannel2 = mock(SocketChannel.class);
        RemoteChannel socketChannel3 = mock(SocketChannel.class);
        when(m_ServerSocketChannelInstance.getInstance()).thenReturn(socketChannel1, socketChannel2, socketChannel3);
        
        stubTransportChannel(m_TransportChannel, "tl1", "tl1:333", "tl1:444");
        
        // sync channels to different ids
        m_SUT.syncChannel(socketChannel1, systemIdA);
        m_SUT.syncChannel(socketChannel2, systemIdB);
        m_SUT.syncTransportChannel("tl1", "tl1:333", "tl1:444", systemIdC);
        m_SUT.syncChannel(socketChannel3, systemIdC);
        m_SUT.syncTransportChannel("tl1", "tl1:333", "tl1:444", systemIdA);
                
        //verify that there are two channels in the list for system A
        assertThat(m_SUT.getChannels(systemIdA).size(), is(2));
        //verify that there is one channel in the list for system B
        assertThat(m_SUT.getChannels(systemIdB).size(), is(1));
        //verify that there are two channels in the list for system C
        assertThat(m_SUT.getChannels(systemIdC).size(), is(2));
        
        //try to get channels for a system id that doesn't have any channels, should return an empty list
        List<RemoteChannel> channels = m_SUT.getChannels(3698);
        assertThat(channels.isEmpty(), is(true));
    }
    
    /**
     * Verify that a map of all channels is returned if multiple channels to one controller are registered.
     */
    @Test
    public void testGetChannelsForSystem()
    {
        // add channels with these ids
        int systemIdA = 897;
        int systemIdB = 1417;
        int systemIdC = 321;
        
        // mock socket channels
        RemoteChannel socketChannel1 = mock(SocketChannel.class);
        RemoteChannel socketChannel2 = mock(SocketChannel.class);
        
        stubTransportChannel(m_TransportChannel, "hamburger", "cheese", "bacon");
        
        // sync channels to different ids
        m_SUT.syncChannel(socketChannel1, systemIdA);
        m_SUT.syncChannel(socketChannel2, systemIdB);
        m_SUT.syncTransportChannel("hamburger", "cheese", "bacon", systemIdC);
        m_SUT.syncChannel(socketChannel1, systemIdC);  // sync channel to new id
        m_SUT.syncTransportChannel("hamburger", "cheese", "bacon", systemIdA);
        
        // systemIdA should only have 1 as the other one was synced to a new location
        assertThat(m_SUT.getAllChannels().get(systemIdA).size(), is(1));
        assertThat(m_SUT.getAllChannels().get(systemIdB).size(), is(1));
        assertThat(m_SUT.getAllChannels().get(systemIdC).size(), is(2));
    }
    
    /**
     * Verify each component instance is disposed
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testDeactivate()
    {
        BundleContext context = mock(BundleContext.class);
        ServiceRegistration service = mock(ServiceRegistration.class);
        ServiceRegistration service2 = mock(ServiceRegistration.class);
        
        when(context.registerService(eq(EventHandler.class), Mockito.any(EventHandlerImpl.class), 
            Mockito.any(Dictionary.class))).thenReturn(service, service2);
        
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        stubTransportChannel(m_TransportChannel, "t12", "blah", "foo");
        
        m_SUT.activate(context);
        
        // create socket and transport channels
        m_SUT.syncClientSocketChannel("host", 100, 1);
        Socket serverSocket = mock(Socket.class);
        m_SUT.newServerSocketChannel(serverSocket);
        m_SUT.syncTransportChannel("t12", "blah", "foo", 2);
        
        m_SUT.deactivate();
        
        // verify that remote channel lookup is unbound from the message router
        verify(m_RouterInternal).unbindRemoteChannelLookup(m_SUT);
        
        // verify all channel instances are disposed, even ones that aren't synced
        verify(m_ServerSocketChannelInstance).dispose();
        verify(m_ClientSocketChannelInstance).dispose();
        verify(m_TransportChannelInstance).dispose();
    }
    
    /**
     * Verify that TOPIC_NEW_OR_CHANGED_CHANNEL_ID event will sync channel with the new system id. 
     * 
     * Verify that TOPIC_REMOVE_CHANNEL event will cause remove to be called.
     */
    @Test
    public void testHandleChannelEventClient()
    {
        BundleContext context = mock(BundleContext.class);
        RemoteChannel channel = mock(RemoteChannel.class);
        EventHandlerImpl handler = m_SUT.new EventHandlerImpl(context);
        
        //changed channel id
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(RemoteConstants.EVENT_PROP_CHANNEL, channel);
        properties.put(RemoteConstants.EVENT_PROP_SYS_ID, 1);
        
        Event event = new Event(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, properties);
        
        handler.handleEvent(event);
        
        assertThat(m_SUT.getChannel(1), is(channel));
        
        //remove channel
        properties.clear();
        properties.put(RemoteConstants.EVENT_PROP_CHANNEL, channel);
        
        event = new Event(RemoteConstants.TOPIC_REMOVE_CHANNEL, properties);
        
        handler.handleEvent(event);
        
        assertThat(m_SUT.getChannels(1).size(), is(0));
    }
    
    /**
     * Verify the activate method by ensuring channels are restored from the mocked XML object.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testActivate() throws IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        ComponentFactoryMocker.mockComponents(SocketChannel.class, m_ClientSocketChannelFactory, 4);
        ComponentFactoryMocker.mockComponents(TransportChannel.class, m_TransportChannelFactory, 4);
        
        // mock the unmarshaller and channel info
        SystemChannels.Builder systemChannels1 = SystemChannels.newBuilder().setSysId(1);
        SystemChannels.Builder systemChannels2 = SystemChannels.newBuilder().setSysId(2);
        
        // mock channel info for system id 1
        systemChannels1.addSocketChannel(SocketChannelType.newBuilder().setHost("hosta").setPort(50).build());
        systemChannels1.addSocketChannel(SocketChannelType.newBuilder().setHost("hostb").setPort(60).build());
        systemChannels1.addTransportChannel(TransportChannelType.newBuilder()
                    .setTransportName("tl1")
                    .setLocalAddress("la-1")
                    .setRemoteAddress("ra-1").build());
        systemChannels1.addTransportChannel(TransportChannelType.newBuilder()
                .setTransportName("tl1")
                .setLocalAddress("la-2")
                .setRemoteAddress("ra-b").build());
        
        // mock channel info for system id 2
        systemChannels2.addSocketChannel(SocketChannelType.newBuilder().setHost("hostc").setPort(50).build());
        systemChannels2.addSocketChannel(SocketChannelType.newBuilder().setHost("hostb").setPort(80).build());
        systemChannels2.addTransportChannel(TransportChannelType.newBuilder()
                .setTransportName("tl2")
                .setLocalAddress("la-1")
                .setRemoteAddress("ra-1").build());
        systemChannels2.addTransportChannel(TransportChannelType.newBuilder()
                .setTransportName("tl1")
                .setLocalAddress("la-1")
                .setRemoteAddress("ra-1").build());
        
        // mock persisted channels collection and query
        PersistentData persistentChannel1 = mock(PersistentData.class);
        when(persistentChannel1.getEntity()).thenReturn(systemChannels1.build().toByteArray());
        PersistentData persistentChannel2 = mock(PersistentData.class);
        when(persistentChannel2.getEntity()).thenReturn(systemChannels2.build().toByteArray());
        byte[] data3 = { 0x1, 0x2 }; // invalid data
        PersistentData persistentChannel3 = mock(PersistentData.class);
        when(persistentChannel3.getEntity()).thenReturn(data3);
        Collection<PersistentData> persistentData = new ArrayList<PersistentData>();
        persistentData.add(persistentChannel1);
        persistentData.add(persistentChannel2);
        persistentData.add(persistentChannel3);
        when(m_PersistentDataStore.query(RemoteChannelLookupImpl.class)).thenReturn(persistentData);
        
        BundleContext context = mock(BundleContext.class);
        m_SUT.activate(context);
        
        // verify that remote channel lookup is bound to the message router 
        verify(m_RouterInternal).bindRemoteChannelLookup(m_SUT);
        
        // verify socket channels are created
        ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_ClientSocketChannelFactory, times(4)).newInstance(props.capture());
        assertThat((String)props.getAllValues().get(0).get(ClientSocketChannel.HOST_PROP_KEY), is("hosta"));
        assertThat((Integer)props.getAllValues().get(0).get(ClientSocketChannel.PORT_PROP_KEY), is(50));
        assertThat((String)props.getAllValues().get(1).get(ClientSocketChannel.HOST_PROP_KEY), is("hostb"));
        assertThat((Integer)props.getAllValues().get(1).get(ClientSocketChannel.PORT_PROP_KEY), is(60));
        assertThat((String)props.getAllValues().get(2).get(ClientSocketChannel.HOST_PROP_KEY), is("hostc"));
        assertThat((Integer)props.getAllValues().get(2).get(ClientSocketChannel.PORT_PROP_KEY), is(50));
        assertThat((String)props.getAllValues().get(3).get(ClientSocketChannel.HOST_PROP_KEY), is("hostb"));
        assertThat((Integer)props.getAllValues().get(3).get(ClientSocketChannel.PORT_PROP_KEY), is(80));
        
        // verify transport channels are created
        verify(m_TransportChannelFactory, times(4)).newInstance(props.capture());
        assertThat((String)props.getAllValues().get(4).get(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY), is("tl1"));
        assertThat((String)props.getAllValues().get(4).get(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY), is("la-1"));
        assertThat((String)props.getAllValues().get(4).get(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY), is("ra-1"));
        assertThat((String)props.getAllValues().get(5).get(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY), is("tl1"));
        assertThat((String)props.getAllValues().get(5).get(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY), is("la-2"));
        assertThat((String)props.getAllValues().get(5).get(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY), is("ra-b"));
        assertThat((String)props.getAllValues().get(6).get(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY), is("tl2"));
        assertThat((String)props.getAllValues().get(6).get(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY), is("la-1"));
        assertThat((String)props.getAllValues().get(6).get(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY), is("ra-1"));
        assertThat((String)props.getAllValues().get(7).get(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY), is("tl1"));
        assertThat((String)props.getAllValues().get(7).get(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY), is("la-1"));
        assertThat((String)props.getAllValues().get(7).get(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY), is("ra-1"));
        
        // verify system id 1 channels are restored
        List<RemoteChannel> channels = m_SUT.getChannels(1);
        assertThat(channels.size(), is(4));  // should be 4 total
        
        // verify system id 2 channels are restored
        channels = m_SUT.getChannels(1);
        assertThat(channels.size(), is(4));  // should be 4 total
        
        // verify persist isn't called when restoring
        verify(m_PersistentDataStore, never()).persist(Mockito.any(Class.class), Mockito.any(UUID.class), anyString(), 
                Mockito.any(Serializable.class));
        verify(m_PersistentDataStore, never()).merge(Mockito.any(PersistentData.class));
    }
    
    /**
     * Verify a channel is persisted if it is the first in the channel set.
     * 
     * Verify socket channel can be persisted.
     * 
     * Verify other system list is cleared if system id is updated.
     */
    @Test
    public void testPersistence() throws IllegalArgumentException, PersistenceFailedException, 
        InvalidProtocolBufferException, ValidationFailedException
    {
        int systemId = 300;
        
        // mock out persistent data store with nothing in it yet
        Collection<PersistentData> channelDataList = new ArrayList<PersistentData>();
        when(m_PersistentDataStore.query(RemoteChannelLookupImpl.class, new Integer(systemId).toString()))
            .thenReturn(channelDataList);
        
        // mock channel that will be created
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        
        // sync channel to see if it is persisted
        m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        // construct the expected model object that will be created based on the current list
        SystemChannels expectedChannels = SystemChannels.newBuilder().setSysId(systemId)
                .addSocketChannel(SocketChannelType.newBuilder().setHost("host").setPort(100).build()).build();
        
        // verify data is persisted
        ArgumentCaptor<UUID> uuid1 = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<byte[]> byteCapture = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentDataStore).persist(eq(RemoteChannelLookupImpl.class), uuid1.capture(), 
                eq(new Integer(systemId).toString()), byteCapture.capture());   
        SystemChannels actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        // mock so that the existing channel shows up
        stubAsExistingSocketChannel(m_ClientSocketChannel);
        
        // mock existing channel data
        PersistentData channelData = mock(PersistentData.class);
        channelDataList.add(channelData);
        
        // sync to a different system id
        m_SUT.syncClientSocketChannel("host", 100, systemId + 1);
        
        // expected channels should be the same but have a different system id
        expectedChannels = SystemChannels.newBuilder().setSysId(systemId + 1)
                .addSocketChannel(SocketChannelType.newBuilder().setHost("host").setPort(100).build()).build();
        
        // verify object is persisted with different system id
        ArgumentCaptor<UUID> uuid2 = ArgumentCaptor.forClass(UUID.class);
        verify(m_PersistentDataStore).persist(eq(RemoteChannelLookupImpl.class), uuid2.capture(), 
                eq(new Integer(systemId + 1).toString()), byteCapture.capture());   
        actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        assertThat("UUID should be different as a new entry", uuid1, is(not(uuid2)));
        
        // expected channels should be empty for the old system id
        expectedChannels = SystemChannels.newBuilder().setSysId(systemId).build();
        
        // verify new channel data is merged with existing data
        verify(channelData).setEntity(byteCapture.capture());
        actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        verify(m_PersistentDataStore).merge(channelData);
    }
    
    /**
     * Verify syncChannel works the same way as syncing a specific channel if synced to new system id
     *  - old list is updated with channel removed
     *  - new list is persisted
     */
    @Test
    public void testPersistenceSyncUpdate() throws PersistenceFailedException, InvalidProtocolBufferException, 
        IllegalArgumentException, ValidationFailedException
    {
        int systemId = 300;
        
        // mock out persistent data store with nothing in it yet
        Collection<PersistentData> channelDataList = new ArrayList<PersistentData>();
        when(m_PersistentDataStore.query(RemoteChannelLookupImpl.class, new Integer(systemId).toString()))
            .thenReturn(channelDataList);
        
        // mock channel that will be created
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        
        // sync channel to see if it is persisted
        SocketChannel channel = m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        // mock so that the existing channel shows up
        stubAsExistingSocketChannel(m_ClientSocketChannel);
        
        // mock existing channel data
        PersistentData channelData = mock(PersistentData.class);
        channelDataList.add(channelData);
        
        // sync to a different system id, but this time call syncChannel
        m_SUT.syncChannel(channel, systemId + 1);
        
        // expected channels will be the same but with a different system id
        SystemChannels expectedChannels = SystemChannels.newBuilder().setSysId(systemId + 1)
                .addSocketChannel(SocketChannelType.newBuilder().setHost("host").setPort(100).build()).build();
        
        // verify object is persisted with different system id
        ArgumentCaptor<byte[]> byteCapture = ArgumentCaptor.forClass(byte[].class);
        verify(m_PersistentDataStore).persist(eq(RemoteChannelLookupImpl.class), Mockito.any(UUID.class), 
                eq(new Integer(systemId + 1).toString()), byteCapture.capture());   
        SystemChannels actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        // expected channels are empty for the old system id
        expectedChannels = SystemChannels.newBuilder().setSysId(systemId).build();
        
        // verify new channel data is merged with existing data
        verify(channelData).setEntity(byteCapture.capture());
        actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        verify(m_PersistentDataStore).merge(channelData);
    }
    
    /**
     * Verify a channel is persisted if it is the 2nd in the channel set (and that the first one is still in the list of
     * channels to persist).
     * 
     * Verify transport channel can be persisted.
     */
    @Test
    public void testPersistenceExistingChannel() throws PersistenceFailedException, InvalidProtocolBufferException, 
        IllegalArgumentException, ValidationFailedException
    {
        int systemId = 300;
        
        // mock channel that will be created
        when(m_TransportChannel.getTransportLayerName()).thenReturn("tl");
        when(m_TransportChannel.getLocalMessageAddress()).thenReturn("l-a");
        when(m_TransportChannel.getRemoteMessageAddress()).thenReturn("r-a");
        
        // mock existing entry in data store
        Collection<PersistentData> channelDataList = new ArrayList<PersistentData>();
        when(m_PersistentDataStore.query(RemoteChannelLookupImpl.class, new Integer(systemId).toString()))
            .thenReturn(channelDataList);
        PersistentData channelData = mock(PersistentData.class);
        channelDataList.add(channelData);
        
        // sync transport channel to see if it is persisted, original socket channel should be part of the model still
        m_SUT.syncTransportChannel("tl", "l-a", "r-a", systemId);
        
        // construct the expected model object that will be created based on the current list
        SystemChannels expectedChannels = SystemChannels.newBuilder().setSysId(systemId)
                .addTransportChannel(TransportChannelType.newBuilder()
                .setTransportName("tl")
                .setLocalAddress("l-a")
                .setRemoteAddress("r-a").build()).build();
        
        // verify channel data is merged with existing data
        ArgumentCaptor<byte[]> byteCapture = ArgumentCaptor.forClass(byte[].class);
        verify(channelData).setEntity(byteCapture.capture());
        SystemChannels actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        verify(m_PersistentDataStore).merge(channelData);
    }
    
    /**
     * Verify server socket channels are not persisted whether synced or removed.
     */
    @Test
    public void testPersistenceServerSocket() throws PersistenceFailedException, IllegalArgumentException, 
        ValidationFailedException
    {
        Socket socket = mock(Socket.class);
        SocketChannel channel = m_SUT.newServerSocketChannel(socket);
        
        // both sync and remove should not work with persistence store
        m_SUT.syncChannel(channel, 100);
        m_SUT.removeChannel(channel);
        
        // verify nothing is persisted
        verify(m_PersistentDataStore, never()).persist(Mockito.any(Class.class), Mockito.any(UUID.class), anyString(),
                Mockito.any(Serializable.class));
        
        // verify nothing is merged either, no updates
        verify(m_PersistentDataStore, never()).merge(Mockito.any(PersistentData.class));
    }
    
    /**
     * Verify removing a channel will update persisted value.
     * 
     * Verify persisted value contains an empty channel set.
     */
    @Test
    public void testPersistenceRemoval() throws PersistenceFailedException, InvalidProtocolBufferException, 
        IllegalArgumentException, ValidationFailedException
    {
        int systemId = 300;
        
        // mock out persistent data store with nothing in it yet
        Collection<PersistentData> channelDataList = new ArrayList<PersistentData>();
        when(m_PersistentDataStore.query(RemoteChannelLookupImpl.class, new Integer(systemId).toString()))
            .thenReturn(channelDataList);
        
        // mock channel that will be created
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        
        // sync channel to see if it is persisted
        SocketChannel channel = m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        // mock so that data is now in the store
        PersistentData channelData = mock(PersistentData.class);
        channelDataList.add(channelData);
        
        // remove channel make sure it is updated
        m_SUT.removeChannel(channel);
        
        // construct the expected model object that will be created based on the current list
        SystemChannels expectedChannels = SystemChannels.newBuilder().setSysId(systemId).build();
        
        // verify channel data is merged with existing data
        ArgumentCaptor<byte[]> byteCapture = ArgumentCaptor.forClass(byte[].class);
        verify(channelData).setEntity(byteCapture.capture());
        SystemChannels actualChannels = SystemChannels.parseFrom(byteCapture.getValue());
        assertThat(expectedChannels, is(actualChannels));
        
        verify(m_PersistentDataStore).merge(channelData);
    }
    
    /**
     * Verify {@link PersistentDataStore} exceptions thrown are handled properly.  If there is a failure, the channel 
     * will not be persisted, but the channel should still be available locally in memory.
     */
    @Test
    public void testPersistenceDataStoreException() throws PersistenceFailedException, IllegalArgumentException,
        ValidationFailedException
    {
        int systemId = 300;
        
        // mock data store to throw exception, should still be able to add item, just won't be persisted
        when(m_PersistentDataStore.persist(Mockito.any(Class.class), Mockito.any(UUID.class), anyString(),
                Mockito.any(Serializable.class))).thenThrow(new PersistenceFailedException());
        doThrow(new PersistenceFailedException()).when(m_PersistentDataStore).merge(Mockito.any(PersistentData.class));
        
        stubSocketChannel(m_ClientSocketChannel, "host", 100);
        
        // sync channel to see if it is added even if not persisted
        m_SUT.syncClientSocketChannel("host", 100, systemId);
        
        RemoteChannel channel = m_SUT.getChannel(systemId);
        assertThat(channel, is(notNullValue()));
        
        // mock existing entry in data store
        Collection<PersistentData> channelDataList = new ArrayList<PersistentData>();
        when(m_PersistentDataStore.query(RemoteChannelLookupImpl.class, new Integer(systemId).toString()))
            .thenReturn(channelDataList);
        PersistentData channelData = mock(PersistentData.class);
        channelDataList.add(channelData);
        
        // sync channel again, should hit merge failure this time
        m_SUT.syncClientSocketChannel("host", 100, systemId);   
    }
    
    /**
     * Verify a proper socket duplicate can be found.
     */
    @Test
    public void testCheckChannelSocketExists()
    {
        //Empty
        assertThat(m_SUT.checkChannelSocketExists("bob", 4000), is(false));
        
        //add a transport channel
        TransportChannel tChan = mock(TransportChannel.class);
        when(tChan.getLocalMessageAddress()).thenReturn("1111");
        when(tChan.getRemoteMessageAddress()).thenReturn("1111");
        when(tChan.getTransportLayerName()).thenReturn("name");
        
        m_SUT.syncChannel(tChan, 1);
        assertThat(m_SUT.checkChannelSocketExists("localhost", 12), is(false));
        assertThat(m_SUT.checkChannelSocketExists("localhost", 4000), is(false));
        
        //add a channel
        SocketChannel sChan = mockSocketChannel("localhost", 4000);
        
        m_SUT.syncChannel(sChan, 1);
        assertThat(m_SUT.checkChannelSocketExists("localhost", 12), is(false));
        assertThat(m_SUT.checkChannelSocketExists("bob", 4000), is(false));
        assertThat(m_SUT.checkChannelSocketExists("localhost", 4000), is(true));
    }
    
    /**
     * Verify that a proper transport duplicate can be found.
     */
    @Test
    public void testCheckChannelTransportExists()
    {
        //Empty
        assertThat(m_SUT.checkChannelTransportExists("1111", "1111"), is(false));
        
        //add a socket channel
        SocketChannel sChan = mockSocketChannel("localhost", 4000);
        
        m_SUT.syncChannel(sChan, 1);
        assertThat(m_SUT.checkChannelTransportExists("1111", "1111"), is(false));
        
        //add the transport and find
        TransportChannel tChan = mock(TransportChannel.class);
        when(tChan.getLocalMessageAddress()).thenReturn("1111");
        when(tChan.getRemoteMessageAddress()).thenReturn("1111");
        when(tChan.getTransportLayerName()).thenReturn("name");
        
        m_SUT.syncChannel(tChan, 1);
        assertThat(m_SUT.checkChannelTransportExists("3333", "3333"), is(false));
        assertThat(m_SUT.checkChannelTransportExists("1111", "3333"), is(false));
        assertThat(m_SUT.checkChannelTransportExists("3333", "1111"), is(false));
        assertThat(m_SUT.checkChannelTransportExists("1111", "1111"), is(true));
    }
    
    private SocketChannel mockSocketChannel(String host, int port)
    {
        SocketChannel sChan = mock(SocketChannel.class);
        stubSocketChannel(sChan, host, port);
        return sChan;
    }
    
    private void stubSocketChannel(SocketChannel channel, String host, int port)
    {
        when(channel.getHost()).thenReturn(host);
        when(channel.getPort()).thenReturn(port);
    }
    
    private void stubAsExistingSocketChannel(SocketChannel channel)
    {
        Map<String, Object> socketProps = new HashMap<String, Object>();
        socketProps.put(ClientSocketChannel.HOST_PROP_KEY, channel.getHost());
        socketProps.put(ClientSocketChannel.PORT_PROP_KEY, channel.getPort());
        when(channel.matches(socketProps)).thenReturn(true);
    }
    
    private void stubTransportChannel(TransportChannel channel, String transportName, String localAddress,
            String remoteAddress)
    {
        when(channel.getLocalMessageAddress()).thenReturn(localAddress);
        when(channel.getRemoteMessageAddress()).thenReturn(remoteAddress);
        when(channel.getTransportLayerName()).thenReturn(transportName);
    }
    
    private void stubAsExistingTransportChannel(TransportChannel channel)
    {
        Map<String, Object> transportProps = new HashMap<String, Object>();
        transportProps.put(TransportChannelImpl.TRANSPORT_NAME_PROP_KEY, channel.getTransportLayerName());
        transportProps.put(TransportChannelImpl.LOCAL_ADDRESS_PROP_KEY, channel.getLocalMessageAddress());
        transportProps.put(TransportChannelImpl.REMOTE_ADDRESS_PROP_KEY, channel.getRemoteMessageAddress());
        when(channel.matches(transportProps)).thenReturn(true);
    }
}
