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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.security.PrivilegedActionException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import mil.dod.th.core.mp.MissionScript;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * @author callen
 *
 */
public class TestMissionProgramSchedulerImpl
{
    private MissionProgramSchedulerImpl m_SUT;
    private ProgramImpl m_Program1;
    private ProgramImpl m_Program2;
    private MissionScript m_Script;
    
    @Mock private EventAdmin m_EventAdmin;
    @Mock private MissionProgramRuntime m_Runtime;
    
    @Before
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new MissionProgramSchedulerImpl();
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setMissionProgramRuntime(m_Runtime);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());
        
        // must set to valid class loader, any will do since running in single class loader environment, if not mocked
        // class loader will be null and nothing can be loaded
        when(m_Runtime.getClassLoader()).thenReturn(getClass().getClassLoader());
        
        //programs
        m_Program1 = mock(ProgramImpl.class);
        m_Program2 = mock(ProgramImpl.class);
        when(m_Program1.getProgramName()).thenReturn("namer");
        when(m_Program2.getProgramName()).thenReturn("namer2");
        Map<String, Object> props = getEventPropsForMock(m_Program1);
        when(m_Program1.getEventProperties()).thenReturn(props);
        Map<String, Object> props2 = getEventPropsForMock(m_Program2);
        when(m_Program2.getEventProperties()).thenReturn(props2);
        
        //mock script object
        m_Script = mock(MissionScript.class);
        when(m_Program1.getMissionScript()).thenReturn(m_Script);
        when(m_Program2.getMissionScript()).thenReturn(m_Script);
    }
    
    /**
     * Test activation and deactivation.
     */
    @Test
    public void testActivateDeactivate()
    {
        //activate the component
        m_SUT.activate();
        
        //deactivate the component
        m_SUT.deactivate();
    }
    
    /**
     * Test that a program can be registered and executed.
     * Verify the programs are registered.
     */
    @Test
    public void testExecuteSimplePosting() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();
        
        //schedule
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 100L, 
                null, true, null, false, true);
        
        //more behavior for the program instance
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        //load the program
        m_SUT.executeProgram(m_Program1);
        
        //wait for it....
        Thread.sleep(200);
        
        //should just be once for execution
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
    }
    
    /**
     * Test executing multiple programs.
     * Verify that the programs execute.
     */
    @Test
    public void testExecutegOneInALittleWhileOneLater() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();
        
        //schedules
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 100L, 
                null, true, null, true, true);
        MissionProgramSchedule schedule2 = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 200L, 
                null, true, null, true, true);
        
        //more behavior for the program instance
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        when(m_Program2.getMissionSchedule()).thenReturn(schedule2);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        
        //execute
        m_SUT.executeProgram(m_Program1);
        m_SUT.executeProgram(m_Program2);
        
        Thread.sleep(150);
        
        //once for the first program executing
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
        
        //wait just a little while longer
        Thread.sleep(300);
        
        //once for each program executing
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program2);
        
        //sleep a little longer
        Thread.sleep(200);
        
        //verify no more events
        verify(m_EventAdmin, times(2)).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Test an immediate execution mission and a scheduled mission.
     * Verify correct programs are executed at the expected times.
     */
    @Test
    public void testeventPostingOneNowOneLater() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();

        //now schedule
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(null, null, true, true, true, true);
        
        MissionProgramSchedule schedule2 = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 200L,
                null, true, false, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        when(m_Program2.getMissionSchedule()).thenReturn(schedule2);
        
        m_SUT.executeProgram(m_Program1);
        m_SUT.executeProgram(m_Program2);
        
        Thread.sleep(100);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //once for the first program
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().get(0).getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getAllValues().get(0), m_Program1);
        
        //wait just a little while longer
        Thread.sleep(200);
        //one more time for the second programs execution
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program2);
    }
    
    /**
     * Test removing a mission from the executor queue.
     * Verify the mission DOES NOT execute.
     */
    @Test
    public void testRemoval() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();

        //quick schedule, shouldn't be able to remove, will already be gone
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 100L,
                null, true, false, true, true);
        //should be able to remove
        MissionProgramSchedule schedule2 = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 400L,
                null, true, false, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        when(m_Program2.getMissionSchedule()).thenReturn(schedule2);
        
        m_SUT.executeProgram(m_Program1);
        m_SUT.executeProgram(m_Program2);
        
        //wait for execution
        Thread.sleep(250);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);

        //remove the remaining one
        assertThat(m_SUT.cancelScheduledProgram("namer2"), is(true));
        
        //sleep again to make sure the removed program does NOT execute.
        Thread.sleep(450);
        
        //try again for exception
        try
        {
            m_SUT.cancelScheduledProgram("namer2");
            fail("expecting exception the program was already removed");
        }
        catch (IllegalArgumentException e)
        {
            //expecting exception
        }
        
       //verify there was only one invocation for the execution of the first program
        verify(m_EventAdmin, times(1)).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Test canceling a mission from the executor queue.
     * Verify the mission DOES NOT execute.
     */
    @Test
    public void testCancelWithShutdownRemoval() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();

        //quick schedule, shouldn't be able to remove will already be gone
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 100L,
                System.currentTimeMillis() + 300L, true, false, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        m_SUT.executeProgram(m_Program1);
        
        //remove the remaining one
        assertThat(m_SUT.cancelScheduledProgram("namer"), is(true));
        
        //sleep again to make sure the removed program does NOT execute.
        Thread.sleep(185);
        
        //verify there was only one invocation for the execution of the program and not of the shutdown because that
        //should of been cancelled.
        verify(m_EventAdmin, never()).postEvent(Mockito.any(Event.class));
    }
    
    /**
     * Test executing a program where the script execution throws an exception.
     * Verify event contains the exception and that the correct topic is used.
     */
    @Test
    public void testException() throws InterruptedException, PrivilegedActionException
    {
        //activate the component
        m_SUT.activate();
        
        doThrow(new PrivilegedActionException(new Exception("EXCEPTION PIZZA!"))).when(m_Script).execute();
        
        //now execution
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(null, null, true, true, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        m_SUT.executeProgram(m_Program1);
        
        //wait for execution
        Thread.sleep(50);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //once for the execution failed event
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        int exceptEventIndex = 0;
        for(Event event : eventCaptor.getAllValues())
        {
            if(event.getTopic().equals(Program.TOPIC_PROGRAM_EXECUTED_FAILURE))
            {
                break;
            }
            else
            {
                exceptEventIndex++;
            }
        }
        assertThat(eventCaptor.getAllValues().get(exceptEventIndex).getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED_FAILURE));
        assertProgramEventProps(eventCaptor.getAllValues().get(exceptEventIndex), m_Program1);
        assertThat((String)eventCaptor.getAllValues().get(exceptEventIndex).
                getProperty(Program.EVENT_PROP_PROGRAM_EXCEPTION), is("EXCEPTION PIZZA!"));
    }
    
    /**
     * Test submitting the same program twice.
     * Verify illegal arg exception.
     */
    @Test
    public void testSubmitTwice() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();

        //quick schedule, shouldn't be able to remove will already be gone
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 300L, null,
                true, false, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        m_SUT.executeProgram(m_Program1);
        
        //try to execute the same program again
        m_SUT.executeProgram(m_Program1);
        
        //give enough time to make sure the program only executes ONCE
        Thread.sleep(700);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(1)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
    }
    
    /**
     * Test submitting a program for scheduled execution with an ending time.
     */
    @Test
    public void testSchdeuledExecution() throws InterruptedException, PrivilegedActionException
    {
        //activate the component
        m_SUT.activate();
        
        //quick schedule, shouldn't be able to remove will already be gone
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 200L, 
            System.currentTimeMillis() + 201L, 
            false, true, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        m_SUT.executeProgram(m_Program1);
        
        //verify interactions
        verify(m_Script, timeout(1000)).execute();
        verify(m_Script, timeout(1500)).shutdown();
    }
    
    /**
     * Test submitting a program for scheduled execution with an ending time.
     * Verify shutdown not successful if status isn't executed
     */
    @Test
    public void testSchdeuledExecutionStillExecAtShutdown() throws InterruptedException, PrivilegedActionException
    {
        //activate the component
        m_SUT.activate();
        
        //quick schedule, shouldn't be able to remove, will already be gone
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() + 100L, 
            System.currentTimeMillis() + 150L, 
            false, true, true, true);
        
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        m_SUT.executeProgram(m_Program1);
        
        //verify interactions
        verify(m_Script, timeout(500)).execute();

        //mock that the program is STILL executing
        doThrow(new IllegalStateException()).when(m_Program1).changeStatus(Mockito.any(ProgramStatus.class));
        
        //wait for shutdown to try
        Thread.sleep(100);
        
        //verify it was unsuccessful because the status change was denied
        verify(m_Script, never()).shutdown();
    }
    
    /**
     * Test that a program can be registered and test executed.
     * Verify the program is test executed.
     */
    @Test
    public void testTestExecution() throws InterruptedException, PrivilegedActionException
    {
        //activate the component
        m_SUT.activate();
        
        //script behavior
        when(m_Script.test()).thenReturn(TestResult.PASSED);
        
        //load the program
        m_SUT.testProgram(m_Program1);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, timeout(1000)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_TESTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
        assertThat((TestResult)eventCaptor.getValue().getProperty(Program.EVENT_PROP_PROGRAM_TEST_RESULT), 
                is(TestResult.PASSED));
    }
    
    /**
     * Test that a program can be registered and test executed, the program is restored.
     * Verify that the state of mission is redeemed. 
     * Verify the program is test executed.
     */
    @Test
    public void testTestExecutionStatePreserved() throws InterruptedException, PrivilegedActionException
    {
        //activate the component
        m_SUT.activate();
        
        //script behavior
        when(m_Script.test()).thenReturn(TestResult.PASSED);
        
        when(m_Program1.getProgramStatus()).thenReturn(ProgramStatus.SCHEDULED);
        
        //load the program
        m_SUT.testProgram(m_Program1);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, timeout(1000)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_TESTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
        assertThat((TestResult)eventCaptor.getValue().getProperty(Program.EVENT_PROP_PROGRAM_TEST_RESULT), 
                is(TestResult.PASSED));
        
        //verify state preservation
        assertThat(m_Program1.getProgramStatus(), is(ProgramStatus.SCHEDULED));
    }
    
    /**
     * Test that a program can be registered and executed, and that if the schedule is not indefinite that shutdown is
     * invoked.
     */
    @Test
    public void testExecuteImmediateWithShutdownPosting() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();
        
        //schedule
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(null, 
                System.currentTimeMillis() + 100L, false, true, true, true);
        
        //more behavior for the program instance
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        when(m_Program1.getProgramStatus()).thenReturn(ProgramStatus.WAITING_INITIALIZED);
        
        //load the program
        m_SUT.executeProgram(m_Program1);
        
        //wait for it, give time for execution and shutdown. 
        Thread.sleep(150);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //once for execution once for shut down
        verify(m_EventAdmin, times (2)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getAllValues().get(0).getTopic(), 
            is(Program.TOPIC_PROGRAM_EXECUTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
        assertThat(eventCaptor.getValue().getTopic(), 
                is(Program.TOPIC_PROGRAM_SHUTDOWN));
        
        assertThat(m_Program1.getProgramStatus(), is(ProgramStatus.WAITING_INITIALIZED));
    }
    
    /**
     * Test that a program can be registered and test executed, but that if an exception is incurred that is it is 
     * contained within the future object returned.
     */
    @Test
    public void testExecuteTestWithException() throws InterruptedException, PrivilegedActionException
    {
        //activate the component
        m_SUT.activate();
        
        //schedule
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(null, 
                null, true, true, true, true);
        
        //more behavior for the program instance
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        doThrow(new PrivilegedActionException(new Exception("exception something failed"))).when(m_Script).test();
        
        //load the program
        Future<?> future = m_SUT.testProgram(m_Program1);
        
        //wait for it....
        Thread.sleep(100);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
            is(Program.TOPIC_PROGRAM_TESTED));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
        
        try
        {
            future.get();
        }
        catch (ExecutionException e)
        {
            assertThat(e.getCause().getMessage(), is("java.lang.Exception: exception something failed"));
        }
    }
    
    /**
     * Test that a program can be registered and executed, but the scheduled time is in the past.
     */
    @Test
    public void testExecutePastScheduled() throws InterruptedException
    {
        //activate the component
        m_SUT.activate();
        
        //schedule
        MissionProgramSchedule schedule = TestProgramImpl.createMissionSchedule(System.currentTimeMillis() - 1000L, 
                System.currentTimeMillis() - 100L, false, false, true, true);
        
        //more behavior for the program instance
        when(m_Program1.getMissionSchedule()).thenReturn(schedule);
        
        //load the program
        m_SUT.executeProgram(m_Program1);
        
        //give time for execution and shutdown which should happen immediately afterward
        Thread.sleep(500);
        verify(m_Program1).changeStatus(ProgramStatus.SHUTDOWN);
        
        Thread.sleep(100);
        
        //once for shutdown and execute only
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times (2)).postEvent(eventCaptor.capture());
        
        //verify no more status changes
        verify(m_Program1, times(1)).changeStatus(ProgramStatus.SHUTDOWN);
        verify(m_Program1, times(1)).changeStatus(ProgramStatus.EXECUTED);
    }
    
    /**
     * Test that a program can be shutdown.
     */
    @Test
    public void testShutdownProgram() throws InterruptedException
    {
        m_SUT.activate();
        m_SUT.shutdownProgram(m_Program1);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //once for shutdown topic (mocked status changes do not post events)
        verify(m_EventAdmin, timeout(100).times (1)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
                is(Program.TOPIC_PROGRAM_SHUTDOWN));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);

        verify(m_Program1).changeStatus(ProgramStatus.SHUTTING_DOWN);
        verify(m_Program1).changeStatus(ProgramStatus.SHUTDOWN);
    }
    
    /**
     * Test shutting down a program and an exception is incurred.
     * Verify further attempts.
     * Verify stop after time out.
     */
    @Test
    public void testShutdownProgramException() throws InterruptedException, PrivilegedActionException
    {
        m_SUT.activate();
        doThrow(new PrivilegedActionException(new Exception("I am an Exception"))).when(m_Script).shutdown();
        m_SUT.shutdownProgram(m_Program1);
        
        Thread.sleep(100);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        //once for shutdown topic (mocked status changes do not post events)
        verify(m_EventAdmin, times (1)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
                is(Program.TOPIC_PROGRAM_SHUTDOWN_FAILURE));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
    }

    /**
     * Test that the shutdown times out after a defined number of failed attempts.
     */
    @Test
    public void testShutdownTimeout() throws InterruptedException
    {
        m_SUT.activate();
        // cannot shutdown a program while executing
        when(m_Program1.getProgramStatus()).thenReturn(ProgramStatus.EXECUTING);
        doThrow(new IllegalStateException()).when(m_Program1).changeStatus(ProgramStatus.SHUTTING_DOWN);
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        m_SUT.shutdownProgram(m_Program1);
        
        //shutdown retries every second
        Thread.sleep(100);
        
        //once for shutdown topic (mocked status changes do not post events)
        verify(m_EventAdmin, times (1)).postEvent(eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getTopic(), 
                is(Program.TOPIC_PROGRAM_SHUTDOWN_FAILURE));
        assertProgramEventProps(eventCaptor.getValue(), m_Program1);
        assertThat((String)eventCaptor.getValue().getProperty(Program.EVENT_PROP_PROGRAM_EXCEPTION), 
                is(nullValue()));
    }
    
    /**
     * Not the actual properties, just same way to verify we got the properties correctly
     */
    public Map<String, Object> getEventPropsForMock(ProgramImpl program)
    {
        //the properties
        final Map<String, Object> statusEventProps = new HashMap<String, Object>();
        statusEventProps.put("key", program);
        return statusEventProps;
    }
    
    public void assertProgramEventProps(Event event, ProgramImpl programImpl)
    {
        assertThat((ProgramImpl)event.getProperty("key"), is(programImpl));
    }
}
