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

package example.platform.power;

import static java.util.concurrent.TimeUnit.*; //NOCHECKSTYLE: needed style of import for TimeUnit

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;



/**
 * This class represents the OS or hardware level code that the platform power manager implementation calls on to manage
 * the controller's power. This is merely for demonstration purposes and may not be needed in an actual implementation, 
 * depending on the hardware and OS the controller is running on.
 * 
 * @author Josh
 *
 */
@Component(provide = ExamplePlatformKernel.class)
public class ExamplePlatformKernel implements EventHandler
{ 
    /**
     * Wake up event topic        
     */
    public static final String TOPIC_SCHEDULE_WAKE_UP_EVENT = "SCHEDULE_WAKE_UP";
    
    /**
     * Wake source activity event topic
     */
    public static final String TOPIC_WAKESOURCE_ACTIVITY_DETECTED_EVENT = "WAKESOURCE_ACTIVITY_DETECTED";
    
    /** Used to store active WakeLocks */
    private Set<WakeLocks> m_ActiveLocks;
    
    /** Used to store scheduled WakeLocks */
    private Set<WakeLocks> m_ScheduledLocks;
    
    /** Used to store inactive WakeLocks */
    private Set<WakeLocks> m_InactiveLocks;
    
    /** Used to store wake sources */
    private Set<PhysicalLink> m_WakeSources;
    
    /**
     * Minimum time to stay awake, in milliseconds
     */
    private long m_MinWakeTimeMs = 0;
    
    /**
     * Standby notice time delay, in milliseconds
     */
    private long m_StandbyNoticeTimeMs = 0;
    
    /**
     * Maximum sleep time, in milliseconds
     */
    private long m_MaxSleepTimeMs = 0;
    
    /**
     * Startup sleep delay time, in milliseconds
     */
    private long m_StartupTimeMs = 0;
    
    /**
     * Reference to the event admin service used to post events.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Service for logging events.
     */
    private LoggingService m_Logging;
     
    /**
     * Simulated hardware battery amp hours remaining
     */
    private int m_BatteryAmpHoursRem = 100;
    
    /**
     * Simulated hardware battery voltage
     */
    private int m_BatteryVoltage = 9;
    
    /**
     * Thread simulating OS/hardware low/normal power mode switching
     */
    private Thread m_LowPowerThread;
    
    /**
     * {@link ScheduledExecutorService} for scheduling events and method calls
     */
    private ScheduledExecutorService m_Scheduler;
    
    /**
     * Low power mode thread handler
     */
    private ScheduledFuture<?> m_PowerModeHandler;
    
    /**
     * Event registrations reference
     */
    private ServiceRegistration<EventHandler> m_Registrations;
    
    /**
     * Sets the current {@link PlatformPowerManager} configuration property values
     * @param config
     *      {@link ExamplePlatformPowerManagerConfig} of the configuration property values
     */
    public void setConfiguration(ExamplePlatformPowerManagerConfig config)
    {
        m_Logging.debug("Setting configuration properties");
        m_MinWakeTimeMs = config.minWakeTimeMs();
        m_StandbyNoticeTimeMs = config.startupTimeMs();
        m_MaxSleepTimeMs = config.maxSleepTimeMs();
        m_StartupTimeMs = config.startupTimeMs();
        
        m_Logging.debug("min.wake.time: [%s]", m_MinWakeTimeMs);
        m_Logging.debug("standby.notice.time.ms: [%s]", m_StandbyNoticeTimeMs);
        m_Logging.debug("max.sleep.time.ms: [%s]", m_MaxSleepTimeMs);
        m_Logging.debug("startup.time.ms [%s]", m_StartupTimeMs);
    }
     
    /**
     * Bind the logging service.
     * 
     * @param logging
     *      service to bind
     */
    @Reference
    public void setLogging(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Sets the event admin service to be used for posting events.
     * 
     * @param eventAdmin
     *          The {@link EventAdmin} service to be used.
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    @Activate
    public void activate(final BundleContext context)
    {
        m_ActiveLocks = new HashSet<WakeLocks>();
        m_ScheduledLocks = new HashSet<WakeLocks>();
        m_InactiveLocks = new HashSet<WakeLocks>();
        m_WakeSources = new HashSet<PhysicalLink>();
        m_Registrations = registerEvents(context);
        m_Scheduler = Executors.newScheduledThreadPool(4);
    }
    
    @Deactivate
    public void deactivate()
    {
        m_Registrations.unregister();
    }
    
    /**
     * Sets up the event registration topics the class is interested in
     * @param context
     *      the {@link BundleContext} to register the events with
     * @return
     *      the {@link ServiceRegistration} object reference for unregistering events on deactivation
     */
    private ServiceRegistration<EventHandler> registerEvents(BundleContext context)
    {
        final String[] topics = {TOPIC_SCHEDULE_WAKE_UP_EVENT, TOPIC_WAKESOURCE_ACTIVITY_DETECTED_EVENT};
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, topics);
        
        return context.registerService(EventHandler.class, this, props);
    }

    /**
     * Activates a {@link WakeLock} for a given duration
     * @param lock
     *      the lock to activate
     * @param duration
     *      the duration to activate, in milliseconds
     */
    public void activateWakelock(WakeLock lock, long duration)
    {
        WakeLocks newLock = new WakeLocks(lock, System.currentTimeMillis(), duration);
        m_ActiveLocks.add(newLock);
    }
    
    /**
     * Deactivates a given {@link WakeLock}
     * @param lock
     *      the WakeLock to deactivate
     */
    public void deactivateWakelock(WakeLock lock)
    {
        WakeLocks lockToRemove = findMatchingLock(lock, m_ActiveLocks);

        if (lockToRemove != null)
        {
            m_ActiveLocks.remove(lockToRemove);
            m_InactiveLocks.add(lockToRemove);

            if (m_ActiveLocks.isEmpty())
            {
                enableLowPowerMode();
            }
        }
        else
        {
            m_Logging.error("Unable to find lock to remove [%s]", lock.toString());
        }
    }

    /**
     * Schedules a {@link WakeLock} to activate at a given time in the future for a given duration
     * @param lock
     *      the lock to activate
     * @param startLockTimeMs
     *      the start time, in milliseconds since epoch, to activate the lock
     * @param duration
     *      the duration of the active lock
     */
    public void scheduleWakelock(WakeLock lock, long startLockTimeMs, long duration)
    {
        WakeLocks newLock = new WakeLocks(lock, startLockTimeMs, duration);
        m_ScheduledLocks.add(newLock);
    }    
    
    /**
     * removes a scheduled {@link WakeLock} from the list of upcoming locks
     * @param lock
     *      the lock to remove
     */
    public void unscheduleWakelock(WakeLock lock)
    {
        WakeLocks lockToRemove = findMatchingLock(lock, m_ScheduledLocks);
        if (lockToRemove != null)
        {
            m_ScheduledLocks.remove(lockToRemove);
            m_InactiveLocks.add(lockToRemove);
        }
        else
        {
            m_Logging.error("Unable to find lock to remove [%s]", lock.toString());
        }        
    }
   
    /**
     * Sets a {@link PhysicalLink} as a valid wake source for the platform
     * @param link
     *      the PhysicalLink to set
     * @throws IllegalStateException
     *      if the PhysicalLink is already enabled as a wake source
     */
    public void enableWakeSource(PhysicalLink link) throws IllegalStateException
    {
        if (m_WakeSources.contains(link))
        {
            throw new IllegalStateException(String.format("Requested wake source [%s] already enabled.", 
                    link.getName()));
        }
        
        m_WakeSources.add(link);
        m_Logging.log(LogService.LOG_INFO, "Wake source [%s] enabled.", link.getName());
    }
    
    /**
     * Removes a {@link PhysicalLink} from the list of valid wake sources
     * @param link
     *      the PhysicalLink to remove
     * @throws IllegalStateException
     *      if the PhysicalLink given is not a valid wake source
     */
    public void disableWakeSource(PhysicalLink link) throws IllegalStateException
    {
        if (m_WakeSources.contains(link))
        {
            m_WakeSources.remove(link);
            m_Logging.log(LogService.LOG_INFO, "Wake source [%s] removed.", link.getName());
        }
        else
        {
            throw new IllegalStateException(String.format("Requested wake source [%s] not currently enabled.", 
                    link.getName()));
        }
    }
    
    /**
     * Gets the set of the names of all the registered {@link PhysicalLink} wake sources on the platform
     * @return
     *      the {@link Set} of PhysicalLink names
     */
    public Set<String> getPhysicalLinkWakeSourceNames()
    {
        Set<String> sourceNames = new HashSet<String>();
        for(PhysicalLink link : m_WakeSources)
        {
            sourceNames.add(link.getName());
        }
        
        return sourceNames;
    }
    
    /**
     * Gets the set of all active {@link WakeLock}s on the platform
     * @return
     *      the {@link Set} of active locks
     */
    public Set<WakeLock> getActiveWakeLocks()
    {
        Set<WakeLock> locksSet = new HashSet<WakeLock>();
        for (WakeLocks lock : m_ActiveLocks)
        {
            locksSet.add(lock.getLock());
        }
        
        return locksSet;
    }

    /**
     * Gets the set of all scheduled {@link WakeLock}s on the platform
     * @return
     *      the {@link Set} of scheduled locks
     */
    public Set<WakeLock> getScheduledWakeLocks()
    {
        Set<WakeLock> locksSet = new HashSet<WakeLock>();
        for (WakeLocks lock : m_ScheduledLocks)
        {
            locksSet.add(lock.getLock());
        }
        
        return locksSet;
    }
    
    /**
     * Gets the battery amp hours remaining
     * @return
     *      battery amp hours
     */
    public int getBatteryAmpHoursRem()
    {
        return m_BatteryAmpHoursRem;
    }
    
    /**
     * Gets the current battery voltage
     * @return
     *      battery voltage
     */
    public int getBatteryVoltage()
    {
        return m_BatteryVoltage;
    }
    
    /**
     * Disables low power mode on the platform
     */
    public void disableLowPowerMode()
    {
        m_Logging.debug("Disabling low power mode.");
        m_PowerModeHandler.cancel(true);
        m_Logging.debug("System now in normal power mode.");
    }
    
    /**
     * Enables low power mode on the platform
     */
    public void enableLowPowerMode()
    {
        if (m_PowerModeHandler == null || m_PowerModeHandler.isDone())
        {
            m_Logging.debug("Enabling low power mode.");
            Long shutdownDelay = m_StandbyNoticeTimeMs;
            
            final Map<String, Object> eventProps = new HashMap<>();
            eventProps.put(PlatformPowerManager.EVENT_PROP_STANDBY_TIME, shutdownDelay);
            
            final Event lowPowerModeEvent = new Event(PlatformPowerManager.TOPIC_STANDBY_NOTICE_EVENT, eventProps);
            m_EventAdmin.postEvent(lowPowerModeEvent);
            
            // Schedule shutdown       
            if(m_LowPowerThread == null)
            {
                m_LowPowerThread = new Thread(new ShutdownTask(shutdownDelay, m_Logging, this, m_EventAdmin));
            }
        
            m_PowerModeHandler = 
                m_Scheduler.scheduleWithFixedDelay(m_LowPowerThread, 0, m_StartupTimeMs, MILLISECONDS);
            
            //Schedule a wake up for the max sleep time
            m_Scheduler.schedule(new Runnable()
            {            
                @Override
                public void run()
                {                
                    disableLowPowerMode();
                }
            }, m_MaxSleepTimeMs, MILLISECONDS);
        }
    }

    @Override
    public void handleEvent(Event event)
    { 
        if (event.getTopic().equals(TOPIC_SCHEDULE_WAKE_UP_EVENT))
        {
            final WakeLocks scheduledLock = (WakeLocks)event.getProperty("wakeLock"); 
            Long startTime = scheduledLock.getStartTime();
            Long duration = scheduledLock.getDuration();
            Long delay = startTime - System.currentTimeMillis();
            
            
            m_Scheduler.schedule(new Runnable()
            {                
                @Override
                public void run()
                {          
                    final Event wakeupEvent = 
                            new Event( PlatformPowerManager.TOPIC_WAKEUP_EVENT, (Map<String, Object>)null);
                    m_EventAdmin.postEvent(wakeupEvent);
                    m_Logging.debug("Scheduled wake lock [%s] activating.", scheduledLock.getLock().getId());
                    disableLowPowerMode();                    
                }
            }, delay, MILLISECONDS);
            
            m_Scheduler.schedule(new Runnable()
            {                
                @Override
                public void run()
                {          
                    m_Logging.debug("Scheduled lock for [%s] expired, system entering low power again.", 
                            scheduledLock.getLock().getId());
                    m_ScheduledLocks.remove(scheduledLock);
                    enableLowPowerMode();
                }
            }, delay + duration, MILLISECONDS);
            
            m_Logging.debug("%s received! Scheduling wake trigger for %s (%s elapsed time)", 
                    TOPIC_SCHEDULE_WAKE_UP_EVENT, startTime, delay);            
        }
        
        if (event.getTopic().equals(TOPIC_WAKESOURCE_ACTIVITY_DETECTED_EVENT))
        {
            PhysicalLink source = (PhysicalLink)event.getProperty("wakeSource");
            
            m_Logging.debug("Activity detected on [%s]!", source.getName());
            
            if (m_WakeSources.contains(source))
            {
                final Event wakeupEvent = 
                        new Event( PlatformPowerManager.TOPIC_WAKEUP_EVENT, (Map<String, Object>)null);
                m_EventAdmin.postEvent(wakeupEvent);
                
                disableLowPowerMode();
                
                m_Scheduler.schedule(new Runnable()
                {                    
                    @Override
                    public void run()
                    {
                        m_Logging.debug("Min Wake Time elapsed after physical link activity.");
                        enableLowPowerMode();                        
                    }
                }, m_MinWakeTimeMs, MILLISECONDS);
            }
            else
            {
                m_Logging.debug("PhysicalLink [%s] not an activate wake source, ignoring.", source.getName());
            }
        }
    }

    /**
     * Gets the set of {@link WakeLocks} that are currently active. This is different than the set of {@link WakeLock} 
     * requested by the {@link PlatformPowerManager} and is only used internally in the OS/hardware code.
     * @return
     *      the set of {@link WakeLocks}
     */
    public Set<WakeLocks> getActiveWakeLocksSet()
    {
        return m_ActiveLocks;
    }

    /**
     * Gets the set of {@link WakeLocks} that are currently scheduled. This is different than the set of 
     * {@link WakeLock} 
     * requested by the {@link PlatformPowerManager} and is only used internally in the OS/hardware code.
     * @return
     *      the set of {@link WakeLocks}
     */
    public Set<WakeLocks> getScheduledWakeLocksSet()
    {
        return m_ScheduledLocks;
    } 
    
    /**
     * Find the {@link WakeLocks} associated with a given {@link WakeLock} instance in a given {@link Set}
     * @param lock
     *      the {@link WakeLock} for which to find a match
     * @param wakeLocksSet
     *      the {@link Set} to search
     * @return
     *      the matching {@link WakeLocks} object or null if no match was found
     */
    private WakeLocks findMatchingLock(WakeLock lock, Set<WakeLocks> wakeLocksSet)
    {
        for (WakeLocks setLock : wakeLocksSet)
        {
            if (setLock.getLock() == lock)
            {
                return setLock;
            }
        }
        
        return null;
    }
}

/**
 * Internal class to store details of a {@link WakeLock} including the lock itself, the start time and the duration 
 * of the lock. This is used by the OS/hardware code to manage low power mode switching and is not exposed to the
 * {@link PlatformPowerManager}
 * 
 * @author Josh
 *
 */
class WakeLocks
{
    /**
     * Reference to the {@link WakeLock} associated with this WakeLocks object
     */
    private WakeLock m_Lock;
    
    /** The start time of the WakeLock in milliseconds since epoch */
    private Long m_StartTime;
    
    /** The duration of the WakeLock in milliseconds */
    private Long m_Duration;
    
    WakeLocks(WakeLock lock, Long startTime, Long duration)
    {
        m_Lock = lock;
        m_StartTime = startTime;
        m_Duration = duration;
    }
    
    /**
     * Gets the {@link WakeLock} object
     * @return
     *      the WakeLock associated with this WakeLocks instance
     */
    public WakeLock getLock()
    {
        return m_Lock;
    }
    
    /**
     * Gets the start time of this WakeLock
     * @return
     *      the start time in milliseconds since epoch
     */
    public Long getStartTime()
    {
        return m_StartTime;
    }
    
    /**
     * Gets the duration of this lock
     * @return
     *      the duration in milliseconds
     */
    public Long getDuration()
    {
        return m_Duration;
    }
}

/**
 * Internal {@link Thread} class used by the OS/hardware to time low power related methods and events
 * @author Josh
 *
 */
class ShutdownTask implements Runnable
{
    /**
     * Comparator to sort {@link WakeLocks} based on starting time
     */
    private Comparator<WakeLocks> wakeTimeComparator = new Comparator<WakeLocks>()
    {
        @Override
        public int compare(WakeLocks o1, WakeLocks o2)
        {
            return new Long(o1.getStartTime()).compareTo(o2.getStartTime());
        }
    };

    private long m_ShutdownDelay;
    private LoggingService m_Logging;
    private ExamplePlatformKernel m_Kernel;
    private EventAdmin m_EventAdmin;
    private boolean m_LowPowerModeActive;

    ShutdownTask(Long delay, LoggingService logger, ExamplePlatformKernel kernel, EventAdmin eventAdmin)
    {
        m_Kernel = kernel;
        m_ShutdownDelay = delay;
        m_Logging = logger;
        m_EventAdmin = eventAdmin;
    }

    @Override
    public void run()
    {
        try
        {  
            if (!m_LowPowerModeActive)
            {     
                m_Logging.debug("Low power mode countdown started...");
                Thread.sleep(m_ShutdownDelay);

                Set<WakeLocks> activeLocks = m_Kernel.getActiveWakeLocksSet();
                Set<WakeLocks> scheduledLocks = m_Kernel.getScheduledWakeLocksSet();
                Set<String> wakeSources = m_Kernel.getPhysicalLinkWakeSourceNames();
                
                if (activeLocks.isEmpty() && !scheduledLocks.isEmpty())
                {
                    // Go into low power mode until first scheduled wake lock start time and duration
                    List<WakeLocks> sortedLocks = sortSetByWakeTime(scheduledLocks); 
                    m_Logging.debug("Entering low power mode until scheduled wake lock activation. WakeupTime [%s]", 
                            sortedLocks.get(0).getStartTime());
                    
                    Map<String, Object> eventProps = new HashMap<>();
                    eventProps.put("wakeLock", sortedLocks.get(0));
                    
                    Event scheduleWakeUpEvent = 
                            new Event(ExamplePlatformKernel.TOPIC_SCHEDULE_WAKE_UP_EVENT, eventProps);
                    m_EventAdmin.sendEvent(scheduleWakeUpEvent);
                    
                    // Sleep until SCHEDULE_WAKE_UP event fires
                    m_LowPowerModeActive = true;
                }
                else if (activeLocks.isEmpty() && scheduledLocks.isEmpty() && !wakeSources.isEmpty())
                {
                    // Sleep until activity on a wake source
                    m_Logging.debug("Entering low power mode until WakeSource activity.");
                    m_LowPowerModeActive = true;
                }
                else if (!activeLocks.isEmpty())
                {
                    // Do nothing
                    m_Logging.debug("System has %s active wake locks, cancelling low power mode request.", 
                            activeLocks.size());
                    m_LowPowerModeActive = false;
                    m_Kernel.disableLowPowerMode();
                }   
                else if (activeLocks.isEmpty() && scheduledLocks.isEmpty() && wakeSources.isEmpty())
                {
                    // Do nothing and show error
                    m_Logging.error("No active or scheduled locks and no wake sources enabled. "
                            + "In this state the system will never wake up. Cancelling low power mode request.");
                    m_LowPowerModeActive = false;
                    m_Kernel.disableLowPowerMode();
                }
                
            }
            else
            {
                m_Logging.debug("System running in low power mode.");
            }
        }
        catch (InterruptedException e)
        {
            m_Logging.debug("Low Power mode countdown interrupted!");
            m_LowPowerModeActive = false;
        }                    
    }

    /**
     * Sorts a {@link Set} of {@link WakeLocks} by starting time and returns the sorted list
     * @param scheduledLocks
     *      the set of WakeLocks to sort
     * @return
     *      the sorted {@link List} of WakeLocks
     */
    private List<WakeLocks> sortSetByWakeTime(Set<WakeLocks> scheduledLocks)
    {
        List<WakeLocks> list = new ArrayList<WakeLocks>(scheduledLocks);
        Collections.sort(list, wakeTimeComparator);
        return list;
    }          
}
