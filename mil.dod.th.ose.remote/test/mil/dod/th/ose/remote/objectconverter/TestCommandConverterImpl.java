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
package mil.dod.th.ose.remote.objectconverter;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.util.UUID;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetVersionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;
import mil.dod.th.remote.converter.CommandTypeEnumConverter;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen;
import mil.dod.th.remote.lexicon.asset.commands.CaptureImageCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.CaptureImageResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.ConfigureProfileCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.ConfigureProfileResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListCommandGen.Action;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListCommandGen.CollectDataParams;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListCommandGen.GoToWaypointParams;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.DetectTargetCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.DetectTargetResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetCameraSettingsCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetCameraSettingsResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetModeCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetModeResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPanTiltCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPanTiltResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPointingLocationCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPointingLocationResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetPositionResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetProfilesCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetProfilesResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetTuneSettingsCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetTuneSettingsResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetVersionCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetVersionResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetCameraSettingsCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetCameraSettingsResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetModeCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetModeResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPanTiltCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPanTiltResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPointingLocationCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPointingLocationResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPositionCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetPositionResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.SetTuneSettingsCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetTuneSettingsResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.StartRecordingCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.StartRecordingResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.StopRecordingCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.StopRecordingResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.TargetRefinementCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.TargetRefinementCommandGen.ChannelMetadata;
import mil.dod.th.remote.lexicon.asset.commands.TargetRefinementCommandGen.RefinementParams;
import mil.dod.th.remote.lexicon.asset.commands.TargetRefinementResponseGen;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.FrequencyKhz;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.Mode;
import mil.dod.th.remote.lexicon.types.SharedTypesGen.SpeedMetersPerSecond;
import mil.dod.th.remote.lexicon.types.image.ImageTypesGen.Camera;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.Coordinates;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.HaeMeters;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LatitudeWgsDegrees;
import mil.dod.th.remote.lexicon.types.spatial.SpatialTypesGen.LongitudeWgsDegrees;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Tests the command converter class.
 * @author nickmarcucci
 *
 */
public class TestCommandConverterImpl
{
    private CommandConverterImpl m_SUT;
    private UUID m_TestUUID = UUID.randomUUID();
    
    @Before
    public void setUp() throws ObjectConverterException, ClassNotFoundException
    {
        m_SUT = new CommandConverterImpl();
        
        JaxbProtoObjectConverter converter = mock(JaxbProtoObjectConverter.class);
        when(converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(mock(Command.class));
        
        SetPanTiltCommandGen.SetPanTiltCommand ptCmd = 
                SetPanTiltCommandGen.SetPanTiltCommand.newBuilder()
                .setBase(BaseTypesGen.Command.getDefaultInstance())
                .setPanTilt(SpatialTypesGen.OrientationOffset.newBuilder().
                        setAzimuth(SpatialTypesGen.AzimuthDegrees.newBuilder().
                                setValue(1.0d).build()).
                        setElevation(SpatialTypesGen.ElevationDegrees.newBuilder().
                                setValue(1.0d)).build()).build();
        when(converter.convertToProto(Mockito.any(Object.class))).thenReturn(ptCmd);
        when(converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(mock(Command.class));

        m_SUT.setJaxbProtoObjectConverter(converter);
        m_SUT.activate();
    }
    
    /**
     * Verify proper command response enum is returned for a specified command type enum.
     */
    @Test
    public void testCommandResponseEnumeration()
    {
        CommandResponseEnum responseTiltEnum = m_SUT
                .getResponseTypeFromCommandType(CommandTypeEnum.GET_PAN_TILT_COMMAND);
        assertThat(responseTiltEnum, is(CommandResponseEnum.GET_PAN_TILT_RESPONSE));
        
        CommandResponseEnum responseTargetEnum = m_SUT
                .getResponseTypeFromCommandType(CommandTypeEnum.DETECT_TARGET_COMMAND);
        assertThat(responseTargetEnum, is(CommandResponseEnum.DETECT_TARGET_RESPONSE));
    }
    
    /**
     * Verify that proper command type enum is returned for a specified command response enum.
     */
    @Test
    public void testGetCommandTypeFromResponseEnumeration()
    {
        CommandTypeEnum type = m_SUT.getCommandTypeFromResponseType(
                CommandResponseEnum.DETECT_TARGET_RESPONSE);
        
        assertThat(type, notNullValue());
        assertThat(type, is(CommandTypeEnum.DETECT_TARGET_COMMAND));
    }
    
    /**
     * Verify that the proper command is returned.
     */
    @Test
    public void testGetJavaCommandType() throws InvalidProtocolBufferException, ObjectConverterException
    {
        for (CommandTypeEnum commandType : CommandTypeEnum.values())
        {
            assertSingleGetJavaCommandType(commandType);
        }
    }
   
    /**
     * Tests a single command type to see if a valid JAXB command can be retrieved.
     * @param commandType
     *  the type of command to retrieve a JAXB command for
     */
    private void assertSingleGetJavaCommandType(CommandTypeEnum commandType) 
        throws InvalidProtocolBufferException, ObjectConverterException
    {
        Message commandGenProto;
        
        switch (commandType)
        {
            case GET_CAMERA_SETTINGS_COMMAND:
                commandGenProto = GetCameraSettingsCommandGen.GetCameraSettingsCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case GET_POSITION_COMMAND:
                commandGenProto = GetPositionCommandGen.GetPositionCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case CAPTURE_IMAGE_COMMAND:
                commandGenProto = CaptureImageCommandGen.CaptureImageCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setCamera(Camera.newBuilder().setId(3).build())
                        .build();
                break;
            case CONFIGURE_PROFILE_COMMAND:
                commandGenProto = ConfigureProfileCommandGen.ConfigureProfileCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setId("asdf")
                        .setMode(Mode.newBuilder().setValue(Mode.Enum.OFF)).build();
                break;
            case DETECT_TARGET_COMMAND:
                commandGenProto = DetectTargetCommandGen.DetectTargetCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setCamera(Camera.newBuilder().setId(3).build())
                        .build();
                break;
            case GET_PAN_TILT_COMMAND:
                commandGenProto = GetPanTiltCommandGen.GetPanTiltCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case SET_CAMERA_SETTINGS_COMMAND:
                commandGenProto = SetCameraSettingsCommandGen.SetCameraSettingsCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case SET_PAN_TILT_COMMAND:
                commandGenProto = SetPanTiltCommandGen.SetPanTiltCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setPanTilt(SpatialTypesGen.OrientationOffset.getDefaultInstance())
                        .build();
                break;
            case SET_POSITION_COMMAND:
                commandGenProto = SetPositionCommandGen.SetPositionCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case GET_VERSION_COMMAND:
                commandGenProto = GetVersionCommandGen.GetVersionCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case GET_POINTING_LOCATION_COMMAND:
                commandGenProto = GetPointingLocationCommandGen.GetPointingLocationCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case SET_POINTING_LOCATION_COMMAND:
                commandGenProto = SetPointingLocationCommandGen.SetPointingLocationCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setLocation(Coordinates.newBuilder()
                            .setLongitude(LongitudeWgsDegrees.newBuilder().setValue(1).build())
                            .setLatitude(LatitudeWgsDegrees.newBuilder().setValue(2).build()))
                        .build();
                break;
            case GET_TUNE_SETTINGS_COMMAND:
                commandGenProto = GetTuneSettingsCommandGen.GetTuneSettingsCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case SET_TUNE_SETTINGS_COMMAND:
                commandGenProto = SetTuneSettingsCommandGen.SetTuneSettingsCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setBandwidth(FrequencyKhz.newBuilder().setValue(10).setPrecision(100).build())
                        .setFrequency(FrequencyKhz.newBuilder().setValue(5).setPrecision(100).build())
                        .addChannels(1).build();
                break;
            case GET_PROFILES_COMMAND:
                commandGenProto = GetProfilesCommandGen.GetProfilesCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case GET_MODE_COMMAND:
                commandGenProto = GetModeCommandGen.GetModeCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case SET_MODE_COMMAND:
                commandGenProto = SetModeCommandGen.SetModeCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .setMode(Mode.newBuilder().setValue(Mode.Enum.AUTO).build())
                        .build();
                break;
            case CREATE_ACTION_LIST_COMMAND:
                commandGenProto = CreateActionListCommandGen.CreateActionListCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .addAction(Action.newBuilder().setCollectDataParams(CollectDataParams.newBuilder()
                            .setMaxDurationMs(5000).setTimeoutMs(1000)).setGoToWaypointParams(
                            GoToWaypointParams.newBuilder().setLocation(Coordinates.newBuilder().setAltitude(
                            HaeMeters.newBuilder().setValue(5000.15)).setLatitude(
                            LatitudeWgsDegrees.newBuilder().setValue(75.3)).setLongitude(
                            LongitudeWgsDegrees.newBuilder().setValue(52.7))).setSpeed(
                            SpeedMetersPerSecond.newBuilder().setValue(105.56))))
                        .build();
                break;
            case TARGET_REFINEMENT_COMMAND:
                commandGenProto = TargetRefinementCommandGen.TargetRefinementCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .addRefinement(RefinementParams.newBuilder().setRefinementAction(
                            TargetRefinementCommandGen.RefinementAction.Enum.ADD)
                            .setChannelMetadata(ChannelMetadata.newBuilder().addChannel(5)
                            .setEstimatedFrequency(FrequencyKhz.newBuilder().setValue(773.21)))
                            .setTimeMs(System.currentTimeMillis()))
                        .build();
                break;
            case START_RECORDING_COMMAND:
                commandGenProto = StartRecordingCommandGen.StartRecordingCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            case STOP_RECORDING_COMMAND:
                commandGenProto = StopRecordingCommandGen.StopRecordingCommand.newBuilder()
                        .setBase(BaseTypesGen.Command.getDefaultInstance())
                        .build();
                break;
            default:
                fail(String.format("Unit test for Command %s is missing", commandType));
                return;
        }
        
        final ExecuteCommandRequestData executeCommandRequestData = 
                ExecuteCommandRequestData.newBuilder()
                .setCommand(commandGenProto.toByteString()).setCommandType(CommandTypeEnumConverter
                        .convertJavaEnumToProto(commandType))
                                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_TestUUID)).build();
        
        Command command = m_SUT.getJavaCommandType(executeCommandRequestData.getCommand().toByteArray(), commandType);
        
        assertThat(command, notNullValue());
    }
    
    /**
     * Verify that the proper response is returned.
     */
    @Test
    public void testGetJavaResponseType() throws InvalidProtocolBufferException, 
        ObjectConverterException, ClassNotFoundException
    {
        m_SUT = new CommandConverterImpl();
        
        JaxbProtoObjectConverter converter = mock(JaxbProtoObjectConverter.class); 
        when(converter.convertToJaxb(Mockito.any(Message.class))).thenReturn(mock(Response.class));
        m_SUT.setJaxbProtoObjectConverter(converter);
        
        m_SUT.activate();
        
        for (CommandResponseEnum commandResponse : CommandResponseEnum.values())
        {
            assertSingleGetJavaResponseType(commandResponse);
        }
    }
    
    /**
     * Tests a single command response type to see if a valid JAXB response can be retrieved.
     * @param commandResponse
     *  the type of command response to retrieve a JAXB response for
     */
    private void assertSingleGetJavaResponseType(CommandResponseEnum commandResponse) 
        throws ObjectConverterException, InvalidProtocolBufferException
    {
        Message commandResponseProto;
        
        switch (commandResponse)
        {
            case GET_CAMERA_SETTINGS_RESPONSE:
                commandResponseProto = GetCameraSettingsResponseGen.GetCameraSettingsResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case GET_POSITION_RESPONSE:
                commandResponseProto = GetPositionResponseGen.GetPositionResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case CAPTURE_IMAGE_RESPONSE:
                commandResponseProto = CaptureImageResponseGen.CaptureImageResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case CONFIGURE_PROFILE_RESPONSE:
                commandResponseProto = ConfigureProfileResponseGen.ConfigureProfileResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case DETECT_TARGET_RESPONSE:
                commandResponseProto = DetectTargetResponseGen.DetectTargetResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case GET_PAN_TILT_RESPONSE:
                commandResponseProto = GetPanTiltResponseGen.GetPanTiltResponse.newBuilder()
                        .setPanTilt(SpatialTypesGen.OrientationOffset.getDefaultInstance())
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case SET_CAMERA_SETTINGS_RESPONSE:
                commandResponseProto = SetCameraSettingsResponseGen.SetCameraSettingsResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case SET_PAN_TILT_RESPONSE:
                commandResponseProto = SetPanTiltResponseGen.SetPanTiltResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case SET_POSITION_RESPONSE:
                commandResponseProto = SetPositionResponseGen.SetPositionResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case GET_VERSION_RESPONSE:
                commandResponseProto = GetVersionResponseGen.GetVersionResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .setCurrentVersion("version")
                        .build();
                break;
            case GET_POINTING_LOCATION_RESPONSE:
                commandResponseProto = GetPointingLocationResponseGen.GetPointingLocationResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .setLocation(Coordinates.newBuilder()
                            .setLongitude(LongitudeWgsDegrees.newBuilder().setValue(1).build())
                            .setLatitude(LatitudeWgsDegrees.newBuilder().setValue(2).build()))
                        .build();
                break;
            case SET_POINTING_LOCATION_RESPONSE:
                commandResponseProto = SetPointingLocationResponseGen.SetPointingLocationResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case GET_TUNE_SETTINGS_RESPONSE:
                commandResponseProto = GetTuneSettingsResponseGen.GetTuneSettingsResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case SET_TUNE_SETTINGS_RESPONSE:
                commandResponseProto = SetTuneSettingsResponseGen.SetTuneSettingsResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case GET_PROFILES_RESPONSE:
                commandResponseProto = GetProfilesResponseGen.GetProfilesResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case GET_MODE_RESPONSE:
                commandResponseProto = GetModeResponseGen.GetModeResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .setMode(Mode.newBuilder().setValue(Mode.Enum.AUTO)
                            .setDescription("mode").build())
                        .build();
                break;
            case SET_MODE_RESPONSE:
                commandResponseProto = SetModeResponseGen.SetModeResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case CREATE_ACTION_LIST_RESPONSE:
                commandResponseProto = CreateActionListResponseGen.CreateActionListResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case TARGET_REFINEMENT_RESPONSE:
                commandResponseProto = TargetRefinementResponseGen.TargetRefinementResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case START_RECORDING_RESPONSE:
                commandResponseProto = StartRecordingResponseGen.StartRecordingResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            case STOP_RECORDING_RESPONSE:
                commandResponseProto = StopRecordingResponseGen.StopRecordingResponse.newBuilder()
                        .setBase(BaseTypesGen.Response.getDefaultInstance())
                        .build();
                break;
            default:
                fail(String.format("Unit test for Response %s is missing", commandResponse));
                return;
        }
        
        final ExecuteCommandResponseData executeCommandResponseData = 
                ExecuteCommandResponseData.newBuilder().setResponseType(CommandResponseEnumConverter
                        .convertJavaEnumToProto(commandResponse))
                .setResponse(commandResponseProto.toByteString())
                                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(m_TestUUID)).build();
        
        Response response = m_SUT.getJavaResponseType(
                executeCommandResponseData.getResponse().toByteArray(), commandResponse);
        
        assertThat(response, notNullValue());
    }
    
    /**
     * verify that the correct proto class name is generated for a given string name.
     */
    @Test
    public void testRetrieveProtoClassName() throws ClassNotFoundException
    {
        Class<? extends Message> protoClass = m_SUT.retrieveProtoClass(CommandTypeEnum.CAPTURE_IMAGE_COMMAND);
        
        assertThat(protoClass.getName(), 
                is("mil.dod.th.remote.lexicon.asset.commands.CaptureImageCommandGen$CaptureImageCommand"));
        
        Class<? extends Message> protoResponseClass = m_SUT
                .retrieveProtoClass(CommandResponseEnum.CAPTURE_IMAGE_RESPONSE);
        
        assertThat(protoResponseClass.getName(), 
                is("mil.dod.th.remote.lexicon.asset.commands.CaptureImageResponseGen$CaptureImageResponse"));
    }
    
    /**
     * Verify that the correct command response enum value is return for a string value representing a 
     * class' fully qualified classname.
     */
    @Test
    public void testGetCommandResponseEnumFromClassName()
    {
        CommandResponseEnum response = 
                m_SUT.getCommandResponseEnumFromClassName(GetVersionResponse.class.getName());
        assertThat(response, is(CommandResponseEnum.GET_VERSION_RESPONSE));
    }
}
