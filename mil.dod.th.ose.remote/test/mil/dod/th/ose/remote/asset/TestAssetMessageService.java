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

package mil.dod.th.ose.remote.asset;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetPositionCommand;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageResponseWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages;
import mil.dod.th.core.remote.proto.AssetMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataResponseData.ObservationCase;
import mil.dod.th.core.remote.proto.AssetMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetActiveStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetLastStatusResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.GetNameResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitResponseData.StatusObservationCase;
import mil.dod.th.core.remote.proto.AssetMessages.SetNameRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype.Type;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.TerraHarvestMessageHelper;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.CommandTypeEnumConverter;
import mil.dod.th.remote.lexicon.asset.commands.CaptureImageResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPanTiltResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPositionCommandGen;
import mil.dod.th.remote.lexicon.observation.types.ObservationGen;
import mil.dod.th.remote.lexicon.observation.types.StatusGen.Status;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.Version;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen.CommandType;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;
import mil.dod.th.remote.lexicon.types.status.StatusTypesGen;
import mil.dod.th.remote.lexicon.types.status.StatusTypesGen.OperatingStatus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

public class TestAssetMessageService
{
    private AssetMessageService m_SUT;
    private UUID m_AssetUuid = UUID.randomUUID();
    private AssetDirectoryService m_AssetDirectoryService;
    private LoggingService m_Logging;
    private EventAdmin m_EventAdmin;
    private MessageFactory m_MessageFactory;
    private JaxbProtoObjectConverter m_Converter;
    private MessageRouterInternal m_MessageRouter;
    private MessageResponseWrapper m_ResponseWrapper;
    private CommandConverter m_CommandConverterService;
    private UUID testUuid = UUID.randomUUID();
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new AssetMessageService();
        m_EventAdmin = mock(EventAdmin.class);
        m_AssetDirectoryService = mock(AssetDirectoryService.class);
        m_Logging = LoggingServiceMocker.createMock();
        m_MessageFactory = mock(MessageFactory.class);
        m_ResponseWrapper = mock(MessageResponseWrapper.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_MessageRouter = mock(MessageRouterInternal.class);
        m_CommandConverterService = mock(CommandConverter.class);

        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);  
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setMessageFactory(m_MessageFactory);  
        m_SUT.setJaxbProtoObjectConverter(m_Converter);
        m_SUT.setMessageRouter(m_MessageRouter);
        m_SUT.setCommandConverter(m_CommandConverterService);

        when(m_MessageFactory.createAssetResponseMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(AssetMessageType.class), Mockito.any(Message.class))).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorMessage(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(ErrorCode.class), Mockito.anyString())).thenReturn(m_ResponseWrapper);
        when(m_MessageFactory.createBaseErrorResponse(Mockito.any(TerraHarvestMessage.class), 
                Mockito.any(Exception.class), anyString())).thenReturn(m_ResponseWrapper);
        
        m_SUT.activate();
    }

    /**
     * Test the activation of the remote asset service. 
     * 
     * Verify message service is registered on activation and unregistered on deactivation.
     */
    @Test 
    public void testActivateDeactivate()
    {
        // verify service is bound
        verify(m_MessageRouter).bindMessageService(m_SUT);
        
        m_SUT.deactivate();
        
        // verify service is unbound
        verify(m_MessageRouter).unbindMessageService(m_SUT);
    }

    /**
     * Verify the namespace is Asset
     */
    @Test
    public void testGetNamespace()
    {
        assertThat(m_SUT.getNamespace(), is(Namespace.Asset));
    }

    /**
     * Verify if a conversion exception occurs during a command response, an error response is sent instead.
     */
    @Test
    public final void testObjectConverterExceptionExecuteCommand() throws 
        ObjectConverterException, IOException
    {
        //Get the ordinal value of the command
        final CommandType.Enum commandType = CommandTypeEnumConverter
                .convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND);
        
        //Generate the proto command   
        SetPositionCommandGen.SetPositionCommand setPositionProto = 
                SetPositionCommandGen.SetPositionCommand.newBuilder()
                .setBase(BaseTypesGen.Command.getDefaultInstance()).build();
        //Generate the proto request data
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setCommandType(commandType).setCommand(setPositionProto.toByteString()).build();
        //Generate the namespace
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandRequest).
                setData(request.toByteString()).build();
        //Generate the TerraHarvest message
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock
        Asset asset = mock(Asset.class);
        
        when(asset.getName()).thenReturn("Name");
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        when(m_CommandConverterService.getResponseTypeFromCommandType(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(CommandResponseEnum.SET_POSITION_RESPONSE);
        Command command = mock(Command.class);
        when(m_CommandConverterService.getJavaCommandType(
                Mockito.any(byte[].class), eq(CommandTypeEnum.SET_POSITION_COMMAND)))
            .thenReturn(command);
       
        Exception exception = new ObjectConverterException("Cannot Execute Command");
        when(m_Converter.convertToProto(Mockito.any(Command.class))).thenThrow(exception);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_ResponseWrapper, timeout(2000)).queue(channel);
        verify(m_MessageFactory).createBaseErrorResponse(message, exception, 
                String.format("Unable to convert command response to " 
                        + "proto message for asset %s when executing command %s", asset.getName(), 
                        CommandTypeEnum.SET_POSITION_COMMAND));       
    }
    
    /*
     * Verify that if an object converter exception occurs when trying to retrieve the java command 
     * then the exception is properly handled.
     */
    @Test
    public void testCommandExecutionObjectConverterException() throws ObjectConverterException, IOException
    {
        //Get the ordinal value of the command
        final CommandType.Enum commandType = CommandTypeEnumConverter
                .convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND);
        
        //Generate the proto command   
        SetPositionCommandGen.SetPositionCommand setPositionProto = 
                SetPositionCommandGen.SetPositionCommand.newBuilder()
                .setBase(BaseTypesGen.Command.getDefaultInstance())
                .build();
        //Generate the proto request data
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setCommandType(commandType).setCommand(setPositionProto.toByteString()).build();
       //Generate the namespace
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandRequest).
                setData(request.toByteString()).build();
       //Generate the TerraHarvest message
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        when(m_CommandConverterService.getResponseTypeFromCommandType(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(CommandResponseEnum.SET_POSITION_RESPONSE);
        
        Exception exception = new ObjectConverterException("");
        when(m_CommandConverterService.getJavaCommandType(Mockito.any(byte[].class), 
                    eq(CommandTypeEnum.SET_POSITION_COMMAND))).thenThrow(exception);
        
        //mock
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(m_AssetUuid);
        when(asset.getName()).thenReturn("Name");
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory).createBaseErrorResponse(message, exception, String.format(
                "Unable to retrieve java command from execute request for asset %s. Cannot execute command %s.", 
                asset.getName(), CommandTypeEnum.SET_POSITION_COMMAND));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify command execution with interrupted exception is properly handled.
     */
    @Test
    public void testCommandExecutionInterruptedException() throws 
        IOException, ObjectConverterException, CommandExecutionException, InterruptedException
    {
        //Get the ordinal value of the command
        final CommandType.Enum commandType = CommandTypeEnumConverter
                .convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND);
        
        //Generate the proto command   
        SetPositionCommandGen.SetPositionCommand setPositionProto = 
                SetPositionCommandGen.SetPositionCommand.newBuilder()
                .setBase(BaseTypesGen.Command.getDefaultInstance()).build();
        //Generate the proto request data
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setCommandType(commandType).setCommand(setPositionProto.toByteString()).build();
       //Generate the namespace
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandRequest).
                setData(request.toByteString()).build();
       //Generate the TerraHarvest message
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        when(m_CommandConverterService.getResponseTypeFromCommandType(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(CommandResponseEnum.SET_POSITION_RESPONSE);
        
        SetPositionCommand command = new SetPositionCommand();
        when(m_CommandConverterService.getJavaCommandType(Mockito.any(byte[].class), 
                    eq(CommandTypeEnum.SET_POSITION_COMMAND))).thenReturn(command);
        //mock
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(m_AssetUuid);
        when(asset.getName()).thenReturn("Name");
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        InterruptedException exception = new InterruptedException("Excuse Me!");
        when(asset.executeCommand(command)).thenThrow(exception);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_ResponseWrapper, timeout(5000)).queue(channel);
        
        verify(m_MessageFactory).createBaseErrorResponse(message, exception, 
                String.format("Execution of command %s for asset %s was interrupted.", 
                CommandTypeEnum.SET_POSITION_COMMAND, asset.getName()));
    }
    
    /**
     * Verify any other exception besides a CommandExecutionException and an ObjectConverterException
     * will be caught by the thread that executes the command.
     */
    @Test
    public void testCommandExecutionThreadGenericExcpetion() 
        throws CommandExecutionException, ObjectConverterException, IOException, InterruptedException
    {
        //Get the ordinal value of the command
        final CommandType.Enum commandType = CommandTypeEnumConverter
                .convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND);
        
        //Generate the proto command   
        SetPositionCommandGen.SetPositionCommand setPositionProto = 
                SetPositionCommandGen.SetPositionCommand.newBuilder()
                .setBase(BaseTypesGen.Command.getDefaultInstance())
                .build();
        //Generate the proto request data
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setCommandType(commandType).setCommand(setPositionProto.toByteString()).build();
        //Generate the namespace
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandRequest).
                setData(request.toByteString()).build();
        //Generate the TerraHarvest message
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        when(m_CommandConverterService.getResponseTypeFromCommandType(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(CommandResponseEnum.SET_POSITION_RESPONSE);
        
        SetPositionCommand command = new SetPositionCommand();
        when(m_CommandConverterService.getJavaCommandType(Mockito.any(byte[].class), 
                    eq(CommandTypeEnum.SET_POSITION_COMMAND))).thenReturn(command);
        //mock
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(m_AssetUuid);
        when(asset.getName()).thenReturn("Name");
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        RuntimeException exception = new RuntimeException();
        when(asset.executeCommand(command)).thenThrow(exception);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_ResponseWrapper, timeout(5000)).queue(channel);
        verify(m_MessageFactory).createBaseErrorResponse(message, exception, 
                String.format("An exception occurred for asset %s while processing command %s.", 
                        asset.getName(), CommandTypeEnum.SET_POSITION_COMMAND));
    }
    
    /**
     * Verify a command execution exception is properly handled.
     */
    @Test
    public final void testCommandExecutionException() throws 
        CommandExecutionException, ObjectConverterException, IOException, InterruptedException
    {     
        //Get the ordinal value of the command
        final CommandType.Enum commandType = CommandTypeEnumConverter
                .convertJavaEnumToProto(CommandTypeEnum.SET_POSITION_COMMAND);
        
        //Generate the proto command   
        SetPositionCommandGen.SetPositionCommand setPositionProto = 
                SetPositionCommandGen.SetPositionCommand.newBuilder()
                .setBase(BaseTypesGen.Command.getDefaultInstance())
                .build();
        //Generate the proto request data
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setCommandType(commandType).setCommand(setPositionProto.toByteString()).build();
        //Generate the namespace
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandRequest).
                setData(request.toByteString()).build();
        //Generate the TerraHarvest message
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        when(m_CommandConverterService.getResponseTypeFromCommandType(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(CommandResponseEnum.SET_POSITION_RESPONSE);
        
        //mock
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(m_AssetUuid);
        when(asset.getName()).thenReturn("Name");
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        when(m_Converter.convertToProto(Mockito.any(Object.class))).thenReturn(setPositionProto);
        Exception exception = new CommandExecutionException("Cannot Execute Command");
        when(asset.executeCommand(Mockito.any(Command.class))).thenThrow(exception);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_ResponseWrapper, timeout(5000)).queue(channel);
        verify(m_MessageFactory).createBaseErrorResponse(message, exception,
                String.format("Unable to execute command %s for asset %s", 
                        CommandTypeEnum.SET_POSITION_COMMAND, asset.getName()));
    }

    /**
     * Ensure that event is handled properly if type does not match expected types in handle message method.
     */
    @Test
    public final void testDefaultCaseInHandleMessage() throws IOException
    {
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateResponse).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

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
                is(assetMessage.getType().toString()));
        assertThat((AssetNamespace)postedEvent.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE), 
                is(assetMessage));
        assertThat(postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), 
                is(nullValue()));
        assertThat((RemoteChannel)postedEvent.getProperty(RemoteConstants.EVENT_PROP_CHANNEL), is(channel));        
    }

    /**
     * Test get status request/response message system for assets.  
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset status is returned.
     *      Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public final void testGetStatus() throws IOException, ObjectConverterException
    {
        //Build get status request message
        GetLastStatusRequestData request = GetLastStatusRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).build();    
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetLastStatusRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        // mock necessary objects/actions
        Asset asset = mock(Asset.class);
        Observation jaxbStatus = mock(Observation.class);
        when(asset.getLastStatus()).thenReturn(jaxbStatus);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        //proto status
        ObservationGen.Observation stat = TerraHarvestMessageHelper.getProtoObs().toBuilder().
                setStatus(Status.newBuilder().
                        setSummaryStatus(OperatingStatus.newBuilder().
                        setDescription("asdf").                        
                        setSummary(StatusTypesGen.SummaryStatus.Enum.OFF).build()).build()).build();
        
        when(m_Converter.convertToProto(jaxbStatus)).thenReturn(stat);
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetLastStatusRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        ArgumentCaptor<GetLastStatusResponseData> messageCaptor = 
                ArgumentCaptor.forClass(GetLastStatusResponseData.class);
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), eq(AssetMessageType.GetLastStatusResponse), 
                messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetLastStatusResponseData response = messageCaptor.getValue();
        assertThat(response.getStatusObservationCase(), 
                is(GetLastStatusResponseData.StatusObservationCase.STATUSOBSERVATIONNATIVE));
        assertThat(response.getStatusObservationNative(), is(stat));
        assertThat(response.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)));
    }

    /**
     * Test get status request results in a failure.
     * Verify error message response.
     */
    @Test
    public final void testGetStatus_ConverterException() throws IOException, ObjectConverterException
    {
        //Build get status request message
        GetLastStatusRequestData request = GetLastStatusRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).build();    
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetLastStatusRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        // mock necessary objects/actions
        Asset asset = mock(Asset.class);
        Observation jaxbStatus = mock(Observation.class);
        when(asset.getLastStatus()).thenReturn(jaxbStatus);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        when(m_Converter.convertToProto(jaxbStatus)).thenThrow(new ObjectConverterException("fail"));
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetLastStatusRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //verify error message
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.CONVERTER_ERROR), 
                anyString());
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test get status request/response message system for assets.  
     * Verify response even if status is null.
     */
    @Test
    public final void testGetStatus_NullStatus() throws IOException, ObjectConverterException
    {
        //Build get status request message
        GetLastStatusRequestData request = GetLastStatusRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).build();    
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetLastStatusRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        // mock necessary objects/actions
        Asset asset = mock(Asset.class);
        when(asset.getLastStatus()).thenReturn(null);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        // verify the event contains the data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetLastStatusRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        ArgumentCaptor<GetLastStatusResponseData> messageCaptor = 
                ArgumentCaptor.forClass(GetLastStatusResponseData.class);
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), eq(AssetMessageType.GetLastStatusResponse), 
                messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        verify (m_Converter, never()).convertToProto(Mockito.any(Observation.class));
        
        //verify message data
        GetLastStatusResponseData response = messageCaptor.getValue();
        assertThat(response.getStatusObservationCase(), 
                is(GetLastStatusResponseData.StatusObservationCase.STATUSOBSERVATION_NOT_SET));
        assertThat(response.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)));
    }

    /**
     * Test perform built-in-test request/response message system for assets.  
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset performs BIT upon request.
     *      Verify response (containing correct data) is sent after completing request. 
     * 
     */
    @Test
    public final void testPerformBit() throws Exception
    {
        //build request
        PerformBitRequestData request = PerformBitRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.PerformBitRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        Observation jaxbStatus = mock(Observation.class);
        when(asset.performBit()).thenReturn(jaxbStatus);

        //make a proto status observation to be returned from converter
        ObservationGen.Observation stat = TerraHarvestMessageHelper.getProtoObs().toBuilder().
                setStatus(Status.newBuilder().
                        setSummaryStatus(OperatingStatus.newBuilder().
                        setDescription("asdf").
                        setSummary(StatusTypesGen.SummaryStatus.Enum.OFF).build()).build()).build();
        
        when(m_Converter.convertToProto(jaxbStatus)).thenReturn(stat);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((PerformBitRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<PerformBitResponseData> messageCaptor = ArgumentCaptor.forClass(PerformBitResponseData.class);  
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.PerformBitResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        PerformBitResponseData response = messageCaptor.getValue();       
        assertThat(response.getStatusObservationCase(), is(StatusObservationCase.STATUSOBSERVATIONNATIVE));
        assertThat(response.getStatusObservationNative(), is(stat));
        assertThat(response.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)));
        verify(asset).performBit();
    }

    /**
     * Test perform built-in-test request/response when there is an object converter exception.
     * Verify error message is sent. 
     */
    @Test
    public final void testPerformBit_ObjectConverterException() throws IOException, ObjectConverterException
    {
        //build request
        PerformBitRequestData request = PerformBitRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.PerformBitRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        Observation jaxbStatus = mock(Observation.class);
        when(asset.performBit()).thenReturn(jaxbStatus);

        when(m_Converter.convertToProto(jaxbStatus)).thenThrow(new ObjectConverterException("exception"));
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((PerformBitRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //verify error message
        verify(m_MessageFactory).createBaseErrorMessage(eq(message), eq(ErrorCode.CONVERTER_ERROR), 
                anyString());
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify exception is thrown if requested format is invalid.
     */
    @Test
    public final void testPerformBit_InvalidFormat() throws Exception
    {
        //build request
        PerformBitRequestData request = PerformBitRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setStatusObservationFormat(RemoteTypesGen.LexiconFormat.Enum.XML).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.PerformBitRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

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
     * Test capture data request/response message system for assets. 
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset captures data upon request.
     *      Verify response (containing correct data) is sent after completing request. 
     */
    @Test
    public final void testCaptureData_ObsUuidOnly() throws Exception
    {
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY).build();        
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        Observation obs = mock(Observation.class);

        UUID obsGenUuid = UUID.randomUUID();
        UUID obsUuid = UUID.randomUUID();
        //Observation proto message
        Message obsGen = ObservationGen.
                Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setAssetName("AssetOne").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("AssetTwo").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(obsGenUuid)).build();

        when(asset.captureData()).thenReturn(obs);
        when(obs.getUuid()).thenReturn(obsUuid);
        when(m_Converter.convertToProto(obs)).thenReturn(obsGen);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((CaptureDataRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<CaptureDataResponseData> messageCaptor = ArgumentCaptor.forClass(CaptureDataResponseData.class); 
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.CaptureDataResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        CaptureDataResponseData response = messageCaptor.getValue();
        assertThat(response.isInitialized(), is(true));
        verify(asset).captureData();
        assertThat(response.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)));
        assertThat(response.getObservationCase(), is(ObservationCase.OBSERVATIONUUID));        
        assertThat(response.getObservationUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(obsUuid)));
    }
    
    /**
     * Test capture data request/response message system for assets. 
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset captures data upon request.
     *      Verify response (containing correct data) is sent after completing request. 
     */
    @Test
    public void testCaptureData_ObsInReponse() throws Exception
    {
        //build new request with attributes set to different boolean values
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.NATIVE).build();        
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        Observation obs = mock(Observation.class);

        UUID obsGenUuid = UUID.randomUUID();
        UUID obsUuid = UUID.randomUUID();
        //Observation proto message
        Message obsGen = ObservationGen.
                Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setAssetName("AssetOne").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("AssetTwo").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(obsGenUuid)).build();

        when(asset.captureData()).thenReturn(obs);
        when(obs.getUuid()).thenReturn(obsUuid);
        when(m_Converter.convertToProto(obs)).thenReturn(obsGen);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((CaptureDataRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        ArgumentCaptor<CaptureDataResponseData> messageCaptor = ArgumentCaptor.forClass(CaptureDataResponseData.class); 
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.CaptureDataResponse), messageCaptor.capture());
        //reused channel
        verify(m_ResponseWrapper, times(1)).queue(channel);
        CaptureDataResponseData response = messageCaptor.getValue();

        assertThat(response.isInitialized(), is(true));
        verify(asset).captureData();
        assertThat(response.getObservationCase(), is(ObservationCase.OBSERVATIONNATIVE));        
        assertThat(response.getObservationNative(), is(obsGen));
    }
    
    /**
     * Verify exception thrown if observation format is unsupported.
     */
    @Test
    public final void testCaptureData_InvalidFormat() throws Exception
    {
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.XML).build();        
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        RemoteChannel channel = mock(RemoteChannel.class);

        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        try
        {
            m_SUT.handleMessage(message, payload, channel);
            fail("Expecting exception");
        }
        catch (UnsupportedOperationException e)
        {
            
        }
    }
    
    /**
     * Test and verify that asset exception is caught and error message is queued
     */
    @Test
    public final void testCaptureData_AssetException() throws Exception
    {
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY).build();        
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        RemoteChannel channel = mock(RemoteChannel.class);

        when(asset.captureData()).thenThrow(new AssetException("asset error"));
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.ASSET_ERROR, 
                "Cannot complete request. asset error");
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Test and verify that object converter exception is caught and error message is queued
     */
    @Test
    public final void testCaptureData_ObjectConverterException() throws Exception
    {   
        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        Observation obs = mock(Observation.class);
        UUID obsUuid = UUID.randomUUID();
        
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.NATIVE).build();        
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //re-mock necessary objects/actions
        asset = mock(Asset.class);
        obs = mock(Observation.class);
        obsUuid = UUID.randomUUID();

        when(asset.captureData()).thenReturn(obs);
        when(obs.getUuid()).thenReturn(obsUuid);
        when(m_Converter.convertToProto(obs)).thenThrow(new ObjectConverterException("convert error 2"));
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        m_SUT.handleMessage(message, payload, channel);
        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.CONVERTER_ERROR, 
                "Cannot complete request. convert error 2");
        //reused channel
        verify(m_ResponseWrapper).queue(channel);
    }

    @Test
    public final void testCaptureData_SensorId() throws Exception
    {
        // build request
        CaptureDataRequestData request = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY)
                .setSensorId("sensor-id").build();        
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.CaptureDataRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        Observation obs = mock(Observation.class);

        UUID obsGenUuid = UUID.randomUUID();
        UUID obsUuid = UUID.randomUUID();
        //Observation proto message
        Message obsGen = ObservationGen.
                Observation.newBuilder().
                setSystemInTestMode(true).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setAssetName("AssetOne").
                setCreatedTimestamp(System.currentTimeMillis()).
                setAssetType("AssetTwo").
                setVersion(Version.newBuilder().setMajorNumber(1).setMinorNumber(2).build()).
                setSystemId(0).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(obsGenUuid)).
                setSensorId("sensor-id").build();

        when(asset.captureData("sensor-id")).thenReturn(obs);
        when(obs.getUuid()).thenReturn(obsUuid);
        when(m_Converter.convertToProto(obs)).thenReturn(obsGen);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);

        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((CaptureDataRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<CaptureDataResponseData> messageCaptor = ArgumentCaptor.forClass(CaptureDataResponseData.class); 
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.CaptureDataResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);

        CaptureDataResponseData response = messageCaptor.getValue();
        assertThat(response.isInitialized(), is(true));
        verify(asset, never()).captureData();
        verify(asset).captureData("sensor-id");
        assertThat(response.getAssetUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)));
        assertThat(response.getObservationCase(), is(ObservationCase.OBSERVATIONUUID));        
        assertThat(response.getObservationUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(obsUuid)));
        assertThat(response.getSensorId(), is("sensor-id"));
    }

    /**
     * Verify that a property request is properly handled
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testSetPropertyRequest() throws IOException, 
        IllegalArgumentException, IllegalStateException, FactoryException
    {
        UUID uuid = UUID.randomUUID();
        // build request
        Multitype multi = Multitype.newBuilder().setStringValue("valueA").setType(Type.STRING).build();
        SimpleTypesMapEntry type = SimpleTypesMapEntry.newBuilder().setKey("AKey").setValue(multi).build();
        SetPropertyRequestData request = SetPropertyRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).addProperties(type).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.SetPropertyRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        //mock necessary objects/actions
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(uuid);
        
        Map<String, Object> propsToSet = new HashMap<>();
        propsToSet.put("AKey", "");
        
        when(asset.getProperties()).thenReturn(propsToSet);
        
        when(m_AssetDirectoryService.getAssetByUuid(Mockito.any(UUID.class))).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_MessageFactory, times(1)).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.SetPropertyResponse), eq((Message)null));
        
        ArgumentCaptor<Map> dictCaptor = ArgumentCaptor.forClass(Map.class);
        verify(asset, times(1)).setProperties(dictCaptor.capture());
                
        Map<String, Object> capDict = dictCaptor.getValue();
        assertThat(capDict.size(), is(1));
        
        assertThat((String)capDict.get("AKey"), is("valueA"));
    }
   
    /**
     * Test executeCommand message system.
     * Specifically, the following behaviors are tested: 
     *      Verify incoming command request message is posted to event admin.
     *      Verify command response is generated 
     *      Verify Remote Message Service is started
     *      Verify correct command response is sent to the requester
     */
    @Test
    public final void testExecuteCommandMessages() throws Exception
    {        
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(m_AssetUuid);
        
        //Generate the proto request data
        ExecuteCommandRequestData request = ExecuteCommandRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setCommandType(CommandType.Enum.CAPTURE_IMAGE_COMMAND).
                setCommand(ByteString.copyFromUtf8("command")).build();
        //Generate the namespace
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandRequest).
                setData(request.toByteString()).build();
        //Generate the TerraHarvest message
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        Command javaCommand = new Command() 
        {
            private static final long serialVersionUID = 1L;
        };
        
        Response javaResponse = new Response() 
        {
            private static final long serialVersionUID = 1L;
        };
        
        when(m_CommandConverterService.getJavaCommandType(Mockito.any(byte[].class), 
                eq(CommandTypeEnum.CAPTURE_IMAGE_COMMAND))).thenReturn(javaCommand);
        when(m_CommandConverterService.getResponseTypeFromCommandType(CommandTypeEnum.CAPTURE_IMAGE_COMMAND))
            .thenReturn(CommandResponseEnum.CAPTURE_IMAGE_RESPONSE);
        
        when(m_Converter.convertToProto(Mockito.any(Object.class)))
            .thenReturn(CaptureImageResponseGen.CaptureImageResponse.getDefaultInstance());
        when(asset.executeCommand(javaCommand)).thenReturn(javaResponse);
        when(m_AssetDirectoryService.getAssetByUuid(m_AssetUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        verify(m_ResponseWrapper, timeout(3000)).queue(channel);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        ArgumentCaptor<ExecuteCommandResponseData> responseCaptor = 
                ArgumentCaptor.forClass(ExecuteCommandResponseData.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());
        //Test the remote message service
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.ExecuteCommandResponse), responseCaptor.capture());
        
        Event postedEvent = eventCaptor.getValue();
        //Verify the received message
        assertThat((TerraHarvestMessage)postedEvent.getProperty(RemoteConstants.EVENT_PROP_MESSAGE), is(message));
        //Verify the Response type
        ExecuteCommandResponseData response = responseCaptor.getValue();
        assertThat(response.getResponseType(), is(CommandResponseEnumConverter
                .convertJavaEnumToProto(CommandResponseEnum.CAPTURE_IMAGE_RESPONSE)));
        assertThat(SharedMessageUtils.convertProtoUUIDtoUUID(response.getUuid()), is(m_AssetUuid));
    }
    
    /**
     * Verify the execute command response message sends an event with null data message.
     */
    @Test
    public void testExecuteCommandResponse() throws IOException
    {
        ExecuteCommandResponseData response = ExecuteCommandResponseData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid)).
                setResponseType(CommandResponseEnumConverter
                        .convertJavaEnumToProto(CommandResponseEnum.SET_PAN_TILT_RESPONSE)).
                setResponse(SetPanTiltResponseGen.SetPanTiltResponse.getDefaultInstance().toByteString()).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ExecuteCommandResponse).
                setData(response.toByteString()).
                build();
        
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);
        
        // replay
        m_SUT.handleMessage(message, payload, channel);
        
        // verify the event contains the null data message
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        Event event = eventCaptor.getValue();
        assertThat(event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is((Object)response));
    }

    /**
     * Verify get status response message when handled will set the data event property.
     */
    @Test
    public final void testGetStatusResponse() throws IOException
    {
        UUID uuid = UUID.randomUUID();
        ObservationGen.Observation stat = TerraHarvestMessageHelper.getProtoObs().toBuilder().
                setStatus(Status.newBuilder().
                        setSummaryStatus(OperatingStatus.newBuilder().
                        setDescription("asdf").
                        setSummary(StatusTypesGen.SummaryStatus.Enum.OFF).build()).build()).build();
        Message response = GetLastStatusResponseData.newBuilder().setStatusObservationNative(stat)
                .setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetLastStatusResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }
    
    /**
     * Verify set asset property response message when handled will set the data event property.
     */
    @Test
    public void testSetAssetPropertyResponse() throws IOException
    {
        Message response = null;
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.SetPropertyResponse).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }

    /**
     * Verify perform BIT response message when handled will set the data event property.
     */
    @Test
    public final void testPerformBitResponse() throws IOException
    {
        UUID uuid = UUID.randomUUID();
        ObservationGen.Observation stat = TerraHarvestMessageHelper.getProtoObs().toBuilder().
                setStatus(Status.newBuilder().
                        setSummaryStatus(OperatingStatus.newBuilder().
                        setDescription("asdf").
                        setSummary(StatusTypesGen.SummaryStatus.Enum.OFF).build()).build()).build();
        Message response = PerformBitResponseData.newBuilder().setStatusObservationNative(stat).
                setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.PerformBitResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }

    /**
     * Verify capture data response message when handled will set the data event property.
     */
    @Test
    public final void testCaptureDataResponse() throws IOException
    {    
        Message response = CaptureDataResponseData.newBuilder()
                .setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(UUID.randomUUID()))
                .build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder()
                .setType(AssetMessageType.CaptureDataResponse)
                .setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }

    /**
     * Test the remove asset request/response remote message system for the asset directory service. 
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset is removed upon request.
     *      Verify response (containing correct data) is sent after completing request.
     */
    @Test
    public void testRemoveAsset() throws IOException, IllegalArgumentException, IllegalStateException
    {
        DeleteRequestData request = DeleteRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.DeleteRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        Asset asset = mock(Asset.class);
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((DeleteRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(asset).delete();
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.DeleteResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the remove asset response message sends an event with null data message.
     */
    @Test
    public void testRemoveAssetResponse() throws IOException
    {
        AssetNamespace assetDirectoryMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.DeleteResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(assetDirectoryMessage);
        TerraHarvestMessage message = createMessage(assetDirectoryMessage);
        
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
                is(AssetMessageType.DeleteResponse.toString()));
    }
    
    /**
     * Test the activate asset request/response remote message system for the asset directory service.
     * Specifically, the following behaviors are tested: 
     * Verify incoming request message is posted to event admin.
     * Verify asset is activated on request.
     * Verify response is sent after completing request to activate asset. 
     */
    @Test
    public void testActivateAsset() throws IOException
    {
        ActivateRequestData request = ActivateRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
        Asset asset = mock(Asset.class);
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((ActivateRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(asset).activateAsync();
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.ActivateResponse), Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the activate asset response message sends an event with null data message.
     */
    @Test
    public void testActivateAssetResponse() throws IOException
    {
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.ActivateResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);
        
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
                is(AssetMessageType.ActivateResponse.toString()));
    }
    
    /**
     * Test the deactivate asset request/response remote message system for the asset directory service.
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset is deactivated upon request.
     *      Verify response is sent after completing request.
     */
    @Test
    public void testDeactivateAsset() throws IOException
    {
        DeactivateRequestData request = DeactivateRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.DeactivateRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset = mock(Asset.class);
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((DeactivateRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));
        verify(asset).deactivateAsync();
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.DeactivateResponse), 
                Mockito.any(Message.class));
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Verify the deactivate asset response message sends an event with null data message.
     */
    @Test
    public void testDeactivateAssetResponse() throws IOException
    {
        AssetNamespace assetDirectoryMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.DeactivateResponse).
                build();
        
        TerraHarvestPayload payload = createPayload(assetDirectoryMessage);
        TerraHarvestMessage message = createMessage(assetDirectoryMessage);
        
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
                is(AssetMessageType.DeactivateResponse.toString()));
    }
    
    /**
     * Test the get asset status request/response remote message system for the asset directory service.
     * Specifically, the following behaviors are tested: 
     *      Verify incoming request message is posted to event admin.
     *      Verify asset status is returned upon request.
     *      Verify response (containing correct data) is sent after completing request. 
     */
    @Test
    public void testGetLastStatus() throws IOException
    {
        GetActiveStatusRequestData request = GetActiveStatusRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetActiveStatusRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);
        
        Asset asset = mock(Asset.class);
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        when(asset.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATED);
        
        // mock the channel the message came from
        RemoteChannel channel = mock(RemoteChannel.class);

        // replay
        m_SUT.handleMessage(message, payload, channel);

        //capture and verify event has been posted locally
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        assertThat((GetActiveStatusRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //capture and verify response
        ArgumentCaptor<GetActiveStatusResponseData> messageCaptor = ArgumentCaptor.
                forClass(GetActiveStatusResponseData.class); 
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.GetActiveStatusResponse), messageCaptor.capture());
        verify(m_ResponseWrapper).queue(channel);
        GetActiveStatusResponseData response = messageCaptor.getValue();
        assertThat(response.getStatus(), is(AssetMessages.AssetActiveStatus.ACTIVATED));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
        
        //Change status
        when(asset.getActiveStatus()).thenReturn(AssetActiveStatus.ACTIVATING);
        m_SUT.handleMessage(message, payload, channel);        
        messageCaptor = ArgumentCaptor.forClass(GetActiveStatusResponseData.class); 
        verify(m_MessageFactory, Mockito.times(2)).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.GetActiveStatusResponse), messageCaptor.capture());
        verify(m_ResponseWrapper, times(2)).queue(channel);
        response = messageCaptor.getValue();
        assertThat(response.getStatus(), is(AssetMessages.AssetActiveStatus.ACTIVATING));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
        
        //Change status
        when(asset.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATED);
        m_SUT.handleMessage(message, payload, channel);        
        messageCaptor = ArgumentCaptor.forClass(GetActiveStatusResponseData.class); 
        verify(m_MessageFactory, Mockito.times(3)).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.GetActiveStatusResponse), messageCaptor.capture());
        verify(m_ResponseWrapper, times(3)).queue(channel);
        response = messageCaptor.getValue();
        assertThat(response.getStatus(), is(AssetMessages.AssetActiveStatus.DEACTIVATED));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
        
        //Change status
        when(asset.getActiveStatus()).thenReturn(AssetActiveStatus.DEACTIVATING);
        m_SUT.handleMessage(message, payload, channel);        
        messageCaptor = ArgumentCaptor.forClass(GetActiveStatusResponseData.class); 
        verify(m_MessageFactory, Mockito.times(4)).createAssetResponseMessage(eq(message), 
                eq(AssetMessageType.GetActiveStatusResponse), messageCaptor.capture());
        verify(m_ResponseWrapper, times(4)).queue(channel);
        response = messageCaptor.getValue();
        assertThat(response.getStatus(), is(AssetMessages.AssetActiveStatus.DEACTIVATING));
        assertThat(response.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }
    
    /**
     * Verify exception is thrown if requested format is invalid.
     */
    @Test
    public final void testGetLastStatus_InvalidFormat() throws Exception
    {
        //build request
        PerformBitRequestData request = PerformBitRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_AssetUuid))
                .setStatusObservationFormat(RemoteTypesGen.LexiconFormat.Enum.XML).build();
        AssetNamespace assetMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetLastStatusRequest).
                setData(request.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetMessage);
        TerraHarvestMessage message = createMessage(assetMessage);

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
     * Verify create asset response message when handled will set the data event property.
     */
    @Test
    public void testGetLastStatusResponse() throws IOException
    {
        UUID uuid = UUID.randomUUID();
        Message response = GetActiveStatusResponseData.newBuilder().
                setStatus(AssetMessages.AssetActiveStatus.ACTIVATED).
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(uuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetActiveStatusResponse).
                setData(response.toByteString()).build();
        TerraHarvestPayload payload = createPayload(assetDirMessage);
        TerraHarvestMessage message = createMessage(assetDirMessage);

        RemoteChannel channel = mock(RemoteChannel.class);

        m_SUT.handleMessage(message, payload, channel);

        // verify event is posted
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event postedEvent = eventCaptor.getValue();
        assertThat((Message)postedEvent.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE), is(response));
    }

    /**
     * Test get name for asset response.
     * Verify that expected event is posted.
     */
    @Test
    public void testGetAssetNameResponse() throws IOException
    {
        GetNameResponseData response = GetNameResponseData.newBuilder().
                setAssetName("name").
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetNameResponse).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMessage(assetDirMessage);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, createPayload(assetDirMessage), channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((AssetNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE),
                is(assetDirMessage));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(AssetMessageType.GetNameResponse.toString()));
        assertThat((GetNameResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
    }
    
    /**
     * Test set name for Asset response.
     * Verify that expected event is posted.
     */
    @Test
    public void testSetAssetNameResponse() throws IOException
    {
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.SetNameResponse).build();
        TerraHarvestMessage message = createMessage(assetDirMessage);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, createPayload(assetDirMessage), channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((AssetNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE),
                is(assetDirMessage));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(AssetMessageType.SetNameResponse.toString()));
    }
    
    /**
     * Test get name for asset request.
     * Verify that expected event is posted.
     */
    @Test
    public void testGetAssetNameRequest() throws IOException, IllegalArgumentException
    {
        //mock behavior
        Asset asset = mock(Asset.class);
        when(asset.getName()).thenReturn("name");
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        
        //request
        GetNameRequestData response = GetNameRequestData.newBuilder().
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.GetNameRequest).
                setData(response.toByteString()).build();
        TerraHarvestMessage message = createMessage(assetDirMessage);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, createPayload(assetDirMessage), channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((AssetNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE),
                is(assetDirMessage));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(AssetMessageType.GetNameRequest.toString()));
        assertThat((GetNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(response));
        
        //verify
        ArgumentCaptor<GetNameResponseData> responseCap = ArgumentCaptor.forClass(GetNameResponseData.class);
        verify(m_MessageFactory).createAssetResponseMessage(eq(message), 
            eq(AssetMessageType.GetNameResponse), responseCap.capture());
        verify(m_ResponseWrapper).queue(channel);
        
        GetNameResponseData data = responseCap.getValue();
        assertThat(data.getAssetName(), is("name"));
        assertThat(data.getUuid(), is(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)));
    }

    /**
     * Test set name for Asset request.
     * Verify that expected event is posted.
     */
    @Test
    public void testSetAssetNameRequest() throws IOException, IllegalArgumentException, FactoryException
    {
        //mock behavior
        Asset asset = mock(Asset.class);
        when(asset.getName()).thenReturn("name");
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        
        SetNameRequestData request = SetNameRequestData.newBuilder().
                setAssetName("name").
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.SetNameRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMessage(assetDirMessage);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, createPayload(assetDirMessage), channel);
        
        //capture event
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event event = eventCaptor.getValue();
        
        //verify
        assertThat((AssetNamespace)event.getProperty(RemoteConstants.EVENT_PROP_NAMESPACE_MESSAGE),
                is(assetDirMessage));
        assertThat((String)event.getProperty(RemoteConstants.EVENT_PROP_MESSAGE_TYPE),
                is(AssetMessageType.SetNameRequest.toString()));
        assertThat((SetNameRequestData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE),
                is(request));

        //verify
        verify(asset).setName("name");

        verify(m_MessageFactory).createAssetResponseMessage(
                message, AssetMessageType.SetNameResponse, null);
        verify(m_ResponseWrapper).queue(channel);
    }
    
    /**
     * Test set name for Asset request where the asset isn't known.
     * Verify that the expected error message is sent.
     */
    @Test
    public void testSetAssetNameRequestIllegal() throws IOException, IllegalArgumentException
    {
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenThrow(new IllegalArgumentException("Exception"));
        
        SetNameRequestData request = SetNameRequestData.newBuilder().
                setAssetName("name").
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.SetNameRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMessage(assetDirMessage);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, createPayload(assetDirMessage), channel);

        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.INVALID_VALUE, 
                "Request to set the asset's name with UUID " + testUuid.toString()
                + " was null, overlapped with another asset's name, or the asset was not found. Exception");
        verify(m_ResponseWrapper).queue(channel);
    }

    /**
     * Test set name for Asset request where the asset's name cannot be persisted.
     * Verify that the expected error message is sent.
     */
    @Test
    public void testSetAssetNameRequestPersist() throws IOException, IllegalArgumentException, FactoryException
    {
        //mock behavior
        Asset asset = mock(Asset.class);
        when(asset.getName()).thenReturn("name");
        when(m_AssetDirectoryService.getAssetByUuid(testUuid)).thenReturn(asset);
        
        //verify
        doThrow(new AssetException("Exception")).when(asset).setName("name");
        
        SetNameRequestData request = SetNameRequestData.newBuilder().
                setAssetName("name").
                setUuid(SharedMessageUtils.convertUUIDToProtoUUID(testUuid)).build();
        AssetNamespace assetDirMessage = AssetNamespace.newBuilder().
                setType(AssetMessageType.SetNameRequest).
                setData(request.toByteString()).build();
        TerraHarvestMessage message = createMessage(assetDirMessage);

        //mock remote channel
        RemoteChannel channel = mock(RemoteChannel.class);

        //handle message
        m_SUT.handleMessage(message, createPayload(assetDirMessage), channel);

        verify(m_MessageFactory).createBaseErrorMessage(message, ErrorCode.ASSET_ERROR, 
            "Unable to set the name for asset with UUID " + testUuid.toString() + ". Exception");
        verify(m_ResponseWrapper).queue(channel);
    }
    
    private TerraHarvestMessage createMessage(AssetNamespace assetMessage)
    {
        return TerraHarvestMessageHelper.createTerraHarvestMessage(0, 1, Namespace.Asset, 3, assetMessage);
    }
    private TerraHarvestPayload createPayload(AssetNamespace assetMessage)
    {
        return TerraHarvestPayload.newBuilder().
               setNamespace(Namespace.Asset).
               setNamespaceMessage(assetMessage.toByteString()).
               build();
    }
}
