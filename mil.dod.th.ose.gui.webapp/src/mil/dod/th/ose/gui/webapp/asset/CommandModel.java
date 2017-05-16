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

import java.util.List;
import java.util.UUID;

import mil.dod.th.core.types.command.CommandTypeEnum;

/**
 * Class to describe base actions of an asset command.
 * @author nickmarcucci
 *
 */
public interface CommandModel
{
    /**
     * Method used to retrieve the UUID of the asset this model represents.
     * 
     * @return
     *      UUID of the asset this model represents.
     */
    UUID getUuid();
    
    /**
     * Method that returns all supported send commands for this asset.
     * 
     * @return
     *      {@link List} of {@link CommandTypeEnum} that represent all supported send commands.
     */
    List<CommandTypeEnum> getSupportedCommands();
    
    /**
     * Method that retrieves the display name for a specified command type.
     * 
     * @param commandType
     *      {@link CommandTypeEnum} that represents the command to retrieve the display name for.
     * @return
     *      A string that represents the display name for the specified command type.
     */
    String getCommandDisplayName(CommandTypeEnum commandType);
}
