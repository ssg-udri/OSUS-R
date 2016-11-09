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
package mil.dod.th.ose.gui.webapp.factory;

import java.util.List;

/**
 * This interface assists with the generic handling of factory objects on their designated pages. 
 * @author callen
 *
 */
public interface FactoryObjectDisplayHelper
{
    /**
     * Get the factory object list from the appropriate service. This list will only return objects related to the
     * currently active controller
     * @return
     *     the models of the specified type
     */
    List<? extends FactoryBaseModel> getFactoryObjectListAsync();

    /**
     * Set the selected factory object.
     * @param model
     *     the model to set as the selected object
     */
    void setSelectedFactoryObject(FactoryBaseModel model);

    /**
     * Get the selected factory object.
     * @return
     *     get the model of the object that is currently selected 
     */
    FactoryBaseModel getSelectedFactoryObject();

    /**
     * If a selected factory object is set.
     * @return
     *     true if a factory object is set, false otherwise
     */
    boolean isSetSelectedObject();

    /**
     * Get the title for which this class is being implemented for.
     * @return
     *     the title of the feature
     */
    String getFeatureTitle();
}
