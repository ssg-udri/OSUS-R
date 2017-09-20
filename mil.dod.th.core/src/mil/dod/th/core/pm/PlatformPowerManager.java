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

import mil.dod.th.core.ccomm.physical.PhysicalLink;

/**
 * OSGi service provided by the platform to allow the {@link PowerManager} to control power to the system. {@link 
 * PowerManager} is the interface that other components use, while this service is implemented by each platform to 
 * interact with the hardware in relation to power management and is used by the {@link PowerManager}. Hardware power
 * management includes going into a standby mode when possible and obtaining battery information.
 * <p>
 * This service will ensure the system is in standby mode unless a {@link WakeLock} has been activated through {@link 
 * #activateWakeLock(WakeLock, long, long)}. Likewise, the {@link WakeLock} can be cancelled through {@link 
 * #cancelWakeLock(WakeLock)}. Before the system goes into standby, the {@link #TOPIC_STANDBY_NOTICE_EVENT} event will 
 * be sent to each registered handler, waiting for each handler to complete. At this time, a {@link WakeLock} can be 
 * activated to keep the system from entering standby. The event will be sent based on the {@link 
 * #CONFIG_PROP_STDBY_NOTICE_TIME_MS}.
 * </p>
 * <p>
 * When this service determines a {@link WakeLock} should be active or an enabled wake source (see {@link 
 * PlatformPowerManager#getPhysicalLinkWakeSourceNames()}) sees activity, the platform will exit standby. If the system
 * wakes due to a wake up source, all {@link WakeLock}s will go to {@link WakeLockState#Inactive}. When the system wakes
 * up, the {@link #TOPIC_WAKEUP_EVENT} event will be sent to each registered handler, waiting for each handler to 
 * complete and will at stay awake after initially waking up for a duration based on the {@link 
 * #CONFIG_PROP_MIN_WAKE_TIME_MS} property.
 * </p>
 * <p>
 * This service will track all {@link WakeLockState#Active} and {@link WakeLockState#Scheduled} locks.
 * </p>
 * <p>
 * This service can be configured through {@link org.osgi.service.cm.ConfigurationAdmin} using the PID matching the 
 * fully qualified class name of this interface.
 * </p>
 * @author dhumeniuk
 */
public interface PlatformPowerManager
{
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/pm/PlatformPowerManager/";
    
    /** 
     * Topic used when the system wakes up (i.e., system exits standby). Event will be sent to each handler and block
     * until the handler is done. Therefore, processing should be kept to a minimum.
     * 
     * The following properties are provided with the event
     * <ul>
     * <li>{@link #EVENT_PROP_WAKEUP_TIME} - when the system exited standby</li>
     * </ul>
     */
    String TOPIC_WAKEUP_EVENT = TOPIC_PREFIX + "WAKEUP";
    
    /** 
     * Topic used when the system is about to go to standby. Event will be sent to each handler and block until the 
     * handler is done. Therefore, processing should be kept to a minimum. Time between sending the event and going to 
     * standby is configured using {@link #CONFIG_PROP_STDBY_NOTICE_TIME_MS}.
     * <p>
     * NOTE: Event handlers can activate a {@link WakeLock} to prevent standby.
     * </p>
     * <ul>
     * <li>{@link #EVENT_PROP_STANDBY_TIME} - when the system is scheduled to enter standby</li>
     * </ul>
     */
    String TOPIC_STANDBY_NOTICE_EVENT = TOPIC_PREFIX + "STANDBY_NOTICE";
    
    /**
     * Topic used when system standby is cancelled. Event will be sent to each handler.
     * 
     * <ul>
     * <li>{@link #EVENT_PROP_CANCELLED_TIME} - when the system cancelled standby</li>
     * </ul>
     */
    String TOPIC_STANDBY_CANCELLED_EVENT = TOPIC_PREFIX + "STANDBY_CANCELLED";
    
    /** 
     * Event property key for the wake up time in milliseconds (Long) since the epoch.
     * 
     * @see System#currentTimeMillis()
     */
    String EVENT_PROP_WAKEUP_TIME = "wakeup.time.ms";
    
    /** 
     * Event property key for the scheduled standby time in milliseconds (Long) since the epoch.
     * 
     * @see System#currentTimeMillis()
     */
    String EVENT_PROP_STANDBY_TIME = "standby.time.ms";
    
    /**
     * Event property key for the time at which standby was cancelled in milliseconds (Long) since the epoch.
     */
    String EVENT_PROP_CANCELLED_TIME = "cancelled.time.ms";

    /**
     * Configuration PID identifier used by implementations of this service.
     */
    String CONFIG_PID = "mil.dod.th.core.pm.PlatformPowerManager";

    /** 
     * Configuration property key for whether power management is enabled. When disabled, the system will stay awake and
     * ignore all requests. If the property is not set, power management is enabled. 
     */
    String CONFIG_PROP_ENABLED = "enabled";
    
    /** 
     * Configuration property key for how long to stay awake when the system exists standby (i.e., minimum time to 
     * remain on after resume).
     */
    String CONFIG_PROP_MIN_WAKE_TIME_MS = "min.wake.time.ms";
    
    /** 
     * Configuration property key for the standby notification time, in milliseconds (i.e., time before standby to send
     * event).
     */
    String CONFIG_PROP_STDBY_NOTICE_TIME_MS = "standby.notice.time.ms";
    
    /** 
     * Configuration property key for the maximum sleep time, in milliseconds (i.e., maximum time to remain asleep 
     * between events).
     */
    String CONFIG_PROP_MAX_SLEEP_TIME_MS = "max.sleep.time.ms";
    
    /** 
     * Configuration property key for the start up time, in milliseconds (i.e., the time to prevent sleep on startup). 
     */
    String CONFIG_PROP_STARTUP_TIME_MS = "startup.time.ms";
    
    
    /**
     * Used to define an indefinite time when working with milliseconds since the epoch.
     * 
     * @see System#currentTimeMillis()
     */
    long INDEFINITE = -1;
    
    /**
     * Enable the given {@link PhysicalLink} to wake up the system for a configured duration ({@link 
     * #CONFIG_PROP_MIN_WAKE_TIME_MS}.
     * 
     * @param link
     *      link to enable as a wake up source
     * @throws IllegalArgumentException
     *      if the provided source cannot be used to wake up the platform
     * @throws IllegalStateException
     *      if the wake source is already enabled
     */
    void enableWakeSource(PhysicalLink link) throws IllegalArgumentException, IllegalStateException;
    
    /**
     * Disable the given {@link PhysicalLink} to wake up the system.
     * 
     * @param link
     *      link to disable as a wake up source
     * @throws IllegalStateException
     *      if the wake source is not currently enabled
     */
    void disableWakeSource(PhysicalLink link) throws IllegalStateException;
    
    /**
     * Get a set of the currently enabled {@link PhysicalLink} wake sources. Input from any of the named sources will
     * cause the system to wake up.
     * 
     * @return
     *      set of names of {@link PhysicalLink}s that are currently enabled to wake the system.
     * 
     * @see #enableWakeSource(PhysicalLink)
     */
    Set<String> getPhysicalLinkWakeSourceNames();
    
    /**
     * Get a set of all {@link WakeLockState#Active} {@link WakeLock}s.
     * 
     * NOTE: Consumer should be careful with modifying the state of a lock as behavior is unpredictable.
     * 
     * @return
     *      The set of locks
     */
    Set<WakeLock> getActiveWakeLocks();
    
    /**
     * Get a set of all {@link WakeLockState#Scheduled} {@link WakeLock}s.
     * 
     * NOTE: Consumer should be careful with modifying the state of a lock as behavior is unpredictable.
     * 
     * @return
     *      The set of locks
     */
    Set<WakeLock> getScheduledWakeLocks();
    
    /**
     * Request the platform to stay awake for the specified time range. This method should only be called by the core.
     * Other bundles should use the {@link PowerManager} service to make requests for wake locks.
     * 
     * @param lock
     *      lock of the requester
     * @param startLockTimeMs
     *      starting time for the wake lock duration in milliseconds since the epoch
     * @param stopLockTimeMs
     *      ending time for the wake lock duration in milliseconds since the epoch or {@link #INDEFINITE} to hold the 
     *      lock until released
     * @return
     *      currently planned standby time in milliseconds since the epoch or {@link #INDEFINITE} if lock is to be held
     *      until released. 
     */
    long activateWakeLock(WakeLock lock, long startLockTimeMs, long stopLockTimeMs);
    
    /**
     * Deactivates an {@link WakeLockState#Active} lock or cancels a {@link WakeLockState#Scheduled} lock. Any previous 
     * requests to activate a lock can be released using this call. If a request was made for waking the system in the 
     * future, the wake lock will no longer apply. This method should only be called by the core. Other bundles should 
     * use the {@link PowerManager} service to release wake locks.
     * 
     * @param lock
     *      lock of the requester
     * @throws IllegalStateException
     *      if the lock is not currently {@link WakeLockState#Active} or {@link WakeLockState#Scheduled}
     */
    void cancelWakeLock(WakeLock lock) throws IllegalStateException;
    
    /**
     * Return the estimated Amp-Hours remaining on the connected battery.
     * 
     * @return
     *      battery life remaining in Amp-Hours
     * @throws UnsupportedOperationException
     *      if the platform does not know this information or not applicable (not battery operated)
     */
    int getBatteryAmpHoursRem() throws UnsupportedOperationException;

    /**
     * Get the operating voltage of the connected battery in mV.
     * 
     * @return
     *      operating voltage of battery in mV
     * @throws UnsupportedOperationException
     *      if the platform does not know this information or not applicable (not battery operated)
     */
    int getBatteryVoltage() throws UnsupportedOperationException;
}
