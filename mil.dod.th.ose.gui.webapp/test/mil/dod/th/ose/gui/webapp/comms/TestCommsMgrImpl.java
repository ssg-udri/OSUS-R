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

package mil.dod.th.ose.gui.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.Message;

import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.link.LinkLayer.LinkStatus;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.LinkLayerMessages;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.GetStatusResponseData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.IsActivatedResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CommType;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayerNameResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.GetLayersResponseData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace.LinkLayerMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.FactoryObjectInfo;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigurationWrapper;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.RemoteCreateLinkLayerHandler;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.EventHelperEventAdminNamespace;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.EventHelperControllerEvent;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.EventHelperCustomCommsNamespace;
import mil.dod.th.ose.gui.webapp.comms.CommsMgrImpl.EventHelperLinkLayerNamespace;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;

/**
 * Test class for the comms manager implementation. 
 * @author bachmakm
 */
public class TestCommsMgrImpl
{
    private CommsMgrImpl m_SUT;
    private BundleContextUtil m_BundleUtil;
    private ConfigurationWrapper m_ConfigWrapper;
    private CommsLayerTypesMgr m_CommsTypesMgr;
    private GrowlMessageUtil m_GrowlUtil;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private MessageWrapper m_MessageWrapper;
      
    private EventHelperCustomCommsNamespace m_EventHelperComms;    
    private EventHelperLinkLayerNamespace m_EventHelperLink;    
    private EventHelperControllerEvent m_ControllerEventListener;
    private EventHelperEventAdminNamespace m_EventHelper;
    
    @SuppressWarnings("rawtypes") 
    private ServiceRegistration m_HandlerReg = mock(ServiceRegistration.class);
    
    //these fields are at this level so they can be used in multiple messages
    //system ids
    private int systemId1 = 123;
    private int systemId2 = 1234;
    //pids
    private String pid1 = "PITTER";
    private String pid2 = "PATTER";
    private String pid3 = "LITTLE";
    private String pid4 = "FEET";
    private String pid5 = "PURPLE";
    private String pid6 = "POSIES";
    private String pid7 = "PROFUSELY";
    
    //UUIDs
    private UUID uuid1 = UUID.randomUUID();
    private UUID uuid2 = UUID.randomUUID();
    private UUID uuid3 = UUID.randomUUID();
    private UUID uuid4 = UUID.randomUUID();
    private UUID uuid5 = UUID.randomUUID();
    private UUID uuid6 = UUID.randomUUID();
    private UUID uuid7 = UUID.randomUUID();

    @SuppressWarnings("unchecked")//because of the use of the dictionary for the event helper
    @Before
    public void setUp()
    {
        m_SUT = new CommsMgrImpl();
        
        m_MessageFactory = mock(MessageFactory.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_ConfigWrapper = mock(ConfigurationWrapper.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_EventAdmin = mock(EventAdmin.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        //set dependencies
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setCommsTypesMgr(m_CommsTypesMgr);
        m_SUT.setConfigWrapper(m_ConfigWrapper);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMessageFactory(m_MessageFactory);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createCustomCommsMessage(Mockito.any(CustomCommsMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createLinkLayerMessage(Mockito.any(LinkLayerMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //register helper
        m_SUT.registerEventHelpers();
        
        //verify
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(4)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(4)).getBundleContext();
        
        m_ControllerEventListener = (EventHelperControllerEvent)captor.getAllValues().get(0);
        m_EventHelperLink = (EventHelperLinkLayerNamespace)captor.getAllValues().get(1); 
        m_EventHelperComms = (EventHelperCustomCommsNamespace)captor.getAllValues().get(2); 
        m_EventHelper = (EventHelperEventAdminNamespace)captor.getAllValues().get(3);        
    }
    
    /**
     * Test the predestroy unregistering of event handlers.
     * Verify that all are unregistered.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.unregisterHelpers();

        //verify listeners are unregistered
        verify(m_HandlerReg, times(4)).unregister();
    }
    
    /**
     * Test the handling of a GetTopMostLayers request.
     * Verify that top layers are added to the appropriate system id as new comms stacks.
     * Verify that GetChildLayers requests are sent for transport and link layers.  
     */
    @Test
    public void testHandleGetLayersResponseAddLayers()
    {    
        Event event = mockGetLayersResponse(systemId1, "transport");
        Event event2 = mockGetLayersResponse(systemId1, "link");
        Event event3 = mockGetLayersResponse(systemId1, "physical");
        
        m_SUT.getPhysicalsAsync(systemId2);
        m_SUT.getLinksAsync(systemId2);
        m_SUT.getTransportsAsync(systemId2);
        Event event4 = mockGetLayersResponse(systemId2, "transport");
        Event event5 = mockGetLayersResponse(systemId2, "link");
        Event event6 = mockGetLayersResponse(systemId2, "physical");
                
        m_EventHelperComms.handleEvent(event);        
        List<CommsLayerBaseModel> transModels = m_SUT.getTransportsAsync(systemId1);
        
        assertThat(transModels.size(), is(1));
        assertThat(transModels.get(0).getUuid(), is(uuid1));
        assertThat(transModels.get(0).getPid(), is(pid1));
        assertThat(transModels.get(0).getCommsClazz(), is(TransportLayer.class.getName())); 
        
        m_EventHelperComms.handleEvent(event2);        
        List<CommsLayerLinkModelImpl> linkModels = m_SUT.getLinksAsync(systemId1);
        
        assertThat(linkModels.size(), is(2));
        assertThat(linkModels.get(0).getUuid(), is(uuid2));
        assertThat(linkModels.get(0).getPid(), is(pid2));
        assertThat(linkModels.get(0).getCommsClazz(), is(LinkLayer.class.getName())); 
        assertThat(linkModels.get(1).getUuid(), is(uuid4));
        assertThat(linkModels.get(1).getPid(), is(pid4));
        assertThat(linkModels.get(1).getCommsClazz(), is(LinkLayer.class.getName())); 
        
        m_EventHelperComms.handleEvent(event3);        
        List<CommsLayerBaseModel> physModels = m_SUT.getPhysicalsAsync(systemId1);
        
        assertThat(physModels.size(), is(3));
        assertThat(physModels.get(0).getUuid(), is(uuid3));
        assertThat(physModels.get(0).getPid(), is(pid3));
        assertThat(physModels.get(0).getCommsClazz(), is(PhysicalLink.class.getName())); 
        assertThat(physModels.get(1).getUuid(), is(uuid5));
        assertThat(physModels.get(1).getPid(), is(pid5));
        assertThat(physModels.get(1).getCommsClazz(), is(PhysicalLink.class.getName())); 
        assertThat(physModels.get(2).getUuid(), is(uuid6));
        assertThat(physModels.get(2).getPid(), is(pid6));
        assertThat(physModels.get(2).getCommsClazz(), is(PhysicalLink.class.getName())); 
        
        /*TEST VALUES FOR NEWLY ADDED CONTROLLER*/        
        m_EventHelperComms.handleEvent(event4); 
        transModels = m_SUT.getTransportsAsync(systemId2);
        
        assertThat(transModels.size(), is(1));
        assertThat(transModels.get(0).getUuid(), is(uuid1));
        assertThat(transModels.get(0).getPid(), is(pid1));
        assertThat(transModels.get(0).getCommsClazz(), is(TransportLayer.class.getName())); 
        
        m_EventHelperComms.handleEvent(event5); 
        linkModels = m_SUT.getLinksAsync(systemId2);
        
        assertThat(linkModels.size(), is(2));
        assertThat(linkModels.get(0).getUuid(), is(uuid2));
        assertThat(linkModels.get(0).getPid(), is(pid2));
        assertThat(linkModels.get(0).getCommsClazz(), is(LinkLayer.class.getName())); 
        assertThat(linkModels.get(1).getUuid(), is(uuid4));
        assertThat(linkModels.get(1).getPid(), is(pid4));
        assertThat(linkModels.get(1).getCommsClazz(), is(LinkLayer.class.getName())); 
        
        m_EventHelperComms.handleEvent(event6);
        physModels = m_SUT.getPhysicalsAsync(systemId2);
        
        assertThat(physModels.size(), is(3));
        assertThat(physModels.get(0).getUuid(), is(uuid3));
        assertThat(physModels.get(0).getPid(), is(pid3));
        assertThat(physModels.get(0).getCommsClazz(), is(PhysicalLink.class.getName())); 
        assertThat(physModels.get(1).getUuid(), is(uuid5));
        assertThat(physModels.get(1).getPid(), is(pid5));
        assertThat(physModels.get(1).getCommsClazz(), is(PhysicalLink.class.getName())); 
        assertThat(physModels.get(2).getUuid(), is(uuid6));
        assertThat(physModels.get(2).getPid(), is(pid6));      
        assertThat(physModels.get(2).getCommsClazz(), is(PhysicalLink.class.getName())); 
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event for get layers response
        verify(m_EventAdmin, times(6)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
    } 
    
    /**
     * Test the handling of a GetTopMostLayers request.
     * Verify that top layers are added to the appropriate system id as new comms stacks.
     * Verify that GetChildLayers requests are sent for transport and link layers.  
     */
    @Test
    public void testHandleGetLayersResponseMergeLayers()
    {    
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event event = mockGetLayersResponse(systemId1, "transport");
        Event event2 = mockGetLayersResponse(systemId1, "link");
        Event event3 = mockGetLayersResponse(systemId1, "physical");
                
        m_EventHelperComms.handleEvent(event);        
        List<CommsLayerBaseModel> transModels = m_SUT.getTransportsAsync(systemId1);        
        assertThat(transModels.size(), is(1)); 
        
        m_EventHelperComms.handleEvent(event2);        
        List<CommsLayerLinkModelImpl> linkModels = m_SUT.getLinksAsync(systemId1);        
        assertThat(linkModels.size(), is(2));
        
        m_EventHelperComms.handleEvent(event3);        
        List<CommsLayerBaseModel> physModels = m_SUT.getPhysicalsAsync(systemId1);        
        assertThat(physModels.size(), is(3));    
        
        UUID transUuid = UUID.randomUUID();
        UUID linkUuid = UUID.randomUUID();
        UUID physUuid = UUID.randomUUID();
        
        Event event4 = mockGetLayersResponseMergeLayers(systemId1, "transport", transUuid, "transPid");
        Event event5 = mockGetLayersResponseMergeLayers(systemId1, "link", linkUuid, "linkPid");
        Event event6 = mockGetLayersResponseMergeLayers(systemId1, "physical", physUuid, "physPid");
        
        m_EventHelperComms.handleEvent(event4);
        transModels = m_SUT.getTransportsAsync(systemId1);
        assertThat(transModels.size(), is(1));
        assertThat(transModels.get(0).getUuid(), is(transUuid));
        assertThat(transModels.get(0).getPid(), is("transPid"));
        
        m_EventHelperComms.handleEvent(event5);
        linkModels = m_SUT.getLinksAsync(systemId1);
        assertThat(linkModels.size(), is(1));
        assertThat(linkModels.get(0).getUuid(), is(linkUuid));
        assertThat(linkModels.get(0).getPid(), is("linkPid"));
        
        m_EventHelperComms.handleEvent(event6);
        physModels = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physModels.size(), is(1));
        assertThat(physModels.get(0).getUuid(), is(physUuid));
        assertThat(physModels.get(0).getPid(), is("physPid"));
        
        Event event7 = mockGetLayersResponseMergeLayersEmpty(systemId1, "transport");
        Event event8 = mockGetLayersResponseMergeLayersEmpty(systemId1, "link");
        Event event9 = mockGetLayersResponseMergeLayersEmpty(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(event7);
        transModels = m_SUT.getTransportsAsync(systemId1);
        assertThat(transModels.size(), is(0));
        
        m_EventHelperComms.handleEvent(event8);
        linkModels = m_SUT.getLinksAsync(systemId1);
        assertThat(linkModels.size(), is(0));
        
        m_EventHelperComms.handleEvent(event9);
        physModels = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physModels.size(), is(0));
    }
    
    /**
     * Test setting of layer name.
     * Verify name of a layer has been set correctly.  
     */
    @Test
    public void testGetLayerNameResponse()
    {
        Event event = mockGetLayersResponse(systemId1, "transport");
        Event event2 = mockGetLayerNameResponse(systemId1, CommType.TransportLayer, uuid1, "transyTrans");
        
        m_EventHelperComms.handleEvent(event); 
        List<CommsLayerBaseModel> transModels = m_SUT.getTransportsAsync(systemId1);
        assertThat(transModels.size(), is(1));
        
        m_EventHelperComms.handleEvent(event2);
        assertThat(transModels.get(0).getName(), is("transyTrans"));     
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event
        verify(m_EventAdmin, times(2)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));  
    }
    
    /**
     * Test proper handling of GetStatus request for link layer namespace.
     * Verify that appropriate link layer from the comms stack is set with the correct status.
     */
    @Test
    public void testHandleGetStatusResponse()
    {
        // Mini test setup
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event transport = mockGetLayersResponse(systemId1, "transport");
        Event link = mockGetLayersResponse(systemId1, "link");
        Event physical = mockGetLayersResponse(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(transport);    
        m_EventHelperComms.handleEvent(link);
        m_EventHelperComms.handleEvent(physical);

        //mock status event after set up
        Event event = mockGetStatusResponse(systemId1, LinkLayerMessages.LinkStatus.OK);
        m_EventHelperLink.handleEvent(event);
        
        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).getStatusString(), is("OK"));   
        
        event = mockGetStatusResponse(systemId1, LinkLayerMessages.LinkStatus.LOST);
        m_EventHelperLink.handleEvent(event);
        
        links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).getStatusString(), is("LOST")); 
        
        //test null case
        event = mockGetStatusResponse(systemId1, null);
        try
        {
            m_EventHelperLink.handleEvent(event);
            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            //verify push event for local link status response
            verify(m_EventAdmin, times(6)).postEvent(captor.capture());
            assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
        }
        catch(Exception e)
        {
            fail("Unexpected exception caught: " + e.getMessage());
        }        
    }
    
    /**
     * Test proper handling of IsActivated request for the link layer namespace.
     * Verify that the appropriate link layer from the comms stack is set with the correct isActivated value.
     */
    @Test
    public void testHandleIsActivatedResponse()
    {
        // Mini test setup
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event transport = mockGetLayersResponse(systemId1, "transport");
        Event link = mockGetLayersResponse(systemId1, "link");
        Event physical = mockGetLayersResponse(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(transport);    
        m_EventHelperComms.handleEvent(link);
        m_EventHelperComms.handleEvent(physical);
        
        //test activate response after mini set up
        Event event = mockIsActivatedResponse(systemId1, true);
        m_EventHelperLink.handleEvent(event);
        
        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).isActivated(), is(true)); 
        
        //test null case
        event = mockIsActivatedResponse(systemId1, null);
        try
        {
            m_EventHelperLink.handleEvent(event);
            ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
            //verify push event for local link layer activate response
            verify(m_EventAdmin, times(5)).postEvent(captor.capture());
            assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
        }
        catch(Exception e)
        {
            fail("Unexpected exception caught: " + e.getMessage());
        }
    }
    
    /**
     * Test proper handling of a controller removed event.
     * Verify that the comms stack objects for the removed controller no longer exists.
     */
    @Test
    public void testHandleControllerRemovedEvent()
    {
        // Mini test setup
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event transport = mockGetLayersResponse(systemId1, "transport");
        Event link = mockGetLayersResponse(systemId1, "link");
        Event physical = mockGetLayersResponse(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(transport);    
        m_EventHelperComms.handleEvent(link);
        m_EventHelperComms.handleEvent(physical);
        
        //assert that there are items in the map
        assertThat(m_SUT.getTransportsAsync(systemId1).size(), is(greaterThan(0)));
        assertThat(m_SUT.getLinksAsync(systemId1).size(), is(greaterThan(0)));
        assertThat(m_SUT.getPhysicalsAsync(systemId1).size(), is(greaterThan(0)));
   
        Event removedController = mockControllerRemovedEvent(systemId1);
        m_ControllerEventListener.handleEvent(removedController);
        
        //verify that all objects have been removed from each map.
        assertThat(m_SUT.getTransportsAsync(systemId1).size(), is(0));
        assertThat(m_SUT.getLinksAsync(systemId1).size(), is(0));
        assertThat(m_SUT.getPhysicalsAsync(systemId1).size(), is(0));
    }
    
    /**
     * Verify correct handling of layer added remotely
     */
    @Test
    public void testRemoteLayerAdded()
    {
        Event addRemoteLayer = mockEventLayerCreated(systemId1, "physical");
        m_EventHelper.handleEvent(addRemoteLayer);
        
        List<CommsLayerBaseModel> physicals = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physicals.size(), is(1)); 
        
        //check uuid and pid
        assertThat(physicals.get(0).getUuid(), is(uuid7));
        assertThat(physicals.get(0).getPid(), is(pid7));
        
        addRemoteLayer = mockEventLayerCreated(systemId1, "link");
        m_EventHelper.handleEvent(addRemoteLayer);
        
        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1); 
        assertThat(links.size(), is(1)); 
        
        //check uuid and pid
        assertThat(links.get(0).getUuid(), is(uuid7));
        assertThat(links.get(0).getPid(), is(pid7));
        
        verify(m_MessageFactory).createLinkLayerMessage(eq(LinkLayerMessageType.GetStatusRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(4)).queue(eq(systemId1), (ResponseHandler) eq(null));
        verify(m_MessageFactory).createLinkLayerMessage(eq(LinkLayerMessageType.IsActivatedRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(4)).queue(eq(systemId1), (ResponseHandler) eq(null));
        
        addRemoteLayer = mockEventLayerCreated(systemId1, "transport");
        m_EventHelper.handleEvent(addRemoteLayer);
        
        List<CommsLayerBaseModel> transports = m_SUT.getTransportsAsync(systemId1);
        assertThat(transports.size(), is(1)); 
        
        //check uuid and pid
        assertThat(transports.get(0).getUuid(), is(uuid7));
        assertThat(transports.get(0).getPid(), is(pid7));    
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event for remotely adding new layer
        verify(m_EventAdmin, times(3)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
    }
    
    /**
     * Verify correct handling of layer deleted remotely
     */
    @Test
    public void testRemoteLayerDeleted()
    {
        // Mini test setup
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event transport = mockGetLayersResponse(systemId1, "transport");
        Event link = mockGetLayersResponse(systemId1, "link");
        Event physical = mockGetLayersResponse(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(transport);    
        m_EventHelperComms.handleEvent(link);
        m_EventHelperComms.handleEvent(physical);
        
        List<CommsLayerBaseModel> transports = m_SUT.getTransportsAsync(systemId1);
        assertThat(transports.size(), is(1));  
        
        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.size(), is(2)); 
        
        List<CommsLayerBaseModel> physicals = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physicals.size(), is(3)); 
        
        Event remoteDeleteLayer = mockEventLayerDeleted(systemId1, "physical");
        m_EventHelper.handleEvent(remoteDeleteLayer);        
        physicals = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physicals.size(), is(2)); //size should be 1 smaller 
        
        remoteDeleteLayer = mockEventLayerDeleted(systemId1, "link");
        m_EventHelper.handleEvent(remoteDeleteLayer);
        links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.size(), is(1)); //size should be 1 smaller 
        
        remoteDeleteLayer = mockEventLayerDeleted(systemId1, "transport");
        m_EventHelper.handleEvent(remoteDeleteLayer);
        transports = m_SUT.getTransportsAsync(systemId1);
        assertThat(transports.size(), is(0)); 
    }
    
    /**
     * Verify layer is not deleted as a result of bad base type sent.
     */
    @Test
    public void testDeleteLayerWithBadBaseType()
    {
        m_SUT.getTransportsAsync(systemId1);
        Event transport = mockGetLayersResponse(systemId1, "transport");      
        m_EventHelperComms.handleEvent(transport);
        
        List<CommsLayerBaseModel> transports = m_SUT.getTransportsAsync(systemId1);
        assertThat(transports.size(), is(1)); 
        
        Event badBase = mockDeleteEventBadBaseType(systemId1);
        
        m_EventHelper.handleEvent(badBase);
        assertThat(transports.size(), is(1)); //layer should not have been successfully deleted  
    }
    
    /**
     * Verify that create configuration currently throw an unsupported exception. 
     */
    @Test
    public void testCreateConfiguration()
    {
        FactoryBaseModel model = mock(FactoryBaseModel.class);
        
        try
        {
            m_SUT.createConfiguration(1, model, new ArrayList<ModifiablePropertyModel>());
            fail("Expecting unsupported operation exception");
        }
        catch (UnsupportedOperationException exception)
        {
            
        }
    }
    
    /**
     * Verify proper handling of remote link activated event.
     */
    @Test
    public void testRemoteLinkActivated()
    {
        // Mini test setup
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event link = mockGetLayersResponse(systemId1, "link");
           
        m_EventHelperComms.handleEvent(link); 
        
        Event layerActivateEvent = mockIsActivatedResponse(systemId1, false); //set layer to be deactivated
        Event remoteLayerActivate = mockEventLinkActivated(systemId1, false);

        m_EventHelperLink.handleEvent(layerActivateEvent);
        m_EventHelper.handleEvent(remoteLayerActivate);

        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).isActivated(), is(true));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event for remote link update
        verify(m_EventAdmin, times(3)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
    }
    
    /**
     * Verify proper handling of remote link activated event.
     */
    @Test
    public void testRemoteLinkDoesNotExist()
    {
        // Mini test setup
        m_SUT.getPhysicalsAsync(systemId1);
        m_SUT.getLinksAsync(systemId1);
        m_SUT.getTransportsAsync(systemId1);
        Event link = mockGetLayersResponse(systemId1, "link");
           
        m_EventHelperComms.handleEvent(link); 
        
        Event layerActivateEvent = mockIsActivatedResponse(systemId1, false); 
        Event remoteLayerActivate = mockEventLinkActivated(systemId1, true); //set layer to be non-existent

        try
        {
            m_EventHelperLink.handleEvent(layerActivateEvent);
            m_EventHelper.handleEvent(remoteLayerActivate);
        }
        catch(Exception e)
        {
            fail("Unexpected exception caught: " + e.getMessage());
        }
    }
    
    /**
     * Verify proper handling of remote link deactivated event.
     */
    @Test
    public void testRemoteLinkDeactivated()
    {
        // Mini test setup
        m_SUT.getLinksAsync(systemId1);
        Event link = mockGetLayersResponse(systemId1, "link");
        
        m_EventHelperComms.handleEvent(link);
        
        Event layerActivateEvent = mockIsActivatedResponse(systemId1, true); //set layer to be deactivated
        Event remoteLayerActivate = mockEventLinkDeactivated(systemId1);

        m_EventHelperLink.handleEvent(layerActivateEvent);
        m_EventHelper.handleEvent(remoteLayerActivate);

        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).isActivated(), is(false)); 
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event for is activated response
        verify(m_EventAdmin, times(3)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
    }
    
    /**
     * Verify proper handling of remote link status change event.
     */
    @Test
    public void testRemoteLinkStatusChange()
    {
        // Mini test setup
        m_SUT.getLinksAsync(systemId1);
        Event link = mockGetLayersResponse(systemId1, "link");
        
        m_EventHelperComms.handleEvent(link);
        
        Event layerStatusEvent = mockGetStatusResponse(systemId1, LinkLayerMessages.LinkStatus.OK);
        Event remoteLayerStatusChanged = mockEventLinkStatusChanged(systemId1, LinkStatus.LOST);

        m_EventHelperLink.handleEvent(layerStatusEvent);
        m_EventHelper.handleEvent(remoteLayerStatusChanged);

        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).getStatus(), is(LinkLayer.LinkStatus.LOST)); 
        
        m_EventHelper.handleEvent(remoteLayerStatusChanged);
        remoteLayerStatusChanged = mockEventLinkStatusChanged(systemId1, LinkStatus.OK);
        m_EventHelper.handleEvent(remoteLayerStatusChanged);
        links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).getStatus(), is(LinkLayer.LinkStatus.OK));    
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event for get status response
        verify(m_EventAdmin, times(5)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
    }
    
    /**
     * Verify proper handling of remote link name change event.
     */
    @Test
    public void testRemoteLinkNameChange()
    {
        // add controller and layer
        m_SUT.getLinksAsync(systemId1);
        Event link = mockGetLayersResponse(systemId1, "link");
        
        m_EventHelperComms.handleEvent(link);
        
        List<CommsLayerLinkModelImpl> links = m_SUT.getLinksAsync(systemId1);
        //link with uuid2 is changed verify assumption that it is the first layer
        assertThat(links.get(0).getUuid(), is(uuid2));
        String name = links.get(0).getName();
        
        //name change events
        Event nameChange = mockEventNameUpdated(systemId1, "link");
        
        //handle event
        m_EventHelper.handleEvent(nameChange);

        links = m_SUT.getLinksAsync(systemId1);
        assertThat(links.get(0).getUuid(), is(uuid2));
        assertThat(links.get(0).getName(), is("linky"));
        assertThat(name, is(not("linky")));
        
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        //verify push event for get layers response
        verify(m_EventAdmin, times(2)).postEvent(captor.capture());
        assertThat(captor.getValue().getTopic(), is(CommsMgr.TOPIC_COMMS_LAYER_UPDATED));
    }
    
    /**
     * Verify all known physical links are retrieved.
     */
    @Test 
    public void testGetPhysicalsAsync()
    {
        List<CommsLayerBaseModel> list = m_SUT.getPhysicalsAsync(systemId1);
        
        assertThat(list.isEmpty(), is(true));        
        verify(m_MessageFactory).createCustomCommsMessage(eq(CustomCommsMessageType.GetLayersRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(systemId1), (ResponseHandler) eq(null));        
        
        Event event = mockGetLayersResponse(systemId1, "physical");
        m_EventHelperComms.handleEvent(event); 
        
        list = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(list.size(), is(3)); //contents are verified in test handle get layers response
    }
    
    /**
     * Verify all known link layers are retrieved. 
     */
    @Test
    public void testGetLinksAsync()
    {
        List<CommsLayerLinkModelImpl> list = m_SUT.getLinksAsync(systemId1);
        
        assertThat(list.isEmpty(), is(true));
        verify(m_MessageFactory).createCustomCommsMessage(eq(CustomCommsMessageType.GetLayersRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(systemId1), (ResponseHandler) eq(null));     
           
        Event event = mockGetLayersResponse(systemId1, "link");
        m_EventHelperComms.handleEvent(event); 
        
        list = m_SUT.getLinksAsync(systemId1);
        assertThat(list.size(), is(2)); //contents are verified in test handle get layers response        
    }
    
    /**
     * Verify all known transport layers are retrieved. 
     */
    @Test 
    public void testGetTransportsAsync()
    {
        List<CommsLayerBaseModel> list = m_SUT.getTransportsAsync(systemId1);
        
        assertThat(list.isEmpty(), is(true));
        verify(m_MessageFactory).createCustomCommsMessage(eq(CustomCommsMessageType.GetLayersRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper, times(4)).queue(eq(systemId1), (ResponseHandler) eq(null));
        
        Event event = mockGetLayersResponse(systemId1, "transport");
        m_EventHelperComms.handleEvent(event); 
        
        list = m_SUT.getTransportsAsync(systemId1);
        assertThat(list.size(), is(1)); //contents are verified in test handle get layers response        
    }
    
    /**
     * Test the handling of create link layer response handler.
     * Verify the handler sends create transport layer requests.
     */
    @Test
    public void testCreateLinkLayerResponseHandler()
    {
        CreateTransportLayerRequestData.Builder transportBuild = CreateTransportLayerRequestData.newBuilder().
                setTransportLayerName("immaTranny").
                setTransportLayerProductType(CommsLayerBaseModel.class.getName()); 
        
        RemoteCreateLinkLayerHandler handler = m_SUT.createLinkLayerHandler(transportBuild);

        //response messages to process
        UUID linkUuid = UUID.randomUUID();
        
        FactoryObjectInfo info = FactoryObjectInfo.newBuilder()
                .setPid("myPid")
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(linkUuid))
                .setProductType("testLinkLayer")
                .build();
        
        CreateLinkLayerResponseData response = CreateLinkLayerResponseData.newBuilder()
                .setInfo(info)
                .build();

        CustomCommsNamespace nameResponse1 = CustomCommsNamespace.newBuilder().
            setData(response.toByteString()).
            setType(CustomCommsMessageType.CreateLinkLayerResponse).build();

        TerraHarvestPayload payload1 = TerraHarvestPayload.newBuilder().setNamespace(Namespace.CustomComms).
            setNamespaceMessage(response.toByteString()).build();  
            
        TerraHarvestMessage thMessage1 = TerraHarvestMessageHelper.createTerraHarvestMessage(systemId1, 0,
            Namespace.CustomComms, 123, nameResponse1);
        
        //handle fake response
        handler.handleResponse(thMessage1, payload1, nameResponse1, response);
        
        //verify that handler will send create transport layer request
        verify(m_MessageFactory).createCustomCommsMessage(eq(CustomCommsMessageType.CreateTransportLayerRequest),
            Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(systemId1), (ResponseHandler) eq(null));
    }
    
    /**
     * Verify proper handling of remote transport name change event.
     */
    @Test
    public void testRemoteTransportName()
    {
        // add layer
        m_SUT.getTransportsAsync(systemId1);
        Event transLayers = mockGetLayersResponse(systemId1, "transport");
        
        m_EventHelperComms.handleEvent(transLayers);
        
        List<CommsLayerBaseModel> trans = m_SUT.getTransportsAsync(systemId1);
        //trans layer with uuid1 is changed verify assumption that it is the first layer
        assertThat(trans.get(0).getUuid(), is(uuid1));
        String name = trans.get(0).getName();
        
        //name change events
        Event nameChange = mockEventNameUpdated(systemId1, "transport");
        
        //handle event
        m_EventHelper.handleEvent(nameChange);

        trans = m_SUT.getTransportsAsync(systemId1);
        assertThat(trans.get(0).getUuid(), is(uuid1));
        assertThat(trans.get(0).getName(), is("transy"));
        assertThat(name, is(not("transy")));
    }
    
    /**
     * Verify proper handling of remote physical link name change event.
     */
    @Test
    public void testRemotePhysLinkNameChange()
    {
        // add controller and layer
        m_SUT.getPhysicalsAsync(systemId1);
        Event physicals = mockGetLayersResponse(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(physicals);
        
        List<CommsLayerBaseModel> physicalsLinks = m_SUT.getPhysicalsAsync(systemId1);
        //physical link with uuid3 is changed verify assumption that it is the first layer
        assertThat(physicalsLinks.get(0).getUuid(), is(uuid3));
        String name = physicalsLinks.get(0).getName();
        
        //name change events
        Event nameChange = mockEventNameUpdated(systemId1, "physical");
        
        //handle event
        m_EventHelper.handleEvent(nameChange);

        physicalsLinks = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physicalsLinks.get(0).getUuid(), is(uuid3));
        assertThat(physicalsLinks.get(0).getName(), is("fizzy"));
        assertThat(name, is(not("fizzy")));
    }
    
    /**
     * Verify correct physical UUID is returned based on given name. 
     */
    @Test
    public void testGetPhysicalUuidByName()
    {
        // add controller and layer
        m_SUT.getPhysicalsAsync(systemId1);
        Event physicals = mockGetLayersResponse(systemId1, "physical");
        Event nameChange = mockEventNameUpdated(systemId1, "physical");

        m_EventHelperComms.handleEvent(physicals);
        m_EventHelper.handleEvent(nameChange);
        
        assertThat(m_SUT.getPhysicalUuidByName("fizzy", systemId1), is(uuid3));
        
        assertThat(m_SUT.getPhysicalUuidByName("jennyJenny", 8675309), is(nullValue()));
    }
    
    /**
     * Verify class type of physical layer is returned based on given layer name.
     */
    @Test
    public void testGetPhysicalClazzByName()
    {
        // add controller and layer
        m_SUT.getPhysicalsAsync(systemId1);
        Event physicals = mockGetLayersResponse(systemId1, "physical");
        Event nameChange = mockEventNameUpdated(systemId1, "physical");

        m_EventHelperComms.handleEvent(physicals);
        m_EventHelper.handleEvent(nameChange);
        
        assertThat(m_SUT.getPhysicalClazzByName("fizzy", systemId1), is(PhysicalLink.class.getName()));
        
        assertThat(m_SUT.getPhysicalClazzByName("jennyJenny", 8675309), is(nullValue()));
    }
    
    /**
     * Verify unused physical links are properly returned.
     */
    @Test
    public void testUnusedPhysicalLink()
    {
        // add controller and layer
        m_SUT.getPhysicalsAsync(systemId1);
        Event physicals = mockGetLayersResponse(systemId1, "physical");
        Event name = mockEventNameUpdated(systemId1, "physical");
        
        m_EventHelperComms.handleEvent(physicals);
        m_EventHelper.handleEvent(name);
        
        List<CommsLayerBaseModel> physicalList = m_SUT.getPhysicalsAsync(systemId1);
        assertThat(physicalList.size(), is(3));
        physicalList.remove(2);
        physicalList.remove(1);
        
        m_SUT.setUnusedPhysicalLinks(systemId1, physicalList);
        List<String> unusedNames = m_SUT.getUnusedPhysicalLinkNames(systemId1);
        
        assertThat(unusedNames.size(), is(1));
        assertThat(unusedNames.get(0), is("fizzy"));
        
    }
    
    /**
     * Helper method for mocking a GetLayersResponse from the controller.
     */
    private Event mockGetLayersResponse(final int systemId, String type)
    {
        List<FactoryObjectInfo> layerInfos = new ArrayList<FactoryObjectInfo>();
        Message response = null;
        
        if(type.equals("transport"))
        {
            FactoryObjectInfo factoryInfo = FactoryObjectInfo.newBuilder().
                    setPid(pid1).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).
                    setProductType(TransportLayer.class.getName()).build();
            
            layerInfos.add(factoryInfo);
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.TransportLayer).build();
        }
        else if(type.equals("link"))
        {
            FactoryObjectInfo factoryInfo = FactoryObjectInfo.newBuilder().
                    setPid(pid2).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid2)).
                    setProductType(LinkLayer.class.getName()).build();
            
            FactoryObjectInfo factoryInfo2 = FactoryObjectInfo.newBuilder().
                    setPid(pid4).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid4)).
                    setProductType(LinkLayer.class.getName()).build();
            
            layerInfos.add(factoryInfo);
            layerInfos.add(factoryInfo2);
            
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.Linklayer).build();
        }
        else
        {
            FactoryObjectInfo factoryInfo = FactoryObjectInfo.newBuilder().
                    setPid(pid3).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid3)).
                    setProductType(PhysicalLink.class.getName()).build();
            
            FactoryObjectInfo factoryInfo2 = FactoryObjectInfo.newBuilder().
                    setPid(pid5).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid5)).
                    setProductType(PhysicalLink.class.getName()).build();
            
            FactoryObjectInfo factoryInfo3 = FactoryObjectInfo.newBuilder().
                    setPid(pid6).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid6)).
                    setProductType(PhysicalLink.class.getName()).build();
            
            layerInfos.add(factoryInfo);
            layerInfos.add(factoryInfo2);
            layerInfos.add(factoryInfo3);
            
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.PhysicalLink).build();
        }
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetLayersResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);        
    }  
    
    /**
     * Helper method for mocking a GetLayerNameResponse from the controller. 
     */
    private Event mockGetLayerNameResponse(final int systemId, CommType type, UUID uuid, String name)
    {
        GetLayerNameResponseData response = GetLayerNameResponseData.newBuilder().setCommType(type).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).setLayerName(name).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetLayerNameResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props); 
    }

    /**
     * Helper method for mocking a GetLayersResponse from the controller.
     */
    private Event mockGetLayersResponseMergeLayers(final int systemId, String type, UUID uuid, String pid)
    {
        List<FactoryObjectInfo> layerInfos = new ArrayList<FactoryObjectInfo>();
        Message response = null;
        
        if(type.equals("transport"))
        {
            FactoryObjectInfo factoryInfo = FactoryObjectInfo.newBuilder().
                    setPid(pid).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).
                    setProductType(TransportLayer.class.getName()).build();
            
            layerInfos.add(factoryInfo);
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.TransportLayer).build();
        }
        else if(type.equals("link"))
        {
            FactoryObjectInfo factoryInfo = FactoryObjectInfo.newBuilder().
                    setPid(pid).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).
                    setProductType(LinkLayer.class.getName()).build();
            
            layerInfos.add(factoryInfo);
            
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.Linklayer).build();
        }
        else
        {
            FactoryObjectInfo factoryInfo = FactoryObjectInfo.newBuilder().
                    setPid(pid).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).
                    setProductType(PhysicalLink.class.getName()).build();
            
            layerInfos.add(factoryInfo);
            
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.PhysicalLink).build();
        }
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetLayersResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);        
    }  
    
    /**
     * Helper method for mocking a GetLayersResponse from the controller.
     */
    private Event mockGetLayersResponseMergeLayersEmpty(final int systemId, String type)
    {
        List<FactoryObjectInfo> layerInfos = new ArrayList<FactoryObjectInfo>();
        Message response = null;
        
        if(type.equals("transport"))
        {
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.TransportLayer).build();
        }
        else if(type.equals("link"))
        {            
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.Linklayer).build();
        }
        else
        {            
            response = GetLayersResponseData.newBuilder().addAllLayerInfo(layerInfos).
                    setCommType(CommType.PhysicalLink).build();
        }
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.CustomComms.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            CustomCommsMessageType.GetLayersResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);        
    }
    
    /**
     * Helper method for mocking a GetStatusResponse from the controller.
     */
    private Event mockGetStatusResponse(final int systemId, LinkLayerMessages.LinkStatus status)
    {
        Message response = null;
        if (status == null)
        {
            response = GetStatusResponseData.newBuilder().setLinkStatus(LinkLayerMessages.LinkStatus.LOST).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).build();//should not be a valid link uuid
        }
        else
        {
            response = GetStatusResponseData.newBuilder().setLinkStatus(status).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid2)).build();
        }

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.LinkLayer.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            LinkLayerMessageType.GetStatusResponse.toString());
        
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /**
     * Helper method for mocking a IsActivatedResponse from the controller.
     */
    private Event mockIsActivatedResponse(final int systemId, Boolean isActivated)
    {
        Message response = null;
        if (isActivated == null)
        {
            // pass invalid link uuid
            response = IsActivatedResponseData.newBuilder().setIsActivated(true).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid1)).build();
        }
        else
        {
            response = IsActivatedResponseData.newBuilder().setIsActivated(isActivated).
                    setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid2)).build();
        }        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, response);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.LinkLayer.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            LinkLayerMessageType.IsActivatedResponse.toString());
        
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);       
    }
    
    /**
     * Create an event that mimics a controller being removed.
     * @param systemId
     *     the system id to be used as the removed controller id
     * @return
     *     an event that is of the type and structure expected when a controller is removed
     */
    private Event mockControllerRemovedEvent(final int systemId)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, systemId);
        //the event
        return new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
    }
    
    /**
     * Used to mock a comms layer created event. 
     */
    private Event mockEventLayerCreated(final int systemId, String type)
    {
        if(type.equals("physical"))
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid7.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, PhysicalLink.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid7);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "PhysicalLink");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        }
        else if(type.equals("link"))
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid7.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid7);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        }
        else
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid7.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, TransportLayer.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid7);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "TransportLayer");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        }
    }
    
    /**
     * Used to mock a comms layer deleted event.
     */
    private Event mockEventLayerDeleted(final int systemId, String type)
    {
        if(type.equals("physical"))
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid3.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, PhysicalLink.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid3);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "PhysicalLink");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        }
        else if(type.equals("link"))
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid2);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        }
        else
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid1.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, TransportLayer.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid1);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "TransportLayer");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
        }
    }
    
    /**
     * Used to mock a link activated event.
     */
    private Event mockEventLinkActivated(final int systemId, boolean isNull)
    {
        if(isNull)
        {
         // properties for the event with bad link layer
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid1.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid2);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(LinkLayer.TOPIC_ACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
                props);
        }
        else
        {
         // properties for the event
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid2);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
            return new Event(LinkLayer.TOPIC_ACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
                props);            
        }
    }
    
    /**
     * Used to mock an event for a link deactivation.
     */
    private Event mockEventLinkDeactivated(final int systemId)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid2);
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(LinkLayer.TOPIC_DEACTIVATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
            props); 
    }
    
    /**
     * Used to mock a remote event representing a change in status for a link layer.
     */
    private Event mockEventLinkStatusChanged(final int systemId, LinkLayer.LinkStatus status)
    {
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid2);
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
        props.put(LinkLayer.EVENT_PROP_LINK_STATUS, status);
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(LinkLayer.TOPIC_STATUS_CHANGED + RemoteConstants.REMOTE_TOPIC_SUFFIX, 
            props);
    }
    
    /**
     * Used to mock a message from a remote controller representing a link layer's name was successfully updated. 
     */
    private Event mockEventNameUpdated(final int systemId, final String type)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        if (type.equals("transport"))
        {
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "transy");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid1.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid1);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "TransportLayer");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, TransportLayer.class.getName());
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        }
        else if (type.equals("link"))
        {
            // properties for the event            
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "linky");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid2.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid2);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "LinkLayer");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, LinkLayer.class.getName());
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId); 
        }
        else if (type.equals("physical"))
        {
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "fizzy");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid3.toString());
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid3);
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "PhysicalLink");
            props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, PhysicalLink.class.getName());
            props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        }
        //the event
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }
    
    /**
     * Event used to test that the program will not crash as a result of a bad basetype
     */
    private Event mockDeleteEventBadBaseType(final int systemId)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, "name");
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_UUID, uuid1.toString());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_TYPE, TransportLayer.class.getName());
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_PID, pid1);
        props.put(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE, "fooType");
        props.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(FactoryDescriptor.TOPIC_FACTORY_OBJ_DELETED + RemoteConstants.REMOTE_TOPIC_SUFFIX, props);
    }   
}
