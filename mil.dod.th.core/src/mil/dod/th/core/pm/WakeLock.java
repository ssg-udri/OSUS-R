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

import java.util.Date;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.factory.FactoryObject;

/**
 * Lock is used by consumers to keep the system awake. Use {@link #activate()} to activate the lock and keep the
 * system awake or use {@link #scheduleWakeTime(Date)} to schedule a lock in the future. In addition, active and 
 * scheduled locks can be canceled using {@link #cancel()}. Activation, schedule and cancel requests are 
 * forwarded to the {@link PlatformPowerManager} where the platform specific logic handles transitioning in and out of 
 * standby mode at the appropriate times.   
 * <p>
 * Each lock is uniquely identified by the context and id and can be created using {@link 
 * PowerManager#createWakeLock(Class, String)}
 * </p>
 * @author dhumeniuk
 *
 */
@ProviderType
public interface WakeLock
{
    /**
     * Get the human readable identifier of this lock.
     * 
     * @return
     *      identifier of the lock
     */
    String getId();
    
    /**
     * Get the identifier of the class that is using this lock. If {@link 
     * mil.dod.th.core.factory.FactoryObjectContext#createPowerManagerWakeLock(String)} was used to create the lock, 
     * this method will return {@link Class#getClass()} for the {@link
     * mil.dod.th.core.factory.FactoryObjectProxy} using the lock. This is typically the same as {@link 
     * mil.dod.th.core.factory.FactoryDescriptor#getProductType()}, but is not always the case.
     * 
     * @return
     *      context for this lock
     */
    Class<?> getContext();
    
    /**
     * Get the {@link FactoryObject} that will use the lock and will be the source of activation requests.
     * 
     * @return object associated with the lock or null if lock is not associated with a {@link FactoryObject}
     */
    FactoryObject getSourceObject();
    
    /**
     * Request the platform to stay awake for at least the specified duration. If the requested lock duration falls 
     * after the currently planned next sleep time, then extend the next sleep until the locked time.
     * 
     * This request will override any previous requests for the lock. 
     * 
     * @param lockAwakeDuration
     *      time required to stay awake
     * @param unit
     *      units of time for specified duration
     * @return
     *      currently planned standby time or null if the {@link PlatformPowerManager} is unavailable
     * @throws IllegalArgumentException
     *      if the duration is negative
     */
    Date activate(long lockAwakeDuration, TimeUnit unit) throws IllegalArgumentException;

    /**
     * Request the platform to stay awake until at least the specified time. If the requested lock time falls after the 
     * currently planned next sleep time, then extend the next sleep until the locked time.
     * 
     * This request will override any previous requests for the lock. 
     * 
     * @param lockAwakeTime
     *      time that the platform may sleep
     * @return
     *      currently planned standby time or null if the {@link PlatformPowerManager} is unavailable
     */
    Date activate(Date lockAwakeTime);
    
    /**
     * Request the platform to stay awake indefinitely.
     * 
     * This request will override any previous requests for the lock.
     */
    void activate();
    
    /**
     * Request the platform to be awake at the specified time in the future. The platform will then stay awake for a 
     * duration based on {@link PlatformPowerManager#CONFIG_PROP_MIN_WAKE_TIME_MS}.
     * 
     * This request will override any previous requests for the lock. 
     * 
     * @param wakeTime
     *      time that the platform should be awake
     */
    void scheduleWakeTime(Date wakeTime);

    /**
     * Deactivates an {@link WakeLockState#Active} lock or cancels a {@link WakeLockState#Scheduled} lock. Any previous 
     * requests to activate a lock can be released using this call. If a request was made for waking the system in the 
     * future, the wake lock will no longer apply.
     * 
     * @throws IllegalStateException
     *      if the lock is not currently {@link WakeLockState#Active} or {@link WakeLockState#Scheduled}
     */
    void cancel() throws IllegalStateException;
    
    /**
     * Deletes an existing lock. {@link #cancel()} is used to verify that the lock being deleted is no longer active or 
     * scheduled. After this method, the lock can no longer be used to keep the system awake.
     * 
     * @throws IllegalStateException
     *      if the lock has already been deleted
     */
    void delete() throws IllegalStateException;
}
