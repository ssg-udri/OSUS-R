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

/**
 * Interface used for maintaining the selected comms stack across multiple browser sessions
 * and assisting in operations performed on a single stack (i.e. activating, deactivating, perform BIT, and removing).  
 * @author bachmakm
 *
 */
public interface SelectedCommsHelper
{
    /**
     * Method which maintains the currently selected comms stack object to be displayed in the web app.
     * @param comms
     *      the comms stack object selected by the user.
     */
    void setSelectedComms(CommsStackModel comms);
    
    /**
     * Returns the user-selected comms stack model.
     * @return
     *      the user-selected {@link CommsStackModel} instance.
     */
    CommsStackModel getSelectedComms();

    /**
     * Sets the selected comms stack to be empty.  Used to display all comms stack objects in the web app.
     */
    void unSetSelectedComms();
    
    /**
     * Method responsible for sending a request to change a link's activation status based on
     * its current activation status. 
     * @param link
     *      the link to be activated or deactivated depending on its current status
     * @param systemId
     *      ID of the controller to which the link belongs
     */
    void sendLinkActivationRequest(CommsLayerLinkModel link, int systemId);
    
    /**
     * Method responsible for sending a request to perform a Built-In Test.
     * @param link
     *      the link selected to perform the Built-In Test
     * @param systemId
     *      ID of the controller to which the link belongs
     */
    void sendLinkPerformBitRequest(CommsLayerLinkModel link, int systemId);
    

    /**
     * Method responsible for sending a request to remove both transport and 
     * link layers from a stack if they exist.  
     * @param systemId
     *      ID of the controller to which the stack belongs.
     */
    void sendRemoveStackRequest(int systemId);
    
    /**
     * Method responsible for returning a string containing the names of the layers to be removed.
     * @return
     *      names of the layers being removed from the stack.
     */
    String getRemoveStackLayerNames();
    
    /**
     * Method responsible for setting the stack to be removed.
     * @param stack
     *      stack to be removed.  Only transport and link layers are removed from a stack.
     */
    void setRemoveStack(CommsStackModel stack);

}
