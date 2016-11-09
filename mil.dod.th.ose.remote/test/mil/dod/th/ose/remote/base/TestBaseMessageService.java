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
package mil.dod.th.ose.remote.base;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesRequestData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetControllerCapabilitiesResponseData.ControllerCapabilitiesCase;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.BaseMessages.SetOperationModeRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen;
import mil.dod.th.remote.lexicon.controller.capability.ControllerCapabilitiesGen.ControllerCapabilities;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.Message;

/**
 * @author Dave Humeniuk
 *
 */
public class TestBaseMessageService
{
    private BaseMessageService m_SUT;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private TerraHarvestSystem m_TerraHarvestSystem;
    private TerraHarvestController m_TerraHarvestController;
    private MessageRouterInternal m_MessageRouter;
    private JaxbProtoObjectConverter m_Converter;
    private MessageResponseWrapper m_ResponseWrapper;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new BaseMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_MessageFactory = mock(MessageFactory.class);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_TerraHarvestSystem = mock(TerraHarvestSystem.class);
        m_TerraHarvestController = mock(TerraHarvestController.class);
        m_SUT.setTerraHarvestController(m_TerraHarvestController);
        m_SUT.setTerraHarvestSystem(m_TerraHarvestSystem);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        
        when(m_MessageFactory.createBaseMessageResponse(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(BaseMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
    }

    /**
     * Verify the namespace is Base
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.Base));
    }
    
    /**
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        // mock context and activate
        BundleContext context = mock(BundleContext.class);
        Bundle systemBundle = mock(Bundle.class);
        when(context.getBundle(0)).thenReturn(systemBundle); // system bundle is always id 0
        
        m_SUT.activate(context);
        
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }
    
    /**
     * Verify the received shutdown request message sends an event with null data message.
     */
    @Test
    public void testReceivedShutdownRequest() throws IOException
    {
        BaseNamespace baseMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.ReceivedShutdownRequest).
                build();
        
        TerraHarvestPayload payload = createPayload(baseMessage);
        TerraHarvestMessage message = createBaseMessage(baseMessage);
        
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
                is(BaseMessageType.ReceivedShutdownRequest.toString()));
    }
    
    /**
     * Verify generic handling of message, that it posts event
     */
    @Test
    public void testGenericHandleMessage() throws IOException
    {
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(baseNamespaceMessage.getType().toString()));
        assertThat((BaseNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(baseNamespaceMessage));
        assertThat((ControllerInfoData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(systemInfoData));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify that a generic error response message is handled and that the event posted contains the appropriate
     * information.
     */
    @Test
    public void testGenericHandleErrorResponse() throws IOException
    {
        // construct a single message containing data within a namespace message
        GenericErrorResponseData genericError = GenericErrorResponseData.newBuilder()
                .setError(ErrorCode.INTERNAL_ERROR)
                .setErrorDescription("OH NO! AN INTERNAL ERROR!").build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GenericErrorResponse)
                .setData(genericError.toByteString())
                .build();
        
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(baseNamespaceMessage.getType().toString()));
        assertThat((BaseNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(baseNamespaceMessage));
        assertThat((GenericErrorResponseData)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(genericError));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));
    }
    
    /**
     * Verify generic handling of message, with the optional time and without. 
     */
    @Test
    public void testGenericHandleMessageWithTime() throws IOException
    {
        // construct a single message containing data within a namespace message
        ControllerInfoData systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .build();
        
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
                
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(baseNamespaceMessage.getType().toString()));
        
         // Replay with time
        systemInfoData = ControllerInfoData.newBuilder()
                .setName("test-name")
                .setCurrentSystemTime(System.currentTimeMillis())
                .build();
        
        baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ControllerInfo)
                .setData(systemInfoData.toByteString())
                .build();
        
        message = createBaseMessage(baseNamespaceMessage);
        payload = createPayload(baseNamespaceMessage);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify event is posted
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        postedEvent = eventCaptor.getValue();
        assertThat(postedEvent.getTopic(), is(RemoteConstants.TOPIC_MESSAGE_RECEIVED));
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        assertThat((String)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
                is(baseNamespaceMessage.getType().toString()));
    }

    /**
     * Verify handling of RequestSystemInfo, should send SystemInfo
     */
    @Test
    public void testRequestSystemInfo() throws IOException
    {
        //build info map
        Map<String, String> props = new HashMap<String, String>();
        props.put("key", "info");
        // mock system interface
        when(m_TerraHarvestController.getName()).thenReturn("system-name");
        when(m_TerraHarvestController.getVersion()).thenReturn("1.0");
        when(m_TerraHarvestController.getBuildInfo()).thenReturn(props);
        
        // construct the request system info message
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.RequestControllerInfo)
                .build();
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify system info message is sent back to source requester
        ArgumentCaptor<ControllerInfoData> messageCaptor = ArgumentCaptor.forClass(ControllerInfoData.class);
        verify(m_MessageFactory).createBaseMessageResponse(eq(message), eq(BaseMessageType.ControllerInfo),
                messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        // verify content of message
        assertThat(messageCaptor.getValue().getName(), is("system-name"));
        assertThat(messageCaptor.getValue().getVersion(), is("1.0"));
        assertThat(messageCaptor.getValue().getBuildInfoList().get(0).getKey(), is("key"));
        assertThat(messageCaptor.getValue().getBuildInfoList().get(0).getValue().getStringValue(), is("info"));
        assertThat(messageCaptor.getValue().getCurrentSystemTime(), lessThanOrEqualTo(System.currentTimeMillis()));
    }

    /**
     * Verify handling of ShutdownSystem with a bundle exception, should queue an error response message.
     */
    @Test
    public void testShutdownSystemBundleException() throws BundleException, IOException
    {
        // mock context and activate
        BundleContext context = mock(BundleContext.class);
        Bundle systemBundle = mock(Bundle.class);
        when(context.getBundle(0)).thenReturn(systemBundle); // system bundle is always id 0
        m_SUT.activate(context);
        
        // construct the shutdown system message
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ShutdownSystem)
                .build();
     
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        doThrow(new BundleException("arghhh bundle exception")).when(systemBundle).stop();
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify system bundle is told to stop which would stop OSGi framework
        verify(systemBundle).stop();
        
        // verify response message is sent back to source requester
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.INTERNAL_ERROR), 
                Mockito.anyString());
        //once for the initial acknowledgment, and then again for the exception
        verify(m_ResponseWrapper, times(2)).queue(channel);
    }
    
    /**
     * Verify handling of ShutdownSystem, should call on system framework bundle to stop.
     */
    @Test
    public void testShutdownSystem() throws IOException, BundleException
    {
        // mock context and activate
        BundleContext context = mock(BundleContext.class);
        Bundle systemBundle = mock(Bundle.class);
        when(context.getBundle(0)).thenReturn(systemBundle); // system bundle is always id 0
        m_SUT.activate(context);
        
        // construct the shutdown system message
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.ShutdownSystem)
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify system bundle is told to stop which would stop OSGi framework
        verify(systemBundle).stop();
        
        // verify response message is sent back to source requester
        verify(m_MessageFactory).createBaseMessageResponse(message, BaseMessageType.ReceivedShutdownRequest, null); 
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Verify handling of get system status request when the mode is test.
     */
    @Test
    public void testGetSystemStatus() throws IOException, BundleException
    {
        // construct the message
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GetOperationModeRequest)
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //terra harvest controller
        when(m_TerraHarvestController.getOperationMode()).thenReturn(OperationMode.TEST_MODE);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify response message is sent back to source requester
        ArgumentCaptor<GetOperationModeReponseData> messageCaptor = 
            ArgumentCaptor.forClass(GetOperationModeReponseData.class);
        verify(m_MessageFactory).createBaseMessageResponse(eq(message), eq(BaseMessageType.GetOperationModeResponse), 
            messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        //inspect message
        GetOperationModeReponseData response = messageCaptor.getValue();
        assertThat(response.getMode(), is(BaseMessages.OperationMode.TEST_MODE));

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }

    /**
     * Verify handling of get system status request.
     */
    @Test
    public void testGetSystemStatusOperationMode() throws IOException, BundleException
    {
        // construct the message
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GetOperationModeRequest)
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        //terra harvest controller
        when(m_TerraHarvestController.getOperationMode()).thenReturn(OperationMode.OPERATIONAL_MODE);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify response message is sent back to source requester
        ArgumentCaptor<GetOperationModeReponseData> messageCaptor = 
            ArgumentCaptor.forClass(GetOperationModeReponseData.class);
        verify(m_MessageFactory).createBaseMessageResponse(eq(message), eq(BaseMessageType.GetOperationModeResponse), 
            messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        //inspect message
        GetOperationModeReponseData response = messageCaptor.getValue();
        assertThat(response.getMode(), is(BaseMessages.OperationMode.OPERATIONAL_MODE));

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }
    
    /**
     * Verify handling of set system status request to operational mode.
     */
    @Test
    public void testSetSystemStatus() throws IOException, BundleException
    {
        // construct the set system mode message
        SetOperationModeRequestData request = SetOperationModeRequestData.newBuilder()
                .setMode(BaseMessages.OperationMode.OPERATIONAL_MODE).build();
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.SetOperationModeRequest)
                .setData(request.toByteString())
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify response message is sent back to source requester
        verify(m_MessageFactory).createBaseMessageResponse(eq(message), eq(BaseMessageType.SetOperationModeResponse), 
            eq((Message)null));
        verify(m_ResponseWrapper).queue(channel);
        
        verify(m_TerraHarvestController).setOperationMode(OperationMode.OPERATIONAL_MODE);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }

    /**
     * Verify handling of set system status request to test mode.
     */
    @Test
    public void testSetSystemStatusTestMode() throws IOException, BundleException
    {
        // construct the set system mode message
        SetOperationModeRequestData request = SetOperationModeRequestData.newBuilder()
                .setMode(BaseMessages.OperationMode.TEST_MODE).build();
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.SetOperationModeRequest)
                .setData(request.toByteString())
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify response message is sent back to source requester
        verify(m_MessageFactory).createBaseMessageResponse(eq(message), eq(BaseMessageType.SetOperationModeResponse), 
            eq((Message)null));
        verify(m_ResponseWrapper).queue(channel);
        
        verify(m_TerraHarvestController).setOperationMode(OperationMode.TEST_MODE);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }
    
    /**
     * Verify handling of set system status response.
     */
    @Test
    public void testSetSystemStatusResponse() throws IOException, BundleException
    {
        // construct the response message
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.SetOperationModeResponse)
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }

    /**
     * Verify handling of get system status response.
     */
    @Test
    public void testGetSystemStatusResponse() throws IOException, BundleException
    {
        // construct the shutdown system message
        GetOperationModeReponseData request = GetOperationModeReponseData.newBuilder()
                .setMode(BaseMessages.OperationMode.OPERATIONAL_MODE).build();
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GetOperationModeResponse)
                .setData(request.toByteString())
                .build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
     
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }
    
    /**
     * Verify handling of get controller capabilities request.
     */
    @Test
    public final void testGetControllerCapRequest() throws UnmarshalException, IOException, ObjectConverterException
    {
        
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(Mockito.any(Bundle.class));
        m_SUT.activate(bundleContext);
        ControllerCapabilities capsProto = ControllerCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setProductName("something").
                    setDescription("someDescription")).
                setLowPowerModeSupported(false).setVoltageReported(false).
                setBatteryAmpHourReported(false).
                build();

        mil.dod.th.core.controller.capability.ControllerCapabilities capabilities = 
                mock (mil.dod.th.core.controller.capability.ControllerCapabilities.class);
        when(m_TerraHarvestController.getCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(capabilities)).thenReturn(capsProto);

        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GetControllerCapabilitiesRequest).build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
        
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
        
        ArgumentCaptor<GetControllerCapabilitiesResponseData> responseCaptor =
                ArgumentCaptor.forClass(GetControllerCapabilitiesResponseData.class);
        verify(m_MessageFactory).createBaseMessageResponse(eq(message), 
                eq(BaseMessageType.GetControllerCapabilitiesResponse), responseCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetControllerCapabilitiesResponseData response = responseCaptor.getValue();
        assertThat(response.getControllerCapabilitiesCase(), 
                is(ControllerCapabilitiesCase.CONTROLLERCAPABILITIESNATIVE));
        assertThat(response.getControllerCapabilitiesNative(), is(capsProto));  
    }
    
    /**
     * Verify exception is thrown if requested format is invalid.
     */
    @Test
    public final void testGetControllerCapRequest_InvalidFormat() throws Exception
    {
        //build request
        GetControllerCapabilitiesRequestData requestData = GetControllerCapabilitiesRequestData.newBuilder()
                .setControllerCapabilitiesFormat(RemoteTypesGen.LexiconFormat.Enum.XML).build();
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GetControllerCapabilitiesRequest)
                .setData(requestData.toByteString()).build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        try
        {
            m_SUT.handleMessage(message, payload, channel);
            fail("Expecting exception as requested format is not supported");
        }
        catch (UnsupportedOperationException e)
        {
            
        }
    }

    /**
     * Verify that correct controller capabilities response is sent when controller capabilities request is received.
     * 
     */
    @Test
    public void testGetControllerCapabilitiesResponse() throws 
        IOException, BundleException, UnmarshalException, ObjectConverterException
    {
        ControllerCapabilities caps = ControllerCapabilities.newBuilder().
                setBase(BaseCapabilitiesGen.BaseCapabilities.newBuilder().
                    setProductName("something").
                    setDescription("someDescription")).
                setLowPowerModeSupported(false).setVoltageReported(false).
                setBatteryAmpHourReported(false).build();
        GetControllerCapabilitiesResponseData responseData = 
            GetControllerCapabilitiesResponseData.newBuilder().setControllerCapabilitiesNative(caps).build();
        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GetControllerCapabilitiesResponse).setData(responseData.toByteString()).build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
        
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        assertThat((String)eventCaptor.getValue().getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE), 
            is(baseNamespaceMessage.getType().toString()));
    }
       
    /**
     *  
     * Verify remote error message when there is unmarshal exception thrown.
     */
    @Test
    public void testObjectConverterExceptionGetControllerCap() 
        throws IOException, UnmarshalException, ObjectConverterException
    
    {
        BundleContext bundleContext = mock(BundleContext.class);
        when(bundleContext.getBundle(0)).thenReturn(Mockito.any(Bundle.class));
        m_SUT.activate(bundleContext);
 
        mil.dod.th.core.controller.capability.ControllerCapabilities capabilities = 
                mock (mil.dod.th.core.controller.capability.ControllerCapabilities.class);
        when(m_TerraHarvestController.getCapabilities()).thenReturn(capabilities);
        when(m_Converter.convertToProto(capabilities)).
            thenThrow(new ObjectConverterException("Object Converter Error"));

        BaseNamespace baseNamespaceMessage = BaseNamespace.newBuilder().
                setType(BaseMessageType.GetControllerCapabilitiesRequest).build();
        TerraHarvestPayload payload = createPayload(baseNamespaceMessage);
        
        TerraHarvestMessage message = createBaseMessage(baseNamespaceMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Unable to convert from java object to google protocol buffer message. " +
                "Object Converter Error");
        verify(m_ResponseWrapper).queue(channel);  
    }    
    
    private TerraHarvestMessage createBaseMessage(BaseNamespace baseNamespaceMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(5, 1, Namespace.Base, 100, 
                baseNamespaceMessage);
    }
    private TerraHarvestPayload createPayload(BaseNamespace baseNamespaceMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.Base).
               setNamespaceMessage(baseNamespaceMessage.toByteString()).
               build();
    }
}
