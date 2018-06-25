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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.model.FlagEnum;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.types.DigitalMedia;

/**
 * Represents the interface to a mission program that is managed by the {@link MissionProgramManager}. 
 * The programs are only persisted at creation. Changes to the variables, schedule, and execution parameters are not 
 * supported via this interface. Each program will have access to the variables from the services rendered by the
 * {@link MissionProgramManager}.
 * 
 * Programs that have transport dependencies are responsible to release these resources after execution, via the
 * source code for the mission. Failure to do so can lead to deadlock of resources.
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface Program
{
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/mp/Program/";

    /** 
     * Topic used for when a program has been executed. Properties included in the event are:
     * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * </ul>
     * 
     * @see #execute()
     */
    String TOPIC_PROGRAM_EXECUTED = TOPIC_PREFIX + "PROGRAM_EXECUTED";
    
    /** 
     * Topic used when a program has been executed, but execution fails. Properties included in the event are:
       * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_EXCEPTION}</li>
     * </ul>
     * 
     * @see #execute()
     */
    String TOPIC_PROGRAM_EXECUTED_FAILURE = TOPIC_PREFIX + "PROGRAM_EXECUTED_FAILURE";

    /** 
     * Topic used for when a program has been 'test' executed. Properties included in the event are:
       * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_TEST_RESULT}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * <br>
     * <br>
     * Additionally if the test result is {@link TestResult#FAILED} and an exception was thrown:
     * <li>{@link Program#EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION}</li>
     * </ul> 
     *
     * @see #executeTest()
     */
    String TOPIC_PROGRAM_TESTED = TOPIC_PREFIX + "PROGRAM_TESTED";
    
    /** 
     * Topic used for when a program has been 'shutdown'. Properties included in the event are:
     * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * </ul> 
     *
     * @see #executeShutdown()
     */
    String TOPIC_PROGRAM_SHUTDOWN = TOPIC_PREFIX + "PROGRAM_SHUTDOWN";
    
    /**
     * Topic used for when a program fails to 'shutdown' within a set number of iterations. 
     * Properties included in the event are:
     * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_EXCEPTION}</li>
     * </ul>
     * 
     * @see #executeShutdown()
     */
    String TOPIC_PROGRAM_SHUTDOWN_FAILURE = TOPIC_PREFIX + "PROGRAM_SHUTDOWN_FAILURE";

    /** 
     * Topic used for when a program status has been changed. Properties included in the event are:
       * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * </ul>
     * 
     */
    String TOPIC_PROGRAM_STATUS_CHANGED = TOPIC_PREFIX + "PROGRAM_STATUS_CHANGED";
    
    /** Event property key for the program's name. */
    String EVENT_PROP_PROGRAM_NAME  = "name";

    /** Event property key for a program's 'Test' function result, the result is of {@link TestResult} type. */
    String EVENT_PROP_PROGRAM_TEST_RESULT  = "program.testresults";

    /** Event property key for a program's 'Test' function's exception, if one occurs. */
    String EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION  = "program.testresults.exception";
    
    /** Event property key for a program's 'Execute' function's exception, if one occurs. */
    String EVENT_PROP_PROGRAM_EXCEPTION  = "program.exception";

    /** Event property key for a program's status as a string {@link ProgramStatus#toString()}. */
    String EVENT_PROP_PROGRAM_STATUS = "program.status";
    
    /**
     * Get the mission program's name.
     * 
     * @return
     *      the name of the program
     */
    String getProgramName();

    /**
     * Get the name of the program's template.
     * 
     * @return
     *      template name
     */
    String getTemplateName();
    
    /**
     * Get the vendor of the program.
     *  
     * @return
     *      vendor as a string
     */
    String getVendor();
    /**
     * Get the JavaScript source of the program.
     *  
     * @return
     *      source as a string
     */
    String getSource();

    /**
     * Get the mission program description.
     * 
     * @return
     *     A string, that represents the mission programs description.
     */
    String getDescription();
    
    /**
     * Remove the program from the manager.
     * @throws IllegalStateException
     *     if the program is currently executing, or the program has already been removed
     */
    void remove() throws IllegalStateException;
    
    /**
     * Cancel the scheduled execution of this program.
     * @return
     *     true if the the program was able to be canceled
     * @throws IllegalStateException
     *     if the program is in a state preventing canceling, or the program has already been removed
     */
    boolean cancel() throws IllegalStateException;

    /**
     * Execute the program. This manual execution, if ran before a scheduled execution will mark the program as 
     * executed. This requires reloading of the mission to re-run the program on a schedule. After execution of the 
     * mission program via its schedule the program can be manually executed without needing to reload the program. 
     * 
     * This method only submits the program for execution and it may not have started by the time this method returns.
     * 
     * @throws MissionProgramException
     *      Deprecated exception.
     * @throws IllegalStateException
     *     if the program's dependencies are not fulfilled when the request was made
     */
    void execute() throws MissionProgramException, IllegalStateException;

    /**
     * This method can run test execution on a separate thread.
     * @return
     *     future object containing the test result or exception incurred during execution
     * @throws IllegalStateException
     *     if the program's dependencies are not fulfilled when the request was made
     * @throws UnsupportedOperationException
     *     if the program is not identified as having a test function in the template
     * @throws MissionProgramException
     *      Deprecated exception.
     */
    Future<TestResult> executeTest() throws IllegalStateException, UnsupportedOperationException,
            MissionProgramException;
    
    /**
     * Run the 'Shutdown' function defined within the program's script and put the program in the {@link 
     * ProgramStatus#SHUTDOWN} afterwards. Call is asynchronous so state may update after this call returns.
     * 
     * TD: should probably be renamed to stopAsync() to clarify that it not only executes the shutdown method, but also 
     * puts the program in the shutdown state, also to make it clear that method is async, ditto other async methods
     */
    void executeShutdown();
    
    /**
     * Get the last test results from executing the test function. Will return null if the test function has never been
     * ran.
     * 
     * @return
     *     the results of the last execution of the test function
     */
    TestResult getLastTestResult();

    /**
     * Query the status of the program.
     * 
     * @return
     *      string representing the status of the program
     */
    ProgramStatus getProgramStatus();
    
    /**
     * Get an unmodifiable set of the fulfilled dependencies for the program. When the {@link MissionProgramManager} 
     * service is provided and the {@link MissionProgramSchedule} represents an active program then execution
     * will take place after all objects named have been updated.
     * 
     * @param clazz
     *      base class of factory object (e.g., {@link mil.dod.th.core.asset.Asset}, {@link 
     *      mil.dod.th.core.ccomm.physical.PhysicalLink})
     * @return
     *      unmodifiable set containing the names of all the factory objects the program depends on
     */
    Set<String> getFactoryObjectDeps(Class<? extends FactoryObject> clazz);    
        
    /**
     * Get an unmodifiable set of the fulfilled programs this program depends on.  If the mission's 
     * {@link MissionProgramSchedule} identifies this mission for immediate execution this program will start when the 
     * {@link MissionProgramManager} service is provided, but only after all programs named have completed.  If a
     * program is called on to execute (using {@link #execute()}), it will not wait for the program dependencies to
     * complete. A program dependency must also be set for immediate execution. Executing a program manually (using
     * {@link #execute()}) does not trigger other programs that have already executed to run immediately for the new
     * mission program.
     * 
     * @return
     *      unmodifiable set containing the names of all the programs this program depends on
     */
    Set<String> getProgramDeps();    
       
    /**
     * Get the specified {@link FlagEnum} attribute value.
     * 
     * @param flag
     *    The enumerated flag type to get the status of
     * @return  
     *    Boolean value that signifies the specified flag's status.
     */      
    boolean getFlag(FlagEnum flag); 
    
    /**
     * Get the primary image. This is a copy of the actual image, any changes made to the 
     * image will not be reflected in the {@link mil.dod.th.core.mp.model.MissionProgramTemplate}'s primary image. 
     * 
     * @return
     *     The primary image for the mission program.
     */
    DigitalMedia getPrimaryImage();
    
    /**
     * Get an unmodifiable list of the secondary image(s). The returned data is not a LIVE list, 
     * alterations to this data will not be reflected in the {@link mil.dod.th.core.mp.model.MissionProgramTemplate}'s
     * secondary image(s). 
     * 
     * @return
     *     A list of all secondary images.
     */
    List<DigitalMedia> getSecondaryImages();   

    /**
     * Get an unmodifiable variable metadata list. The list contains all variable metadata set within 
     * {@link MissionVariableMetaData} for each variable. The returned data is not a LIVE list, 
     * alterations to this data will not be reflected in the mission program's actual variable metadata.
     * 
     * @return
     *     List of all variable metadata held within this mission program.
     */
    List<MissionVariableMetaData> getVariableMetaData();
    
    /**
     * Get an unmodifiable view of the mission program's schedule. Changes to the returned object will not be
     * reflected in the mission program's actual {@link MissionProgramSchedule}. 
     * 
     * @return
     *     A mission program object representing the mission program's schedule. 
     */
    MissionProgramSchedule getMissionSchedule();
    
    /**
     * Get a view of this program's execution parameters. The returned map is read-only. The only supported way to 
     * change execution parameters is to construct a new program.
     * @return
     *     a map representing the execution parameters set for this program.
     */
    Map<String, Object> getExecutionParameters();
    
    /**
     * Get the last exception that might of occurred during testing of a mission program.
     * @return
     *     string cause of last exception, could be empty
     */
    String getLastTestExceptionCause();
    
    /**
     * Get the last exception that might of occurred during execution of a mission program.
     * @return
     *     string cause of last exception, could be empty
     */
    String getLastExecutionExceptionCause();

    /**
     * Enumeration representing the status of a mission program.
     */
    enum ProgramStatus
    {
        /** Means the program has executed. */
        EXECUTED,
        
        /** The mission program is currently executing. */
        EXECUTING,
        
        /** The mission program is currently executing its {@link Program#executeTest()} function. */
        EXECUTING_TEST,
        
        /** Signifies a mission that failed to correctly execute due to a script error. */
        SCRIPT_ERROR,
        
        /** Signifies a mission that will never execute due to a variable error. Possible reasons are that a 
         * variable's value is not a supported type, or variable's metadata values and execution parameters 
         * could not be used to get a valid value for a variable.*/
        VARIABLE_ERROR,
        
        /** The mission program's dependencies are unsatisfied this is not an error with program. */
        UNSATISFIED,
        
        /** The mission program's dependencies are satisfied, and the mission has not been initialized. 
         * The initialization process evaluates the script once all dependencies are satisfied.
         */
        WAITING_UNINITIALIZED,
        
        /** The mission has been initialized and is waiting to execute. The initialization process evaluates
         * the script and binds the variable(s) to the script engine.
         */
        WAITING_INITIALIZED,
        
        /** The mission incurred an error during the initial evaluation of the script. */
        INITIALIZATION_ERROR,
        
        /** The mission is scheduled for future execution. */
        SCHEDULED,
        
        /** The mission program is currently shutting down. */
        SHUTTING_DOWN,
        
        /** The mission program has been shutdown. */
        SHUTDOWN,
        
        /** The mission program has been cancelled. */
        CANCELED;
    }
}
