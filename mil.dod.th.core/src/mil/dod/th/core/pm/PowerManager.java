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

package mil.dod.th.core.pm;

import java.util.Set;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.factory.FactoryObject;

/**
 * This core provided OSGi service keeps track of all {@link WakeLock}s and forwards lock requests to the {@link 
 * PlatformPowerManager}.
 * <p>
 * Consumers first create a {@link WakeLock} using {@link #createWakeLock(Class, String)} or 
 * {@link mil.dod.th.core.factory.FactoryObjectContext#createPowerManagerWakeLock(String)} and then the created lock can
 * be activated, which prevents the system from going into standby.
 * </p> 
 * <p>
 * Each platform must also provide the {@link PlatformPowerManager} as an OSGi service to allow going to standby mode.
 * If not, the {@link PowerManager} will still be available, but will not attempt to make requests to the 
 * {@link PlatformPowerManager}. In addition, locks can still be created or deleted, but will remain in the {@link 
 * WakeLockState#Inactive} state.
 * </p>
 * @author dhumeniuk
 */
@ProviderType
public interface PowerManager
{
    /**
     * Create a unique {@link WakeLock} that can be used to request a lock later. This method does not keep the system
     * awake, instead it creates a lock that can be used to keep the system awake. Use 
     * {@link mil.dod.th.core.factory.FactoryObjectContext#createPowerManagerWakeLock(String)} instead, if the lock is 
     * to be used by a {@link mil.dod.th.core.factory.FactoryObject} (e.g., {@link mil.dod.th.core.asset.Asset}).
     * 
     * @param context
     *      context for the lock, typically the class using the lock
     * @param lockId
     *      human readable unique string that identifies the lock
     * @return
     *      new wake lock
     * @throws IllegalArgumentException
     *      if the combination of context and lockId are in use by an existing lock
     */
    WakeLock createWakeLock(Class<?> context, String lockId) throws IllegalArgumentException;
    
    /**
     * Get a set of all {@link WakeLock}s. Can be used to get all locks in any state at once or only one state. This 
     * could be used to enumerate all of the {@link WakeLock}s and override their locks. This service maintains all 
     * created locks, while {@link PlatformPowerManager#getActiveWakeLocks()} is used to determine currently active 
     * locks.
     * 
     * NOTE: Consumer should be careful with modifying the state of a lock as behavior is unpredictable.
     *
     * @param state
     *      filter by this lock state
     * @return
     *      the set of locks that match the state
     */
    Set<WakeLock> getWakeLocks(WakeLockState state);
    
    /**
     * Similar to {@link #getWakeLocks(WakeLockState)}, but return only locks with the given context.
     * 
     * NOTE: Consumer should be careful with modifying the state of a lock as behavior is unpredictable.
     * 
     * @param context
     *      context for the lock, typically the class using the lock
     * @param state
     *      filter by this lock state
     * @return
     *      the set of locks that match the parameters
     */
    Set<WakeLock> getWakeLocks(Class<?> context, WakeLockState state);
    
    /**
     * Similar to {@link #getWakeLocks(WakeLockState)}, but return only locks used by the given source object.
     * 
     * NOTE: Consumer should be careful with modifying the state of a lock as behavior is unpredictable.
     * 
     * @param sourceObject
     *      object to filter on
     * @param state
     *      filter by this lock state
     * @return
     *      the set of locks that match the parameters
     */
    Set<WakeLock> getWakeLocks(FactoryObject sourceObject, WakeLockState state);
    
    /**
     * Get the set of contexts associated with currently known locks.
     * 
     * @return
     *      set of context classes
     */
    Set<Class<?>> getWakeLockContexts();
}
