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
package mil.dod.th.ose.controller.integration.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;

import java.io.IOException;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.ose.controller.integration.api.EventHandlerSyncer;
import mil.dod.th.ose.controller.integration.utils.AssetTestUtils;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import example.asset.ExampleAsset;

/**
 * Exercise the ability of the Mission Program Manager to execute, test and clean up mission programs. Many of the
 * Mission Program interactions happen asynchronously.
 * @author callen
 *
 */
public class TestMissionProgramManager
{
    private MissionProgramManager m_MissionProgramManager;
    private TemplateProgramManager m_TemplateProgramManager;
    
    @Before
    public void setUp() throws IllegalArgumentException, AssetException
    {
        m_MissionProgramManager = IntegrationTestRunner.getService(MissionProgramManager.class);
        assertThat(m_MissionProgramManager, is(notNullValue()));
        m_TemplateProgramManager = IntegrationTestRunner.getService(TemplateProgramManager.class);
        assertThat(m_TemplateProgramManager, is(notNullValue()));
    }
    
    @After
    public void tearDown() throws InterruptedException, IllegalArgumentException, 
        IllegalStateException, FactoryException
    {
        AssetTestUtils.deleteAllAssets();
    }
    
    /**
     * This tests that a simple script can execute, and utilize primitive arguments as execution parameters.
     * Re-tested {@link TestPersistedMissionPrograms#testSimpleScript()}.
     */
    @Test
    public void testSimpleScript() throws IllegalArgumentException, MissionProgramException, IOException,
        PersistenceFailedException, InterruptedException
    {
        //verify that the template was loaded
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("simple-template"));
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 5)).withProgramName("TestSimpleTest1");
        
        Program program = m_MissionProgramManager.loadParameters(params);
        EventHandlerSyncer syncer = new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleTest1"));
        
        program.execute();
        
        syncer.waitForEvent(10);
        //verify that the parameters were loaded successfully and created a valid Mission program that executed 
        //successfully
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
    }
    
    /**
     * This tests that a simple script can execute with a schedule.
     * Verify that the program executes.
     * Verified that execution restarts in {@link TestPersistedMissionPrograms#testSimpleScriptScheduled()}
     */
    @Test
    public void testSimpleScriptScheduleAtRestart() throws IllegalArgumentException, MissionProgramException, 
        IOException, PersistenceFailedException, InterruptedException
    {
        //verify that the template was loaded
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("simple-template"));
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule(true, false, true, 
            null, null, System.currentTimeMillis() + 900000L);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 99)).withProgramName("Test_Restart");

        EventHandlerSyncer executionSyncer = new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "Test_Restart"));
        
        m_MissionProgramManager.loadParameters(params);

        executionSyncer.waitForEvent(5);
    }
    
    /**
     * Tests that asset type dependencies can be looked up within the AssetDirectoryService and satisfied in a way
     * that the scripts that depend on the asset can execute correctly.
     * Re-tested {@link TestPersistedMissionPrograms#testAssetDirectoryService()}.
     */
    @Test
    public void testAssetDirectoryService() 
        throws IllegalArgumentException, MissionProgramException, AssetException, IOException, 
            PersistenceFailedException, FactoryException, InterruptedException
    {
        //syncer used to listen for executed event, mission's execute on a separate thread
        EventHandlerSyncer programExecutedSyncer = new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "ads-test"));

        //get directory service, and create/register an asset with the service
        AssetDirectoryService assetDirectoryService = IntegrationTestRunner.getService(AssetDirectoryService.class);
        assetDirectoryService.createAsset(ExampleAsset.class.getName(), "adsTestAsset");   

        //verify the template was loaded correctly
        assertThat(m_TemplateProgramManager.getMissionTemplateNames(), hasItem("asset-directory-service-template"));
        
        //parameters and schedule for the mission program    
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().
                withTemplateName("asset-directory-service-template").
                withProgramName("ads-test").withSchedule(schedule).
                withParameters(new MapEntry("asset", "adsTestAsset"));
        //load the parameters, this indirectly verifies that the script was fulfilled/valid, and executed successfully
        Program program = m_MissionProgramManager.loadParameters(params);
        
        //ask the program to execute.
        program.execute();
        //wait for execute event
        programExecutedSyncer.waitForEvent(5);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        //create a 'bad' program with an invalid asset
        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(false).withActive(true).withAtReset(false);
        params = new MissionProgramParameters().
                withTemplateName("asset-directory-service-template").
                withProgramName("ads-test-failure").
            withSchedule(schedule).withParameters(new MapEntry("assetName", "bad-name"));
        
        //the asset will not be found in the asset directory
        try
        {
            m_MissionProgramManager.loadParameters(params);
            fail("Expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            
        }
    }
    
    /**
     * Test that remote services (see mission program bindings) can work from within a script. 
     */
    @Test
    public void testRemoteService() throws Exception
    {
        //variable data
        MissionProgramSchedule schedule = new MissionProgramSchedule()
            .withIndefiniteInterval(false)
            .withImmediately(true)
            .withActive(true);
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("remote-services-template")
            .withSchedule(schedule)
            .withProgramName("remote-services-test");

        EventHandlerSyncer executionSyncer = new EventHandlerSyncer(
                new String[] { Program.TOPIC_PROGRAM_EXECUTED, Program.TOPIC_PROGRAM_EXECUTED_FAILURE }, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "remote-services-test"));
        
        //the test actually happens from within the script
        Program program = m_MissionProgramManager.loadParameters(params);
        
        executionSyncer.waitForEvent(10);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
    }
    
    /**
     * Test that a mission is correctly removed. See {@link TestPersistedMissionPrograms#testMissionRemoved()}.
     */
    @Test
    public void testRemoval() throws MissionProgramException, IOException, PersistenceFailedException, 
        InterruptedException
    {
        MissionProgramSchedule schedule = new MissionProgramSchedule().
                withStartInterval(System.currentTimeMillis() + 500).
                withStopInterval(System.currentTimeMillis() + 1000).
                withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 10)).withProgramName("Removal");
        EventHandlerSyncer syncer = new EventHandlerSyncer(Program.TOPIC_PROGRAM_SHUTDOWN);
        
        Program program = m_MissionProgramManager.loadParameters(params);

        syncer.waitForEvent(20);
        
        //remove the program
        program.remove();

        //verify the program is not found
        try
        {
            m_MissionProgramManager.getProgram("Removal");
            fail("Expecting exception as this program was just called to be removed.");
        }
        catch(IllegalArgumentException e)
        {
           //Expecting this exception as the program was just removed.
        }
    }
    
    /**
     * Test that mission programs that are inactive do not execute. 
     * Rechecked in {@link TestPersistedMissionPrograms#testInactiveProgram()}.
     */
    @Test
    public void testInactiveProgram() throws IllegalArgumentException, PersistenceFailedException, 
        MissionProgramException
    {
        //template and parameters
        MissionProgramTemplate progTemplate = new MissionProgramTemplate().
            withSource("obj = {};").withName("TestInactive");
        m_TemplateProgramManager.loadMissionTemplate(progTemplate);
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(true).withActive(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("TestInactive").
            withSchedule(schedule).withProgramName("TestInactive");
        Program program = m_MissionProgramManager.loadParameters(params);
        //a mission that has not attempted to execute should be unsatisfied
        assertThat(program.getProgramStatus(), is(ProgramStatus.UNSATISFIED));
        //Mission is not active this should execute
        try
        {
            program.execute();
            fail("Expected Exception");
        }
        catch (IllegalStateException exception)
        {
            //expected exception
        }
        assertThat(program.getProgramStatus(), is(not(ProgramStatus.EXECUTED)));
    }
}
