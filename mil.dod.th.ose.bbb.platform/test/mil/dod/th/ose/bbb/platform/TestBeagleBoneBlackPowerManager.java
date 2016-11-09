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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.fail;

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

import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test class for the {@link BeagleBoneBlackPowerManager} class.
 * 
 * @author cweisenborn
 */
public class TestBeagleBoneBlackPowerManager
{
    private BeagleBoneBlackPowerManager m_SUT;
    @Mock private LoggingService m_LogService;
    @Mock private FileService m_FileService;
    @Mock private File m_File;
    @Mock private FileOutputStream m_FileOutputStream;
    @Mock private PrintStream m_PrintStream;
    @Mock private FileReader m_FileReader;
    @Mock private BufferedReader m_BufferedReader;
    
    @Mock private WakeLock m_Lock1;
    @Mock private WakeLock m_Lock2;
    @Mock private WakeLock m_Lock3;
    
    @Mock private BeagleBoneBlackTimerManager m_TimerMgr;
    @Mock private BeagleBoneBlackWakeManager m_WakeMgr;
    @Mock private BeagleBoneBlackLockManager m_LockMgr;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new BeagleBoneBlackPowerManager();
        m_SUT.setLogService(m_LogService);
        m_SUT.setFileService(m_FileService);
        m_SUT.setBeagleBoneBlackWakeLockManager(m_LockMgr);
        m_SUT.setBeagleBoneBlackWakeTimer(m_WakeMgr);
        m_SUT.setBeagleBoneBlackTimerManager(m_TimerMgr);
        
        final Map<String, Object> props = createProperties();
        m_SUT.activate(props);
    }
    
    @Test
    public void testEnableWakeSource() throws IOException
    {
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS1");
        when(m_FileService.getFile("/sys/devices/gpio0_4_wake/power/wakeup")).thenReturn(m_File);
        when(m_FileService.createFileOutputStream(m_File, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.createPrintStream(m_FileOutputStream)).thenReturn(m_PrintStream);
        when(m_FileService.createFileReader(m_File)).thenReturn(m_FileReader);
        when(m_FileService.createBufferedReader(m_FileReader)).thenReturn(m_BufferedReader);
        when(m_BufferedReader.readLine()).thenReturn("disabled");
        
        m_SUT.enableWakeSource(link);
        
        verify(m_PrintStream).print("enabled");
    }
    
    @Test
    public void testEnableWakeSourceIllegalState() throws IOException
    {
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS1");
        when(m_FileService.getFile("/sys/devices/gpio0_4_wake/power/wakeup")).thenReturn(m_File);
        when(m_FileService.createFileOutputStream(m_File, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.createPrintStream(m_FileOutputStream)).thenReturn(m_PrintStream);
        when(m_FileService.createFileReader(m_File)).thenReturn(m_FileReader);
        when(m_FileService.createBufferedReader(m_FileReader)).thenReturn(m_BufferedReader);
        when(m_BufferedReader.readLine()).thenReturn("enabled");
        
        try
        {
            m_SUT.enableWakeSource(link);
            fail("Expecting illegal state exception.");
        }
        catch (final IllegalStateException ex)
        {
            assertThat(ex.getMessage(), equalTo("Physical link [/dev/ttyS1] is already [enabled] as a wakeup source."));
        }
        
        verify(m_PrintStream, never()).print("enabled");
    }
    
    @Test
    public void testEnableWakeSourceIllegalArg()
    {
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS1337");
        
        try
        {
            m_SUT.enableWakeSource(link);
            fail("Expecting illegal argument exception as ttyS1337 is not a valid wake source.");
        }
        catch (final IllegalArgumentException ex)
        {
            assertThat(ex.getMessage(), equalTo("Specified physical link [/dev/ttyS1337] is not a valid wake source."));
        }
    }
    
    @Test
    public void testDisableWakeSource() throws IOException
    {
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS2");
        when(m_FileService.getFile("/sys/devices/gpio0_5_wake/power/wakeup")).thenReturn(m_File);
        when(m_FileService.createFileOutputStream(m_File, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.createPrintStream(m_FileOutputStream)).thenReturn(m_PrintStream);
        when(m_FileService.createFileReader(m_File)).thenReturn(m_FileReader);
        when(m_FileService.createBufferedReader(m_FileReader)).thenReturn(m_BufferedReader);
        when(m_BufferedReader.readLine()).thenReturn("enabled");
        
        m_SUT.disableWakeSource(link);
        
        verify(m_PrintStream).print("disabled");
    }
    
    @Test
    public void testDisableWakeSourceIllegalState() throws IOException
    {
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS1");
        when(m_FileService.getFile("/sys/devices/gpio0_4_wake/power/wakeup")).thenReturn(m_File);
        when(m_FileService.createFileOutputStream(m_File, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.createPrintStream(m_FileOutputStream)).thenReturn(m_PrintStream);
        when(m_FileService.createFileReader(m_File)).thenReturn(m_FileReader);
        when(m_FileService.createBufferedReader(m_FileReader)).thenReturn(m_BufferedReader);
        when(m_BufferedReader.readLine()).thenReturn("disabled");
        
        try
        {
            m_SUT.disableWakeSource(link);
            fail("Expecting illegal state exception.");
        }
        catch (final IllegalStateException ex)
        {
            assertThat(ex.getMessage(), 
                    equalTo("Physical link [/dev/ttyS1] is already [disabled] as a wakeup source."));
        }
        
        verify(m_PrintStream, never()).print("disabled");
    }
    
    @Test
    public void testDisableWakeSourceIllegalArg()
    {
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS25");
        
        try
        {
            m_SUT.disableWakeSource(link);
            fail("Expecting illegal argument exception as ttyS25 is not a valid wake source.");
        }
        catch (final IllegalArgumentException ex)
        {
            assertThat(ex.getMessage(), equalTo("Specified physical link [/dev/ttyS25] is not a valid wake source."));
        }
    }
    
    @Test
    public void testGetBatteryAmpHoursRem()
    {
        try
        {
            m_SUT.getBatteryAmpHoursRem();
            fail("Expecting unsupported operation exception.");
        }
        catch (final UnsupportedOperationException ex)
        {
            assertThat(ex.getMessage(), equalTo("Get battery amp hours remaining is not currently supported."));
        }
    }
    
    @Test
    public void testGetBatteryVoltage()
    {
        try
        {
            m_SUT.getBatteryVoltage();
            fail("Expecting unsupported operation exception.");
        }
        catch (final UnsupportedOperationException ex)
        {
            assertThat(ex.getMessage(), equalTo("Get battery voltage is not currently supported."));
        }
    }
    
    @Test
    public void testGetPhysicalLinkWakeSourceNames() throws IOException
    {
        Set<String> names = m_SUT.getPhysicalLinkWakeSourceNames();
        assertThat(names.size(), equalTo(0));
        
        PhysicalLink link = mock(PhysicalLink.class);
        when(link.getName()).thenReturn("/dev/ttyS1");
        when(m_FileService.getFile("/sys/devices/gpio0_4_wake/power/wakeup")).thenReturn(m_File);
        when(m_FileService.createFileOutputStream(m_File, false)).thenReturn(m_FileOutputStream);
        when(m_FileService.createPrintStream(m_FileOutputStream)).thenReturn(m_PrintStream);
        when(m_FileService.createFileReader(m_File)).thenReturn(m_FileReader);
        when(m_FileService.createBufferedReader(m_FileReader)).thenReturn(m_BufferedReader);
        when(m_BufferedReader.readLine()).thenReturn("disabled");
        
        m_SUT.enableWakeSource(link);
        
        names = m_SUT.getPhysicalLinkWakeSourceNames();
        assertThat(names.size(), equalTo(1));
        assertThat(names.toArray()[0], equalTo("/dev/ttyS1"));
        
        when(m_BufferedReader.readLine()).thenReturn("enabled");
        
        m_SUT.disableWakeSource(link);
        
        names = m_SUT.getPhysicalLinkWakeSourceNames();
        assertThat(names.size(), equalTo(0));
    }
    
    @Test
    public void testUpdated()
    {
        final Map<String, Object> props = createProperties();
        
        m_SUT.updated(props);
        
        final ArgumentCaptor<BeagleBoneBlackPowerManagerConfig> configCaptor = 
                ArgumentCaptor.forClass(BeagleBoneBlackPowerManagerConfig.class);
        verify(m_WakeMgr, times(2)).update(configCaptor.capture());
        
        final BeagleBoneBlackPowerManagerConfig config = configCaptor.getValue();
        assertThat(config.enabled(), equalTo(true));
        assertThat(config.minWakeTimeMs(), equalTo(7000L));
        assertThat(config.maxSleepTimeMs(), equalTo(10000L));
        assertThat(config.startupTimeMs(), equalTo(0L));
        assertThat(config.standbyNoticeTimeMs(), equalTo(3000L));
        assertThat(config.standbyBackoffTimeMs(), equalTo(5000L));
        assertThat(config.standbyMode(), equalTo(BeagleBoneBlackStandbyMode.MEM));
    }
    
    @Test
    public void testActivateWakeLock()
    {   
        final WakeLock lock = mock(WakeLock.class);
        m_SUT.setBeagleBoneBlackWakeTimer(null);
        
        long standbyTime = m_SUT.activateWakeLock(lock, 10, 50);
        assertThat(standbyTime, equalTo(-1L));
        verify(m_LockMgr).addWakeLock(lock, 10, 50);
        
        standbyTime = m_SUT.activateWakeLock(lock, 5, 25);
        assertThat(standbyTime, equalTo(-1L));
        verify(m_LockMgr).addWakeLock(lock, 5, 25);
        
        m_SUT.setBeagleBoneBlackWakeTimer(m_WakeMgr);
        when(m_WakeMgr.getStandbyTime()).thenReturn(5000L);
        
        standbyTime = m_SUT.activateWakeLock(lock, 3, 43);
        assertThat(standbyTime, equalTo(5000L));
        verify(m_LockMgr).addWakeLock(lock, 3, 43);
    }
    
    @Test
    public void testCancelWakeLock()
    {
        m_SUT.cancelWakeLock(null);
        
        verify(m_LockMgr, never()).removeWakeLock(any(WakeLock.class));
        
        final WakeLock lock = mock(WakeLock.class);
        
        m_SUT.cancelWakeLock(lock);
        
        verify(m_LockMgr).removeWakeLock(lock);
    }
    
    @Test
    public void testGetActiveWakeLocks()
    {
        when(m_LockMgr.getActiveLocks()).thenReturn(createLockSet());
        
        Set<WakeLock> locks = m_SUT.getActiveWakeLocks();
        
        assertThat(locks, hasItems(m_Lock1, m_Lock2, m_Lock3));
    }
    
    @Test
    public void testGetScheduledWakeLocks()
    {
        when(m_LockMgr.getScheduledLocks()).thenReturn(createLockSet());
        
        Set<WakeLock> locks = m_SUT.getScheduledWakeLocks();
        
        assertThat(locks, hasItems(m_Lock1, m_Lock2, m_Lock3));
    }
    
    private Map<String, Object> createProperties()
    {
        final Map<String, Object> props = new HashMap<>();
        props.put(PlatformPowerManager.CONFIG_PROP_ENABLED, true);
        props.put(PlatformPowerManager.CONFIG_PROP_MIN_WAKE_TIME_MS, 7000);
        props.put(PlatformPowerManager.CONFIG_PROP_MAX_SLEEP_TIME_MS, 10000);
        props.put(PlatformPowerManager.CONFIG_PROP_STARTUP_TIME_MS, 0);
        props.put(PlatformPowerManager.CONFIG_PROP_STDBY_NOTICE_TIME_MS, 3000);
        props.put(BeagleBoneBlackPowerManagerConfig.CONFIG_BACKOFF_TIME, 5000);
        props.put(BeagleBoneBlackPowerManagerConfig.CONFIG_STANDBY_MODE, BeagleBoneBlackStandbyMode.MEM);
        
        return props;
    }
    
    private Set<BeagleBoneBlackLockInfo> createLockSet()
    {
        BeagleBoneBlackLockInfo bbbLock1 = new BeagleBoneBlackLockInfo(m_Lock1, 0, -1);
        BeagleBoneBlackLockInfo bbbLock2 = new BeagleBoneBlackLockInfo(m_Lock2, 0, -1);
        BeagleBoneBlackLockInfo bbbLock3 = new BeagleBoneBlackLockInfo(m_Lock3, 0, -1);
        
        Set<BeagleBoneBlackLockInfo> lockSet = new HashSet<>();
        lockSet.add(bbbLock1);
        lockSet.add(bbbLock2);
        lockSet.add(bbbLock3);
        
        return lockSet;
    }
}
