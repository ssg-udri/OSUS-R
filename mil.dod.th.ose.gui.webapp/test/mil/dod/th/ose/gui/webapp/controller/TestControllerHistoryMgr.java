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
package mil.dod.th.ose.gui.webapp.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.Serializable;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.TransportChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.gui.webapp.controller.ControllerHistoryMgr.ChannelEventHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerHistoryMgr.ControllerInfoHelper;
import mil.dod.th.ose.gui.webapp.controller.history.ControllerHistory;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Test class for the {@link ControllerHistoryMgr} class.
 * 
 * @author cweisenborn
 */
public class TestControllerHistoryMgr
{
    private @Mock PersistentDataStore m_DataStore;
    private @Mock RemoteChannelLookup m_RmtChnLkp;
    private @Mock MessageFactory m_MsgFactory;
    private @Mock BundleContextUtil m_ContextUtil;
    private @Mock GrowlMessageUtil m_GrowlUtil;
    private @Mock BundleContext m_Context;
    private @Mock ServiceRegistration<?> m_ServiceReg;
    private @Mock MessageWrapper m_Wrapper;
    
    private ControllerHistoryMgr m_SUT;
    private ControllerInfoHelper m_ControllerInfoHelper;
    private ChannelEventHelper m_ChannelEventHelper;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        when(m_ContextUtil.getBundleContext()).thenReturn(m_Context);
        when(m_Context.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
                Mockito.any(Dictionary.class))).thenReturn(m_ServiceReg);
        
        when(m_MsgFactory.createBaseMessage(BaseMessageType.RequestControllerInfo, null)).thenReturn(m_Wrapper);
        
        //Setup remote channel lookup to contain no channels initially.
        when(m_RmtChnLkp.getAllChannels()).thenReturn(new HashMap<Integer, Set<RemoteChannel>>());
        
        m_SUT = new ControllerHistoryMgr();
        
        m_SUT.setPersistentDataStore(m_DataStore);
        m_SUT.setRemoteChannelLookup(m_RmtChnLkp);
        m_SUT.setMessageFactory(m_MsgFactory);
        m_SUT.setBundleUtil(m_ContextUtil);
        m_SUT.setGrowlUtil(m_GrowlUtil);
        
        m_SUT.setupHistoryManager();
        
        //verify event handlers are registered
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(m_Context, times(2)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_ContextUtil, times(2)).getBundleContext();
        
        m_ControllerInfoHelper = (ControllerInfoHelper)captor.getAllValues().get(0);
        m_ChannelEventHelper = (ChannelEventHelper)captor.getAllValues().get(1);
    }
    
    /**
     * Test the unregister event helpers method and verify both event helpers are unregistered;
     */
    @Test
    public void testUnregisterEventHelpers()
    {
        m_SUT.unregisterEventHelpers();
        
        verify(m_ServiceReg, times(2)).unregister();
    }
    
    /**
     * Tests the get controller history method and verifies that the appropriate list of controllers is returned.
     */
    @Test
    public void testGetControllerHistory()
    {
        Map<String, ControllerHistory> historyMap = m_SUT.getControllerHistory();
        assertThat(historyMap.size(), is(1));
        assertThat(historyMap.containsKey("localhost"), is(true));
        
        mockControllerData();
        historyMap = m_SUT.getControllerHistory();
        assertThat(historyMap.size(), is(3));
        assertThat(historyMap.containsKey("localhost:generic-controller"), is(true));
        assertThat(historyMap.containsKey("10.110.7.90:Roof"), is(true));
        assertThat(historyMap.containsKey("192.168.2.1:House"), is(true));
    }
    
    /**
     * Verify that the controller history map only contains the last 9 controllers that have been connected to and 
     * localhost.
     */
    @Test
    public void testGetControllerHistoryMaxList()
    {
        mockLargeNumberControllerData();
        assertThat(m_DataStore.query(ControllerHistory.class).size(), is(13));
        
        Map<String, ControllerHistory> historyMap = m_SUT.getControllerHistory();
        assertThat(historyMap.size(), is(10));
        assertThat(historyMap.containsKey("127.0.0.1:generic-controller"), is(true));
        assertThat(historyMap.containsKey("192.168.1.12:test12"), is(true));
        assertThat(historyMap.containsKey("192.168.1.11:test11"), is(true));
        assertThat(historyMap.containsKey("192.168.1.10:test10"), is(true));
        assertThat(historyMap.containsKey("192.168.1.9:test9"), is(true));
        assertThat(historyMap.containsKey("192.168.1.8:test8"), is(true));
        assertThat(historyMap.containsKey("192.168.1.7:test7"), is(true));
        assertThat(historyMap.containsKey("192.168.1.6:test6"), is(true));
        assertThat(historyMap.containsKey("192.168.1.5:test5"), is(true));
        assertThat(historyMap.containsKey("192.168.1.4:test4"), is(true));
        
        assertThat(historyMap.containsKey("192.168.1.3:test3"), is(false));
        assertThat(historyMap.containsKey("192.168.1.2:test2"), is(false));
        assertThat(historyMap.containsKey("192.168.1.1:test1"), is(false));
    }
    
    @Test
    public void testInitialRemoteChannels()
    {
        final Collection<PersistentData> emptyCollection = new HashSet<>();
        when(m_DataStore.query(eq(ControllerHistory.class), Mockito.anyString())).thenReturn(emptyCollection);
        
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(SocketChannel.class));
        final String hostname = "12.11.10.9";
        when(((SocketChannel)remoteChannel).getHost()).thenReturn(hostname);
        when(((SocketChannel)remoteChannel).getPort()).thenReturn(5001);
        
        final Map<Integer, Set<RemoteChannel>> channelsMap = new HashMap<>();
        final Set<RemoteChannel> channelsSet = new HashSet<>();
        channelsSet.add(remoteChannel);
        channelsMap.put(100, channelsSet);
        
        //Setup remote channel lookup so that there is one channel initially.
        when(m_RmtChnLkp.getAllChannels()).thenReturn(channelsMap);
        
        m_SUT.setupHistoryManager();
        
        final ArgumentCaptor<ControllerHistory> persistentCaptor = ArgumentCaptor.forClass(ControllerHistory.class);
        verify(m_DataStore).persist(eq(ControllerHistory.class), Mockito.any(UUID.class), Mockito.anyString(), 
                persistentCaptor.capture());
        final ControllerHistory addedController = persistentCaptor.getValue();
        assertThat(addedController.getControllerId(), is(100));
        assertThat(addedController.getHostName(), is(hostname));
        assertThat(addedController.getPort(), is(5001));
        
        verify(m_Wrapper).queue(100, null);
    }
    
    /**
     * Tests the controller info helper and verifies that the persistent data for the controller history is updated 
     * and merged.
     */
    @Test
    public void testControllerInfoHelper() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        final String hostname = "100.200.300.400";
        final int port = 99;
        final int systemId = 123;
        final long initialTime = System.currentTimeMillis();
        
        final ControllerHistory initialController = new ControllerHistory();
        initialController.setControllerId(systemId);
        initialController.setHostName(hostname);
        initialController.setPort(port);
        initialController.setLastConnected(initialTime);
        
        final UUID uuid = UUID.randomUUID();
        final PersistentData data = new PersistentData(uuid, hostname, ControllerHistory.class.toString(), 
                initialController);
        
        final Collection<PersistentData> collection = new HashSet<>();
        collection.add(data);
        when(m_DataStore.query(eq(ControllerHistory.class), Mockito.anyString())).thenReturn(collection);
        
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(SocketChannel.class));
        when(((SocketChannel)remoteChannel).getHost()).thenReturn(hostname);
        when(((SocketChannel)remoteChannel).getPort()).thenReturn(port);
        
        final ControllerInfoData dataMsg = ControllerInfoData.newBuilder().setName("Dart").build();
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_SYS_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, remoteChannel);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMsg);
        final Event controllerInfoEvent = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        m_ControllerInfoHelper.handleEvent(controllerInfoEvent);
        
        final ArgumentCaptor<PersistentData> dataCaptor = ArgumentCaptor.forClass(PersistentData.class);
        verify(m_DataStore).merge(dataCaptor.capture());
        final PersistentData updatedData = dataCaptor.getValue();
        final ControllerHistory updatedController = (ControllerHistory)updatedData.getEntity();
        assertThat(updatedController.getControllerId(), is(systemId));
        assertThat(updatedController.getControllerName(), is("Dart"));
        assertThat(updatedController.getHostName(), is(hostname));
        assertThat(updatedController.getPort(), is(port));
        assertThat(updatedController.getLastConnected(), is(initialTime));
    }
    
    /**
     * Verify that the controller info associated with a transport channel isn't persisted as controller history.
     */
    @Test
    public void testControllerInfoHelperTransportChannel() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(TransportChannel.class));
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, remoteChannel);
        final Event controllerInfoEvent = new Event(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, props);
        
        m_ControllerInfoHelper.handleEvent(controllerInfoEvent);
        
        verify(m_DataStore, never()).query(eq(ControllerHistory.class), Mockito.anyString());
        verify(m_DataStore, never()).merge(Mockito.any(PersistentData.class));
    }
    
    /**
     * Tests the channel event helper and verifies that the controller information is persisted as a controller history
     * object.
     */
    @Test
    public void testChannelEventHelperNewChannel()
    {
        final Collection<PersistentData> emptyCollection = new HashSet<>();
        when(m_DataStore.query(eq(ControllerHistory.class), Mockito.anyString())).thenReturn(emptyCollection);
        
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(SocketChannel.class));
        final String hostname = "12.11.10.9";
        when(((SocketChannel)remoteChannel).getHost()).thenReturn(hostname);
        when(((SocketChannel)remoteChannel).getPort()).thenReturn(5001);
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_SYS_ID, 100);
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, remoteChannel);
        final Event remoteChannelAddedEvent = new Event(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, props);
        
        m_ChannelEventHelper.handleEvent(remoteChannelAddedEvent);
        
        final ArgumentCaptor<ControllerHistory> persistentCaptor = ArgumentCaptor.forClass(ControllerHistory.class);
        verify(m_DataStore).persist(eq(ControllerHistory.class), Mockito.any(UUID.class), Mockito.anyString(), 
                persistentCaptor.capture());
        final ControllerHistory addedController = persistentCaptor.getValue();
        assertThat(addedController.getControllerId(), is(100));
        assertThat(addedController.getHostName(), is(hostname));
        assertThat(addedController.getPort(), is(5001));
    }
    
    /**
     * Verify that the controller associated with a transport channel isn't persisted as controller history.
     */
    @Test
    public void testChannelEventHelperTransportChannel() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(TransportChannel.class));
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_SYS_ID, 500);
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, remoteChannel);
        final Event remoteChannelAddedEvent = new Event(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, props);
        
        m_ChannelEventHelper.handleEvent(remoteChannelAddedEvent);
        
        verify(m_DataStore, never()).persist(eq(ControllerHistory.class), Mockito.any(UUID.class), Mockito.anyString(),
                Mockito.any(Serializable.class));
        verify(m_DataStore, never()).merge(Mockito.any(PersistentData.class));
    }
    
    /**
     * Tests the channel event helper and verifies that the persistent data for the controller history is updated 
     * and merged.
     */
    @Test
    public void testChannelEventHelperUpdatedChannel() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException, InterruptedException
    {
        final String hostname = "2.1.168.192";
        final int port = 1001;
        final int systemId = 201;
        final long initialTime = System.currentTimeMillis();
        
        final ControllerHistory initialController = new ControllerHistory();
        initialController.setControllerId(systemId);
        initialController.setControllerName("test");
        initialController.setHostName(hostname);
        initialController.setPort(port);
        initialController.setLastConnected(initialTime);
        
        //Sleep needed to make sure initial time varies from the updated time.
        Thread.sleep(500);
        
        final UUID uuid = UUID.randomUUID();
        final PersistentData data = new PersistentData(uuid, hostname, ControllerHistory.class.toString(), 
                initialController);
        
        final Collection<PersistentData> collection = new HashSet<>();
        collection.add(data);
        when(m_DataStore.query(eq(ControllerHistory.class), Mockito.anyString())).thenReturn(collection);
        
        final RemoteChannel remoteChannel = mock(RemoteChannel.class, 
                withSettings().extraInterfaces(SocketChannel.class));
        when(((SocketChannel)remoteChannel).getHost()).thenReturn(hostname);
        when(((SocketChannel)remoteChannel).getPort()).thenReturn(port);
        when(((SocketChannel)remoteChannel).isSslEnabled()).thenReturn(true);
        
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_SYS_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_CHANNEL, remoteChannel);
        final Event remoteChannelAddedEvent = new Event(RemoteChannelLookup.TOPIC_CHANNEL_UPDATED, props);
        
        m_ChannelEventHelper.handleEvent(remoteChannelAddedEvent);
        
        final ArgumentCaptor<PersistentData> dataCaptor = ArgumentCaptor.forClass(PersistentData.class);
        verify(m_DataStore).merge(dataCaptor.capture());
        final PersistentData updatedData = dataCaptor.getValue();
        final ControllerHistory updatedController = (ControllerHistory)updatedData.getEntity();
        assertThat(updatedController.getControllerId(), is(systemId));
        assertThat(updatedController.getControllerName(), is("test"));
        assertThat(updatedController.getHostName(), is(hostname));
        assertThat(updatedController.getPort(), is(port));
        assertThat(updatedController.isSslEnabled(), is(true));
        assertThat(updatedController.getLastConnected(), greaterThan(initialTime));
    }
    
    /**
     * Creates mock controller history to be returned by the persistent data store.
     */
    private void mockControllerData()
    {
        final ControllerHistory localhost = new ControllerHistory();
        localhost.setControllerId(0);
        localhost.setControllerName("generic-controller");
        localhost.setHostName("localhost");
        localhost.setLastConnected(19000);
        localhost.setPort(4000);
        localhost.setSslEnabled(false);
        
        final ControllerHistory controller1 = new ControllerHistory();
        controller1.setControllerId(5);
        controller1.setControllerName("Roof");
        controller1.setHostName("10.110.7.90");
        controller1.setLastConnected(5000);
        controller1.setPort(3001);
        controller1.setSslEnabled(true);
        
        final ControllerHistory controller2 = new ControllerHistory();
        controller2.setControllerId(5);
        controller2.setControllerName("House");
        controller2.setHostName("192.168.2.1");
        controller2.setLastConnected(75000);
        controller2.setPort(8976);
        controller2.setSslEnabled(false);
        
        final PersistentData localhostData = new PersistentData(UUID.randomUUID(), localhost.getHostName(), 
                ControllerHistory.class.getName(), localhost);
        final PersistentData controller1Data = new PersistentData(UUID.randomUUID(), controller1.getHostName(), 
                ControllerHistory.class.getName(), controller1);
        final PersistentData controller2Data = new PersistentData(UUID.randomUUID(), controller2.getHostName(), 
                ControllerHistory.class.getName(), controller2);
        
        final Collection<PersistentData> data = new HashSet<>();
        data.add(localhostData);
        data.add(controller1Data);
        data.add(controller2Data);
        
        when(m_DataStore.query(ControllerHistory.class)).thenReturn(data);
    }
    
    /**
     * Creates mock controller history to be returned by the persistent data store.
     */
    private void mockLargeNumberControllerData()
    {
        final ControllerHistory localhost = new ControllerHistory();
        localhost.setControllerId(0);
        localhost.setControllerName("generic-controller");
        localhost.setHostName("127.0.0.1");
        localhost.setLastConnected(1000);
        localhost.setPort(4000);
        
        final ControllerHistory controller1 = new ControllerHistory();
        controller1.setControllerId(1);
        controller1.setControllerName("test1");
        controller1.setHostName("192.168.1.1");
        controller1.setLastConnected(5000);
        controller1.setPort(4001);
        
        final ControllerHistory controller2 = new ControllerHistory();
        controller2.setControllerId(2);
        controller2.setControllerName("test2");
        controller2.setHostName("192.168.1.2");
        controller2.setLastConnected(10000);
        controller2.setPort(4002);
        
        final ControllerHistory controller3 = new ControllerHistory();
        controller3.setControllerId(3);
        controller3.setControllerName("test3");
        controller3.setHostName("192.168.1.3");
        controller3.setLastConnected(15000);
        controller3.setPort(4003);
        
        final ControllerHistory controller4 = new ControllerHistory();
        controller4.setControllerId(4);
        controller4.setControllerName("test4");
        controller4.setHostName("192.168.1.4");
        controller4.setLastConnected(20000);
        controller4.setPort(4004);
        
        final ControllerHistory controller5 = new ControllerHistory();
        controller5.setControllerId(5);
        controller5.setControllerName("test5");
        controller5.setHostName("192.168.1.5");
        controller5.setLastConnected(25000);
        controller5.setPort(4005);
        
        final ControllerHistory controller6 = new ControllerHistory();
        controller6.setControllerId(6);
        controller6.setControllerName("test6");
        controller6.setHostName("192.168.1.6");
        controller6.setLastConnected(30000);
        controller6.setPort(4006);
        
        final ControllerHistory controller7 = new ControllerHistory();
        controller7.setControllerId(7);
        controller7.setControllerName("test7");
        controller7.setHostName("192.168.1.7");
        controller7.setLastConnected(35000);
        controller7.setPort(4007);
        
        final ControllerHistory controller8 = new ControllerHistory();
        controller8.setControllerId(8);
        controller8.setControllerName("test8");
        controller8.setHostName("192.168.1.8");
        controller8.setLastConnected(40000);
        controller8.setPort(4008);
        
        final ControllerHistory controller9 = new ControllerHistory();
        controller9.setControllerId(9);
        controller9.setControllerName("test9");
        controller9.setHostName("192.168.1.9");
        controller9.setLastConnected(45000);
        controller9.setPort(4009);
        
        final ControllerHistory controller10 = new ControllerHistory();
        controller10.setControllerId(10);
        controller10.setControllerName("test10");
        controller10.setHostName("192.168.1.10");
        controller10.setLastConnected(50000);
        controller10.setPort(4010);
      
        final ControllerHistory controller11 = new ControllerHistory();
        controller11.setControllerId(11);
        controller11.setControllerName("test11");
        controller11.setHostName("192.168.1.11");
        controller11.setLastConnected(55000);
        controller11.setPort(4011);
        
        final ControllerHistory controller12 = new ControllerHistory();
        controller12.setControllerId(12);
        controller12.setControllerName("test12");
        controller12.setHostName("192.168.1.12");
        controller12.setLastConnected(60000);
        controller12.setPort(4012);
        
        final PersistentData localhostData = new PersistentData(UUID.randomUUID(), localhost.getHostName(), 
                ControllerHistory.class.getName(), localhost);
        final PersistentData controller1Data = new PersistentData(UUID.randomUUID(), controller1.getHostName(), 
                ControllerHistory.class.getName(), controller1);
        final PersistentData controller2Data = new PersistentData(UUID.randomUUID(), controller2.getHostName(), 
                ControllerHistory.class.getName(), controller2);
        final PersistentData controller3Data = new PersistentData(UUID.randomUUID(), controller3.getHostName(), 
                ControllerHistory.class.getName(), controller3);
        final PersistentData controller4Data = new PersistentData(UUID.randomUUID(), controller4.getHostName(), 
                ControllerHistory.class.getName(), controller4);
        final PersistentData controller5Data = new PersistentData(UUID.randomUUID(), controller5.getHostName(), 
                ControllerHistory.class.getName(), controller5);
        final PersistentData controller6Data = new PersistentData(UUID.randomUUID(), controller6.getHostName(), 
                ControllerHistory.class.getName(), controller6);
        final PersistentData controller7Data = new PersistentData(UUID.randomUUID(), controller7.getHostName(), 
                ControllerHistory.class.getName(), controller7);
        final PersistentData controller8Data = new PersistentData(UUID.randomUUID(), controller8.getHostName(), 
                ControllerHistory.class.getName(), controller8);
        final PersistentData controller9Data = new PersistentData(UUID.randomUUID(), controller9.getHostName(), 
                ControllerHistory.class.getName(), controller9);
        final PersistentData controller10Data = new PersistentData(UUID.randomUUID(), controller10.getHostName(), 
                ControllerHistory.class.getName(), controller10);
        final PersistentData controller11Data = new PersistentData(UUID.randomUUID(), controller11.getHostName(), 
                ControllerHistory.class.getName(), controller11);
        final PersistentData controller12Data = new PersistentData(UUID.randomUUID(), controller12.getHostName(), 
                ControllerHistory.class.getName(), controller12);
        
        final Collection<PersistentData> data = new HashSet<>();
        data.add(localhostData);
        data.add(controller1Data);
        data.add(controller2Data);
        data.add(controller3Data);
        data.add(controller4Data);
        data.add(controller5Data);
        data.add(controller6Data);
        data.add(controller7Data);
        data.add(controller8Data);
        data.add(controller9Data);
        data.add(controller10Data);
        data.add(controller11Data);
        data.add(controller12Data);
        
        when(m_DataStore.query(ControllerHistory.class)).thenReturn(data);
    }
}
