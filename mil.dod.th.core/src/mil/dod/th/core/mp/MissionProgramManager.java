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

import java.util.Set;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.persistence.PersistenceFailedException;


/**
 * Manages a set of programs written in JavaScript and allows them to be executed.  The manager does not associate
 * a unique name with each program. The programs managed are configurations of parameters stored within the 
 * MissionProgramManager and templates acquired from the {@link TemplateProgramManager}. When managing a program
 * that has transport dependencies, it is the mission's responsibility to release these resources after execution.
 * Failure to do so can lead to deadlock of resources.
 * 
 * Must be provided as an OSGi service by the core.
 * 
 * The core provides interfaces for the following components that can be accessed from mission programs using the
 * the variables below.
 * <ul>
 * <li>{@value ADDRESS_MANAGER_SERVICE} - {@link mil.dod.th.core.ccomm.AddressManagerService}
 * <li>{@value ASSET_DIRECTORY_SERVICE} - {@link mil.dod.th.core.asset.AssetDirectoryService}
 * <li>{@value BUNDLE_CONTEXT} - {@link org.osgi.framework.BundleContext} for the bundle that offers this service
 * <li>{@value CUSTOM_COMMS_SERVICE} - {@link mil.dod.th.core.ccomm.CustomCommsService}
 * <li>{@value DEVICE_POWER_MANAGER} - {@link mil.dod.th.core.pm.DevicePowerManager}
 * <li>{@value EVENT_HANDLER_HELPER} - {@link mil.dod.th.core.mp.EventHandlerHelper}
 * <li>{@value JAXB_PROTO_OBJECT_CONVERTER} - 
 * {@link mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter}
 * <li>{@value LOGGING_SERVICE} - {@link mil.dod.th.core.log.LoggingService}
 * <li>{@value MANAGED_EXECUTORS} - {@link ManagedExecutors}
 * <li>{@value MESSAGE_FACTORY} - {@link mil.dod.th.core.remote.messaging.MessageFactory}
 * <li>{@value META_TYPE_SERVICE} - {@link org.osgi.service.metatype.MetaTypeService}
 * <li>{@value MISSION_PROGRAM_MANAGER} - {@link MissionProgramManager}
 * <li>{@value OBSERVATION_STORE} - {@link mil.dod.th.core.persistence.ObservationStore}
 * <li>{@value PERSISTENT_DATA_STORE} - {@link mil.dod.th.core.persistence.PersistentDataStore}
 * <li>{@value POWER_MANAGER} - {@link mil.dod.th.core.pm.PowerManager}
 * <li>{@value REMOTE_CHANNEL_LOOKUKP} - {@link mil.dod.th.core.remote.RemoteChannelLookup}
 * <li>{@value REMOTE_SYSTEM_ENCRYPTION} - {@link mil.dod.th.core.remote.RemoteSystemEncryption}
 * <li>{@value TEMPLATE_PROGRAM_MANAGER} - {@link mil.dod.th.core.mp.TemplateProgramManager}
 * <li>{@value TERRA_HARVEST_CONTROLLER} - {@link mil.dod.th.core.controller.TerraHarvestController}
 * <li>{@value TERRA_HARVEST_SYSTEM} - {@link mil.dod.th.core.system.TerraHarvestSystem}
 * <li>{@value VALIDATOR} - {@link mil.dod.th.core.validator.Validator} 
 * <li>{@value XML_MARSHAL_SERVICE} - {@link mil.dod.th.core.xml.XmlMarshalService}
 * <li>{@value XML_UNMARSHAL_SERVICE} - {@link mil.dod.th.core.xml.XmlUnmarshalService}
 * </ul>
 *
 * NOTE: Persistence needs to be thread safe.
 * @see Program
 * 
 * @author dhumeniuk
 */
@ProviderType
@SuppressWarnings("javadoc")
public interface MissionProgramManager
{
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.ccomm.AddressManagerService}.
     */
    String ADDRESS_MANAGER_SERVICE  = "addMgrSvc";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.asset.AssetDirectoryService}.
     */
    String ASSET_DIRECTORY_SERVICE  = "astDirSvc";
    
    /**
     * Constant containing the shell command for the {@link org.osgi.framework.BundleContext}, the bundle that offers
     * this service.
     */
    String BUNDLE_CONTEXT           = "bndCnt";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.ccomm.CustomCommsService}.
     */
    String CUSTOM_COMMS_SERVICE     = "cusComSvc";

    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.pm.DevicePowerManager}.
     */
    String DEVICE_POWER_MANAGER     = "devPwrMgr";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.mp.EventHandlerHelper}.
     */
    String EVENT_HANDLER_HELPER     = "evtHndHlp";
        
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.log.LoggingService}.
     */
    String LOGGING_SERVICE          = "logSvc";    
    
    /**
     * Constant containing the shell command for the {@link ManagedExecutors}.
     */
    String MANAGED_EXECUTORS        = "mngExe";
    
    /**
     * Constant containing the shell command for the {@link org.osgi.service.metatype.MetaTypeService}.
     */
    String META_TYPE_SERVICE        = "metaType";

    /**
     * Constant containing the shell command for the {@link MissionProgramManager}.
     */
    String MISSION_PROGRAM_MANAGER  = "misPrgMgr";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.persistence.ObservationStore}.
     */
    String OBSERVATION_STORE        = "obsStr";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.persistence.PersistentDataStore}.
     */
    String PERSISTENT_DATA_STORE    = "prsDatStr";

    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.pm.PowerManager}.
     */
    String POWER_MANAGER            = "pwrMgr";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.remote.RemoteChannelLookup}.
     */
    String REMOTE_CHANNEL_LOOKUKP   = "rmtChnLkp"; 
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.system.TerraHarvestSystem}.
     */
    String TERRA_HARVEST_SYSTEM     = "terHrvSys";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.controller.TerraHarvestController}.
     */
    String TERRA_HARVEST_CONTROLLER = "terHrvCtl";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.xml.XmlMarshalService}.
     */
    String XML_MARSHAL_SERVICE     = "xmlMrshlSvc";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.xml.XmlUnmarshalService}.
     */
    String XML_UNMARSHAL_SERVICE   = "xmlUnmrshlSvc";
    
    /**
     * Constant containing the shell command for the 
     * {@link mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter}.
     */
    String JAXB_PROTO_OBJECT_CONVERTER = "jxbPrtObjCnvrtr";
    
    /**
     * Constant containing the shell command for the {@link mil.dod.th.core.remote.messaging.MessageFactory}.
     */
    String MESSAGE_FACTORY         = "msgFty";

    /**
     * Constant containing the shell command for the 
     * {@link mil.dod.th.core.mp.TemplateProgramManager}.
     */
    String TEMPLATE_PROGRAM_MANAGER = "tmpltPrgMgr";
    
    /**
     * Constant containing the shell command for 
     * the {@link mil.dod.th.core.remote.RemoteSystemEncryption} service. 
     * This service may not always be available.
     */
    String REMOTE_SYSTEM_ENCRYPTION = "rmtSysEnct";
    
    /**
     * Constant for the mission program variable to access the {@link mil.dod.th.core.validator.Validator} service.
     */
    String VALIDATOR = "validator";
    
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/mp/MissionProgramManager/";
    
    /** 
     * Topic used when a program has been added. Properties included in the event are:
     * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * </ul>
     * 
     */
    String TOPIC_PROGRAM_ADDED = TOPIC_PREFIX + "PROGRAM_ADDED";
    
    /** 
     * Topic used when a program has been removed. Properties included in the event are:
      * 
     * <ul>
     * <li>{@link Program#EVENT_PROP_PROGRAM_NAME}</li>
     * <li>{@link Program#EVENT_PROP_PROGRAM_STATUS}</li>
     * </ul>
     * 
     */
    String TOPIC_PROGRAM_REMOVED = TOPIC_PREFIX + "PROGRAM_REMOVED";
    
    /**
     * Unites a schedule and parameters to a template managed by {@link TemplateProgramManager}. 
     * 
     * @param params
     *     the parameters used during the execution of the program
     * @return
     *     the program to be managed
     * @throws IllegalArgumentException
     *     if the template named does not exist or the parameters or schedule are invalid
     * @throws PersistenceFailedException
     *     if the parameters fail validation or other persistence error is incurred while persisting the parameters    
     */
    Program loadParameters(MissionProgramParameters params) throws IllegalArgumentException, PersistenceFailedException;
    
    /**
     * Retrieves a set of all known active programs constructed from the named template.
     * 
     * @param name
     *      name of the template
     * @return
     *      set of programs that are using the specified template
     * @throws IllegalArgumentException
     *      if the template name is not a known template
     */
    Set<Program> getActiveProgramsUsingTemplate(String name) throws IllegalArgumentException;

    /**
     * Retrieves a set of all known inactive programs constructed from the named template.
     * 
     * @param name
     *      name of the template
     * @return
     *      set of programs that are using the specified template
     * @throws IllegalArgumentException
     *      if the template name is not a known template
     */
    Set<Program> getInactiveProgramsUsingTemplate(String name) throws IllegalArgumentException;

    /**
     * Retrieves a set of all known active and inactive programs.
     * 
     * @return
     *      set of all programs that are known to the system
     */
    Set<Program> getPrograms();
    
    /**
     * Retrieve a {@link Program} by name.
     * 
     * @param name
     *     the name specified in the {@link MissionProgramParameters}, or a concatenation of the name of the template
     *     and the program's UUID.
     * @return
     *     the program instance
     * @throws IllegalArgumentException
     *     thrown in the event that the name given does not correlate to a program instance
     */
    Program getProgram(String name) throws IllegalArgumentException;
    
}
