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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.MissionProgramMessages.GetTemplatesResponseData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.remote.proto.SharedMessages.UUID;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ActiveControllerImpl;
import mil.dod.th.ose.gui.webapp.controller.ControllerMgr;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;
import mil.dod.th.ose.gui.webapp.mp.MissionProgramMgrImpl.EventHelperControllerEvent;
import mil.dod.th.ose.gui.webapp.mp.MissionProgramMgrImpl.EventHelperMissionNamespace;
import mil.dod.th.ose.gui.webapp.mp.MissionProgramMgrImpl.MissionErrorHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.MapEntry;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

/**
 * Test the functionality of the mission program manager impl bean.
 * @author callen
 *
 */
public class TestMissionProgramMgrImpl 
{
    private MissionProgramMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private MissionProgramManager m_MPManager;
    private TemplateProgramManager m_TemplateManager;
    private BundleContextUtil m_BundleUtil;
    private JaxbProtoObjectConverter m_Converter;
    private EventHelperControllerEvent m_ListenerController;
    private EventHelperMissionNamespace m_ListenerMission;
    private ActiveController m_ActiveController;
    private GrowlMessageUtil m_GrowlUtil;
    @SuppressWarnings("rawtypes")
    private ServiceRegistration m_HandlerReg = mock(ServiceRegistration.class);
    private MessageWrapper m_MessageWrapper;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setUp()
    {
        //test class
        m_SUT = new MissionProgramMgrImpl();

        //mock services
        m_MessageFactory = mock(MessageFactory.class);
        m_MPManager = mock(MissionProgramManager.class);
        m_TemplateManager = mock(TemplateProgramManager.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        BundleContext bundleContext = mock(BundleContext.class);
        m_ActiveController = mock(ActiveControllerImpl.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        
        //set injected services
        m_SUT.setBundleContextUtil(m_BundleUtil);
        m_SUT.setProgramManager(m_MPManager);
        m_SUT.setTemplateProgramManager(m_TemplateManager);
        m_SUT.setConverter(m_Converter);
        m_SUT.setGrowlMessageUtility(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(bundleContext);
        when(bundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        when(m_MessageFactory.createMissionProgrammingMessage(
            Mockito.any(MissionProgrammingMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //register helper
        m_SUT.setupDependency();

        //verify
        ArgumentCaptor<EventHandler> captor = ArgumentCaptor.forClass(EventHandler.class);
        verify(bundleContext, times(2)).registerService(eq(EventHandler.class), captor.capture(), 
            Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(2)).getBundleContext();
        
        //enter classes
        m_ListenerController = (EventHelperControllerEvent) captor.getAllValues().get(0);
        m_ListenerMission = (EventHelperMissionNamespace) captor.getAllValues().get(1);
    }

    /**
     * Test get local program names.
     * 
     * Verify that the mission program manager is called to return a set of program objects.
     */
    @Test
    public void testGetLocalMissionNames()
    {
        //mock program objects
        Program prog1 = mock(Program.class);
        when(prog1.getProgramName()).thenReturn("Larry");
        Program prog2 = mock(Program.class);
        when(prog2.getProgramName()).thenReturn("Bob");
        Program prog3 = mock(Program.class);
        when(prog3.getProgramName()).thenReturn("James");

        //set of programs to return
        Set<Program> programs = new HashSet<Program>();
        programs.add(prog1);
        programs.add(prog2);
        programs.add(prog3);

        //mock behavior of mission program manager
        when(m_MPManager.getPrograms()).thenReturn(programs);

        //verify
        assertThat(m_SUT.getLocalMissionNames(), hasItems("Larry", "Bob", "James"));
    }
    
    /**
     * Test get remote mission names, does not return null when there are no known controllers.
     * 
     */
    @Test
    public void testGetRemoteMissionNames()
    {
        //mock controller
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.getActiveController()).thenReturn(model);

        //verify
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(0));
    }

    /**
     * Test handling of GetTemplates response.
     * 
     */
    @Test
    public void testGetTemplatesResponse() throws ObjectConverterException
    {
        //mock controller
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(model);

        Event event = mockGetTemplatesRequestMutiples(911);

        //behavior for converter
        MissionProgramTemplate template1 = mock(MissionProgramTemplate.class);
        when(template1.getName()).thenReturn("I am a name");
        MissionProgramTemplate template2 = mock(MissionProgramTemplate.class);
        when(template2.getName()).thenReturn("I am a name too");
        MissionProgramTemplate template3 = mock(MissionProgramTemplate.class);
        when(template3.getName()).thenReturn("I am a name three");
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(template1, template2, template3);

        m_ListenerMission.handleEvent(event);

        //verify that the templates were added
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(3));
    }

    /**
     * Test handling of GetTemplates response in the context of the event being an update.
     * 
     * Verify that the list of templates is emptied out.
     * 
     */
    @Test
    public void testGetTemplatesResponseUpdate() throws ObjectConverterException
    {
        //mock controller
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(model);

        Event event = mockGetTemplatesRequestMutiples(911);

        //behavior for converter
        MissionProgramTemplate template1 = mock(MissionProgramTemplate.class);
        when(template1.getName()).thenReturn("I am a name");
        MissionProgramTemplate template2 = mock(MissionProgramTemplate.class);
        when(template2.getName()).thenReturn("I am a name too");
        MissionProgramTemplate template3 = mock(MissionProgramTemplate.class);
        when(template3.getName()).thenReturn("I am a name three");
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(template1, template2, template3);

        m_ListenerMission.handleEvent(event);

        //verify that the templates were added
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(3));
        assertThat(m_SUT.getRemoteTemplateNames(911), hasItems("I am a name", "I am a name too", "I am a name three"));

        //this event will return only one template
        event = mockGetTemplatesRequest(911);
        m_ListenerMission.handleEvent(event);

        //verify update
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(1));
    }

    /**
     * Test handling of GetTemplates response when one template cannot be converted.
     * 
     * Verify other templates are still loaded.
     * 
     */
    @Test
    public void testGetTemplatesResponseObjConvertException() throws ObjectConverterException
    {
        //mock controller
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(model);

        Event event = mockGetTemplatesRequestMutiples(911);

        //behavior for converter
        MissionProgramTemplateGen.MissionProgramTemplate genTemp1 = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                setName("name1").
                setSource("source1").build();
        MissionProgramTemplateGen.MissionProgramTemplate genTemp2 = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                 setName("name2").
                 setSource("source2").build();
        MissionProgramTemplateGen.MissionProgramTemplate genTemp3 = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                setName("name3").
                setSource("source3").build();
        MissionProgramTemplate template1 = mock(MissionProgramTemplate.class);
        when(template1.getName()).thenReturn("I am a name");
        MissionProgramTemplate template3 = mock(MissionProgramTemplate.class);
        when(template3.getName()).thenReturn("I am a name three");
        when(m_Converter.convertToJaxb(genTemp1)).thenReturn(template1);
        when(m_Converter.convertToJaxb(genTemp3)).thenReturn(template3);
        when(m_Converter.convertToJaxb(genTemp2)).thenThrow(new ObjectConverterException("ABORTTTTT"));

        m_ListenerMission.handleEvent(event);

        //verify growl message of a template failing
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), eq("Syncing Templates Warning"), 
            eq("A template from controller 0x0000038f wasn't able to be processed."), 
                Mockito.any(ObjectConverterException.class));

        //verify that the templates were added
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(2));
    }

    /**
     * Test handling of GetTemplates response with a template being unable to be persisted.
     * 
     * Verify that the list of templates retrieved for the system still contains all templates.
     * 
     */
    @Test
    public void testGetTemplatesPersistenceFail() throws ObjectConverterException, IllegalArgumentException, 
        PersistenceFailedException
    {
        //mock controller
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(model);

        Event event = mockGetTemplatesRequestMutiples(911);

        //behavior for converter
        MissionProgramTemplate template1 = mock(MissionProgramTemplate.class);
        when(template1.getName()).thenReturn("I am a name");
        MissionProgramTemplate template2 = mock(MissionProgramTemplate.class);
        when(template2.getName()).thenReturn("I am a name too");
        MissionProgramTemplate template3 = mock(MissionProgramTemplate.class);
        when(template3.getName()).thenReturn("I am a name three");
        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(template1, template2, template3);
        
        //template manager behavior
        doThrow(new PersistenceFailedException("asdfsa")).when(m_TemplateManager).loadMissionTemplate(template1);

        m_ListenerMission.handleEvent(event);

        //verify growl message of a template failing
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_WARN), eq("Syncing Templates Info"), 
            eq("A template from controller 0x0000038f wasn't able to be stored locally."), 
                Mockito.any(PersistenceFailedException.class));

        //verify that the templates were added
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(3));
        assertThat(m_SUT.getRemoteTemplateNames(911), hasItems("I am a name", "I am a name too", "I am a name three"));
    }

    /**
     * Test receiving a controller added event.
     * 
     * Verify that the request for templates and remote events is sent.
     */
    @Test
    public void testControllerAdded()
    {
        //the system id to use
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(model);

        m_SUT.requestSyncingOfTemplates(911);

        //verify the request for templates is sent
        verify(m_MessageFactory).createMissionProgrammingMessage(
            eq(MissionProgrammingMessageType.GetTemplatesRequest), Mockito.any(Message.class));
        verify(m_MessageWrapper).queue(eq(911), (ResponseHandler) eq(null));
        //verify that the request to get templates is made
        verify(m_MessageFactory).createMissionProgrammingMessage(MissionProgrammingMessageType.GetTemplatesRequest,
            null);
        verify(m_MessageWrapper).queue(eq(911), (ResponseHandler) eq(null));
    }

    /**
     * Test receiving a controller removed event.
     * 
     * Verify that the templates in the lookup for the removed controller are in fact removed.
     */
    @Test
    public void testControllerRemoved() throws ObjectConverterException
    {
        //the system id to use
        int systemId = 911;

        //mock controller
        ControllerModel model = mock(ControllerModel.class);
        when(model.getId()).thenReturn(911);
        
        //active controller behavior
        when(m_ActiveController.isActiveControllerSet()).thenReturn(true);
        when(m_ActiveController.getActiveController()).thenReturn(model);

        //behavior for converter
        MissionProgramTemplate template1 = mock(MissionProgramTemplate.class);
        when(template1.getName()).thenReturn("I am a name");
        MissionProgramTemplate template2 = mock(MissionProgramTemplate.class);
        when(template2.getName()).thenReturn("I am a name too");
        MissionProgramTemplate template3 = mock(MissionProgramTemplate.class);
        when(template3.getName()).thenReturn("I am a name three");

        when(m_Converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(template1, template2, template3);

        //load templates
        Event event = mockGetTemplatesRequestMutiples(systemId);
        m_ListenerMission.handleEvent(event);

        //verify that there are template registered for the given system id
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(3));

        //remove controller
        event = mockControllerRemovedEvent(systemId);
        m_ListenerController.handleEvent(event);

        //verify that the listing for the given system is removed
        assertThat(m_SUT.getRemoteTemplateNames(911).size(), is(0));
    }

    /**
     * Test the predestroy unregistering of event handlers.
     * Verify that both are unregistered.
     */
    @Test
    public void testPreDestroy()
    {
        m_SUT.unregisterHelpers();

        //verify listeners are unregistered, there are 5
        verify(m_HandlerReg, times(2)).unregister();
    }
    
    /**
     * Verify mission error handler response handler outputs a proper error message.
     */
    @Test
    public void testMissionErrorHandler()
    {
        MissionProgramParametersGen.MissionProgramSchedule sched = MissionProgramParametersGen.MissionProgramSchedule.
                newBuilder().setAtReset(true).setImmediately(true).setIndefiniteInterval(true).build();
        
        Multitype type = Multitype.newBuilder().setType(Type.STRING).setStringValue("value").build();
        
        MapEntry entry = MapEntry.newBuilder().setKey("key").setValue(type).build();
        
        MissionProgramParametersGen.MissionProgramParameters params = MissionProgramParametersGen.
                MissionProgramParameters.newBuilder().setProgramName("program1").setTemplateName("tName").
                setSchedule(sched).addParameters(entry).build();
        
        m_SUT.queueMessage(1, LoadParametersRequestData.newBuilder().setParameters(params).build(), 
                MissionProgrammingMessageType.LoadParametersRequest);
        
        ArgumentCaptor<MissionErrorHandler> argumentCaptor = ArgumentCaptor.forClass(MissionErrorHandler.class);
        
        verify(m_MessageWrapper).queue(eq(1), argumentCaptor.capture());
        
        //build the data first
        final GenericErrorResponseData genericErrorResponseMessage = GenericErrorResponseData.newBuilder().
            setError(ErrorCode.ILLEGAL_STATE).
            setErrorDescription("Error desc").
            build();
        
        // build a base namespace message
        final BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GenericErrorResponse).
                setData(genericErrorResponseMessage.toByteString()).
                build();
        //build a TerraHarvestPayLoad message
        final TerraHarvestPayload payLoad = TerraHarvestPayload.newBuilder().
                setNamespace(Namespace.Base).
                setNamespaceMessage(baseMessage.toByteString()).
                build();
        // build a terra harvest message
        final TerraHarvestMessage fullMessage = TerraHarvestMessage.newBuilder().
                setVersion(RemoteConstants.SPEC_VERSION).
                setSourceId(1).
                // send message back to source
                setDestId(0).
                setMessageId(22).
                setTerraHarvestPayload(payLoad.toByteString()).
                build();
        
        MissionErrorHandler errorHandler = argumentCaptor.getValue();
        assertThat(errorHandler, notNullValue());
        
        errorHandler.handleResponse(fullMessage, payLoad, baseMessage, genericErrorResponseMessage);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Mission Loading Error Occurred"), Mockito.anyString());
    }
    
    /**
     * Verify that with wrong namespace nothing happens.
     */
    @Test
    public void testMissionErrorHandlerWithWrongNamespace()
    {
        MissionErrorHandler errHandler = m_SUT.new MissionErrorHandler(0);
        
        TerraHarvestPayload payload = TerraHarvestPayload.newBuilder()
                .setNamespace(Namespace.Asset)
                .setNamespaceMessage(ByteString.EMPTY).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(payload);
        AssetNamespace namespaceMessage = AssetNamespace.newBuilder()
                .setType(AssetMessageType.ExecuteCommandResponse)
                .setData(ByteString.EMPTY).build();
        ExecuteCommandResponseData dataMsg = ExecuteCommandResponseData.newBuilder()
                .setResponseType(CommandTypesGen.CommandResponse.Enum.CAPTURE_IMAGE_RESPONSE)
                .setResponse(ByteString.EMPTY)
                .setUuid(UUID.newBuilder().setLeastSignificantBits(0).setMostSignificantBits(0).build())
                .build();
        
        errHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Mission Loading Error Occurred"), Mockito.anyString());
    }
    
    /**
     * Verify that with the right namespace but wrong type does nothing.
     */
    @Test
    public void testMissionErrorHandlerWithBaseNamespaceWrongType()
    {
        MissionErrorHandler errHandler = m_SUT.new MissionErrorHandler(0);
        
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
                eq("Mission Loading Error Occurred"), Mockito.anyString());
    }
    

    //////////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                                  //
    //The methods below are used for mocking events that the class under test needs to be able to       //
    //process.                                                                                          //
    //////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Used to mock the event that a controller was removed.
     */
    private Event mockControllerRemovedEvent(final int systemId)
    {
        //Create a controller removed event and post it to the event admin service.
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, systemId);
        return new Event(ControllerMgr.TOPIC_CONTROLLER_REMOVED, props);
    }

    /**
     * Used to mock the event that a response to get templates was received.
     */
    private Event mockGetTemplatesRequest(final int systemId)
    {
        MissionProgramTemplateGen.MissionProgramTemplate genTemp = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                setName("name1").
                setSource("source1").build();
        
        GetTemplatesResponseData data = GetTemplatesResponseData.newBuilder().addTemplate(genTemp).build();
        
        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, data);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.MissionProgramming.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            MissionProgrammingMessageType.GetTemplatesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }

    /**
     * Used to mock the event that a response to get templates was received.
     */
    private Event mockGetTemplatesRequestMutiples(final int systemId)
    {
        MissionProgramTemplateGen.MissionProgramTemplate genTemp1 = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                setName("name1").
                setSource("source1").build();
        MissionProgramTemplateGen.MissionProgramTemplate genTemp2 = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                setName("name2").
                setSource("source2").build();
        MissionProgramTemplateGen.MissionProgramTemplate genTemp3 = 
            MissionProgramTemplateGen.MissionProgramTemplate.newBuilder().
                setName("name3").
                setSource("source3").build();

        GetTemplatesResponseData data = GetTemplatesResponseData.newBuilder().addTemplate(genTemp1).
             addTemplate(genTemp2).addTemplate(genTemp3).build();

        // properties for the event
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, data);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, systemId);
        props.put(RemoteConstants.EVENT_PROP_DEST_ID, 0);
        props.put(RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.MissionProgramming.toString());
        props.put(RemoteConstants.EVENT_PROP_MESSAGE_TYPE, 
            MissionProgrammingMessageType.GetTemplatesResponse.toString());
        //the event
        return new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
    }
}
