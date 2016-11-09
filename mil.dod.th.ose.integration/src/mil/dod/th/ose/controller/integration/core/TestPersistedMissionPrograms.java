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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.physical.PhysicalLinkException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.ose.controller.integration.api.EventHandlerSyncer;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.service.log.LogService;

/**
 * Test that missions previously created will function at the reset of the system. 
 * See {@link TestMissionProgramManager}.
 * 
 * @author dhumeniuk
 *
 */
public class TestPersistedMissionPrograms
{
    private MissionProgramManager m_MissionProgramManager;

    @Before
    public void setUp()
    {
        m_MissionProgramManager = IntegrationTestRunner.getService(MissionProgramManager.class);
        assertThat(m_MissionProgramManager, is(notNullValue()));
    }
    
    /**
     * See {@link TestMissionProgramManager#testSimpleScript()}. 
     */
    @Test
    public void testSimpleScript() throws IllegalArgumentException, MissionProgramException
    {
        Program program = m_MissionProgramManager.getProgram("TestSimpleTest1");
        assertThat(program, is(notNullValue()));
        program.execute();
        assertThat(program.getVariableMetaData().get(0).getName(), is("a"));
    }
    
    /**
     * See {@link TestMissionProgramManager#testSimpleScriptScheduleAtRestart()}. 
     */
    @Test
    public void testSimpleScriptScheduled() throws IllegalArgumentException, MissionProgramException
    {
        Program program = m_MissionProgramManager.getProgram("Test_Restart");
        assertThat(program, is(notNullValue()));
        assertThat(program.getProgramStatus(), is(not(ProgramStatus.SHUTDOWN)));
    }
    
    /**
     * Test the asset directory program made in {@link TestMissionProgramManager#testAssetDirectoryService()}
     * function properly after restart.
     * 
     * Asset is created by {@link 
     * mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#createAssetForMissionProgram()}.
     */
    @Test
    public void testAssetDirectoryService() throws IllegalArgumentException, MissionProgramException, AssetException, 
        InterruptedException, FactoryException, PersistenceFailedException
    {
        Program program = m_MissionProgramManager.getProgram("ads-test");
        assertThat(program, is(notNullValue()));

        // wait for program to execute
        EventHandlerSyncer syncer = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=ads-test)");
        
        // execute the program, not scheduled to restart at startup
        program.execute();
        
        //wait for execution
        syncer.waitForEvent(10);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
    }
    
    /**
     * Tests that the asset dependent program made in {@link 
     * mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#createAutoExecPrograms()} recovers and executes. This may
     * be happening as dependencies are still coming up so wait.
     */
    @Test
    public void testAssetDepAutoExec() 
        throws IOException, InterruptedException, IllegalArgumentException, AssetException, BundleException, 
            FactoryException, MissionProgramException, PersistenceFailedException
    {        
        // wait for program to execute
        EventHandlerSyncer syncer = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=auto-exec-asset-dep-test)");
        
        Program program = m_MissionProgramManager.getProgram("auto-exec-asset-dep-test");
        assertThat(program, is(notNullValue()));
        
        if (program.getProgramStatus().equals(ProgramStatus.UNSATISFIED))
        {
            Logging.log(LogService.LOG_INFO, "auto-exec-asset-dep-test not complete yet, waiting for it");
            syncer.waitForEvent(10);
            Logging.log(LogService.LOG_INFO, "auto-exec-asset-dep-test complete event received");
        }
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        PersistentDataStore persistentDataStore = IntegrationTestRunner.getService(PersistentDataStore.class);
        
        // each time the program runs, an entry is added, this is checking how many times the program ran
        Collection<? extends PersistentData> dataList = persistentDataStore
            .query(Integer.class, "auto-exec-asset-dep-test");
        //should be two one from the first run and one from restart
        assertThat(dataList.size(), is(2));
    }
    
    /**
     * Test that the link layer dep program made in {@link 
     * mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#createAutoExecPrograms()} recovers and executes as 
     * expected.
     */
    @Test
    public void testLinkLayerDepAutoExec() 
        throws IOException, InterruptedException, IllegalArgumentException, AssetException, BundleException, 
        CCommException, PersistenceFailedException, PhysicalLinkException, IllegalStateException, 
            MissionProgramException
    {
        //get all programs using that template, should only be one
        Program program = m_MissionProgramManager.getProgram("auto-exec-linklayer-dep-test");
        assertThat(program, is(notNullValue()));
        
        // wait for program to execute
        EventHandlerSyncer syncer = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=auto-exec-linklayer-dep-test)");
        if (program.getProgramStatus().equals(ProgramStatus.UNSATISFIED))
        {
            Logging.log(LogService.LOG_INFO, "auto-exec-linklayer-dep-test not complete yet, waiting for it");
            syncer.waitForEvent(10);
        }
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        PersistentDataStore persistentDataStore = IntegrationTestRunner.getService(PersistentDataStore.class);
        
        Collection<? extends PersistentData> dataList = persistentDataStore.query(
            Integer.class, "auto-exec-linklayer-dep-test");
        assertThat(dataList.size(), is(2));
    }
    
    /**
     * Test that the programs made in {@link 
     * mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#createAutoExecPrograms()} recover and execute as 
     * expected.
     */
    @Test
    public void testProgramDepAutoExec() 
        throws IOException, InterruptedException, IllegalArgumentException, BundleException
    {
        // wait for program to execute
        EventHandlerSyncer syncer1 = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=auto-exec-program-dep1-test)");
        EventHandlerSyncer syncer2 = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=auto-exec-program-dep2-test)");
        EventHandlerSyncer syncer3 = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=auto-exec-program-dep3-test)");
        EventHandlerSyncer syncer4 = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=auto-exec-program-dep4-test)");
        
        Program program1 = m_MissionProgramManager.getProgram("auto-exec-program-dep1-test");
        Program program2 = m_MissionProgramManager.getProgram("auto-exec-program-dep2-test");
        Program program3 = m_MissionProgramManager.getProgram("auto-exec-program-dep3-test");
        Program program4 = m_MissionProgramManager.getProgram("auto-exec-program-dep4-test");
        // 4 depends on immediate program
        if (!program4.getProgramStatus().equals(ProgramStatus.EXECUTED))
        {
            syncer4.waitForEvent(10);
        }
        // 3 depends on 4
        if (!program3.getProgramStatus().equals(ProgramStatus.EXECUTED))
        {
            syncer3.waitForEvent(10);
        }
        // 1 and 2 depend on link layer test
        if (!program1.getProgramStatus().equals(ProgramStatus.EXECUTED))
        {
            syncer1.waitForEvent(10);
        }
        if (!program2.getProgramStatus().equals(ProgramStatus.EXECUTED))
        {
            syncer2.waitForEvent(10);
        }
        
        assertThat(program1.getProgramStatus(), is(ProgramStatus.EXECUTED));
        assertThat(program2.getProgramStatus(), is(ProgramStatus.EXECUTED));
        assertThat(program3.getProgramStatus(), is(ProgramStatus.EXECUTED));
        assertThat(program4.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        PersistentDataStore persistentDataStore = IntegrationTestRunner.getService(PersistentDataStore.class);
        
        Collection<? extends PersistentData> dataList = persistentDataStore.query(
            Integer.class, "auto-exec-program-dep1-test");
        assertThat(dataList.size(), greaterThan(0));
        PersistentData data = dataList.iterator().next();
        assertThat((Integer)data.getEntity(), is(1));
        dataList = persistentDataStore.query(Integer.class, "auto-exec-program-dep2-test");
        assertThat(dataList.size(), greaterThan(0));
        data = dataList.iterator().next();
        assertThat((Integer)data.getEntity(), is(2));
        dataList = persistentDataStore.query(Integer.class, "auto-exec-program-dep3-test");
        assertThat(dataList.size(), greaterThan(0));
        assertThat((Integer)data.getEntity(), is(2));
        dataList = persistentDataStore.query(Integer.class, "auto-exec-program-dep4-test");
        assertThat(dataList.size(), greaterThan(0));
        assertThat((Integer)data.getEntity(), is(2));
    }    
    
    /**
     * Test that the mission remove {@link TestMissionProgramManager#testRemoval()} still does not exist.
     */
    @Test
    public void testMissionRemoved()
    {
        //verify that the program is not in the managed program list
        try
        {
            m_MissionProgramManager.getProgram("Removal");
            fail("Expecting exception as the program was removed.");
        }
        catch (IllegalArgumentException e)
        {
            //expected exception the program was removed
        }
    }
    
    /**
     * Tests that missions with different types of execution parameters are correctly recreated upon system restart.
     * See {@link mil.dod.th.ose.controller.integration.TestPrepFor2ndRun#testCreateMissionProgram()}.
     */
    @Test
    public void testPersistedMissionExecArgs() throws IllegalStateException, MissionProgramException, 
        InterruptedException
    {
        //register primitive test Service
        final TestPrimitivesService service = new TestPrimitivesService();        
        IntegrationTestRunner.getBundleContext().registerService(TestPrimitivesService.class, service, null); 
        
        // wait for program to execute
        EventHandlerSyncer programExecutedSyncer = 
                new EventHandlerSyncer(Program.TOPIC_PROGRAM_EXECUTED, "(name=Test_Diff_Auto_Args)");
        
        //get the program made during prep for second run of tests
        Program program = m_MissionProgramManager.getProgram("Test_Diff_Auto_Args");
        //check that program persisted 
        assertThat(program, is(notNullValue()));
        program.execute();
        
        //wait for event
        programExecutedSyncer.waitForEvent(5);
        //if the program is executed successfully the status will be executed, if params are not restored properly
        //the script execution will throw an error and the status will not be executed.
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));  
    }
    
    /**
     * Service for testing primitive values. These methods are called from within the script-variable-types. 
     * It tests that these values can be set, and that the correct types are passed as arguments. 
     * @author callen
     */
    public class TestPrimitivesService
    {
        private int testIntA;
        private int testIntB;
        private long testLongA;
        private short testShortA;
        private double testDoubleA;
        private boolean testBooleanA;
        
        public void setIntegerA(final int scriptInt)
        {
            testIntA = scriptInt;
        }
        public void setIntegerB(final int scriptInt)
        {
            testIntB = scriptInt;
        }
        public void setLongA(final long scriptLong)
        {
            testLongA = scriptLong;
        }
        public void setShortA(final short scriptShort)
        {
            testShortA = scriptShort;
        }
        public void setDoubleA(final double scriptDouble)
        {
            testDoubleA = scriptDouble;
        }
        public void setBooleanA(final boolean scriptBoolean)
        {
            testBooleanA = scriptBoolean;
        }
        public void assertValues()
        {
            //if this service is used by other tests in the future these values will need to be adjusted.
            //these values are what the program execution parameters are set to. See TestPrepFor2ndRun.
            assertThat(testIntA, is(12));
            assertThat(testIntB, is(90));
            assertThat(testLongA, is(12L));
            assertThat(testShortA, is((short)3));
            assertThat(testDoubleA, is(19.36));
            assertThat(testBooleanA, is(true));
        }
    }
    
    /**
     * Test that mission programs that are inactive do not execute. 
     * Rechecked in {@link TestMissionProgramManager#testInactiveProgram()}.
     */
    @Test
    public void testInactiveProgram() throws IllegalArgumentException, PersistenceFailedException, 
        MissionProgramException
    {
        Program program = m_MissionProgramManager.getProgram("TestInactive");
        //a mission that has not attempted to execute should be unsatisfied
        assertThat(program.getProgramStatus(), is(ProgramStatus.UNSATISFIED));
        assertThat(program.getMissionSchedule().isActive(), is(false));
    }
}
