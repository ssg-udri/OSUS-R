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

import mil.dod.th.core.controller.capability.ControllerCapabilities;

/**
 * Class handles the retrieval and display of an individual controller's
 * information.
 * @author nickmarcucci
 *
 */
public interface ControllerInfoDialogHelper
{
    /**
     * Function to retrieve the controller that a user has specified to get information
     * from. This information contains things like controller version.
     * @return
     *  the controller model in question.
     */
    ControllerModel getInfoController();
    
    /**
     * Function to set the controller that information is being requested for.
     * @param model
     *  the controller model for which information is being requested.
     */
    void setInfoController(ControllerModel model);
    
    /**
     * Get Controller Capabilities from the model, if they exist.
     * @return Controller Capabilities object, or null.
     */
    ControllerCapabilities getCtlrCaps();
}
