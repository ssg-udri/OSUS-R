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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import java.util.TimerTask;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestBeagleBoneBlackTimerManager
{
    private BeagleBoneBlackTimerManager m_SUT;
    
    @Before
    public void activate()
    {
        m_SUT = new BeagleBoneBlackTimerManager();
        
        m_SUT.activate();
    }
    
    @Test
    public void testAddScheduledAtFixedRateTask() throws InterruptedException
    {
        TestTimerTask task = new TestTimerTask();
        
        m_SUT.addScheduledAtFixedRateTask(task, 0, 1000);
        
        Thread.sleep(5000);
        
        assertThat(task.getCount(), greaterThanOrEqualTo(5));
    }
    
    private class TestTimerTask extends TimerTask
    {
        private int m_Counter = 0;
        
        @Override
        public void run()
        {
            m_Counter++;
        }
        
        public int getCount()
        {
            return m_Counter;
        }
    }
}
