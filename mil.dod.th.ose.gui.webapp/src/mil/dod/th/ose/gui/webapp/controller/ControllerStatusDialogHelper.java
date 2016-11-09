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
 * Helper that class that holds the information needed for the dialog box that used to change the status of the
 * controller. This class also is responsible for sending the remote message that changes the status of the controller.
 * 
 * @author cweisenborn
 */
public interface ControllerStatusDialogHelper
{
    /**
     * Method for retrieving the controller whose status will be displayed and altered.
     * 
     * @return
     *          {@link ControllerModel} that represents controller whose status is to be displayed and altered.
     */
    ControllerModel getController();

    /**
     * Method that sets the controller whose status will be displayed and altered.  
     * 
     * @param model
     *          {@link ControllerModel} that represents the controller whose status is to be displayed and altered.
     */
    void setController(ControllerModel model);

    /**
     * Method used to send a request to set the system status.
     * @param mode the current system status.
     */
    void updatedSystemStatus(String mode);
}
