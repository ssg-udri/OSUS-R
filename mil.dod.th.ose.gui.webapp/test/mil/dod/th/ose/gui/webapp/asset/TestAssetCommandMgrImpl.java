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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.GetCameraSettingsResponse;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.messaging.MessageWrapper;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.image.ExposureModeEnum;
import mil.dod.th.core.types.image.ExposureSettings;
import mil.dod.th.core.types.image.WhiteBalanceEnum;
import mil.dod.th.core.types.spatial.OrientationOffset;
import mil.dod.th.ose.gui.webapp.TerraHarvestMessageHelper;
import mil.dod.th.ose.gui.webapp.asset.AssetCommandMgrImpl.SyncCommandResponseHandler;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen.Response;
import mil.dod.th.remote.lexicon.asset.commands.GetCameraSettingsResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPanTiltResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPointingLocationResponseGen;
import mil.dod.th.remote.lexicon.types.command.CommandTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HaeMeters;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.EventAdmin;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Test class for the {@link AssetCommandMgrImpl} class.
 * 
 * @author cweisenborn
 */
public class TestAssetCommandMgrImpl
{
    private static int CONTROLLER_ID = 5;
    
    private static final String PROTO_COMMAND_PACKAGE = "mil.dod.th.remote.lexicon.asset.commands.";
    
    private AssetCommandMgrImpl m_SUT;
    private MessageFactory m_MessageFactory;
    private EventAdmin m_EventAdmin;
    private GrowlMessageUtil m_GrowlUtil;
    private MessageWrapper m_MessageWrapper;
    private AssetTypesMgr m_AssetTypesMgr;
    private CommandCapabilities m_Capabilities;
    private AssetModel m_AssetModel;
    private CommandConverter m_CommandConverter;
    private AssetFactoryModel m_AssetFactory;
    private AssetCapabilities m_AssetCaps;
    
    @Before
    public void setup()
    {
        m_MessageFactory = mock(MessageFactory.class);
        m_CommandConverter = mock(CommandConverter.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_GrowlUtil = mock(GrowlMessageUtil.class);
        m_MessageWrapper = mock(MessageWrapper.class);
        m_AssetTypesMgr = mock(AssetTypesMgr.class);
        m_Capabilities = mock(CommandCapabilities.class);
        m_AssetModel = mock(AssetModel.class);
        m_AssetFactory = mock(AssetFactoryModel.class);
        m_AssetCaps = mock(AssetCapabilities.class);
        
        m_SUT = new AssetCommandMgrImpl();
        
        m_SUT.setMessageFactory(m_MessageFactory);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setGrowlMessageUtil(m_GrowlUtil);
        m_SUT.setAssetTypesMgr(m_AssetTypesMgr);
        m_SUT.setCommandConverter(m_CommandConverter);
        
        when(m_MessageFactory.createAssetMessage(Mockito.any(AssetMessageType.class), 
                Mockito.any(Message.class))).thenReturn(m_MessageWrapper);
        
        //Mock command caps.
        List<CommandTypeEnum> commandTypes = new ArrayList<CommandTypeEnum>();
        for (CommandTypeEnum type: CommandTypeEnum.values())
        {
            if (!type.equals(CommandTypeEnum.SET_POINTING_LOCATION_COMMAND) 
                    && !type.equals(CommandTypeEnum.GET_POINTING_LOCATION_COMMAND))
            {
                commandTypes.add(type);
            }
        }
        when(m_Capabilities.getSupportedCommands()).thenReturn(commandTypes);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(
                eq(CONTROLLER_ID), Mockito.anyString())).thenReturn(m_AssetFactory);
        when(m_AssetFactory.getFactoryCaps()).thenReturn(m_AssetCaps);
        when(m_AssetCaps.getCommandCapabilities()).thenReturn(m_Capabilities);
        
        m_SUT.postConstruct();
    }
    
    /**
     * Test the get asset commands method.
     * Verify that the appropriate asset command models are returned.
     */
    @Test
    public void testGetAssetCommands() throws SecurityException, IllegalArgumentException
    {
        UUID assetUuid1 = UUID.randomUUID();
        AssetModel model1 = mock(AssetModel.class);
        when(model1.getControllerId()).thenReturn(CONTROLLER_ID);
        when(model1.getUuid()).thenReturn(assetUuid1);  
        
        AssetSyncableCommandModel commandModel1 = m_SUT.getAssetCommands(model1);
        assertThat(commandModel1.getUuid(), is(assetUuid1));
        SetPanTiltCommand panTilt = 
                (SetPanTiltCommand)commandModel1.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);

        panTilt.setPanTilt(SpatialTypesFactory.newOrientationOffset(5.5, 2.11));
        
        commandModel1 = m_SUT.getAssetCommands(model1);
        assertThat(commandModel1.getUuid(), is(assetUuid1));
        panTilt = (SetPanTiltCommand)commandModel1.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        OrientationOffset ptOffset = panTilt.getPanTilt();
        assertThat(ptOffset.getAzimuth().getValue(), is(5.5));
        assertThat(ptOffset.getElevation().getValue(), is(2.11));
        
        UUID assetUuid2 = UUID.randomUUID();
        AssetModel model2 = mock(AssetModel.class);
        when(model2.getControllerId()).thenReturn(CONTROLLER_ID);
        when(model2.getUuid()).thenReturn(assetUuid2);
        
        AssetSyncableCommandModel commandModel2 = m_SUT.getAssetCommands(model2);
        assertThat(commandModel2.getUuid(), is(assetUuid2));
        
        //Mock command caps.
        List<CommandTypeEnum> commandTypes = new ArrayList<CommandTypeEnum>();
        for (CommandTypeEnum type: CommandTypeEnum.values())
        {
            if (!type.equals(CommandTypeEnum.SET_POINTING_LOCATION_COMMAND) 
                    && !type.equals(CommandTypeEnum.GET_POINTING_LOCATION_COMMAND)
                    && !type.equals(CommandTypeEnum.SET_PAN_TILT_COMMAND)
                    && !type.equals(CommandTypeEnum.GET_PAN_TILT_COMMAND))
            {
                commandTypes.add(type);
            }
        }
        m_Capabilities = mock(CommandCapabilities.class);
        when(m_Capabilities.getSupportedCommands()).thenReturn(commandTypes);
        when(m_AssetTypesMgr.getAssetFactoryForClassAsync(
                eq(CONTROLLER_ID), Mockito.anyString())).thenReturn(m_AssetFactory);
        when(m_AssetFactory.getFactoryCaps()).thenReturn(m_AssetCaps);
        when(m_AssetCaps.getCommandCapabilities()).thenReturn(m_Capabilities);
        
        commandModel1 = m_SUT.getAssetCommands(model1);
        assertThat(commandModel1.getUuid(), is(assetUuid1));
        panTilt = (SetPanTiltCommand)commandModel1.getCommandByType(CommandTypeEnum.SET_PAN_TILT_COMMAND);
        assertThat(panTilt, is(nullValue()));
    }
    
    /**
     * Test the sync call method.
     * Verify that the appropriate remote messages are sent.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testSyncCall() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        //SYNCABLE COMMAND - Those which have set/get pairs
        Class smodeClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetModeCommandGen$SetModeCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_MODE_COMMAND))
            .thenReturn(smodeClazz);
        
        Class gmodeClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetModeCommandGen$GetModeCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_MODE_COMMAND))
            .thenReturn(gmodeClazz);
        
        Class scamClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetCameraSettingsCommandGen$SetCameraSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND))
            .thenReturn(scamClazz);
        
        Class camClazz = Class.forName(PROTO_COMMAND_PACKAGE 
                + "GetCameraSettingsCommandGen$GetCameraSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_CAMERA_SETTINGS_COMMAND))
            .thenReturn(camClazz);

        Class sptClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetPanTiltCommandGen$SetPanTiltCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_PAN_TILT_COMMAND))
            .thenReturn(sptClazz);
        
        Class ptClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetPanTiltCommandGen$GetPanTiltCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_PAN_TILT_COMMAND))
            .thenReturn(ptClazz);
        
        //NON-SYNCABLE commands
        Class capClazz = Class.forName(PROTO_COMMAND_PACKAGE + "CaptureImageCommandGen$CaptureImageCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.CAPTURE_IMAGE_COMMAND))
            .thenReturn(capClazz);
        
        Class tgtClazz = Class.forName(PROTO_COMMAND_PACKAGE + "DetectTargetCommandGen$DetectTargetCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.DETECT_TARGET_COMMAND))
            .thenReturn(tgtClazz);

        Class posClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetPositionCommandGen$GetPositionCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_POSITION_COMMAND))
            .thenReturn(posClazz);
        
        Class tuneClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetTuneSettingsCommandGen$GetTuneSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_TUNE_SETTINGS_COMMAND))
            .thenReturn(tuneClazz);
        
        Class verClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetVersionCommandGen$GetVersionCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_VERSION_COMMAND))
            .thenReturn(verClazz);

        Class sposClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetPositionCommandGen$SetPositionCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(sposClazz);
        
        Class stuneClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetTuneSettingsCommandGen$SetTuneSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND))
            .thenReturn(stuneClazz);
        
        CommandCapabilities cmndCaps = mock(CommandCapabilities.class);
        when(cmndCaps.isSetCameraSettings()).thenReturn(true);
        when(cmndCaps.isSetPanTilt()).thenReturn(true);
        
        UUID assetUuid = UUID.randomUUID();
        AssetSyncableCommandModel model = new AssetSyncableCommandModel(assetUuid, m_Capabilities);
        m_SUT.syncCall(CONTROLLER_ID, model);
        
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        //Should send message for the number of Get/Set pairs exist in the syncable commands denoted above
        verify(m_MessageFactory, times(3)).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper, times(3)).queue(eq(CONTROLLER_ID), Mockito.any(ResponseHandler.class));
        
        List<CommandTypesGen.CommandType.Enum> commandEnums = new ArrayList<>();
        for (Message message : messageCaptor.getAllValues())
        {
            commandEnums.add(((ExecuteCommandRequestData)message).getCommandType());
        }
        
        assertThat(commandEnums, containsInAnyOrder(CommandTypesGen.CommandType.Enum.GET_CAMERA_SETTINGS_COMMAND,
                CommandTypesGen.CommandType.Enum.GET_PAN_TILT_COMMAND,
                CommandTypesGen.CommandType.Enum.GET_MODE_COMMAND));        
    }
    
    /**
     * Test the do sync method.
     * Verify that the method sends the appropriate remote messages for each of the command types that can be synced.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testDoSync() throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        Class capClazz = Class.forName(PROTO_COMMAND_PACKAGE + "CaptureImageCommandGen$CaptureImageCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.CAPTURE_IMAGE_COMMAND))
            .thenReturn(capClazz);
        
        Class tgtClazz = Class.forName(PROTO_COMMAND_PACKAGE + "DetectTargetCommandGen$DetectTargetCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.DETECT_TARGET_COMMAND))
            .thenReturn(tgtClazz);
        
        Class camClazz = Class.forName(PROTO_COMMAND_PACKAGE 
                + "GetCameraSettingsCommandGen$GetCameraSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_CAMERA_SETTINGS_COMMAND))
            .thenReturn(camClazz);
        
        Class ptClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetPanTiltCommandGen$GetPanTiltCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_PAN_TILT_COMMAND))
            .thenReturn(ptClazz);
        
        Class posClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetPositionCommandGen$GetPositionCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_POSITION_COMMAND))
            .thenReturn(posClazz);
        
        Class tuneClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetTuneSettingsCommandGen$GetTuneSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_TUNE_SETTINGS_COMMAND))
            .thenReturn(tuneClazz);
        
        Class verClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetVersionCommandGen$GetVersionCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.GET_VERSION_COMMAND))
            .thenReturn(verClazz);
        
        Class scamClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetCameraSettingsCommandGen$SetCameraSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND))
            .thenReturn(scamClazz);
        
        Class sptClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetPanTiltCommandGen$SetPanTiltCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_PAN_TILT_COMMAND))
            .thenReturn(sptClazz);
        
        Class sposClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetPositionCommandGen$SetPositionCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_POSITION_COMMAND))
            .thenReturn(sposClazz);
        
        Class stuneClazz = Class.forName(PROTO_COMMAND_PACKAGE + "SetTuneSettingsCommandGen$SetTuneSettingsCommand");
        when(m_CommandConverter.retrieveProtoClass(CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND))
            .thenReturn(stuneClazz);
        
        UUID assetUuid = UUID.randomUUID();
        
        //Test do sync for camera settings command.
        AssetSyncableCommandModel commandModel = new AssetSyncableCommandModel(assetUuid, m_Capabilities);
        m_SUT.doSync(CONTROLLER_ID, commandModel, CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND);
        
        //Test do sync for pan/tilt command.
        m_SUT.doSync(CONTROLLER_ID, commandModel, CommandTypeEnum.SET_PAN_TILT_COMMAND);
        
        //Capture messages being sent out by remote message sender.
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        
        //Verify two messages were sent out and that the information contained by the messages are appropriate.
        verify(m_MessageFactory, times(2)).createAssetMessage(eq(AssetMessageType.ExecuteCommandRequest), 
                messageCaptor.capture());
        verify(m_MessageWrapper, times(2)).queue(eq(CONTROLLER_ID), Mockito.any(ResponseHandler.class));
        
        ExecuteCommandRequestData msg = (ExecuteCommandRequestData)messageCaptor.getAllValues().get(0);
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.GET_CAMERA_SETTINGS_COMMAND));
        msg = (ExecuteCommandRequestData)messageCaptor.getAllValues().get(1);
        assertThat(msg.getCommandType(), is(CommandTypesGen.CommandType.Enum.GET_PAN_TILT_COMMAND));
    }
    
    /**
     * Verify that trying to sync a command that does not support syncing results in an illegal argument exception.
     */
    @Test
    public void testDoSyncUnsupportedCommand() throws ClassNotFoundException, InstantiationException, 
        IllegalAccessException
    {
        UUID assetUuid = UUID.randomUUID();
        AssetSyncableCommandModel commandModel = new AssetSyncableCommandModel(assetUuid, m_Capabilities);
        try
        {
            m_SUT.doSync(CONTROLLER_ID, commandModel, CommandTypeEnum.CAPTURE_IMAGE_COMMAND);
            fail("Illegal argument exception was expected since the specified command does not support syncing.");
        }
        catch (final IllegalArgumentException exception)
        {
            //expected exception
        }
    }
    
    /**
     * Test the command response handler.
     * Verify that the response handler handles both get camera setting responses  get pan/tilt setting responses
     * appropriately.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testCommandResponseHandler() throws ObjectConverterException, InvalidProtocolBufferException, 
        ClassNotFoundException
    {
        SyncCommandResponseHandler commandResponseHandler = m_SUT.new SyncCommandResponseHandler();
        
        UUID assetUuid = UUID.randomUUID();
        mil.dod.th.core.remote.proto.SharedMessages.UUID assetUuidProto = 
                SharedMessageUtils.convertUUIDToProtoUUID(assetUuid);
        
        Response baseResponse = Response.newBuilder().build();
        GetCameraSettingsResponseGen.GetCameraSettingsResponse camSettingsCommand = 
                GetCameraSettingsResponseGen.GetCameraSettingsResponse.newBuilder()
                .setBase(baseResponse)
                .build();
        ExecuteCommandResponseData dataMsg = ExecuteCommandResponseData.newBuilder()
                .setUuid(assetUuidProto)
                .setResponseType(CommandTypesGen.CommandResponse.Enum.GET_CAMERA_SETTINGS_RESPONSE)
                .setResponse(camSettingsCommand.toByteString())
                .build();
        AssetNamespace namespaceMessage = AssetNamespace.newBuilder().setType(
                AssetMessageType.ExecuteCommandResponse).setData(dataMsg.toByteString()).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, 
                Namespace.Asset, 5000, namespaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        //Added command models to map.
        when(m_AssetModel.getControllerId()).thenReturn(CONTROLLER_ID);
        when(m_AssetModel.getUuid()).thenReturn(assetUuid);
        when(m_AssetModel.getType()).thenReturn("Some Type");
        m_SUT.getAssetCommands(m_AssetModel);
        
        //Test response for camera settings command.
        Class camClazz = Class.forName(PROTO_COMMAND_PACKAGE 
                + "GetCameraSettingsResponseGen$GetCameraSettingsResponse");
        when(m_CommandConverter.retrieveProtoClass(CommandResponseEnum.GET_CAMERA_SETTINGS_RESPONSE))
            .thenReturn(camClazz);
        GetCameraSettingsResponse response = new GetCameraSettingsResponse();
        
        
        response.setFocus(2.3f);
        response.setZoom(5.1f);
        response.setWhiteBalance(WhiteBalanceEnum.TUNGSTEN);
        ExposureSettings settings = new ExposureSettings();
        settings.setAperture(5.55);
        settings.setExposureMode(ExposureModeEnum.AUTO);
        settings.setExposureTimeInMS(1000);
        settings.setExposureIndex(25);
        response.setExposureSettings(settings);
        when(m_CommandConverter.getJavaResponseType(Mockito.any(byte[].class), 
                eq(CommandResponseEnum.GET_CAMERA_SETTINGS_RESPONSE))).thenReturn(response);
        
        commandResponseHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        //Verify response was handle appropriately by checking command values to make sure they match the responses
        //values.
        SetCameraSettingsCommand cameraSettings = (SetCameraSettingsCommand)m_SUT.getAssetCommands(
                m_AssetModel).getCommandByType(CommandTypeEnum.SET_CAMERA_SETTINGS_COMMAND);
        assertThat(cameraSettings.getFocus(), is(2.3f));
        assertThat(cameraSettings.getZoom(), is(5.1f));
        assertThat(cameraSettings.getWhiteBalance(), is(WhiteBalanceEnum.TUNGSTEN));
        assertThat(cameraSettings.getExposureSettings().getExposureMode(), is(ExposureModeEnum.AUTO));
        assertThat(cameraSettings.getExposureSettings().getExposureTimeInMS(), is(1000));
        assertThat(cameraSettings.getExposureSettings().getAperture(), is(5.55));
        assertThat(cameraSettings.getExposureSettings().getExposureIndex(), is(25));
        
        //Test response for pan/tilt commands.
        dataMsg = dataMsg.toBuilder().setResponseType(CommandTypesGen.CommandResponse.Enum.GET_PAN_TILT_RESPONSE)
            .setResponse(GetPanTiltResponseGen.GetPanTiltResponse.newBuilder().
            setPanTilt(SpatialTypesGen.OrientationOffset.getDefaultInstance()).setBase(baseResponse).build()
                .toByteString()).build();
        thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, Namespace.Asset, 5001, 
                namespaceMessage);
        
        Class ptClazz = Class.forName(PROTO_COMMAND_PACKAGE + "GetPanTiltResponseGen$GetPanTiltResponse");
        when(m_CommandConverter.retrieveProtoClass(CommandResponseEnum.GET_PAN_TILT_RESPONSE))
            .thenReturn(ptClazz);
        
        GetPanTiltResponse panTiltResponse = new GetPanTiltResponse();
        panTiltResponse.setPanTilt(SpatialTypesFactory.newOrientationOffset(55.2, 75.7));

        when(m_CommandConverter.getJavaResponseType(Mockito.any(byte[].class), 
                eq(CommandResponseEnum.GET_PAN_TILT_RESPONSE))).thenReturn(panTiltResponse);
        
        commandResponseHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        //Verify response was handle appropriately by checking command values to make sure they match the responses
        //values.
        SetPanTiltCommand panTilt = (SetPanTiltCommand)m_SUT.getAssetCommands(m_AssetModel).getCommandByType(
                CommandTypeEnum.SET_PAN_TILT_COMMAND);
        OrientationOffset ptValues = panTilt.getPanTilt();
        assertThat(panTilt.isSetPanTilt(), is(true));
        assertThat(ptValues.isSetAzimuth(), is(true));
        assertThat(ptValues.isSetElevation(), is(true));
        assertThat(ptValues.isSetBank(), is(false));
        assertThat(ptValues.getAzimuth().getValue(), is(55.2));
        assertThat(ptValues.getElevation().getValue(), is(75.7));
        
        //Test response with null fields to test unset command.
        dataMsg = dataMsg.toBuilder().setResponseType(CommandTypesGen.CommandResponse.Enum.GET_PAN_TILT_RESPONSE)
            .setResponse(GetPanTiltResponseGen.GetPanTiltResponse.newBuilder()
                .setPanTilt(SpatialTypesGen.OrientationOffset.getDefaultInstance()).setBase(baseResponse)
                .build().toByteString()).build();
        thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, Namespace.Asset, 5001, 
                namespaceMessage);
        
        Class ptClazz2 = Class.forName(PROTO_COMMAND_PACKAGE + "GetPanTiltResponseGen$GetPanTiltResponse");
        when(m_CommandConverter.retrieveProtoClass(CommandResponseEnum.GET_PAN_TILT_RESPONSE))
            .thenReturn(ptClazz2);
        
        GetPanTiltResponse noPanTilt = new GetPanTiltResponse();
        when(m_CommandConverter.getJavaResponseType(Mockito.any(byte[].class), 
                eq(CommandResponseEnum.GET_PAN_TILT_RESPONSE))).thenReturn(noPanTilt);
        
        commandResponseHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        //Verify response was handle appropriately and that the fields were unset correctly.
        panTilt = (SetPanTiltCommand)m_SUT.getAssetCommands(m_AssetModel).getCommandByType(
                CommandTypeEnum.SET_PAN_TILT_COMMAND);
        assertThat(panTilt.isSetPanTilt(), is(false));
        
        //Test response with command that is unsupported by the specified asset.
        HaeMeters haeMeters = HaeMeters.newBuilder().setValue(5.0).build();
        LongitudeWgsDegrees longitude = LongitudeWgsDegrees.newBuilder().setValue(5.0).build();
        LatitudeWgsDegrees latitude = LatitudeWgsDegrees.newBuilder().setValue(5.0).build();
        Coordinates coord = Coordinates.newBuilder().setAltitude(haeMeters).setLongitude(longitude).
                setLatitude(latitude).build();
        dataMsg = dataMsg.toBuilder().setResponseType(
                CommandTypesGen.CommandResponse.Enum.GET_POINTING_LOCATION_RESPONSE).
            setResponse(GetPointingLocationResponseGen.
                    GetPointingLocationResponse.newBuilder().setBase(baseResponse).setLocation(coord).build()
                    .toByteString()).build();
        thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, Namespace.Asset, 5001, 
                namespaceMessage);
        
        Class pointClazz = Class.forName(PROTO_COMMAND_PACKAGE 
                + "GetPointingLocationResponseGen$GetPointingLocationResponse");
        when(m_CommandConverter.retrieveProtoClass(CommandResponseEnum.GET_POINTING_LOCATION_RESPONSE))
            .thenReturn(pointClazz);
        
        commandResponseHandler.handleResponse(thMessage, payload, namespaceMessage, dataMsg);
        
        verify(m_GrowlUtil).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_ERROR), eq("Asset Command Error:"), 
                eq(String.format("Command Pointing Location could not be synced for asset with UUID: %s because the " 
                        + "command does not exist or the command isn't supported.", assetUuid)));
    }
    
    /**
     * Verify another AssetNamespace message does not get handled.
     */
    @Test
    public void testCommandResponseHandlerWithNonHandledAssetMessage() throws InvalidProtocolBufferException
    {
        SyncCommandResponseHandler commandResponseHandler = m_SUT.new SyncCommandResponseHandler();

        AssetNamespace namespaceMessage = AssetNamespace.newBuilder().setType(
                AssetMessageType.GetNameResponse).build();
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMessage(CONTROLLER_ID, 0, 
                Namespace.Asset, 5000, namespaceMessage);
        TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(thMessage.getTerraHarvestPayload());
        
        commandResponseHandler.handleResponse(thMessage, payload, namespaceMessage, null);
        
        verify(m_GrowlUtil, never()).createGlobalFacesMessage(eq(FacesMessage.SEVERITY_INFO), 
                Mockito.anyString(), Mockito.anyString());
    }
}
