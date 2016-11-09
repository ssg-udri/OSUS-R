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
package mil.dod.th.ose.gui.webapp.channel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.remote.ChannelStatus;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.types.remote.RemoteChannelTypeEnum;
import mil.dod.th.ose.gui.webapp.controller.ControllerStatus;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.service.component.ComponentException;

/**
 * Tests for the channel manager.
 * @author callen
 *
 */
public class TestChannelMgrImpl 
{
    private ChannelMgrImpl m_SUT;
    private RemoteChannelLookup m_RemoteChannelLookup;
    private GrowlMessageUtil m_GrowlUtil;

    @Before
    public void setUp() throws SecurityException, IllegalArgumentException
    {
        m_RemoteChannelLookup = mock(RemoteChannelLookup.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        
        m_SUT = new ChannelMgrImpl();
                
        m_SUT.setRemoteChannelLookup(m_RemoteChannelLookup);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
    }
    
    /**
     * Verify a create transport channel will sync request with remote channel lookup.
     * 
     * Verify system info request will be sent to new channel.
     * 
     * Verify controller manager is told about controller id.
     */
    @Test
    public void testCreateTransportChannel()
    {
        // mock remote channel
        TransportChannel channel = mock(TransportChannel.class);
        when(m_RemoteChannelLookup.syncTransportChannel("Sausage",  "98.76.54.132", "123.45.67.689", 1596))
            .thenReturn(channel);
        
        // replay
        m_SUT.createTransportChannel(1596, "Sausage", "98.76.54.132", "123.45.67.689");
        
        // verify channel is synced
        verify(m_RemoteChannelLookup).syncTransportChannel("Sausage",  "98.76.54.132", "123.45.67.689", 1596);
    }
    
    /**
     * Verify remote channel is created with the correct socket and then synced with lookup.
     * 
     * Verify system info request will be queued to new channel.
     * 
     * Verify controller manager is told about controller id.
     */
    @Test
    public void testCreateSocketChannel()
    {
        // mock lookup
        SocketChannel remoteChannel = mock(SocketChannel.class);
        when(m_RemoteChannelLookup.syncClientSocketChannel("localhost", 3001, 1957)).thenReturn(remoteChannel);
        
        // replay
        m_SUT.createSocketChannel(1957, "localhost", 3001);
        
        // verify channel is created and synced
        verify(m_RemoteChannelLookup).syncClientSocketChannel("localhost", 3001, 1957);
        
        // change sync call to throw exception
        when(m_RemoteChannelLookup.syncClientSocketChannel("localhost", 3001, 1957))
            .thenThrow(new ComponentException("fail"));
        m_SUT.createSocketChannel(1957, "localhost", 3001);
        
        // verify exception is caught and appropriately handled with a growl message
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), anyString(), anyString(),
            Mockito.any(ComponentException.class));
    }
    
    /**
     * Verify the removing a channel removes it from the lookup.
     * 
     * Verify that the controller for the channel is removed after the channel is removed.
     */
    @Test
    public void testRemoveChannel() throws IllegalArgumentException
    {
        // mock lookup
        SocketChannel socketChannel = createMockSocketChannel("host", 1000, ChannelStatus.Unknown);
        TransportChannel transportChannel = createMockTransportChannel("tl1", "local", 
                "remote", ChannelStatus.Unavailable);
        when(m_RemoteChannelLookup.getSocketChannel("host", 1000)).thenReturn(socketChannel);
        when(m_RemoteChannelLookup.getTransportChannel("tl1", "local", "remote")).thenReturn(transportChannel);
        
        //create channel models that will be removed
        TransportChannelModel transportChannelModel = new TransportChannelModel(1, transportChannel);
        SocketChannelModel socketChannelModel = new SocketChannelModel(2, socketChannel);
        
        //mock that there are no channels for the id after removal of the tranport channel
        when(m_RemoteChannelLookup.getChannels(1)).thenReturn(new ArrayList<RemoteChannel>());
        //remove the transport channel
        m_SUT.removeChannel(transportChannelModel); 

        // verify lookup told to remove the correct channel
        verify(m_RemoteChannelLookup).removeChannel(transportChannel);

        //mock exception that the system id is no longer valid after removal of the socket channel
        when(m_RemoteChannelLookup.getChannels(2)).thenReturn(new ArrayList<RemoteChannel>());
        // now remove the socket channel
        m_SUT.removeChannel(socketChannelModel); 

        // verify lookup told to remove the correct channel
        verify(m_RemoteChannelLookup).removeChannel(socketChannel);
    }
    
    /**
     * Verify that removing a channel removes it from the lookup.
     * 
     * Verify that the controller for the channel is NOT removed after one channel is removed.
     */
    @Test
    public void testRemoveChannelMultiple() throws IllegalArgumentException
    {
        // mock lookup
        SocketChannel socketChannel = createMockSocketChannel("host", 1000, ChannelStatus.Unknown);
        TransportChannel transportChannel = createMockTransportChannel("tl1", "local", 
                "remote", ChannelStatus.Unavailable);
        
        when(m_RemoteChannelLookup.getSocketChannel("host", 1000)).thenReturn(socketChannel);
        when(m_RemoteChannelLookup.getTransportChannel("tl1", "local", "remote")).thenReturn(transportChannel);
        
        //create channel models that will be removed
        TransportChannelModel transportChannelModel = new TransportChannelModel(1, transportChannel);
        SocketChannelModel socketChannelModel = new SocketChannelModel(1, socketChannel);
        
        //mock the list of channels from the remote channel look up, this list does not include the transport channel
        //because it is being removed
        List<RemoteChannel> channels = new ArrayList<RemoteChannel>();
        channels.add(socketChannel);
        when(m_RemoteChannelLookup.getChannels(1)).thenReturn(channels);
        //remove the transport channel
        m_SUT.removeChannel(transportChannelModel); 

        // verify lookup told to remove the correct channel
        verify(m_RemoteChannelLookup).removeChannel(transportChannel);

        //mock that there are no channels for the id after removal of the socket channel
        when(m_RemoteChannelLookup.getChannels(1)).thenReturn(new ArrayList<RemoteChannel>());
        // now remove the socket channel
        m_SUT.removeChannel(socketChannelModel); 

        // verify lookup told to remove the correct channel
        verify(m_RemoteChannelLookup).removeChannel(socketChannel);
    }
    
    /**
     * Test that a socket channel model contains all info from the actual RemoteChannel.
     */
    @Test
    public void testGetSocketChannels()
    {
        //mocking
        SocketChannel socChannel = createMockSocketChannel("localhost", 3001, ChannelStatus.Unknown);
        
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(socChannel);
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1957, setChans);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        //check that the mock channel is returned, this would be the same behavior as if the channel was created
        //in the remote channel look up and merely retrieved by the channel manager
        assertThat(m_SUT.getSocketChannel("localhost", 3001), is(notNullValue()));
        SocketChannelModel socketModel = m_SUT.getSocketChannel("localhost", 3001);
        
        //check the model is what was expected
        assertThat(socketModel.getHost(), is("localhost"));
        assertThat(socketModel.getPort(), is(3001));
        assertThat(socketModel.getControllerId(), is(1957));
        assertThat(socketModel.getBytesTransmitted(), is(991L));
        assertThat(socketModel.getBytesReceived(), is(402L));
        assertThat(socketModel.getQueuedMessageCount(), is(1));
    }
    
    /**
     * Verify that an empty remote channel lookup causes null to be returned 
     * for requested socket channel.
     */
    @Test
    public void testGetSocketChannelWithNoKnownChannels()
    {
        SocketChannelModel socket = m_SUT.getSocketChannel("hostName", 1111);
        
        assertThat(socket, nullValue());
    }
    
    /**
     * Verify that with an unknown host name and port returns null for the 
     * requested inputs.
     */
    @Test
    public void testGetUnknownSocketChannel()
    {
        //mocking
        SocketChannel socChannel = createMockSocketChannel("localhost", 3001, ChannelStatus.Unknown);
        
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(socChannel);
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1957, setChans);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        SocketChannelModel socket = m_SUT.getSocketChannel("unknown-host", 444);
        assertThat(socket, nullValue());
    }
    
    /**
     * Test that all transport channel model contain all info from the actual transport channels.
     */
    @Test
    public void testGetTransportChannel()
    {
        //behavior for remote lookup
        TransportChannel remoteChan = createMockTransportChannel("transport", "localaddr-diff", 
                "remoteaddr", ChannelStatus.Unknown);
        when(remoteChan.getBytesTransmitted()).thenReturn(2L);
        when(remoteChan.getBytesReceived()).thenReturn(50L);
        when(remoteChan.getQueuedMessageCount()).thenReturn(40);
        
        TransportChannel remoteChan1 = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr-diff", ChannelStatus.Unknown);
        when(remoteChan1.getBytesTransmitted()).thenReturn(921L);
        when(remoteChan1.getBytesReceived()).thenReturn(8382L);
        when(remoteChan1.getQueuedMessageCount()).thenReturn(41);
         
        TransportChannel remoteChan2 = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr", ChannelStatus.Unknown);
                
        when(remoteChan2.getBytesTransmitted()).thenReturn(991L);
        when(remoteChan2.getBytesReceived()).thenReturn(402L);
        when(remoteChan2.getQueuedMessageCount()).thenReturn(42);
        
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(remoteChan);
        setChans.add(remoteChan1);
        setChans.add(remoteChan2);
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1596, setChans);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        // replay
        TransportChannelModel model = m_SUT.getTransportLayerChannel("transport", "localaddr", "remoteaddr");
        
        // verify correct channel is returned
        assertThat(model.getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
        assertThat(model.getName(), is("transport"));
        assertThat(model.getLocalMessageAddress(), is("localaddr"));
        assertThat(model.getRemoteMessageAddress(), is("remoteaddr"));
        assertThat(model.getBytesTransmitted(), is(991L));
        assertThat(model.getBytesReceived(), is(402L));
        assertThat(model.getQueuedMessageCount(), is(42));
        
        // replay for other channel
        TransportChannelModel model2 = m_SUT.getTransportLayerChannel("transport", "localaddr", "remoteaddr-diff");
        
        // verify correct channel is returned
        assertThat(model2.getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
        assertThat(model2.getName(), is("transport"));
        assertThat(model2.getLocalMessageAddress(), is("localaddr"));
        assertThat(model2.getRemoteMessageAddress(), is("remoteaddr-diff"));
        assertThat(model2.getBytesTransmitted(), is(921L));
        assertThat(model2.getBytesReceived(), is(8382L));
        assertThat(model2.getQueuedMessageCount(), is(41));
    }
    
    /**
     * Verify that with empty remote channel lookup null is returned.
     */
    @Test
    public void testGetTransportLayerChannelWithNoKnownChannels()
    {
        TransportChannelModel transportChannel = m_SUT.getTransportLayerChannel("name", "address", "remote-address");
        
        assertThat(transportChannel, nullValue());
    }
    
    /**
     * Verify that trying to get an unknown transport layer channel will return null.
     * Remote channel lookup has channels but not the unknown transport channel.
     */
    @Test
    public void testGetUnknownTransportLayerChannel()
    {
      //behavior for remote lookup
        TransportChannel remoteChan = createMockTransportChannel("transport", "localaddr-diff", 
                "remoteaddr", ChannelStatus.Unknown);

        TransportChannel remoteChan1 = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr-diff", ChannelStatus.Unknown);
         
        TransportChannel remoteChan2 = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr", ChannelStatus.Unknown);
       
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(remoteChan);
        setChans.add(remoteChan1);
        setChans.add(remoteChan2);
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1596, setChans);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        TransportChannelModel transportChannel = m_SUT.getTransportLayerChannel("unknown-trnsprt", 
                "unknown-localaddr", "unknown-remoteAddr");
    
        assertThat(transportChannel, nullValue());
    }
    
    @Test
    public void testGetAllTransportChannels()
    {
        //behavior for remote lookup
        TransportChannel remoteChan = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr", ChannelStatus.Unknown);
        
        TransportChannel remoteChan1 = createMockTransportChannel("transport1", "localaddr1", 
                "remoteaddr1", ChannelStatus.Unknown);
        
        TransportChannel remoteChan2 = createMockTransportChannel("transport2", "localaddr2", 
                "remoteaddr2", ChannelStatus.Unknown);
        
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(remoteChan);
        
        Set<RemoteChannel> setChans1 = new HashSet<RemoteChannel>();
        setChans1.add(remoteChan1);
        
        Set<RemoteChannel> setChans2 = new HashSet<RemoteChannel>();
        setChans2.add(remoteChan2);
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1596, setChans);
        channels.put(1234, setChans1);
        channels.put(1478, setChans2);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        //check that all three channels are known
        final List<TransportChannelModel> models = m_SUT.getAllTransportLayerChannels();
        assertThat(models.size(), is(3));
    }
    
    /**
     * Test getting channels for a particular controller.
     */
    @Test
    public void getChannelsForController()
    {
        //behavior for remote lookup
        TransportChannel remoteChan = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr", ChannelStatus.Unknown);
        
        TransportChannel remoteChan1 = createMockTransportChannel("transport1", "localaddr1", 
                "remoteaddr1", ChannelStatus.Active);
        
        TransportChannel remoteChan2 = createMockTransportChannel("transport2", "localaddr2", 
                "remoteaddr2", ChannelStatus.Unknown);
        
        List<RemoteChannel> listChans = new ArrayList<RemoteChannel>();
        listChans.add(remoteChan);
        listChans.add(remoteChan1);        
        
        List<RemoteChannel> listChans2 = new ArrayList<RemoteChannel>();
        listChans2.add(remoteChan2);
        
        //mocking a socket channel
        SocketChannel socChannel = createMockSocketChannel("localhost", 3001, ChannelStatus.Active);
        
        List<RemoteChannel> listChans3 = new ArrayList<RemoteChannel>();
        listChans3.add(socChannel);
        
        //mocking behavior
        when(m_RemoteChannelLookup.getChannels(1596)).thenReturn(listChans);
        when(m_RemoteChannelLookup.getChannels(1478)).thenReturn(listChans2);
        when(m_RemoteChannelLookup.getChannels(1957)).thenReturn(listChans3);
        
        //check that there are channels returned for the different controllers
        assertThat(m_SUT.getChannelsForController(1596).size(), is(2));
        assertThat(m_SUT.getChannelsForController(1478).size(), is(1));
        assertThat(m_SUT.getChannelsForController(1957).size(), is(1));
        
        //check the status of the channels
        assertThat(m_SUT.getStatusForController(1596), is(ControllerStatus.Degraded));
        assertThat(m_SUT.getStatusForController(1478), is(ControllerStatus.Bad));
        assertThat(m_SUT.getStatusForController(1957), is(ControllerStatus.Good));        
    }
    
    /**
     * Test getting all channels known to the system.
     * Verify that the correct channel types are returned for the controller id referenced.
     */
    @Test
    public void testGetChannelsByType()
    {
        //behavior for remote lookup
        TransportChannel remoteChan = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr", ChannelStatus.Unknown);
        
        TransportChannel remoteChan1 = createMockTransportChannel("transport1", "localaddr1", 
                "remoteaddr1", ChannelStatus.Unknown);
        
        TransportChannel remoteChan2 = createMockTransportChannel("transport2", "localaddr2", 
                "remoteaddr2", ChannelStatus.Unknown);
        
        List<RemoteChannel> listChans = new ArrayList<RemoteChannel>();
        listChans.add(remoteChan);        
        
        //mocking a socket channel
        SocketChannel socChannel = createMockSocketChannel("localhost", 3001, ChannelStatus.Active);
        
        List<RemoteChannel> listChans2 = new ArrayList<RemoteChannel>();
        listChans2.add(socChannel);
        listChans2.add(remoteChan1);
        listChans2.add(remoteChan2);
        
        //mocking behavior
        when(m_RemoteChannelLookup.getChannels(8523)).thenReturn(listChans);
        when(m_RemoteChannelLookup.getChannels(1478)).thenReturn(listChans2);
        
        //replay
        List<Channel> channels8523Trans = m_SUT.getChannelsForController(8523, RemoteChannelTypeEnum.TRANSPORT);
        List<Channel> channels8523Sock = m_SUT.getChannelsForController(8523, RemoteChannelTypeEnum.SOCKET);
        List<Channel> channels1478Trans = m_SUT.getChannelsForController(1478, RemoteChannelTypeEnum.TRANSPORT);
        List<Channel> channels1478Sock = m_SUT.getChannelsForController(1478, RemoteChannelTypeEnum.SOCKET);
        
        //check that there are channels returned for the different controllers
        assertThat(channels8523Trans.size(), is(1));
        assertThat(channels8523Sock.size(), is(0));
        assertThat(channels1478Sock.size(), is(1));
        assertThat(channels1478Trans.size(), is(2));    
        
        //check that info with in the abstract base channel type is correct, because the lists are Channel models and 
        //not remote channels cannot do hasItem for the remote channels mocked.
        assertThat(channels8523Trans.get(0).getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
        assertThat(channels8523Trans.get(0).getControllerId(), is(8523));
        assertThat(channels1478Sock.get(0).getChannelType(), is(RemoteChannelTypeEnum.SOCKET));
        assertThat(channels1478Sock.get(0).getControllerId(), is(1478));
        assertThat(channels1478Trans.get(0).getChannelType(), is(RemoteChannelTypeEnum.TRANSPORT));
        assertThat(channels1478Trans.get(0).getControllerId(), is(1478));
    }
    
    /**
     * Test that the request to clear a socket channel's message queue.
     * Verify clear called on the appropriate channel.
     */
    @Test
    public void testClearSocketChannelQueue()
    {
        //create mock socket
        SocketChannel channel = createMockSocketChannel("localhost", 3001, ChannelStatus.Active);
        
        List<RemoteChannel> listChans = new ArrayList<RemoteChannel>();
        listChans.add(channel);
        
        //mocking behavior
        when(m_RemoteChannelLookup.getChannels(8523)).thenReturn(listChans);
        List<Channel> channels = m_SUT.getChannelsForController(8523);
        when(m_RemoteChannelLookup.getSocketChannel("localhost", 3001)).thenReturn(channel);
        
        //ask the channel manager to clear the channels queue
        m_SUT.clearChannelQueue(channels.get(0));
        
        //verify
        verify(channel).clearQueuedMessages();
    }
    
    /**
     * Test that the request to clear a transport channel's message queue.
     * Verify clear called on the appropriate channel.
     */
    @Test
    public void testClearTransportChannelQueue()
    {
        //create mock transport
        TransportChannel channel = createMockTransportChannel("transport", "localaddr", 
                "remoteaddr", ChannelStatus.Unknown);
            
        List<RemoteChannel> listChans = new ArrayList<RemoteChannel>();
        listChans.add(channel);
        
        //mocking behavior
        when(m_RemoteChannelLookup.getChannels(8523)).thenReturn(listChans);
        List<Channel> channels = m_SUT.getChannelsForController(8523);
        when(m_RemoteChannelLookup.getTransportChannel("transport", "localaddr", "remoteaddr")).thenReturn(channel);
        
        //ask the channel manager to clear the channels queue
        m_SUT.clearChannelQueue(channels.get(0));
        
        //verify
        verify(channel).clearQueuedMessages();
    }
    
    /**
     * Verify that when remote channel lookup has no channel entries an empty list of
     * socket channels is returned.
     */
    @Test
    public void testGetAllSocketChannelsEmptyList()
    {
        List<SocketChannelModel> sockets = m_SUT.getAllSocketChannels();
        
        assertThat(sockets, notNullValue());
        assertThat(sockets.size(), is(0));
    }
    
    /**
     * Verify that the correct socket channels are returned.
     */
    @Test
    public void testGetAllSocketChannels()
    {
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(createMockSocketChannel("localhost", 4444, ChannelStatus.Unknown));
        setChans.add(createMockTransportChannel("transportName", "localAddr", "remoteAddr", ChannelStatus.Unknown));
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1957, setChans);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        List<SocketChannelModel> sockets = m_SUT.getAllSocketChannels();
        
        assertThat(sockets, notNullValue());
        assertThat(sockets.size(), is(1));
    }
    
    /**
     * Verify with no channels in the remote channel lookup an empty list of channels is 
     * returned.
     */
    @Test
    public void testGetAllChannelsWithNoKnownChannels()
    {
        List<Channel> channels = m_SUT.getAllChannels();
        
        assertThat(channels, notNullValue());
        assertThat(channels.size(), is(0));
    }
    
    /**
     * Verify that with known channels in the remote channel lookup, all channels are returned.
     */
    @Test
    public void testGetAllChannels()
    {
        Set<RemoteChannel> setChans = new HashSet<RemoteChannel>();
        setChans.add(createMockSocketChannel("localhost", 4444, ChannelStatus.Unknown));
        setChans.add(createMockTransportChannel("transportName", "localAddr", "remoteAddr", ChannelStatus.Unknown));
        
        Map<Integer, Set<RemoteChannel>> channels = new HashMap<Integer, Set<RemoteChannel>>();
        channels.put(1957, setChans);
        when(m_RemoteChannelLookup.getAllChannels()).thenReturn(channels);
        
        List<Channel> returnedChannels = m_SUT.getAllChannels();
        
        assertThat(returnedChannels, notNullValue());
        assertThat(returnedChannels.size(), is(2));
        
        for(Channel aChannel : returnedChannels)
        {
            if (aChannel.getChannelType() == RemoteChannelTypeEnum.SOCKET)
            {
                SocketChannelModel sChannel = (SocketChannelModel)aChannel;
                assertThat(sChannel.getHost(), is("localhost"));
                assertThat(sChannel.getPort(), is(4444));
            }
            else
            {
                TransportChannelModel tChannel = (TransportChannelModel)aChannel;
                assertThat(tChannel.getName(), is("transportName"));
                assertThat(tChannel.getLocalMessageAddress(), is("localAddr"));
                assertThat(tChannel.getRemoteMessageAddress(), is("remoteAddr"));
            }
        }
    }
    
    /**
     * Creates a mocked socket channel based on the hostname and port number 
     * passed in.
     * @param hostName
     *  the hostname that the mocked socket channel should have.
     * @param portNum
     *  the port number that the mocked socket should have
     * @param status
     *  the status of the channel
     * @return
     *  the mocked socket channel
     */
    private SocketChannel createMockSocketChannel(String hostName, int portNum, ChannelStatus status)
    {
      //mocking
        SocketChannel socketChannel = mock(SocketChannel.class);
        when(socketChannel.getChannelType()).thenReturn(RemoteChannelTypeEnum.SOCKET);
        when(socketChannel.getStatus()).thenReturn(status);
        when(socketChannel.getHost()).thenReturn(hostName);
        when(socketChannel.getPort()).thenReturn(portNum);
        when(socketChannel.getBytesTransmitted()).thenReturn(991L);
        when(socketChannel.getBytesReceived()).thenReturn(402L);
        when(socketChannel.getQueuedMessageCount()).thenReturn(1);
        
        return socketChannel;
    }
    
    /**
     * Creates a mocked transport channel based on the transport name and local and remote 
     * addresses passed in.
     * @param transportName
     *  the name that the mocked transport channel should have
     * @param localAddr
     *  the local address of the mocked transport channel
     * @param remoteAddr
     *  the remote address of the mocked transport channel
     * @param status
     *  the status of the mocked channel
     * @return
     *  the mocked transport channel
     */
    private TransportChannel createMockTransportChannel(String transportName, String localAddr,
            String remoteAddr, ChannelStatus status)
    {
        TransportChannel transportChannel = mock(TransportChannel.class);
        when(transportChannel.getChannelType()).thenReturn(RemoteChannelTypeEnum.TRANSPORT);
        when(transportChannel.getStatus()).thenReturn(status);
        when(transportChannel.getTransportLayerName()).thenReturn(transportName);
        when(transportChannel.getLocalMessageAddress()).thenReturn(localAddr);
        when(transportChannel.getRemoteMessageAddress()).thenReturn(remoteAddr);
        
        return transportChannel;
    }
}
