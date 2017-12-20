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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;

/**
 * Provides an implementation of {@link PlatformPowerManager} for integration test purposes.
 * 
 * @author jlatham
 */
@Component(provide = PlatformPowerManager.class, name = PlatformPowerManager.CONFIG_PID,
           designate = ExamplePlatformPowerManagerConfig.class, configurationPolicy = ConfigurationPolicy.optional)
public class ExamplePlatformPowerManager implements PlatformPowerManager, EventHandler
{
    /** 
     * Used to log messages. 
     */
    private LoggingService m_Logging;
    
    /**
     * Reference to the OS/hardware level example kernel code
     */
    private ExamplePlatformKernel m_ExamplePlatformKernel;
    
    /** 
     * Reference to registered event handler registrations 
     */
    private ServiceRegistration<EventHandler> m_Registration;
    
    /**
     * Reference to the {@link ScheduledExecutorService} used to manage initial startup delay 
     */
    private ScheduledExecutorService m_Scheduler;
    
    /**
     * Flag indicating whether the platform power manager is enabled
     */
    private boolean m_Enabled;
    
    /**
     * Minimum time to stay awake, in milliseconds
     */
    private long m_MinWakeTimeMs;
    
    /**
     * Standby notice time delay, in milliseconds
     */
    private long m_StandbyNoticeTimeMs;
    
    /**
     * Maximum sleep time, in milliseconds
     */
    private long m_MaxSleepTimeMs;
    
    /**
     * Startup sleep delay time, in milliseconds
     */
    private long m_StartupTimeMs;

    /**
     * Keeps a log of power management method calls made for testing purposes.
     */
    private List<ExamplePowerManagerMethodLog> m_MethodLog;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    @Reference
    public void setPlatformKernel(final ExamplePlatformKernel kernel)
    {
        m_ExamplePlatformKernel = kernel;
    }

    @Activate
    public void activate(final BundleContext context, final Map<String, Object> props)
    {
        m_MethodLog = new ArrayList<>();
        m_Registration = registerEvents(context);
        m_Scheduler = Executors.newScheduledThreadPool(2);
        update(props);
    }
    
    @Modified
    public void modified(final Map<String, Object> props)
    {
        update(props);
    }
    
    @Deactivate
    public void deactivate()
    {
        m_Registration.unregister();
        m_MethodLog.clear();
    }
     
    private void update(final Map<String, Object> props)
    {
        final ExamplePlatformPowerManagerConfig config = Configurable
                .createConfigurable(ExamplePlatformPowerManagerConfig.class, props);
        m_Enabled = config.enabled();
        m_MinWakeTimeMs = config.minWakeTimeMs();
        m_StandbyNoticeTimeMs = config.standbyNoticeTimeMs();
        m_MaxSleepTimeMs = config.maxSleepTimeMs();
        m_StartupTimeMs = config.startupTimeMs();
        
        if (m_Enabled)
        {
            // Schedule low power mode for after startup delay
            m_Scheduler.schedule(new Runnable()
            {                
                @Override
                public void run()
                {
                    m_ExamplePlatformKernel.enableLowPowerMode();                    
                }
            }, m_StartupTimeMs, TimeUnit.MILLISECONDS);          
        }
        
        // Notify platform of the config settings.
        m_ExamplePlatformKernel.setConfiguration(config);
    }
    
    @Override
    public int getBatteryAmpHoursRem()
    {
        return m_ExamplePlatformKernel.getBatteryAmpHoursRem();
    }

    @Override
    public int getBatteryVoltage()
    {
        return m_ExamplePlatformKernel.getBatteryVoltage();
    }

    @Override
    public void enableWakeSource(PhysicalLink link) throws IllegalArgumentException, IllegalStateException
    {
        m_ExamplePlatformKernel.enableWakeSource(link);
    }

    @Override
    public void disableWakeSource(PhysicalLink link) throws IllegalStateException
    {
        m_ExamplePlatformKernel.disableWakeSource(link);
    }

    @Override
    public Set<String> getPhysicalLinkWakeSourceNames()
    {
        return m_ExamplePlatformKernel.getPhysicalLinkWakeSourceNames();
    }

    /**
     * Gets the current Set of WakeLocks which have been activated by the OS/hardware.
     */
    @Override
    public Set<WakeLock> getActiveWakeLocks()
    {
        return m_ExamplePlatformKernel.getActiveWakeLocks();
    }

    /**
     * Gets the current Set of scheduled, but not active, WakeLocks which have been registered with the OS/hardware.
     */
    @Override
    public Set<WakeLock> getScheduledWakeLocks()
    {
        return m_ExamplePlatformKernel.getScheduledWakeLocks();
    }

    /**
     * Calls the OS/hardware code to activate a WakeLock for a given time, preventing the system from going into low
     * powered more. Having one or more WakeLoacks active will prevent the OS/hardware from going into low power 
     * standby mode.
     */
    @Override
    public long activateWakeLock(WakeLock lock, long startLockTimeMs, long stopLockTimeMs)
    {
        ExamplePowerManagerMethodLog methodLog = new ExamplePowerManagerMethodLog();
        methodLog.setWakeLockId(lock.getId());
        methodLog.setStartTimeMs(startLockTimeMs);
        methodLog.setEndtimeMs(stopLockTimeMs);                
        logMethodCall(lock.getId(), methodLog, ExamplePowerManagerMethodLog.MethodCalled.Activate);

        Calendar calendar = Calendar.getInstance();        
        long currentTimeMs = calendar.getTimeInMillis();
        long duration = stopLockTimeMs - startLockTimeMs;
        
        if (duration < 0)
        {
            duration = PlatformPowerManager.INDEFINITE;
        }
        
        if (duration < m_MinWakeTimeMs && duration > 0)
        {
            duration = m_MinWakeTimeMs;
        }
        
        if(startLockTimeMs <= currentTimeMs)
        {
            m_Logging.debug("Start time [%s] less than current time [%s]. Activating lock now.", 
                    startLockTimeMs, currentTimeMs);
            m_ExamplePlatformKernel.activateWakelock(lock, duration);
        }
        else
        {
            m_Logging.debug("Start time [%s] is in the future. Scheduling lock activation for the future.", 
                    startLockTimeMs);
            m_ExamplePlatformKernel.scheduleWakelock(lock, startLockTimeMs, duration);
        }
        
        m_Logging.log(LogService.LOG_INFO, "WakeLock [%s] will be active for %d ms.", lock.getId(), duration);
        return duration;
    }

    /**
     * Calls the OS/hardware code to cancel a given active or scheduled WakeLock.
     */
    @Override
    public void cancelWakeLock(WakeLock lock) throws IllegalStateException
    {
        ExamplePowerManagerMethodLog methodLog = new ExamplePowerManagerMethodLog();
        methodLog.setWakeLockId(lock.getId());
        logMethodCall(lock.getId(), methodLog, ExamplePowerManagerMethodLog.MethodCalled.Cancel);        

        if(getActiveWakeLocks().contains(lock))  
        {    
            m_ExamplePlatformKernel.deactivateWakelock(lock);
            m_Logging.log(LogService.LOG_INFO, "Cancelling Active WakeLock for [%s]", lock.getId());
        }
        else if(getScheduledWakeLocks().contains(lock))
        {
            m_ExamplePlatformKernel.unscheduleWakelock(lock);
            m_Logging.log(LogService.LOG_INFO, "Cancelling Scheduled WakeLock for [%s]", lock.getId());
        }
        else
        {
            throw new IllegalStateException("WakeLock not in active or scheduled state.");
        }
    }
    
    /**
     * Registers the platform power manager to listen for 
     * {@link PlatformPowerManager#TOPIC_STANDBY_NOTICE_EVENT} and then handle them.
     * @param context 
     *      the bundle context to register with
     * @return 
     *      Service Registration reference
     */
    public ServiceRegistration<EventHandler> registerEvents(final BundleContext context)
    {
        final String configEvents = ExamplePlatformPowerManager.TOPIC_STANDBY_NOTICE_EVENT;
        
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC, configEvents);
        
        return context.registerService(EventHandler.class, this, props);
    }

    /**
     * Retrieve the log of power management methods called. Used for testing purposes.
     * 
     * @return
     *      array of method log calls
     */
    public ExamplePowerManagerMethodLog[] getMethodLog()
    {
        return m_MethodLog.toArray(new ExamplePowerManagerMethodLog[]{});
    }
    
    /**
     * Clear the power management method call log.
     */
    public void clearMethodLog()
    {
        m_MethodLog.clear();
    }

    /**
     * Writes an {@link ExamplePowerManagerMethodLog} to the persistent data store.
     * 
     * @param description
     *      the description to use for querying purposes
     * @param methodLog
     *      the {@link ExamplePowerManagerMethodLog} object to persist
     * @param caller
     *      the method called
     */
    private void logMethodCall(String description, ExamplePowerManagerMethodLog methodLog, 
            ExamplePowerManagerMethodLog.MethodCalled caller)
    {           
        methodLog.setCalledMethod(caller);

        m_MethodLog.add(methodLog);
    }

    /**
     * Gets the current values of the configuration properties for the {@link PlatformPowerManager}
     * @return
     *      the {@link ExamplePlatformPowerManagerConfig} of the current configuration property values
     */
    public ExamplePlatformPowerManagerConfig getConfigurationDetails()
    {
        Map<String, Object> props = new HashMap<>();
        props.put(PlatformPowerManager.CONFIG_PROP_ENABLED, m_Enabled);
        props.put(PlatformPowerManager.CONFIG_PROP_MAX_SLEEP_TIME_MS, m_MaxSleepTimeMs);
        props.put(PlatformPowerManager.CONFIG_PROP_MIN_WAKE_TIME_MS, m_MinWakeTimeMs);
        props.put(PlatformPowerManager.CONFIG_PROP_STARTUP_TIME_MS, m_StartupTimeMs);
        props.put(PlatformPowerManager.CONFIG_PROP_STDBY_NOTICE_TIME_MS, m_StandbyNoticeTimeMs);
        
        ExamplePlatformPowerManagerConfig config = Configurable
                .createConfigurable(ExamplePlatformPowerManagerConfig.class, props);
        
        return config;        
    }
    
/** EVENT HANDLER */
    @Override
    public void handleEvent(Event event)
    {
        if (event.getTopic().equals(PlatformPowerManager.TOPIC_STANDBY_NOTICE_EVENT))
        {
            m_Logging.debug("Received [%s], shutdown in %s", event.getTopic(),
                    TimeUnit.MILLISECONDS.toSeconds((Long)event.getProperty(EVENT_PROP_STANDBY_TIME)));
        }
        
        if (event.getTopic().equals(PlatformPowerManager.TOPIC_WAKEUP_EVENT))
        {
            m_Logging.debug("Received [%s], system awoken.", event.getTopic());
        }
    }
    
}
