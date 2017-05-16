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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.ConfigurationPolicy;
import aQute.bnd.annotation.component.Modified;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.utils.FileService;

/**
 * Beagle bone black specific implementation of the {@link PlatformPowerManager}.
 * 
 * @author cweisenborn
 */
@Component(immediate = true, designate = BeagleBoneBlackPowerManagerConfig.class, 
    configurationPolicy = ConfigurationPolicy.optional, provide = {PlatformPowerManager.class, 
            BeagleBoneBlackPowerManager.class})
public class BeagleBoneBlackPowerManager implements PlatformPowerManager
{  
    private static final String ILLEGAL_WAKE_SOURCE = "Specified physical link [%s] is not a valid wake source.";
    private static final int SECOND_MS = 1000;
    
    private Map<String, String> m_SupportedLinks;
    private Set<String> m_WakeSources;
    
    private LoggingService m_LogService;
    private FileService m_FileService;
    private BeagleBoneBlackTimerManager m_TimerMgr;
    private BeagleBoneBlackWakeManager m_WakeMgr;
    private BeagleBoneBlackLockManager m_LockMgr;
    
    @Reference
    public void setLogService(final LoggingService logService)
    {
        m_LogService = logService;
    }
    
    @Reference
    public void setFileService(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    @Reference
    public void setBeagleBoneBlackTimerManager(final BeagleBoneBlackTimerManager beagleBoneTimerMgr)
    {
        m_TimerMgr = beagleBoneTimerMgr;
    }
    
    @Reference
    public void setBeagleBoneBlackWakeLockManager(final BeagleBoneBlackLockManager beagleBoneWakeLockMgr)
    {
        m_LockMgr = beagleBoneWakeLockMgr;
    }
    
    @Reference
    public void setBeagleBoneBlackWakeTimer(final BeagleBoneBlackWakeManager beagleBoneWakeMgr)
    {
        m_WakeMgr = beagleBoneWakeMgr;
    }
    
    /**
     * Method that is called upon activation of the service.
     * 
     * @param props
     *      Map that contains the initial configuration properties for the service.
     */
    @Activate
    public void activate(final Map<String, Object> props)
    {
        m_WakeSources = new HashSet<>();
        m_SupportedLinks = new HashMap<>();
        m_SupportedLinks.put("/dev/ttyS1", "gpio0_4_wake");
        m_SupportedLinks.put("/dev/ttyS2", "gpio0_5_wake");
        
        final BeagleBoneBlackPowerManagerConfig config = 
                Configurable.createConfigurable(BeagleBoneBlackPowerManagerConfig.class, props);
        m_WakeMgr.update(config);
        
        m_TimerMgr.addScheduledAtFixedRateTask(m_LockMgr, 0, SECOND_MS);
        final long startupDelay = config.enabled() ? config.startupTimeMs() : 0;
        m_TimerMgr.addScheduledAtFixedRateTask(m_WakeMgr, startupDelay, SECOND_MS);
    }
    
    /**
     * Method that is called when the configuration is updated.
     * 
     * @param props
     *      Updated configuration properties.
     */
    @Modified
    public void updated(final Map<String, Object> props)
    {
        final BeagleBoneBlackPowerManagerConfig config = 
                Configurable.createConfigurable(BeagleBoneBlackPowerManagerConfig.class, props);
        
        m_WakeMgr.update(config);
    }

    @Override
    public void enableWakeSource(final PhysicalLink link) throws IllegalArgumentException, IllegalStateException
    {
        if (m_SupportedLinks.containsKey(link.getName()))
        {
            handleWakeSource(link, "enabled");
            m_WakeSources.add(link.getName());
            m_LogService.info("Physical link [%s] was enabled as a wake source.", link.getName());
            return;
        }
        throw new IllegalArgumentException(String.format(ILLEGAL_WAKE_SOURCE, link.getName()));
    }

    @Override
    public void disableWakeSource(final PhysicalLink link) throws IllegalStateException
    {
        if (m_SupportedLinks.containsKey(link.getName()))
        {
            handleWakeSource(link, "disabled");
            m_WakeSources.remove(link.getName());
            m_LogService.info("Physical link [%s] was disabled as a wake source.", link.getName());
            return;
        }
        throw new IllegalArgumentException(String.format(ILLEGAL_WAKE_SOURCE, link.getName()));
    }

    @Override
    public Set<String> getPhysicalLinkWakeSourceNames()
    {
        return new HashSet<>(m_WakeSources);
    }

    @Override
    public Set<WakeLock> getActiveWakeLocks()
    {
        final Set<WakeLock> activeLocks = new HashSet<>();
        for (BeagleBoneBlackLockInfo lockInfo : m_LockMgr.getActiveLocks())
        {
            activeLocks.add(lockInfo.getLock());
        }
        return activeLocks;
    }

    @Override
    public Set<WakeLock> getScheduledWakeLocks()
    {
        final Set<WakeLock> scheduledLocks = new HashSet<>();
        for (BeagleBoneBlackLockInfo lockInfo : m_LockMgr.getScheduledLocks())
        {
            scheduledLocks.add(lockInfo.getLock());
        }
        return scheduledLocks;
    }

    @Override
    public long activateWakeLock(final WakeLock lock, final long startLockTimeMs, final long stopLockTimeMs)
    {
        m_LockMgr.addWakeLock(lock, startLockTimeMs, stopLockTimeMs);
        if (stopLockTimeMs == INDEFINITE)
        {
            return INDEFINITE;
        }
        else if (m_WakeMgr == null)
        {
            return INDEFINITE;
        }
        return m_WakeMgr.getStandbyTime();
    }

    @Override
    public void cancelWakeLock(final WakeLock lock) throws IllegalStateException
    {
        if (lock == null)
        {
            return;
        }
        m_LockMgr.removeWakeLock(lock);
    }

    @Override
    public int getBatteryAmpHoursRem() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Get battery amp hours remaining is not currently supported.");
    }

    @Override
    public int getBatteryVoltage() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException("Get battery voltage is not currently supported.");
    }
    
    /**
     * Method that handles enabling and disabling wake sources.
     * 
     * @param link
     *      The physical link to be enabled/disabled as a wake source.
     * @param state
     *      The state the the wake source is to be set to (enabled/disabled).
     */
    private void handleWakeSource(final PhysicalLink link, final String state)
    {
        final String path = String.format("/sys/devices/%s/power/wakeup", m_SupportedLinks.get(link.getName()));
        final File wakeupFile = m_FileService.getFile(path);
        
        final String currentState;
        try (FileReader fileReader = m_FileService.createFileReader(wakeupFile);
                BufferedReader buffReader = m_FileService.createBufferedReader(fileReader))
        {
            currentState = buffReader.readLine();
        }
        catch (final IOException ex)
        {
            m_LogService.warning(ex, "Unable to read current wakeup state for physical link [%s].", link.getName());
            return;
        }
        
        if (currentState.equals(state))
        {
            throw new IllegalStateException(String.format("Physical link [%s] is already [%s] as a wakeup source.",
                    link.getName(), state));
        }
        
        try (FileOutputStream fos = m_FileService.createFileOutputStream(wakeupFile, false);
                PrintStream printStream = m_FileService.createPrintStream(fos))
        {
            printStream.print(state);
        }
        catch (final IOException ex)
        {
            m_LogService.warning(ex, "Unable to set wakeup state for physical link [%s] to [%s].", 
                    link.getName(), state);
        }
    }
}
