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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import com.google.common.base.Preconditions;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionScript;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.model.FlagEnum;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.mp.model.MissionVariableTypesEnum;
import mil.dod.th.core.mp.model.ScheduleEnum;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.ose.shared.MapTranslator;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;

/**
 * Implements the {@link Program} interface providing a data/control interface for mission programs.  
 * 
 * NOTE: Modifications to the program must be synchronized using the instance of the program as other threads may be 
 * reading the state of the program, in particular for auto execution purposes
 * 
 * @author dhumeniuk
 * 
 */
public class ProgramImpl implements Program //NOPMD NOCHECKSTYLE - Too many fields, large number of fields needed for 
                                    //wide variety of potential dependency types (Assets, 3 types of ccomm layers, etc)
{
    /**
     * Manager of this program.
     */
    private final MissionProgramManagerImpl m_Manager;
    
    /**
     * Reference to the EventAdmin service that all events will be posted to.
     */
    private final EventAdmin m_EventAdmin;
    
    /**
     * Map of execution parameters. At initialization of the program these parameters are of simple types that
     * easily map to xml simple Types. After creation of the program and validation that the parameters and 
     * variable metadata are sound, the map of execution parameters holds actual values for the variables they 
     * represent. For example parameter entry, (x, asset.getName()), will become (x, ExampleAsset{random PID}). This
     * translation/fulfillment of dependency values relies on the look up services, if the dependency value is
     * a non-primitive.
     */
    private final Map<String, Object> m_ExecParams = new HashMap<String, Object>();
    
    /**
     * Untranslated execution parameters.
     */
    private final Map<String, Object> m_ExecutionParameters;
    
    /**
     * Set of Asset dependencies.
     */
    private Set<String> m_AssetDeps;
    
    /**
     * Set of Transport Layer dependencies.
     */
    private Set<String> m_TransportDeps;
    
    /**
     * Set of Link Layer dependencies. 
     */
    private Set<String> m_LinkLayerDeps;
    
    /**
     * Set of Physical Layer dependencies.
     */
    private Set<String> m_PhysicalLinkDeps;
    
    /**
     * Set of all program dependencies.
     */
    private Set<String> m_ProgDeps;
    
    /**
     * Represents the status of the mission.
     */
    private ProgramStatus m_ProgramStatus;

    /**
     * The mission script object representing this program's initialized JavaScript implementation.
     */
    private MissionScript m_MissionScript;

    /**
     * The program's mission program template. 
     */
    private final MissionProgramTemplate m_MissionTemplate;
    
    /**
     * Mission program schedule for this particular program. 
     */
    private final MissionProgramSchedule m_MissionSchedule;

    /**
     * The mission program's name, as set in the parameters.
     */
    private final String m_ProgramName;
        
    /**
     * UUID for the MissionProgram, this is used to uniquely identify the program instance model within the 
     * {@link MissionProgramManagerImpl}.
     */
    private final UUID m_ProgramUuid;

    /**
     * Results of last test execution.
     */
    private TestResult m_LastTestResult;
    
    /**
     * Last test exception cause.
     */
    private String m_LastTestException;
    
    /**
     * Last execution exception cause.
     */
    private String m_LastExecutionException;
    
     /**
     * This constructor assumes MissionProgramInstance is already validated. 
     * 
     * @param manager
     *     The mission program manager required for the removal and execution.
     * @param eventAdmin
     *     The event admin required to post events such as status changed.
     * @param template
     *     The mission program template to use for this program.
     * @param params
     *     The parameters to use during the execution of this program.         
     * @param uuid     
     *     The UUID assigned for the mission program instance. 
     */
    public ProgramImpl(final MissionProgramManagerImpl manager, final EventAdmin eventAdmin, 
        final MissionProgramTemplate template, final MissionProgramParameters params, final UUID uuid)
    {
        //This constructor is used by the MissionProgramManager during the creation of new missions.
        m_Manager = manager;
        m_EventAdmin = eventAdmin;
        m_MissionTemplate = template;
        m_MissionSchedule = params.getSchedule();
        m_ProgramUuid = uuid;
        m_ProgramName = params.getProgramName();
        m_ExecParams.putAll(MapTranslator.translatePairList(params.getParameters()));
        m_ExecutionParameters = new HashMap<String, Object>(m_ExecParams);
        checkParameters(m_ExecParams);
        //set factory and program deps
        setFactoryProgramDepencencies();
        m_LastTestException = "";
        m_LastExecutionException = "";
    }
    
    @Override
    public String getProgramName()
    {
        return m_ProgramName; 
    }

    @Override
    public String getTemplateName()
    {
        return m_MissionTemplate.getName();
    }
    
    @Override
    public String getVendor()
    {
        return m_MissionTemplate.getVendor();
    }
    
    @Override
    public String getSource()
    {
        return m_MissionTemplate.getSource();
    }

    @Override
    public String getDescription()
    {
        return m_MissionTemplate.getDescription();
    }
    
    @Override
    public void remove() throws IllegalStateException
    {
       //can only permit a program to be removed if it is canceled, shutdown, or wait_initialized
        checkReadyForRemoval();
        try
        {
            m_Manager.removeProgram(this, m_ProgramUuid);
        }
        catch (final IllegalArgumentException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public void execute() throws MissionProgramException, IllegalStateException
    {
        if (isExecuteReady())
        {
            m_Manager.executeProgram(this);
        }
    }
    
    @Override
    public Future<TestResult> executeTest() throws IllegalStateException
    {
        //check is ready to test
        isTestExecuteReady();
        return m_Manager.testProgram(this);
    }
    
    @Override
    public void executeShutdown()
    {
        m_Manager.shutdownProgram(this);
    }

    @Override
    public TestResult getLastTestResult()
    {
        return m_LastTestResult;
    }

    @Override
    public ProgramStatus getProgramStatus()
    {
        return m_ProgramStatus;
    }

    @Override
    public Set<String> getFactoryObjectDeps(final Class<? extends FactoryObject> clazz)
    {
        final Set<String> depSet;
        if (clazz.equals(Asset.class))
        {
            depSet = new HashSet<String>(m_AssetDeps);
        }
        else if (clazz.equals(PhysicalLink.class))
        {
            depSet = new HashSet<String>(m_PhysicalLinkDeps);
        }
        else if (clazz.equals(LinkLayer.class))
        {
            depSet = new HashSet<String>(m_LinkLayerDeps);
        }
        else if (clazz.equals(TransportLayer.class))
        {
            depSet = new HashSet<String>(m_TransportDeps);
        }
        else
        {
            throw new IllegalArgumentException("Not a valid factory object type: " + clazz);
        }
        return Collections.unmodifiableSet(depSet);
    }
    
    @Override
    public MissionProgramSchedule getMissionSchedule() 
    {
        //get the values from the schema
        final boolean isActive = m_MissionSchedule.isActive();
        final boolean atReset = m_MissionSchedule.isAtReset();
        final Long startTime = m_MissionSchedule.getStartInterval();
        final Boolean isImmediate = m_MissionSchedule.isImmediately();
        final Long stopInterval =  m_MissionSchedule.getStopInterval();
        final Boolean indefiniteInterval = m_MissionSchedule.isIndefiniteInterval();
        final MissionProgramSchedule schedule = new MissionProgramSchedule(isActive, atReset, isImmediate, startTime, 
            indefiniteInterval, stopInterval);
        return schedule;
    }
    
    @Override
    public Set<String> getProgramDeps()
    {
        if (m_ProgDeps.isEmpty())
        {
            return  Collections.emptySet();
        }
        return Collections.unmodifiableSet(m_ProgDeps);
    }
    
    @Override
    public boolean getFlag(final FlagEnum flag)
    {
        switch (flag)
        {
            case WITH_IMAGE_CAPTURE: 
                return m_MissionTemplate.isWithImageCapture();
            case WITH_INTERVAL:
                return m_MissionTemplate.isWithInterval();
            case WITH_SENSOR_TRIGGER: 
                return m_MissionTemplate.isWithSensorTrigger();
            case WITH_TIMER_TRIGGER: 
                return m_MissionTemplate.isWithTimerTrigger();
            case WITH_PAN_TILT: 
                return m_MissionTemplate.isWithPanTilt();
            default: 
                throw new IllegalArgumentException(flag + " is not a recognized flag type.");
        }
    }
    
    @Override
    public DigitalMedia getPrimaryImage()
    {
        final String encoding = m_MissionTemplate.getPrimaryImage().getEncoding();
        if (m_MissionTemplate.getPrimaryImage().isSetValue())
        {
            //To prevent changes to the image a copy is made
            final int length = m_MissionTemplate.getPrimaryImage().getValue().length;
            final byte[] value = new byte[length];
            System.arraycopy(m_MissionTemplate.getPrimaryImage().getValue(), 0, value, 0, length);
            return new DigitalMedia(value, encoding);
        }
        return new DigitalMedia(null, encoding);  
    }
    
    @Override
    public List<DigitalMedia> getSecondaryImages()
    {
        //A new list that contains copies of the images is returned to prevent changes being made to the data.
        final List<DigitalMedia> mediaList = new ArrayList<DigitalMedia>();
        for (DigitalMedia media : m_MissionTemplate.getSecondaryImages())
        {            
            final String encoding = media.getEncoding();
            if (media.isSetValue())
            {
                final int length = m_MissionTemplate.getPrimaryImage().getValue().length;
                final byte[] value = new byte[length]; 
                System.arraycopy(media.getValue(), 0, value, 0, length);
                final DigitalMedia digMed = new DigitalMedia(value, encoding);  
                mediaList.add(digMed); 
            }
            final DigitalMedia digMed = new DigitalMedia(null, encoding);  
            mediaList.add(digMed);
            //attach all the copied attributes to the new instance.
        }
        return Collections.unmodifiableList(mediaList);
    }
    
    @Override
    public final List<MissionVariableMetaData> getVariableMetaData()
    {
        //copies of the variable metadata are returned to prevent changes being made to the data.
        final List<MissionVariableMetaData> data = new ArrayList<MissionVariableMetaData>();      
        for (MissionVariableMetaData entry : m_MissionTemplate.getVariableMetaData())
        {            
            final String name = entry.getName();
            final String description = entry.getDescription();
            final String defaultValue = entry.getDefaultValue();            
            final String minValue = entry.getMinValue();
            final String maxValue = entry.getMaxValue();
            final MissionVariableTypesEnum type = entry.getType();
            final List<String> optionalValues = new ArrayList<String>(entry.getOptionValues());
            final String humanreadablename = entry.getHumanReadableName();
            final MissionVariableMetaData returnEntry = new MissionVariableMetaData(name, defaultValue,  
                 optionalValues, maxValue, minValue, description, humanreadablename, type);
          
                                                                                  
            data.add(returnEntry);    
        }
        return Collections.unmodifiableList(data);
    }
    
    /**
     * Get event properties used when this program is the main concern of an event.
     * Properties are:
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * </ul>
     * @return
     *     the program's event properties
     */
    public Map<String, Object> getEventProperties()
    {
        //the properties
        final Map<String, Object> statusEventProps = new HashMap<String, Object>();
        statusEventProps.put(EVENT_PROP_PROGRAM_NAME, m_ProgramName);
        statusEventProps.put(EVENT_PROP_PROGRAM_STATUS, m_ProgramStatus.toString());
        return statusEventProps;
    }

    @Override
    public boolean cancel()
    {
        try
        {
            if (m_Manager.cancelProgram(getProgramName()))
            {
                changeStatus(ProgramStatus.CANCELED);
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (final IllegalArgumentException e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Map<String, Object> getExecutionParameters()
    {
        return Collections.unmodifiableMap(m_ExecutionParameters);
    }
    
    @Override
    public String getLastTestExceptionCause()
    {
        return m_LastTestException;
    }

    @Override
    public String getLastExecutionExceptionCause()
    {
        return m_LastExecutionException;
    }
    
    /**
     * Get schedule flag values.
     * 
     * @param flag
     *     the flag to get the value of
     * @return
     *     boolean value corresponding to the passed flag value
     */
    public boolean getScheduleFlag(final ScheduleEnum flag)
    {
        switch (flag)
        {
            case START_IMMEDIATELY:
                return m_MissionSchedule.isSetImmediately() ? m_MissionSchedule.isImmediately() : false;
            case START_AT_RESTART:
                return m_MissionSchedule.isAtReset();
            case WITH_INDEFINITE_INTERVAL:
                return m_MissionSchedule.isSetIndefiniteInterval() ? m_MissionSchedule.isIndefiniteInterval() : false;
            case IS_ACTIVE:
                return m_MissionSchedule.isActive();
            default: 
                throw new IllegalArgumentException(flag + " is not a recognized schedule flag type");
        }
    }
    
    /**
     * Get the execution parameters.
     * 
     * @return
     *     map representing execution parameters
     */
    public Map<String, Object> getExecParams()
    {
        return Collections.unmodifiableMap(m_ExecParams);
    }
    
    /**
     * Translates the variable metadata into transport, linklayer, program and asset dependencies.
     */
    public void reconcileDependencies() //NOCHECKSTYLE: Cyclomatic complexity //NOPMD:Avoid REALLY long methods 
    {                                  //- needed because of the different  variable types
        for (MissionVariableMetaData data : this.getVariableMetaData())
        {
            final MissionVariableTypesEnum type = data.getType();
            final String name = data.getName();            
            final Object variableValue = m_ExecParams.get(name);
            if (variableValue.getClass() != String.class)
            {
                // dependencies will be stored as the actual object once satisfied, not a string anymore so the 
                // dependency is satisfied
                continue;
            }
            final String variableStringVal = (String)variableValue;
            switch (type)
            {
                //for each factory or program type if the execution parameter values are not of the correct type then
                //they need to be looked up still.
                case ASSET:
                {
                    final boolean assetSatisfied = assetDep(data);
                    if (!assetSatisfied)
                    {
                        //if the dep wasn't satisfied no need to check others
                        Logging.log(LogService.LOG_DEBUG, "Asset dependency [%s] not available for program [%s]",
                                variableStringVal, getProgramName());
                        return;
                    }
                    break;
                }
                case PHYSICAL_LINK:
                {
                    final boolean physSatisfied = physicalDep(data);
                    if (!physSatisfied)
                    {
                        //if the dep wasn't satisfied no need to check others
                        Logging.log(LogService.LOG_DEBUG, 
                                "Physical link dependency [%s] not available for program [%s]", variableStringVal, 
                                getProgramName());
                        return;
                    }
                    break;                    
                }
                case LINK_LAYER:
                {
                    final boolean linkSatisfied = linkLayerDep(data);
                    if (!linkSatisfied)
                    {
                        //if the dep wasn't satisfied no need to check others
                        Logging.log(LogService.LOG_DEBUG, 
                                "Link layer dependency [%s] not available for program [%s]", variableStringVal, 
                                getProgramName());
                        return; 
                    }
                    break;
                }
                case TRANSPORT_LAYER:
                {
                    final boolean transSatisfied = transportDep(data);
                    if (!transSatisfied)
                    {
                        //if the dep wasn't satisfied no need to check others
                        Logging.log(LogService.LOG_DEBUG, 
                                "Transport layer dependency [%s] not available for program [%s]", variableStringVal, 
                                getProgramName());
                        return;
                    }                    
                    break;
                }
                case PROGRAM_DEPENDENCIES:
                {
                    final boolean programSatisfied = programDep(data);
                    if (!programSatisfied)
                    {
                        //if the dep wasn't satisfied no need to check others
                        Logging.log(LogService.LOG_DEBUG, 
                                "Program [%s] depends on another program [%s], but it hasn't run yet", 
                                getProgramName(), variableStringVal);
                        return;
                    }                    
                    break;
                }                
                default: 
                {
                    break;
                }
            }            
        }
        //if this point is reached all deps were successful reconciled
        changeStatus(ProgramStatus.WAITING_UNINITIALIZED);
    }
    
    /**
     * Requests the asset dependency value from the {@link MissionProgramManagerImpl}.
     * @param data
     *     the variable metadata object
     * @return 
     *     boolean value representing if the variable's value was able to be fulfilled
     */
    private boolean assetDep(final MissionVariableMetaData data)
    {
        final String depVal = (String)m_ExecParams.get(data.getName());
        final String name = data.getName();
        final Asset value = m_Manager.getAssetDep(depVal);
        if (value == null)
        {
            //no valid values returned
            return false;
        }
        m_ExecParams.put(name, value);
        return true;
    }
    
    /**
     * Requests the physical dependency value.
     * @param data
     *     the variable metadata object
     * @return
     *     boolean value representing if the variable's value was able to be fulfilled
     */
    private boolean physicalDep(final MissionVariableMetaData data)
    {
        final String depVal = (String)m_ExecParams.get(data.getName());
        final String name = data.getName();         
        final PhysicalLink value = m_Manager.getPhysicalDep(depVal);
        if (value == null)
        {
            //no valid values returned
            return false;
        }
        m_ExecParams.put(name, value);
        return true;
    }
    
    /**
     * Requests the transport dependency value.
     * @param data
     *     the variable metadata object
     * @return
     *     boolean value representing if the variable's value was able to be fulfilled
     */
    private boolean transportDep(final MissionVariableMetaData data)
    {
        final String depVal = (String)m_ExecParams.get(data.getName());
        final String name = data.getName();
        final TransportLayer value = m_Manager.getTransportDep(depVal);
        if (value == null)
        {
            //no valid values returned
            return false;
        }
        m_ExecParams.put(name, value);
        return true;
    }
    
    /**
     * Requests the linklayer dependency value.
     * @param data
     *     the variable metadata object
     * @return
     *     boolean value representing if the variable's value was able to be fulfilled
     */
    private boolean linkLayerDep(final MissionVariableMetaData data)
    {
        final String depVal = (String)m_ExecParams.get(data.getName());
        final String name = data.getName(); 
        final LinkLayer value = m_Manager.getLinkDep(depVal);
        if (value == null)
        {
            //no valid values returned
            return false;
        }
        m_ExecParams.put(name, value);
        return true;
    }
    
    /**
     * Requests the program dependency value.
     * @param data
     *     the variable metadata object
     * @return
     *     boolean value representing if the variable's value was able to be fulfilled
     */
    private boolean programDep(final MissionVariableMetaData data)
    {
        final String depVal = (String)m_ExecParams.get(data.getName());
        final String name = data.getName();         
        final Program value = m_Manager.getProgramDep(depVal);
        if (value == null)
        {
            //no valid values returned
            return false;
        }
        m_ExecParams.put(name, value);
        return true;
    }
    
    /**
     * This method is needed when the program is first created. It takes the variable metadata and the execution
     * parameters to create the factory and program dependency sets.
     */
    private void setFactoryProgramDepencencies()
    {
        m_AssetDeps = new HashSet<String>(); //initialize the dependency sets
        m_TransportDeps = new HashSet<String>();        
        m_LinkLayerDeps = new HashSet<String>();
        m_PhysicalLinkDeps = new HashSet<String>();
        m_ProgDeps = new HashSet<String>();
        for (MissionVariableMetaData data : m_MissionTemplate.getVariableMetaData())
        {
            final MissionVariableTypesEnum type = data.getType();
            if (m_ExecParams.isEmpty())
            {
                //if the execution params are empty there is nothing to match deps to.
                return;
            }
            //note that by the time this method is called the params and varMetaData are already verified
            final String depVal = m_ExecParams.get(data.getName()).toString();
            switch (type)
            {
                case ASSET:
                {
                    m_AssetDeps.add(depVal);
                    break;
                }
                case PHYSICAL_LINK:
                {
                    m_PhysicalLinkDeps.add(depVal);
                    break;                    
                }
                case LINK_LAYER:
                {
                    m_LinkLayerDeps.add(depVal);
                    break;
                }
                case TRANSPORT_LAYER:
                {
                    m_TransportDeps.add(depVal);
                    break;
                }
                case PROGRAM_DEPENDENCIES:
                {
                    m_ProgDeps.add(depVal);
                    break;
                }                
                default:
                {
                    break;
                }
            }            
        }
    }
    
    /**
     * Checks that the parameter values, and variable metadata match. These execution parameters will be sent to the 
     * {@link javax.script.ScriptEngine} during execution of the program.
     * 
     * @param executionParams
     *     the parameters rendered from the MissionProgramParameters
     * @return
     *     map representing valid parameter values 
     */
    private Map<String, Object> checkParameters(final Map<String, Object> executionParams) 
    {
        for (MissionVariableMetaData data : m_MissionTemplate.getVariableMetaData())
        {            
            final String name = data.getName();
            if (executionParams.containsKey(name))
            { //NOCHECKSTYLE:Avoid empty statements
                //do nothing param and variable metadata match
            }
            else if (data.isSetDefaultValue())
            {
                final MissionVariableTypesEnum type = data.getType();
                final String value = data.getDefaultValue();
                if (type == MissionVariableTypesEnum.INTEGER)
                {
                    //change numerics to their proper type
                    m_ExecParams.put(name, Integer.parseInt(value));
                }
                else if (type == MissionVariableTypesEnum.DOUBLE)
                {
                    m_ExecParams.put(name, Double.parseDouble(value));
                }
                else if (type == MissionVariableTypesEnum.FLOAT)
                {
                    m_ExecParams.put(name, Float.parseFloat(value));
                }
                else if (type == MissionVariableTypesEnum.LONG)
                {
                    m_ExecParams.put(name, Long.parseLong(value));
                }
                else if (type == MissionVariableTypesEnum.SHORT)
                {
                    m_ExecParams.put(name, Short.parseShort(value));
                }
                else if (type == MissionVariableTypesEnum.BOOLEAN)
                {
                    m_ExecParams.put(name, Boolean.parseBoolean(value));
                }
                executionParams.put(name, data.getDefaultValue());  
            }
            else
            {
                //update the program status, not calling change status because it is overridable. If called
                //and the class is not completely constructed the program status could be null, which would lead
                //to unwanted behavior from the change status method. 
                m_ProgramStatus = ProgramStatus.VARIABLE_ERROR;
                throw new IllegalArgumentException("The program " + m_MissionTemplate.getName() + " will never execute."
                    + " Illegal parameter value!");
            }
        }
        //if all params are good, assign the program status to unsatisfied
        m_ProgramStatus = ProgramStatus.UNSATISFIED;
        return executionParams;
    }
    
    /**
     * Called when the program has executed, dependencies are not satisfied, and upon satisfying of dependencies. 
     * 
     * @param status
     *     the status value to update the program's status to
     * @exception IllegalStateException
     *     thrown in the event that the status change is illegal
     */
    // TODO TH-1043: Simplify logic (cyclomatic complexity above limit)
    public void changeStatus(final ProgramStatus status) throws IllegalStateException //NOCHECKSTYLE 
    {
        if (m_ProgramStatus == ProgramStatus.UNSATISFIED && status == ProgramStatus.EXECUTING)
        {
            throw new IllegalStateException("Mission [" + getProgramName() //NOCHECKSTYLE:multiple 
                    + "] incurred an error and may have executed without dependecies being satisfied."); //string 
        }                                                                    //literals the final messages are different
        if (m_ProgramStatus == ProgramStatus.EXECUTED)
        {
            if (status != ProgramStatus.EXECUTING //NOPMD the statements could be combined, but would make the code
                && status != ProgramStatus.SHUTTING_DOWN //less readable.
                && status != ProgramStatus.EXECUTING_TEST)
            {
                throw new IllegalStateException("Mission [" + getProgramName() 
                    + "] attempted an illegal state change from executed to " + status.toString() 
                    + ". Status remains executed.");
            }
        }
        if (m_ProgramStatus == ProgramStatus.WAITING_INITIALIZED && status == ProgramStatus.UNSATISFIED)
        {
            throw new IllegalStateException("Mission [" + getProgramName() 
                + "] attempted an illegal state change from waiting to unsatisfied. " 
                + "Status remains waiting initialized.");
        }
        if (status == ProgramStatus.SHUTTING_DOWN && m_ProgramStatus != ProgramStatus.EXECUTED)
        {
            throw new IllegalStateException(String.format("Mission [%s]" 
                + " attempted an illegal state change from [%s] to shutting down. " 
                + "Status remains %s.", getProgramName(), status.toString(), status.toString()));
        }
        if (status == ProgramStatus.SHUTDOWN && m_ProgramStatus != ProgramStatus.SHUTTING_DOWN)
        {
            throw new IllegalStateException("Mission [" + getProgramName() 
                + "] attempted an illegal state change from execute to shutting down. " 
                + "Status remains executing.");
        }
        Logging.log(LogService.LOG_DEBUG, "Program [" + getProgramName() + "] changed state from " 
            + m_ProgramStatus.toString() + " to " + status.toString());
        m_ProgramStatus = status;
        
        //post event
        m_EventAdmin.postEvent(new Event(TOPIC_PROGRAM_STATUS_CHANGED, getEventProperties()));
    }

    /**
     * Check that the program is ready to execute. This includes executing the test function.
     * @return
     *     true if the program is ready to execute.
     * @throws IllegalStateException
     *     if the program's dependencies are not fulfilled when the request was made
     */
    private boolean isExecuteReady() throws IllegalStateException
    {
        if (!getScheduleFlag(ScheduleEnum.IS_ACTIVE))
        {
            //inactive programs are programs, but are not allowed to execute until active
            throw new IllegalStateException("Inactive mission: " + getProgramName() 
                + " attempted to execute."); 
        }
        //programs that are not executed from the MPManager will not have their deps evaluated until called to do so
        if (getProgramStatus() == ProgramStatus.UNSATISFIED)
        {
            reconcileDependencies();
        }
        //if the script is being manually executed and has not executed previously the script needs to be initialized
        if (getProgramStatus() == ProgramStatus.WAITING_UNINITIALIZED)
        {
            m_Manager.scriptInitialization(this);
        }
        if (getProgramStatus() == ProgramStatus.WAITING_INITIALIZED 
                || getProgramStatus() == ProgramStatus.EXECUTED 
                || getProgramStatus() == ProgramStatus.SCHEDULED)
        {
            return true;
        }
        else
        {
            throw new IllegalStateException(String.format("Unable to execute/test [%s] because dependencies are " 
                    + "unsatisfied or there is an error in the program script. The status is of the program is: %s", 
                    getProgramName(), getProgramStatus()));
        }
    }

    /**
     * Check that the program is ready to test execute.
     * @throws IllegalStateException
     *     if the program's dependencies are not fulfilled when the request was made
     */
    private void isTestExecuteReady() throws IllegalStateException
    {
        //check that the template supports a test function
        if (!m_MissionTemplate.isSupportTestExecution())
        {
            throw new UnsupportedOperationException(String.format(
                "The mission [%s] does not have a test function!", getProgramName()));
        }
        
        if (getProgramStatus() == ProgramStatus.SCHEDULED)
        {
            //if scheduled than the mission is execute ready no need to check
            return;
        }
        else
        {
            isExecuteReady();
        }
    }

    /**
     * Check that the mission program is able to be removed.
     * @throws IllegalStateException
     *    thrown in the event that the program is not in a state that permits removal
     */
    private void checkReadyForRemoval() throws IllegalStateException
    {
        if (getProgramStatus() == ProgramStatus.EXECUTING
                || getProgramStatus() == ProgramStatus.EXECUTED
                || getProgramStatus() == ProgramStatus.SHUTTING_DOWN
                || getProgramStatus() == ProgramStatus.SCHEDULED
                || getProgramStatus() == ProgramStatus.EXECUTING_TEST)
        {
            throw new IllegalStateException(String.format("Mission program [%s] is %s, therefore cannot be removed.", 
                    getProgramName(), getProgramStatus()));
        }
    }

    /**
     * Set this program's mission script. This is created by implementing the {@link MissionScript} methods in the
     * program's source.
     * @param mission
     *    the mission script object
     */
    public void setMissionScript(final MissionScript mission)
    {
        Preconditions.checkNotNull(mission, "Mission script cannot be null for program [%s]", getProgramName());
        m_MissionScript = mission;
    }

    /**
     * Get this program's mission script. This is created by implementing the {@link MissionScript} methods in the
     * program's source.
     * @return
     *    the mission script object
     */
    public MissionScript getMissionScript()
    {
        return m_MissionScript;
    }
    
    /**
     * Set the test result of this program. Used when the test execution is requested to happen as a non-blocking call.
     * @param result
     *     the result of executing this program's test function
     */
    public void setTestResults(final TestResult result)
    {
        m_LastTestResult = result;
    }
    
    /**
     * Set the test exception for this program.
     * @param exceptionMessage
     *     the exception message if an exception occurred during test execution of this program
     */
    public void setTestExceptionMessage(final String exceptionMessage)
    {
        m_LastTestException = exceptionMessage;
    }
    
    /**
     * Set the exception for this program.
     * @param exceptionMessage
     *     the exception message if an exception occurred during test execution of this program
     */
    public void setExecutionExceptionMessage(final String exceptionMessage)
    {
        m_LastExecutionException = exceptionMessage;
    }
}
