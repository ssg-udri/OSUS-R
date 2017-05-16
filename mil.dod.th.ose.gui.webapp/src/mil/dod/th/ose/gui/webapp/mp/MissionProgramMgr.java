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
package mil.dod.th.ose.gui.webapp.mp;

import java.util.List;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;

/**
 * This interface assists with the management of mission programs on remote systems.
 * @author callen
 *
 */
public interface MissionProgramMgr 
{
    /**
     * Get a List of all mission program names that are on the local system. An empty list will be returned if there
     * are no known local mission programs.
     * @return
     *     list of mission names
     */
    List<String> getLocalMissionNames();

    /**
     * Get a List of all mission template names that are on the active controller. An empty list will be returned if
     * there are no missions mapped to the currently active controller.
     * @param systemId
     *     the system ID to retrieve template names for
     * @return
     *     list of mission names 
     */
    List<String> getRemoteTemplateNames(int systemId);

    /**
     * Request mission program templates from the remote controller.
     * @param systemId
     *     the ID of the system to request templates from
     */
    void requestSyncingOfTemplates(int systemId);
    
    /**
     * Send the mission program message to the active controller.
     * @param controllerId
     *     the id of the controller to send the message to
     * @param message
     *     the mission program type message
     * @param type
     *     the type of the message    
     */
    void queueMessage(int controllerId, Message message, MissionProgrammingMessageType type);
}
