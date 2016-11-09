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
package mil.dod.th.ose.gui.webapp.controller;

/**
 * Interface for the session scoped bean that is responsible for keeping track of all observations that have not been
 * view for each known controller.
 * 
 * @author cweisenborn
 */
public interface ObservationCountMgr
{
    /** Event topic prefix to use for all topics in the observation count manager. */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/controller/ObservationCount/";
    
    /** 
     * Topic used when the observation count for a controller has been updated. 
     * 
     * Contains the following event properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - controller id of 
     *      the controller that has had its count updated</li>
     * <li>{@link #EVENT_PROP_OBS_COUNT} - latest count of observations</li>
     * </ul>
     */
    String TOPIC_OBSERVATION_COUNT_UPDATED = TOPIC_PREFIX + "COUNT_UPDATED";

    /**
     * Event property containing the observation count.
     */
    String EVENT_PROP_OBS_COUNT = "obs.count";
    
    /**
     * Method that retrieves the current number of unread observations for the specified controller.
     * 
     * @param controllerId
     *          ID of the controller to retrieve the observation count for.
     * @return
     *          Integer that represents the current unread observation count for the specified controller.
     */
    int getObservationCount(int controllerId);

    /**
     * Method that sets the current number of unread observations for the specified controller to zero.
     * 
     * @param controllerId
     *          ID of the controller to set the number of unread observations to zero.
     */
    void clearObsCount(int controllerId);

}
