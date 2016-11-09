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

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.types.command.CommandTypeEnum;

/**
 * Class which handles sending and handling of all commands.
 * @author nickmarcucci
 *
 */
public interface CommandHandlerMgr
{
    /**
     * Method used to send the remote command for execution by the asset.
     * 
     * @param controllerId
     *          ID of the controller where the asset is located.
     * @param uuid
     *          UUID of the asset that is execute the command.
     * @param commandType
     *          The enumeration that represents the type of command being sent.
     *          
     * @throws ClassNotFoundException
     *          if the class cannot be found when trying to create a valid command for the given type
     * @throws InstantiationException
     *          if the command of the given type cannot be instantiated
     * @throws IllegalAccessException
     *          if the rights to access the methods on the command are not available to this class
     */
    void sendCommand(int controllerId, UUID uuid, CommandTypeEnum commandType) throws ClassNotFoundException, 
            InstantiationException, IllegalAccessException;
    
    /**
     * Method used to send the remote command for execution by the asset.
     * 
     * @param controllerId
     *          ID of the controller where the asset is located.
     * @param uuid
     *          UUID of the asset that is execute the command.
     * @param command
     *          The command object to be executed.
     * @param commandType
     *          The enumeration that represents the type of command being sent.
     */
    void sendCommand(int controllerId, UUID uuid, Command command, CommandTypeEnum commandType);
}
