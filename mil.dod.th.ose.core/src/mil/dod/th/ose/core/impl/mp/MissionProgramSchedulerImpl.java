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
package mil.dod.th.ose.core.impl.mp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;
import mil.dod.th.ose.shared.WrappedExceptionExtractor;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Implementation of the MissionProgramScheduler interface.
 * @author callen
 *
 */
@Component 
public class MissionProgramSchedulerImpl implements MissionProgramScheduler
{
    /**
     * Number of thread to place in the executor pool.
     */
    private static final int THREAD_POOL_MAX_SIZE = 10;
    
    /**
     * Map of mission programs that execute on a schedule.
     */
    private final Map<String, Future<?>> m_ScheduledFuturePrograms = 
        Collections.synchronizedMap(new HashMap<String, Future<?>>());
    
    /**
     * Map of mission programs that execute on a schedule and need to be shutdown in the future.
     */
    private final Map<String, Future<?>> m_ScheduledProgramShutdowns = 
        Collections.synchronizedMap(new HashMap<String, Future<?>>());
    
    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Event admin service to use.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Scheduled thread pool executor. Used for executing, testing and shutting down all programs known to this service.
     */
    private ScheduledExecutorService m_Executor;

    private MissionProgramRuntime m_MissionProgramRuntime;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service for posting events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    @Reference
    public void setMissionProgramRuntime(final MissionProgramRuntime runtime)
    {
        m_MissionProgramRuntime = runtime;
    }

    /**
     * Activate this service and create the thread pool for executing mission programs.
     */
    @Activate
    public void activate()
    {
        m_Executor = Executors.newScheduledThreadPool(THREAD_POOL_MAX_SIZE);
    }
    
    /**
     * Shutdown the executor.
     */
    @Deactivate
    public void deactivate()
    {
        m_Executor.shutdownNow();
    }
    
    @Override
    public synchronized void executeProgram(final ProgramImpl program) throws IllegalStateException
    {
        //make sure the mission isn't already scheduled, if so just remove
        final Future<?> previousSchedule = m_ScheduledFuturePrograms.remove(program.getProgramName());
        if (previousSchedule != null)
        {
            previousSchedule.cancel(false);
            if (!previousSchedule.isCancelled())
            {
                new IllegalStateException(String.format("Mission [%s] cannot execute because it is already scheduled, "
                        + "and cannot be cancelled.", program.getProgramName()));
            }
            immediatelyExecute(program);
            return;
        }

        //check if the mission is scheduled
        if (program.getMissionSchedule().isSetStartInterval())
        {
            //get the schedule
            final MissionProgramSchedule schedule = program.getMissionSchedule();
            //current time
            final long currTime = System.currentTimeMillis();
            
            //change status to scheduled
            program.changeStatus(ProgramStatus.SCHEDULED);
            
            //get the future object, and save for removal
            final Future<?> futureTask = m_Executor.schedule(new MissionExecutionRunner(program), 
                schedule.getStartInterval() - currTime, TimeUnit.MILLISECONDS);
            
            m_Logging.debug("The time to wait to start scheduled mission [%s] is: %s millis, the current time is: %s", 
                    program.getProgramName(), schedule.getStartInterval() - currTime, currTime);
            
            //save for later
            m_ScheduledFuturePrograms.put(program.getProgramName(), futureTask);
        }
        else
        {
            immediatelyExecute(program);
        }        
    }

    @Override //NOPMD: Junit tests should have the correct annotation. This is not a junit test, but descriptive method
    public Future<TestResult> testProgram(final ProgramImpl program) //name that describes executing a mission's
    {
        //test function.
        return m_Executor.submit(new MissionTestRunner(program));
    }

    @Override
    public synchronized void shutdownProgram(final ProgramImpl program)
    {
        m_Executor.submit(new MissionShutdownRunner(program));
    }
    
    @Override
    public boolean cancelScheduledProgram(final String programName) throws IllegalArgumentException
    {
        final Future<?> scheduledProgram = m_ScheduledFuturePrograms.remove(programName);
        if (scheduledProgram == null)
        {
            throw new IllegalArgumentException(String.format("Program %s is not known to be a scheduled program", 
                programName));
        }
        final Future<?> shutdown = m_ScheduledProgramShutdowns.remove(programName);
        //could be null if scheduled to run indefinitely
        if (shutdown != null)
        {
            shutdown.cancel(false);
        }
        //this will only work if the task has not started. The cancel argument of false means that this call
        //will be denied if execution is happening at the time of this call.
        return scheduledProgram.cancel(false);
    }
    
    /**
     * Execute a program immediately.
     * @param program
     *     the program to immediately execute
     */
    private void immediatelyExecute(final ProgramImpl program)
    {
        //execute NOW
        final Future<?> futureTask = m_Executor.submit(new MissionExecutionRunner(program));
        //store the program name with its task
        m_ScheduledFuturePrograms.put(program.getProgramName(), futureTask);
    }
    /**
     * Runnable that the parent class uses to execute {@link Program}s.
     * @author callen
     *
     */
    public class MissionExecutionRunner implements Runnable
    {
        /**
         * The program to execute.
         */
        private final ProgramImpl m_Program;
        
        /**
         * Constructor that stores away the program to execute.
         * @param program
         *     the program that will be executed at the specified time.
         */
        public MissionExecutionRunner(final ProgramImpl program)
        {
            m_Program = program;
        }
        
        @Override
        public void run()
        {
            // thread context class loader is used by script engine to access the TH API, set it to the runtime's class 
            // loader
            Thread.currentThread().setContextClassLoader(m_MissionProgramRuntime.getClassLoader());
            
            m_Logging.debug("The time that mission [%s] started executing is: %s", m_Program.getProgramName(), 
                    System.currentTimeMillis());
            try
            {
                m_Logging.info("Starting to execute [%s]", m_Program.getProgramName());
                m_Program.changeStatus(ProgramStatus.EXECUTING);
                m_Program.getMissionScript().execute();
                
                //update the program's status
                m_Program.changeStatus(ProgramStatus.EXECUTED);
                m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_EXECUTED, m_Program.getEventProperties()));
                
                m_Logging.info("Executed [%s] successfully", m_Program.getProgramName());
            }
            catch (final Exception e)
            {
                //change the status
                m_Program.changeStatus(ProgramStatus.SCRIPT_ERROR);
                
                //log the error
                m_Logging.error(e, "Executing [%s] failed!", m_Program.getProgramName());
                
                //post the event
                final Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(Program.EVENT_PROP_PROGRAM_EXCEPTION, WrappedExceptionExtractor.getRootCauseMessage(e));
                properties.putAll(m_Program.getEventProperties());
                m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_EXECUTED_FAILURE, properties));
                m_Program.setExecutionExceptionMessage(WrappedExceptionExtractor.getRootCauseMessage(e));
            }
            
            //remove me from the queue
            m_ScheduledFuturePrograms.remove(m_Program.getProgramName());
            
            //if not indefinite need to schedule the shutdown
            final MissionProgramSchedule schedule = m_Program.getMissionSchedule();
            if (schedule.isSetStopInterval())
            {
                final long currTime = System.currentTimeMillis();

                //get the future for calling shutdown, delta of shutdown already calculated
                final Future<?> futureTaskShutdown = m_Executor.schedule(
                    new MissionShutdownRunner(m_Program), schedule.getStopInterval() - currTime, TimeUnit.MILLISECONDS);
                
                m_Logging.debug("The time to wait to stop [%s] which is a scheduled mission is : %s millis, "
                    + "current time is: %s", m_Program.getProgramName(), schedule.getStopInterval() - currTime, 
                        currTime);
                //save in case manual shutdown is requested
                m_ScheduledProgramShutdowns.put(m_Program.getProgramName(), futureTaskShutdown);
            }
        }
    }
    
    /**
     * Runnable that the parent class uses to shutdown {@link Program}s.
     * @author callen
     *
     */
    public class MissionShutdownRunner implements Runnable
    {
        /**
         * The program to execute.
         */
        private final ProgramImpl m_Program;
        
        /**
         * Constructor that stores away the program to shutdown.
         * @param program
         *     the program that will be shutdown.
         */
        public MissionShutdownRunner(final ProgramImpl program)
        {
            m_Program = program;
        }
        
        @Override
        public void run()
        {
            // thread context class loader is used by script engine to access the TH API, set it to the runtime's class 
            // loader
            Thread.currentThread().setContextClassLoader(m_MissionProgramRuntime.getClassLoader());
            
            //remove the shutdown registration if this is called for a scheduled program            
            final Future<?> schedFuture = m_ScheduledProgramShutdowns.remove(m_Program.getProgramName());
            
            m_Logging.debug("The time that mission [%s] started shutdown is: %s", m_Program.getProgramName(), 
                    System.currentTimeMillis());
            final Map<String, Object> properties = new HashMap<String, Object>();
            
            m_Logging.debug("ProgramStatus for %s is %s", m_Program.getProgramName(), m_Program.getProgramStatus());
            try
            {
                m_Program.changeStatus(ProgramStatus.SHUTTING_DOWN);
            }
            catch (final IllegalStateException e)
            {
                m_Logging.warning("Mission program [%s] is trying to shutdown, but hasn't finished executing.", 
                        m_Program.getProgramName());
                //post failure event
                properties.put(Program.EVENT_PROP_PROGRAM_EXCEPTION, WrappedExceptionExtractor.getRootCauseMessage(e));
                properties.putAll(m_Program.getEventProperties());
                m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_SHUTDOWN_FAILURE, properties));
                return;
            }
            try
            {
                m_Program.getMissionScript().shutdown();
                
                m_Logging.info("Program [%s] shutdown successfully!", m_Program.getProgramName());
                
                //update the programs status and post event
                m_Program.changeStatus(ProgramStatus.SHUTDOWN);
                properties.putAll(m_Program.getEventProperties());
                m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_SHUTDOWN, properties));
                m_Logging.info("Event posted for program [%s] successful shutdown.", m_Program.getProgramName());
            }
            catch (final Exception e)
            {
                //change the status
                m_Program.changeStatus(ProgramStatus.SCRIPT_ERROR);
                
                //log the error
                m_Logging.error(e, "shutdown [%s] failed!", m_Program.getProgramName());
                
                //post failure event
                properties.putAll(m_Program.getEventProperties());
                properties.put(Program.EVENT_PROP_PROGRAM_EXCEPTION, WrappedExceptionExtractor.getRootCauseMessage(e));
                m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_SHUTDOWN_FAILURE, properties));
            }
            schedFuture.cancel(true);
        }
    }
    
    /**
     * Runnable that the parent class uses to test {@link Program}s.
     * @author callen
     *
     */
    class MissionTestRunner implements Callable<TestResult>
    {
        /**
         * The program to test execute.
         */
        private final ProgramImpl m_Program;

        /**
         * Constructor that stores away the program to test execute.
         * @param program
         *     the program to test execute
         */
        MissionTestRunner(final ProgramImpl program)
        {
            m_Program = program;
        }
        
        @Override
        public TestResult call() throws MissionProgramException
        {
            // thread context class loader is used by script engine to access the TH API, set it to the runtime's class 
            // loader
            Thread.currentThread().setContextClassLoader(m_MissionProgramRuntime.getClassLoader());
            
            m_Logging.debug("The time that mission [%s] started test execution is: %s", m_Program.getProgramName(), 
                    System.currentTimeMillis());
            
            //map for event properties
            final Map<String, Object> properties = new HashMap<String, Object>();
            
            //save the current state of the mission
            final ProgramStatus prevStatus = m_Program.getProgramStatus();
            
            final TestResult test;
            try
            {
                m_Logging.info("Starting to test execute [%s]", m_Program.getProgramName());
                m_Program.changeStatus(ProgramStatus.EXECUTING_TEST);
                test = m_Program.getMissionScript().test();
            }
            catch (final Exception e)
            {
                m_Logging.error(e,
                        "Test for [%s] incurred an exception during testing.", m_Program.getProgramName());
                
                m_Program.setTestResults(TestResult.FAILED);
                
                //post the outcome event
                m_Program.changeStatus(prevStatus);
                properties.putAll(m_Program.getEventProperties());
                properties.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION, 
                        WrappedExceptionExtractor.getRootCauseMessage(e));
                properties.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT, TestResult.FAILED);
                m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_TESTED, properties));
                m_Program.setTestExceptionMessage(WrappedExceptionExtractor.getRootCauseMessage(e));
                throw new MissionProgramException(e.getCause()); //NOPMD the stack trace is not lost. The warning is
                //not applicable in this scenario because the caught exception is already a wrapper.
            }
            
            //put the program's status back to the original state
            m_Program.changeStatus(prevStatus);
            m_Program.setTestResults(test);
            properties.put(Program.EVENT_PROP_PROGRAM_TEST_RESULT, test);
            properties.putAll(m_Program.getEventProperties());
            m_EventAdmin.postEvent(new Event(Program.TOPIC_PROGRAM_TESTED, properties));
            m_Logging.info("Test for [%s] completed with result %s", m_Program.getProgramName(), test);
            return test;
        }
    }
}
