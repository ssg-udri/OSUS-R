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
package mil.dod.th.ose.shell;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.PrintStream;

import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.factory.FactoryDescriptor;
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
import mil.dod.th.ose.test.LoggingServiceMocker;

import org.apache.felix.service.command.CommandSession;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.MetaTypeService;

/**
 * @author dlandoll
 */
public class TestThoseCommands
{
    private ThoseCommands m_SUT;
    private BundleContext m_Context;
    private AssetDirectoryService m_AssetDirectoryService;
    private CustomCommsService m_CustomCommsService;
    private ManagedExecutors m_ManagedExecutors;
    private AddressManagerService m_AddressManagerService;
    private MissionProgramManager m_MissionProgramManager;
    private ObservationStore m_ObservationStore;
    private PersistentDataStore m_PersistentDataStore;
    private PowerManager m_PowerManager;
    private TerraHarvestSystem m_TerraHarvestSystem;
    private EventHandlerHelper m_EventHandlerHelper;
    private DevicePowerManager m_DevicePowerManager;
    private ConfigurationAdmin m_ConfigurationAdmin;
    private MetaTypeService m_MetaTypeService;
    private EventAdmin m_EventAdmin;
    private XmlMarshalService m_XMLMarshalService;
    private XmlUnmarshalService m_XMLUnmarshalService;
    private TemplateProgramManager m_TemplateProgramManager;
    private LoggingService m_Logging;
    
    @Before
    public void setUp() throws Exception
    {
        m_Context = mock(BundleContext.class);
 
        m_SUT = new ThoseCommands();

        m_SUT.activate(m_Context);
        m_Logging = LoggingServiceMocker.createMock();
        m_SUT.setLoggingService(m_Logging);
    }
    
    /**
     * Verify that the asset directory service command will retrieve the correct service.
     */
    @Test
    public void testAds()
    {
        m_AssetDirectoryService = mock(AssetDirectoryService.class);
        
        m_SUT.setAssetDirectoryService(m_AssetDirectoryService);
        
        assertThat("Command 'astDirSvc' returns the AssetDirectoryService", m_SUT.astDirSvc(), 
            is(m_AssetDirectoryService));
    }
    
    /**
     * Verify that the custom comms service command will retrieve the correct service.
     */
    @Test
    public void testCcs()
    {
        m_CustomCommsService = mock(CustomCommsService.class);
        
        m_SUT.setCustomCommsService(m_CustomCommsService);
        
        assertThat("Command 'cusComSvc' returns the CustomCommsService", m_SUT.cusComSvc(), is(m_CustomCommsService));
    }
    
    /**
     * Verify that the managed executor service command will retrieve the correct service.
     */
    @Test
    public void testMexecs()
    {
        m_ManagedExecutors = mock(ManagedExecutors.class);
        
        m_SUT.setManagedExecutors(m_ManagedExecutors);
        
        assertThat("Command 'mngExe' returns the ManagedExecutors service", m_SUT.mngExe(), is(m_ManagedExecutors));
    }
    
    /**
     * Verify that the address manager service command will retrieve the correct service.
     */
    @Test
    public void testAl()
    {
        m_AddressManagerService = mock(AddressManagerService.class);
        
        m_SUT.setAddressManagerService(m_AddressManagerService);
        
        assertThat("Command 'addMgrSvc' returns the AddressManagerService", m_SUT.addMgrSvc(), 
                is(m_AddressManagerService));
    }
    
    /**
     * Verify that the mission program manager command will retrieve the correct service.
     */
    @Test
    public void testMpm()
    {
        m_MissionProgramManager = mock(MissionProgramManager.class);
        
        m_SUT.setMissionProgramManager(m_MissionProgramManager);
        
        assertThat("Command 'misPrgMgr' returns the MissionProgramManager service", m_SUT.misPrgMgr(), 
                is(m_MissionProgramManager));
    }
    
    /**
     * Verify that the observation store command will retrieve the correct service.
     */
    @Test
    public void testOs()
    {
        m_ObservationStore = mock(ObservationStore.class);
        
        m_SUT.setObservationStore(m_ObservationStore);
        
        assertThat("Command 'obsStr' returns the ObservationStore service", m_SUT.obsStr(), is(m_ObservationStore));
    }
    
    /**
     * Verify that the persistent data store command will retrieve the correct service.
     */
    @Test
    public void testPds()
    {
        m_PersistentDataStore = mock(PersistentDataStore.class);
        
        m_SUT.setPersistentDataStore(m_PersistentDataStore);
        
        assertThat("Command 'prsDatStr' returns the PersistentDataStore service", m_SUT.prsDatStr(), 
                is(m_PersistentDataStore));
    }
    
    /**
     * Verify that the power manager command will retrieve the correct service.
     */
    @Test
    public void testPm()
    {
        m_PowerManager = mock(PowerManager.class);
        
        m_SUT.setPowerManager(m_PowerManager);
        
        assertThat("Command 'pwrMgr' returns the PowerManager service", m_SUT.pwrMgr(), is(m_PowerManager));
    }
    
    /**
     * Verify that the {@link TerraHarvestController} service is available as a command.
     */
    @Test
    public void testTerraHarvestController()
    {
        TerraHarvestController controller = mock(TerraHarvestController.class);
        
        m_SUT.setTerraHarvestController(controller);
        
        assertThat("Command 'terHrvCtl' returns the TerraHarvestController service", m_SUT.terHrvCtl(), is(controller));
    }
    
    /**
     * Verify that the terra harvest system command will retrieve the correct service.
     */
    @Test
    public void testTerraHarvestSystem()
    {
        m_TerraHarvestSystem = mock(TerraHarvestSystem.class);
        
        m_SUT.setTerraHarvestSystem(m_TerraHarvestSystem);
        
        assertThat("Command 'terHrvSys' returns the TerraHarvestSystem service", m_SUT.terHrvSys(), 
                is(m_TerraHarvestSystem));
    }
    
    /**
     * Verify that the event handler helper service command will retrieve the correct service.
     */
    @Test
    public void testEhh()
    {
        m_EventHandlerHelper = mock(EventHandlerHelper.class);
        
        m_SUT.setEventHandlerHelper(m_EventHandlerHelper);
        
        assertThat("Command 'evtHndHlp' returns the EventHandlerHelper service", m_SUT.evtHndHlp(), 
                is(m_EventHandlerHelper));
    }
    
    /**
     * Verify that the device power manager service command will retrieve the correct service.
     */
    @Test
    public void testDpm()
    {
        m_DevicePowerManager = mock(DevicePowerManager.class);
        
        m_SUT.setDevicePowerManager(m_DevicePowerManager);
        
        assertThat("Command 'devPwrMgr' returns the DevicePowerManager service", m_SUT.devPwrMgr(), 
                is(m_DevicePowerManager));
    }
    
    /**
     * Verify that the configuration admin service command will retrieve the correct service.
     */
    @Test
    public void testConfigAdmin()
    {
        m_ConfigurationAdmin = mock(ConfigurationAdmin.class);
        
        m_SUT.setConfigurationAdmin(m_ConfigurationAdmin);
        
        assertThat("Command 'configAdmin' returns the ConfigAdmin service", m_SUT.configAdmin(), 
            is(m_ConfigurationAdmin));
    }
    
    /**
     * Verify that the metatype service command will retrieve the correct service.
     */
    @Test
    public void testMetaType()
    {
        m_MetaTypeService = mock(MetaTypeService.class);
        
        m_SUT.setMetaTypeService(m_MetaTypeService);
        
        assertThat("Command 'metaType' returns the MetaTypeService service", m_SUT.metaType(), is(m_MetaTypeService));
    }
    
    /**
     * Verify that the event admin service command will retrieve the correct service.
     */
    @Test
    public void testEventAdmin()
    {
        m_EventAdmin = mock(EventAdmin.class);
        
        m_SUT.setEventAdmin(m_EventAdmin);
        
        assertThat("Command 'eventAdmin' returns the EventAdmin service", m_SUT.eventAdmin(), is(m_EventAdmin));
    }

    /**
     * Verify that the xml marshal service command will retrieve the correct service.
     */
    @Test
    public void testXMLMarshalService()
    {
        m_XMLMarshalService = mock(XmlMarshalService.class);
        
        m_SUT.setXmlMarshalService(m_XMLMarshalService);
        
        assertThat("Command 'xmlMrshlSvc' returns the XMLMarshalService service", m_SUT.xmlMrshlSvc(), 
            is(m_XMLMarshalService));
    }
    
    /**
     * Verify that the xml unmarshal service command will retrieve the correct service.
     */
    @Test
    public void testXMLUnmarshalService()
    {
        m_XMLUnmarshalService = mock(XmlUnmarshalService.class);
        
        m_SUT.setXmlUnmarshalService(m_XMLUnmarshalService);
        
        assertThat("Command 'xmlUnmrshlSvc' returns the XMLUnmarshalService service", m_SUT.xmlUnmrshlSvc(), 
            is(m_XMLUnmarshalService));
    }
    
    /**
     * Verify that the template program manager command will retrieve the correct service.
     */
    @Test
    public void testTemplateProgramManager()
    {
        m_TemplateProgramManager= mock(TemplateProgramManager.class);
        
        m_SUT.setTemplateProgramManager(m_TemplateProgramManager);
        
        assertThat("Command 'tmpltPrgMgr' returns the TemplateProgramManager service", m_SUT.tmpltPrgMgr(), 
            is(m_TemplateProgramManager));
    }
    
    /**
     * Verify that the validator command will retrieve the correct service.
     */
    @Test
    public void testValidator()
    {
        Validator validator = mock(Validator.class);
        
        m_SUT.setValidator(validator);
        
        assertThat("Command 'validator' returns the Validator service", m_SUT.validator(), is(validator));
    }
    
    /**
     * Verify that the 'factories' command will list factories known to the system.
     */
    @Test
    public void testFactories() throws InvalidSyntaxException
    {
        CommandSession testSession = mock(CommandSession.class);
        PrintStream testStream = mock(PrintStream.class);
        when(testSession.getConsole()).thenReturn(testStream);

        when(m_Context.getServiceReferences(FactoryDescriptor.class.getName(), null)).thenReturn(null);

        m_SUT.factories(testSession, false, false);
        verify(testStream, atLeastOnce()).println(anyString());
        m_SUT.factories(testSession, true, false);
        verify(testStream, atLeastOnce()).println(anyString());
        m_SUT.factories(testSession, false, true);
        verify(testStream, atLeastOnce()).println(anyString());
        m_SUT.factories(testSession, false, false);
        verify(testStream, atLeastOnce()).println(anyString());
    }
}
