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

import mil.dod.th.ose.gui.webapp.advanced.configuration.ModifiablePropertyModel;

/**
 * Common methods that a {@link mil.dod.th.core.factory.FactoryObject} manager can use to manage objects.
 * 
 * @author nickmarcucci
 *
 */
public interface FactoryObjMgr
{        
    /**
     * Method to create a remote configuration for an object identified by a uuid and 
     * sets the configuration's properties to the given properties.
     * @param systemId
     *  the system id on which the configuration is set
     * @param model
     *  the {@link FactoryBaseModel} that the configuration will be set on.
     * @param properties
     *  the properties that are to be set on the configuration
     */
    void createConfiguration(int systemId, FactoryBaseModel model, List<ModifiablePropertyModel> properties);
        
}
