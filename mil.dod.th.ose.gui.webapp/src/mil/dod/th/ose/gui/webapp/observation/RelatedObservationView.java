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
package mil.dod.th.ose.gui.webapp.observation;

import java.util.UUID;

/**
 * View scoped managed bean interface.  Manages a history of viewed observations.
 * 
 * @author dhumeniuk
 */
public interface RelatedObservationView
{
    /**
     * Move backwards in the list of viewed observations, setting the current node to the previous observation UUID.
     */
    void back();

    /**
     * Move forwards in the list of viewed observations, setting the current node to the next observation UUID.
     */
    void forward();
    
    /**
     * Method used to determine if there is a observation UUID in the list of viewed observations.
     * 
     * @return
     *      True if there is a observation UUID in the list and false otherwise.
     */
    boolean canMoveBack();
    
    /**
     * Method used to determine if there is a previous observation in the list of viewed observations.
     * 
     * @return
     *      True if there is a previous observation in the list and false otherwise.
     */
    boolean canMoveForward();
 
    /**
     * Clears the list of observations.
     * 
     * @param initialNodeUuid
     *      UUID of the observation that will represent the initial node.
     */
    void initialize(UUID initialNodeUuid);
    
    /**
     * Sets the current node to view, call the getObservation method to get the observation for this node.
     * 
     * @param currentNodeUuid
     *      Set the current to the observation with the specified UUID.
     */
    void setCurrentNode(UUID currentNodeUuid);
    
    /**
     * Get the observation for the current node.
     * 
     * @return
     *      Observation that represents the current node.
     */
    GuiObservation getObservation();
}
