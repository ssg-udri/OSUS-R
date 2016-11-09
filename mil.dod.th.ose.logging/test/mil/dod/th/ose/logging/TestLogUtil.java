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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import mil.dod.th.ose.shared.LogLevel;
import mil.dod.th.ose.test.AssertUtils;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @author dlandoll
 *
 */
public class TestLogUtil
{
    /**
     * Verify an OSGi log level can be converted to a native level.
     */
    @Test
    public void testConvertOsgiToNativeLevel()
    {
        assertThat(LogUtil.convertOsgiToNativeLevel(LogService.LOG_DEBUG), is(LogLevel.Debug));
        assertThat(LogUtil.convertOsgiToNativeLevel(LogService.LOG_INFO), is(LogLevel.Info));
        assertThat(LogUtil.convertOsgiToNativeLevel(LogService.LOG_WARNING), is(LogLevel.Warning));
        assertThat(LogUtil.convertOsgiToNativeLevel(LogService.LOG_ERROR), is(LogLevel.Error));
        
        try
        {
            LogUtil.convertOsgiToNativeLevel(-1);
            fail("Expected exception due to invalid value");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Verify a native level can be converted to an OSGi log level.
     */
    @Test
    public void testConvertNativeToOsgiLevel()
    {
        assertThat(LogUtil.convertNativeToOsgiLevel(LogLevel.Debug), is(LogService.LOG_DEBUG));
        assertThat(LogUtil.convertNativeToOsgiLevel(LogLevel.Info), is(LogService.LOG_INFO));
        assertThat(LogUtil.convertNativeToOsgiLevel(LogLevel.Warning), is(LogService.LOG_WARNING));
        assertThat(LogUtil.convertNativeToOsgiLevel(LogLevel.Error), is(LogService.LOG_ERROR));
    }
    
    @Test
    public void testFromLogEntry()
    {
        final String noExceptionString = LogUtil.fromLogEntry(LogService.LOG_DEBUG, System.currentTimeMillis(),
                null, null, "test", new Exception("Test Exception", new Exception("Cause")), false);
        assertThat(noExceptionString, endsWith("test"));
        
        // mock
        Bundle bundle = mock(Bundle.class);
        when(bundle.getBundleId()).thenReturn(1L);
        @SuppressWarnings("unchecked")// due to Mocking of the class
        ServiceReference<Object> serviceRef = mock(ServiceReference.class);
        when(serviceRef.getProperty(Constants.SERVICE_ID)).thenReturn("10");
        String[] clazz = {"test.ObjectClass1", "test.ObjectClass2"};
        when(serviceRef.getProperty(Constants.OBJECTCLASS)).thenReturn(clazz);
        
        final String exceptionString = LogUtil.fromLogEntry(LogService.LOG_DEBUG, System.currentTimeMillis(),
                bundle, serviceRef, "test", new Exception("Test Exception", 
                        new Exception(null, new Exception("Cause"))), true);
        assertThat(exceptionString, startsWith("DEBUG   "));
        assertThat(exceptionString, containsString("ID#1"));
        assertThat(exceptionString, containsString("10"));
        assertThat(exceptionString, containsString("test.ObjectClass1"));
        assertThat(exceptionString, containsString("test.ObjectClass2"));
        assertThat(exceptionString, containsString("Caused by: java.lang.Exception: Test Exception"));
        assertThat(exceptionString, containsString("Caused by: java.lang.Exception: Cause"));
        assertThat(exceptionString, containsString("Test Exception: Cause"));
    }
    
    /**
     * Verify trace has correct number of methods under limit.
     */
    @Test
    public void testFromLogEntryExceptionTraceUnderLimit()
    {
        Exception exception = new Exception("Test Exception");
        StackTraceElement[] elementArray = new StackTraceElement[10];
        for (int i=0; i<elementArray.length; i++)
        {
            elementArray[i] = new StackTraceElement("blah", "blah" + (i + 1), "blah.java", i+20);
        }
        exception.setStackTrace(elementArray);
        final String entry = LogUtil.fromLogEntry(LogService.LOG_DEBUG, System.currentTimeMillis(),
                null, null, "test", exception, true);
        AssertUtils.assertSubStringCount(entry, " at ", is(10));
        assertThat(entry, not(containsString(" ... ")));
    }
    
    /**
     * Verify trace has correct number of methods at limit.
     */
    @Test
    public void testFromLogEntryExceptionTraceAtLimit()
    {
        Exception exception = new Exception("Test Exception");
        StackTraceElement[] elementArray = new StackTraceElement[15];
        for (int i=0; i<elementArray.length; i++)
        {
            elementArray[i] = new StackTraceElement("blah", "blah" + (i + 1), "blah.java", i+20);
        }
        exception.setStackTrace(elementArray);
        final String entry = LogUtil.fromLogEntry(LogService.LOG_DEBUG, System.currentTimeMillis(),
                null, null, "test", exception, true);
        AssertUtils.assertSubStringCount(entry, " at ", is(15));
        assertThat(entry, not(containsString(" ... ")));
    }   
    
    /**
     * Verify all traces are limited to 15 methods.
     */
    @Test
    public void testFromLogEntryExceptionTraceLimit()
    {
        Exception exception = new Exception("Test Exception");
        StackTraceElement[] elementArray = new StackTraceElement[16];
        for (int i=0; i<elementArray.length; i++)
        {
            elementArray[i] = new StackTraceElement("blah", "blah" + (i + 1), "blah.java", i+20);
        }
        exception.setStackTrace(elementArray);
        final String entry = LogUtil.fromLogEntry(LogService.LOG_DEBUG, System.currentTimeMillis(),
                null, null, "test", exception, true);
        AssertUtils.assertSubStringCount(entry, " at ", is(15));
        assertThat(entry.endsWith("... 1 more\n"), is(true));
    }    
}
