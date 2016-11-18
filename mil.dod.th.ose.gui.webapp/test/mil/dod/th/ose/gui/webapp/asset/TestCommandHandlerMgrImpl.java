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
package mil.dod.th.ose.gui.webapp.asset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.asset.commands.CaptureImageCommand;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.DetectTargetCommand;
import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace.
    AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.GetCapabilitiesResponseData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.BaseMessages.GenericErrorResponseData;
import mil.dod.th.core.remote.proto.BaseMessages.GetOperationModeReponseData;
import mil.dod.th.core.remote.proto.BaseMessages.OperationMode;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.spatial.AzimuthDegrees;
import mil.dod.th.core.types.spatial.ElevationDegrees;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.asset.CommandHandlerMgrImpl.ErrorResponseHandler;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.asset.capability.AssetCapabilitiesGen.AssetCapabilities;
import mil.dod.th.remote.lexicon.capability.BaseCapabilitiesGen.BaseCapabilities;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * @author nickmarcucci
 *
 */
public class TestCommandHandlerMgrImpl
{
    private static int CONTROLLER_ID = 5;
    
    private CommandHandlerMgrImpl m_SUT;
    private GrowlMessageUtil m_GrowlUtil;
    private CommandConverter m_CommandConverter;
    private JaxbProtoObjectConverter m_Converter;
    private MessageFactory m_MessageFactory;
    private BundleContextUtil m_BundleUtil;
    private BundleContext m_BundleContext;
    private MessageWrapper m_MessageWrapper;
    
    @SuppressWarnings("rawtypes") //TH-534:unable to parameterize at the moment
    private ServiceRegistration m_HandlerReg;
    
    @SuppressWarnings("unchecked")
    @Before
    public void init()
    {
        m_SUT = new CommandHandlerMgrImpl();
        
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_CommandConverter = mock(CommandConverter.class);
        m_Converter = mock(JaxbProtoObjectConverter.class);
        m_MessageFactory = mock(MessageFactory.class);
        m_BundleUtil = mock(BundleContextUtil.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_BundleContext = mock(BundleContext.class);
        m_HandlerReg = mock(ServiceRegistration.class);
        
        when(m_MessageFactory.createAssetMessage(Mockito.any(AssetMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //mock behavior for event listener
        when(m_BundleUtil.getBundleContext()).thenReturn(m_BundleContext);
        when(m_BundleContext.registerService(eq(EventHandler.class), Mockito.any(EventHandler.class), 
            Mockito.any(Dictionary.class))).thenReturn(m_HandlerReg);
        
        m_SUT.setCommandConverter(m_CommandConverter);
        m_SUT.setConverter(m_Converter);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setBundleContextUtil(m_BundleUtil);
        
        m_SUT.postConstruct();
        
        verify(m_BundleContext, times(1)).registerService(eq(EventHandler.class), 
                Mockito.any(EventHandler.class), Mockito.any(Dictionary.class));
        verify(m_BundleUtil, times(1)).getBundleContext();
    }
    
    @After
    public void cleanup()
    {
        m_SUT.cleanup();
        verify(m_HandlerReg).unregister();
    }
    
    /**
     * Verify that send command method without a command parameter works correctly.
     */
    @Test
    public void testSendCommandWithNoCommandInstance() throws ObjectConverterException, ClassNotFoundException, 
        InstantiationException, IllegalAccessException
    {
        UUID assetUuid = UUID.randomUUID();
        Message testMessage = mock(Message.class);
        ByteString testByteString = ByteString.copyFrom(new byte[1024]);
        
        //Mock converter behavior.
        when(m_Converter.convertToProto(Mockito.any(Command.class))).thenReturn(testMessage);
        when(testMessage.toByteString()).thenReturn(testByteString);
        
        m_SUT.sendCommand(CONTROLLER_ID, assetUuid, CommandTypeEnum.GET_VERSION_COMMAND);
        
        //Capture messages being sent out by remote message sender.
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        //Verify four messages were sent by the remote message sender, one for each command type.
        verify(m_MessageFactory).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper).queue(eq(CONTROLLER_ID), Mockito.any(ResponseHandler.class));
        
        ExecuteCommandRequestData msg = (ExecuteCommandRequestData)messageCaptor.getValue();
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.GET_VERSION_COMMAND));
    }
    
    /**
     * Test the send command method.
     * Verify that the appropriate remote message is sent for a command.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSendCommandWithCommandInstance() throws ObjectConverterException
    {
        UUID assetUuid = UUID.randomUUID();
        
        Message testMessage = mock(Message.class);
        ByteString testByteString = ByteString.copyFrom(new byte[1024]);
        SetCameraSettingsCommand cameraSettingsCmnd = new SetCameraSettingsCommand(null, null, 0.5f, 0.5f, 
                null, null, null, null, null);
        
        AzimuthDegrees pan = new AzimuthDegrees().withValue(0.2);
        ElevationDegrees tilt = new ElevationDegrees().withValue(0.2);
        OrientationOffset panTilt = new OrientationOffset(pan, tilt, null);
        SetPanTiltCommand panTiltCmd = new SetPanTiltCommand(null, panTilt);
        DetectTargetCommand detectTargetCmnd = new DetectTargetCommand(null, null, null, null, null, null, 5, 5, 6,
                null);
        CaptureImageCommand captureImageCmd = new CaptureImageCommand(null, null, null, null, null, null);
        
        //Mock converter behavior.
        when(m_Converter.convertToProto(Mockito.any(Command.class))).thenReturn(testMessage);
        when(testMessage.toByteString()).thenReturn(testByteString);
        
        //Test sending the four known command types.
        m_SUT.sendCommand(CONTROLLER_ID, assetUuid, cameraSettingsCmnd, CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND);
        m_SUT.sendCommand(CONTROLLER_ID, assetUuid, panTiltCmd, CommandTypeEnum.SET_PAN_TILT_COMMAND);
        m_SUT.sendCommand(CONTROLLER_ID, assetUuid, detectTargetCmnd, CommandTypeEnum.DETECT_TARGET_COMMAND);
        m_SUT.sendCommand(CONTROLLER_ID, assetUuid, captureImageCmd, CommandTypeEnum.CAPTURE_IMAGE_COMMAND);
        
        //Capture messages being sent out by remote message sender.
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        //Verify four messages were sent by the remote message sender, one for each command type.
        verify(m_MessageFactory, times(4)).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper, times(4)).queue(eq(CONTROLLER_ID), Mockito.any(ResponseHandler.class));
        
        ExecuteCommandRequestData msg = (ExecuteCommandRequestData)messageCaptor.getAllValues().get(0);
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.SET_CAMERA_SETTINGS_COMMAND));
        msg = (ExecuteCommandRequestData)messageCaptor.getAllValues().get(1);
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.SET_PAN_TILT_COMMAND));
        msg = (ExecuteCommandRequestData)messageCaptor.getAllValues().get(2);
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.DETECT_TARGET_COMMAND));
        msg = (ExecuteCommandRequestData)messageCaptor.getAllValues().get(3);
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.CAPTURE_IMAGE_COMMAND));
        
        //Test exception being thrown when converting command to a protocol message.
        when(m_Converter.convertToProto(Mockito.any(Command.class))).thenThrow(ObjectConverterException.class);
        m_SUT.sendCommand(CONTROLLER_ID, assetUuid, panTiltCmd, CommandTypeEnum.SET_PAN_TILT_COMMAND);
        
        verify(m_GrowlUtil).createLocalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), eq("Asset Command Error:"), 
                eq("Unable to send " + panTiltCmd.getClass().getSimpleName().toString() + " command. See server "
                        + "log for futher details."), Mockito.any(ObjectConverterException.class));
    }
    
    /**
     * Test the send command message when the command type is null.
     * Verify that no message is sent since the command type is null.
     */
    @Test
    public void testSendCommandWithNullType() throws ObjectConverterException
    {
        UUID assetUuid = UUID.randomUUID();
        
        Message testMessage = mock(Message.class);
        ByteString testByteString = ByteString.copyFrom(new byte[1024]);
        
        AzimuthDegrees pan = new AzimuthDegrees().withValue(0.2);
        ElevationDegrees tilt = new ElevationDegrees().withValue(0.2);
        OrientationOffset panTilt = new OrientationOffset(pan, tilt, null);
        SetPanTiltCommand panTiltCmd = new SetPanTiltCommand(null, panTilt);
        
        //Mock converter behavior.
        when(m_Converter.convertToProto(Mockito.any(Command.class))).thenReturn(testMessage);
        when(testMessage.toByteString()).thenReturn(testByteString);
        
        try 
        {
            m_SUT.sendCommand(CONTROLLER_ID, assetUuid, panTiltCmd, null);
        }
        catch (IllegalArgumentException exception)
        {
            assertThat(exception.getMessage(), is(String.format("Cannot send %s command to system 0x%08x " 
                    + "because command type is null", panTiltCmd.getClass().getSimpleName(), CONTROLLER_ID)));
        }
        
        //Verify no messages are ever sent.
        verify(m_MessageFactory, never()).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                Mockito.any(Message.class));
        verify(m_MessageWrapper, never()).queue(eq(CONTROLLER_ID), Mockito.any(ResponseHandler.class));
    }
    
    /**
     * Test the command event handler.
     * Verify that a responses for each of the four command types is handled appropriately.
     */
    @Test
    public void testCommandEventHandler()
    {
        String summary = "Asset Command Executed:";
        UUID assetUuid = UUID.randomUUID();
        mil.dod.th.core.remote.proto.SharedMessages.UUID assetUuidProto = 
                SharedMessageUtils.convertUUIDToProtoUUID(assetUuid);
        
        ExecuteCommandResponseData testResponse = ExecuteCommandResponseData.newBuilder().setUuid(
            assetUuidProto).setResponseType(CommandTypesGen.CommandResponse.Enum.SET_PAN_TILT_RESPONSE).setResponse(
                        ByteString.copyFrom(new byte[] {0 ,1 ,2})).build();
        
        when(m_CommandConverter.getCommandTypeFromResponseType(CommandResponseEnum.SET_PAN_TILT_RESPONSE))
            .thenReturn(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, testResponse);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        Event testEvent = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        
        //Test set pan tilt response.
        m_SUT.handleEvent(testEvent);
        
        //Verify growl message was posted and that its content is correct.
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, summary, 
                "SetPanTiltCommand executed for asset with uuid: " + assetUuid);
        
        //Test set camera settings response.
        testResponse = testResponse.toBuilder().setResponseType(
                CommandTypesGen.CommandResponse.Enum.SET_CAMERA_SETTINGS_RESPONSE).build();
        when(m_CommandConverter.getCommandTypeFromResponseType(CommandResponseEnum.SET_CAMERA_SETTINGS_RESPONSE))
            .thenReturn(CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, testResponse);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        testEvent = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        m_SUT.handleEvent(testEvent);
        
        //Verify growl message was posted and that its content is correct.
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, summary, 
                "SetCameraSettingsCommand executed for asset with uuid: " + assetUuid);
        
        //Test capture image command.
        testResponse = testResponse.toBuilder().setResponseType(
                CommandTypesGen.CommandResponse.Enum.CAPTURE_IMAGE_RESPONSE).build();
        when(m_CommandConverter.getCommandTypeFromResponseType(CommandResponseEnum.CAPTURE_IMAGE_RESPONSE))
            .thenReturn(CommandTypeEnum.CAPTURE_IMAGE_COMMAND);
        
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, testResponse);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        testEvent = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        m_SUT.handleEvent(testEvent);
        
        //Verify growl message was posted and that its content is correct.
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, summary, 
                "CaptureImageCommand executed for asset with uuid: " + assetUuid);

        //Test detect target command.
        testResponse = testResponse.toBuilder().setResponseType(
                CommandTypesGen.CommandResponse.Enum.DETECT_TARGET_RESPONSE).build();
        when(m_CommandConverter.getCommandTypeFromResponseType(CommandResponseEnum.DETECT_TARGET_RESPONSE))
            .thenReturn(CommandTypeEnum.DETECT_TARGET_COMMAND);
        props.put(RemoteConstants.EVENT_PROP_DATA_MESSAGE, testResponse);
        props.put(RemoteConstants.EVENT_PROP_SOURCE_ID, CONTROLLER_ID);
        testEvent = new Event(RemoteConstants.TOPIC_MESSAGE_RECEIVED, props);
        m_SUT.handleEvent(testEvent);
        
        //Verify growl message was posted and that its content is correct.
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_INFO, summary, 
                "DetectTargetCommand executed for asset with uuid: " + assetUuid);
    }
    
    /**
     * Test the error response handler.
     * Verify the error response is handled appropriately.
     */
    @Test
    public void testErrorResponseHandler() throws InvalidProtocolBufferException
    {
        //Instantiate response handler.
        ErrorResponseHandler errorHandler = m_SUT.new ErrorResponseHandler();
        
        //String that represents the error message to be displayed by growl.
        String errorStr = "Test Error!";
        
        GenericErrorResponseData dataMessage = GenericErrorResponseData.newBuilder().setError(
                ErrorCode.ASSET_ERROR).setErrorDescription(errorStr).build();
        BaseNamespace namespaceMessage = BaseNamespace.newBuilder().setType(
                BaseMessageType.GenericErrorResponse).setData(dataMessage.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, 
                Namespace.Base, 5000, namespaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        //Test generic error response handler.
        errorHandler.handleResponse(thMessage, payload, namespaceMessage, dataMessage);
        
        //Verify that a growl message is displayed and that its contents are correct.
        verify(m_GrowlUtil).createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "Could not execute command:", 
                ErrorCode.ASSET_ERROR.toString() + ", " + errorStr);
    }
    
    /**
     * Verify that error response handler doesn't do anything if not a generic error response message.
     */
    @Test
    public void testErrorResponseHandlerNotGenericErrorResponse() throws InvalidProtocolBufferException
    {
        //Instantiate response handler.
        ErrorResponseHandler errorHandler = m_SUT.new ErrorResponseHandler();
        
        GetOperationModeReponseData dataMessage = GetOperationModeReponseData.newBuilder()
                .setMode(OperationMode.OPERATIONAL_MODE).build();
        BaseNamespace namespaceMessage = BaseNamespace.newBuilder()
                .setType(BaseMessageType.GetOperationModeResponse).setData(dataMessage.toByteString()).build();
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, 
                Namespace.Base, 5000, namespaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        //Test generic error response handler.
        errorHandler.handleResponse(thMessage, payload, namespaceMessage, dataMessage);
        
        //Verify that a growl message is displayed and that its contents are correct.
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Could not execute command:"), Mockito.anyString());
    }
        
    /**
     * Verify that error response handler doesn't do anything if not a base namespace message.
     */
    @Test
    public void testErrorResponseHandlerNotBaseNamespace() throws InvalidProtocolBufferException
    {
        //Instantiate response handler.
        ErrorResponseHandler errorHandler = m_SUT.new ErrorResponseHandler();
        BaseCapabilities baseCapabilities = BaseCapabilities.newBuilder().setDescription("Blah")
                .setProductName("Some Product").build();
        AssetCapabilities assetCapabilities = AssetCapabilities.newBuilder().setBase(baseCapabilities).build();
        GetCapabilitiesResponseData dataMessage = GetCapabilitiesResponseData.newBuilder()
                .setCapabilities(assetCapabilities).setProductType("Some Product").build();
        AssetDirectoryServiceNamespace namespaceMessage = AssetDirectoryServiceNamespace.newBuilder()
                .setType(AssetDirectoryServiceMessageType.GetCapabilitiesResponse)
                .setData(dataMessage.toByteString()).build();
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, 
                Namespace.Asset, 5000, namespaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        //Test generic error response handler.
        errorHandler.handleResponse(thMessage, payload, namespaceMessage, dataMessage);
        
        //Verify that a growl message is displayed and that its contents are correct.
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), 
                eq("Could not execute command:"), Mockito.anyString());
    }
}
