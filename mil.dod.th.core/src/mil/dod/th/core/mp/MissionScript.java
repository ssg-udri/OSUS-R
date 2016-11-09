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
package mil.dod.th.core.mp;

import java.security.PrivilegedActionException;

import javax.script.ScriptException; //NOCHECKSTYLE: import not used - needed for javadoc links

import aQute.bnd.annotation.ProviderType;

/**
 * This interface describes the methods that the {@link MissionProgramManager} will call to execute mission programs.
 * Implementation of this interface must be within all scripts that are embedded in mission program templates.
 * instances. Once all dependencies required for a script to execute are available and this interface is initialized
 * within the {@link MissionProgramManager} it is assumed that the dependencies will stay statically available.
 * 
 * @author callen
 */
@ProviderType
public interface MissionScript 
{
    /** 
     * Run the mission as described by the 'execute' function.
     * 
     * @throws PrivilegedActionException
     *      method is invoked by the script engine, which will throw this exception if a {@link ScriptException} occurs
     */
    void execute() throws PrivilegedActionException;

    /**
     * Test the mission as described by the 'test' function. It is assumed
     * that all data that may be created from this test will be marked as 'test' data.
     * @return
     *     enumerative value representing if the test was successful
     * @throws PrivilegedActionException
     *      method is invoked by the script engine, which will throw this exception if a {@link ScriptException} occurs 
     */
    TestResult test() throws PrivilegedActionException;
    
    /**
     * Shutdown the execution of the mission as described by the 'shutdown' function. 
     * All cleanup/release of resources is expected to by handled by the mission itself. Failing to cleanup properly 
     * can prevent other missions from being able to execute.
     * @throws PrivilegedActionException
     *      method is invoked by the script engine, which will throw this exception if a {@link ScriptException} occurs 
     */
    void shutdown() throws PrivilegedActionException;

    /**
     * Enumeration representing success of a mission test.
     */
    enum TestResult
    {
        /** The mission test passed. */
        PASSED,
        
        /** The mission test failed. */
        FAILED;
    }

    /**
     * Used to acquire the test result from mission script objects.
     */
    class TestResultConversionClass //NOCHECKSTYLE: NOPMD: No private constructor for utility class. Need public
    {                               //constructor to bind this class to the script engine.
        /** The mission test passed. */
        public final static TestResult PASSED = TestResult.PASSED;
        /** The mission test failed. */
        public final static TestResult FAILED = TestResult.FAILED;
    }
}
