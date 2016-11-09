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
package mil.dod.th.ose.core.impl.mp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;

/**
 * @author Dave Humeniuk
 *
 */
public class TestManagedExecutorsImpl
{

    private ManagedExecutorsImpl m_SUT;
    private LoggingService m_Logger;

    @Before
    public void setUp() throws Exception
    {
        m_Logger = LoggingServiceMocker.createMock();
        m_SUT = new ManagedExecutorsImpl(); 
        m_SUT.setLoggingService(m_Logger);
    }
    
    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#newCachedThreadPool()}.
     * Verifies a cached thread pool is created with the expected parameters.
     */
    @Test
    public void testNewCachedThreadPool()
    {
        ExecutorService es = m_SUT.newCachedThreadPool();
        assertThat(es, is(notNullValue()));
        
        // check that the ExecutorService has the expected parameters
        assertThat(((ThreadPoolExecutor)es).getCorePoolSize(), is(0)); // core size for cached pool is 0
        assertThat(((ThreadPoolExecutor)es).getMaximumPoolSize(), is(Integer.MAX_VALUE)); // unbounded thread pool
        assertThat(((ThreadPoolExecutor)es).getKeepAliveTime(TimeUnit.SECONDS), is(60L)); // 60 second timeout
        assertThat(((ThreadPoolExecutor)es).getQueue().remainingCapacity(), is(0)); // no queue capacity
    }

    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#newFixedThreadPool(int)}.
     * Verifies a fixed thread pool is created with the expected parameters.
     */
    @Test
    public void testNewFixedThreadPool()
    {
        ExecutorService es = m_SUT.newFixedThreadPool(3);
        assertThat(es, is(notNullValue()));
        
        // check that the ExecutorService has the expected parameters
        assertThat(((ThreadPoolExecutor)es).getCorePoolSize(), is(3)); //given size
        assertThat(((ThreadPoolExecutor)es).getMaximumPoolSize(), is(3)); //coreSize == maxSize for fixed pool
        assertThat(((ThreadPoolExecutor)es).getKeepAliveTime(TimeUnit.SECONDS), is(0L)); //no timeout
        assertThat(((ThreadPoolExecutor)es).getQueue().remainingCapacity(), is(Integer.MAX_VALUE)); //unbounded queue
    }

    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#newScheduledThreadPool(int)}.
     * Verifies a scheduled thread pool is created with the expected parameters.
     */
    @Test
    public void testNewScheduledThreadPool()
    {
        ScheduledExecutorService es = m_SUT.newScheduledThreadPool(5);
        assertThat(es, is(notNullValue()));
        
        // check that the ScheduledExecutorService has the expected parameters
        assertThat(((ScheduledThreadPoolExecutor)es).getCorePoolSize(), is(5)); //given size
        assertThat(((ScheduledThreadPoolExecutor)es).getMaximumPoolSize(), is(Integer.MAX_VALUE)); //unbounded pool
        assertThat(((ScheduledThreadPoolExecutor)es).getKeepAliveTime(TimeUnit.SECONDS), is(0L)); //no timeout
        assertThat(((ScheduledThreadPoolExecutor)es).getQueue().remainingCapacity(), 
                is(Integer.MAX_VALUE)); //unbounded queue
    }

    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#newSingleThreadExecutor()}.
     * Verifies that a single thread executor is created with the expected parameters.
     */
    @Test
    public void testNewSingleThreadExecutor()
    {
        ExecutorService es = m_SUT.newSingleThreadExecutor();
        assertThat(es, is(notNullValue()));
        
        // check that the ExecutorService has the expected parameters
        assertThat(((ThreadPoolExecutor)es).getCorePoolSize(), is(1)); //just a single thread
        assertThat(((ThreadPoolExecutor)es).getMaximumPoolSize(), is(1)); //never more than a single thread
        assertThat(((ThreadPoolExecutor)es).getKeepAliveTime(TimeUnit.SECONDS), is(0L)); //no timeout
        assertThat(((ThreadPoolExecutor)es).getQueue().remainingCapacity(), is(Integer.MAX_VALUE)); //unbounded queue
    }

    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#newSingleThreadScheduledExecutor()}.
     * Verifies that a single thread scheduled executor is created with the expected parameters.
     */ 
    @Test
    public void testNewSingleThreadScheduledExecutor()
    {
        ScheduledExecutorService es = m_SUT.newSingleThreadScheduledExecutor();
        assertThat(es, is(notNullValue()));
        
        // check that the ScheduledExecutorService has the expected parameters
        assertThat(((ScheduledThreadPoolExecutor)es).getCorePoolSize(), is(1)); // just a single thread
        assertThat(((ScheduledThreadPoolExecutor)es).getMaximumPoolSize(), is(Integer.MAX_VALUE)); // unbounded pool
        assertThat(((ScheduledThreadPoolExecutor)es).getKeepAliveTime(TimeUnit.SECONDS), is(0L)); // no timeout
        assertThat(((ScheduledThreadPoolExecutor)es).getQueue().remainingCapacity(), 
                is(Integer.MAX_VALUE)); //unbounded queue
    }

    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#shutdownAllExecutorServices()}.
     */
    @Test
    public void testShutdownAllExecutorServices()
    {
        m_SUT.newCachedThreadPool();
        m_SUT.newFixedThreadPool(82);
        m_SUT.newScheduledThreadPool(22);
        m_SUT.newSingleThreadExecutor();
        m_SUT.newSingleThreadScheduledExecutor();
        
        m_SUT.shutdownAllExecutorServices();
        
        // ideally would verify shutdown is called but no way to mock static system calls
        // just make sure the system doesn't puke

        // successive calls should work as well
        m_SUT.shutdownAllExecutorServices();
    }

    /**
     * Test method for {@link mil.dod.th.ose.core.impl.mp.ManagedExecutorsImpl#shutdownAllExecutorServicesNow()}.
     */
    @Test
    public void testShutdownAllExecutorServciesNow()
    {
        m_SUT.newCachedThreadPool();
        m_SUT.newFixedThreadPool(82);
        m_SUT.newScheduledThreadPool(22);
        m_SUT.newSingleThreadExecutor();
        m_SUT.newSingleThreadScheduledExecutor();
        
        m_SUT.shutdownAllExecutorServicesNow();
        
        // ideally would verify shutdown is called but no way to mock static system calls
        // just make sure the system doesn't puke
        
        // successive calls should work as well
        m_SUT.shutdownAllExecutorServicesNow();
    }
}
