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

import mil.dod.th.core.mp.Program.ProgramStatus;

/**
 * Mission manager to manage all currently loaded missions on a 
 * connected controller.
 * 
 * @author nickmarcucci
 *
 */
public interface CurrentMissionMgr
{
    /**
     * Topic prefix for all current mission events.
     */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/mp/CurrentMissionsConstants/";
    
    /**
     * Event for topic for mission updated events.
     */
    String TOPIC_MISSION_UPDATED = TOPIC_PREFIX + "MISSION_UPDATED";
    
    /**
     * Retrieves the list of all known mission for a specific system. 
     * @param systemId
     *  the id of the system to retrieve missions for
     * @return
     *  the list of missions for the specified system. an empty list will be
     *  returned if no programs are known for the specified id.
     */
    List<CurrentMissionModel> getCurrentMissionsForControllerAsync(int systemId);
    
    /**
     * Function to create an execute test request message and 
     * send it to the correct controller.
     * @param programName
     *  the name of the program that is to be tested
     * @param controllerId
     *  the id of the controller to which this request should be sent.
     */
    void executeTestRequest(String programName, int controllerId);
    
    /**
     * Function to create an execute mission request message and 
     * send it to the correct controller.
     * @param programName
     *  the name of the program that is to be executed
     * @param controllerId
     *  the id of the controller to which this request should be sent
     */
    void executeExecuteRequest(String programName, int controllerId);
    
    /**
     * Function to create an shutdown mission request message and 
     * send it to the correct controller.
     * @param programName
     *  the name of the program that is to be shutdown
     * @param controllerId
     *  the id of the controller to which this request should be sent
     */
    void executeShutdownRequest(String programName, int controllerId);
    
    /**
     * Function to create a cancel mission request message and 
     * send it to the correct controller.
     * 
     * @param programName
     *  the name of the program that is to be cancelled
     * @param controllerId
     *  the id of the controller to which this request should be sent
     */
    void executeCancelRequest(String programName, int controllerId);
    
    /**
     * Function to create an remove mission request message and 
     * send it to the correct controller.
     * @param programName
     *  the name of the program that is to be removed
     * @param controllerId
     *  the id of the controller to which this request should be sent
     */
    void executeRemoveRequest(String programName, int controllerId);
    
    /**
     * Function to translate {@link ProgramStatus} into an acceptable text 
     * representation. Intended as a temporary method.
     * @param status
     *  the status that is to be translated
     * @return
     *  the translated string representation of the {@link ProgramStatus}
     */
    String translateProgramStatus(ProgramStatus status);
}
