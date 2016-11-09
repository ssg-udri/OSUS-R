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

package mil.dod.th.ose.logging;

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mil.dod.th.core.log.LoggingService;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public class TestJavaLoggerBridge
{
    private static PrintStream m_OrigStdErr;
    private static Handler[] m_OrigLoggerHandlers;
    private static Logger m_Logger;
    private static ByteArrayOutputStream m_OutputStream;

    private JavaLoggerBridge m_SUT;
    private LoggingService m_LoggingService;
    private BundleContext m_Context;
    private final String ENABLED_PROP = "mil.dod.th.ose.logging.javaloggerbridge.enabled";
    
    @BeforeClass
    public static void init()
    {
        m_OrigStdErr = System.err;
        m_OutputStream = new ByteArrayOutputStream();
        System.setErr(new PrintStream(m_OutputStream));
        m_Logger = Logger.getLogger("test");
        
        Logger rootLogger = Logger.getLogger("");
        m_OrigLoggerHandlers = rootLogger.getHandlers().clone();
    }
    
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new JavaLoggerBridge();
        // don't use the standard mock as we DON'T want to print logs to standard error 
        m_LoggingService = mock(LoggingService.class); 
        
        m_Context = Mockito.mock(BundleContext.class);
        
        when(m_Context.getProperty(ENABLED_PROP)).thenReturn(null);
        
        m_SUT.setLoggingService(m_LoggingService);       
    }

    @After
    public void tearDown()
    {
        //make sure to add back the original log handler 
        //and remove the JavaLoggerBridge handler.
        final Logger rootLogger = Logger.getLogger("");
        final Handler[] handlers = rootLogger.getHandlers().clone();
        
        for (Handler handler : handlers)
        {
            rootLogger.removeHandler(handler);
        }
        
        for (Handler handler : m_OrigLoggerHandlers)
        {
            rootLogger.addHandler(handler);
        }
    }
    
    @AfterClass
    public static void revert()
    {
        System.setErr(m_OrigStdErr);
    }
    
    /**
     * Test that JUL to OSGi logging is enabled even if the framework property is not present.
     */
    @Test
    public void testActivateNoFrameworkProp()
    {
        m_Logger.info("test message");
        assertThat(new String(m_OutputStream.toByteArray()), containsString("test message"));
        
        //activate with no framework property found 
        m_SUT.activate(m_Context);
        
        verify(m_LoggingService, never()).log(LogService.LOG_INFO, "test message");
        
        m_Logger.info("no prop message");
        assertThat(new String(m_OutputStream.toByteArray()), not(containsString("no prop message")));
        verify(m_LoggingService).log(LogService.LOG_INFO, "no prop message");       
    }
    
    /**
     * Test that JUL to OSGi logging is disabled when enabled property is set to false. 
     * (i.e. "disabled message" does not appear in the OSGi log service)
     */
    @Test
    public void testActivateDisabledFrameworkProp()
    {
        m_Logger.info("test disabled");
        assertThat(new String(m_OutputStream.toByteArray()), containsString("test disabled"));
        
        //activate with framework property disabled.
        when(m_Context.getProperty(ENABLED_PROP)).thenReturn("false");
        
        m_SUT.activate(m_Context);
        
        verify(m_LoggingService, never()).log(LogService.LOG_INFO, "test disabled");
        
        m_Logger.info("disabled message");
        assertThat(new String(m_OutputStream.toByteArray()), containsString("disabled message"));
        verify(m_LoggingService, never()).log(LogService.LOG_INFO, "disabled message");
    }
    
    /**
     * /**
     * Test that JUL to OSGi logging is enabled if the framework property is set to true.
     */
    @Test
    public void testActivateEnabledFrameworkProp()
    {
        m_Logger.info("test enabled");
        assertThat(new String(m_OutputStream.toByteArray()), containsString("test enabled"));
        
        //activate with framework property enabled.
        when(m_Context.getProperty(ENABLED_PROP)).thenReturn("true");
        m_SUT.activate(m_Context);
        
        verify(m_LoggingService, never()).log(LogService.LOG_INFO, "test active");
        
        m_Logger.info("enabled message");
        assertThat(new String(m_OutputStream.toByteArray()), not(containsString("enabled message")));
        verify(m_LoggingService).log(LogService.LOG_INFO, "enabled message");
    }
    
    @Test
    public void testPublish()
    {
        LogRecord record = new LogRecord(Level.SEVERE, "hello");
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_ERROR, "hello");
 
        record = new LogRecord(Level.INFO, "test");
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_INFO, "test");
        
        record = new LogRecord(Level.WARNING, "blah");
        Throwable thrown = new Exception("some exception");
        record.setThrown(thrown);
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_WARNING, thrown, "blah");
        
        record = new LogRecord(Level.CONFIG, "afa");
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_DEBUG, "afa");
        
        record = new LogRecord(Level.FINE, "test3");
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_DEBUG, "test3");
        
        record = new LogRecord(Level.FINER, "test");
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_DEBUG, "test");
        
        record = new LogRecord(Level.FINEST, "test2");
        m_SUT.publish(record);
        verify(m_LoggingService).log(LogService.LOG_DEBUG, "test2");
    }
}
