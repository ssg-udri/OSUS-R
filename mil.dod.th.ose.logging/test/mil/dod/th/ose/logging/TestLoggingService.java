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

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
public class TestLoggingService
{
    private LoggingServiceImpl m_SUT;
    private ComponentContext m_Context;
    private BundleContext m_UsingBundleContext;
    private ServiceReference<LogService> m_LogServiceRef;
    private LogService m_LogService;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new LoggingServiceImpl();
        
        m_Context = mock(ComponentContext.class);
        
        Bundle usingBundle = mock(Bundle.class);
        when(m_Context.getUsingBundle()).thenReturn(usingBundle);
        
        m_UsingBundleContext = mock(BundleContext.class);
        when(usingBundle.getBundleContext()).thenReturn(m_UsingBundleContext);
        
        m_LogServiceRef = mock(ServiceReference.class);
        when(m_UsingBundleContext.getServiceReference(LogService.class)).thenReturn(m_LogServiceRef);
        
        m_LogService = mock(LogService.class);
        when(m_UsingBundleContext.getService(m_LogServiceRef)).thenReturn(m_LogService);
        
        m_SUT.activate(m_Context);
    }
    
    /**
     * Verify the using bundle's context is used to get the service.
     */
    @Test
    public final void testActivate()
    {
        // verify activation that happened in setup
        verify(m_UsingBundleContext).getService(m_LogServiceRef);
    }
    
    /**
     * Verify the using bundle's context is used to unget the service.
     */
    @Test
    public final void testDeactivate()
    {
        // replay
        m_SUT.deactivate(m_Context);
        
        // verify
        verify(m_UsingBundleContext).ungetService(m_LogServiceRef);
    }

    /**
     * Verify logging a throwable will be sent to the OSGi log service.
     */
    @Test
    public final void testLogThrowable()
    {
        m_SUT.activate(m_Context);
        
        Exception e = new Exception();
        
        // replay
        m_SUT.log(LogService.LOG_DEBUG, e, "hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_DEBUG, "hello", e);
        
        // replay
        m_SUT.log(LogService.LOG_DEBUG, e, "some %s %d u", "message", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_DEBUG, "some message 4 u", e);
    }
    
    /**
     * Verify logging a service reference will be sent to the OSGi log service.
     */
    @Test
    public final void testLogServiceRef()
    {
        // mock
        ServiceReference<?> ref = mock(ServiceReference.class);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(5);
        when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] { "test" });
        
        // replay
        m_SUT.log(ref, LogService.LOG_DEBUG, "hello");
        
        // verify
        verify(m_LogService).log(ref, LogService.LOG_DEBUG, "hello");
        
        // replay
        m_SUT.log(ref, LogService.LOG_DEBUG, "hello %s what is up %d", "some", 4);
        
        // verify
        verify(m_LogService).log(ref, LogService.LOG_DEBUG, "hello some what is up 4");
    }

    /**
     * Verify logging a service reference and throwable will both be sent to the OSGi log service.
     */
    @Test
    public final void testLogServiceRefAndThrowable()
    {
        // mock
        ServiceReference<?> ref = mock(ServiceReference.class);
        when(ref.getProperty(Constants.SERVICE_ID)).thenReturn(5);
        when(ref.getProperty(Constants.OBJECTCLASS)).thenReturn(new String[] { "test" });
        
        Exception e = new Exception();
        
        // replay
        m_SUT.log(ref, LogService.LOG_DEBUG, e, "hello");
        
        // verify
        verify(m_LogService).log(ref, LogService.LOG_DEBUG, "hello", e);
        
        // replay
        m_SUT.log(ref, LogService.LOG_DEBUG, e, "some %s %d u", "message", 4);
        
        // verify
        verify(m_LogService).log(ref, LogService.LOG_DEBUG, "some message 4 u", e);
    }
    
    /**
     * Verify calling the debug method will call on log service with the debug log level.
     */
    @Test
    public void testLogDebug()
    {
        // replay
        m_SUT.debug("hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_DEBUG, "hello");
        
        // replay
        m_SUT.debug("hello %s what is up %d", "some", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_DEBUG, "hello some what is up 4");
    }
    
    /**
     * Verify calling the info method will call on log service with the info log level.
     */
    @Test
    public void testLogInfo()
    {
        // replay
        m_SUT.info("hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_INFO, "hello");
        
        // replay
        m_SUT.info("hello %s what is up %d", "some", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_INFO, "hello some what is up 4");
    }
    
    /**
     * Verify calling the warning method will call on log service with the warning log level.
     */
    @Test
    public void testLogWarning()
    {
        // replay
        m_SUT.warning("hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_WARNING, "hello");
        
        // replay
        m_SUT.warning("hello %s what is up %d", "some", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_WARNING, "hello some what is up 4");
    }
    
    /**
     * Verify calling the warning method with a throwable will call on log service with the warning log level and pass 
     * the throwable.
     */
    @Test
    public final void testLogWarningThrowable()
    {
        Exception e = new Exception();
        
        // replay
        m_SUT.warning(e, "hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_WARNING, "hello", e);
        
        // replay
        m_SUT.warning(e, "some %s %d u", "message", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_WARNING, "some message 4 u", e);
    }
    
    /**
     * Verify calling the error method will call on log service with the error log level.
     */
    @Test
    public void testLogError()
    {
        // replay
        m_SUT.error("hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_ERROR, "hello");
        
        // replay
        m_SUT.error("hello %s what is up %d", "some", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_ERROR, "hello some what is up 4");
    }
    
    /**
     * Verify calling the error method with a throwable will call on log service with the error log level and pass the 
     * throwable.
     */
    @Test
    public final void testLogErrorThrowable()
    {
        Exception e = new Exception();
        
        // replay
        m_SUT.error(e, "hello");
        
        // verify
        verify(m_LogService).log(LogService.LOG_ERROR, "hello", e);
        
        // replay
        m_SUT.error(e, "some %s %d u", "message", 4);
        
        // verify
        verify(m_LogService).log(LogService.LOG_ERROR, "some message 4 u", e);
    }
}
