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
package mil.dod.th.ose.shared.pm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.shared.pm.CountingWakeLock.CountingWakeLockHandle;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class TestCountingWakeLock
{
    private CountingWakeLock m_SUT;
    @Mock private WakeLock m_WakeLock;

    @Before
    public void setUp() throws Exception
    {
        // mock
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.pm.CountingWakeLock#getWakeLock()}.
     */
    @Test
    public void testGetWakeLock()
    {
        m_SUT = new CountingWakeLock();
        assertThat(m_SUT.getWakeLock(), is(nullValue()));

        m_SUT = new CountingWakeLock(m_WakeLock);
        assertThat(m_SUT.getWakeLock(), is(m_WakeLock));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.pm.CountingWakeLock#setWakeLock(mil.dod.th.core.pm.WakeLock)}.
     */
    @Test
    public void testSetWakeLock()
    {
        m_SUT = new CountingWakeLock();
        assertThat(m_SUT.getWakeLock(), is(nullValue()));
        m_SUT.setWakeLock(m_WakeLock);
        assertThat(m_SUT.getWakeLock(), is(m_WakeLock));

        WakeLock newWakeLock = Mockito.mock(WakeLock.class);
        m_SUT = new CountingWakeLock(m_WakeLock);
        m_SUT.setWakeLock(newWakeLock);
        assertThat(m_SUT.getWakeLock(), is(newWakeLock));
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.pm.CountingWakeLock#deleteWakeLock()}.
     */
    @Test
    public void testDeleteWakeLock()
    {
        // Verify no exception if there is no wake lock
        m_SUT = new CountingWakeLock();
        m_SUT.deleteWakeLock();

        m_SUT = new CountingWakeLock(m_WakeLock);
        m_SUT.deleteWakeLock();
        Mockito.verify(m_WakeLock).delete();
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.pm.CountingWakeLock#activateWithHandle()}.
     */
    @Test
    public void testActivateWithHandle()
    {
        // Verify no exception if there is no wake lock
        m_SUT = new CountingWakeLock();
        try (CountingWakeLockHandle wakeHandle = m_SUT.activateWithHandle())
        {
            // do nothing
        }

        m_SUT = new CountingWakeLock(m_WakeLock);
        try (CountingWakeLockHandle wakeHandle = m_SUT.activateWithHandle())
        {
            // do nothing
        }
        Mockito.verify(m_WakeLock).activate();
        Mockito.verify(m_WakeLock).cancel();
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.pm.CountingWakeLock#activate()}.
     */
    @Test
    public void testActivate()
    {
        // Verify no exception if there is no wake lock
        m_SUT = new CountingWakeLock();
        m_SUT.activate();

        m_SUT = new CountingWakeLock(m_WakeLock);
        m_SUT.activate();
        Mockito.verify(m_WakeLock).activate();
        m_SUT.activate();
        Mockito.verify(m_WakeLock, Mockito.times(1)).activate();
    }

    /**
     * Test method for {@link mil.dod.th.ose.shared.pm.CountingWakeLock#cancel()}.
     */
    @Test
    public void testCancel()
    {
        // Verify no exception if there is no wake lock
        m_SUT = new CountingWakeLock();
        m_SUT.cancel();

        m_SUT = new CountingWakeLock(m_WakeLock);
        m_SUT.cancel();
        Mockito.verify(m_WakeLock, Mockito.never()).cancel();
        m_SUT.activate();
        Mockito.verify(m_WakeLock).activate();
        m_SUT.cancel();
        Mockito.verify(m_WakeLock).cancel();

        m_SUT.activate();
        Mockito.verify(m_WakeLock, Mockito.times(2)).activate();
        m_SUT.activate();
        Mockito.verify(m_WakeLock, Mockito.times(2)).activate();
        m_SUT.cancel();
        Mockito.verify(m_WakeLock, Mockito.times(1)).cancel();
        m_SUT.cancel();
        Mockito.verify(m_WakeLock, Mockito.times(2)).cancel();
    }
}
