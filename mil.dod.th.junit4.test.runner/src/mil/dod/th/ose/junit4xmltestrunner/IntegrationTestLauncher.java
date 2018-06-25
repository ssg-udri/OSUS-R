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
import java.io.PrintStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.ose.config.loading.api.ConfigLoadingConstants;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;

/**
 * This is the test runner component that ensures the test runner component activates and then runs the tests provided
 * by it if auto run property is enabled.
 * 
 * @author dhumeniuk
 *
 */
@Component(immediate = true) // NOCHECKSTYLE: abstraction coupling (due to legacy code)
public class IntegrationTestLauncher
{
    /**
     * How long to wait by default before timing out start of test.
     */
    private static final int DEF_TEST_START_TIMEOUT = 30;

    /**
     * How many milliseconds in a second.
     */
    private static final int MS_PER_SECOND = 1000;

    /**
     * How many startup events are required for the {@link #m_SystemInitWaitSem}. See
     * {@link #createConfigEventHandler()} and {@link #createSystemBundleTracker(BundleContext)}.
     */
    private static final int NUM_STARTUP_EVENTS = 5;

    /**
     * Keeps track of the exception that caused the launcher to not start test, used by 
     * {@link IntegrationTestLauncherFailure}.
     */
    private static Exception m_LauncherException;
    
    /**
     * Log service used to log events.
     */
    private LogService m_LogService;

    /**
     * Framework system bundle.
     */
    private Bundle m_SystemBundle;
    
    /**
     * Folder that will contain the generated test reports.
     */
    private File m_ReportDirFile;

    /**
     * Used to track the {@link IntegrationTestRunner} service.
     */
    private ServiceTracker<IntegrationTestRunner, IntegrationTestRunner> m_TestRunnerTracker;

    /**
     * Used to track the registration of the {@link EventHandler}.
     */
    private ServiceRegistration<EventHandler> m_EventHandlerReg;

    /**
     * Service that processes commands.
     */
    private CommandProcessor m_CommandProcessor;

    /**
     * Tracks the system bundle, releases {@link #m_SystemInitWaitSem} when bundle has activated.
     */
    private BundleTracker<Object> m_SystemBundleTracker;
    
    /**
     * Semaphore for syncing full initialization of the system, which includes starting of the system bundle and
     * loading of configurations from configs.xml. Releases must be performed for the OSGi configurations being
     * loaded and all factory objects being loaded.
     */
    final private Semaphore m_SystemInitWaitSem = new Semaphore(0);

    /**
     * Used for printing data, goes to OSGi log service.
     */
    private PrintStream m_PrintStream;

    /**
     * Bind the log service.
     * 
     * @param logService
     *      log service used for logging events
     */
    @Reference
    public void setLogService(final LogService logService)
    {
        m_LogService = logService;
    }
    
    /**
     * Bind the command processor.
     * 
     * @param commandProcessor
     *      used to execute commands
     */
    @Reference
    public void setCommandProcessor(final CommandProcessor commandProcessor)
    {
        m_CommandProcessor = commandProcessor;
    }
    
    /**
     * Bind the log output stream.
     * 
     * @param outputStream
     *      stream that sends output to the OSGi LogService
     */
    @Reference
    public void setLogOutputStream(final LogOutputStream outputStream)
    {
        m_PrintStream = new PrintStream(outputStream);
    }
    
    /**
     * Activate this component by checking if auto start is enabled.  If so, wait for test runner to be activated then
     * run it.
     * 
     * @param context
     *      bundle context used to acquire system bundle
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_SystemBundle = context.getBundle(0);
        
        m_LogService.log(LogService.LOG_INFO, "Starting Test Launcher");
        
        final boolean autoStartShutdown = Boolean.parseBoolean(
                System.getProperty("mil.dod.th.ose.junit4xmltestrunner.autoStartShutdown"));
        if (!autoStartShutdown)
        {
            m_LogService.log(LogService.LOG_WARNING, "Will not start running tests, use test command to execute tests");
            return;
        }

        String reportDir = System.getProperty("mil.dod.th.ose.junit4xmltestrunner.reportDir");
        if (reportDir == null)
        {
            reportDir = ".";
        }
        
        m_ReportDirFile = new File(reportDir);
        if (!m_ReportDirFile.isDirectory() && !m_ReportDirFile.mkdirs())
        {
            m_LogService.log(LogService.LOG_ERROR, 
                    "Unable to use given directory: " + m_ReportDirFile.getAbsolutePath());
            return;
        }
        
        m_LogService.log(LogService.LOG_INFO, "Will put JUnit reports here: " + m_ReportDirFile.getAbsolutePath());
        
        m_TestRunnerTracker = new ServiceTracker<IntegrationTestRunner, IntegrationTestRunner>(context, 
              IntegrationTestRunner.class.getName(), null);
        m_TestRunnerTracker.open();

        m_SystemBundleTracker = createSystemBundleTracker(context);
        m_SystemBundleTracker.open();
    
        final EventHandler eventHandler = createConfigEventHandler();

        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(EventConstants.EVENT_TOPIC,
            new String[] {ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT,
                          ConfigLoadingConstants.TOPIC_CONFIG_OSGI_COMPLETE_EVENT,
                          ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_OBJS_COMPLETE_EVENT,
                          ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT});
        m_EventHandlerReg = context.registerService(EventHandler.class, eventHandler, props);

        runTestRunner();
    }
    
    /**
     * Creates a bundle tracker for the system bundle.
     * 
     * @param context
     *      bundle context reference
     * @return
     *      system bundle tracker
     */
    private BundleTracker<Object> createSystemBundleTracker(final BundleContext context)
    {
        return new BundleTracker<Object>(context, Bundle.ACTIVE, new BundleTrackerCustomizer<Object>()
        {
            @Override
            public void removedBundle(final Bundle bundle, final BundleEvent event, final Object object)
            {
                // nothing needs to be done if bundle is removed
            }
            
            @Override
            public void modifiedBundle(final Bundle bundle, final BundleEvent event, final Object object)
            {
                // nothing needs to be done if bundle is removed
            }
            
            @Override
            public Object addingBundle(final Bundle bundle, final BundleEvent event)
            {
                if (bundle.equals(m_SystemBundle))
                {
                    m_SystemInitWaitSem.release();
                    m_LogService.log(LogService.LOG_INFO, 
                         String.format("System bundle started, released semaphore [%s] to trigger test runner thread", 
                                 m_SystemInitWaitSem));
                }
                
                return null;
            }
        });
    }

    /**
     * Creates an event handler to be used for configuration loading events.
     * 
     * @return
     *      configuration loading event handler
     */
    private EventHandler createConfigEventHandler()
    {
        return new EventHandler()
        {
            private boolean m_OsgiComplete;
            private boolean m_FactObjsComplete;
            private boolean m_AddressComplete;
            private boolean m_ConfigProcessingComplete;

            @Override
            public void handleEvent(final Event event)
            {
                if (event.getTopic().equals(ConfigLoadingConstants.TOPIC_CONFIG_OSGI_COMPLETE_EVENT) 
                        && !m_OsgiComplete)
                {
                    m_OsgiComplete = true;
                    m_SystemInitWaitSem.release();
                    m_LogService.log(LogService.LOG_INFO, 
                        String.format("OSGi configs loaded, released semaphore [%s] to trigger test runner thread",
                                      m_SystemInitWaitSem));
                }
                else if (event.getTopic().equals(ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT)
                    && !m_FactObjsComplete)
                {
                    m_FactObjsComplete = true;
                    m_SystemInitWaitSem.release();
                    m_LogService.log(LogService.LOG_INFO, 
                        String.format("Factory objects loaded, released semaphore [%s] to trigger test runner thread",
                                      m_SystemInitWaitSem));
                }
                else if (event.getTopic().equals(ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_OBJS_COMPLETE_EVENT)
                        && !m_AddressComplete)
                {
                    m_AddressComplete = true;
                    m_SystemInitWaitSem.release();
                    m_LogService.log(LogService.LOG_INFO, 
                        String.format("Addresses loaded, released semaphore [%s] to trigger test runner thread",
                                      m_SystemInitWaitSem));
                }
                else if (event.getTopic().equals(ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT)
                        && !m_ConfigProcessingComplete)
                {
                    m_ConfigProcessingComplete = true;
                    m_SystemInitWaitSem.release();
                    m_LogService.log(LogService.LOG_INFO, 
                        String.format("Processing complete, released semaphore [%s] to trigger test runner thread",
                                      m_SystemInitWaitSem));
                }
                else
                {
                    m_LogService.log(LogService.LOG_ERROR, 
                        String.format("Unexpected event %s, trying to trigger test runner thread", event.getTopic()));
                }
            }
        };
    }

    /**
     * Wait for the {@link IntegrationTestService} to activate and then run it if available.
     */
    private void runTestRunner()
    {
        final Thread thread = new Thread(new TestRunner());
        thread.setName("IntegrationTestLauncher");
        thread.start();
    }
    
    /**
     * Print information about loaded bundles and service components.
     */
    private void printSystemInfo()
    {
        final CommandSession session = m_CommandProcessor.createSession(System.in, m_PrintStream, m_PrintStream);
        try
        {
            session.execute("lb");
            session.execute("scr:list");
        }
        catch (final Exception e)
        {
            m_LogService.log(LogService.LOG_ERROR, "Unable to execute commands", e);
        }
    }

    /**
     * Retrieve how long to wait for timing out test startup.  Use property if available, otherwise, use default.
     * 
     * @return
     *      timeout value in seconds
     */
    private int getTimeout()
    {
        final String timeoutString = 
            System.getProperty("mil.dod.th.ose.junit4xmltestrunner.timeoutSec");
        
        if (timeoutString == null)
        {
            return DEF_TEST_START_TIMEOUT;
        }
        else
        {
            return Integer.parseInt(timeoutString);
        }
    }
    
    /**
     * Method will shutdown the OSGi framework.  This can be done in the context of an activation method
     * since it will start a separate thread to shut things down.
     */
    private void shutdownFramework()
    {
        final Thread thread = new Thread(new Runnable()
        {
            
            @Override
            public void run()
            {
                try
                {
                    m_SystemBundle.stop();
                }
                catch (final BundleException e)
                {
                    m_LogService.log(LogService.LOG_ERROR, "Failed to stop system bundle", e);
                }
            }
        });
        
        thread.start();
    }
    
    /**
     * Create a dummy test report to show that the framework failed to start meaning the test couldn't be run.
     * 
     * @param cause
     *      cause of failure
     * @param suffix
     *      suffix to add to all test classes
     */
    private void createFailureXMLTestReport(final Exception cause, final String suffix)
    {
        try
        {
            m_LauncherException = cause;
            XMLTestRunner.run(m_ReportDirFile, suffix, false, IntegrationTestLauncherFailure.class);
        }
        catch (final IOException e)
        {
            // current method used to report failure, no need to do further handling
            m_LogService.log(LogService.LOG_ERROR, "Failed to execute dummy test", e);
        }
    }
    
    /**
     * Get the exception that caused the launcher to fail starting a test.
     * 
     * @return
     *      last exception
     */
    public static Exception getTestException()
    {
        return m_LauncherException;
    }
    
    /**
     * Runs all of the tests according to properties on a separate thread.
     * 
     * @author dhumeniuk
     *
     */
    private final class TestRunner implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                final int timeout = getTimeout();
                IntegrationTestRunner testRunner = null;
                try
                {
                    m_LogService.log(LogService.LOG_INFO, 
                            String.format("Waiting for system initialization for %d seconds", timeout));

                    final String unknownTarget = "unknown";

                    // Multiple permits should be acquired since system initialization is dependent on different events
                    if (m_SystemInitWaitSem.tryAcquire(NUM_STARTUP_EVENTS, timeout, TimeUnit.SECONDS))
                    {
                        m_LogService.log(LogService.LOG_INFO, 
                                String.format("Waiting for integration tests for %d seconds", timeout));
                        testRunner = m_TestRunnerTracker.waitForService(timeout * MS_PER_SECOND);
                        if (testRunner == null)
                        {
                            createFailureXMLTestReport(new Exception("Timeout waiting for service tracker: " 
                                    + m_TestRunnerTracker), unknownTarget);
                        }
                        m_TestRunnerTracker.close();
                    }
                    else
                    {
                        final String message = String.format("Timeout waiting for system and events %s, %s, %s and %s",
                            ConfigLoadingConstants.TOPIC_CONFIG_OSGI_COMPLETE_EVENT,
                            ConfigLoadingConstants.TOPIC_CONFIG_FACTORY_OBJS_COMPLETE_EVENT,
                            ConfigLoadingConstants.TOPIC_CONFIG_ADDRESS_OBJS_COMPLETE_EVENT,
                            ConfigLoadingConstants.TOPIC_CONFIG_PROCESSING_COMPLETE_EVENT);
                        m_LogService.log(LogService.LOG_ERROR, 
                              String.format("%s [%s], waited for %d seconds", m_SystemInitWaitSem, message, timeout));
                        createFailureXMLTestReport(new Exception(String.format(
                                "Timeout (%d) waiting for system to initialize. Search for '%s' in the console log",
                                timeout, message)), unknownTarget);
                    }

                    m_SystemBundleTracker.close();
                    m_EventHandlerReg.unregister();
                }
                catch (final InterruptedException e)
                {
                    m_LogService.log(LogService.LOG_ERROR, "Interrupted while waiting for tests", e);
                }
                
                if (testRunner != null)
                {
                    try
                    {
                        final String delimiter = ",";
                        
                        // by default run all, run subset if set
                        final String testClasses = 
                            System.getProperty("mil.dod.th.ose.junit4xmltestrunner.testClasses");
                        if (testClasses == null)
                        {
                            testRunner.runAll(m_ReportDirFile, false);
                        }
                        else
                        {
                            final String[] classNames = testClasses.split(delimiter);
                            testRunner.runClass(m_ReportDirFile, false, classNames);
                        }
                        
                        //if optional classes, 
                        //then run optional classes in addition to the default or subset                   
                        final String optionalClasses = 
                            System.getProperty("mil.dod.th.ose.junit4xmltestrunner.optionalClasses");

                        if (optionalClasses != null && !optionalClasses.equals(""))
                        {
                            final String[] classNames = optionalClasses.split(delimiter);
                            testRunner.runClass(m_ReportDirFile, false, classNames);
                        }
                    }
                    catch (final Exception e)
                    {
                        createFailureXMLTestReport(e, testRunner.getSuffix());
                    }
                }
            }
            finally
            {
                printSystemInfo();
                
                m_LogService.log(LogService.LOG_INFO, "Shutting down OSGi framework");
                shutdownFramework();
            }
        }
    }
}
