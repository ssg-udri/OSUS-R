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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;

/**
 * Service that is responsible for all beagle bone black wake locks.
 * 
 * @author cweisenborn
 */
@Component(provide = BeagleBoneBlackLockManager.class)
public class BeagleBoneBlackLockManager extends TimerTask
{
    private LoggingService m_LogService;
    private Object m_Lock;
    private Set<BeagleBoneBlackLockInfo> m_ActiveLocks;
    private Set<BeagleBoneBlackLockInfo> m_ScheduledLocks;
    private List<BeagleBoneBlackLockListener> m_Listeners;
    
    @Reference
    public void setLogService(final LoggingService logService)
    {
        m_LogService = logService;
    }
    
    /**
     * Activation method. Sets up the needed lists.
     */
    @Activate
    public void activate()
    {
        m_Lock = new Object();
        m_ActiveLocks = new HashSet<>();
        m_ScheduledLocks = new HashSet<>();
        m_Listeners = new ArrayList<>();
    }
    
    /**
     * Register the specified wake lock added listener.
     * 
     * @param listener
     *      Listener to be registered.
     */
    public void registerWakeLockAddedListener(final BeagleBoneBlackLockListener listener)
    {
        m_Listeners.add(listener);
    }
    
    /**
     * Adds the specified wake lock.
     * 
     * @param lock
     *      The wake lock to be added.
     * @param startLockTimeMs
     *      The start time of the wake lock.
     * @param stopLockTimeMs
     *      The end time of the wake lock.
     */
    public void addWakeLock(final WakeLock lock, final long startLockTimeMs, final long stopLockTimeMs)
    {
        final BeagleBoneBlackLockInfo lockInfo = new BeagleBoneBlackLockInfo(lock, startLockTimeMs, stopLockTimeMs);
        synchronized (m_Lock)
        {
            if (lockInfo.getStartTimeMs() <= System.currentTimeMillis())
            {
                m_ActiveLocks.add(lockInfo);
                callActiveWakeLockAddedListners();
            }
            else
            {
                m_ScheduledLocks.add(lockInfo);
            }
        }
    }
    
    /**
     * Removes the specified wake lock.
     * 
     * @param lock
     *      The lock to be removed.
     */
    public void removeWakeLock(final WakeLock lock)
    {
        synchronized (m_Lock)
        {
            final Iterator<BeagleBoneBlackLockInfo> scheduledIterator = m_ScheduledLocks.iterator();
            while (scheduledIterator.hasNext())
            {
                final BeagleBoneBlackLockInfo scheduledLock = scheduledIterator.next();
                if (scheduledLock.getLock().equals(lock))
                {
                    scheduledIterator.remove();
                }
            }
            final Iterator<BeagleBoneBlackLockInfo> activeIterator = m_ActiveLocks.iterator();
            while (activeIterator.hasNext())
            {
                final BeagleBoneBlackLockInfo activeLock = activeIterator.next();
                if (activeLock.getLock().equals(lock))
                {
                    activeIterator.remove();
                }
            }
        }
    }
    
    /**
     * Method that returns the currently active wake locks.
     * 
     * @return
     *      Set of {@link BeagleBoneBlackLockInfo}s that represents the currently active wake locks.
     */
    public Set<BeagleBoneBlackLockInfo> getActiveLocks()
    {
        synchronized (m_Lock)
        {
            return new HashSet<>(m_ActiveLocks);
        }
    }
    
    /**
     * Method that returns the currently scheduled wake locks.
     * 
     * @return
     *      Set of {@link BeagleBoneBlackLockInfo}s that represents the currently scheduled wake locks.
     */
    public Set<BeagleBoneBlackLockInfo> getScheduledLocks()
    {
        synchronized (m_Lock)
        {
            return new HashSet<>(m_ScheduledLocks);
        }
    }
    
    /**
     * Returns the next scheduled wake lock.
     * 
     * @return
     *      {@link BeagleBoneBlackLockInfo} that represents the next scheduled wake lock.
     */
    public BeagleBoneBlackLockInfo getNextScheduledLock()
    {
        BeagleBoneBlackLockInfo nextScheduledLock = null;
        synchronized (m_Lock)
        {
            for (BeagleBoneBlackLockInfo info : m_ScheduledLocks)
            {
                if (nextScheduledLock == null)
                {
                    nextScheduledLock = info;
                }
                else if (info.getStartTimeMs()  < nextScheduledLock.getStartTimeMs())
                {
                    nextScheduledLock = info;
                }
            }
        }
        return nextScheduledLock;
    }

    @Override
    public void run()
    {
        synchronized (m_Lock)
        {
            final Iterator<BeagleBoneBlackLockInfo> scheduledIterator = m_ScheduledLocks.iterator();
            while (scheduledIterator.hasNext())
            {
                final BeagleBoneBlackLockInfo scheduledLock = scheduledIterator.next();
                if (scheduledLock.getStartTimeMs() <= System.currentTimeMillis())
                {
                    scheduledIterator.remove();
                    m_ActiveLocks.add(scheduledLock);
                    callActiveWakeLockAddedListners();
                    m_LogService.debug("Moved scheduled wake lock [%s] to active.", 
                            scheduledLock.getLock().getId());
                }
            }
            final Iterator<BeagleBoneBlackLockInfo> activeIterator = m_ActiveLocks.iterator();
            while (activeIterator.hasNext())
            {
                final BeagleBoneBlackLockInfo activeLock = activeIterator.next();
                if (activeLock.getEndTimeMs() != PlatformPowerManager.INDEFINITE 
                        && activeLock.getEndTimeMs() <= System.currentTimeMillis())
                {
                    activeIterator.remove();
                    m_LogService.debug("Removed expired active wake lock [%s].", activeLock.getLock().getId());
                }
            }
        }
    }
    
    /**
     * Method that calls all registered wake lock listeners.
     */
    private void callActiveWakeLockAddedListners()
    {
        for (BeagleBoneBlackLockListener listener: m_Listeners)
        {
            listener.activeWakeLockAdded();
        }
    }
}
