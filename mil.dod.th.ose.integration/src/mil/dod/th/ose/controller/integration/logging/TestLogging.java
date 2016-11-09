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
package mil.dod.th.ose.controller.integration.logging;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import mil.dod.th.core.log.Logging;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
public class TestLogging
{
    protected boolean m_FoundLogMessage = false;

    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public final void testLogging() throws InterruptedException
    {
        LogReaderService logReader = IntegrationTestRunner.getService(LogReaderService.class);
        
        final Semaphore semaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            @Override
            public void logged(LogEntry entry)
            {
                if ("!some string with a value of: 5".equals(entry.getMessage()))
                {
                    semaphore.release();
                }
            }
        });
        
        Logging.log(LogService.LOG_INFO, "some string with a value of: %d", 5);
        
        // verify log message is read
        assertThat(semaphore.tryAcquire(5, TimeUnit.SECONDS), is(true));
    }
    
    @Test
    public final void testJulLogging() throws InterruptedException
    {
        LogReaderService logReader = IntegrationTestRunner.getService(LogReaderService.class);
        
        final Semaphore semaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            
            @Override
            public void logged(LogEntry entry)
            {
                if ("my string has a better value: 88".equals(entry.getMessage()))
                {
                    semaphore.release();
                }
            }
        });
        
        Logger log = Logger.getLogger("");
        log.info(String.format("my string has a better value: %d", 88));
        
        assertThat(semaphore.tryAcquire(10, TimeUnit.SECONDS), is(true));
    }
}
