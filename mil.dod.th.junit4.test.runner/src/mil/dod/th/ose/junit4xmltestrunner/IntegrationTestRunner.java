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
package mil.dod.th.ose.junit4xmltestrunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import static org.hamcrest.MatcherAssert.assertThat; // NOCHECKSTYLE: avoid static import (used in context of testing)
import static org.hamcrest.Matchers.*; // NOCHECKSTYLE: avoid static import (used in context of testing)

/**
 * This is a test runner that can run within the OSGi framework.  A property must be set to tell this bundle
 * which bundle to test.  When the bundle has completed testing it will shutdown the OSGi framework.
 * 
 * @author dhumeniuk
 *
 */
@Component(provide = IntegrationTestRunner.class, 
        properties = { "osgi.command.scope=test", "osgi.command.function=runAll|runClass|list" })
public class IntegrationTestRunner
{
    /**
     * Reference to the integration test service.
     */
    private static IntegrationTestService m_TestService;
    
    /**
     * Reference to the bundle context.
     */
    private static BundleContext m_Context;

    /**
     * Suffix to add to the end of test reports for this service.
     */
    private String m_Suffix;

    /**
     * Logging service for all log messages.
     */
    private LogService m_LogService;
    
    /**
     * Set the test service to use.  Know that it is ready now.
     * 
     * @param testService
     *      Test service to execute
     */
    @Reference
    public void setTestService(final IntegrationTestService testService)
    {
        m_TestService = testService;
    }
    
    /**
     * Set the log service to use.
     *
     * @param logService
     *      logging service
     */
    @Reference
    public void setLogService(final LogService logService)
    {
        m_LogService = logService;
    }
    
    /**
     * Start the runner.
     * 
     * @param context
     *      Bundle context reference for this bundle.
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_Context = context;
        
        m_Suffix = m_TestService.getTargetName();
        
        final String reportSuffix = System.getProperty("mil.dod.th.ose.junit4xmltestrunner.reportSuffix");
        if (reportSuffix != null)
        {
            m_Suffix += "-" + reportSuffix;
        }
    }
    
    /**
     * Run all standard tests (not the optional ones) defined by the reference test service.
     * 
     * @param reportDir
     *      where to place test results
     * @param printStackTrace
     *      whether to print stack trace on failures
     * @throws IOException
     *      if there is an using the {@code reportDir} path
     */
    @Descriptor("Run all integration tests")
    public void runAll(
            @Descriptor("Directoy location to put reports")
            final File reportDir,
            @Descriptor("Whether to print the stack trace on failures")
            final boolean printStackTrace) throws IOException
    {
        m_LogService.log(LogService.LOG_INFO, "Running all tests: " + m_TestService.getStandardTestClasses());
        final Class<?>[] classes = new Class<?>[m_TestService.getStandardTestClasses().size()];
        m_TestService.getStandardTestClasses().toArray(classes);
        XMLTestRunner.run(reportDir, m_Suffix, printStackTrace, classes);
    }
    
    /**
     * Run a particular test classes based on name (will pick from optional ones as well).
     * 
     * @param reportDir
     *      where to place test results
     * @param printStackTrace
     *      whether to print stack trace on failures
     * @param classNames
     *      List of simple class names of the test classes as returned by {@link Class#getSimpleName()}
     * @throws IOException
     *      if there is an using the {@code reportDir} path
     * @throws IllegalArgumentException
     *      if one of the class names is not a valid test class
     */
    public void runClass(final File reportDir, final boolean printStackTrace, final String[] classNames) 
            throws IOException, IllegalArgumentException
    {
        m_LogService.log(LogService.LOG_INFO, "Running the following tests: " + Arrays.asList(classNames));
        for (String className : classNames)
        {
            boolean foundClass = false;
            
            for (Class<?> testClass : m_TestService.getAllTestClasses())
            {
                if (className.equalsIgnoreCase(testClass.getSimpleName()))
                {
                    XMLTestRunner.run(reportDir, m_Suffix, printStackTrace, testClass);
                    foundClass = true;
                    break;
                }
            }
            
            if (!foundClass)
            {
                throw new IllegalArgumentException("Test class " + className + " not found");
            }
        }
    }
    
    /**
     * Run a particular test classes based on name (will pick from optional ones as well).
     * 
     * @param classNames
     *      List of simple class names of the test classes as returned by {@link Class#getSimpleName()}
     * @throws IOException
     *      if there is an using the {@code reportDir} path
     * @throws IllegalArgumentException
     *      if one of the class names is not a valid test class
     */
    @Descriptor("Run a single test class")
    public void runClass(
            @Descriptor("List of simple class names of test classes to execute")
            final String[] classNames) throws IOException, IllegalArgumentException
    {
        runClass(new File("."), true, classNames);
    }
    
    /**
     * List the test classes that are available to run.
     * 
     * @param session
     *      command session that is executing the command
     */
    @Descriptor("List available test classes to run (using runClass)")
    public void list(final CommandSession session)
    {
        for (Class<?> testClass : m_TestService.getStandardTestClasses())
        {
            session.getConsole().println(testClass.getSimpleName());
        }
    }
    
    /**
     * Get the suffix to add to the end of test reports for this service.
     * 
     * @return
     *      the suffix for test reports
     */
    public String getSuffix()
    {
        return m_Suffix;
    }
    
    /**
     * Get a static reference to the bundle context.  This should only be used by test classes that are 
     * created by the JUnit framework.
     * 
     * @return
     *      Static reference to the Bundle Context.
     */
    public static BundleContext getBundleContext()
    {
        return m_Context;
    }
    
    /**
     * Get a service by its interface.  This assumes the reference name is the same as the class name.  Test will fail
     * if service is not found.
     * 
     * @param <T>
     *      Type of the service, automatic casting
     * @param clazz
     *      Interface that the service provides
     * @return
     *      Service given the name
     */
    public static <T> T getService(final Class<T> clazz)
    {
        return m_TestService.getService(clazz);
    }
    
    /**
     * Get a service by it's service name. Name is it's key from the add reference call.
     * 
     * @param serviceName
     *      Name of the service to be retrieved.
     * @return
     *      Object that represents the retrieved service.
     */
    public static Object getService(final String serviceName)
    {
        return m_TestService.getService(serviceName);
    }
    
    /**
     * Get a service by its name.  Just a wrapper to {@link IntegrationTestService#getService(String)}.
     * 
     * @param serviceName
     *      Name of the service
     */
    public static void assertServiceReferenceFound(final String serviceName)
    {
        final Object service = m_TestService.getService(serviceName);
        assertThat(service, is(notNullValue()));
    }
    
    /**
     * Get a service by its name.  Just a wrapper to {@link IntegrationTestService#getService(String)}.
     * 
     * @param serviceName
     *      Name of the service
     * @param timeoutMS
     *      How many milliseconds to wait for service to become available 
     * @throws InterruptedException
     *      if interrupted while waiting
     */
    public static void assertServiceReferenceFound(final String serviceName, final long timeoutMS) 
            throws InterruptedException
    {
        final Object service = m_TestService.getService(serviceName, timeoutMS);
        assertThat(service, is(notNullValue()));
    }
    
    /**
     * Get the bundle that registered the service with the given name.  Just a wrapper to 
     * {@link IntegrationTestService#getServiceBundle(Class)}.
     * 
     * @param clazz
     *      interface class that the bundle provides
     * @return
     *      Service given the name
     */
    public static Bundle getServiceBundle(final Class<?> clazz)
    {
        return m_TestService.getServiceBundle(clazz);
    }
    
    /**
     * Wait for all services as defined by the {@link IntegrationTestService} for the specified time.  Fail assertion
     * if a tracked service is not found.
     * 
     * @param timeoutMS
     *      How long to wait, is rolling timeout to wait on each individual service so can take longer
     * @throws InterruptedException
     *      If the thread is interrupted while waiting
     */
    public static void waitForServices(final long timeoutMS) throws InterruptedException
    {
        m_TestService.waitForServices(timeoutMS);
    }
}
