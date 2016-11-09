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

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.service.command.Descriptor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;


/**
 * Contains debug commands to demonstrate platform power manager functions.
 * @author Josh
 *
 */
@Component(provide = DebugCommands.class, 
    properties = { "osgi.command.scope=ppm", 
    "osgi.command.function=enableLowPowerMode|disableLowPowerMode|createWakeLock|removeWakeLock|addWakeSource|"
    + "removeWakeSource|simulateWakeActivity|scheduleLock|getPlatformConfiguration" })
public class DebugCommands
{
    /** Used to log messages. */
    private LoggingService m_Logging;
    
    /** Reference to OSUS platform power manager instance */
    private ExamplePlatformPowerManager m_PlatformPowerManager;
    private ExamplePlatformKernel m_Kernel;
    private PowerManager m_CorePowerManager;
    
    /**
     * Reference to the event admin service used to post events.
     */
    private EventAdmin m_EventAdmin;
    
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
    public void setExamplePlatformKernel(final ExamplePlatformKernel kernel)
    {
        m_Kernel = kernel;
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
    
    /**
     * Binds the platform power manager implementation.
     * 
     * @param powerManager
     *      platform power manager object
     */
    @Reference
    public void setPlatformPowerManager(final PlatformPowerManager powerManager)
    {
        m_PlatformPowerManager = (ExamplePlatformPowerManager)powerManager;
    }
    
    /**
     * Binds the core power manager service
     * 
     * @param manager
     *      Power Manager object
     */
    @Reference
    public void setPowerManager(final PowerManager manager)
    {
        m_CorePowerManager = manager;
    }
    
    
/* DEBUG COMMANDS */

    /**
     * Starts the count down for entering low power mode on the platform
     */
    @Descriptor("Enables low power mode for the platform")
    public void enableLowPowerMode()
    {
        if  (m_Kernel != null)
        {
            m_Kernel.setConfiguration(m_PlatformPowerManager.getConfigurationDetails());
            m_Kernel.enableLowPowerMode();
        }
        else
        {
            m_Logging.debug("Example Platform Kernel is null");
        }
    }
    
    /**
     * Stops low power mode on the platform
     */
    @Descriptor("Disables low power mode for the platform")
    public void disableLowPowerMode()
    {
        if  (m_Kernel != null)
        {
            m_Kernel.disableLowPowerMode();
        }
        else
        {
            m_Logging.debug("Example Platform Kernel is null");
        }
    }
    
    /**
     * Helper method to create a WakeLock from the shell
     * @param lockId
     *      the identification string of the lock
     * @return
     *      the newly created WakeLock
     */
    @Descriptor("Creates a WakeLock with a given ID")
    public WakeLock createWakeLock(
            @Descriptor("The lock ID")
            final String lockId)
    {
        return m_CorePowerManager.createWakeLock(this.getClass(), lockId);
    }
    
    /**
     * Removes a WakeLock from the system
     * @param lock
     *      the WakeLock to remove
     */
    @Descriptor("Removes a WakeLock from the system")
    public void removeWakeLock(
            @Descriptor("The lock to remove")
            final WakeLock lock)
    {
        m_PlatformPowerManager.cancelWakeLock(lock);
    }
    
    /**
     * Used to simulate activity on a particular {@link PhysicalLink}
     * @param link
     *      the {@link PhysicalLink} to simulate activity on.
     */
    @Descriptor("Simulates activity on a given PhysicalLink")
    public void simulateWakeActivity(
            @Descriptor("The PhysicalLink to simulate activity on")
            final PhysicalLink link)
    {
        final Map<String, Object> eventProps = new HashMap<>();
        eventProps.put("wakeSource", link);
        
        final Event physicalLinkActivityEvent = 
                new Event(ExamplePlatformKernel.TOPIC_WAKESOURCE_ACTIVITY_DETECTED_EVENT, 
                eventProps);
        m_EventAdmin.postEvent(physicalLinkActivityEvent);
    }
    
    /**
     * Adds a {@link PhysicalLink} to the list of wake sources for the platform
     * @param link
     *      the link to add
     */
    @Descriptor("Adds a PhysicalLink to the list of wake sources for the platform")
    public void addWakeSource(
            @Descriptor("The PhysicalLink to add")
            final PhysicalLink link)
    {
        m_PlatformPowerManager.enableWakeSource(link);
    }
    
    /**
     * Removes a {@link PhysicalLink} from the list of wake sources for the platform
     * @param link
     *      the link to remove
     */
    @Descriptor("Removes a PhysicalLink from the list of wake sources for the platform")
    public void removeWakeSource(
            @Descriptor("The PhysicalLink to remove")
            final PhysicalLink link)
    {
        m_PlatformPowerManager.disableWakeSource(link);
    }
    
    /**
     * Creates a {@link Date} object for a time in the future
     * @param futureTimeInMs
     *      the amount of time in milliseconds in the future to create a date for
     * @return
     *      the newly created future Date
     */
    private Date createFutureDate(final long futureTimeInMs)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.MILLISECOND, (int)futureTimeInMs);
        return cal.getTime();
    }
    
    /**
     * Schedules a WakeLock to activate at a time in the future
     * @param lock
     *      the WakeLock to schedule to activate in the future
     * @param timeDelayInSeconds
     *      the time from now when the wake lock should activate, in seconds
     */
    @Descriptor("Schedules a WakeLock to activate at a time in the future")
    public void scheduleLock(
            @Descriptor("The WakeLock to schedule to activate in the future")
            final WakeLock lock, 
            @Descriptor("the time from now when the wake lock should activate, in seconds")
            final long timeDelayInSeconds)
    {
        Date futureDate = createFutureDate(timeDelayInSeconds * 1000);
        lock.scheduleWakeTime(futureDate);
    }
    
    /**
     * Outputs the current platform power manager configuration values
     */
    @Descriptor("Outputs the current platform power manager configuration values")
    public void getPlatformConfiguration()
    {
        ExamplePlatformPowerManagerConfig config = m_PlatformPowerManager.getConfigurationDetails();
        m_Logging.debug("===== Platform Power Manager Configuration Values =====");
        m_Logging.debug("enabled: [%s]", config.enabled());
        m_Logging.debug("min.wake.time: [%s]", config.minWakeTimeMs());
        m_Logging.debug("standby.notice.time.ms: [%s]", 
                config.standbyNoticeTimeMs());
        m_Logging.debug("max.sleep.time.ms: [%s]", config.maxSleepTimeMs());
        m_Logging.debug("startup.time.ms: [%s]", config.startupTimeMs());
        m_Logging.debug("==========");
    }
}
