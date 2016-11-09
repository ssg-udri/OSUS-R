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
package mil.dod.th.core.log;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
public class TestStaticLogging
{
    private Logging m_SUT;

    @Before
    public void setUp() throws Exception
    {
        m_SUT = new Logging();
    }

    /**
     * Verify that the logging service can log permutations of messages.
     * Verify that if the LogService is not set that messages cannot be logged.
     */
    @Test
    public final void testLogIntStringObjectArray()
    {
        // replay
        m_SUT.setLogService(null);
        assertThat(Logging.log(LogService.LOG_DEBUG, "hello"), is(false));
        
        // mock
        LogService logService = mock(LogService.class);
        m_SUT.setLogService(logService);
        
        // replay
        assertThat(Logging.log(LogService.LOG_DEBUG, "hello"), is(true));
        
        // verify
        verify(logService).log(LogService.LOG_DEBUG, "!hello");
        
        // replay
        assertThat(Logging.log(LogService.LOG_DEBUG, "hello %s what is up %d", "some", 4), is(true));
        
        // verify
        verify(logService).log(LogService.LOG_DEBUG, "!hello some what is up 4");
        
        m_SUT.unsetLogService(logService);
        assertThat(Logging.log(LogService.LOG_DEBUG, "hello"), is(false));
    }

    /**
     * Verify that the logging service can log permutations of exception messages.
     * Verify that if the LogService is not set that messages cannot be logged.
     */
    @Test
    public final void testLogIntThrowableStringObjectArray()
    {
        // replay, make sure the log service isn't set
        m_SUT.setLogService(null);
        
        // mock
        Exception e = new Exception();
        
        // replay
        assertThat(Logging.log(LogService.LOG_DEBUG, e, "hello"), is(false));
        
        // mock
        LogService logService = mock(LogService.class);
        m_SUT.setLogService(logService);
        
        // replay
        assertThat(Logging.log(LogService.LOG_DEBUG, e, "hello"), is(true));
        
        // verify
        verify(logService).log(LogService.LOG_DEBUG, "!hello", e);
        
        // replay
        assertThat(Logging.log(LogService.LOG_DEBUG, e, "some %s %d u", "message", 4), is(true));
        
        // verify
        verify(logService).log(LogService.LOG_DEBUG, "!some message 4 u", e);
        
        m_SUT.unsetLogService(logService);
        assertThat(Logging.log(LogService.LOG_DEBUG, e, "hello"), is(false));
    }
}
