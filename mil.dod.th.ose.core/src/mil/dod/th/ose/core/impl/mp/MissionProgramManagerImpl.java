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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.MissionScript;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.ScheduleEnum;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteSystemEncryption;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;
import mil.dod.th.ose.mp.runtime.MissionProgramRuntime;
import mil.dod.th.ose.utils.xml.XmlUtils;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;

/**
 * Service provider implementation for the {@link MissionProgramManager}.
 * 
 * @author dhumeniuk
 *
 */
@Component(immediate = true) // NOCHECKSTYLE
public class MissionProgramManagerImpl implements MissionProgramManager // NOPMD: NOCHECKSTYLE: class 
//fan out complexity, class pulls together all core API services so it has to reference many classes
// PMD: Too many fields, class pulls together all core API services so it has to reference many classes
{

    /**
     * Set of all managed programs. Not all managed programs are active, this needs to be checked before execution.
     */
    private final Set<ProgramImpl> m_Programs = Collections.synchronizedSet(new HashSet<ProgramImpl>());

    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * JavaScript engine bound to this component.
     */
    private ScriptEngine m_ScriptEngine;

    /**
     * Map of the basic bindings.  Each execution of script will contain these bindings.
     */
    private final Map<String, Object> m_BaseBindings = new HashMap<String, Object>();
    
    /**
     * Map of all core factory base types.  Key is the simple name of the class, the value is the class of the base 
     * type.
     */
    private final Map<String, Class<? extends FactoryObject>> m_BaseFactoryTypes = 
        new HashMap<String, Class<? extends FactoryObject>>();

    /**
     * Service for managing assets, bound so it is available through the scripts.
     */
    private AssetDirectoryService m_AssetDirectoryService;

    /**
     * Service for managing custom communication layers, bound so it is available through the scripts.
     */
    private CustomCommsService m_CustomCommsService;

    /**
     * Service for storing asset observations, bound so it is available through the scripts.
     */
    private ObservationStore m_ObservationStore;

    /**
     * Service for managing communication addresses, bound so it is available through the scripts.
     */
    private AddressManagerService m_AddressManagerService;

    /**
     * Service for storing programs.
     */
    private PersistentDataStore m_PersistentDataStore;

    /**
     * Service for posting events.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Service to make it easier to register event handlers.
     */
    private EventHandlerHelper m_EventHandlerHelper;
    
    /**
     * Service to managed all {@link java.util.concurrent.Executors} used by scripts.
     */
    private ManagedExecutors m_ManagedExecutors;
    
    /**
     * Service for getting information about the controller.
     */
    private TerraHarvestController m_TerraHarvestController;
    
    /**
     * Service for persisting name and id of systems.
     */
    private TerraHarvestSystem m_TerraHarvestSystem;

    /**
     * Service for marshalling XML documents.
     */
    private XmlMarshalService m_XMLMarshalService;
    
    /**
     * Service for unmarshalling XML documents.
     */
    private XmlUnmarshalService m_XMLUnmarshalService;
    
    /**
     * Service that validates mission programs, used before and after persistence of mission programs.
     */
    private Validator m_MissionValidator;
    
    /**
     * Service that manages {@link MissionProgramTemplate}s.
     */
    private TemplateProgramManager m_TemplateManager;

    /**
     * Mission execution service.
     */
    private MissionProgramScheduler m_Scheduler;
    
    /**
     * Event handler for mission program manager.
     */
    private MissionProgramManagerEventHandler m_MisPrgEvntHandler;

    private MissionProgramRuntime m_MissionProgramRuntime;

    /**
     * URL to bundle location of upgrade script.
     */
    private URL m_UpgradeSource;
    
    /**
     * Default constructor.
     */
    public MissionProgramManagerImpl()
    {
        m_BaseFactoryTypes.put(Asset.class.getSimpleName(), Asset.class);
        m_BaseFactoryTypes.put(PhysicalLink.class.getSimpleName(), PhysicalLink.class);
        m_BaseFactoryTypes.put(LinkLayer.class.getSimpleName(), LinkLayer.class);
        m_BaseFactoryTypes.put(TransportLayer.class.getSimpleName(), TransportLayer.class);
    }
    
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
     * Bind the JavaScript engine available.
     * 
     * @param scriptEngine
     *      JavaScript engine
     */
    @Reference(target = "(name=JavaScript)")
    public void setScriptEngine(final ScriptEngine scriptEngine)
    {
        m_ScriptEngine = scriptEngine;
    }
    
    /**
     * Bind the {@link AssetDirectoryService}.
     * 
     * @param assetDirectoryService
     *      service that is bound to the script engine
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }
    
    /**
     * Bind the {@link CustomCommsService}.
     * 
     * @param customCommsService
     *      service that is bound to the script engine
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CustomCommsService = customCommsService;
    }
    
    /**
     * Bind the {@link AddressManagerService} service.
     * 
     * @param service
     *      service that is bound to the script engine
     */
    @Reference
    public void setAddressManagerService(final AddressManagerService service)
    {
        m_AddressManagerService = service;
    }

    /**
     * Bind the {@link DevicePowerManager} service.
     * 
     * @param devicePowerManager
     *      service that is bound to the script engine
     */
    @Reference(optional = true, dynamic = true)
    public void setDevicePowerManager(final DevicePowerManager devicePowerManager)
    {
        bindService(DEVICE_POWER_MANAGER, devicePowerManager);
    }
    
    /**
     * Unbind the {@link DevicePowerManager} service.
     * 
     * @param devicePowerManager
     *      service that is bound to the script engine
     */
    public void unsetDevicePowerManager(final DevicePowerManager devicePowerManager)
    {
        unbindService(devicePowerManager);
    }

    /**
     * Bind the {@link PowerManager} service.
     * 
     * @param powerManager
     *      service that is bound to the script engine
     */
    @Reference(optional = true, dynamic = true)
    public void setPowerManager(final PowerManager powerManager)
    {
        bindService(POWER_MANAGER, powerManager);
    }
    
    /**
     * Unbind the {@link PowerManager} service.
     * 
     * @param powerManager
     *      service that is bound to the script engine
     */
    public void unsetPowerManager(final PowerManager powerManager)
    {
        unbindService(powerManager);
    }

    /**
     * Bind the {@link ObservationStore} service.
     * 
     * @param observationStore
     *      service that is bound to the script engine
     */
    @Reference
    public void setObservationStore(final ObservationStore observationStore)
    {
        m_ObservationStore = observationStore;
    }
    
    /**
     * Bind the persistent data store.
     * 
     * @param persistentDataStore
     *      used to store the set of mission programs
     */
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
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
    
    /**
     * Bind the EventHandlerHelper service.
     * 
     * @param eventHandlerHelper
     *      service for making it easier to register event handlers
     */
    @Reference
    public void setEventHandlerHelper(final EventHandlerHelper eventHandlerHelper)
    {
        m_EventHandlerHelper = eventHandlerHelper;
    }
    
    /**
     * Bind the ManagedExecutors service.
     * 
     * @param managedExecutors
     *      service for managing {@link java.util.concurrent.Executors} instances created by scripts
     */
    @Reference
    public void setManagedExecutors(final ManagedExecutors managedExecutors)
    {
        m_ManagedExecutors = managedExecutors;
    }
    
    /**
     * Bind the TerraHarvestController service.
     * 
     * @param terraHarvestController
     *     service to bind
     */
    @Reference
    public void setTerraHarvestController(final TerraHarvestController terraHarvestController)
    {
        m_TerraHarvestController = terraHarvestController;
    }
    
    /**
     * Bind the TerraHarvestSystem service.
     * 
     * @param terraHarvestSystem
     *     Service for saving and recalling system name and uuid. 
     */
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
    }
    
    /**
     * Bind the MissionProgramValidator service.
     * 
     * @param missionProgramValidator
     *     service needed to validate MissionProgramInstances.
     */
    @Reference
    public void setMissionProgramValidator(final Validator missionProgramValidator)
    {
        m_MissionValidator = missionProgramValidator;
    }
    
    /**
     * Binding for the {@link TemplateProgramManager}.
     * 
     * @param templateProgramManager
     *     template program manager service that holds all system known {@link MissionProgramTemplate}
     */
    @Reference
    public void setTemplateProgramManager(final TemplateProgramManager templateProgramManager)
    {
        m_TemplateManager = templateProgramManager;
    }
    
    /**
     * Bind the {@link MissionProgramScheduler}.
     * 
     * @param missionScheduler
     *     service to use
     */
    @Reference
    public void setMissionScheduler(final MissionProgramScheduler missionScheduler)
    {
        m_Scheduler = missionScheduler;
    }
    
    /**
     * Bind the {@link XmlMarshalService}.
     * 
     * @param xmlMarshalService
     *     service used to marshal xml documents.
     */
    @Reference
    public void setXMLMarshalService(final XmlMarshalService xmlMarshalService)
    {
        m_XMLMarshalService = xmlMarshalService;
    }
    
    /**
     * Bind the {@link XmlUnmarshalService}.
     * 
     * @param xmlUnmarshalService
     *     service used to unmarshal xml documents.
     */
    @Reference
    public void setXMLUnmarshalService(final XmlUnmarshalService xmlUnmarshalService)
    {
        m_XMLUnmarshalService = xmlUnmarshalService;
    }
    
    /**
     * Bind the {@link MessageFactory}.
     * 
     * @param messageFactory
     *     service used to create and send a given message to a desired system
     */
    @Reference (optional = true, dynamic = true)
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        bindService(MESSAGE_FACTORY, messageFactory);
    }
    
    /**
     * Unbind the service and make the variable no longer available.
     * 
     * @param messageFactory
     *      service being removed
     */
    public void unsetMessageFactory(final MessageFactory messageFactory)
    {
        unbindService(messageFactory);
    }
    
    /**
     * Bind the {@link RemoteChannelLookup}.
     * 
     * @param remoteChannelLookup
     *     service used to maintain list of channels that are used to send messages .
     */
    @Reference (optional = true, dynamic = true)
    public void setRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        bindService(REMOTE_CHANNEL_LOOKUKP, remoteChannelLookup);
    }
    
    /**
     * Unbind the service and make the variable no longer available.
     * 
     * @param remoteChannelLookup
     *      service being removed
     */
    public void unsetRemoteChannelLookup(final RemoteChannelLookup remoteChannelLookup)
    {
        unbindService(remoteChannelLookup);
    }
    
    /**
     * Bind the {@link RemoteSystemEncryption}.
     * 
     * @param remoteSystemEncryption
     *     service used to fetch the encryption level of a remote system
     */
    @Reference (optional = true, dynamic = true)
    public void setRemoteSystemEncryption(final RemoteSystemEncryption remoteSystemEncryption)
    {
        bindService(REMOTE_SYSTEM_ENCRYPTION, remoteSystemEncryption);
    }
    
    /**
     * Unbind the {@link RemoteSystemEncryption} service.
     * 
     * @param remoteSystemEncryption
     *      service that is bound to the script engine
     */
    public void unsetRemoteSystemEncryption(final RemoteSystemEncryption remoteSystemEncryption)
    {
        unbindService(remoteSystemEncryption);
    }
    

    /**
     * Bind the {@link MetaTypeService}.
     * 
     * @param metaTypeService
     *     service used to get meta info for configurations.
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaTypeService)
    {
        bindService(META_TYPE_SERVICE, metaTypeService);
    }
    
    /**
     * Bind the {@link Validator} service.
     * 
     * @param validator
     *      service used to validate JAXB objects against a schema
     */
    @Reference
    public void setValidator(final Validator validator)
    {
        bindService(VALIDATOR, validator);
    }
    
    @Reference
    public void setMissionProgramRuntime(final MissionProgramRuntime runtime)
    {
        m_MissionProgramRuntime = runtime;
    }
    
    /**
     * Bind the data service to the script engine.
     * @param jaxbProtoObjectConverter
     *      the service to be bound
     */
    @Reference
    public void setJaxbProtoObjectConverter(final JaxbProtoObjectConverter jaxbProtoObjectConverter)
    {
        bindService(JAXB_PROTO_OBJECT_CONVERTER, jaxbProtoObjectConverter);
    }
    
    /**
     * Bind a service to variable so it can be used from a program (JavaScript).
     * 
     * @param name
     *      name of the variable
     * @param service
     *      service to make available
     */
    private void bindService(final String name, final Object service)
    {
        m_BaseBindings.put(name, service);
        // script engine may not be available yet, will bind in activate method in this case
        if (m_ScriptEngine != null)
        {
            m_ScriptEngine.put(name, service);
        }
        Logging.log(LogService.LOG_DEBUG, 
                "Added the following binding to the Mission Program Manager context: %s", name);
    }

    /**
     * Unbind a service from the variable so it will no longer be available for use by the program (JavaScript).
     * 
     * @param service
     *      service to make unavailable
     */
    private void unbindService(final Object service)
    {
        for (String key : m_BaseBindings.keySet())
        {
            if (m_BaseBindings.get(key).equals(service))
            {
                m_BaseBindings.remove(key);
                m_ScriptEngine.put(key, null);
                m_Logging.debug("Removed the following binding from the Mission Program Manager context: %s", key);
                return;
            }
        }
    }
    
    /**
     * Activate this component by initializing the base bindings.
     * 
     * @param context
     *      context for the bundle containing this component  
     * @throws IllegalArgumentException 
     *     if a mission template or parameters can not be united correctly to create a program
     */
    @Activate
    public void activate(final BundleContext context) throws IllegalArgumentException
    {
        m_MisPrgEvntHandler = new MissionProgramManagerEventHandler();
        m_MisPrgEvntHandler.registerEvents(context);
        
        m_BaseBindings.put(BUNDLE_CONTEXT, context);
        m_BaseBindings.put(ASSET_DIRECTORY_SERVICE, m_AssetDirectoryService);
        m_BaseBindings.put(CUSTOM_COMMS_SERVICE,    m_CustomCommsService);
        m_BaseBindings.put(ADDRESS_MANAGER_SERVICE, m_AddressManagerService);
        m_BaseBindings.put(OBSERVATION_STORE,       m_ObservationStore);
        m_BaseBindings.put(PERSISTENT_DATA_STORE,   m_PersistentDataStore);
        m_BaseBindings.put(LOGGING_SERVICE,         m_Logging);
        m_BaseBindings.put(EVENT_HANDLER_HELPER,    m_EventHandlerHelper);
        m_BaseBindings.put(MANAGED_EXECUTORS,       m_ManagedExecutors);
        m_BaseBindings.put(MISSION_PROGRAM_MANAGER, this);
        m_BaseBindings.put(TERRA_HARVEST_CONTROLLER, m_TerraHarvestController);
        m_BaseBindings.put(TERRA_HARVEST_SYSTEM,    m_TerraHarvestSystem);
        m_BaseBindings.put(XML_MARSHAL_SERVICE,    m_XMLMarshalService);
        m_BaseBindings.put(XML_UNMARSHAL_SERVICE,    m_XMLUnmarshalService);
        m_BaseBindings.put(TEMPLATE_PROGRAM_MANAGER,    m_TemplateManager);
        
        // add all base bindings to the script engine (only used by execute())
        for (String key : m_BaseBindings.keySet())
        {
            m_ScriptEngine.put(key, m_BaseBindings.get(key));
        }
       
        // Load upgrade script
        final Bundle bundle = context.getBundle();
        final String upgradeScript = "nashornUpgrade.js";
        m_UpgradeSource = bundle.getEntry(upgradeScript);

        if (m_UpgradeSource == null)
        {            
            throw new IllegalStateException("Unable to find mission programming upgrade script, "
                    + "cannot activate MissionProgramManager.");            
        }
        
        // load saved programs
        final Collection<? extends PersistentData> query = m_PersistentDataStore.query(this.getClass());
        if (query.isEmpty())
        {
            // programs not persisted
            m_Logging.info("No mission programs found to restore");
        }
        else
        {
            //get all known template names
            final Set<String> templateNames = m_TemplateManager.getMissionTemplateNames();
            for (PersistentData dataEntity : query)
            {
                final byte[] byteData = (byte[])dataEntity.getEntity();
                //load the parameters                
                final MissionProgramParameters params = (MissionProgramParameters)(XmlUtils.fromXML(byteData, 
                    MissionProgramParameters.class));
                //check that parameters contain the expected variable types
                m_Logging.debug("Mission parameters retrieved from the data store%n%s", params);
                final String name = params.getTemplateName();
                if (templateNames.contains(name))
                {
                    final ProgramImpl program = new ProgramImpl(this, m_EventAdmin, m_TemplateManager.
                            getTemplate(params.getTemplateName()), params, dataEntity.getUUID());
                    program.setLoadedAfterReset(true);
                    //add program to set of managed programs
                    m_Programs.add(program);
                }
                else
                {
                    m_Logging.error("Template not found with name: %s", params.getTemplateName());
                }
            }
            m_Logging.info("Restored the following mission programs: %s",  getActiveProgramTemplateNames());
        }
          
        // try to execute each restart program as long as at least one ran as it could be a trigger for 
        // another program
        for (ProgramImpl program : m_Programs)
        {
            synchronized (program)
            {
                //check if the program is active and set to start at restart
                if (program.getScheduleFlag(ScheduleEnum.START_AT_RESTART) 
                        && program.getScheduleFlag(ScheduleEnum.IS_ACTIVE)
                        && program.getProgramStatus() == ProgramStatus.UNSATISFIED)
                {
                    m_Logging.debug("checking if program [%s] is ready to execute at activation", 
                            program.getProgramName());
                    
                    execReadyProgram(program);
                }
            }
        }
    }
    
    /**
     * Method for when the mission program manager deactivates.
     */
    @Deactivate
    public void deactivate()
    {
        m_MisPrgEvntHandler.unregisterEvents();
    }
    
    @Override
    public Program loadParameters(final MissionProgramParameters params) 
            throws IllegalArgumentException, PersistenceFailedException
    {
        //ensure that the params have the name of the template to use
        if (!params.isSetTemplateName())
        {
            throw new IllegalArgumentException("Parameters loaded do not have a valid mission template named");
        }
       
        //make sure the name does not overlap
        for (Program program : getPrograms())
        {
            if (program.getProgramName().equals(params.getProgramName()))
            {
                throw new IllegalArgumentException(String.format(
                    "The name [%s] overlaps with an already managed program!", program.getProgramName()));
            }
        }

        //get a uuid
        final UUID progUUID = UUID.randomUUID();
        //get the template from the template manager
        final MissionProgramTemplate template = m_TemplateManager.getTemplate(params.getTemplateName());
        //create a program with the template
        final ProgramImpl program = new ProgramImpl(this, m_EventAdmin, template, params, progUUID);
        //attempt to validate parameters and persist them
        persistParameters(params, progUUID);
        //if the program is scheduled for later execution the script should still be attempted to be initialized
        if (program.getScheduleFlag(ScheduleEnum.IS_ACTIVE))
        {
            //try to initialize the script now, need deps satisfied
            program.reconcileDependencies();
            if (program.getProgramStatus() == ProgramStatus.WAITING_UNINITIALIZED)
            {
                scriptInitialization(program);
            }
            else
            {
                m_Logging.debug("Dependencies are not satisfied for %s, script unable to be initialized. When "
                    + "dependencies are available, the initialization process will be tried again.", 
                        program.getProgramName());
            }
            //if the program is initialized and is set to start immediately, submit the program to the scheduler
            if (program.getProgramStatus() == ProgramStatus.WAITING_INITIALIZED)
            {
                if (program.getScheduleFlag(ScheduleEnum.START_IMMEDIATELY) //NOPMD if statements could be combined
                        || program.getMissionSchedule().isSetStartInterval()) //but would make the code less readable
                {
                    m_Scheduler.executeProgram(program);
                }
            }
        }
        //Add new program to the set of managed programs
        m_Programs.add(program);
        m_Logging.info("Added program [%s] to mission program manager", params.getProgramName());
        m_EventAdmin.postEvent(new Event(MissionProgramManager.TOPIC_PROGRAM_ADDED, program.getEventProperties()));
        return program;
    }

    @Override
    public Set<Program> getActiveProgramsUsingTemplate(final String name) throws IllegalArgumentException 
    {
        final Set<Program> programs = new HashSet<Program>();
        for (ProgramImpl program : m_Programs)
        {
            //if the name of the template matches the template used to create the program 
            if (program.getTemplateName().equals(name) && program.getScheduleFlag(ScheduleEnum.IS_ACTIVE))
            {
                programs.add(program);
            }
        }
        return programs;
    }

    @Override
    public Set<Program> getInactiveProgramsUsingTemplate(final String name) throws IllegalArgumentException 
    {
        final Set<Program> programs = new HashSet<Program>();
        for (ProgramImpl program : m_Programs)
        {
            //if the name of the template matches the template used to create the program 
            if (program.getTemplateName().equals(name) && !program.getScheduleFlag(ScheduleEnum.IS_ACTIVE))
            {
                programs.add(program);
            }
        }
        return programs;
    }

    @Override
    public Set<Program> getPrograms()
    {
        final Set<Program> programs = new HashSet<Program>();
        for (ProgramImpl program : m_Programs)
        {
            programs.add(program);
        }
        return programs;
    }

    @Override
    public Program getProgram(final String name) throws IllegalArgumentException
    {
        for (ProgramImpl program : m_Programs)
        {
            //if the name of the name matches the name given
            if (program.getProgramName() != null && program.getProgramName().equals(name))
            {
                return program;
            }
        }
        
        //program was not found
        throw new IllegalArgumentException(String.format("The name [%s] is not associated with a mission program "
            + "known!", name));
    }
    /**
     * Retrieves a set of template names that currently have active programs.
     * 
     * @return
     *     set of template names representing currently active program
     */
    public Set<String> getActiveProgramTemplateNames() 
    {
        final Set<String> names = new HashSet<String>();
        for (ProgramImpl program : m_Programs)
        {
            if (program.getScheduleFlag(ScheduleEnum.IS_ACTIVE))
            {
                names.add(program.getTemplateName());
            }
        }
        return names;
    }

    /**
     * Remove the program from the manager. This does not remove the template that the mission
     * was built off of.
     * 
     * @param program
     *     program to remove
     * @param progUuid
     *     Uuid of the program instance model
     * @throws IllegalArgumentException
     *     if the program is not being managed (already removed)
     * @throws IllegalStateException
     *     if the program is currently executing
     */
    public void removeProgram(final Program program, final UUID progUuid) throws IllegalArgumentException, 
            IllegalStateException
    {
        //can't remove a program that doesn't exist
        if (!m_Programs.remove(program))
        {
            throw new IllegalArgumentException(String.format("Mission program [%s] not managed.", 
                program.getProgramName()));
        }
        
        //Remove the mission program model from datastore
        m_PersistentDataStore.remove(progUuid);
        m_EventAdmin.postEvent(new Event(MissionProgramManager.TOPIC_PROGRAM_REMOVED, 
                ((ProgramImpl)program).getEventProperties()));
    }

    /**
     * Submit the given program for execution based on its schedule. May not have started by the time this method 
     * returns.
     * 
     * @param program
     *      program to execute
     * @throws IllegalArgumentException
     *    if the program is already known to this service
     */
    public void executeProgram(final ProgramImpl program) throws IllegalArgumentException
    {
        m_Scheduler.executeProgram(program);
    }

    /**
     * Test the given program. Invocation of this method will cause the given program to execute its 'test' function.
     * This function is implemented within the programs JavaScript source code.
     * @param program
     *      program to test
     * @return
     *      the results of the the execution of the test method
     */
    public Future<TestResult> testProgram(final ProgramImpl program) //NOPMD: use of 
                  // 'test'accurately describes the action being called on within the mission script object
    { 
        return m_Scheduler.testProgram(program);
    }
    
    /**
     * Cancel a program that is scheduled for future execution.
     * @param programName
     *      the name of the program to cancel
     * @return
     *      true if the program was cancelled, false otherwise
     * @throws IllegalArgumentException
     *      thrown if the given program is not contained within the queue of mission programs scheduled to execute
     */
    public boolean cancelProgram(final String programName) throws IllegalArgumentException
    { 
        return m_Scheduler.cancelScheduledProgram(programName);
    }

    
    /**
     * Stop the given program. Invocation of this method will cause the given program to execute its 
     * 'shutdown' function. This function is implemented within the program's JavaScript source code.
     * 
     * @param program
     *      program to stop
     */
    public void shutdownProgram(final ProgramImpl program)
    {       
        m_Scheduler.shutdownProgram(program);
    }
    
    /**
     * Initial evaluation of the script. This must be called immediately once a script's dependencies are satisfied, 
     * and before the script is called to be executed.
     * @param program
     *     the program to containing the script to initialize
     */
    public void scriptInitialization(final ProgramImpl program)
    {
        // thread context class loader is used by script engine to access the TH API, set it to the runtime's class 
        // loader
        Thread.currentThread().setContextClassLoader(m_MissionProgramRuntime.getClassLoader());

        try
        {
            // put base bindings together with arguments for all bindings
            final Bindings bindings = m_ScriptEngine.createBindings();
            bindings.putAll(m_BaseBindings);
            bindings.put("TestResult", new MissionScript.TestResultConversionClass());
            bindings.putAll(program.getExecParams());

            synchronized (m_ScriptEngine)
            {
                m_ScriptEngine.put(ScriptEngine.FILENAME, program.getProgramName());
                m_Logging.info("Starting to initialize [%s]", program.getProgramName());

                final ScriptContext context = new SimpleScriptContext();
                context.setBindings(bindings, ScriptContext.ENGINE_SCOPE);                

                try (InputStream is = m_UpgradeSource.openStream())
                {
                    m_ScriptEngine.eval(new InputStreamReader(is), context);
                }

                final Object result = m_ScriptEngine.eval(program.getSource(), context);
                final Invocable inv = (Invocable) m_ScriptEngine;
                final MissionScript scriptObj = inv.getInterface(result, MissionScript.class);
                program.setMissionScript(scriptObj);
            }

            m_Logging.info("Mission program [%s] initialized successfully. "
                    + "The program is waiting to execute.", program.getProgramName());
            program.changeStatus(ProgramStatus.WAITING_INITIALIZED);
        }
        catch (final ScriptException | IOException e)
        {
            program.changeStatus(ProgramStatus.INITIALIZATION_ERROR);
            m_Logging.error(e, "Mission program %s has failed initialization, this will prevent the "
                    + "script from ever executing. Check the script and reload the mission. This error "
                    + "does NOT mean that the dependencies could not be fulfilled.", program.getProgramName());
        }
    }

    /**
     * Execute the program if enabled and ready.
     * 
     * @param program
     *      program to execute at restart if ready
     * @return
     *      true if the given program executed, false otherwise
     */
    private boolean execReadyProgram(final ProgramImpl program)
    {
        //evaluate the status of the program
        if (program.getProgramStatus() == ProgramStatus.EXECUTED)
        {
            m_Logging.debug("The program [%s] has already executed, will not run", program.getProgramName());
            return false;
        }

        //check if deps are available
        if (program.getProgramStatus() == ProgramStatus.UNSATISFIED)
        {
            m_Logging.debug("checking if program [%s] dependencies are satisfied", program.getProgramName());
            program.reconcileDependencies();
        }

        //check if the program is initialized, as deps must be satisfied for this to take place
        if (program.getProgramStatus() == ProgramStatus.WAITING_UNINITIALIZED)
        {
            //do not initialize if program should not be executed after a restart
            if (!program.isLoadedAfterReset()
                  || (program.isLoadedAfterReset() && program.getScheduleFlag(ScheduleEnum.START_AT_RESTART)))
            {
                scriptInitialization(program);
            }
            else
            {
                m_Logging.debug("Program [%s] will not run after a restart", program.getProgramName());
            }
        }

        // if dependencies are satisfied execute the program
        if (program.getProgramStatus() == ProgramStatus.WAITING_INITIALIZED)
        {
            m_Logging.debug("deps satisfied, executing program [%s]", program.getProgramName());
            executeProgram(program);
            return true;
        }

        //There are no more possibilities that the program's status can be updated to a 
        //satisfactory state to allow execution right now. The program will be checked again as long as
        //there are no errors with the program.
        m_Logging.debug("Program [%s] could not be executed, state is [%s]", program.getProgramName(), 
                program.getProgramStatus());
        return false;
    }

   /**
    * These methods check if the specific factory object dependency (assets, custom comms) 
    * can be satisfied. Will return null if the dependency is not available. 
    * 
    * @param depValue
    *      dependency name
    * @return
    *      the value of the dependency
    */
    public Asset getAssetDep(final String depValue)
    {
        if (m_AssetDirectoryService.isAssetAvailable(depValue))
        {
            return m_AssetDirectoryService.getAssetByName(depValue);
        }
        return null;
    }
    
   /**
    * See {@link #getAssetDep}. Will return null if the dependency is not available. 
    * @param depValue
    *      dependency name
    * @return
    *      the value of the dependency
    */
    public PhysicalLink getPhysicalDep(final String depValue)
    {
        if (m_CustomCommsService.getPhysicalLinkNames().contains(depValue))
        {
            return m_CustomCommsService.requestPhysicalLink(depValue);
        }
        return null;
    }
    
   /**
    * See {@link #getAssetDep}. Will return null if the dependency is not available. 
    * @param depValue
    *      dependency name
    * @return
    *      the value of the dependency
    */
    public LinkLayer getLinkDep(final String depValue)
    {
        if (m_CustomCommsService.isLinkLayerCreated(depValue))
        {
            return m_CustomCommsService.getLinkLayer(depValue);
        }        
        return null;
    }
    
   /**
    * See {@link #getAssetDep}. Will return null if the dependency is not available. 
    * @param depValue
    *      dependency name
    * @return
    *      the value of the dependency
    */
    public TransportLayer getTransportDep(final String depValue)
    {
        if (m_CustomCommsService.isTransportLayerCreated(depValue))
        {
            return m_CustomCommsService.getTransportLayer(depValue);
        }
        return null;        
    }
     
    /**
     * See {@link #getAssetDep}. Will return null if the dependency is not available. 
     * @param depValue
     *      dependency name
     * @return
     *      the value of the dependency
     */
    public Program getProgramDep(final String depValue)
    {
        try
        {
            final Program prog = getProgram(depValue);
            if (prog.getProgramStatus() == ProgramStatus.EXECUTED)
            {
                return prog; 
            }
        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.debug("Mission %s was not found in the lookup", depValue);
        }
        return null;
    }
    
    /**
     * Persist the passed mission parameters to the {@link PersistentDataStore}. 
     * Each {@link MissionProgramParameters} is persisted using the UUID and the name of the mission template
     * they belong with as identifiers.
     * 
     * @param params
     *     the parameters to persist
     * @param paramUUID
     *     the uuid to store the parameters with       
     * @throws PersistenceFailedException
     *     if the parameters were unable to be persisted 
     * @throws IllegalArgumentException
     *     if the values are incorrectly passed to the data store. 
     */
    private synchronized void persistParameters(final MissionProgramParameters params, final UUID paramUUID) 
            throws IllegalArgumentException, PersistenceFailedException
    {
        m_Logging.debug("Trying to validate and persist new mission parameters for %s", params.getProgramName());
        try
        {
            m_MissionValidator.validate(params);
        }
        catch (final ValidationFailedException ex)
        {
            throw new IllegalArgumentException(ex);
        }
        final byte[] bArrayParam = XmlUtils.toXML(params, true);
        m_PersistentDataStore.persist(this.getClass(), paramUUID, params.getTemplateName(), bArrayParam);
        m_Logging.info("Added mission parameters to data store for template: ", params.getTemplateName());
    }
    
    /**
     * Class used to listen for mission program manager events. 
     * @author nickmarcucci
     *
     */
    public class MissionProgramManagerEventHandler implements EventHandler
    {
        /**
         * Service registration object for this event handler class.
         */
        @SuppressWarnings("rawtypes")
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers all program events to listen for.
         * @param context
         *  the bundle context to use to register for events
         */
        public void registerEvents(final BundleContext context)
        {
            final String[] topics = new String[] {FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED, 
                FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED, Program.TOPIC_PROGRAM_EXECUTED};
            
            final Dictionary<String, Object> props = new Hashtable<>();
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        /**
         * Method used to unregister events.
         */
        public void unregisterEvents()
        {
            m_Registration.unregister();
        }
        
        @Override //NOCHECKSTYLE : Cyclomatic complexity is high need to handle update events
        public void handleEvent(final Event event)
        {
            if (event.getTopic().equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_CREATED) 
                || event.getTopic().equals(FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED))
            { 
                final String objectName = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_NAME);
                final String baseTypeName = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE);
                m_Logging.debug(
                      "factory object created or name updated, checking for ready programs, "
                      + "object name: %s, base type: %s",
                      objectName, baseTypeName);
                final Class<? extends FactoryObject> baseType = m_BaseFactoryTypes.get(baseTypeName);
                if (baseType == null)
                {
                    // ignore unknown base types like Address
                    return;
                }
                
                // check to see if any programs are waiting on this factory object
                synchronized (m_Programs)
                {
                    for (ProgramImpl program : m_Programs)
                    {
                        synchronized (program)
                        {
                            if (program.getFactoryObjectDeps(baseType).contains(objectName)
                                  && program.getScheduleFlag(ScheduleEnum.IS_ACTIVE) 
                                  && program.getProgramStatus() == ProgramStatus.UNSATISFIED)
                            {
                                m_Logging.debug("checking if program [%s] is ready to execute as"
                                        + " it depends on newly ready factory object [%s]", 
                                        program.getProgramName(), objectName);
                                execReadyProgram(program);
                            }                
                        }
                    }
                }
            }
            else if (event.getTopic().equals(Program.TOPIC_PROGRAM_EXECUTED))
            {
                final String executedProgramName = (String)event.getProperty(Program.EVENT_PROP_PROGRAM_NAME);
                m_Logging.debug("program [%s] executed, checking for other ready programs", executedProgramName);
                synchronized (m_Programs)
                {
                    for (ProgramImpl program : m_Programs)
                    {
                        synchronized (program)
                        {
                            if (program.getProgramDeps().contains(executedProgramName) 
                                    && program.getScheduleFlag(ScheduleEnum.IS_ACTIVE)
                                    && program.getProgramStatus() == ProgramStatus.UNSATISFIED)
                            {
                                m_Logging.debug("checking if program [%s] is ready "
                                        + "to execute as it depends on recently" 
                                        + " executed program [%s]", program.getProgramName(), executedProgramName);
                                execReadyProgram(program);
                            }
                        }
                    }
                }
            }
        }
    }

}
