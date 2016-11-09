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
package mil.dod.th.remote.client.impl;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.service.log.LogService;

public class TestLogging
{
    private Logging m_SUT;

    @Mock private LogService m_LogService;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        m_SUT = new Logging();
        m_SUT.setLogService(m_LogService);
    }

    @Test
    public void testDebug()
    {
        m_SUT.debug("test %d", 1);
        verify(m_LogService).log(LogService.LOG_DEBUG, "test 1");
    }

    @Test
    public void testInfo()
    {
        m_SUT.info("test %d", 2);
        verify(m_LogService).log(LogService.LOG_INFO, "test 2");
    }

    @Test
    public void testWarning()
    {
        m_SUT.warning("test %d", 3);
        verify(m_LogService).log(LogService.LOG_WARNING, "test 3");
    }

    @Test
    public void testWarningThrowable()
    {
        Exception ex = new Exception("error");
        m_SUT.warning(ex, "test %d", 3);
        verify(m_LogService).log(LogService.LOG_WARNING, "test 3", ex);
    }

    @Test
    public void testError()
    {
        m_SUT.error("test %d", 4);
        verify(m_LogService).log(LogService.LOG_ERROR, "test 4");
    }

    @Test
    public void testErrorThrowable()
    {
        Exception ex = new Exception("error");
        m_SUT.error(ex, "test %d", 4);
        verify(m_LogService).log(LogService.LOG_ERROR, "test 4", ex);
    }

    @Test
    public void testMissingLogService()
    {
        m_SUT.setLogService(null);

        // Verify that no exceptions are thrown
        m_SUT.error("test %d", 4);
    }

    @Test
    public void testMissingLogServiceThrowable()
    {
        m_SUT.setLogService(null);

        Exception ex = new Exception("error");

        // Verify that no exceptions are thrown
        m_SUT.error(ex, "test %d", 4);
    }
}
