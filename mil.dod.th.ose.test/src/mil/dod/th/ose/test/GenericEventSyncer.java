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
package mil.dod.th.ose.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Class which provides generic methods for waiting for events.
 * 
 * @author nickmarcucci
 *
 */
public abstract class GenericEventSyncer<T>
{
    protected boolean m_Used;
    protected int m_TimesRemaining;
    protected final Semaphore m_WaitSem = new Semaphore(0);
    protected String m_DesiredTopic;
    protected int m_TotalTimes;
    protected List<T> m_FoundEvents = new ArrayList<>();
    
    public List<T> waitFor(int timeout, TimeUnit units)
    {
        assertThat("Can only use this syncer one time.  Create a new one.", m_Used, is(false));
        m_Used = true;
        
        try
        {
            if (m_TotalTimes == 0)
            {
                assertThat("Expected 0 events, but got at least 1", m_WaitSem.tryAcquire(timeout, units), is(false));
            }
            else
            {
                assertThat("Timeout waiting for event: " + m_DesiredTopic + "\nExpected " + m_TotalTimes 
                        + " times, only got " + (m_TotalTimes - m_TimesRemaining) + " times",
                        m_WaitSem.tryAcquire(timeout, units));
            }
        }
        catch (InterruptedException e)
        {
            fail("Interrupted while waiting");
        }
        
        return m_FoundEvents;
    }
}
