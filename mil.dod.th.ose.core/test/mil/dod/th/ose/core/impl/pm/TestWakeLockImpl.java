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
package mil.dod.th.ose.core.impl.pm;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.pm.PlatformPowerManager;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;

public class TestWakeLockImpl
{
    private static final String TEST_ID = "wl_id1";

    private WakeLockImpl m_SUT;
    private PlatformPowerManager m_PPM;
    private PowerManagerInternal m_PowerInternal;
    private BundleContext m_Context;
    private LoggingService m_Logging;

    @Before
    public void setUp() throws Exception
    {
        m_PowerInternal = mock(PowerManagerInternal.class);
        m_Context = mock(BundleContext.class);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(WakeLockImpl.COMPONENT_PROP_ID, TEST_ID);
        props.put(WakeLockImpl.COMPONENT_PROP_CONTEXT, TestContext.class);
        props.put(WakeLockImpl.COMPONENT_PROP_POWER_MGR, m_PowerInternal);

        m_Logging = mock(LoggingService.class);

        m_PPM = mock(PlatformPowerManager.class);
        m_SUT = new WakeLockImpl();
        m_SUT.setLoggingService(m_Logging);
        m_SUT.setPlatformPowerManager(m_PPM);
        m_SUT.activateInstance(m_Context, props);       
    }

    @After
    public void tearDown() throws Exception
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);
    }

    @Test
    public void testComponentFactory()
    {
        assertThat(WakeLockImpl.COMPONENT_FACTORY, is(WakeLockImpl.class.getName()));
    }

    /**
     * Test method for {@link WakeLockImpl#getId()}.
     */
    @Test
    public void testGetId()
    {
        assertThat(m_SUT.getId(), is(TEST_ID));
    }

    /**
     * Test method for {@link WakeLockImpl#activate(long, java.util.concurrent.TimeUnit)} when
     * a negative duration is provided.
     */
    @Test
    public void testActivateWakeLockLongTimeUnitNegDuration()
    {
        try
        {
            m_SUT.activate(-1, TimeUnit.MILLISECONDS);
            fail("Exception not thrown for a negative duration");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }

    /**
     * Test method for {@link WakeLockImpl#activate(long, java.util.concurrent.TimeUnit)} when
     * {@link PlatformPowerManager} is available.
     */
    @Test
    public void testActivateWakeLockLongTimeUnit()
    {
        ArgumentCaptor<Long> startTimeCapture = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> stopTimeCapture = ArgumentCaptor.forClass(Long.class);

        final long testTimeOffset = 20;
        final long wakeDurationMs = 86400000; // 1 day

        // Milliseconds
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long currentTime = System.currentTimeMillis();

        m_SUT.activate(wakeDurationMs, unit);

        verify(m_PPM).activateWakeLock(eq(m_SUT), startTimeCapture.capture(), stopTimeCapture.capture());
        assertThat(startTimeCapture.getValue(), greaterThanOrEqualTo(currentTime));
        assertThat(startTimeCapture.getValue(), lessThan(currentTime + testTimeOffset));
        assertThat(stopTimeCapture.getValue() - startTimeCapture.getValue(), is(wakeDurationMs));

        // Days
        unit = TimeUnit.DAYS;
        currentTime = System.currentTimeMillis();

        m_SUT.activate(1, unit);

        verify(m_PPM, times(2)).activateWakeLock(eq(m_SUT), startTimeCapture.capture(), stopTimeCapture.capture());
        assertThat(startTimeCapture.getValue(), greaterThanOrEqualTo(currentTime));
        assertThat(startTimeCapture.getValue(), lessThan(currentTime + testTimeOffset));
        assertThat(stopTimeCapture.getValue() - startTimeCapture.getValue(), is(wakeDurationMs));
    }

    /**
     * Test method for {@link WakeLockImpl#activate(long, java.util.concurrent.TimeUnit)} with no
     * {@link PlatformPowerManager}.
     */
    @Test
    public void testActivateWakeLockLongTimeUnitNoPPM()
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);

        m_SUT.activate(100, TimeUnit.MILLISECONDS);

        verify(m_PPM, never()).activateWakeLock(eq(m_SUT), anyLong(), anyLong());
    }

    /**
     * Test method for {@link WakeLockImpl#activate(java.util.Date)} when {@link PlatformPowerManager} is
     * available.
     */
    @Test
    public void testActivateWakeLockDate()
    {
        ArgumentCaptor<Long> startTimeCapture = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> stopTimeCapture = ArgumentCaptor.forClass(Long.class);

        long wakeDurationMs = 60000;

        // Create time one minute from now
        Date currentTime = new Date();
        Date lockAwakeTime = DateUtils.addMinutes(currentTime, 1);
        m_SUT.activate(lockAwakeTime);

        verify(m_PPM).activateWakeLock(eq(m_SUT), startTimeCapture.capture(), stopTimeCapture.capture());
        assertThat(stopTimeCapture.getValue() - startTimeCapture.getValue(), greaterThan(wakeDurationMs - 100));
        assertThat(stopTimeCapture.getValue() - startTimeCapture.getValue(), lessThanOrEqualTo(wakeDurationMs));
    }

    /**
     * Test method for {@link WakeLockImpl#activate(java.util.Date)} with no {@link PlatformPowerManager}.
     */
    @Test
    public void testActivateWakeLockDateNoPPM()
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);

        m_SUT.activate(new Date());

        verify(m_PPM, never()).activateWakeLock(eq(m_SUT), anyLong(), anyLong());
    }

    /**
     * Test method for {@link WakeLockImpl#activate()} when {@link PlatformPowerManager} is available.
     */
    @Test
    public void testActivateWakeLock()
    {
        ArgumentCaptor<Long> stopTimeCapture = ArgumentCaptor.forClass(Long.class);

        m_SUT.activate();

        verify(m_PPM).activateWakeLock(eq(m_SUT), anyLong(), stopTimeCapture.capture());
        assertThat(stopTimeCapture.getValue(), is(PlatformPowerManager.INDEFINITE));
    }

    /**
     * Test method for {@link WakeLockImpl#activate()} with no {@link PlatformPowerManager}.
     */
    @Test
    public void testActivateWakeLockNoPPM()
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);

        m_SUT.activate();

        verify(m_PPM, never()).activateWakeLock(eq(m_SUT), anyLong(), anyLong());
    }

    /**
     * Test method for {@link WakeLockImpl#scheduleWakeTime(java.util.Date)} when {@link PlatformPowerManager} is
     * available.
     */
    @Test
    public void testScheduleWakeTime()
    {
        ArgumentCaptor<Long> startTimeCapture = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Long> stopTimeCapture = ArgumentCaptor.forClass(Long.class);

        long wakeTime = System.currentTimeMillis();
        Date wakeDate = new Date(wakeTime);
        m_SUT.scheduleWakeTime(wakeDate);

        verify(m_PPM).activateWakeLock(eq(m_SUT), startTimeCapture.capture(), stopTimeCapture.capture());
        assertThat(startTimeCapture.getValue(), is(wakeTime));
        assertThat(stopTimeCapture.getValue(), is(wakeTime + 1));
    }

    /**
     * Test method for {@link WakeLockImpl#scheduleWakeTime(java.util.Date)} with no {@link PlatformPowerManager}.
     */
    @Test
    public void testScheduleWakeTimeNoPPM()
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);

        Date wakeDate = new Date();
        m_SUT.scheduleWakeTime(wakeDate);

        verify(m_PPM, never()).activateWakeLock(eq(m_SUT), anyLong(), anyLong());
    }

    /**
     * Test method for {@link WakeLockImpl#cancel()} when {@link PlatformPowerManager} is available.
     */
    @Test
    public void testCancelWakeLock()
    {
        m_SUT.cancel();
        verify(m_PPM).cancelWakeLock(m_SUT);
    }

    /**
     * Test method for {@link WakeLockImpl#cancel()} with no {@link PlatformPowerManager}.
     */
    @Test
    public void testCancelWakeLockNoPPM()
    {
        m_SUT.unsetPlatformPowerManager(m_PPM);
        m_SUT.cancel();
        verify(m_PPM, never()).cancelWakeLock(m_SUT);
    }
    
    /**
     * Verify delete calls power manager internal.
     */
    @Test
    public void testDelete()
    {
        m_SUT.delete();
        verify(m_PowerInternal).deleteWakeLock(m_SUT);
    }

    /**
     * Verify that wake lock log tracing can be enabled.
     */
    @Test
    public void testLogTracing()
    {
        m_SUT.activate();
        verify(m_Logging, never()).debug(anyString(), anyVararg());

        when(m_Context.getProperty(WakeLockImpl.WAKELOCK_TRACE_PROPERTY)).thenReturn("true");
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(WakeLockImpl.COMPONENT_PROP_ID, TEST_ID);
        props.put(WakeLockImpl.COMPONENT_PROP_CONTEXT, TestContext.class);
        props.put(WakeLockImpl.COMPONENT_PROP_POWER_MGR, m_PowerInternal);
        m_SUT.activateInstance(m_Context, props);

        m_SUT.activate();
        verify(m_Logging).debug(anyString(), anyVararg());
    }

    /**
     * Class used for the wake lock context.
     */
    private class TestContext
    {
    }
}
