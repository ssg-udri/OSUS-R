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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.List;

import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;


/**
 * This class is to be used to retrieve and edit modifiable property entries for a given factory object.
 * 
 * @author nickmarcucci
 *
 */

public interface FactoryObjectConfigModifierView
{
    /**
     * Method used to set the factory manager and the factory object model that are to be used to be 
     * displayed.
     * @param model
     *  the factory model which is going to be used to display its configuration values
     */
    void setSelectedFactoryModel(final FactoryBaseModel model);
    
    /**
     * Method to return factory base model. 
     * @return
     *  the factory base model that this bean represents
     */
    FactoryBaseModel getSelectedFactoryModel();
    
    /**
     * Method used to return the modifiable properties for the selected factory model.
     * @return
     *  the list of modifiable properties for the given factory model
     */
    List<ModifiablePropertyModel> getProperties();
    
    /**
     * Method used to update the properties of the known factory base model.
     */
    void updateAllPropertiesAsync();
}
