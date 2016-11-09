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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.Test;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.ose.test.LoggingServiceMocker;

/**
 * Test class for the exception util class.
 * 
 * @author cweisenborn
 */
public class TestExceptionUtil
{   
    /**
     * Verify the handle executor exception method logs the exception appropriately.
     */
    @Test
    public void testHandleExecutorException()
    {
        final LoggingService log = LoggingServiceMocker.createMock();
        final Exception ex = new Exception("Something went horribly wrong...");
        final Future<?> future = mock(Future.class, withSettings().extraInterfaces(Runnable.class));
        
        ExceptionUtil.handleExecutorExceptions(log, (Runnable)future, ex);
        verify(log).error(ex, "Executor threw an exception");
    }
    
    /**
     * Verify the handle executor exception method appropriately logs if an exception is thrown by the future passed
     * to it.
     */
    @Test
    public void testHandleExecutorExceptionNullException() throws InterruptedException, ExecutionException
    {
        final LoggingService log = LoggingServiceMocker.createMock();
        final Future<?> future = mock(Future.class, withSettings().extraInterfaces(Runnable.class));
        final CancellationException canEx = new CancellationException("CANCELLED!");
        when(future.get()).thenThrow(canEx);
        when(future.isDone()).thenReturn(true);
        
        ExceptionUtil.handleExecutorExceptions(log, (Runnable)future, null);
        verify(log).error(canEx, "Executor threw an exception");
    }
}
