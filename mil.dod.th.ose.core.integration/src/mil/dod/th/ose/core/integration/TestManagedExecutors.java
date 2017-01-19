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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.MissionProgramUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import example.asset.ExampleAsset;

public class TestManagedExecutors extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    private ManagedExecutors m_ManagedExecutors;
    private MissionProgramManager m_MissionProgramManager;
    private TemplateProgramManager m_TemplateProgramManager;

    @Override
    public void setUp()
    {
        m_ManagedExecutors = ServiceUtils.getService(m_Context, ManagedExecutors.class);
        assertThat(m_ManagedExecutors, is(notNullValue()));
        
        m_MissionProgramManager = ServiceUtils.getService(m_Context, MissionProgramManager.class, 5000);
        assertThat(m_MissionProgramManager, is(notNullValue()));
        
        m_TemplateProgramManager = ServiceUtils.getService(m_Context, TemplateProgramManager.class);
        assertThat(m_TemplateProgramManager, is(notNullValue()));
        
        AssetUtils.deleteAllAssets(m_Context);
        
        MissionProgramUtils.removeAllPrograms(m_Context);
    }
    
    @Override
    public void tearDown()
    {
        AssetUtils.deleteAllAssets(m_Context);
        
        MissionProgramUtils.removeAllPrograms(m_Context);
    }
    
    /*
     * Verify that an executor service that has already been shutdown doesn't affect the managed executor shutting all 
     * down.
     */
    public final void testMultiShutdown()
    {
        ExecutorService es = m_ManagedExecutors.newCachedThreadPool();
        
        es.execute(new Runnable()
        {           
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(2000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        });
        
        // shutdown service directly
        es.shutdownNow();
        
        // now shutdown indirectly using managed service
        m_ManagedExecutors.shutdownAllExecutorServicesNow();
    }
    /*
     * This test that an exception thrown by a thread in an executor service logs the thrown exception.
     */
    @SuppressWarnings({ "unchecked", "unused", "rawtypes" })
    public final void testExecutionException() throws Exception
    {
        LogReaderService logReader = ServiceUtils.getService(m_Context, LogReaderService.class);
        
        final Semaphore semaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            @Override
            public void logged(LogEntry entry)
            {
                if (entry.getMessage().contains("Executor threw an exception"))
                {
                    semaphore.release();
                }
            }
        });

        ExecutorService es = m_ManagedExecutors.newCachedThreadPool();

        Future future = es.submit(new Callable()
        {
            @Override
            public Throwable call() throws IOException
            {
                IOException throwMe = new IOException("A test exception");
                throw throwMe;
            }
        });
        
        // verify log message is read
        assertThat(semaphore.tryAcquire(5, TimeUnit.SECONDS), is(true));
    }
    
    /*
     * This tests exception logging if the exception is thrown from a running mission script
     */
    public final void testScriptExceptionLoggiong() throws Exception
    {
        LogReaderService logReader = ServiceUtils.getService(m_Context, LogReaderService.class);
        
        final Semaphore semaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            @Override
            public void logged(LogEntry entry)
            {
                if (entry.getMessage().contains("Executor threw an exception"))
                {
                    semaphore.release();
                }
            }
        });
        
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(true).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withSchedule(schedule).
                withProgramName("test-exception-executor").withTemplateName("exceptionExecutor-template");
        
        Program program = m_MissionProgramManager.loadParameters(params);
        
        try
        {
            // allow some errors to be logged to be made
            Thread.sleep(2500);
            
            // verify log message is read
            assertThat(semaphore.tryAcquire(5, TimeUnit.SECONDS), is(true));
            
            // stop asset data capture        
            m_ManagedExecutors.shutdownAllExecutorServices();
            
            // allow current iteration to finish
            Thread.sleep(500);
        }
        finally
        {
            MissionProgramUtils.shutdownSync(m_Context, program);
        }
    }

    /*
     * This tests the execution of scripts that have dependencies and the ability to stop the executor. Furthermore, 
     * this test allows time for the script/mission to execute for a specific period of time, then shuts down the 
     * executor and verifies the observations collected(from the mission), and ensures that after shutdown no additional
     * observations were made.   
     */
    public void testExecuteScript() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        asset.setName("executorAsset");
                
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        assertThat(observationStore, is(notNullValue()));

        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(true).
            withImmediately(true).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withSchedule(schedule).
            withProgramName("test-executor").withTemplateName("executor-template").
                withParameters(new MapEntry("asset", "executorAsset"));
        
        EventHandlerSyncer executionSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "test-executor"));
        
        Program program = m_MissionProgramManager.loadParameters(params);
        
        try
        {
            executionSyncer.waitForEvent(5);
            
            // allow some observations to be made
            Thread.sleep(4500);
            
            // stop asset data capture        
            m_ManagedExecutors.shutdownAllExecutorServices();
            
            // allow current iteration to finish
            Thread.sleep(1500);
            
            // should have capture 1/second 4-5 in 4500 ms, depends on how quickly this check is made
            Collection<Observation> observations1 = observationStore.queryByAsset(asset);
            assertThat("At least 4 observations not found", observations1.size() >= 4);
            
            // wait and make sure no more captured
            Thread.sleep(2500);
            Collection<Observation> observations2 = observationStore.queryByAsset(asset);
            // should still be 4
            assertThat(observations2.size(), is(observations1.size()));
        }
        finally
        {
            MissionProgramUtils.shutdownSync(m_Context, program);
        }
    }
}
