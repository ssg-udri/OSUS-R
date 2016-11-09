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

import java.util.concurrent.Future;

import mil.dod.th.core.mp.MissionScript.TestResult;


/**
 * Interface that manages executing mission programs.
 * @author callen
 *
 */
public interface MissionProgramScheduler
{
    /**
     * Submit a mission program for execution. If the program is a program that operates on a schedule the 
     * program will be queued for execution at the times identified in the mission's schedule. Even if scheduled for 
     * immediate execution, the program may not have started before this method returns.
     * 
     * @param program
     *    the program to be executed
     * @throws IllegalStateException
     *    if the program is requested to execute, but can't because of the mission is already scheduled and cannot be 
     *    cancelled
     * @throws IllegalArgumentException
     *    if the program is already known to this service
     */
    void executeProgram(ProgramImpl program) throws IllegalStateException, IllegalArgumentException;
    
    /**
     * Submit a mission program for execution of its test function.
     * @param program
     *    the program who's test function is to be executed
     * @return
     *     a future object which will contain any exceptions that may have occurred during execution.
     */
    Future<TestResult> testProgram(ProgramImpl program); //NOPMD test execute is not a JUNIT test, the name
                                                                //of a function in a mission program can be called test,
                                                       //this nomenclature is used else where, used here for consistency
    
    /**
     * Request that shutdown is initialized for the specified program. This request will fail for programs which are 
     * currently executing.
     * @param program
     *     the program being requested to shutdown
     */
    void shutdownProgram(ProgramImpl program);
    
    /**
     * Cancel the execution of the specified program. Programs can only be canceled if they operate on a schedule and
     * have not yet executed. 
     * @param programName
     *     the name of the program to remove from the queue
     * @return
     *     true if successfully canceled, otherwise false
     * @throws IllegalArgumentException
     *     thrown if the given program is not contained within the queue of mission programs scheduled to execute
     */
    boolean cancelScheduledProgram(String programName) throws IllegalArgumentException;
}
