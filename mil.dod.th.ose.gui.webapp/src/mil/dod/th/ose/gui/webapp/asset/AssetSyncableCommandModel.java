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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.AssetCommandUtil;

/**
 * Model that represents all commands that are syncable (corresponding get/set commands) for an asset.
 * 
 * @author cweisenborn
 */
public class AssetSyncableCommandModel extends AbstractCommandModel
{
    /**
     * {@link Map} where the key is a {@link CommandTypeEnum} and the value is the {@link Command}
     * associated with that command type. This map is used to store all supported commands for an asset.
     */
    private final Map<CommandTypeEnum, Command> m_CommandMap = 
            Collections.synchronizedMap(new HashMap<CommandTypeEnum, Command>());
    
    /**
     * {@link BiMap} where the key is a {@link CommandTypeEnum} that represents a set command and the value is a 
     * {@link CommandTypeEnum} that represents the associated get command. This map is used to store the associated
     * get command type for a particular set command type.
     */
    private final BiMap<CommandTypeEnum, CommandTypeEnum> m_SyncableCommandTypeBiMap = 
            Maps.synchronizedBiMap(HashBiMap.create(new HashMap<CommandTypeEnum, CommandTypeEnum>()));
    
    private CommandCapabilities m_CommandCaps;
    
    /**
     * Constructor method that excepts the UUID of the asset the model represents as a parameter.
     * 
     * @param uuid
     *      UUID of the asset this model represents.
     * @param capabilities
     *      {@link CommandCapabilities} object associated with the asset.
     * @throws IllegalAccessException
     *      Thrown if the command class to be instantiated cannot be accessed.
     * @throws InstantiationException
     *      Thrown if the command object cannot be instantiated using reflections.
     * @throws ClassNotFoundException
     *      Thrown if the class for the command to be instantiated cannot be found. 
     */
    public AssetSyncableCommandModel(final UUID uuid, final CommandCapabilities capabilities) 
            throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        super(uuid);
        
        updateCapabilities(capabilities);
    }

    @Override
    public List<CommandTypeEnum> getSupportedCommands()
    {
        return new ArrayList<CommandTypeEnum>(m_CommandMap.keySet());
    }
    
    /**
     * Returns the command capabilities for the model.
     * 
     * @return
     *      The command capabilities for the model.
     */
    public CommandCapabilities getCapabilities()
    {
        return m_CommandCaps;
    }
    
    /**
     * Updates list of supported commands based off the specified command capabilities.
     * 
     * @param capabilities
     *      Command capabilities used to determine supported commands.
     * @throws IllegalAccessException
     *      Thrown if the command class to be instantiated cannot be accessed.
     * @throws InstantiationException
     *      Thrown if the command object cannot be instantiated using reflections.
     * @throws ClassNotFoundException
     *      Thrown if the class for the command to be instantiated cannot be found. 
     */
    public final void updateCapabilities(final CommandCapabilities capabilities) 
            throws ClassNotFoundException, InstantiationException, IllegalAccessException
    {
        m_CommandCaps = capabilities;
        final List<CommandTypeEnum> supportedCommands = capabilities.getSupportedCommands();
        //Add new commands to the command map.
        for (CommandTypeEnum commandType: supportedCommands)
        {
            //The set position command is handle on the asset configuration page and therefore should be ignored.
            //Also, check if the command type already exists within the map and skip adding it if it does.
            if (commandType.equals(CommandTypeEnum.SET_POSITION_COMMAND) || m_CommandMap.containsKey(commandType))
            {
                continue;
            }
            //The set tune settings command does not support syncing since it would require a large amount
            //of special handling to support. Therefore null is entered into the map so no get command is associated.
            else if (commandType.equals(CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND))
            {
                m_CommandMap.put(commandType, AssetCommandUtil.instantiateCommandBasedOnType(commandType));
            } 
            else if (commandType.toString().startsWith(AssetCommandUtil.SET_COMMAND_PREFIX))
            {
                m_CommandMap.put(commandType, AssetCommandUtil.instantiateCommandBasedOnType(commandType));
                m_SyncableCommandTypeBiMap.put(commandType, CommandTypeEnum.valueOf(AssetCommandUtil.GET_COMMAND_PREFIX 
                        + commandType.toString().substring(AssetCommandUtil.GET_COMMAND_PREFIX.length())));
            }
            else if (!commandType.toString().startsWith(AssetCommandUtil.GET_COMMAND_PREFIX))
            {
                m_CommandMap.put(commandType, AssetCommandUtil.instantiateCommandBasedOnType(commandType));
            }
        }
        
        //Remove any commands that are no longer supported.
        final Iterator<Entry<CommandTypeEnum, Command>> commandIterator = m_CommandMap.entrySet().iterator();
        while (commandIterator.hasNext())
        {
            final Entry<CommandTypeEnum, Command> commandEntry = commandIterator.next();
            if (!supportedCommands.contains(commandEntry.getKey()))
            {
                commandIterator.remove();
            }
        }
    }
    
    /**
     * Method used to retrieve a command object for the specified command type.
     * 
     * @param commandType
     *      {@link CommandTypeEnum} that represents the type of command object to be retrieved.
     * @return
     *      {@link Command} object that represents the associated command or null if no command of the type is stored
     *      within the command map.
     */
    public Command getCommandByType(final CommandTypeEnum commandType)
    {
        return m_CommandMap.get(commandType);
    }
    
    /**
     * Method used to retrieve a command object for the specified sync command type.
     * 
     * @param commandSyncType
     *      {@link CommandTypeEnum} that represents the sync type of command object to be retrieved.
     * @return
     *      {@link Command} object that represents the associated command or null if no command of the type is stored
     *      within the command map.
     */
    public Command getCommandBySyncType(final CommandTypeEnum commandSyncType)
    {
        final CommandTypeEnum commandType = m_SyncableCommandTypeBiMap.inverse().get(commandSyncType);
        return m_CommandMap.get(commandType);
    }
    
    /**
     * Method used to determine if the command of a specified type can be synced with the values stored on the remote 
     * controller.
     * 
     * @param commandType
     *      {@link CommandTypeEnum} that represents the type of command to be checked for syncing capabilities.
     * @return
     *      True if the command can be synced and false otherwise.
     */
    public boolean canSync(final CommandTypeEnum commandType)
    {
        if (m_SyncableCommandTypeBiMap.get(commandType) != null)
        {
            return true;
        }
        return false;
    }
    
    /**
     * Method used to retrieve the sync action name for the specified command type.
     *
     * @param commandType
     *      {@link CommandTypeEnum} that represents the command to retrieve the sync action name for.
     * @return
     *      A string that represents the sync action name for the specified command type.
     */
    public String getSyncActionName(final CommandTypeEnum commandType)
    {
        return "Sync " + getCommandDisplayName(commandType);
    }
    
    /**
     * Method that returns the command type associated with the specified sync command type.
     * 
     * @param commandType
     *      {@link CommandTypeEnum} that represents the sync command type.
     * @return
     *      {@link CommandTypeEnum} that represents the command type associated with the specified sync type. Will
     *      return null if no command type is associated with the specified sync type.
     */
    public CommandTypeEnum getCommandTypeBySyncType(final CommandTypeEnum commandType)
    {
        return m_SyncableCommandTypeBiMap.inverse().get(commandType);
    }
    
    /**
     * Method that returns the sync command type associated with the specified command type.
     * 
     * @param commandType
     *      {@link CommandTypeEnum} that represents the command type.
     * @return
     *      {@link CommandTypeEnum} that represents the sync command type associated with the specified command type. 
     *      Will return null if no sync command type is associated with the specified command type.
     */
    public CommandTypeEnum getCommandSyncTypeByType(final CommandTypeEnum commandType)
    {
        return m_SyncableCommandTypeBiMap.get(commandType);
    }
    
    /**
     * Retrieve a command instance by the specified {@link CommandTypeEnum}.
     * @param commandType
     *      the {@link CommandTypeEnum} for which a {@link Command} instance should be created
     * @return
     *      the {@link Command} that corresponds to the given {@link CommandTypeEnum}
     * @throws IllegalAccessException
     *      if command cannot be instantiated 
     * @throws InstantiationException 
     *      if command cannot be instantiated
     * @throws ClassNotFoundException 
     *      if command for the given type cannot be found
     */
    public Command getCommandInstanceByType(final CommandTypeEnum commandType) throws ClassNotFoundException, 
            InstantiationException, IllegalAccessException
    {
        return AssetCommandUtil.instantiateCommandBasedOnType(commandType);
    }
    
}
