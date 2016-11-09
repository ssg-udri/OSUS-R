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

import java.util.UUID;

import mil.dod.th.ose.gui.webapp.CompletableModel;
import mil.dod.th.ose.gui.webapp.advanced.configuration.UnmodifiablePropertyModel;

/**
 * This interface describes common attributes that factory objects possess.
 * @author callen
 *
 */
public interface FactoryBaseModel extends CompletableModel
{
    /**
     * Event property for the {@link UUID} of the {@link mil.dod.th.core.factory.FactoryObject} model as a string.
     */
    String EVENT_PROP_UUID = "obj.uuid";
    
    /**
     * Get the PID of the factory object represented by this model.
     *  If the object does not have a configuration and 
     *  therefore does not have a PID, the empty string will be 
     *  returned.
     * @return
     *     the PID or an empty string if no configuration for the 
     *     object exists.
     */
    String getPid();
    
    /**
     * Set the PID of the factory object represented by this model.
     * If passed in PID is null, the object's PID will be set to 
     * the empty string.
     * @param pid
     *  the PID that the factory model is to be set to. If PID value
     *  is null then value will be set to the empty string.
     */
    void setPid(String pid);

    /**
     * Get the UUID of this factory object.
     * @return
     *     the uuid
     */
    UUID getUuid();

    /**
     * Get the id of the controller that this factory object belongs to.
     * @return
     *    the controller id that this object belongs to
     */
    int getControllerId();

    /**
     * Get the object's name.
     * @return
     *     the name
     */
    String getName();
    
    /**
     * Get the property model based on the key for the property.
     * 
     * @param key
     *      key of the property to retrieve
     * @return
     *      model representing the property or null if not found
     */
    UnmodifiablePropertyModel getPropertyAsync(String key);
    
    /**
     * Get the image of the factory.
     * @return
     *      the URL of the image
     */
    String getImage();
    
    /**
     * Method used to return the factory configuration PID.
     * @return
     *  the factory configuration PID
     */
    String getFactoryPid();
    
    /**
     * Method to retrieve the factory manager that is used to manage this factory object.
     * @return
     *  the factory manager that is used to operate on this factory object
     */
    FactoryObjMgr getFactoryManager();
}
