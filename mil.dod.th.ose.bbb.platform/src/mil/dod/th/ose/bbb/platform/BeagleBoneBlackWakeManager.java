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
package mil.dod.th.ose.bbb.platform;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.TimerTask;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.ose.bbb.platform.BeagleBoneBlackGpioManager.GpioState;
import mil.dod.th.ose.utils.ProcessService;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Timer task that handles putting the system to sleep.
 * 
 * @author cweisenborn
 */
@Component(provide = BeagleBoneBlackWakeManager.class)
public class BeagleBoneBlackWakeManager extends TimerTask implements BeagleBoneBlackLockListener
{
    private static final int SECOND_MS = 1000;
    private static final int POWER_STATUS_PIN = 7;
    
    private LoggingService m_LogService;
    private EventAdmin m_EventAdmin;
    private ProcessService m_ProcessService;
    private BeagleBoneBlackLockManager m_BeagleBoneBlackWakeLockMgr;
    private BeagleBoneBlackGpioManager m_BegaleBoneBlackGpioMgr;
    
    private Long m_WakeTime = System.currentTimeMillis();
    private Long m_StandbyTime = PlatformPowerManager.INDEFINITE;
    
    private boolean m_IsEnabled;
    private long m_MinWakeTimeMs;
    private long m_MaxSleepTimeMs;
    private long m_StandbyNoticeTimeMs;
    private long m_StandbyBackoffTimeMs;
    private BeagleBoneBlackStandbyMode m_StandbyMode;
    
    @Reference
    public void setLoggingService(final LoggingService logService)
    {
        m_LogService = logService;
    }
    
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    @Reference
    public void setProcessService(final ProcessService processService)
    {
        m_ProcessService = processService;
    }
    
    @Reference
    public void setBeagleBoneBlackWakeLockMgr(final BeagleBoneBlackLockManager wakeLockMgr)
    {
        m_BeagleBoneBlackWakeLockMgr = wakeLockMgr;
    }
    
    @Reference
    public void setBeagleBoneBlackGpioMgr(final BeagleBoneBlackGpioManager gpioMgr)
    {
        m_BegaleBoneBlackGpioMgr = gpioMgr;
    }
    
    /**
     * Activate method. Sets the power status GPIO pin to the high state.
     */
    @Activate
    public void activate()
    {
        m_BeagleBoneBlackWakeLockMgr.registerWakeLockAddedListener(this);
        m_BegaleBoneBlackGpioMgr.setGpioState(POWER_STATUS_PIN, GpioState.HIGH);
    }
    
    /**
     * Deactivate method. Sets the power status GPIO pin to the low state.
     */
    @Deactivate
    public void deactivate()
    {
        m_BegaleBoneBlackGpioMgr.setGpioState(POWER_STATUS_PIN, GpioState.LOW);
    }
    
    /**
     * Updates the beaglebone wake timer with the specified configuration properties.
     * 
     * @param config
     *      Configuration that contains the properties the wake timer should be updated with.
     */
    public void update(final BeagleBoneBlackPowerManagerConfig config)
    {
        m_MinWakeTimeMs = config.minWakeTimeMs();
        m_MaxSleepTimeMs = config.maxSleepTimeMs();
        m_StandbyNoticeTimeMs = config.standbyNoticeTimeMs();
        m_StandbyBackoffTimeMs = config.standbyBackoffTimeMs();
        m_StandbyMode = config.standbyMode();
        if (!m_IsEnabled && config.enabled())
        {
            m_WakeTime = System.currentTimeMillis();
        }
        m_IsEnabled = config.enabled();
    }
    
    public long getStandbyTime()
    {
        return m_StandbyTime;
    }
    
    @Override
    public void activeWakeLockAdded()
    {
        if (m_StandbyTime != PlatformPowerManager.INDEFINITE 
                && !m_BeagleBoneBlackWakeLockMgr.getActiveLocks().isEmpty())
        {
            handleCancelStandby();
        }
    }
    
    @Override
    public void run()
    {
        if (m_IsEnabled)
        {
            if (System.currentTimeMillis() - m_WakeTime >= m_MinWakeTimeMs 
                    && m_StandbyTime == PlatformPowerManager.INDEFINITE
                    && m_BeagleBoneBlackWakeLockMgr.getActiveLocks().isEmpty())
            {
                m_StandbyTime = System.currentTimeMillis() + m_StandbyNoticeTimeMs;
                final Dictionary<String, Object> props = new Hashtable<>();
                props.put(PlatformPowerManager.EVENT_PROP_STANDBY_TIME, m_StandbyTime);
                final Event standbyEvent = new Event(PlatformPowerManager.TOPIC_STANDBY_NOTICE_EVENT, props);
                m_LogService.debug("Posting standby event...");
                m_EventAdmin.sendEvent(standbyEvent);
                m_LogService.debug("Standby event posted...");
            }
            else if (m_BeagleBoneBlackWakeLockMgr.getActiveLocks().isEmpty()
                    && m_StandbyTime != PlatformPowerManager.INDEFINITE 
                    && System.currentTimeMillis() >= m_StandbyTime)
            {
                try
                {
                    handleRtcWakeCommand();
                }
                catch (final IOException ex)
                {
                    m_LogService.error(ex, "Unable to execute RTC wake command.");
                    return;
                }
                
                final Dictionary<String, Object> props = new Hashtable<>();
                props.put(PlatformPowerManager.EVENT_PROP_WAKEUP_TIME, System.currentTimeMillis());
                final Event wakeEvent = new Event(PlatformPowerManager.TOPIC_WAKEUP_EVENT, props);
                m_EventAdmin.postEvent(wakeEvent);
                m_LogService.debug("Wake event posted...");
                
                m_WakeTime = System.currentTimeMillis();
                m_StandbyTime = PlatformPowerManager.INDEFINITE;
            }
            else if (m_StandbyTime != PlatformPowerManager.INDEFINITE 
                    && !m_BeagleBoneBlackWakeLockMgr.getActiveLocks().isEmpty())
            {
                handleCancelStandby();
            }
        }
    }
    
    /**
     * Method that handles issuing an RTC wake command.
     * 
     * @throws IOException
     *      Thrown if the RTC wake command cannot be issued or if the power status GPIO cannot be set to high 
     *      or low.
     */
    private void handleRtcWakeCommand() throws IOException
    {
        try
        {
            m_BegaleBoneBlackGpioMgr.setGpioState(POWER_STATUS_PIN, GpioState.LOW);
        }
        catch (final IllegalStateException ex)
        {
            m_LogService.warning(ex, "Cannot set wake status GPIO to low.");
        }
        
        final long sleepTime = determineSleepTime();
        final List<String> args = new ArrayList<String>();
        args.add("bash");
        args.add("-c");
        args.add(String.format("rtcwake -d rtc0 -m %s -s %s", m_StandbyMode.toString(), sleepTime));
        final Process process;

        m_LogService.debug(String.format("Executing RTC wake command with standby mode [%s] "
                + "for [%s] seconds...", m_StandbyMode.toString(), sleepTime));
        process = m_ProcessService.createProcess(args);
       
        try
        {
            m_LogService.debug("Waiting for RTC wake command...");
            process.waitFor();
        }
        catch (final InterruptedException ex)
        {
            m_LogService.warning(ex, "Wait for RTC wake interrupted.");
        }
        
        try
        {
            m_BegaleBoneBlackGpioMgr.setGpioState(POWER_STATUS_PIN, GpioState.HIGH);
        }
        catch (final IllegalStateException ex)
        {
            m_LogService.warning(ex, "Cannot set wake status GPIO to high.");
        }
    }
    
    /**
     * Method that handles canceling system standby.
     */
    private void handleCancelStandby()
    {
        m_LogService.debug("Canceling standby...");
        m_WakeTime = System.currentTimeMillis() - m_MinWakeTimeMs + m_StandbyBackoffTimeMs;
        m_StandbyTime = PlatformPowerManager.INDEFINITE;
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(PlatformPowerManager.EVENT_PROP_CANCELLED_TIME, System.currentTimeMillis());
        final Event canceledEvent = 
                new Event(PlatformPowerManager.TOPIC_STANDBY_CANCELLED_EVENT, props);
        m_EventAdmin.postEvent(canceledEvent);
    }
    
    /**
     * Method that determine the amount of time system should sleep for based on the currently scheduled wake locks.
     * 
     * @return
     *      Time in seconds in the system should sleep.
     */
    private long determineSleepTime()
    {
        long sleepTime = m_MaxSleepTimeMs / SECOND_MS;
        
        final BeagleBoneBlackLockInfo nextScheduledLock = m_BeagleBoneBlackWakeLockMgr.getNextScheduledLock();
        if (nextScheduledLock != null)
        {
            final long timeOffset = (nextScheduledLock.getStartTimeMs() - System.currentTimeMillis()) / SECOND_MS;
            if (timeOffset > 0 && timeOffset < sleepTime)
            {
                sleepTime = timeOffset;
            }
        }
        return sleepTime;
    }
}
