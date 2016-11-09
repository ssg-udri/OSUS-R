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
package mil.dod.th.ose.remote.mp;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.MissionProgramMessages;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.CancelProgramResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteShutdownResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ExecuteTestResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetExecutionParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetExecutionParametersResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetLastTestResultsRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetLastTestResultsResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramInformationResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramNamesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramStatusRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetProgramStatusResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplateNamesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ManagedExecutorsShutdownRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionStatus;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionTestResult;
import mil.dod.th.core.remote.proto.MissionProgramMessages.ProgramInfo;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveMissionProgramResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.RemoveTemplateRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.MapEntry;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * Tests the functionality of the {@link MissionProgrammingMessageService}.
 * 
 * @author callen
 *
 */
public class TestMissionProgrammingMessageService 
{
    private MissionProgrammingMessageService m_SUT;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private LoggingService m_Logger;
    private JaxbProtoObjectConverter m_Converter;
    private MissionProgramManager m_Manager;
    private TemplateProgramManager m_TemplateManager;    
    private MessageRouterInternal m_MessageRouter;
    private ManagedExecutors m_ManagedExecutors;
    private EventHandlerHelper m_EventHandlerHelper;
    private MessageResponseWrapper m_ResponseWrapper;

    @Before
    public void setUp() throws IllegalArgumentException, PersistenceFailedException //load parameters mocking behavior
    {                                                                                //could throw these exceptions
        m_SUT = new MissionProgrammingMessageService();
        m_Manager = mock(MissionProgramManager.class);
        m_SUT.setMissionProgramManager(m_Manager);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_ManagedExecutors = mock(ManagedExecutors.class);
        m_EventHandlerHelper = mock(EventHandlerHelper.class);
        
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_MessageFactory = mock(MessageFactory.class);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_Logger = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logger);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setManagedExecutorService(m_ManagedExecutors);
        m_SUT.setEventHelperService(m_EventHandlerHelper);
        
        //mock mission program
        m_TemplateManager = mock(TemplateProgramManager.class);
        when(m_Manager.loadParameters(Mockito.any(MissionProgramParameters.class))).thenReturn(mock(Program.class));
        m_SUT.setTemplateProgramManager(m_TemplateManager);
        
        when(m_MessageFactory.createMissionProgrammingResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(MissionProgrammingMessageType.class), Mockito.any(Message.class))).
                    thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        m_SUT.activate();
        
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }
    
    /**
     * Set of templates, used for the get templates test.
     */
    private Set<MissionProgramTemplate> getTemplatesSet()
    {
        //mock templates
        Set<MissionProgramTemplate> templates = new HashSet<MissionProgramTemplate>();
        MissionProgramTemplate template = mock(MissionProgramTemplate.class);
        templates.add(template);
        MissionVariableMetaData variableData = mock(MissionVariableMetaData.class);
        List<MissionVariableMetaData> variableList = new ArrayList<>();
        variableList.add(variableData);
    
        when(template.getName()).thenReturn("Name 1");
        when(template.getDescription()).thenReturn("Something");
        when(template.getVariableMetaData()).thenReturn(variableList);
        when(variableData.getName()).thenReturn("Awesome Variable!");
        when(variableData.getDefaultValue()).thenReturn("Default");
        
        MissionProgramTemplate template2 = mock(MissionProgramTemplate.class);
        templates.add(template2);
        MissionVariableMetaData variableData2 = mock(MissionVariableMetaData.class);
        List<MissionVariableMetaData> variableList2 = new ArrayList<MissionVariableMetaData>();
        variableList2.add(variableData2);
        
        when(template2.getName()).thenReturn("Name 2");
        when(template2.getDescription()).thenReturn("Something else");
        when(template2.getVariableMetaData()).thenReturn(variableList2);
        when(variableData2.getName()).thenReturn("A variable");
        when(variableData2.getDefaultValue()).thenReturn("Default2");
        
        return templates;
    }
    
    /**
     * Test the activation of the remote mission programming message service. 
     */
    @Test 
    public void testActivate()
    {
        //activate the mission programming message service
        m_SUT.activate();
    }
    
    /**
     * Verify the namespace is {@link MissionProgrammingNamespace}.
     */
    @Test
    public void testGetNameSpace()
    {
        //activate the mission programming message service
        m_SUT.activate();
        assertThat(m_SUT.getNamespace(), is(Namespace.MissionProgramming));
    }
    
    /**
     * Tests the message handling and event service response upon receiving a message.
     *   
     */
    @Test
    public void testMessageHandling() throws IllegalArgumentException, IOException, ObjectConverterException, 
        PersistenceFailedException
    {
        //activate the mission programming message service
        m_SUT.activate();
        //create a simple mission template to send
        MissionProgramTemplateGen.MissionProgramTemplate template = MissionProgramTemplateGen.MissionProgramTemplate.
            newBuilder().setName("IamATest").setSource("TheSource!@@#(*&$^%_@##$").build();
        //construct a message        
        LoadTemplateRequestData programMessage = MissionProgramMessages.LoadTemplateRequestData.newBuilder().
            setMission(template).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.LoadTemplateRequest).setData(programMessage.toByteString()).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
    
        //mocked converter behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).
            thenReturn(mock(MissionProgramTemplate.class));
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        //verify mission program manager is interacted with
        verify(m_TemplateManager, times(1)).loadMissionTemplate(Mockito.any(MissionProgramTemplate.class));
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(missionMessage.getType().toString()));
        assertThat((MissionProgrammingNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(missionMessage));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify that the loaded template response is sent after the load program message is processed.
     */
    @Test
    public void testLoadTemplate() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //activate the mission programming message service
        m_SUT.activate();
        //create a simple mission template to send
        MissionProgramTemplateGen.MissionProgramTemplate template = MissionProgramTemplateGen.MissionProgramTemplate.
            newBuilder().setName("IamATest").setSource("TheSource!@@#(*&$^%_@##$").build();
        //construct a message        
        LoadTemplateRequestData programMessage = MissionProgramMessages.LoadTemplateRequestData.newBuilder().
            setMission(template).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.LoadTemplateRequest).setData(programMessage.toByteString()).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        //mock converter behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).
            thenReturn(mock(MissionProgramTemplate.class));

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((LoadTemplateRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(programMessage));

        //verify the interaction with the template manager
        verify(m_TemplateManager, times(1)).loadMissionTemplate(Mockito.any(MissionProgramTemplate.class));
           
        // verify program loaded response is sent back from source requester
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(message,
                MissionProgrammingMessageType.LoadTemplateResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the load template response message sends an event with null data message.
     */
    @Test
    public void testLoadTemplateResponse() throws IllegalArgumentException, IOException
    {
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.LoadTemplateResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the null data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(nullValue()));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.LoadTemplateResponse.toString()));
    }
    
    /**
     * Test that the load parameters call works and that a parameters loaded response is sent.
     */
    @Test
    public void testLoadParameters() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //activate the mission programming message service
        m_SUT.activate();
        //create a simple mission parameter object to send to the template program manager
        MissionProgramParametersGen.MissionProgramSchedule schedule = 
                MissionProgramParametersGen.MissionProgramSchedule.
            newBuilder().setActive(true).setImmediately(false).setIndefiniteInterval(true).build();
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramParametersGen.MissionProgramParameters.newBuilder().setTemplateName("Sue").
                setSchedule(schedule).setProgramName("name").build();
        //construct a message        
        LoadParametersRequestData programMessage = MissionProgramMessages.LoadParametersRequestData.newBuilder().
            setParameters(params).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.LoadParametersRequest).setData(programMessage.toByteString()).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        //mock converter behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).
            thenReturn(mock(MissionProgramParameters.class));

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((LoadParametersRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(programMessage));

        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).loadParameters(Mockito.any(MissionProgramParameters.class));
           
        // verify parameters loaded message is sent back from source requester
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.LoadParametersResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the load parameters response message sends an event with null data message.
     */
    @Test
    public void testLoadParametersResponse() throws IllegalArgumentException, IOException
    {
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.LoadParametersResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the null data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(nullValue()));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.LoadParametersResponse.toString()));
    }
    
    /**
     * Test that the load parameters exception that the object could not be converted.
     * 
     * Verify PERSIST_ERROR is part of the response message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLoadParametersException() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //activate the mission programming message service
        m_SUT.activate();
        //create a simple mission parameter object to send to the template program manager
        MissionProgramParametersGen.MissionProgramSchedule schedule = 
                MissionProgramParametersGen.MissionProgramSchedule.
                    newBuilder().setActive(true).setImmediately(false)
                    .setIndefiniteInterval(true).build();
        MissionProgramParametersGen.MissionProgramParameters params = 
            MissionProgramParametersGen.MissionProgramParameters.newBuilder().setTemplateName("Sue").
                setSchedule(schedule).setProgramName("names").build();
        //construct a message        
        LoadParametersRequestData programMessage = MissionProgramMessages.LoadParametersRequestData.newBuilder().
            setParameters(params).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.LoadParametersRequest).setData(programMessage.toByteString()).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        //lay-out mocked behavior
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).
            thenReturn(mock(MissionProgramParameters.class));
        when(m_Manager.loadParameters(Mockito.any(MissionProgramParameters.class))).
            thenThrow(PersistenceFailedException.class);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify loaded parameters response sent back, due to converter exception
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.PERSIST_ERROR, 
                "Failed to persist parameters from message. " + null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test load templates with converter exception.
     * 
     * Verify CONVERTER_ERROR is part of the response message.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testLoadTemplatesExceptions() throws IOException, ObjectConverterException
    {
        //activate the mission programming message service
        m_SUT.activate();
        //create a simple mission template to send
        MissionProgramTemplateGen.MissionProgramTemplate template = MissionProgramTemplateGen.MissionProgramTemplate.
            newBuilder().setName("IamATest").setSource("TheSource!@@#(*&$^%_@##$").build();
        //construct a message        
        LoadTemplateRequestData programMessage = MissionProgramMessages.LoadTemplateRequestData.newBuilder().
            setMission(template).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.LoadTemplateRequest).setData(programMessage.toByteString()).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenThrow(ObjectConverterException.class);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify loaded program response NEVER sent back 
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Failed to create template object from message. " + null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test the removal of a template request.
     */
    @Test
    public void testRemoveTemplate() throws IllegalArgumentException, IOException
    {
        //activate the message service
        m_SUT.activate();
        //remove template request
        RemoveTemplateRequestData removeMsg = MissionProgramMessages.RemoveTemplateRequestData.newBuilder().
            setNameOfTemplate("Mom").build();
        //construct mission namespace and terra harvest message
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.RemoveTemplateRequest).setData(removeMsg.toByteString()).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((RemoveTemplateRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(removeMsg));
        
        verify(m_TemplateManager, times(1)).removeTemplate(anyString());
        
        // verify parameters loaded message is sent back from source requester
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.RemoveTemplateResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the remove template response message sends an event with null data message.
     */
    @Test
    public void testRemoveTemplateResponse() throws IllegalArgumentException, IOException
    {
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.RemoveTemplateResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the null data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(nullValue()));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(MissionProgrammingMessageType.RemoveTemplateResponse.toString()));
    }
    
    /**
     * Test request to get templates.
     * 
     * Verify that two templates are asked to be converted.
     * 
     * Verify that the response message is sent.
     */
    @Test
    public void testSuccessGetTemplates() throws IllegalArgumentException, IOException, ObjectConverterException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //construct the request for templates
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.GetTemplatesRequest).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        Set<MissionProgramTemplate> templates = getTemplatesSet();
        when(m_TemplateManager.getTemplates()).thenReturn(templates);
        
        //mock converter behavior
        MissionProgramTemplateGen.MissionProgramTemplate protoTemplate = MissionProgramTemplateGen.
            MissionProgramTemplate.newBuilder().setName("Name").setSource("Source").build();
        when(m_Converter.convertToProto(Mockito.any(MissionProgramTemplate.class))).
            thenReturn(protoTemplate);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        //verify data message is null as there is not a data message for this request
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(nullValue()));

        //verify the interaction with the template manager
        verify(m_TemplateManager).getTemplates();
        //verfiy that the two templates in the mocked set are passed to the converter
        ArgumentCaptor<MissionProgramTemplate> templateCaptor = ArgumentCaptor.forClass(MissionProgramTemplate.class);
        verify(m_Converter, times(2)).convertToProto(templateCaptor.capture());
           
        // verify get templates response is sent
        ArgumentCaptor<GetTemplatesResponseData> messageCaptor = 
            ArgumentCaptor.forClass(GetTemplatesResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetTemplatesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetTemplatesResponseData response = messageCaptor.getValue();
        //verify that two templates were made. Behavior is mocked..
        assertThat(response.getTemplateCount(), is(2));
        assertThat(response.getTemplateList(), hasItem(protoTemplate));
        assertThat(response.getTemplate(0).getName(), is("Name"));
        assertThat(response.getTemplate(0).getSource(), is("Source"));
    }
    
    /**
     * Test request to get templates with object converter error.
     * 
     * Verify that three templates are asked to be converted.
     * 
     * Verify that only two are in the response message.
     */
    @SuppressWarnings("unchecked") //mock exception for the proto converter
    @Test
    public void testInvalidGetTemplates() throws IllegalArgumentException, IOException, ObjectConverterException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //construct the request for templates
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
            setType(MissionProgrammingMessageType.GetTemplatesRequest).build();
        TerraHarvestPayload payload = createPayload(missionMessage);
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        //mock the template manager response to get templates
        Set<MissionProgramTemplate> templates = getTemplatesSet();
        
        //add mock template to throw error
        MissionProgramTemplate templateBad = mock(MissionProgramTemplate.class);
        templates.add(templateBad);

        when(templateBad.getName()).thenReturn("NameBad");
        when(m_TemplateManager.getTemplates()).thenReturn(templates);
        //mock converter behavior
        MissionProgramTemplateGen.MissionProgramTemplate protoTemplate = MissionProgramTemplateGen.
            MissionProgramTemplate.newBuilder().setName("Name").setSource("Source").build();
        when(m_Converter.convertToProto(Mockito.any(MissionProgramTemplate.class))).
            thenReturn(protoTemplate);
        when(m_Converter.convertToProto(templateBad)).thenThrow(ObjectConverterException.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        //verify data message is null as there is not a data message for this request
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(nullValue()));

        //verify the interaction with the template manager
        verify(m_TemplateManager).getTemplates();
        //verfiy that the three templates in the mocked set are passed to the converter
        verify(m_Converter, times(3)).convertToProto(Mockito.any(MissionProgramTemplate.class));
           
        // verify get templates response is sent back
        ArgumentCaptor<GetTemplatesResponseData> messageCaptor = 
            ArgumentCaptor.forClass(GetTemplatesResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetTemplatesResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetTemplatesResponseData response = messageCaptor.getValue();
         //verify that the two valid templates were made. Behavior is mocked..
        assertThat(response.getTemplateCount(), is(2));
        assertThat(response.getTemplateList(), hasItem(protoTemplate));
        assertThat(response.getTemplate(0).getName(), is("Name"));
        assertThat(response.getTemplate(0).getSource(), is("Source"));       
    }
    
    /**
     * Test an event from a response to a getting templates.
     */
    @Test
    public void testGetTemplatesResponse() throws IllegalArgumentException, IOException
    {
        //response message
        MissionProgramTemplateGen.MissionProgramTemplate protoTemplate1 = MissionProgramTemplateGen.
                MissionProgramTemplate.newBuilder().setName("Name1").setSource("Source").build();
        MissionProgramTemplateGen.MissionProgramTemplate protoTemplate2 = MissionProgramTemplateGen.
                MissionProgramTemplate.newBuilder().setName("Name2").setSource("Source").build();
        GetTemplatesResponseData response = GetTemplatesResponseData.newBuilder().
                addTemplate(protoTemplate2).addTemplate(protoTemplate1).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetTemplatesResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetTemplatesResponse.toString()));
        assertThat((GetTemplatesResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }

    /**
     * Test that execute test mission will call to mission program manager to execute the test.
     */
    @Test
    public void testExecuteTest() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteTestRequestData request = ExecuteTestRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteTestRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ExecuteTestRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).getProgram(name);
        
        ArgumentCaptor<ExecuteTestResponseData> responseCaptor = ArgumentCaptor.forClass(ExecuteTestResponseData.class);
        // verify parameters loaded message is sent back from source requester
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.ExecuteTestResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        //verify data is what is expected
        assertThat(responseCaptor.getValue().getMissionName(), is(name));
    }

    /**
     * Test that execute test mission will call to mission program manager to execute the test.
     */
    @Test
    public void testExecuteTestFailed() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteTestRequestData request = ExecuteTestRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteTestRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ExecuteTestRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).getProgram(name);

        ArgumentCaptor<ExecuteTestResponseData> responseCaptor = ArgumentCaptor.forClass(ExecuteTestResponseData.class);
        // verify parameters loaded message is sent back from source requester
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.ExecuteTestResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        //verify data is what is expected
        assertThat(responseCaptor.getValue().getMissionName(), is(name));
    }
    
    /**
     * Test that requesting to test execute a mission which causes a mission program execution exception that an
     * illegal state exception is thrown.
     */
    @Test
    public void testTestExecuteMPException() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);
        when(program.executeTest()).thenThrow(new MissionProgramException(new Exception("asdf")));

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteTestRequestData request = ExecuteTestRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteTestRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        try
        {
            m_SUT.handleMessage(message, createPayload(missionMessage), channel);
            fail("exception expected");
        }
        catch (IllegalStateException e)
        {
            //expected
        }
    }

    /**
     * Test that requesting to test execute a mission that is not in a legal state.
     */
    @Test
    public void testTestExecuteIllegalState() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);
        when(program.executeTest()).thenThrow(new IllegalStateException("Utah"));

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteTestRequestData request = ExecuteTestRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteTestRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).getProgram(name);
        
        // verify response
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INTERNAL_ERROR, 
                "Utah");
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test that requesting to test execute a mission that does not support a test execution sends an error response.
     */
    @Test
    public void testTestExecuteExceptionUnsupported() throws IOException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);
        when(program.executeTest()).thenThrow(new UnsupportedOperationException("Ohio"));

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteTestRequestData request = ExecuteTestRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteTestRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).getProgram(name);
        
        // verify response
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INTERNAL_ERROR, 
                "Mission program [wicked] was requested to execute its test function, "
                + "but it does not support this ability.");
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test an event from a response to a test execution.
     */
    @Test
    public void testTestExecutionResponse() throws IllegalArgumentException, IOException
    {
        //response message
        ExecuteTestResponseData response = ExecuteTestResponseData.newBuilder().
                setMissionName("pop").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteTestResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.ExecuteTestResponse.toString()));
        assertThat((ExecuteTestResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test that requesting to execute a mission will send a response when an event is posted for the requested program,
     * representing that the program completed execution.
     */
    @Test
    public void testExecute() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteRequestData request = ExecuteRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ExecuteRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).getProgram(name);
        
        ArgumentCaptor<ExecuteResponseData> responseCaptor = ArgumentCaptor.forClass(ExecuteResponseData.class);
        // verify parameters loaded message is sent back from source requester
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.ExecuteResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        //verify data is what is expected
        assertThat(responseCaptor.getValue().getMissionName(), is(name));
    }
    
    /**
     * Test that requesting to execute a program and the execution throws a Mission Program Exception
     * that a new illegal state exception is thrown.
     */
    @Test
    public void testExecuteMPException() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);
        doThrow(new MissionProgramException(new Exception("red"))).when(program).execute();

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteRequestData request = ExecuteRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        try
        {
            m_SUT.handleMessage(message, createPayload(missionMessage), channel);
            fail("Expecting exception");
        }
        catch(IllegalStateException e)
        {
            //expected
        }
    }
    
    /**
     * Test that requesting to execute a program that is in an illegal state.
     */
    @Test
    public void testExecuteIllegalState() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        String name = "wicked";
        //mocks
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn(name);
        when(m_Manager.getProgram(name)).thenReturn(program);
        doThrow(new IllegalStateException("Antartica")).when(program).execute();

        //activate the mission programming message service
        m_SUT.activate();
        
        //create request
        ExecuteRequestData request = ExecuteRequestData.newBuilder().setMissionName(name).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ExecuteRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify that mission program manager is given parameters
        verify(m_Manager, times(1)).getProgram(name);
        
        // verify response
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INTERNAL_ERROR, 
                "Antartica");
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test an event from a response to a execution of a program.
     */
    @Test
    public void testExecutionResponse() throws IllegalArgumentException, IOException
    {
        //response message
        ExecuteResponseData response = ExecuteResponseData.newBuilder().
                setMissionName("pop").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.ExecuteResponse.toString()));
        assertThat((ExecuteResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test that requesting to get program names that all expected names are returned.
     */
    @Test
    public void testGetProgramNames() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //mock mission program manager behavior
        final Set<Program> programs = new HashSet<Program>();
        Program programA = mock(Program.class);
        when(programA.getProgramName()).thenReturn("a");
        programs.add(programA);
        Program programB = mock(Program.class);
        when(programB.getProgramName()).thenReturn("b");
        programs.add(programB);
        when(m_Manager.getPrograms()).thenReturn(programs);
        //create request
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramNamesRequest).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetProgramNamesRequest.toString()));
        
        //verify the contents of the response
        ArgumentCaptor<GetProgramNamesResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetProgramNamesResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetProgramNamesResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(responseCaptor.getValue().getMissionNameCount(), is(2));
        assertThat(responseCaptor.getValue().getMissionNameList(), hasItems("a", "b"));
    }
    
    /**
     * Test an event from a response to a get program names request.
     */
    @Test
    public void testGetProgramNamesResponse() throws IllegalArgumentException, IOException
    {
        //response message
        GetProgramNamesResponseData response = GetProgramNamesResponseData.newBuilder().
                addMissionName("squirrel").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramNamesResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetProgramNamesResponse.toString()));
        assertThat((GetProgramNamesResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test the request to get template names.
     * Verify that all expected names are returned.
     */
    @Test
    public void testGetTemplateNames() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //mock template manager
        final Set<String> names = new HashSet<String>();
        names.add("a");
        names.add("b");
        when(m_TemplateManager.getMissionTemplateNames()).thenReturn(names);
        
        //create request
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetTemplateNamesRequest).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetTemplateNamesRequest.toString()));
        
        //verify the contents of the response
        ArgumentCaptor<GetTemplateNamesResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetTemplateNamesResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetTemplateNamesResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        assertThat(responseCaptor.getValue().getTemplateNameCount(), is(2));
        assertThat(responseCaptor.getValue().getTemplateNameList(), hasItems("a", "b"));
    }
    
    /**
     * Test an event from a response to getting template names.
     */
    @Test
    public void testGetTemplateNamesResponse() throws IllegalArgumentException, IOException
    {
        //response message
        GetTemplateNamesResponseData response = GetTemplateNamesResponseData.newBuilder().
                addTemplateName("pop").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetTemplateNamesResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetTemplateNamesResponse.toString()));
        assertThat((GetTemplateNamesResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test requesting to cancel a program.
     * Verify correct program is canceled.
     */
    @Test
    public void testCancelPrograms() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //mock mission program manager behavior
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn("a");
        when(program.getProgramStatus()).thenReturn(ProgramStatus.CANCELED);
        when(m_Manager.getProgram("a")).thenReturn(program);
        
        //create request
        CancelProgramRequestData data = CancelProgramRequestData.newBuilder().
                setMissionName("a").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.CancelProgramRequest).
                setData(data.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.CancelProgramRequest.toString()));
        
        //verify response
        ArgumentCaptor<CancelProgramResponseData> messageCaptor = 
                ArgumentCaptor.forClass(CancelProgramResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.CancelProgramResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        CancelProgramResponseData responseData = messageCaptor.getValue();
        assertThat(responseData.getMissionName(), is("a"));
        assertThat(responseData.getMissionStatus(), is(MissionStatus.CANCELED));
        
    }
    
    /**
     * Test an event from a response to canceling a program.
     */
    @Test
    public void testCancelProgramResponse() throws IllegalArgumentException, IOException
    {
        //response message
        CancelProgramResponseData response = CancelProgramResponseData.newBuilder().
                setMissionName("pop").setMissionStatus(MissionStatus.CANCELED).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.CancelProgramResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.CancelProgramResponse.toString()));
        assertThat((CancelProgramResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }

    /**
     * Test that requesting to get program information when there are no specific program's identified.
     * Verify all programs are returned.
     */
    @Test
    public void testGetProgramInfoEmptyList() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //schdeules
        MissionProgramSchedule schedule = mock(MissionProgramSchedule.class);
        MissionProgramParametersGen.MissionProgramSchedule protoSched = 
                MissionProgramParametersGen.MissionProgramSchedule.
                newBuilder().setActive(true).setAtReset(false).setImmediately(true).setIndefiniteInterval(true).build();
        
        //execution params
        Map<String, Object> paramsA = new HashMap<String, Object>();
        paramsA.put("asset", "asset");
        paramsA.put("physicalLink", "phys");
        Map<String, Object> paramsB = new HashMap<String, Object>();
        paramsB.put("linklayer", "link");
        paramsB.put("physicalLink", "phys");

        //mock mission program manager behavior
        Program programA = mock(Program.class);
        when(programA.getProgramName()).thenReturn("a");
        when(programA.getTemplateName()).thenReturn("a template Name");
        when(programA.getProgramStatus()).thenReturn(ProgramStatus.EXECUTED);
        when(programA.getMissionSchedule()).thenReturn(schedule);
        when(programA.getExecutionParameters()).thenReturn(paramsA);
        Program programB = mock(Program.class);
        when(programB.getProgramName()).thenReturn("b");
        when(programB.getProgramStatus()).thenReturn(ProgramStatus.SHUTDOWN);
        when(programB.getMissionSchedule()).thenReturn(schedule);
        when(programB.getTemplateName()).thenReturn("another template name");
        when(programB.getExecutionParameters()).thenReturn(paramsB);
        
        Set<Program> programs = new HashSet<Program>();
        programs.add(programA);
        programs.add(programB);
        
        //behaviors
        when(m_Manager.getPrograms()).thenReturn(programs);
        when(m_Converter.convertToProto(schedule)).thenReturn(protoSched);
        
        //create request
        GetProgramInformationRequestData data = GetProgramInformationRequestData.newBuilder().build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramInformationRequest).
                setData(data.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetProgramInformationRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(data));
        
        //verify response
        ArgumentCaptor<GetProgramInformationResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetProgramInformationResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetProgramInformationResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //inspect the message
        GetProgramInformationResponseData responseMessage = responseCaptor.getValue();
        assertThat(responseMessage.getMissionInfoCount(), is(2));
        
        for (ProgramInfo programInfo : responseMessage.getMissionInfoList())
        {
            if (programInfo.getMissionName().equals("a"))
            {
                assertThat(programInfo.getTemplateName(), is("a template Name"));
                assertThat(programInfo.getMissionSchedule(), is(protoSched));
                assertThat(programInfo.getMissionStatus(), is(MissionStatus.EXECUTED));
                assertThat(programInfo.getExecutionParamCount(), is(2));
                assertThat(programInfo.getExecutionParamList().get(0).getKey(), isOneOf("asset", "physicalLink"));
                assertThat(programInfo.getExecutionParamList().get(0).getValue().getStringValue(), 
                        isOneOf("asset", "phys"));
            }
            else
            {
                assertThat(programInfo.getMissionName(), is("b"));
                assertThat(programInfo.getTemplateName(), is("another template name"));
                assertThat(programInfo.getMissionSchedule(), is(protoSched));
                assertThat(programInfo.getMissionStatus(), is(MissionStatus.SHUTDOWN));
                assertThat(programInfo.getExecutionParamCount(), is(2));
                assertThat(programInfo.getExecutionParamList().get(0).getKey(), isOneOf("linklayer", "physicalLink"));
                assertThat(programInfo.getExecutionParamList().get(0).getValue().getStringValue(), 
                        isOneOf("link", "phys"));
            }
        }
    }
    
    /**
     * Test an event from a response from a request to get program information.
     */
    @Test
    public void testGetProgramInfoResponse() throws IllegalArgumentException, IOException
    {
        //response message
        MissionProgramParametersGen.MissionProgramSchedule protoSched = 
                MissionProgramParametersGen.MissionProgramSchedule.
                newBuilder().setActive(true).setAtReset(false).setImmediately(true).setIndefiniteInterval(true).build();
        ProgramInfo info = ProgramInfo.newBuilder().
                setMissionName("l").
                setMissionStatus(MissionStatus.EXECUTING).
                setTemplateName("m").setMissionSchedule(protoSched).
                addExecutionParam(MapEntry.newBuilder().
                        setKey("key").
                        setValue(Multitype.newBuilder().
                                setType(Type.STRING).
                                setStringValue("Golden").build()).build()).build();
        GetProgramInformationResponseData response = GetProgramInformationResponseData.newBuilder().
                addMissionInfo(info).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramInformationResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetProgramInformationResponse.toString()));
        assertThat((GetProgramInformationResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test that requesting to get program information when only a specific program is wanted.
     * Verify that programs is returned.
     */
    @Test
    public void testGetProgramInfoList() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //schedules
        MissionProgramSchedule schedule = mock(MissionProgramSchedule.class);
        MissionProgramParametersGen.MissionProgramSchedule protoSched = 
                MissionProgramParametersGen.MissionProgramSchedule.
                newBuilder().setActive(true).setAtReset(false).setImmediately(true).setIndefiniteInterval(true).build();
        
        //execution params
        Map<String, Object> paramsA = new HashMap<String, Object>();
        paramsA.put("asset", "asset");
        paramsA.put("physicalLink", "phys");
        Map<String, Object> paramsB = new HashMap<String, Object>();
        paramsB.put("linklayer", "link");
        paramsB.put("physicalLink", "phys");

        //mock mission program manager behavior
        Program programA = mock(Program.class);
        when(programA.getProgramName()).thenReturn("a");
        when(programA.getTemplateName()).thenReturn("a template Name");
        when(programA.getProgramStatus()).thenReturn(ProgramStatus.EXECUTED);
        when(programA.getMissionSchedule()).thenReturn(schedule);
        when(programA.getExecutionParameters()).thenReturn(paramsA);
        Program programB = mock(Program.class);
        when(programB.getProgramName()).thenReturn("b");
        when(programB.getProgramStatus()).thenReturn(ProgramStatus.SHUTDOWN);
        when(programB.getMissionSchedule()).thenReturn(schedule);
        when(programB.getTemplateName()).thenReturn("another template name");
        when(programB.getExecutionParameters()).thenReturn(paramsB);
        
        //behaviors
        when(m_Manager.getProgram("a")).thenReturn(programA);
        when(m_Converter.convertToProto(schedule)).thenReturn(protoSched);
        
        //create request
        GetProgramInformationRequestData data = GetProgramInformationRequestData.newBuilder().
                addMissionName("a").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramInformationRequest).
                setData(data.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetProgramInformationRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(data));
        
        //verify response
        ArgumentCaptor<GetProgramInformationResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetProgramInformationResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetProgramInformationResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //inspect the message
        GetProgramInformationResponseData responseMessage = responseCaptor.getValue();
        assertThat(responseMessage.getMissionInfoCount(), is(1));
        
        //expected values
        ProgramInfo programInfo = responseMessage.getMissionInfo(0);
        assertThat(programInfo.getMissionName(), is("a"));
        assertThat(programInfo.getTemplateName(), is("a template Name"));
        assertThat(programInfo.getMissionSchedule(), is(protoSched));
        assertThat(programInfo.getMissionStatus(), is(MissionStatus.EXECUTED));
        assertThat(programInfo.getExecutionParamCount(), is(2));
        assertThat(programInfo.getExecutionParamList().get(0).getKey(), isOneOf("asset", "physicalLink"));
        assertThat(programInfo.getExecutionParamList().get(0).getValue().getStringValue(), 
                isOneOf("asset", "phys"));
    }
    
    /**
     * Test that requesting to get program information when there are no specific program's identified, but the
     * schedule for a program cannot be converted.
     * Verify all programs are returned.
     */
    @Test
    public void testGetProgramInfoEmptyListException() throws IOException, ObjectConverterException, 
        IllegalArgumentException, PersistenceFailedException, InterruptedException, IllegalStateException, 
        UnsupportedOperationException, MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //schedules
        MissionProgramSchedule schedule = mock(MissionProgramSchedule.class);
        MissionProgramParametersGen.MissionProgramSchedule protoSched = 
                MissionProgramParametersGen.MissionProgramSchedule.
                newBuilder().setActive(true).setAtReset(false).setImmediately(true).setIndefiniteInterval(true).build();
        MissionProgramSchedule scheduleBad = mock(MissionProgramSchedule.class);
        
        //execution params
        Map<String, Object> paramsA = new HashMap<String, Object>();
        paramsA.put("asset", "asset");
        paramsA.put("physicalLink", "phys");
        Map<String, Object> paramsB = new HashMap<String, Object>();
        paramsB.put("linklayer", "link");
        paramsB.put("physicalLink", "phys");

        //mock mission program manager behavior
        Program programA = mock(Program.class);
        when(programA.getProgramName()).thenReturn("a");
        when(programA.getTemplateName()).thenReturn("a template Name");
        when(programA.getProgramStatus()).thenReturn(ProgramStatus.EXECUTED);
        when(programA.getMissionSchedule()).thenReturn(schedule);
        when(programA.getExecutionParameters()).thenReturn(paramsA);
        Program programB = mock(Program.class);
        when(programB.getProgramName()).thenReturn("b");
        when(programB.getProgramStatus()).thenReturn(ProgramStatus.SHUTDOWN);
        when(programB.getMissionSchedule()).thenReturn(scheduleBad);
        when(programB.getTemplateName()).thenReturn("another template name");
        when(programB.getExecutionParameters()).thenReturn(paramsB);
        
        Set<Program> programs = new HashSet<Program>();
        programs.add(programA);
        programs.add(programB);
        
        //behaviors
        when(m_Manager.getPrograms()).thenReturn(programs);
        when(m_Converter.convertToProto(schedule)).thenReturn(protoSched);
        when(m_Converter.convertToProto(scheduleBad)).thenThrow(new ObjectConverterException("Can't Convert"));
        
        //create request
        GetProgramInformationRequestData data = GetProgramInformationRequestData.newBuilder().build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramInformationRequest).
                setData(data.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        //verify response
        ArgumentCaptor<GetProgramInformationResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetProgramInformationResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetProgramInformationResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        //inspect the message
        GetProgramInformationResponseData responseMessage = responseCaptor.getValue();
        assertThat(responseMessage.getMissionInfoCount(), is(1));
        
        //expected values, for only one mission because the other one failed to convert
        ProgramInfo programInfo = responseMessage.getMissionInfo(0);
        assertThat(programInfo.getMissionName(), is("a"));
        assertThat(programInfo.getTemplateName(), is("a template Name"));
        assertThat(programInfo.getMissionSchedule(), is(protoSched));
        assertThat(programInfo.getMissionStatus(), is(MissionStatus.EXECUTED));
        assertThat(programInfo.getExecutionParamCount(), is(2));
        assertThat(programInfo.getExecutionParamList().get(0).getKey(), isOneOf("asset", "physicalLink"));
        assertThat(programInfo.getExecutionParamList().get(0).getValue().getStringValue(), 
                isOneOf("asset", "phys"));
    }
    
    /**
     * Test requesting the managed executor service to shutdown immediately.
     * Verify response and the interaction with that service.
     */
    @Test
    public void testShutdownExecutorsNow() throws IllegalArgumentException, IOException
    {
        //request
        ManagedExecutorsShutdownRequestData request = ManagedExecutorsShutdownRequestData.newBuilder().
                setShutdownNow(true).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ManagedExecutorsShutdownRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        //verify interaction
        verify(m_ManagedExecutors).shutdownAllExecutorServicesNow();
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ManagedExecutorsShutdownRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify response
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.ManagedExecutorsShutdownResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test requesting the managed executor service to shutdown.
     * Verify response and the interaction with that service.
     */
    @Test
    public void testShutdownExecutors() throws IllegalArgumentException, IOException
    {
        //request
        ManagedExecutorsShutdownRequestData request = ManagedExecutorsShutdownRequestData.newBuilder().
                setShutdownNow(false).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ManagedExecutorsShutdownRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        //verify interaction
        verify(m_ManagedExecutors).shutdownAllExecutorServices();
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ManagedExecutorsShutdownRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        //verify response
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.ManagedExecutorsShutdownResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test an event from a response to shutting down managed executors.
     */
    @Test
    public void testShutdownManagedExecutorsResponse() throws IllegalArgumentException, IOException
    {
        //response message
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ManagedExecutorsShutdownResponse).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.ManagedExecutorsShutdownResponse.toString()));
    }
    
    /**
     * Test requesting event handler helper to unregister all handlers.
     * Verify response and the interaction with that service.
     */
    @Test
    public void testUnregisterHandlers() throws IllegalArgumentException, IOException
    {
        //request
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.UnregisterEventHandlerRequest).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        //verify interaction
        verify(m_EventHandlerHelper).unregisterAllHandlers();
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.UnregisterEventHandlerRequest.toString()));
        
        //verify response
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(message, 
                MissionProgrammingMessageType.UnregisterEventHandlerResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test an event from a response to unregistering event handlers.
     */
    @Test
    public void testUnregHandlerResponse() throws IllegalArgumentException, IOException
    {
        //response message
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.UnregisterEventHandlerResponse).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.UnregisterEventHandlerResponse.toString()));
    }
    
    /**
     * Test requesting to remove a program.
     * Verify correct program is removed.
     */
    @Test
    public void testRemoveProgram() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //mock mission program manager behavior
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn("a");
        when(m_Manager.getProgram("a")).thenReturn(program);
        
        //create request
        RemoveMissionProgramRequestData data = RemoveMissionProgramRequestData.newBuilder().
                setMissionName("a").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.RemoveMissionProgramRequest).
                setData(data.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.RemoveMissionProgramRequest.toString()));
        
        //verify interaction
        verify(program).remove();
        
        //verify response
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
            eq(MissionProgrammingMessageType.RemoveMissionProgramResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test an event from a response to removing a program.
     */
    @Test
    public void testRemoveResponse() throws IllegalArgumentException, IOException
    {
        //response message
        RemoveMissionProgramResponseData responseData = RemoveMissionProgramResponseData.newBuilder().
                setMissionName("name").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.RemoveMissionProgramResponse).
                setData(responseData.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.RemoveMissionProgramResponse.toString()));
    }
    
    /**
     * Test requesting to shutdown a program.
     * Verify correct program is shutdown.
     */
    @Test
    public void testShutdownProgram() throws IOException, ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, IllegalStateException, UnsupportedOperationException, 
           MissionProgramException
    {
        //activate the mission programming message service
        m_SUT.activate();
        
        //mock mission program manager behavior
        Program program = mock(Program.class);
        when(program.getProgramName()).thenReturn("a");
        when(program.getProgramStatus()).thenReturn(ProgramStatus.SHUTTING_DOWN);
        when(m_Manager.getProgram("a")).thenReturn(program);
        
        //create request
        ExecuteShutdownRequestData data = ExecuteShutdownRequestData.newBuilder().
                setMissionName("a").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteShutdownRequest).
                setData(data.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.ExecuteShutdownRequest.toString()));
        
        //verify interaction
        verify(program).executeShutdown();
        
        //verify response
        ArgumentCaptor<ExecuteShutdownResponseData> messageCaptor = 
                ArgumentCaptor.forClass(ExecuteShutdownResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.ExecuteShutdownResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        ExecuteShutdownResponseData response = messageCaptor.getValue();
        assertThat(response.getMissionName(), is("a"));
    }
    
    /**
     * Test an event from a response to a shutdown response.
     */
    @Test
    public void testShutdownResponse() throws IllegalArgumentException, IOException
    {
        //response message
        ExecuteShutdownResponseData response = ExecuteShutdownResponseData.newBuilder().
                setMissionName("a").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.ExecuteShutdownResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.ExecuteShutdownResponse.toString()));
        assertThat((ExecuteShutdownResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test the handling of a request to get a programs status.
     * Verify event is posted when the message is received
     */
    @Test
    public void testGetProgramStatus() throws IllegalArgumentException, IOException
    {
        //mock the program
        Program program = mock(Program.class);
        when(program.getProgramStatus()).thenReturn(ProgramStatus.SCRIPT_ERROR);
        when(program.getLastExecutionExceptionCause()).thenReturn("Random Exception");
        when(m_Manager.getProgram("man")).thenReturn(program);
        
        //request
        GetProgramStatusRequestData request = GetProgramStatusRequestData.newBuilder().setMissionName("man").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramStatusRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetProgramStatusRequest.toString()));
        assertThat((GetProgramStatusRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        ArgumentCaptor<GetProgramStatusResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetProgramStatusResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetProgramStatusResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetProgramStatusResponseData response = responseCaptor.getValue();
        assertThat(response.getMissionName(), is("man"));
        assertThat(response.getMissionStatus(), is(MissionStatus.SCRIPT_ERROR));
        assertThat(response.getException(), is("Random Exception"));
    }
    
    /**
     * Test an event from a response to getting a program's status.
     */
    @Test
    public void testGetProgramStatusResponse() throws IllegalArgumentException, IOException
    {
        //response message
        GetProgramStatusResponseData response = GetProgramStatusResponseData.newBuilder().
                setMissionName("pop").setMissionStatus(MissionStatus.CANCELED).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetProgramStatusResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetProgramStatusResponse.toString()));
        assertThat((GetProgramStatusResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test the handling of a request to get a program's last test results.
     * Verify event is posted when the message is received
     */
    @Test
    public void testGetProgramLastTestResult() throws IllegalArgumentException, IOException
    {
        //mock the program
        Program program = mock(Program.class);
        when(program.getLastTestResult()).thenReturn(TestResult.PASSED);
        when(program.getLastTestExceptionCause()).thenReturn("BLAH");
        when(m_Manager.getProgram("man")).thenReturn(program);
        
        //request
        GetLastTestResultsRequestData request = GetLastTestResultsRequestData.newBuilder().
                setMissionName("man").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetLastTestResultsRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetLastTestResultsRequest.toString()));
        assertThat((GetLastTestResultsRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        ArgumentCaptor<GetLastTestResultsResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetLastTestResultsResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetLastTestResultsResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLastTestResultsResponseData response = responseCaptor.getValue();
        assertThat(response.getMissionName(), is("man"));
        assertThat(response.getResult(), is(MissionTestResult.PASSED));
        assertThat(response.getException(), is("BLAH"));
    }
    
    /**
     * Test the handling of a request to get a program's last test results where the result does not exist.
     */
    @Test
    public void testGetProgramLastTestResultNull() throws IllegalArgumentException, IOException
    {
        //mock the program
        Program program = mock(Program.class);
        when(program.getLastTestResult()).thenReturn(null);
        when(program.getLastTestExceptionCause()).thenReturn("");
        when(m_Manager.getProgram("man")).thenReturn(program);
        
        //request
        GetLastTestResultsRequestData request = GetLastTestResultsRequestData.newBuilder().
                setMissionName("man").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetLastTestResultsRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        ArgumentCaptor<GetLastTestResultsResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetLastTestResultsResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
                eq(MissionProgrammingMessageType.GetLastTestResultsResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetLastTestResultsResponseData response = responseCaptor.getValue();
        assertThat(response.getMissionName(), is("man"));
        assertThat(response.hasResult(), is(false));
    }
    
    /**
     * Test an event from a response to getting a program's last test results.
     */
    @Test
    public void testGetLastTestResultResponse() throws IllegalArgumentException, IOException
    {
        //response message
        GetLastTestResultsResponseData response = GetLastTestResultsResponseData.newBuilder().
                setMissionName("pop").setResult(MissionTestResult.PASSED).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetLastTestResultsResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetLastTestResultsResponse.toString()));
        assertThat((GetLastTestResultsResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test the handling of a request to get a program's execution parameters.
     * Verify event is posted when the message is received
     */
    @Test
    public void testGetExecutionParamsResult() throws IllegalArgumentException, IOException
    {
        //mock the program
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("asset", "asset1");
        Program program = mock(Program.class);
        when(program.getExecutionParameters()).thenReturn(params);
        when(m_Manager.getProgram("man")).thenReturn(program);
        
        //request
        GetExecutionParametersRequestData request = GetExecutionParametersRequestData.newBuilder().
                setMissionName("man").build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetExecutionParametersRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetExecutionParametersRequest.toString()));
        assertThat((GetExecutionParametersRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        
        ArgumentCaptor<GetExecutionParametersResponseData> responseCaptor = 
                ArgumentCaptor.forClass(GetExecutionParametersResponseData.class);
        verify(m_MessageFactory).createMissionProgrammingResponseMessage(eq(message), 
            eq(MissionProgrammingMessageType.GetExecutionParametersResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetExecutionParametersResponseData response = responseCaptor.getValue();
        assertThat(response.getMissionName(), is("man"));
        assertThat(response.getExecutionParamCount(), is(1));
        assertThat(response.getExecutionParamList().get(0).getKey(), is("asset"));
        assertThat(response.getExecutionParamList().get(0).getValue().getStringValue(), is("asset1"));
    }
    
    /**
     * Test an event from a response to getting a program's execution parameters.
     */
    @Test
    public void testGetExecutionParamsResponse() throws IllegalArgumentException, IOException
    {
        //response message
        GetExecutionParametersResponseData response = GetExecutionParametersResponseData.newBuilder().
                setMissionName("pop").addExecutionParam(MapEntry.newBuilder().
                        setKey("asset").
                        setValue(Multitype.newBuilder().
                                setType(Type.STRING).
                                setStringValue("asset").build())).build();
        MissionProgrammingNamespace missionMessage = MissionProgrammingNamespace.newBuilder().
                setType(MissionProgrammingMessageType.GetExecutionParametersResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMissionProgrammingMessage(missionMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, createPayload(missionMessage), channel);
        
        // verify the event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(MissionProgrammingMessageType.GetExecutionParametersResponse.toString()));
        assertThat((GetExecutionParametersResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    TerraHarvestMessage createMissionProgrammingMessage(MissionProgrammingNamespace missionProgrammingNamespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(90, 1, Namespace.MissionProgramming, 100, 
                missionProgrammingNamespaceMessage);
    }
    private TerraHarvestPayload createPayload(MissionProgrammingNamespace missionProgrammingNamespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.MissionProgramming).
               setNamespaceMessage(missionProgrammingNamespaceMessage.toByteString()).
               build();
    }
}
