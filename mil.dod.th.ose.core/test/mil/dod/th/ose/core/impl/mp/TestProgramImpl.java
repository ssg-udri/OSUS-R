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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.security.PrivilegedActionException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.script.ScriptException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionScript;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.model.FlagEnum;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.mp.model.ScheduleEnum;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.ose.shared.MapTranslator;
import mil.dod.th.ose.test.EventAdminVerifier;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestProgramImpl
{
    private MissionProgramManagerImpl m_Manager;
    private EventAdmin m_EventAdmin;

    @Before
    public void setUp()
    {
        m_Manager = mock(MissionProgramManagerImpl.class);
        m_EventAdmin = mock(EventAdmin.class);
    }
    
    /**
     * Test getting the name of the template used to create this mission program.
     */
    @Test
    public void testGetTemplateName()
    {
        ProgramImpl program = createProgram("test");
        assertThat(program.getTemplateName(), is("test"));
    }

    /**
     * Test getting the program name.
     */
    @Test
    public void testGetProgramName()
    {
        ProgramImpl program = createProgram("NAME");
        assertThat(program.getProgramName(), is("NAME"));
    }

    /**
     * Test getting the template's vendor name.
     */
    @Test
    public void testGetVendor()
    {
        ProgramImpl program = createProgram("name");
        assertThat(program.getVendor(), is("hello"));
    }

    /**
     * Test getting the mission's source code from the template.
     */
    @Test
    public void testGetSource()
    {
        ProgramImpl program = createProgram("test");
        assertThat(program.getSource(), is("TheSourceIsHere"));
    }

    /**
     * Test removing the program. 
     * 
     * Verify Mission Program Manager is interacted with to remove the program from store of managed programs.
     */
    @Test
    public void testRemove()
    {
        ProgramImpl program = createProgram("test");
        program.remove();
        verify(m_Manager).removeProgram(eq(program), Mockito.any(UUID.class));
        
        //now try with the program in an illegal state
        program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        program.changeStatus(ProgramStatus.EXECUTING);
        
        try
        {
            program.remove();
            fail("Expecting exception because once a script is initialized the deps are bound to the mission. They"
                + "need to be released.");
        }
        catch (IllegalStateException e)
        {
            //expecting exception
        }
        //make sure the mission program manager is never called again.
        verify(m_Manager).removeProgram(eq(program), Mockito.any(UUID.class));
    }

    /**
     * Test that the program can be manually executed.
     * 
     * Verify that if script initialization does not take place that the program will not execute.
     */
    @Test
    public void testExecute() throws MissionProgramException, InterruptedException, ExecutionException
    {
        ProgramImpl program = createProgram("test");
        ArgumentCaptor<ProgramImpl> arguments = ArgumentCaptor.forClass(ProgramImpl.class);

        //change the status and try again
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        
        program.execute();
        verify(m_Manager).executeProgram(arguments.capture());
        assertThat(arguments.getValue().getExecParams().size(), is(0));
    }

    /**
     * Test that the program can be manually test executed.
     * 
     * Verify that if script initialization does not take place that the program will not execute.
     */
    @Test
    public void testTestExecuteScheduled() throws MissionProgramException, InterruptedException, ExecutionException
    {
        ProgramImpl program = createProgram("test");
        ArgumentCaptor<ProgramImpl> arguments = ArgumentCaptor.forClass(ProgramImpl.class);

        //change the status and try again
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        program.changeStatus(ProgramStatus.SCHEDULED);
        
        @SuppressWarnings("unchecked")
        Future<TestResult> future = mock(Future.class);
        when(future.get()).thenReturn(TestResult.PASSED);
        
        doReturn(future).when(m_Manager).testProgram(program);
        
        program.executeTest();
        verify(m_Manager).testProgram(arguments.capture());
        assertThat(arguments.getValue().getExecParams().size(), is(0));
    }
    
    /**
     * Tests that execution parameter setting does not allow illegal parameter changes.
     */
    @Test
    public void testExecSetting()
    {
        // Set up program with example parameters
        Map<String, Object> arguments = new HashMap<String, Object>();
        arguments.put("test", 8);
        UUID progUuid = UUID.randomUUID();
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, new MissionProgramTemplate().
            withSource("something").withName("test"), new MissionProgramParameters().withParameters(MapTranslator.
                translateMap(arguments)).withSchedule(new MissionProgramSchedule().withIndefiniteInterval(false).
                    withImmediately(true)), progUuid); 
        assertThat(program.getScheduleFlag(ScheduleEnum.START_IMMEDIATELY), is(true));
        assertThat(program.getExecParams(), hasEntry("test", (Object)8));
        
        try
        {
            program.getExecParams().put("blah", 56);
            fail("expecting exception");
        }
        catch (UnsupportedOperationException e)
        {
            // Check that new parameters cannot be added
        }
        
        //try setting the immediate flag to false
        program.getMissionSchedule().setImmediately(false);
        //the value should still be true, because this field cannot be set through the program interface
        assertThat(program.getScheduleFlag(ScheduleEnum.START_IMMEDIATELY), is(true));
    }

    /**
     * Test getting factory object dependencies.
     * 
     * Verify that the mission program manager is called to try to the reference of the dependecy based on the type.
     */
    @Test
    public void testGetFactoryObjectDeps()
    {
        //create a program with no dependencies
        ProgramImpl program = createProgram("test");
        //should not be null because they are initialized at construction of the program
        assertThat(program.getFactoryObjectDeps(Asset.class), is(notNullValue()));
        assertThat(program.getFactoryObjectDeps(PhysicalLink.class), is(notNullValue()));
        assertThat(program.getFactoryObjectDeps(LinkLayer.class), is(notNullValue()));
        assertThat(program.getFactoryObjectDeps(TransportLayer.class), is(notNullValue()));
        
        try
        {
            program.getFactoryObjectDeps(FactoryObject.class);
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
        //verify that you cannot add to the list externally        
        try
        {
            program.getFactoryObjectDeps(PhysicalLink.class).add("link 1");
            fail("expecting exception");
        }
        catch (UnsupportedOperationException e)
        {
            
        }        
    }
    
    /**
     * Tests that program dependencies are correctly set and can be retrieved from a created program.
     * Dependencies can be of different FactoryObject types.
     */
    @Test
    public void testSetExecutionDependencies()
    {
        //set up variable data
        MissionVariableMetaData programVars = new MissionVariableMetaData().withDescription("made up program deps").
            withName("assetX").withType(MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData programVars2 = new MissionVariableMetaData().withDescription("made up program deps").
            withName("assetY").withType(MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData programVars3 = new MissionVariableMetaData().withDescription("made up program deps").
            withName("assetZ").withType(MissionVariableTypesEnum.ASSET);
        MissionVariableMetaData programVars4 = new MissionVariableMetaData().withDescription("made up program deps").
                withName("pl1").withType(MissionVariableTypesEnum.PHYSICAL_LINK);
        MissionVariableMetaData programVars5 = new MissionVariableMetaData().withDescription("made up program deps").
                withName("plZ").withType(MissionVariableTypesEnum.PHYSICAL_LINK);
        //the program's parameters
        List<MapEntry> params = new ArrayList<MapEntry>();
        params.add(new MapEntry("assetX", "assetX"));
        params.add(new MapEntry("assetY", "assetY"));
        params.add(new MapEntry("assetZ", "assetZ"));
        params.add(new MapEntry("plZ", "plZ"));
        params.add(new MapEntry("pl1", "pl1"));
        //create the program
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, new MissionProgramTemplate().
            withSource("something").withName("test").withVariableMetaData(programVars, programVars2, programVars3, 
                programVars4, programVars5),  new MissionProgramParameters().withSchedule(new MissionProgramSchedule().
                    withIndefiniteInterval(false)).withParameters(params), UUID.randomUUID());
        assertThat(program.getTemplateName(), is("test"));
        assertThat(program.getSource(), is("something"));
        //check dependency lists
        assertThat(program.getFactoryObjectDeps(Asset.class), hasItem("assetX"));
        assertThat(program.getFactoryObjectDeps(Asset.class), hasItem("assetY"));
        assertThat(program.getFactoryObjectDeps(Asset.class), hasItem("assetZ"));
        assertThat(program.getFactoryObjectDeps(PhysicalLink.class), hasItem("pl1"));
        assertThat(program.getFactoryObjectDeps(PhysicalLink.class), hasItem("plZ"));
    }
    
    /**
     *  Checks that program defined program dependencies can be correctly retrieved.
     */
    @Test
    public void testGetProgramDeps()
    {
        //Creating variable data, schedule and template
        MissionVariableMetaData programVars = new MissionVariableMetaData().withDescription("made up program deps")
                .withName("prog1").
            withOptionValues("prog2", "prog3").withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionProgramTemplate progInstance = new MissionProgramTemplate().withSource("dragon").withName("George").
            withVariableMetaData(programVars);
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false);
        List<MapEntry> paramVals = new ArrayList<MapEntry>();
        paramVals.add(new MapEntry("prog1", "prog2"));
        //the program's parameters
        MissionProgramParameters params = new MissionProgramParameters().withSchedule(schedule).
            withParameters(paramVals).withProgramName("NAME");
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, progInstance, params,  UUID.randomUUID());
        assertThat(program.getProgramDeps(), hasItem("prog2"));
        //verify that optional values were not placed as dependencies, as they should only be used when actual value is
        //not assigned
        assertThat(program.getProgramDeps(), not(hasItem("prog3")));
        List<MissionVariableMetaData> newVarData = program.getVariableMetaData();
        //there should only be one in the list
        assertThat(newVarData.get(0).getOptionValues(), hasItem("prog2"));
        assertThat(newVarData.get(0).getOptionValues(), hasItem("prog3"));
        assertThat(program.getProgramDeps(), is(notNullValue()));        
        try
        {
            program.getProgramDeps().add("other program");
            fail("expecting exception");
        }
        catch (UnsupportedOperationException e)
        {
            //expected exception    
        }
    }
    
    /**
     * This tests that the program dependencies are extracted from the mission program and placed into the program 
     * deps.
     * Verify that the mission's status is update to waiting, but not initialized.
     */
    @Test
    public void testProgramDeps()
    {
        UUID progUuid = UUID.randomUUID();
        //mocks for MissionProgramManager behavior
        Program mockProg = mock(Program.class);
        when(m_Manager.getProgramDep(Mockito.anyString())).thenReturn(mockProg);
        //program variables, schedule, and template
        MissionVariableMetaData programVars = new MissionVariableMetaData().withDescription("made up program deps")
                .withName("prog1").
            withOptionValues("prog2", "prog3").withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionVariableMetaData programVars2 = new MissionVariableMetaData().withDescription("made up program deps").
            withName("prog2").withOptionValues("prog1", "prog3").withType(
                        MissionVariableTypesEnum.PROGRAM_DEPENDENCIES);
        MissionProgramTemplate progInstance = new MissionProgramTemplate().withSource("dragon").withName("George").
            withVariableMetaData(programVars, programVars2);
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false);
        //program parameters
        List<MapEntry> params = new ArrayList<MapEntry>();
        params.add(new MapEntry("prog1", "prog3"));
        params.add(new MapEntry("prog2", "prog3"));
        //create new program
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, progInstance, new MissionProgramParameters().
            withParameters(params).withSchedule(schedule).withProgramName("NAME"), progUuid);
        //both variables have the same dependency
        assertThat(program.getProgramDeps(), hasItem("prog3"));
        List<MissionVariableMetaData> newVarData = program.getVariableMetaData();
        //there should only be two in the list
        assertThat(newVarData.get(0).getOptionValues(), hasItem("prog2"));
        assertThat(newVarData.get(0).getOptionValues(), hasItem("prog3"));
        assertThat(newVarData.get(1).getOptionValues(), hasItem("prog1"));
        assertThat(newVarData.get(1).getOptionValues(), hasItem("prog3"));
        
        //should be unsatisfied as the program has only been created, and the execution params have not been changed
        //to values for their give type
        assertThat(program.getProgramStatus(), is(ProgramStatus.UNSATISFIED));
        //attempt to reconcile the deps to values
        program.reconcileDependencies();

        //without the script engine evaluating the program's script in the mission program manager, 
        //this is the waiting status
        assertThat(program.getProgramStatus(), is(ProgramStatus.WAITING_UNINITIALIZED));
    }
    
    /**
     * This tests the change status method in the program implementation. Certain status changes are not allowed
     * and will throw errors. 
     */
    @Test
    public void testProgramStatus() throws MissionProgramException, InterruptedException, ExecutionException
    {
        ProgramImpl program = createProgram("NAME");
        
        //at construction is not checked that deps are unsatisfied
        assertThat(program.getProgramStatus(), is(ProgramStatus.UNSATISFIED));

        //check if deps need to be reconciled if not the program is satisfied
        program.reconcileDependencies();
        assertThat(program.getProgramStatus(), is(ProgramStatus.WAITING_UNINITIALIZED));

        //this would take place in the mission program manager's script initialization
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        
        program.execute();

        //verify the call to the MissionProgramManager
        verify(m_Manager).executeProgram(program);
        //within the manager the following calls are made
        program.changeStatus(ProgramStatus.EXECUTING);
        //check that the flag has changed
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTING));
        //after successful completion
        program.changeStatus(ProgramStatus.EXECUTED);
        //check that the flag has changed
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        // the executed status is an end-point, after reaching this end-point the status cannot be changed 
        //back to waiting
        try
        {
            program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
            fail("expected exception");
        }
        catch (IllegalStateException exception)
        {
            //expected exception
        }
        //verify still executed status
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        //programs can be re-executed after execution, but parameter values are not re-check, so status changes to
        // executing from executed. 
        program.changeStatus(ProgramStatus.EXECUTING);
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTING));
        
        try
        {
            //programs with bad variable metatdata or parameter values will throw an exception during construction
            new ProgramImpl(m_Manager, m_EventAdmin, new MissionProgramTemplate().
                withSource("something").withName("test").withVariableMetaData(new MissionVariableMetaData().
                    withName("badness")), new MissionProgramParameters().withSchedule(new MissionProgramSchedule().
                    withIndefiniteInterval(false).withActive(true)), UUID.randomUUID());
            fail("expected exception because the variable data is not complete.");
        }
        catch(IllegalArgumentException e)
        {
            //expected exception
        }
    }

    /**
     * Test that if the mission is in a initialization error state that the program cannot execute.
     */
    @Test
    public void testProgramStatusIllegalState() throws MissionProgramException
    {
        ProgramImpl program = createProgram("test");

        //this would take place in the mission program manager's script initialization process if initialization fails
        program.changeStatus(ProgramStatus.INITIALIZATION_ERROR);

        try
        {
            program.execute();
            fail("Expecting exception because the script failed initialization.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }

        //verify the call to the MissionProgramManager
        verify(m_Manager, never()).executeProgram(program);

        //check that the flag has NOT changed
        assertThat(program.getProgramStatus(), is(ProgramStatus.INITIALIZATION_ERROR));
    }
    
    /**
     * Test that if the mission is in a waiting initialized state that an error is thrown if the 
     * status tries to change to unsatisfied.
     */
    @Test
    public void testProgramStatusChangeIllegalState() throws MissionProgramException
    {
        ProgramImpl program = createProgram("name");
        
        //this would take place in the mission program manager's script initialization process if initialization fails
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);

        try
        {
            program.changeStatus(ProgramStatus.UNSATISFIED);
            fail("Expecting exception because the script should not go from initialized to unsatisfied.");
        }
        catch (IllegalStateException e)
        {
            //expected exception, because the script was satisfied to get initialized, going back to unsatisfied
            //would mean that the system lost on the dependencies.
        }
    }
    
    /**
     * Tests that if a mission has unsatisfied dependencies the status change throws an error if
     * execution is attempted.
     */
    @Test
    public void testProgramStatusChangeUnsatisfied() throws MissionProgramException
    {
        ProgramImpl program = createProgram("name");

        //this is the status of a program when dependencies have not yet been fulfilled
        program.changeStatus(ProgramStatus.UNSATISFIED);
        
        try
        {
            program.changeStatus(ProgramStatus.EXECUTING);
            fail("Expecting exception because the script should not go from unsatisfied to executing.");
        }
        catch (IllegalStateException e)
        {
            //expected exception, because the script had unsatisfied dependencies it should not execute.
        }      
    }
    
    /**
     * Tests that a mission status cannot be changed from executing to shutting down.
     * Verify exception.
     */
    @Test
    public void testProgramStatusChangeIllegalShuttingDown() throws MissionProgramException
    {
        ProgramImpl program = createProgram("name");

        //work through legal state changes
        program.changeStatus(ProgramStatus.UNSATISFIED);
        program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        program.changeStatus(ProgramStatus.EXECUTING);
        
        try
        {
            program.changeStatus(ProgramStatus.SHUTTING_DOWN);
            fail("Expecting exception because the script should not go from executing to shutting down.");
        }
        catch (IllegalStateException e)
        {
            //expected exception, because the script had unsatisfied dependencies it should not execute.
        }
    }
    
    /**
     * Tests that a mission must be executed in order for the status to change to shutting down.
     */
    @Test
    public void testProgramStatusChangeShuttingDown() throws MissionProgramException
    {
        ProgramImpl program = createProgram("name");

        //work through legal state changes
        program.changeStatus(ProgramStatus.UNSATISFIED);
        program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        program.changeStatus(ProgramStatus.EXECUTING);
        program.changeStatus(ProgramStatus.EXECUTED);
        
        //now shutdown
        program.changeStatus(ProgramStatus.SHUTTING_DOWN);
    }
    
    /**
     * Verifies that an event is posted when the program status is changed.
     */
    @Test
    public void testProgramStatusChangeEventPosted()
    {
        ProgramImpl program = createProgram("statusEventProgram");
        
        // Test that event is posted correctly if status is changed.
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());
 
        Event event = EventAdminVerifier.assertEventByTopicOnly(eventCaptor, Program.TOPIC_PROGRAM_STATUS_CHANGED);

        assertThat((String)event.getProperty(Program.EVENT_PROP_PROGRAM_NAME), is("statusEventProgram"));
        assertThat((String)event.getProperty(Program.EVENT_PROP_PROGRAM_STATUS), 
            is(ProgramStatus.WAITING_INITIALIZED.toString()));
        
        //test that an attempted change that causes an illegal state does not post a status changed event
        //because the status is not changed in this case.
        try
        {
            program.changeStatus(ProgramStatus.UNSATISFIED);
            fail("Expecting exception because the script should not go from initialized to unsatisfied.");
        }
        catch (IllegalStateException e)
        {
            //expected exception
        }
        
        // Only one event should have been captured from previous change to WAITING_INITIALIZED.
        // Verify that no event was posted for attempted but failed change to UNSATISFIED
        EventAdminVerifier.assertEventByTopicOnly(eventCaptor, Program.TOPIC_PROGRAM_STATUS_CHANGED, 1);
    }

    /**
     * Tests that metadata can be set and retrieved for a program.
     */
    @Test
    public void testGetVariableMetaData()
    {
        //set up mission program
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false);
        MissionProgramTemplate program = new MissionProgramTemplate();
        program.setName("TestMission");
        program.setDescription("A test mission that tests the usage of metadata");
        program.setSource("TheSourceIsHere");
        program.withVariableMetaData(new MissionVariableMetaData().withName("Var"). 
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES).withDescription("Really awesome variable").
            withDefaultValue("Steve").withOptionValues("Super", "Bat", "Yeti").
            withHumanReadableName("Nilesh"));
        ProgramImpl programTest =  new ProgramImpl(m_Manager, m_EventAdmin, program, new MissionProgramParameters().
            withSchedule(schedule), UUID.randomUUID());
        
        //check that the new metadata was added
        assertThat(programTest.getVariableMetaData().size(), is(1));
        assertThat(programTest.getDescription(), is("A test mission that tests the usage of metadata"));
        assertThat(programTest.getVariableMetaData().iterator().next().getName(), is("Var"));
        assertThat(programTest.getVariableMetaData().iterator().next().getDefaultValue(), is("Steve"));
        assertThat(programTest.getVariableMetaData().iterator().next().getDescription(), is("Really awesome variable"));
        assertThat(programTest.getVariableMetaData().iterator().next().getType(), 
            is(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES));
        assertThat(programTest.getVariableMetaData().iterator().next().getOptionValues(), hasItem("Bat"));
        assertThat(programTest.getVariableMetaData().iterator().next().getHumanReadableName(), is("Nilesh"));
    }
    
    /*
     * Tests that although a list is returned when the method getVariableMetadata is called, that any changes to the
     * returned list do not actually change the values within the template. 
     */
    @Test
    public void testGetVariableMetaDataWithReset()
    {
        //set up mission program
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false);
        MissionProgramTemplate program = new MissionProgramTemplate();
        program.setName("TestMission");
        program.setDescription("A test mission that tests the usage of metadata");
        program.setSource("TheSourceIsHere");
        program.withVariableMetaData(new MissionVariableMetaData().withName("Var"). 
            withType(MissionVariableTypesEnum.PROGRAM_DEPENDENCIES).withDescription("Really awesome variable").
            withDefaultValue("Steve").withOptionValues("Super", "Bat", "Yeti"));
        program.withVariableMetaData(new MissionVariableMetaData().withName("Var"). 
            withType(MissionVariableTypesEnum.ASSET).withDescription("Really awesome variable"));
        ProgramImpl programTest =  new ProgramImpl(m_Manager, m_EventAdmin, program, new MissionProgramParameters().
            withSchedule(schedule), UUID.randomUUID());

        //get all metadata set for the program
        List<MissionVariableMetaData> lister = programTest.getVariableMetaData();
        //reset name to barbie
        lister.get(0).setName("barbie");
        try
        {
            //Unable to add new items to the metadata list
            lister.add(new MissionVariableMetaData().withName("@").withType(MissionVariableTypesEnum.STRING).
                withDescription("@ is really just a character!"));
            fail("Expected Excetpion");
        }
        catch (UnsupportedOperationException e)
        {

        }
        //verify that this setting of the name had no effect on the actual mission program data.
        List<MissionVariableMetaData> lister2 = programTest.getVariableMetaData();
        assertThat(lister2.get(0).getName(), is("Var"));
    }
    
    /*
     * Test getting the flag values
     */
    @Test
    public void testGetFlags()
    {
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, new MissionProgramTemplate().
            withSource("Fish").withName("Snake").withWithInterval(true).withWithPanTilt(true).
            withWithImageCapture(true).withWithSensorTrigger(false), new MissionProgramParameters().
            withSchedule(new MissionProgramSchedule().withIndefiniteInterval(false).withActive(true).
            withImmediately(true)), UUID.randomUUID());
        
        //get the value of each flag.
        assertThat(program.getFlag(FlagEnum.WITH_IMAGE_CAPTURE), is(true));
        assertThat(program.getFlag(FlagEnum.WITH_TIMER_TRIGGER), is(false));
        assertThat(program.getFlag(FlagEnum.WITH_SENSOR_TRIGGER), is(false));
        assertThat(program.getFlag(FlagEnum.WITH_INTERVAL), is(true));
        assertThat(program.getFlag(FlagEnum.WITH_PAN_TILT), is(true));
        
        assertThat(program.getScheduleFlag(ScheduleEnum.START_AT_RESTART), is(false));
        assertThat(program.getScheduleFlag(ScheduleEnum.START_IMMEDIATELY), is(true));
        assertThat(program.getScheduleFlag(ScheduleEnum.WITH_INDEFINITE_INTERVAL), is(false));
        assertThat(program.getScheduleFlag(ScheduleEnum.IS_ACTIVE), is(true));
    }
    
    /**
     * Test getting the flag values when the there is start/stop times.
     */
    @Test
    public void testGetFlagsStartStop()
    {
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, new MissionProgramTemplate().
            withSource("Fish").withName("Snake").withWithInterval(true).withWithImageCapture(true).
            withWithSensorTrigger(false), new MissionProgramParameters().withSchedule(new MissionProgramSchedule().
                withStartInterval(4L).withActive(true).withStopInterval(7L)), UUID.randomUUID());
        
        //get the value of each flag.
        assertThat(program.getFlag(FlagEnum.WITH_IMAGE_CAPTURE), is(true));
        assertThat(program.getFlag(FlagEnum.WITH_TIMER_TRIGGER), is(false));
        assertThat(program.getFlag(FlagEnum.WITH_SENSOR_TRIGGER), is(false));
        assertThat(program.getFlag(FlagEnum.WITH_INTERVAL), is(true));
        assertThat(program.getScheduleFlag(ScheduleEnum.START_AT_RESTART), is(false));
        assertThat(program.getScheduleFlag(ScheduleEnum.START_IMMEDIATELY), is(false));
        assertThat(program.getScheduleFlag(ScheduleEnum.WITH_INDEFINITE_INTERVAL), is(false));
        assertThat(program.getScheduleFlag(ScheduleEnum.IS_ACTIVE), is(true));
    }
    
    /**
     * Verify that secondary images can be retrieved from the program with all information intact. Also verify that the
     * list of secondary images cannot be modified.
     */
    @Test
    public void testGetSecondaryImages()
    {
        DigitalMedia media1 = new DigitalMedia().withEncoding("encoding");
        DigitalMedia media2 = new DigitalMedia().withEncoding("alsoEncoding");
        List<DigitalMedia> setOfImages = new ArrayList<DigitalMedia>();
        setOfImages.add(media1);
        setOfImages.add(media2);

        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, new MissionProgramTemplate().
            withSource("Fish").withName("Snake").withSecondaryImages(setOfImages), 
            new MissionProgramParameters().withSchedule(new MissionProgramSchedule().withIndefiniteInterval(false)), 
            UUID.randomUUID());

        //verify the set is 2
        assertThat(program.getSecondaryImages().size(), is(2));
        try
        {
            //attempt to add a new item, expect failure as the list is unmodifiable
            program.getSecondaryImages().add(new DigitalMedia());
            fail("Expected Exception");
        }
        catch (UnsupportedOperationException e)
        {

        }
        //verify that the list of images is still 2
        assertThat(program.getSecondaryImages().size(), is(2));

        List<DigitalMedia> images = program.getSecondaryImages();
        assertThat(images.get(0).getEncoding(), is("encoding"));
        assertThat(images.get(1).getEncoding(), is("alsoEncoding"));
        images.get(0).setEncoding("badEncoding");
        
        //verify that the encoding was NOT changed.
        assertThat(program.getSecondaryImages().size(), is (2));
        assertThat(program.getSecondaryImages().get(0).getEncoding(), is("encoding"));
        assertThat(program.getSecondaryImages().get(1).getEncoding(), is("alsoEncoding"));
    }
    
    @Test
    public void testGetPrimaryImage()
    {
        ProgramImpl program = createProgram("testMission");
        assertThat(program.getPrimaryImage().getEncoding(), is("Hieroglyphics"));
        
        //try to change the encoding value
        program.getPrimaryImage().setEncoding("jpeg");
        //check that the actual image encoding was not changed.
        assertThat(program.getPrimaryImage().getEncoding(), is("Hieroglyphics"));
    }

    /**
     * Test the setting of a mission script object within a program.
     */
    @Test
    public void testSetMissionScript()
    {
        //mock mission script object
        MissionScript mission = mock(MissionScript.class);

        //create program
        ProgramImpl program = createProgram("mission");

        //set the mission script object
        program.setMissionScript(mission);

        //verify
        assertThat(program.getMissionScript(), is(mission));
    }
    
    /**
     * Test the setting of a mission script to null causing an error.
     */
    @Test
    public void testSetNullMissionScript()
    {
        //create program
        ProgramImpl program = createProgram("mission");

        //set the mission script object
        try
        {
            program.setMissionScript(null);
            fail("Expecting exception since argument is null");
        }
        catch (NullPointerException e)
        {
            
        }
    }

    /**
     * Test that the program can be test executed.
     * 
     * Verify that if script initialization does not take place that the program will not execute.
     */
    @Test
    public void testTestExecute() throws MissionProgramException, InterruptedException, ExecutionException
    {
        ProgramImpl program = createProgram("test");
        ArgumentCaptor<ProgramImpl> arguments = ArgumentCaptor.forClass(ProgramImpl.class);

        try
        {
            program.executeTest();
            fail("Expecting exception because the programs status needs to be waiting and initialized for"
                + " execution to take place");
        }
        catch (IllegalStateException ex)
        {
            //expected failure
        }

        //verify that the mission script's status is waiting, but uninitialized
        assertThat(program.getProgramStatus(), is(ProgramStatus.WAITING_UNINITIALIZED));

        //change the status and try again
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        
        @SuppressWarnings("unchecked")
        Future<TestResult> result = mock(Future.class);
        when(result.get()).thenReturn(TestResult.PASSED);

        //return value from mission program manager
        when(m_Manager.testProgram(program)).thenReturn(result);
        
        Future<TestResult> endResult = program.executeTest();
        verify(m_Manager).testProgram(arguments.capture());
        assertThat(arguments.getValue().getExecParams().size(), is(0));
        
        //verify result
        assertThat(endResult.get(), is(TestResult.PASSED));
    }

    /**
     * Test that test executed exceptions are properly handled.
     * 
     * Verify Mission program exception.
     */
    @Test
    public void testTestExecuteException() throws MissionProgramException, InterruptedException, ExecutionException
    {
        ProgramImpl program = createProgram("test");

        program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        
        @SuppressWarnings("unchecked")
        Future<TestResult> result = mock(Future.class);
        doThrow(new ExecutionException(new ScriptException("Wheeew I am exhausted!"))).
            when(result).get();

        //return value from mission program manager
        when(m_Manager.testProgram(program)).thenReturn(result);
        Future<TestResult> endResult = program.executeTest();
        
        try
        {
            endResult.get();
            fail("Expected exception");
        }
        catch (ExecutionException e)
        {
            //expected exception
        }
    }
    
    /**
     * Test that the program will not attempt to be test executed if the operation is not signified as supported within
     * the template XSD.
     * 
     * Verify unsupported operation is thrown.
     */
    @Test
    public void testTestExecuteUnsupportedOperation() throws MissionProgramException
    {
        //mission information
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).withActive(true);
        MissionProgramTemplate programMission = new MissionProgramTemplate().withVendor("hello").
            withSupportTestExecution(false);
        programMission.setName("test");
        programMission.setSource("TheSourceIsHere");
        programMission.setPrimaryImage(new DigitalMedia().withEncoding("Hieroglyphics"));
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, programMission, new MissionProgramParameters().
             withSchedule(schedule).withProgramName("test"), UUID.randomUUID());

        //try to execute the test function
        try
        {
            program.executeTest();
            fail("Expecting exception because the programs does not support a test");
        }
        catch (UnsupportedOperationException ex)
        {
            //expected failure
        }
    }

    /**
     * Test that the program requested to test execute non blocking will not throw an exception as expected.
     * In these scenarios one must listen for the event posted elsewhere that test execution was unsuccessful.
     * 
     * Verify null is returned.
     * 
     */
    @Test
    public void testTestExecuteWithExceptionNonBlock() throws MissionProgramException, InterruptedException, 
           ExecutionException
    {
        //mission information
        ProgramImpl program = createProgram("test");

        program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);

        Future<?> result = mock(Future.class);
        when(result.get()).thenThrow(new ExecutionException(
                new PrivilegedActionException(
                    new ScriptException("Wheeew I am exhausted!")).getCause()));

        //return value from mission program manager
        doReturn(result).when(m_Manager).testProgram(program);
        
        //try to execute
        Future<?> execFuture = program.executeTest();
        
        //expected and documented behavior
        assertThat(execFuture, is(notNullValue()));
    }
    
    /**
     * Test setting a mission's test results.
     */
    @Test
    public void testSetTestResult()
    {
        //program
        ProgramImpl program = createProgram("baaa");
        
        assertThat(program.getLastTestResult(), is(nullValue()));
        
        //set the result
        program.setTestResults(TestResult.PASSED);
        
        //verify the value was set
        assertThat(program.getLastTestResult(), is(TestResult.PASSED));
    }
    
    /**
     * Test that the program can be called to cancel scheduled execution.
     * 
     */
    @Test
    public void testCancelTrue() throws MissionProgramException, InterruptedException, 
           ExecutionException
    {
        //mission information
        ProgramImpl program = createProgram("test");

        program.changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
        program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        program.changeStatus(ProgramStatus.SCHEDULED);

        when(m_Manager.cancelProgram("test")).thenReturn(true);
        
        //request
        boolean bool = program.cancel();
        
        assertThat(bool, is(true));
    }
    
    /**
     * Test getting a program' execution parameters returns a manageable version of those parameters.
     */
    @Test
    public void testGetExecutionParams()
    {
        //program
        MissionProgramSchedule schedule = createMissionSchedule(0L, 0L, false, true, true, false);
        MissionProgramTemplate programMission = new MissionProgramTemplate().withVendor("hello").
            withSupportTestExecution(true);
        programMission.setName("jaja");
        programMission.setSource("TheSourceIsHere");
        programMission.setPrimaryImage(new DigitalMedia().withEncoding("Hieroglyphics"));
        Program program = new ProgramImpl(m_Manager, m_EventAdmin, programMission, new MissionProgramParameters().
             withParameters(new MapEntry().withKey("asset").withValue("asset")).
             withSchedule(schedule).
             withProgramName("jaja"), UUID.randomUUID());
        
        assertThat(program.getExecutionParameters(), hasEntry("asset", (Object)"asset"));
    }
    
    /**
     * Test getting a program' execution parameters returns a manageable version of those parameters.
     * Verify the returned parameters are NOT the exec parameters, which are translated to the actual
     * target objects in a map<String, Object>.
     */
    @Test
    public void testGetExecutionParamsNotExecParams() throws IllegalStateException, MissionProgramException
    {
        //program
        MissionProgramSchedule schedule = createMissionSchedule(0L, 0L, false, true, true, false);
        MissionProgramTemplate programMission = new MissionProgramTemplate().withVendor("hello").
            withSupportTestExecution(true).withVariableMetaData(new MissionVariableMetaData().withName("asset"). 
                    withType(MissionVariableTypesEnum.ASSET).withDescription("Really awesome variable"));
        programMission.setName("jaja");
        programMission.setSource("TheSourceIsHere");
        programMission.setPrimaryImage(new DigitalMedia().withEncoding("Hieroglyphics"));
        Asset asset = mock(Asset.class);
        when(m_Manager.getAssetDep("asset")).thenReturn(asset);
        ProgramImpl program = new ProgramImpl(m_Manager, m_EventAdmin, programMission, new MissionProgramParameters().
             withParameters(new MapEntry("asset", "asset")).
             withSchedule(schedule).
             withProgramName("jaja"), UUID.randomUUID());
        
        //reconcile the dep, this will change the value in the exec params to hold the asset value and not the asset's
        //name
        program.reconcileDependencies();
        
        assertThat(program.getExecutionParameters(), hasEntry("asset", (Object)"asset"));
        assertThat(program.getExecutionParameters(), is(not(program.getExecParams())));
    }
    
    /**
     * Helper method that creates a mission with the given name.
     * @return 
     *     the program impl object
     */
    private ProgramImpl createProgram(final String name)
    {
        MissionProgramSchedule schedule = createMissionSchedule(0L, 0L, false, true, true, false);
        MissionProgramTemplate programMission = new MissionProgramTemplate().withVendor("hello").
            withSupportTestExecution(true);
        programMission.setName(name);
        programMission.setSource("TheSourceIsHere");
        programMission.setPrimaryImage(new DigitalMedia().withEncoding("Hieroglyphics"));
        return new ProgramImpl(m_Manager, m_EventAdmin, programMission, new MissionProgramParameters().
             withSchedule(schedule).withProgramName(name), UUID.randomUUID());
    }
    
    /**
     * Create a mission program schedule.
     */
    public static MissionProgramSchedule createMissionSchedule(final Long startInterval, final Long stopInterval, 
        final Boolean indefiniteInterval, final Boolean immediately, final boolean active, final boolean atReset)
    {
        return new MissionProgramSchedule(
            active, atReset, immediately, startInterval, indefiniteInterval, stopInterval);
    }
}
