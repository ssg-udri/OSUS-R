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
package mil.dod.th.ose.gui.webapp.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.
    AssetDirectoryServiceNamespace.AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ProgramInfo;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.mp.CurrentMissionMgrImpl.EventHelperControllerEvent;
import mil.dod.th.ose.gui.webapp.mp.CurrentMissionMgrImpl.EventHelperMissions;
import mil.dod.th.ose.gui.webapp.mp.CurrentMissionMgrImpl.EventHelperRemoteEvents;
import mil.dod.th.ose.gui.webapp.mp.CurrentMissionMgrImpl.MissionAction;
import mil.dod.th.ose.gui.webapp.mp.CurrentMissionMgrImpl.MissionErrorHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen.MissionProgramSchedule;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;

/**
 * Tests the current mission manager class.
 * @author nickmarcucci
 *
 */
public class TestCurrentMissionMgrImpl
{
    private CurrentMissionMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private EventAdmin m_EventAdmin;
    private GrowlMessageUtil m_GrowlUtil;
    private BundleContextUtil m_BundleUtil;
    private EventHelperMissions m_MissionEventHelper;
    private EventHelperControllerEvent m_ControllerEventHelper;
    private EventHelperRemoteEvents m_RemoteEventHelper;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_HandlerReg;
    private MessageWrapper m_MessageWrapper;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        m_SUT = new CurrentMissionMgrImpl();
        
        m_MessageFactory = mock(MessageFactory.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        m_HandlerReg = mock(ServiceRegistration.class);
        
        when(bundleContext.registerService(eq(EventHandler.class),
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        
        m_SUT.initAndRegisterEvents();
        
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(3)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(3)).getBundleContext();
        
        m_MissionEventHelper = (EventHelperMissions)captor.getAllValues().get(0);
        m_ControllerEventHelper = (EventHelperControllerEvent)captor.getAllValues().get(1);
        m_RemoteEventHelper = (EventHelperRemoteEvents)captor.getAllValues().get(2);
        
        when(m_MessageFactory.createMissionProgrammingMessage(Mockito.any(MissionProgrammingMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        when(m_MessageFactory.createEventAdminMessage(Mockito.any(EventAdminMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
    }
    
    /**
     * Verify cleanup method unregisters all handlers.
     */
    @Test
    public void testCleanUp()
    {
        m_SUT.cleanup();
        
        verify(m_HandlerReg, times(3)).unregister();
    }
    
    /**
     * Verify proper missions are returned for a controller.
     */
    @Test
    public void testGetCurrentMissionsForController()
    {
        //verify first time with no controller requests are sent out
        List<CurrentMissionModel> expectedModels = m_SUT.getCurrentMissionsForControllerAsync(123);
        
        assertThat(expectedModels.size(), is(0));
        
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.GetProgramInformationRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(123), (ResponseHandler) eq(null));
        
        ArgumentCaptor<Message> eventCaptor = ArgumentCaptor.forClass(Message.class);
        
        verify(m_MessageFactory).createEventAdminMessage(eq(EventAdminMessageType.EventRegistrationRequest), 
                eventCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(123), Mockito.any(ResponseHandler.class));
        
        EventRegistrationRequestData requestData = (EventRegistrationRequestData)eventCaptor.getValue();
        
        assertThat(requestData.getTopicCount(), is(2));
        
        List<String> topics = requestData.getTopicList();
        
        assertThat(topics.contains(Program.TOPIC_PREFIX + "*"), is(true));
        
        assertThat(topics.contains(MissionProgramManager.TOPIC_PREFIX + "*"), is(true));
        
        //call again with same controller
        List<CurrentMissionModel> expectedModels2 = m_SUT.getCurrentMissionsForControllerAsync(123); 
        assertThat(expectedModels2.size(), is(0));
        
        verify(m_MessageFactory, times(1)).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.GetProgramInformationRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(123), (ResponseHandler) eq(null));

        verify(m_MessageFactory, times(1)).createEventAdminMessage(
                eq(EventAdminMessageType.EventRegistrationRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(123), (ResponseHandler) eq(null));
        
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
    }
    
    /**
     * Verify execute test request is sent out.
     */
    @Test
    public void testExecuteTestRequest()
    {
        m_SUT.executeTestRequest("Hawks", 123);
        
        ArgumentCaptor<Message> requestCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.ExecuteTestRequest),
                requestCaptor.capture());
        verify(m_MessageWrapper).queue(eq(123), Mockito.any(MissionErrorHandler.class));
        
        ExecuteTestRequestData message = (ExecuteTestRequestData)requestCaptor.getValue();
        
        assertThat(message.getMissionName(), is("Hawks"));
    }
    
    /**
     * Verify that an execute mission request is sent out.
     */
    @Test
    public void testExecuteExecuteRequest()
    {
        m_SUT.executeExecuteRequest("Hawks", 123);
        
        ArgumentCaptor<Message> requestCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.ExecuteRequest),
                requestCaptor.capture());
        verify(m_MessageWrapper).queue(eq(123), Mockito.any(MissionErrorHandler.class));
        
        ExecuteRequestData message = (ExecuteRequestData)requestCaptor.getValue();
        
        assertThat(message.getMissionName(), is("Hawks"));
    }
    
    /**
     * Verify that a shutdown request is sent out.
     */
    @Test
    public void testExecuteShutdownRequest()
    {
        m_SUT.executeShutdownRequest("Hawks", 123);
        
        ArgumentCaptor<Message> requestCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.ExecuteShutdownRequest),
                requestCaptor.capture());
        verify(m_MessageWrapper).queue(eq(123), Mockito.any(MissionErrorHandler.class));
        
        ExecuteShutdownRequestData message = (ExecuteShutdownRequestData)requestCaptor.getValue();
        
        assertThat(message.getMissionName(), is("Hawks"));
    }
    
    /**
     * Verify that a cancel request is sent out.
     */
    @Test
    public void testExecuteCancelRequest()
    {
        m_SUT.executeCancelRequest("Hawks", 123);
        
        ArgumentCaptor<Message> requestCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.CancelProgramRequest),
                requestCaptor.capture());
        verify(m_MessageWrapper).queue(eq(123), Mockito.any(MissionErrorHandler.class));
        
        CancelProgramRequestData message = (CancelProgramRequestData)requestCaptor.getValue();
        
        assertThat(message.getMissionName(), is("Hawks"));
    }
    
    /**
     * Verify that a remove request is sent out.
     */
    @Test
    public void testExecuteRemoveRequest()
    {
        m_SUT.executeRemoveRequest("Hawks", 123);
        
        ArgumentCaptor<Message> requestCaptor = ArgumentCaptor.forClass(Message.class);
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.RemoveMissionProgramRequest),
                requestCaptor.capture());
        verify(m_MessageWrapper).queue(eq(123), Mockito.any(MissionErrorHandler.class));
        
        RemoveMissionProgramRequestData message = (RemoveMissionProgramRequestData)requestCaptor.getValue();
        
        assertThat(message.getMissionName(), is("Hawks"));
    }
    
    /**
     * Verify mission error handler properly handles error messages.
     */
    @Test
    public void testMissionErrorHandler()
    {
        m_SUT.executeShutdownRequest("Hawks", 123);
        
        ArgumentCaptor<MissionErrorHandler> handleCaptor = ArgumentCaptor.forClass(MissionErrorHandler.class);
        verify(m_MessageFactory).createMissionProgrammingMessage(
                eq(MissionProgrammingMessageType.ExecuteShutdownRequest),
                Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(123), handleCaptor.capture());
        
        assertThat(handleCaptor.getValue(), notNullValue());
        
        MissionErrorHandler handler = handleCaptor.getValue();
        
        GenericErrorResponseData data = GenericErrorResponseData.newBuilder().
                setError(ErrorCode.INTERNAL_ERROR).setErrorDescription("O man more errors").build();
        
        BaseNamespace nameResponse = BaseNamespace.newBuilder().
                setData(data.toByteString()).
                setType(BaseMessageType.GenericErrorResponse).build();
        
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder().setNamespace(Namespace.Base).
                setNamespaceMessage(data.toByteString()).build();    
        
        TerraHarvestMessage message = TerraHarvestMessageHelper.
                createTerraHarvestMessage(123, 0, Namespace.Base, 1, data);
        
        handler.handleResponse(message, payload, nameResponse, data);
        
        ArgumentCaptor<String> errMsgCaptor = ArgumentCaptor.forClass(String.class);
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Mission Error Occurred"), errMsgCaptor.capture());
        
        
        assertThat(errMsgCaptor.getValue().contains("O man more errors"), is(true));
        assertThat(errMsgCaptor.getValue().contains(ErrorCode.INTERNAL_ERROR.toString()), is(true));
    }
    
    /**
     * Verify with right namespace but wrong type nothing happens.
     */
    @Test
    public void testMissionErrorHandlerWithNamespaceWrongType()
    {
        MissionErrorHandler errHandler = m_SUT.new MissionErrorHandler("name", MissionAction.EXECUTE);
        
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Base)
                .setNamespaceMessage(ByteString.EMPTY).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(payload);
        BaseNamespace namespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GetControllerCapabilitiesResponse)
                .setData(ByteString.EMPTY).build();
        ExecuteCommandResponseData dataMsg = ExecuteCommandResponseData.newBuilder()
                .setResponse(ByteString.EMPTY)
                .setResponseType(CommandTypesGen.CommandResponse.Enum.CAPTURE_IMAGE_RESPONSE)
                .setUuid(UUID.newBuilder().setLeastSignificantBits(0).setMostSignificantBits(0).build())
                .build();
        
        errHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Mission Error Occurred"), Mockito.anyString());
    }
    
    /**
     * Verify mission error handle does nothing when namespace is not correct.
     */
    @Test
    public void testMissionErrorHandlerWithWrongNamespace() throws UnsupportedEncodingException
    {
        MissionErrorHandler errHandler = m_SUT.new MissionErrorHandler("name", MissionAction.EXECUTE);

        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Asset)
                .setNamespaceMessage(ByteString.EMPTY).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(payload);
        AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace namespaceMessage = 
                AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.GetCapabilitiesResponse)
                .setData(ByteString.EMPTY).build();
        ExecuteCommandResponseData dataMsg = ExecuteCommandResponseData.newBuilder()
               .setResponseType(CommandTypesGen.CommandResponse.Enum.CAPTURE_IMAGE_RESPONSE)
                .setResponse(ByteString.EMPTY)
                .setUuid(UUID.newBuilder()
                    .setLeastSignificantBits(0)
                    .setMostSignificantBits(0).build()).build();

        errHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Mission Error Occurred"), Mockito.anyString());
    }
    
    /**
     * Verify program status is correctly translated.
     */
    @Test
    public void testTranslateProgramStatus()
    {
        //expect running
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.EXECUTED), is("RUNNING"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.EXECUTING), is("RUNNING"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.SHUTTING_DOWN), is("RUNNING"));
        
        //expect loading
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.WAITING_UNINITIALIZED), is("LOADING"));
        
        //expect loaded
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.WAITING_INITIALIZED), is("LOADED"));
        
        //expect testing
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.EXECUTING_TEST), is("TESTING"));
        
        //expect error
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.VARIABLE_ERROR), is("ERROR"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.INITIALIZATION_ERROR), is("ERROR"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.SCRIPT_ERROR), is("ERROR"));
        
        //expect the status that is passed in
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.CANCELED), is("CANCELED"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.SCHEDULED), is("SCHEDULED"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.SHUTDOWN), is("SHUTDOWN"));
        assertThat(m_SUT.translateProgramStatus(ProgramStatus.UNSATISFIED), is("UNSATISFIED"));
    }
    
    /**
     * Verify that remote event program status changed is properly handled.
     */
    @Test
    public void testRemoteEventProgramStatusChanged()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        //get an event for the Program Status changed
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.SCRIPT_ERROR.toString());
        
        Event statusEvent = mockARemoteEvent(123, Program.TOPIC_PROGRAM_STATUS_CHANGED, "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(statusEvent);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), eq("Mission Status Updated"), 
                eq("The mission Mission 1 for system 0x0000007b has been updated to the status ERROR."));
        
        //verify status has changed
        CurrentMissionModel modelToTest = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest, notNullValue());
        assertThat(modelToTest.getMissionStatus(), is(ProgramStatus.SCRIPT_ERROR));
        
        //verify logging message when mission name is wrong 
        map.clear();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.EXECUTED.toString());
        Event eventNoGrowl = mockARemoteEvent(123, Program.TOPIC_PROGRAM_STATUS_CHANGED, "Mission 1", map);
        m_RemoteEventHelper.handleEvent(eventNoGrowl);
        
        CurrentMissionModel modelToTest2 = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest2, notNullValue());
        assertThat(modelToTest2.getMissionStatus(), is(ProgramStatus.EXECUTED));
        
        verify(m_GrowlUtil, times(1)).createGlobalFacesMessage(
                eq(FacesMessage.SEVERITY_INFO), eq("Mission Status Updated"),
                Mockito.anyString());
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        //verify push events are called 
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
         
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
        
        Event capturedEvent3 = eventCaptor.getAllValues().get(2);
        
        assertThat((String)capturedEvent3.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    /**
     * Verify program executed failure event is properly handled.
     */
    @Test
    public void testRemoteEventProgramExecutedFailureEvent()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        //get an event for the Program Status changed
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.SCRIPT_ERROR.toString());
        map.put(Program.EVENT_PROP_PROGRAM_EXCEPTION, "There is an error");
        
        Event failureEvent = mockARemoteEvent(123, Program.TOPIC_PROGRAM_EXECUTED_FAILURE, "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(failureEvent);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Mission Failed Execution!"), contains("There is an error"));
        
        //verify status has changed
        CurrentMissionModel modelToTest = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest, notNullValue());
        assertThat(modelToTest.getMissionStatus(), is(ProgramStatus.SCRIPT_ERROR));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        //verify push events are called 
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
         
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    /**
     * Verify program executed event is properly handled.
     */
    @Test
    public void testRemoteEventProgramExecutedEvent()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        //get an event for the Program Status changed
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.EXECUTED.toString());
        
        Event failureEvent = mockARemoteEvent(123, Program.TOPIC_PROGRAM_EXECUTED, "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(failureEvent);
        
        //verify status has changed
        CurrentMissionModel modelToTest = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest, notNullValue());
        assertThat(modelToTest.getMissionStatus(), is(ProgramStatus.EXECUTED));
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("Mission Executed."), contains("The mission Mission 1 for system 0x0000007b has been executed."));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        //verify push events are called 
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
         
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    /**
     * Verify program shutdown event is properly handled.
     */
    @Test
    public void testRemoteEventProgramShutdownEvent()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        //get an event for the Program Status changed
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.SHUTDOWN.toString());
        
        Event shutdownEvent = mockARemoteEvent(123, Program.TOPIC_PROGRAM_SHUTDOWN, "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(shutdownEvent);
        
        //verify status has changed
        CurrentMissionModel modelToTest = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest, notNullValue());
        assertThat(modelToTest.getMissionStatus(), is(ProgramStatus.SHUTDOWN));
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("Mission Shutdown"), eq("The mission Mission 1 for system 0x0000007b has been shutdown."));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        //verify push events are called 
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
         
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    @Test
    public void testRemoteEventProgramTestedEvent()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        //get an event for the Program Status changed
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.WAITING_INITIALIZED.toString());
        map.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT, TestResult.PASSED);
        map.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION, "");
        
        Event testedEventPassed = mockARemoteEvent(123, Program.TOPIC_PROGRAM_TESTED, "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(testedEventPassed);
        
        //verify status has changed
        CurrentMissionModel modelToTest = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest, notNullValue());
        assertThat(modelToTest.getMissionStatus(), is(ProgramStatus.WAITING_INITIALIZED));
        assertThat(modelToTest.getLastTestResult(), is(TestResult.PASSED));
        
        
        //test with a failure exception
        map.clear();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.WAITING_INITIALIZED.toString());
        map.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT, TestResult.FAILED);
        map.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION, "O man another exception.");
        
        Event testEventFailed = mockARemoteEvent(123, Program.TOPIC_PROGRAM_TESTED, "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(testEventFailed);
        
        //verify status has changed
        CurrentMissionModel modelToTest2 = findAMissionModel(123, "Mission 1");
        
        assertThat(modelToTest2, notNullValue());
        assertThat(modelToTest2.getMissionStatus(), is(ProgramStatus.WAITING_INITIALIZED));
        assertThat(modelToTest2.getLastTestResult(), is(TestResult.FAILED));
        
        //verify growl messages 
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                eq("Mission Test"), eq("The mission Mission 1 for system 0x0000007b has been tested."));
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_WARN), 
                eq("Mission Test"), eq("The mission Mission 1 for system 0x0000007b has been tested and has " 
                        + "resulted in the following error O man another exception."));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(3)).postEvent(eventCaptor.capture());
        
        //verify push events are called 
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
         
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
        
        Event capturedEvent3 = eventCaptor.getAllValues().get(2);
        
        assertThat((String)capturedEvent3.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    /**
     * Verify program added event is properly handled.
     */
    @Test
    public void testRemoteEventProgramAddedEvent()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        Event programAdded = mockARemoteEvent(123, MissionProgramManager.TOPIC_PROGRAM_ADDED, 
                "Mission 4", new HashMap<String, Object>());
        
        m_RemoteEventHelper.handleEvent(programAdded);
        
        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        
        //init function above causes the first GetProgramInformationRequest message. We are just concerned 
        //with the second message
        verify(m_MessageFactory, times(2)).createMissionProgrammingMessage(eq(MissionProgrammingMessageType.
                GetProgramInformationRequest), msgCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(123), (ResponseHandler)eq(null));
        
        GetProgramInformationRequestData requestMsg = (GetProgramInformationRequestData)
                msgCaptor.getAllValues().get(1);
        
        assertThat(requestMsg.getMissionNameCount(), is(1));
        assertThat(requestMsg.getMissionName(0), is("Mission 4"));
    }
    
    /**
     * Verify program removed event is properly handled.
     */
    @Test
    public void testRemoteEventProgramRemovedEvent()
    {
        //initialize m_SUT. Will post a push update
        assertThat(initCurrentMissionModels(123), is(3));
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.SHUTDOWN.toString());
        
        Event programRemoved = mockARemoteEvent(123, MissionProgramManager.TOPIC_PROGRAM_REMOVED, 
                "Mission 1", map);
        
        m_RemoteEventHelper.handleEvent(programRemoved);
        
        //verify there is one less mission
        List<CurrentMissionModel> models = m_SUT.getCurrentMissionsForControllerAsync(123);
        assertThat(models.size(), is(2));
        
        for (CurrentMissionModel model : models)
        {
            if (model.getMissionName().equals("Mission 1"))
            {
                fail("Mission 1 is expected to have been removed. It was not.");
            }
        }
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), eq("Mission Removed!"), 
                eq("The mission Mission 1 for system 0x0000007b has been removed with status SHUTDOWN."));
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        //verify push events are called 
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
         
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    /**
     * Verify mission event helper properly handles mission events.
     */
    @Test
    public void testEventHelperMissions()
    {
        //verify for some other message topic nothing is done
        Event event = mockAnEvent(123, MissionProgrammingMessageType.CancelProgramRequest.toString(), null);
        
        m_MissionEventHelper.handleEvent(event);
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
        
        //verify for right message a mission is added
        Event eventRightMsgType = mockAnEvent(123, 
                MissionProgrammingMessageType.GetProgramInformationResponse.toString(),
                mockGetProgramInfoResponse());
        
        m_MissionEventHelper.handleEvent(eventRightMsgType);
        
        //verify that since there is no record of controller 123 none of the missions are saved
        List<CurrentMissionModel> currentMissions = m_SUT.getCurrentMissionsForControllerAsync(123);
        
        assertThat(currentMissions.size(), is(0));
        
        //verify that now missions will be saved
        m_MissionEventHelper.handleEvent(eventRightMsgType);
        
        currentMissions = m_SUT.getCurrentMissionsForControllerAsync(123);
        assertThat(currentMissions.size(), is(3));
        
        for(CurrentMissionModel missionModel : currentMissions)
        {
            if (!missionModel.getMissionName().equals("Mission 1") 
                    && !missionModel.getMissionName().equals("Mission 2") 
                    && !missionModel.getMissionName().equals("Mission 3"))
            {
                fail("Received a mission back that was not expected. The mission name was " 
                        + missionModel.getMissionName());
            }
        }
        
        //verify push event is posted 
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        //first is posted after second call to handleEvent
        Event capturedEvent1 = eventCaptor.getAllValues().get(0);
        
        assertThat((String)capturedEvent1.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
        
        //this is posted on the second call 
        Event capturedEvent2 = eventCaptor.getAllValues().get(1);
        
        assertThat((String)capturedEvent2.getProperty(EventConstants.EVENT_TOPIC), 
                is(CurrentMissionMgr.TOPIC_MISSION_UPDATED));
    }
    
    /**
     * Verify controller events are properly handled.
     */
    @Test
    public void testEventHelperControllerEvent()
    {
        assertThat(initCurrentMissionModels(123), is(3));
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, 123);
        
        Event event = new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
        
        //this will remove from the controllers list
        m_ControllerEventHelper.handleEvent(event);
        
        //call to get controllers should now be an empty list
        List<CurrentMissionModel> expectedEmptyList = m_SUT.getCurrentMissionsForControllerAsync(123);
        
        assertThat(expectedEmptyList.size(), is(0));
    }
    
    /*
     * Initialize m_SUT with missions. Returns count of mission models added.
     */
    private int initCurrentMissionModels(int systemId)
    {
        //verify that since there is no record of controller 123 none of the missions are saved
        m_SUT.getCurrentMissionsForControllerAsync(systemId);
        
        //verify for right message a mission is added
        Event eventRightMsgType = mockAnEvent(systemId, 
                MissionProgrammingMessageType.GetProgramInformationResponse.toString(),
                mockGetProgramInfoResponse());
        
        m_MissionEventHelper.handleEvent(eventRightMsgType);
        
        return m_SUT.getCurrentMissionsForControllerAsync(systemId).size();
    }
    
    /**
     * Gets a mission model based on a program name and system id
     */
    private CurrentMissionModel findAMissionModel(int systemId, String program)
    {
        List<CurrentMissionModel> models = m_SUT.getCurrentMissionsForControllerAsync(systemId);
        
        CurrentMissionModel modelToTest = null;
        for (CurrentMissionModel model : models)
        {
            if (model.getMissionName().equals(program))
            {
                modelToTest = model;
                break;
            }
        }
        
        return modelToTest;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////
    // Helper methods which create events.                                                      //
    //////////////////////////////////////////////////////////////////////////////////////////////
    
    /*
     * Mock a TH message event.
     */
    private Event mockAnEvent(int systemId, String messageType, Object dataMessage)
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, messageType);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, dataMessage);
        
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
    
    /*
     * Mock a remote event.
     */
    private Event mockARemoteEvent(int systemId, String topic, String programName, Map<String, Object> props)
    {
        Map<String, Object> finalProps = new HashMap<String, Object>();
        
        finalProps.put(Program.EVENT_PROP_PROGRAM_NAME, programName);
        finalProps.put(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID, systemId);
        
        for (String key : props.keySet())
        {
            finalProps.put(key, props.get(key));
        }
        
        return new Event(topic + RemoteConstants.REMOTE_TOPIC_SUFFIX, finalProps);
    }
    
    /**
     * Mock a GetProgramInformationResponseData message.
     */
    private GetProgramInformationResponseData mockGetProgramInfoResponse()
    {
        GetProgramInformationResponseData message = GetProgramInformationResponseData.newBuilder().
                addAllMissionInfo(mockProgramInfo()).build();
        
        return message;
    }
    
    /*
     * Mock ProgramInfo objects.
     */
    private List<ProgramInfo> mockProgramInfo()
    {
        List<ProgramInfo> programs = new ArrayList<ProgramInfo>();
        
        MissionProgramSchedule sched1 = MissionProgramSchedule.newBuilder().
                setImmediately(true).setIndefiniteInterval(true).build();
        
        MissionProgramSchedule sched2 = MissionProgramSchedule.newBuilder().
                setImmediately(false).setStartInterval(12).setIndefiniteInterval(true).build();
        
        MissionProgramSchedule sched3 = MissionProgramSchedule.newBuilder().
                setImmediately(true).setIndefiniteInterval(false).setStopInterval(99).build();
        
        ProgramInfo info1 = ProgramInfo.newBuilder().setMissionName("Mission 1").
                setMissionStatus(MissionStatus.WAITING_INITIALIZED).setMissionSchedule(sched1).
                setTemplateName("Template 1").build();
        
        ProgramInfo info2 = ProgramInfo.newBuilder().setMissionName("Mission 2").
                setMissionStatus(MissionStatus.EXECUTED).setMissionSchedule(sched2).
                setTemplateName("Template 2").build();
        
        ProgramInfo info3 = ProgramInfo.newBuilder().setMissionName("Mission 3").
                setMissionStatus(MissionStatus.SHUTDOWN).setMissionSchedule(sched3).
                setTemplateName("Template 3").build();
        
        programs.add(info1);
        programs.add(info2);
        programs.add(info3);
        
        return programs;
    }
}
