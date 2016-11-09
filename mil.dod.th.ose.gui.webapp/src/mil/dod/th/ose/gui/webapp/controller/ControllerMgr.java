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

import java.util.List;

/**
 * Manages known controllers for the system. Added controllers are not required to have
 * unique names. When adding a controller the controller name and id are retrieved from the
 * requested controller via a {@link mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage}.
 * A channel to the controller is required before adding the controller. This is because there must be
 * a known channel to send the terra harvest message through. The channel does not have to be confirmed before adding 
 * the controller. 
 * @author callen
 *
 */
public interface ControllerMgr 
{
    /** Event topic prefix to use for all topics in ControllerConstants. */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/controller/ControllerConstants/";
    
    /**
     * Topic used when a controller is added.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller that has been added.
     * </ul>
     */
    String TOPIC_CONTROLLER_ADDED = TOPIC_PREFIX + "CONTROLLER_ADDED";
    
    /**
     * Topic used when a controller is removed.
     * 
     * Contains the following fields:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller the bundle is located on.
     * <li> {@link #EVENT_PROP_CONTROLLER_NAME} - Name of the controller removed.
     * <li> {@link #EVENT_PROP_CONTROLLER_VERSION} - Version of the controller removed.
     * </ul>
     */
    String TOPIC_CONTROLLER_REMOVED = TOPIC_PREFIX + "CONTROLLER_REMOVED";
    
    /**
     * Topic used when information about a controller is received/updated.
     */
    String TOPIC_CONTROLLER_UPDATED = TOPIC_PREFIX + "CONTROLLER_UPDATED";
    
    /** Event property key for the controller name. */
    String EVENT_PROP_CONTROLLER_NAME = "controller.name";
    
    /** Event property key for the controller version. */
    String EVENT_PROP_CONTROLLER_VERSION = "controller.version";
    
    /**
     * Get a list of all controllers known to the application.
     * @return
     *     a list representing all controllers managed
     */
    List<ControllerModel> getAllControllers();

    /**
     * Get the model representing the controller with the id passed.
     * @param controllerId
     *     id of the controller model to retrieve
     * @return
     *     the controller model with the given id
     */
    ControllerModel getController(int controllerId);

    /**
     * Remove a controller from the controller manager. Will return false if the controller is not known to the 
     * application.
     * @param controllerId
     *     the id of the controller to remove
     * @exception IllegalArgumentException
     *     thrown if the controller id passed is not found     
     */
    void removeController(int controllerId) throws IllegalArgumentException;
}
