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
package mil.dod.th.ose.core.pm.api;

import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.pm.WakeLock;

/**
 * Internal interface used by the core bundle for methods available for the {@link mil.dod.th.core.pm.PowerManager} 
 * implementation, but not available to other bundles. Interface is provided as an OSGi service.
 * 
 * @author cweisenborn
 */
public interface PowerManagerInternal 
{
    /**
     * Create a unique {@link WakeLock} that can be used to request a lock later. This method does not keep the system
     * awake, instead it creates a lock that can be used to keep the system awake. Use 
     * {@link mil.dod.th.core.pm.PowerManager#createWakeLock(Class, String)} instead, if the lock is not to be used by 
     * a {@link FactoryObject}.
     * 
     * @param context
     *      context for the wake lock which will be the plug-in class which implements {@link FactoryObjectProxy}
     * @param sourceObject
     *      object that will use the lock
     * @param lockId
     *      human readable unique string that identifies the lock
     * @return
     *      new wake lock
     * @throws IllegalArgumentException
     *      if the combination of context and lockId are in use by an existing lock
     */
    WakeLock createWakeLock(Class<? extends FactoryObjectProxy> context, FactoryObject sourceObject, String lockId) 
            throws IllegalArgumentException;
    
    /**
     * Deletes an existing lock. {@link mil.dod.th.core.pm.PlatformPowerManager#cancelWakeLock(WakeLock)} is 
     * used to verify that the lock being deleted is no longer active or scheduled. After this method, 
     * the {@link WakeLock} can no longer be used to keep the system awake.
     * 
     * @param lock
     *      lock to delete
     * @throws IllegalArgumentException
     *      if the lock provided has already been deleted
     */
    void deleteWakeLock(WakeLock lock) throws IllegalArgumentException;
}
