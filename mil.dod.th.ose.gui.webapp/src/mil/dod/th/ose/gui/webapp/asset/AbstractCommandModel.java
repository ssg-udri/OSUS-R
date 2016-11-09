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

import java.util.UUID;

import com.google.common.base.Joiner;

import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.AssetCommandUtil;

/**
 * Base implementation for a asset command.
 * @author nickmarcucci
 *
 */
public abstract class AbstractCommandModel implements CommandModel
{
    /**
     * Reference to the UUID of the asset.
     */
    private final UUID m_Uuid;
    
    /**
     * Constructor.
     * @param uuid
     *  the UUID of the model that the command pertains to
     */
    public AbstractCommandModel(final UUID uuid)
    {
        m_Uuid = uuid;
    }
    
    
    @Override
    public UUID getUuid() 
    {
        return m_Uuid;
    }
    
    @Override
    public String getCommandDisplayName(final CommandTypeEnum commandType)
    {
        final String commandString = commandType.toString();
        final String[] splitList = commandString.split("_");
        
        //remove the get or the set 
        if (commandString.startsWith(AssetCommandUtil.GET_COMMAND_PREFIX) 
                || commandString.startsWith(AssetCommandUtil.SET_COMMAND_PREFIX))
        {
            splitList[0] = null;
        }
        
        //remove the command suffix
        if (splitList[splitList.length - 1].equals("COMMAND"))
        {
            splitList[splitList.length - 1] = null;
        }
        
        //iterate through list and transform to first letter cap and rest lower case
        for (int i = 0; i < splitList.length; i++)
        {
            if (splitList[i] != null)
            {
                final StringBuilder builder = new StringBuilder();
                splitList[i] = builder.append(splitList[i].charAt(0) 
                        + splitList[i].substring(1).toLowerCase()).toString();
            }
        }
        
        return Joiner.on(" ").skipNulls().join(splitList);
    }
}
