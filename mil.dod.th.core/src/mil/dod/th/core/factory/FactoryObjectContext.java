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
package mil.dod.th.core.factory;

import java.util.Map;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.pm.WakeLock;

/**
 * Context given to a {@link FactoryObjectProxy} implementation to perform specific operations.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface FactoryObjectContext extends FactoryObject
{
    /**
     * Post the given event topic with properties about the given object.
     * 
     * <p>
     * Contains the following properties at a minimum:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the factory object which created this event
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the factory object's name
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - the factory object's product 
     *                                                     type represented as a string
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the factory object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the factory object which is a string identifier
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this factory object represents 
     * (e.g., Asset)
     * </ul>
     *
     * @param topic
     *      topic of the event
     * @param props
     *      additional properties to add to the event or null if there are none
     */
    void postEvent(final String topic, final Map<String, Object> props);
    
    /**
     * Create a unique {@link WakeLock} through the {@link mil.dod.th.core.pm.PowerManager} service. This method does 
     * not keep the system awake, instead it creates a lock that can be used to keep the system awake.
     * 
     * @param lockId
     *      human readable unique string that identifies the lock
     * @return
     *      new wake lock
     * @throws IllegalArgumentException
     *      if the lockId is in use by an existing lock
     */
    WakeLock createPowerManagerWakeLock(final String lockId) throws IllegalArgumentException;
}
