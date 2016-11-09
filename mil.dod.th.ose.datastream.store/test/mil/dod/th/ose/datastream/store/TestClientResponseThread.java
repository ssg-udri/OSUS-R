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
package mil.dod.th.ose.datastream.store;

import mil.dod.th.core.datastream.StreamProfile;


import mil.dod.th.ose.datastream.store.ThreadStatusListener.ThreadState;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * @author jmiller
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ClientResponseThread.class)
public class TestClientResponseThread
{    
    private static final long HEARTBEAT_PERIOD = 2L;
    
    @Mock private ThreadStatusListener m_ThreadStatusListener;
    @Mock private StreamProfile m_StreamProfile;
    
    private ClientResponseThread m_SUT;
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
    }
    
    @After
    public void tearDown() throws InterruptedException
    {
        if (m_SUT != null)
        {
            m_SUT.interrupt();
            m_SUT.join();
        }
    }
    
    @Test
    public void testReset() throws InterruptedException
    {
        m_SUT = new ClientResponseThread(m_ThreadStatusListener, m_StreamProfile, HEARTBEAT_PERIOD, 0, true);
        
        m_SUT.reset();
        long origCountdownTime = ((Long)Whitebox.getInternalState(m_SUT, "m_CountdownTime")).longValue();
        Thread.sleep(1000);
        m_SUT.reset();
        long updatedCountdownTime = ((Long)Whitebox.getInternalState(m_SUT, "m_CountdownTime")).longValue();
        
        assertThat(updatedCountdownTime, is(greaterThan(origCountdownTime)));
    }
    
    @Test
    public void testRunWithHeartbeatPeriodZero()
    {
        m_SUT = new ClientResponseThread(m_ThreadStatusListener, m_StreamProfile, 0, 0, true);
        
        m_SUT.run();
        long archiveStartTime = ((Long)Whitebox.getInternalState(m_SUT, "m_ArchiveStartTime")).longValue();
        
        assertThat(archiveStartTime, is(greaterThan(0L)));
        verify(m_ThreadStatusListener).notifyObserver(m_StreamProfile, ThreadState.FINISHED);
    }
    
    @Test
    public void testRunWithNonZeroHeartbeatPeriod() throws InterruptedException
    {
        m_SUT = new ClientResponseThread(m_ThreadStatusListener, m_StreamProfile, HEARTBEAT_PERIOD, 0, true);
        
        long origArchiveStartTime = ((Long)Whitebox.getInternalState(m_SUT, "m_ArchiveStartTime")).longValue();
        assertThat(origArchiveStartTime, is(-1L));
        
        m_SUT.run();
        
        //sleep for an additional two seconds beyond heartbeat period
        Thread.sleep(1000*(HEARTBEAT_PERIOD + 2));
        
        long newArchiveStartTime = ((Long)Whitebox.getInternalState(m_SUT, "m_ArchiveStartTime")).longValue();
        assertThat(newArchiveStartTime, is(greaterThan(0L)));
        
        verify(m_ThreadStatusListener).notifyObserver(m_StreamProfile, ThreadState.FINISHED);       
    }
}
