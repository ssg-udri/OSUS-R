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

import java.util.List;

import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;

/**
 * Request scoped bean that builds up all the comms stacks based on the current comm layers available.
 * 
 * @author Dave Humeniuk
 *
 */
public interface CommsStackRequest
{
    /**
     * Method is used to help update the list of comms stacks displayed in the web application as users navigate
     * to different comms stacks in an active controller. 
     * 
     * @param systemId
     *      ID of the active controller
     * @param selectedStack
     *      object that is currently selected or null if none are selected
     * @return
     *      list of all comms stacks or the user-selected comms stack to be displayed in the web application
     */
    List<CommsStackModel> getSelectedCommsStacksAsync(int systemId, CommsStackModel selectedStack);
    
    /**
     * Returns all instances of comms stacks for the given system.
     * 
     * @param systemId
     *      ID of the system
     * @return
     *      all instances of comms stacks for the given system
     */
    List<CommsStackModel> getCommsStacksAsync(int systemId);
    
    /**
     * Returns the top most {@link FactoryBaseModel} objects for the given system.
     * @param systemId
     *      ID of the system
     * @return
     *      all instances of {@link FactoryBaseModel}s which are the topmost layers for the system.
     */
    List<FactoryBaseModel> getTopMostComms(int systemId);
    
    /**
     * Returns the {@link CommsStackModel} for the specified {@link FactoryBaseModel}
     * residing on the specified system id.
     * @param systemId
     *      ID of the system
     * @param model
     *      the {@link FactoryBaseModel} for which to find its associated comms stack.
     * @return
     *      the {@link CommsStackModel} that contains the specified {@link FactoryBaseModel}
     *      on the specified system id. If no match is found then null is returned.
     */
    CommsStackModel getCommsStackForBaseModel(int systemId, FactoryBaseModel model);
}
