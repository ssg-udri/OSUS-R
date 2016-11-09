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
package mil.dod.th.ose.gui.webapp.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.protobuf.Message;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.remote.lexicon.asset.commands.BaseTypesGen;

/**
 * Class that contains utilities for asset commands.
 * @author nickmarcucci
 *
 */
public final class AssetCommandUtil
{
    /**
     * String reference for the get command prefix.
     */
    public static final String GET_COMMAND_PREFIX = "GET";
    
    /**
     * String reference for the set command prefix.
     */
    public static final String SET_COMMAND_PREFIX = "SET";
    
    /**
     * String reference for the underscore character.
     */
    private static final String UNDERSCORE = "_";
    
    /**
     * Constructor.
     */
    private AssetCommandUtil()
    {
        //private constructor
    }
    
    /**
     * Function to create a Message.Builder instance for the given class.
     * @param clazz
     *          the class of the Message.Builder that is to be created
     * @return
     *          the Message.Builder for the given {@link CommandTypeEnum}
     * @throws NoSuchMethodException
     *          if the "newBuilder" method on the Message.Builder cannot be found
     * @throws SecurityException
     *          if the rights to access methods on the Message.Builder for the given type do not exist
     * @throws ClassNotFoundException
     *          if the class based on the given {@link CommandTypeEnum} cannot be found
     * @throws IllegalAccessException
     *          if the rights to access methods on the Message.Builder for the given type do not exist
     * @throws IllegalArgumentException
     *          if an invalid argument is passed when creating the Message.Builder instance
     * @throws InvocationTargetException
     *          if the Message.Builder instance cannot be created
     */
    public static Message.Builder createMessageBuilderFromCommandType(final Class<?> clazz) throws 
            NoSuchMethodException, 
            SecurityException, ClassNotFoundException, IllegalAccessException, 
            IllegalArgumentException, InvocationTargetException
    {
        //Call the static new builder method to instantiate the command.
        final Method newBuilderMethod = clazz.getDeclaredMethod("newBuilder");
        final Message.Builder builder = (Message.Builder)newBuilderMethod.invoke(null);
        
        //Build and set a base command for the command message builder that was instantiated.
        final BaseTypesGen.Command baseCommand = BaseTypesGen.Command.newBuilder().build();
        final Method setBaseCommand = builder.getClass().getDeclaredMethod("setBase", BaseTypesGen.Command.class);
        setBaseCommand.invoke(builder, baseCommand);
        
        return builder;
    }
    
    /**
     * Converts a given {@link CommandTypeEnum} to its related class name.
     * @param commandType
     *          the command type that is to be converted
     * @return
     *          the string class name that represents the given command type enumeration
     */
    public static String commandTypeToClassName(final CommandTypeEnum commandType)
    {
        final String[] wordArray = commandType.toString().split(UNDERSCORE);
        final StringBuilder builder = new StringBuilder();
        for (String word: wordArray)
        {
            builder.append(word.charAt(0) + word.substring(1).toLowerCase());
        }
        
        return builder.toString();
    }
    
    /**
     * Method to generate a command instance based on the given type.
     * @param commandType
     *      The {@link CommandTypeEnum} that represents the type of command to be instantiated
     * @return
     *      the command instance that has been generated from the given type
     * @throws ClassNotFoundException
     *      Thrown if the class for the command to be instantiated cannot be found. 
     * @throws InstantiationException
     *      Thrown if the command object cannot be instantiated using reflections.
     * @throws IllegalAccessException
     *      Thrown if the command class to be instantiated cannot be accessed.
     */
    public static Command instantiateCommandBasedOnType(final CommandTypeEnum commandType) throws 
            ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        final String packageName = Command.class.getPackage().getName();
        final String className = commandTypeToClassName(commandType);            
        final Class<?> commandClass = Class.forName(packageName + "." + className);
        
        return (Command)commandClass.newInstance();
    }
}
