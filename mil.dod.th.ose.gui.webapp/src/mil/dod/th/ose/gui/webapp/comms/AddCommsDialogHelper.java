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
package mil.dod.th.ose.gui.webapp.comms;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

/**
 * Interface containing the methods used for adding new comms stacks to an active controller.
 * @author bachmakm
 *
 */
public interface AddCommsDialogHelper
{
    /**
     * Return the key to be used to load the Capabilities document.
     * @return the last updated Capabilities key.
     */
    String getCapsKey();
    
    /**
     * Set the current Capabilities key/name to be used to retrieve them from the CommsLayerTypesMgr.
     * @param key the class name or key used to retrieve Capabilities.
     */
    void setCapsKey(String key);

    /**
     * Returns the active index of an accordion panel.
     * @return
     *      active index of an accordion panel
     */
    int getActiveIndex();
    
    /**
     * Sets the active index of an accordion panel.
     * Used to reset the starting position of the tabs in the add comms
     * dialog after a request has been submitted.
     * @param activeIndex
     *      index of the accordion panel
     */
    void setActiveIndex(int activeIndex);

    /**
     * Resets all user-selected values from the add comms dialog.  Values are reset
     * after the request to create a stack.
     */
    void clearAllSelectedValues();
    
    /**
     * Resets the state of the input values on a form.  Used to clear
     * the validation state. 
     */
    void resetState();

    /**
     * Method used to validate that the transport timeout is positive. 
     * 
     * @param context
     *          The current faces context.
     * @param component
     *          The JSF component calling the validate method.
     * @param value
     *          The value being validated.
     * @throws ValidatorException
     *          Thrown value passed is not valid.
     */
    void validatePositiveTimeout(FacesContext context, UIComponent component, Object value) 
            throws ValidatorException;    
    
    /**
     * Get the model that is holding the new stack's information.
     * @return
     *     the model that contains all the desired values for a new comms stack
     */
    CommsStackCreationModel getCommsCreationModel();
}
