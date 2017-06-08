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

import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.remote.lexicon.asset.commands.CaptureImageCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.CaptureImageResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.ConfigureProfileCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.ConfigureProfileResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.CreateActionListResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.DetectTargetCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.DetectTargetResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetCameraSettingsCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetCameraSettingsResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.GetLiftCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.GetLiftResponseGen;
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
import mil.dod.th.remote.lexicon.asset.commands.SetLiftCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.SetLiftResponseGen;
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
import mil.dod.th.remote.lexicon.asset.commands.TargetRefinementResponseGen;
import mil.dod.th.remote.lexicon.asset.commands.ZeroizeCommandGen;
import mil.dod.th.remote.lexicon.asset.commands.ZeroizeResponseGen;


/**
 * Class provides a set of utility functions to help in processing asset commands.
 * @author nickmarcucci
 *
 */
@Component(provide = { CommandConverter.class })
public class CommandConverterImpl implements CommandConverter
{
    /**
     * String reference that represents the package where the protocol equivalent of the JAXB commands are located.
     */
    private static final String PROTO_COMMAND_PACKAGE = "mil.dod.th.remote.lexicon.asset.commands.";
    
    /**
     * String reference to suffix that all commands have.
     */
    private static final String COMMAND_SUFFIX = "Command";
    
    /**
     * String reference to suffix that all response commands have.
     */
    private static final String RESPONSE_SUFFIX = "Response";
    
    /**
     *  Used to convert JAXB command/response objects to proto messages.
     */
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     *  Used to map enum command types to their enum response types.
     */
    private Map<CommandTypeEnum, CommandResponseEnum> m_ResponseTypes;
    
    /**
     * Set the {@link mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between proto and JAXB objects.
     */
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;        
    }
    
    /**
     * Activate method.
     * 
     * @throws ClassNotFoundException 
     *  thrown when initializing converters cannot find a class for 
     *  a corresponding command type.
     */
    @Activate
    public void activate() throws ClassNotFoundException
    {
        m_ResponseTypes = new HashMap<CommandTypeEnum, CommandResponseEnum>();

        final CommandTypeEnum[] values = CommandTypeEnum.values();
        for (CommandTypeEnum commandType : values)
        {
            final String commandResponse = commandType.value().replace(COMMAND_SUFFIX, RESPONSE_SUFFIX);
            
            m_ResponseTypes.put(commandType, CommandResponseEnum.fromValue(commandResponse));
        }
    }
    
    @Override
    public CommandResponseEnum getResponseTypeFromCommandType(final CommandTypeEnum commandType)
    {
        return m_ResponseTypes.get(commandType);
    }
    
    @Override
    public CommandTypeEnum getCommandTypeFromResponseType(final CommandResponseEnum commandResponse)
    {
        for (CommandTypeEnum type : m_ResponseTypes.keySet())
        {
            final CommandResponseEnum responseType = m_ResponseTypes.get(type);
            
            if (commandResponse.equals(responseType))
            {
                return type;
            }
        }
        return null;
    }
    
    @Override //NOCHECKSTYLE - Reached max cyclomatic complexity - needed to implement all asset commands
    public Command getJavaCommandType(final byte[] commandRequest, 
            final CommandTypeEnum commandEnum) throws InvalidProtocolBufferException, ObjectConverterException
    {
        Message commandProto;//NOCHECKSTYLE will get assigned inside switch statement
        
        switch (commandEnum)
        {
            case CAPTURE_IMAGE_COMMAND:
                commandProto = CaptureImageCommandGen.CaptureImageCommand.parseFrom(commandRequest);
                break;
            case DETECT_TARGET_COMMAND:
                commandProto =  DetectTargetCommandGen.DetectTargetCommand.parseFrom(commandRequest);
                break;
            case GET_CAMERA_SETTINGS_COMMAND:
                commandProto = 
                    GetCameraSettingsCommandGen.GetCameraSettingsCommand.parseFrom(commandRequest);
                break;
            case GET_PAN_TILT_COMMAND:
                commandProto = GetPanTiltCommandGen.GetPanTiltCommand.parseFrom(commandRequest);
                break;
            case GET_POSITION_COMMAND:
                commandProto = GetPositionCommandGen.GetPositionCommand.parseFrom(commandRequest);
                break;
            case GET_VERSION_COMMAND:
                commandProto = GetVersionCommandGen.GetVersionCommand.parseFrom(commandRequest);
                break;
            case GET_MODE_COMMAND:
                commandProto = GetModeCommandGen.GetModeCommand.parseFrom(commandRequest);
                break;
            case SET_CAMERA_SETTINGS_COMMAND:
                commandProto = 
                    SetCameraSettingsCommandGen.SetCameraSettingsCommand.parseFrom(commandRequest);
                break;
            case SET_PAN_TILT_COMMAND:
                commandProto = SetPanTiltCommandGen.SetPanTiltCommand.parseFrom(commandRequest); 
                break;
            case SET_POSITION_COMMAND:
                commandProto = SetPositionCommandGen.SetPositionCommand.parseFrom(commandRequest); 
                break; 
            case GET_POINTING_LOCATION_COMMAND:
                commandProto = 
                    GetPointingLocationCommandGen.GetPointingLocationCommand.parseFrom(commandRequest);
                break;
            case SET_POINTING_LOCATION_COMMAND:
                commandProto = 
                    SetPointingLocationCommandGen.SetPointingLocationCommand.parseFrom(commandRequest);
                break;
            case SET_TUNE_SETTINGS_COMMAND:
                commandProto = 
                    SetTuneSettingsCommandGen.SetTuneSettingsCommand.parseFrom(commandRequest);
                break;
            case GET_TUNE_SETTINGS_COMMAND:
                commandProto = 
                    GetTuneSettingsCommandGen.GetTuneSettingsCommand.parseFrom(commandRequest);
                break;
            case CONFIGURE_PROFILE_COMMAND:
                commandProto = 
                    ConfigureProfileCommandGen.ConfigureProfileCommand.parseFrom(commandRequest);
                break;
            case GET_PROFILES_COMMAND:
                commandProto = 
                    GetProfilesCommandGen.GetProfilesCommand.parseFrom(commandRequest);
                break;
            case SET_MODE_COMMAND:
                commandProto = SetModeCommandGen.SetModeCommand.parseFrom(commandRequest);
                break;
            case START_RECORDING_COMMAND:
                commandProto = StartRecordingCommandGen.StartRecordingCommand.parseFrom(commandRequest);
                break;
            case STOP_RECORDING_COMMAND:
                commandProto = StopRecordingCommandGen.StopRecordingCommand.parseFrom(commandRequest);
                break;
            case CREATE_ACTION_LIST_COMMAND:
                commandProto = CreateActionListCommandGen.CreateActionListCommand.parseFrom(commandRequest);
                break;
            case TARGET_REFINEMENT_COMMAND:
                commandProto = TargetRefinementCommandGen.TargetRefinementCommand.parseFrom(commandRequest);
                break;
            case GET_LIFT_COMMAND:
                commandProto = GetLiftCommandGen.GetLiftCommand.parseFrom(commandRequest);
                break;
            case SET_LIFT_COMMAND:
                commandProto = SetLiftCommandGen.SetLiftCommand.parseFrom(commandRequest);
                break;
            case ZEROIZE_COMMAND:
                commandProto = ZeroizeCommandGen.ZeroizeCommand.parseFrom(commandRequest);
                break;
            default:
                throw new IllegalArgumentException(String.format("%s is not a supported command", commandEnum));
        }
        
        return (Command)m_Converter.convertToJaxb(commandProto);
    }
    
    @Override //NOCHECKSTYLE - Reached max cyclomatic complexity - needed to implement all asset commands
    public Response getJavaResponseType(final byte[] response, final CommandResponseEnum commandEnum) throws 
            InvalidProtocolBufferException, ObjectConverterException
    {
        Message responseProto;//NOCHECKSTYLE will get assigned inside switch statement
        
        switch (commandEnum)
        {
            case CAPTURE_IMAGE_RESPONSE:
                responseProto = CaptureImageResponseGen.CaptureImageResponse.parseFrom(response);
                break;
            case DETECT_TARGET_RESPONSE:
                responseProto = DetectTargetResponseGen.DetectTargetResponse.parseFrom(response);
                break;
            case GET_CAMERA_SETTINGS_RESPONSE:
                responseProto = GetCameraSettingsResponseGen.GetCameraSettingsResponse.parseFrom(response);
                break;
            case GET_PAN_TILT_RESPONSE:
                responseProto = GetPanTiltResponseGen.GetPanTiltResponse.parseFrom(response);
                break;
            case GET_POSITION_RESPONSE:
                responseProto = GetPositionResponseGen.GetPositionResponse.parseFrom(response);
                break;
            case GET_VERSION_RESPONSE:
                responseProto = GetVersionResponseGen.GetVersionResponse.parseFrom(response);
                break;
            case GET_MODE_RESPONSE:
                responseProto = GetModeResponseGen.GetModeResponse.parseFrom(response);
                break;
            case SET_CAMERA_SETTINGS_RESPONSE:
                responseProto = SetCameraSettingsResponseGen.SetCameraSettingsResponse.parseFrom(response);
                break;
            case SET_PAN_TILT_RESPONSE:
                responseProto = SetPanTiltResponseGen.SetPanTiltResponse.parseFrom(response);
                break;
            case SET_POSITION_RESPONSE:
                responseProto = SetPositionResponseGen.SetPositionResponse.parseFrom(response);
                break;
            case GET_POINTING_LOCATION_RESPONSE:
                responseProto = GetPointingLocationResponseGen.GetPointingLocationResponse.parseFrom(response);
                break;
            case SET_POINTING_LOCATION_RESPONSE:
                responseProto = SetPointingLocationResponseGen.SetPointingLocationResponse.parseFrom(response);
                break;
            case GET_TUNE_SETTINGS_RESPONSE:
                responseProto = GetTuneSettingsResponseGen.GetTuneSettingsResponse.parseFrom(response);
                break;
            case SET_TUNE_SETTINGS_RESPONSE:
                responseProto = SetTuneSettingsResponseGen.SetTuneSettingsResponse.parseFrom(response);
                break;
            case CONFIGURE_PROFILE_RESPONSE:
                responseProto = ConfigureProfileResponseGen.ConfigureProfileResponse.parseFrom(response);
                break;
            case GET_PROFILES_RESPONSE:
                responseProto = GetProfilesResponseGen.GetProfilesResponse.parseFrom(response);
                break;
            case SET_MODE_RESPONSE:
                responseProto = SetModeResponseGen.SetModeResponse.parseFrom(response);
                break;
            case START_RECORDING_RESPONSE:
                responseProto = StartRecordingResponseGen.StartRecordingResponse.parseFrom(response);
                break;
            case STOP_RECORDING_RESPONSE:
                responseProto = StopRecordingResponseGen.StopRecordingResponse.parseFrom(response);
                break;
            case CREATE_ACTION_LIST_RESPONSE:
                responseProto = CreateActionListResponseGen.CreateActionListResponse.parseFrom(response);
                break;
            case TARGET_REFINEMENT_RESPONSE:
                responseProto = TargetRefinementResponseGen.TargetRefinementResponse.parseFrom(response);
                break;
            case GET_LIFT_RESPONSE:
                responseProto = GetLiftResponseGen.GetLiftResponse.parseFrom(response);
                break;
            case SET_LIFT_RESPONSE:
                responseProto = SetLiftResponseGen.SetLiftResponse.parseFrom(response);
                break;
            case ZEROIZE_RESPONSE:
                responseProto = ZeroizeResponseGen.ZeroizeResponse.parseFrom(response);
                break;
            default: 
                throw new IllegalArgumentException(String.format("%s is not a supported response", commandEnum));
        }
        
        return (Response)m_Converter.convertToJaxb(responseProto);
    }

    @Override
    public Class<? extends Message> retrieveProtoClass(final CommandTypeEnum commandType)
            throws ClassNotFoundException
    {
        return getProtoClass(convertEnumeration(commandType.toString()));
    }
    
    @Override
    public Class<? extends Message> retrieveProtoClass(final CommandResponseEnum responseEnum)
            throws ClassNotFoundException
    {
        return getProtoClass(convertEnumeration(responseEnum.toString()));
    }

    @Override
    public CommandResponseEnum getCommandResponseEnumFromClassName(final String fullyQualifiedResponseClassName)
            throws IllegalArgumentException
    {
        return CommandResponseEnum.fromValue(fullyQualifiedResponseClassName.substring(
                fullyQualifiedResponseClassName.lastIndexOf('.') + 1));
    }
    
    /**
     * Function to take a string value of an enumeration and format
     * to camel cased class.
     * @param enumeration
     *  the string representation of the enumeration
     * @return
     *  the camel cased class name format for the given enumeration
     */
    private String convertEnumeration(final String enumeration)
    {
        final String[] wordArray = enumeration.split("_");
        final StringBuilder builder = new StringBuilder();
        for (String word: wordArray)
        {
            builder.append(word.charAt(0) + word.substring(1).toLowerCase());
        }
        return builder.toString();
    }
    
    /**
     * Returns a proto buf class from the given class name.
     * @param className
     *  the class name that is to be instantiated into a proto buf class object
     * @return
     *  the proto buf class for the given class name
     * @throws ClassNotFoundException
     *  thrown if a class cannot be found for the given class name
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Message> getProtoClass(final String className) throws ClassNotFoundException
    {
        final String clazz = PROTO_COMMAND_PACKAGE + className + "Gen$" + className;
        return (Class<? extends Message>)Class.forName(clazz);
    }
}
