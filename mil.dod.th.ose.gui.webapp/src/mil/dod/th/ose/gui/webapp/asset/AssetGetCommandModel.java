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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.types.command.CommandTypeEnum;

/**
 * Class represents an asset's supported get commands that do not have a corresponding set command.
 * 
 * @author nickmarcucci
 *
 */
public class AssetGetCommandModel extends AbstractCommandModel
{
    /**
     * Map that contains responses for commands that do not have a corresponding set command.
     */
    private final Map<CommandTypeEnum, AssetGetCommandResponse> m_CommandMap = 
            Collections.synchronizedMap(new HashMap<CommandTypeEnum, AssetGetCommandResponse>());
    
    /**
     * Constructor.
     * 
     * @param uuid
     *  UUID of the asset this model represents
     * @param commands
     *  the list of command types that represent the get commands that this model
     *  represents
     */
    public AssetGetCommandModel(final UUID uuid, final List<CommandTypeEnum> commands) 
    {
        super(uuid);
        
        updateSupportedCommands(commands);
    }

    @Override
    public List<CommandTypeEnum> getSupportedCommands()
    {
        return new ArrayList<CommandTypeEnum>(m_CommandMap.keySet());
    }
    
    /**
     * Updates the list of supported get commands for the model.
     * 
     * @param commands
     *      Updated list of get commands the model supports.
     */
    public final void updateSupportedCommands(final List<CommandTypeEnum> commands)
    {
        for (CommandTypeEnum commandType : commands)
        {
            if (!m_CommandMap.containsKey(commandType))
            {
                m_CommandMap.put(commandType, new AssetGetCommandResponse(null, null));
            }
        }
        
        final Iterator<Entry<CommandTypeEnum, AssetGetCommandResponse>> commandsIterator = 
                m_CommandMap.entrySet().iterator();
        while (commandsIterator.hasNext())
        {
            final Entry<CommandTypeEnum, AssetGetCommandResponse> commandEntry = commandsIterator.next();
            if (!commands.contains(commandEntry.getKey()))
            {
                commandsIterator.remove();
            }
        }
    }
    
    /**
     * Returns the last known response for the given command type for a get command
     *      that has no corresponding set command.
     * @param commandType
     *      the command type that a response should be retrieved for
     * @return
     *      the last known response for the given command type
     */
    public AssetGetCommandResponse getCommandResponseByType(final CommandTypeEnum commandType)
    {
        return m_CommandMap.get(commandType);
    }
    
    /**
     * Attempts to set a response for the given command type if it is a get command that 
     * has no corresponding set command.
     * 
     * @param commandType
     *      the command type that the response should be set for
     * @param response
     *      the response that was received for the command type
     * @param date
     *      the date that the response was received
     * @return 
     *      true if {@link CommandTypeEnum} give corresponds to a get command with no set command and response
     *      is successfully set. false otherwise.
     */
    public boolean trySetResponseByType(final CommandTypeEnum commandType, 
            final Response response, final Date date)
    {
        if (m_CommandMap.containsKey(commandType))
        {
            m_CommandMap.put(commandType, new AssetGetCommandResponse(response, date));
            
            return true;
        }
        
        return false;
    }
}
