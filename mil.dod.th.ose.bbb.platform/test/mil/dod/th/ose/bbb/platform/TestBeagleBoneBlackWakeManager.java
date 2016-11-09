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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.bbb.platform.BeagleBoneBlackGpioManager.GpioState;
import mil.dod.th.ose.utils.ProcessService;

/**
 * @author cweisenborn
 */
public class TestBeagleBoneBlackWakeManager
{
    private BeagleBoneBlackWakeManager m_SUT;
    @Mock private EventAdmin m_EventAdmin;
    @Mock private LoggingService m_LogService;
    @Mock private ProcessService m_ProcessBuilderService;
    @Mock private Process m_Process;
    @Mock private BeagleBoneBlackGpioManager m_GpioMgr;
    @Mock private BeagleBoneBlackLockManager m_LockMgr;
    
    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws IOException, InterruptedException
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new BeagleBoneBlackWakeManager();
        
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setLoggingService(m_LogService);
        m_SUT.setProcessService(m_ProcessBuilderService);
        m_SUT.setBeagleBoneBlackGpioMgr(m_GpioMgr);
        m_SUT.setBeagleBoneBlackWakeLockMgr(m_LockMgr);
        
        when(m_ProcessBuilderService.createProcess(Mockito.anyList())).thenReturn(m_Process);
        when(m_Process.waitFor()).thenReturn(0);
    }
    
    @Test
    public void testActivate()
    {
        m_SUT.activate();
        
        verify(m_GpioMgr).setGpioState(7, GpioState.HIGH);
    }
    
    @Test
    public void testDeactivate()
    {
        m_SUT.deactivate();
        
        verify(m_GpioMgr).setGpioState(7, GpioState.LOW);
    }
    
    @Test
    public void testGetStandbyTime()
    {
        final long standbyTime = m_SUT.getStandbyTime();
        assertThat(standbyTime, equalTo(standbyTime));
    }
    
    @Test
    public void testUpdate()
    {
        BeagleBoneBlackPowerManagerConfig config = mockConfig(true);
        
        m_SUT.update(config);
        
        verify(config).minWakeTimeMs();
        verify(config).maxSleepTimeMs();
        verify(config).standbyNoticeTimeMs();
        verify(config).standbyBackoffTimeMs();
        verify(config).standbyMode();
        verify(config, times(2)).enabled();
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRunNoLocks() throws InterruptedException, IOException
    {
        when(m_LockMgr.getActiveLocks()).thenReturn(new HashSet<BeagleBoneBlackLockInfo>());
        BeagleBoneBlackPowerManagerConfig config = mockConfig(true);
        
        m_SUT.update(config);
        
        Thread.sleep(2200);
        
        m_SUT.run();
        
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).sendEvent(eventCaptor.capture());
        Event standbyEvent = eventCaptor.getValue();
        assertThat(standbyEvent.getTopic(), equalTo(BeagleBoneBlackPowerManager.TOPIC_STANDBY_NOTICE_EVENT));
        verify(m_LogService).debug("Posting standby event...");
        verify(m_LogService).debug("Standby event posted...");
        
        Thread.sleep(2200);
        
        m_SUT.run();
        
        verify(m_GpioMgr).setGpioState(7, GpioState.LOW);
        verify(m_LogService).debug(
                String.format("Executing RTC wake command with standby mode [%s] for [%s] seconds...", "mem", 2));
        
        ArgumentCaptor<List> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(m_ProcessBuilderService).createProcess(listCaptor.capture());
        List<String> args = listCaptor.getValue();
        assertThat(args, hasItems("bash", "-c", "rtcwake -d rtc0 -m mem -s 2"));
        
        verify(m_LogService).debug("Waiting for RTC wake command...");
        verify(m_GpioMgr).setGpioState(7, GpioState.HIGH);
        
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event wakeEvent = eventCaptor.getValue();
        assertThat(wakeEvent.getTopic(), equalTo(BeagleBoneBlackPowerManager.TOPIC_WAKEUP_EVENT));
        verify(m_LogService).debug("Wake event posted...");
    }
    
    @Test
    public void testCancelStandby() throws InterruptedException
    {
        when(m_LockMgr.getActiveLocks()).thenReturn(new HashSet<BeagleBoneBlackLockInfo>());
        BeagleBoneBlackPowerManagerConfig config = mockConfig(true);
        
        m_SUT.update(config);
        
        Thread.sleep(2200);
        
        m_SUT.run();
        
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).sendEvent(eventCaptor.capture());
        Event standbyEvent = eventCaptor.getValue();
        assertThat(standbyEvent.getTopic(), equalTo(BeagleBoneBlackPowerManager.TOPIC_STANDBY_NOTICE_EVENT));
        verify(m_LogService).debug("Posting standby event...");
        verify(m_LogService).debug("Standby event posted...");
        
        BeagleBoneBlackLockInfo activeLock = mock(BeagleBoneBlackLockInfo.class);
        Set<BeagleBoneBlackLockInfo> activeLocksSet = new HashSet<>();
        activeLocksSet.add(activeLock);
        when(m_LockMgr.getActiveLocks()).thenReturn(activeLocksSet);
        
        Thread.sleep(500);
        
        m_SUT.run();
        
        verify(m_LogService).debug("Canceling standby...");
        
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        Event canceledEvent = eventCaptor.getValue();
        assertThat(canceledEvent.getTopic(), equalTo(BeagleBoneBlackPowerManager.TOPIC_STANDBY_CANCELLED_EVENT));
    }
    
    private BeagleBoneBlackPowerManagerConfig mockConfig(final boolean enabled)
    {
        BeagleBoneBlackPowerManagerConfig config = mock(BeagleBoneBlackPowerManagerConfig.class);
        when(config.minWakeTimeMs()).thenReturn(2000L);
        when(config.maxSleepTimeMs()).thenReturn(2000L);
        when(config.standbyNoticeTimeMs()).thenReturn(2000L);
        when(config.standbyBackoffTimeMs()).thenReturn(2000L);
        when(config.standbyMode()).thenReturn(BeagleBoneBlackStandbyMode.MEM);
        when(config.enabled()).thenReturn(enabled);
        
        return config;
    }
}
