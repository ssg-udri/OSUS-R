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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.WakeLock;

/**
 * @author cweisenborn
 */
public class TestBeagleBoneBlackLockManager
{
    private BeagleBoneBlackLockManager m_SUT;
    @Mock private LoggingService m_LogService;
    @Mock private WakeLock m_Lock1;
    @Mock private WakeLock m_Lock2;
    @Mock private WakeLock m_Lock3;
    @Mock private WakeLock m_Lock4;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new BeagleBoneBlackLockManager();
        
        m_SUT.setLogService(m_LogService);
        m_SUT.activate();
    }
    
    @Test
    public void testAddActiveWakeLock()
    {
        when(m_Lock1.getId()).thenReturn("test");
        
        m_SUT.addWakeLock(m_Lock1, 0, -1);
        
        final List<BeagleBoneBlackLockInfo> activeLocks = new ArrayList<>(m_SUT.getActiveLocks());
        assertThat(activeLocks.size(), equalTo(1));
        final BeagleBoneBlackLockInfo lock = activeLocks.get(0);
        assertThat(lock.getLock().getId(), equalTo("test"));
        assertThat(lock.getStartTimeMs(), equalTo(0L));
        assertThat(lock.getEndTimeMs(), equalTo(-1L));
    }
    
    @Test
    public void testAddScheduledWakeLock()
    {
        when(m_Lock1.getId()).thenReturn("test");
        
        final long startTime = System.currentTimeMillis() + 1000000;
        m_SUT.addWakeLock(m_Lock1, startTime, -1);
        
        final List<BeagleBoneBlackLockInfo> scheduledLocks = new ArrayList<>(m_SUT.getScheduledLocks());
        assertThat(scheduledLocks.size(), equalTo(1));
        final BeagleBoneBlackLockInfo lock = scheduledLocks.get(0);
        assertThat(lock.getLock().getId(), equalTo("test"));
        assertThat(lock.getStartTimeMs(), equalTo(startTime));
        assertThat(lock.getEndTimeMs(), equalTo(-1L));
    }
    
    @Test
    public void testRemoveWakeLock()
    {
        when(m_Lock1.getId()).thenReturn("active");
        when(m_Lock2.getId()).thenReturn("scheduled");
        when(m_Lock3.getId()).thenReturn("active-extra");
        when(m_Lock4.getId()).thenReturn("schedulded-extra");
        
        final long scheduledStartTime = System.currentTimeMillis() + 1000000;
        
        m_SUT.addWakeLock(m_Lock1, 0, -1);
        m_SUT.addWakeLock(m_Lock3, 0, -1);
        m_SUT.addWakeLock(m_Lock2, scheduledStartTime, -1);
        m_SUT.addWakeLock(m_Lock4, scheduledStartTime, -1);
        
        List<BeagleBoneBlackLockInfo> activeLocks = new ArrayList<>(m_SUT.getActiveLocks());
        assertThat(activeLocks.size(), equalTo(2));
        List<BeagleBoneBlackLockInfo> scheduledLocks = new ArrayList<>(m_SUT.getScheduledLocks());
        assertThat(scheduledLocks.size(), equalTo(2));
        
        m_SUT.removeWakeLock(m_Lock1);
        
        activeLocks = new ArrayList<>(m_SUT.getActiveLocks());
        assertThat(activeLocks.size(), equalTo(1));
        
        m_SUT.removeWakeLock(m_Lock2);
        
        scheduledLocks = new ArrayList<>(m_SUT.getScheduledLocks());
        assertThat(scheduledLocks.size(), equalTo(1));
    }
    
    @Test
    public void testGetNextScheduledLock()
    {
        when(m_Lock1.getId()).thenReturn("first");
        when(m_Lock2.getId()).thenReturn("second");
        
        final long startTimeFirst = System.currentTimeMillis() + 1000000;
        final long startTimeSecond = System.currentTimeMillis() + 500000;
        
        m_SUT.addWakeLock(m_Lock1, startTimeFirst, -1);
        m_SUT.addWakeLock(m_Lock2, startTimeSecond, -1);
        
        final List<BeagleBoneBlackLockInfo> scheduledLocks = new ArrayList<>(m_SUT.getScheduledLocks());
        assertThat(scheduledLocks.size(), equalTo(2));
        
        final BeagleBoneBlackLockInfo nextLock = m_SUT.getNextScheduledLock();
        assertThat(nextLock.getLock().getId(), equalTo("second"));
        assertThat(nextLock.getStartTimeMs(), equalTo(startTimeSecond));
        assertThat(nextLock.getEndTimeMs(), equalTo(-1L));
    }
    
    @Test
    public void testRun() throws InterruptedException
    {
        when(m_Lock1.getId()).thenReturn("active");
        when(m_Lock2.getId()).thenReturn("active-to-removed");
        when(m_Lock3.getId()).thenReturn("scheduled");
        when(m_Lock4.getId()).thenReturn("scheduled-to-active");
        
        final long timeFuture = System.currentTimeMillis() + 1000000;
        final long timeNearFuture = System.currentTimeMillis() + 1000;
        
        m_SUT.addWakeLock(m_Lock1, 0, -1);
        m_SUT.addWakeLock(m_Lock2, 0, timeNearFuture);
        m_SUT.addWakeLock(m_Lock3, timeFuture, -1);
        m_SUT.addWakeLock(m_Lock4, timeNearFuture, -1);
        
        BeagleBoneBlackLockInfo activeLock = null;
        BeagleBoneBlackLockInfo scheduledToActiveLock = null;
        
        List<BeagleBoneBlackLockInfo> activeLocks = new ArrayList<>(m_SUT.getActiveLocks());
        assertThat(activeLocks.size(), equalTo(2));
        for (BeagleBoneBlackLockInfo lock: activeLocks)
        {
            if (lock.getLock().getId().equals("active"))
            {
                activeLock = lock;
            }
        }
        
        List<BeagleBoneBlackLockInfo> scheduledLocks = new ArrayList<>(m_SUT.getScheduledLocks());
        assertThat(scheduledLocks.size(), equalTo(2));
        for (BeagleBoneBlackLockInfo lock: scheduledLocks)
        {
            if (lock.getLock().getId().equals("scheduled-to-active"))
            {
                scheduledToActiveLock = lock;
            }
        }
        
        Thread.sleep(3000);
        
        m_SUT.run();
        activeLocks = new ArrayList<>(m_SUT.getActiveLocks());
        assertThat(activeLocks.size(), equalTo(2));
        assertThat(activeLocks, hasItems(activeLock, scheduledToActiveLock));
        
        scheduledLocks = new ArrayList<>(m_SUT.getScheduledLocks());
        assertThat(scheduledLocks.size(), equalTo(1));
        BeagleBoneBlackLockInfo lock = scheduledLocks.get(0);
        assertThat(lock.getLock().getId(), equalTo("scheduled"));
        assertThat(lock.getStartTimeMs(), equalTo(timeFuture));
        assertThat(lock.getEndTimeMs(), equalTo(-1L));
        
        verify(m_LogService).debug("Moved scheduled wake lock [%s] to active.", "scheduled-to-active");
        verify(m_LogService).debug("Removed expired active wake lock [%s].", "active-to-removed");
    }
}
