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
package mil.dod.th.ose.shared;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.test.ExceptionThrower;
import mil.dod.th.ose.test.LoggingServiceMocker;

/**
 * Test class for the {@link ScheduledExceptionLoggingThreadPool} class.
 * 
 * @author cweisenborn
 */
public class TestScheduledExceptionLoggingThreadPool
{
    private ScheduledExceptionLoggingThreadPool m_SUT;
    private LoggingService m_Log;
    
    @Before
    public void setup()
    {
        m_Log = LoggingServiceMocker.createMock();
        m_SUT = new ScheduledExceptionLoggingThreadPool(m_Log, 4);
    }
    
    /**
     * Test method for exception handling in scheduled thread pools.
     */
    @Test
    public void testHandleThrownExceptionScheduled() throws InterruptedException, ExecutionException
    {
        Exception throwMe = new Exception("A purple people eater attacked.");
        Exception t = Mockito.spy(throwMe);
        Callable<String> r = new ExceptionThrower(t);
        Future<String> future = m_SUT.schedule(r, 0, TimeUnit.SECONDS);
        try
        {
            future.get();
        }
        catch (Exception e)
        {
            //expecting exception
        }
        
        //verify logging
        Mockito.verify(m_Log, Mockito.timeout(25)).error(t, "Executor threw an exception");
                
        //test for CancellationException
        CancellationException canceled = new CancellationException("A cancellation exception message.");
        CancellationException ce = Mockito.spy(canceled);
        r = new ExceptionThrower(ce);
        future = m_SUT.schedule(r, 0, TimeUnit.SECONDS);
        try
        {
            future.get();
        }
        catch (Exception e)
        {
            //expecting exception
        }
        
        //verify logging
        Mockito.verify(m_Log, Mockito.timeout(25)).error(ce, "Executor threw an exception");
        
        //test for ExecutionException
        IOException ioExcept = new IOException("An IO exception message");
        IOException io = Mockito.spy(ioExcept);
        ExecutionException eExcept = new ExecutionException(io);
        ExecutionException ee = Mockito.spy(eExcept);
        r = new ExceptionThrower(ee);
        future = m_SUT.schedule(r, 0, TimeUnit.SECONDS);
        try
        {
            future.get();
        }
        catch (Exception e)
        {
            //expecting exception
        }
        
        //verify logging
        Mockito.verify(m_Log, Mockito.timeout(25)).error(ee, "Executor threw an exception"); 
        
        //test for InterruptedException
        InterruptedException ieExcept = new InterruptedException("An interrupted exception message.");
        InterruptedException ie = Mockito.spy(ieExcept);
        r = new ExceptionThrower(ie);
        future = m_SUT.schedule(r, 0, TimeUnit.SECONDS);
        try
        {
            future.get();
        }
        catch (Exception e)
        {
            //expecting exception
        }
        
        //verify logging
        Mockito.verify(m_Log, Mockito.timeout(25)).error(ie, "Executor threw an exception"); 
    }
}
