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
 * The active controller is session scoped and represents the controller that is actively being interacted with.
 * @author callen
 *
 */
public interface ActiveController 
{
    /** Event topic prefix to use for all topics in ActiveController. */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/controller/ActiveController/";
    
    /** Topic used when a controller becomes the active controller or there is no longer an active controller. 
     * 
     * Contains the following event properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - controller id of 
     *      the currently active controller. If there is no longer an active controller, the property will not be set.
     * </li>
     * </ul>
     * */
    String TOPIC_ACTIVE_CONTROLLER_CHANGED = TOPIC_PREFIX + "ACTIVE_CONTROLLER_CHANGED";
    
    /**
     * Set the controller to be the 'active' controller.
     * @param model
     *     the model to use as the active controller
     */
    void setActiveController(ControllerModel model);
    
    /**
     * Get the controller model for the active controller.
     * @return
     *    the model that represents this controller
     */
    ControllerModel getActiveController();
    
    /**
     * Check to see if the active controller is set.
     * @return
     *      if the active controller is set
     */
    boolean isActiveControllerSet();
}
