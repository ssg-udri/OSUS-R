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
package mil.dod.th.ose.remote.api;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;

/**
 * Class provides a set of utility functions to aid in the processing of 
 * asset commands.
 * 
 * @author nickmarcucci
 *
 */
public interface CommandConverter
{
    /**
     *  Returns the appropriate {@link CommandResponseEnum} for the given {@link CommandTypeEnum}.
     * @param commandType
     *  the {@link CommandTypeEnum} for which a {@link CommandResponseEnum} is desired
     * @return
     *  the appropriate {@link CommandResponseEnum} for the specified {@link CommandTypeEnum}; if 
     *  a response does not exist for the specified type then null will be returned
     */
    CommandResponseEnum getResponseTypeFromCommandType(CommandTypeEnum commandType);
    
    /**
     * Returns the appropriate {@link CommandTypeEnum} for the given {@link CommandResponseEnum}.
     * @param commandResponse
     *  the {@link CommandResponseEnum} for which a {@link CommandTypeEnum} is desired
     * @return
     *  the appropriate {@link CommandTypeEnum} for the specified {@link CommandResponseEnum}; if 
     *  a response does not exist for the specified type then null will be returned
     */
    CommandTypeEnum getCommandTypeFromResponseType(CommandResponseEnum commandResponse);
    
    /**
     * Method responsible for converting byte command message to java command type.
     * @param commandRequest 
     *  Remote proto request data 
     * @param commandEnum 
     *  Enumeration value of command
     * @return Command
     *  Return java command type
     * @throws InvalidProtocolBufferException 
     *  thrown when a protocol message being parsed is invalid
     * @throws ObjectConverterException 
     *  thrown when proto to jaxb conversion fails
     */
    Command getJavaCommandType(byte[] commandRequest,
            CommandTypeEnum commandEnum) throws InvalidProtocolBufferException, ObjectConverterException;
    
    /**
     * Method responsible for converting byte command response message to java response type.
     * @param response
     *  Remote proto response data
     * @param commandEnum
     *  Enumeration of the response
     * @return
     *  Return java response 
     * @throws InvalidProtocolBufferException
     *  thrown when a protocol message being parsed is invalid
     * @throws ObjectConverterException
     *  thrown when proto to jaxb conversion fails
     */
    Response getJavaResponseType(byte[] response, CommandResponseEnum commandEnum) throws 
            InvalidProtocolBufferException, ObjectConverterException;
    
    /**
     * Converts {@link CommandTypeEnum} to its protocol buffer equivalent class.
     * 
     * @param commandType
     *  the {@link CommandTypeEnum} that is to be converted to a proto buffer class.
     * @return
     *  the class that is represented by the given command type enumeration
     * @throws ClassNotFoundException
     *  throws exception if a class cannot be found for a given command type enumeration
     */
    Class<? extends Message> retrieveProtoClass(CommandTypeEnum commandType) throws ClassNotFoundException;
    
    /**
     * Converts {@link CommandResponseEnum} to its protocol buffer equivalent class.
     * @param responseEnum
     *  the {@link CommandResponseEnum} that is to be converted to a proto buffer class
     * @return
     *  the class that is represented by the given command response enumeration
     * @throws ClassNotFoundException
     *  throws exception if a class cannot be found for a given command response enumeration
     */
    Class<? extends Message> retrieveProtoClass(CommandResponseEnum responseEnum) throws ClassNotFoundException;
    
    /**
     * Get the command response type enum value corresponding to the given fully qualified class name.
     * @param fullyQualifiedResponseClassName
     *  the class name of the response type
     * @return 
     *  the corresponding enum value
     * @throws IllegalArgumentException
     *  if the given class name does not have response enumeration value
     */
    CommandResponseEnum getCommandResponseEnumFromClassName(String fullyQualifiedResponseClassName) 
            throws IllegalArgumentException;
}
