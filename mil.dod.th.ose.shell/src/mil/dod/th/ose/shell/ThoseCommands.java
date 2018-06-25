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
package mil.dod.th.ose.shell; // NOPMD: ExcessivePublicCount and CouplingBetweenObjects, TD: split up class

import java.io.PrintStream;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayerFactory;
import mil.dod.th.core.ccomm.physical.PhysicalLinkFactory;
import mil.dod.th.core.ccomm.transport.TransportLayerFactory;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.pm.DevicePowerManager;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.core.xml.XmlMarshalService;
import mil.dod.th.core.xml.XmlUnmarshalService;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.log.LogService;
import org.osgi.service.metatype.MetaTypeService;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Parameter;

/**
 * Implements a command for the Felix Shell Service.
 * 
 * @author dlandoll
 */
@Component(provide = ThoseCommands.class, properties = { "osgi.command.scope=those",
        "osgi.command.function="
        + MissionProgramManager.ADDRESS_MANAGER_SERVICE     + "|"
        + MissionProgramManager.ASSET_DIRECTORY_SERVICE     + "|"
        + "configAdmin"                                     + "|"
        + MissionProgramManager.CUSTOM_COMMS_SERVICE        + "|"
        + MissionProgramManager.DEVICE_POWER_MANAGER        + "|"
        + "eventAdmin"                                      + "|"
        + MissionProgramManager.EVENT_HANDLER_HELPER        + "|"
        + "factories"                                       + "|"
        + MissionProgramManager.MANAGED_EXECUTORS           + "|"
        + "metaType"                                        + "|"
        + MissionProgramManager.MISSION_PROGRAM_MANAGER     + "|"
        + MissionProgramManager.OBSERVATION_STORE           + "|"
        + MissionProgramManager.PERSISTENT_DATA_STORE       + "|"
        + MissionProgramManager.POWER_MANAGER               + "|"
        + MissionProgramManager.TEMPLATE_PROGRAM_MANAGER    + "|"
        + MissionProgramManager.TERRA_HARVEST_CONTROLLER    + "|"
        + MissionProgramManager.TERRA_HARVEST_SYSTEM        + "|"
        + MissionProgramManager.VALIDATOR                   + "|"
        + MissionProgramManager.XML_MARSHAL_SERVICE         + "|"
        + MissionProgramManager.XML_UNMARSHAL_SERVICE 
        })
public class ThoseCommands //NOPMD: too many fields, TD: should split up class
{
    
    /**
     * Reference to the logging service.
     */
    private LoggingService m_LoggingService;
    
    /**
     * Reference to the address manager service.
     */
    private AddressManagerService m_AddressManagerService;
    
    /**
     * Reference to the asset directory service.
     */
    private AssetDirectoryService m_AssetDirectoryService;

    /**
     * Reference to the custom comms service.
     */
    private CustomCommsService m_CustomCommsService;

    /**
     * Reference to the mission program manager store service.
     */
    private MissionProgramManager m_MissionProgramManager;
    
    /**
     * Reference to the observation store service.
     */
    private ObservationStore m_ObservationStore;
    
    /**
     * Reference to the persistent data store service.
     */
    private PersistentDataStore m_PersistentDataStore;
    
    /**
     * Reference to the managed executors service.
     */
    private ManagedExecutors m_ManagedExecutors;
    
    /**
     * Reference to the configuration administration service.
     */
    private ConfigurationAdmin m_ConfigurationAdmin;
    
    /**
     * Reference to the metatype service.
     */
    private MetaTypeService m_MetaTypeService;
    
    /**
     * Reference to the event administration service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Reference to the power manager service.
     */
    private PowerManager m_PowerManager;
    
    /**
     * Service for getting information about the controller.
     */
    private TerraHarvestController m_TerraHarvestController;
    
    /**
     * Reference to the Terra Harvest system service.
     */
    private TerraHarvestSystem m_TerraHarvestSystem;
    
    /**
     * Reference to the event handler helper service.
     */
    private EventHandlerHelper m_EventHandlerHelper;
    
    /**
     * Reference to the device power manager service.
     */
    private DevicePowerManager m_DevicePowerManager;

    /**
     * Reference to the xml marshal service.
     */
    private XmlMarshalService m_XmlMarshalService;
    
    /**
     * Reference to the xml unmarshal service.
     */
    private XmlUnmarshalService m_XmlUnmarshalService; 
    
    /**
     * Reference to the template program manager service.
     */
    private TemplateProgramManager m_TemplateProgramManager; 
    
    /**
     * Bundle context for this bundle.
     */
    private BundleContext m_BundleContext;

    /**
     * Validator service available as a command.
     */
    private Validator m_Validator;

    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_LoggingService = logging;
    }
    
    /**
     * Sets the address manager service to use.
     * 
     * @param AddressManagerService
     *            Reference to the TH core address manager service.
     */
    @Reference
    public void setAddressManagerService(final AddressManagerService AddressManagerService)
    {
        m_AddressManagerService = AddressManagerService;
    }
    
    /**
     * Sets the asset directory service to use.
     * 
     * @param assetDirectoryService
     *            Reference to the TH core asset directory service.
     */
    @Reference
    public void setAssetDirectoryService(final AssetDirectoryService assetDirectoryService)
    {
        m_AssetDirectoryService = assetDirectoryService;
    }

    /**
     * Sets the custom comms service to use.
     * 
     * @param customCommsService
     *            Reference to the TH core custom comms service.
     */
    @Reference
    public void setCustomCommsService(final CustomCommsService customCommsService)
    {
        m_CustomCommsService = customCommsService;
    }
    
    /**
     * Sets the mission program manager service to use.
     * 
     * @param missionProgramManager
     *            Reference to the TH core mission program manager service.
     */
    @Reference
    public void setMissionProgramManager(final MissionProgramManager missionProgramManager)
    {
        m_MissionProgramManager = missionProgramManager;
    }
    
    /**
     * Sets the observation store service to use.
     * 
     * @param observationStore
     *            Reference to the TH core observation store service.
     */
    @Reference
    public void setObservationStore(final ObservationStore observationStore)
    {
        m_ObservationStore = observationStore;
    }
    
    /**
     * Sets the persistent data store service to use.
     * 
     * @param persistentDataStore
     *            Reference to the TH core persistent data store service.
     */
    @Reference
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
    }
    
    /**
     * Sets the managed executors service to use.
     * 
     * @param managedExecutors
     *            Reference to the TH core managed executors service.
     */
    @Reference
    public void setManagedExecutors(final ManagedExecutors managedExecutors)
    {
        m_ManagedExecutors = managedExecutors;
    }
    
    /**
     * Sets the configuration admin service to use.
     * 
     * @param configurationAdmin
     *            Reference to the OSGi configuration admin to be used.
     */
    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin)
    {
        m_ConfigurationAdmin = configurationAdmin;
    }
    
    /**
     * Sets the meta type service to use.
     * 
     * @param metaTypeService
     *            Reference to the OSGi meta type service to be used.
     */
    @Reference
    public void setMetaTypeService(final MetaTypeService metaTypeService)
    {
        m_MetaTypeService = metaTypeService;
    }
    
    /**
     * Sets the event admin service to use.
     * 
     * @param eventAdmin
     *            Reference to the OSGi event admin service to be used.
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Sets the power manager service to use.
     * 
     * @param powerManager
     *            Reference to the TH core power manager service to be used.
     */
    @Reference(optional = true, dynamic = true)
    public void setPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = powerManager;
    }

    /**
     * Unbinds the power manager service.
     * 
     * @param powerManager
     *         parameter not used, must match binding method signature
     */
    public void unsetPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = null; //NOPMD null is used to unbind the service.
    }
    
    /**
     * Bind the service.
     * 
     * @param terraHarvestController 
     *          Service to bind.
     */
    @Reference
    public void setTerraHarvestController(final TerraHarvestController terraHarvestController)
    {
        m_TerraHarvestController = terraHarvestController;
    }
    
    /**
     * Sets the Terra Harvest System service to use.
     * 
     * @param terraHarvestSystem
     *            Reference to the TH core Terra Harvest System service to be used.
     */
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
    }
    
    /**
     * Sets the event handler helper service to use.
     * 
     * @param eventHandlerHelper
     *            Reference to the TH core event handler helper service.
     */
    @Reference
    public void setEventHandlerHelper(final EventHandlerHelper eventHandlerHelper)
    {
        m_EventHandlerHelper = eventHandlerHelper;
    }
    
    /**
     * Sets the device power manager service to use.
     * 
     * @param devicePowerManager
     *            Reference to the TH core device power manager service.
     */
    @Reference(optional = true, dynamic = true)
    public void setDevicePowerManager(final DevicePowerManager devicePowerManager)
    {
        m_DevicePowerManager = devicePowerManager;
    }
    
    /**
     * Unbinds the device power manager service.
     * 
     * @param devicePowerManager
     *              parameter not used, must match binding method signature
     */
    public void unsetDevicePowerManager(final DevicePowerManager devicePowerManager)
    {
        m_DevicePowerManager = null; //NOPMD null is used to unbind the service.
    }
    
    /** 
     * Binds the xml marshal service to use as the command.
     * 
     * @param xmlMarshalService
     *      service to use for the command
     */
    @Reference
    public void setXmlMarshalService(final XmlMarshalService xmlMarshalService)
    {
        m_XmlMarshalService = xmlMarshalService;
    }
    
    /** 
     * Binds the xml unmarshal service to use as the command.
     * 
     * @param xmlUnmarshalService
     *      service to use for the command
     */
    @Reference
    public void setXmlUnmarshalService(final XmlUnmarshalService xmlUnmarshalService)
    {
        m_XmlUnmarshalService = xmlUnmarshalService;
    }
    
    /** 
     * Binds the template program manager service to use as the command.
     * 
     * @param templateProgramManager
     *      service to use for the command
     */
    @Reference
    public void setTemplateProgramManager(final TemplateProgramManager templateProgramManager)
    {
        m_TemplateProgramManager = templateProgramManager;
    }
    
    /** 
     * Binds the validator service to use as the command.
     * 
     * @param validator
     *      service to use for the command
     */
    @Reference
    public void setValidator(final Validator validator)
    {
        m_Validator = validator;
    }
    
    /**
     * Activate the component.
     * 
     * @param context
     *            Bundle context
     */
    @Activate
    public void activate(final BundleContext context)
    {
        m_BundleContext = context;
    }
    
    /**
     * Returns the TH core address manager service.
     * 
     * @return {@link AddressManagerService} reference
     */
    @Descriptor("Terra Harvest AddressManagerService")
    public AddressManagerService addMgrSvc()
    {
        return m_AddressManagerService;
    }

    /**
     * Returns the TH core asset directory service.
     * 
     * @return {@link AssetDirectoryService} reference
     */
    @Descriptor("Terra Harvest AssetDirectoryService")
    public AssetDirectoryService astDirSvc()
    {
        return m_AssetDirectoryService;
    }

    /**
     * Returns the TH core custom comms service.
     * 
     * @return {@link CustomCommsService} reference
     */
    @Descriptor("Terra Harvest CustomCommsService")
    public CustomCommsService cusComSvc()
    {
        return m_CustomCommsService;
    }
    
    /**
     * Returns the TH core mission program manager service.
     * 
     * @return {@link MissionProgramManager} reference
     */
    @Descriptor("Terra Harvest MissionProgramManager")
    public MissionProgramManager misPrgMgr()
    {
        return m_MissionProgramManager;
    }
    
    /**
     * Returns the TH core observation store service.
     * 
     * @return {@link ObservationStore} reference
     */
    @Descriptor("Terra Harvest ObservationStore")
    public ObservationStore obsStr()
    {
        return m_ObservationStore;
    }
    
    /**
     * Returns the TH core persistent data store service.
     * 
     * @return {@link PersistentDataStore} reference
     */
    @Descriptor("Terra Harvest PersistentDataStore")
    public PersistentDataStore prsDatStr()
    {
        return m_PersistentDataStore;
    }
    
    /**
     * Returns the TH core managed executors service.
     * 
     * @return {@link ManagedExecutors} reference
     */
    @Descriptor("Terra Harvest ManagedExecutors")
    public ManagedExecutors mngExe()
    {
        return m_ManagedExecutors;
    }
    
    /**
     * Returns the TH core power manager service.
     * 
     * @return {@link PowerManager} reference
     */
    @Descriptor("Terra Harvest PowerManager")
    public PowerManager pwrMgr()
    {
        if (m_PowerManager == null)
        {
            m_LoggingService.log(LogService.LOG_INFO, "The power manager service is currently unavailable!");
        }
        return m_PowerManager;
    }
    
    /**
     * Returns the {@link TerraHarvestController} service.
     * 
     * @return {@link TerraHarvestController} reference
     */
    @Descriptor("TerraHarvestController service")
    public TerraHarvestController terHrvCtl()
    {
        return m_TerraHarvestController;
    }
    
    /**
     * Returns the TH core Terra Harvest System service.
     * 
     * @return {@link TerraHarvestSystem} reference
     */
    @Descriptor("Terra Harvest TerraHarvestSystem")
    public TerraHarvestSystem terHrvSys()
    {
        return m_TerraHarvestSystem;
    }
    
    /**
     * Returns the TH core EventHandlerHelper service.
     * 
     * @return {@link EventAdmin} reference
     */
    @Descriptor("Terra Harvest EventHandlerHelper")
    public EventHandlerHelper evtHndHlp()
    {
        return m_EventHandlerHelper;
    }
    
    /**
     * Returns the TH core DevicePowerManager service.
     * 
     * @return {@link DevicePowerManager} reference
     */
    @Descriptor("Terra Harvest DevicePowerManager")
    public DevicePowerManager devPwrMgr()
    {
        if (m_DevicePowerManager == null)
        {
            m_LoggingService.log(LogService.LOG_INFO, "The device power manager service is currently unavailable!");
        }
        return m_DevicePowerManager;
    }
    
    /**
     * Returns the configuration admin service.
     * 
     * @return {@link ConfigurationAdmin} reference
     */
    public ConfigurationAdmin configAdmin()
    {
        return m_ConfigurationAdmin;
    }
    
    /**
     * Returns the meta type service.
     * 
     * @return {@link MetaTypeService} reference
     */
    public MetaTypeService metaType()
    {
        return m_MetaTypeService;
    }
    
    /**
     * Returns the event admin service.
     * 
     * @return {@link EventAdmin} reference
     */
    public EventAdmin eventAdmin()
    {
        return m_EventAdmin;
    }  
    
    /**
     * Returns the XML unmarshal service.
     * 
     * @return {@link XmlUnmarshalService} reference
     */
    @Descriptor("Terra Harvest XmlUnmarshalService")
    public XmlUnmarshalService xmlUnmrshlSvc()
    {
        return m_XmlUnmarshalService;
    }
    
    /**
     * Returns the template program manager service.
     * 
     * @return {@link TemplateProgramManager} reference
     */
    @Descriptor("Terra Harvest TemplateProgramManager")
    public TemplateProgramManager tmpltPrgMgr()
    {
        return m_TemplateProgramManager;
    }

    /**
     * Returns the XML marshal service.
     * 
     * @return {@link XmlMarshalService} reference
     */
    @Descriptor("Terra Harvest XmlMarshalService")
    public XmlMarshalService xmlMrshlSvc()
    {
        return m_XmlMarshalService;
    }
    
    /**
     * Returns the validator service.
     * 
     * @return
     *      validator service
     */
    @Descriptor("Terra Harvest Validator service")
    public Validator validator()
    {
        return m_Validator;
    }

    /**
     * Factory information command.
     * 
     * @param session
     *            provides access to the Gogo shell session
     * @param assets
     *            whether to show asset factories
     * @param ccomms
     *            whether to show custom comms factories
     */
    @Descriptor("Displays factories registered with the Terra Harvest Core")
    public void factories(
            final CommandSession session,
            @Descriptor("Flag used to display asset factories")
            @Parameter(names = { "-a", "--asset" }, presentValue = "true", absentValue = "false")
            final boolean assets,
            @Descriptor("Flag used to display custom comms factories")
            @Parameter(names = { "-c", "--ccomm" }, presentValue = "true", absentValue = "false")
            final boolean ccomms
    )
    {
        final PrintStream out = session.getConsole();

        if (assets)
        {
            out.println("Asset Factories:");
            Utilities.printServiceImplementations(m_BundleContext, out, AssetFactory.class, false, false);
        }

        if (ccomms)
        {
            out.println("PhysicalLink Factories:");
            Utilities.printServiceImplementations(m_BundleContext, out, PhysicalLinkFactory.class, false, false);
            out.println();

            out.println("LinkLayer Factories:");
            Utilities.printServiceImplementations(m_BundleContext, out, LinkLayerFactory.class, false, false);
            out.println();

            out.println("TransportLayer Factories:");
            Utilities.printServiceImplementations(m_BundleContext, out, TransportLayerFactory.class, false, false);
        }

        // If no flags are provided, display all factories
        if (!(assets | ccomms))
        {
            out.println("Factories:");
            Utilities.printServiceImplementations(m_BundleContext, out, null, false, true);
        }
    }
}
