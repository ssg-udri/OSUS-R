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


/**
 * Class to handle users display of information for the currently running missions on a controller.
 * 
 * @author nickmarcucci
 *
 */
public interface CurrentMissionRequest
{
    /**
     * Retrieves a list of the current missions for a specific system based on the 
     * currently selected filters. 
     * @param controllerId
     *  the id of the system to for which to retrieve the current missions for.
     * @return
     *  the list of current missions for the specified controller. 
     */
    List<CurrentMissionModel> getCurrentFilteredMissions(int controllerId);
    
}
