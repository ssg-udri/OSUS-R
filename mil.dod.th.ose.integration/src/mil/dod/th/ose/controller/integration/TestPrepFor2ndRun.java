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
package mil.dod.th.ose.controller.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.SetPositionCommand;
import mil.dod.th.core.ccomm.Address;
import mil.dod.th.core.ccomm.AddressManagerService;
import mil.dod.th.core.ccomm.CCommException;
import mil.dod.th.core.ccomm.CustomCommsService;
import mil.dod.th.core.ccomm.link.LinkLayer;
import mil.dod.th.core.ccomm.physical.PhysicalLink;
import mil.dod.th.core.ccomm.transport.TransportLayer;
import mil.dod.th.core.log.Logging;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.ose.controller.integration.api.EventHandlerSyncer;
import mil.dod.th.ose.controller.integration.core.TestPersistedMissionPrograms;
import mil.dod.th.ose.controller.integration.utils.AssetTestUtils;
import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;
import mil.dod.th.ose.test.matchers.JaxbUtil;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;

import example.asset.ExampleAsset;
import example.asset.ExampleAssetAttributes;
import example.ccomms.EchoTransport;
import example.ccomms.ExampleAddress;
import example.ccomms.ExampleLinkLayer;

/**
 * @author dhumeniuk
 *
 */
public class TestPrepFor2ndRun
{
    private MissionProgramManager m_MissionProgramManager;

    @Before
    public void setUp()
    {
        m_MissionProgramManager = IntegrationTestRunner.getService(MissionProgramManager.class);
    }
    
    /**
     * Verify that factory objects are persisted along with their appropriate previously assigned values.
     * Values are verified in during second run by {@link 
     * mil.dod.th.ose.controller.integration.core.TestPersistedAsset#testAssetRestored()}.
     */
    @Test
    public void testCreateFactoryObjects() throws Exception
    {
        AssetDirectoryService assetDirectoryService = IntegrationTestRunner.getService(AssetDirectoryService.class);

        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "persistObsAsset");
        
        AssetTestUtils.activateAsset(asset, 5);
        
        Map<String, Object> properties = asset.getProperties();
        properties.put(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME, "exampleA");
        asset.setProperties(properties);
        
        // verify later the asset is activated during 2nd run
        asset.setActivateOnStartUp(true);
        
        assertThat(asset.getConfig().activateOnStartup(), is(true));
        
        asset.setPluginOverridesPosition(false);
        
        assertThat(asset.getConfig().pluginOverridesPosition(), is(false));
        
        // Test execute for value within range
        // Syncer to listen event that data was merged for the position
        EventHandlerSyncer syncerPosistion = new EventHandlerSyncer(PersistentDataStore.TOPIC_DATA_MERGED);
        
        //location
        final Command setPos = new SetPositionCommand().withLocation(
            new Coordinates().
                withLongitude(
                    new LongitudeWgsDegrees().withValue(1)).
                withLatitude(
                    new LatitudeWgsDegrees().withValue(-2)));
        assertThat(setPos, is(notNullValue()));
        asset.executeCommand(setPos);
        syncerPosistion.waitForEvent(10);
    }
    
    /**
     * Create a physical link that will be check for in
     * {@link mil.dod.th.ose.controller.integration.core.TestPersistedCommLayers#testLayersRestoredCheckName()}.
     * 
     * This link will verify that physical links are recreated at restart with their previously
     * assigned name.
     */
    @Test
    public void createPhysicalLinkStandalone() throws Exception
    {
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        
        //standalone physical link
        PhysicalLink link = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        
        //name setting
        link.setName("testPersistedPhysLink");
    }
    
    /**
     * Create comms layers that will be checked for restore in the second run by {@link 
     * mil.dod.th.ose.controller.integration.core.TestPersistedCommLayers#testLayersRestored()}.
     */
    @Test
    public void createCommsLayers() throws Exception
    {
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        
        PhysicalLink physicalLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C, "saved-pl");
        LinkLayer linkLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), 
                physicalLink.getName());
        linkLayer.setName("testLLDep"); 

        physicalLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C, "saved-pl2");
        
        linkLayer = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), "saved-ll", 
                physicalLink.getName());

        customCommsService.createTransportLayer(EchoTransport.class.getName(), "saved-tl", linkLayer.getName());
    }
    
    /**
     * Create address that will be checked for restore in the second run by {@link 
     * mil.dod.th.ose.controller.integration.core.TestPersistedAddresses#testAddressesRestored()}.
     */
    @Test
    public void createAddresses() throws CCommException
    {
        AddressManagerService addressManagerService = IntegrationTestRunner.getService(AddressManagerService.class);
        
        addressManagerService.getOrCreateAddress("Example:1001");
        
        Map<String, Object> propsAddress2 = new HashMap<String, Object>();
        propsAddress2.put("a", 1002);
        addressManagerService.getOrCreateAddress(ExampleAddress.class.getName(), "Example Address 1002", propsAddress2);
        
        addressManagerService.getOrCreateAddress("Example:1003");
    }
    
    /**
     * This tests that a java primitive is un-marshalled to the proper type tested 
     * {@link mil.dod.th.ose.controller.integration.core.TestPersistedMissionPrograms#testPersistedMissionExecArgs()}.
     */
    @Test
    public void testCreateMissionProgram() throws IllegalArgumentException, PersistenceFailedException, 
        MissionProgramException, IOException
    {
        //This mission is created to test the ability to create programs with 
        //different types of auto execute arguments.                 
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(false);
        String name = TestPersistedMissionPrograms.TestPrimitivesService.class.getName();
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("variable-types-template").
            withParameters(new MapEntry("PrimitiveServiceRef", name), 
                new MapEntry("intA", 12), new MapEntry("intB", 90), 
                new MapEntry("longA", 12L), 
                new MapEntry("msg", "ALL DONE TESTING PRIMITIVES"), 
                new MapEntry("shortA", (short)3), 
                new MapEntry("doubleA", 19.36), 
                new MapEntry("booleanA", true)).
                withSchedule(schedule).withProgramName("Test_Diff_Auto_Args");
        
        Program program = m_MissionProgramManager.loadParameters(params);
        assertThat(m_MissionProgramManager.getActiveProgramsUsingTemplate("variable-types-template"), 
            hasItem(program)); 
    }
    
    /**
     * This block creates new mission programs that will be tested during the 2nd run by 
     * {@link TestPersistedMissionPrograms#testAssetDepAutoExec()},  
     * {@link TestPersistedMissionPrograms#testLinkLayerDepAutoExec()}, 
     * {@link TestPersistedMissionPrograms#testProgramDepAutoExec()}.
     */
    @Test
    public void createAutoExecPrograms() throws Exception
    {
        AssetDirectoryService assetDirectoryService = 
                IntegrationTestRunner.getService(AssetDirectoryService.class);
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        asset.setName("excuteAssetDep");
        
        // Asset dependency for the program, dependency will be added on restart to verify script will start on restart
        // only after asset is added
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(true).withAtReset(true).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters().withSchedule(schedule).
            withProgramName("auto-exec-asset-dep-test").
                withTemplateName("immediate-exec-template-asset").withParameters(new MapEntry(
                    "msg","auto-exec-asset-dep-test")).withParameters(new MapEntry("arg", new Integer(84)), 
                        new MapEntry("dep", "excuteAssetDep"));
        
        //This is asking the MissionProgramManager to add the program to the list of programs
        m_MissionProgramManager.loadParameters(params);
        
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        assertThat(customCommsService, is(notNullValue()));
        final PhysicalLink cLink = customCommsService.createPhysicalLink(PhysicalLinkTypeEnum.I_2_C);
        cLink.setName("programPhysicalLink");
        cLink.open();
        LinkLayer link = customCommsService.createLinkLayer(ExampleLinkLayer.class.getName(), "programPhysicalLink");
        link.setName("programLinkLayer");
        
        //Set LinkLayer dependency to a list of "link layer" dependencies.
        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withAtReset(true).withImmediately(true).withActive(true);
        params = new MissionProgramParameters().withParameters(new MapEntry("msg", "auto-exec-linklayer-dep-test"),
                new MapEntry("arg", new Integer(122)), new MapEntry("dep", link.getName())).
                withProgramName("auto-exec-linklayer-dep-test").
                withTemplateName("immediate-exec-template-linklayer").
                withSchedule(schedule);
        //This is asking the MissionProgramManager to add the program to the list of programs
        m_MissionProgramManager.loadParameters(params);
        
        //Set program dependency to "program" dependencies.
        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).withActive(true).
            withAtReset(true).withImmediately(true);
        params = new MissionProgramParameters().withParameters(new MapEntry("msg", "auto-exec-program-dep2-test"), 
            new MapEntry("arg", new Integer(2)), new MapEntry("program", "auto-exec-linklayer-dep-test")).
            withTemplateName("immediate-execute-withProgramDep").withProgramName("auto-exec-program-dep2-test").
            withSchedule(schedule);
        
        //This is asking the MissionProgramManager to add the program to the list of programs
        m_MissionProgramManager.loadParameters(params);

        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).withActive(true).
             withAtReset(true).withImmediately(true);
        params = new MissionProgramParameters().withParameters(new MapEntry("msg", "auto-exec-program-dep4-test"), 
             new MapEntry("arg", new Integer(4))).withTemplateName("immediate-execute-withoutdep").
             withSchedule(schedule).withProgramName("auto-exec-program-dep4-test");
        //This is asking the MissionProgramManager to add the program to the list of programs
        m_MissionProgramManager.loadParameters(params);
        
        //Set program dependency to "program" dependencies.
        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).withActive(true).
            withAtReset(true).withImmediately(true);
        params = new MissionProgramParameters().withParameters(new MapEntry("msg", "auto-exec-program-dep3-test"), 
            new MapEntry("arg", new Integer(3)), new MapEntry("program", "auto-exec-program-dep4-test")).
            withTemplateName("immediate-execute-withProgramDep").withSchedule(schedule).
            withProgramName("auto-exec-program-dep3-test");
        
        //This is asking the MissionProgramManager to add the program to the list of programs
        m_MissionProgramManager.loadParameters(params);
        
        //Set program dependency to "program" dependencies.
        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).withActive(true).
            withAtReset(true).withImmediately(true);
        params = new MissionProgramParameters().withParameters(new MapEntry("msg", "auto-exec-program-dep1-test"), 
            new MapEntry("arg", new Integer(1)), new MapEntry("program", "auto-exec-linklayer-dep-test")).
            withTemplateName("immediate-execute-withProgramDep").withSchedule(schedule).
                withProgramName("auto-exec-program-dep1-test");

        //This is asking the MissionProgramManager to add the program to the list of programs
        m_MissionProgramManager.loadParameters(params);
    }
    
    /**
     * Create some remote channels that will be tested on the 2nd run by {@link 
     * mil.dod.th.ose.controller.integration.remote.TestPersistedRemoteChannelLookup}.
     */
    @Test
    public void testCreateRemoteChannels()
    {
        RemoteChannelLookup remoteChannelLookup = IntegrationTestRunner.getService(RemoteChannelLookup.class);
        
        remoteChannelLookup.syncTransportChannel("test", "local", "remote", 500, true);
        remoteChannelLookup.syncTransportChannel("test", "local2", "remote2", 500);
        remoteChannelLookup.syncTransportChannel("test", "local3", "remote3", 500);
        remoteChannelLookup.syncTransportChannel("test2", "local-A", "remote-B", 600);
        remoteChannelLookup.syncTransportChannel("test3", "local-B", "remote-C", 700, false);
        RemoteChannel channelToRemove = remoteChannelLookup.syncTransportChannel("to remove", "local", "remote", 500);
        
        // at least one that changes id, verify on 2nd run still moved
        remoteChannelLookup.syncTransportChannel("test", "local2", "remote2", 600);
        
        // at least one that is removed, verify on 2nd run still gone
        remoteChannelLookup.removeChannel(channelToRemove);
        
        // just a sanity check, verified elsewhere
        assertThat(remoteChannelLookup.getChannels(500).size(), is(2));
        assertThat(remoteChannelLookup.getChannels(600).size(), is(2));
        
        // Verify that the channel that will not be persisted exists
        assertThat(remoteChannelLookup.getChannels(700).size(), is(1));
    }

    /**
     * Make sure that a completely filled out image observation can be persisted and retrieved and will still contain
     * all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */

    @Test
    public void testPersistImageObservations() throws Exception
    {        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createCompleteImageObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
       
    /**
     * Make sure that a completely filled out coordinates observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */
    @Test
    public void testPersistCoordinatesObservations() throws Exception
    {        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createCoordinatesObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out orientation observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */
    @Test
    public void testPersistOrientationObservations() throws Exception
    {        
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createOrientationObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out detection observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */   
    @Test
    public void testPersistDetectionObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        for (int i = 1; i <= 4; i++)
        {
            Observation obs = ObservationHelper.createDetectionObservation(i);
            observationStore.persist(obs);
        
            Observation retrievedObs = observationStore.find(obs.getUuid());
        
            JaxbUtil.assertEqualContent(retrievedObs, obs);
        }
    }
    
    /**
     * Make sure that a completely filled out status observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */
    @Test
    public void testPersistStatusObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createStatusObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out weather observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */
    @Test
    public void testPersistWeatherObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createWeatherObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out audio observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */ 
    @Test
    public void testPersistAudioObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createAudioObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out video observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */
    @Test
    public void testPersistVideoObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createVideoObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Persist a basic observation will check the contents in 
     * {@link mil.dod.th.ose.controller.integration.core.TestPersistedObservations#testObservationRestored()}
     */
    @Test
    public void testPersistBasicObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        
        Observation obs = ObservationHelper.createBasicObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out biological observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */   
    @Test
    public void testPersistBiologicalObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation obs = ObservationHelper.createBiologicalObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out Cbrne trigger observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */   
    @Test
    public void testPersistCbrneTriggerObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation obs = ObservationHelper.createCbrneTriggerObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out chemical observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */   
    @Test
    public void testPersistChemicalObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation obs = ObservationHelper.createChemicalObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    
    /**
     * Make sure that a completely filled out water quality observation can be persisted and retrieved and 
     * will still contain all the data.
     * 
     * If this test fails, this probably means a JDO file for one of the types in the Observation needs to be updated 
     * because a complex field is missing that needs to be declared as persisted and be in the default fetch group.
     */   
    @Test
    public void testPersistWaterQualityObservations() throws Exception
    {
        ObservationStore observationStore = IntegrationTestRunner.getService(ObservationStore.class);
        Observation obs = ObservationHelper.createWaterQualityObservation();
        observationStore.persist(obs);
        
        Observation retrievedObs = observationStore.find(obs.getUuid());
        
        JaxbUtil.assertEqualContent(retrievedObs, obs);
    }
    /**
     * Configure system for tests in {@link mil.dod.th.ose.controller.integration.config.TestXmlConfigurations2ndRun}.
     */
    @Test
    public void testFirstRunAndOverrideConfigurations() throws Exception
    {
        // Remove Assets to test first run setting
        String assetName1 = "xmlConfigAsset1";
        String assetName2 = "xmlConfigAsset2";
        
        AssetDirectoryService assetDirectoryService = IntegrationTestRunner.getService(AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        try
        {
            Asset testAsset1 = assetDirectoryService.getAssetByName(assetName1);
            AssetTestUtils.deactivateAsset(testAsset1);
            AssetTestUtils.tryDeleteAsset(testAsset1);
        }
        catch (IllegalArgumentException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Asset [%s] not found in lookup, no need to remove.", assetName1);
        }

        try
        {
            Asset testAsset2 = assetDirectoryService.getAssetByName(assetName2);    
            AssetTestUtils.tryDeleteAsset(testAsset2);
        }
        catch (IllegalArgumentException e)
        {
            Logging.log(LogService.LOG_WARNING, e, "Asset [%s] not found in lookup, no need to remove.", assetName2);
        }
                               
        // Remove Addresses to test FirstRun setting
        String address1desc = "Example:1";
        String address2desc = "Example:2";
        String address3desc = "Example:3";
        AddressManagerService addressManagerService = IntegrationTestRunner.getService(AddressManagerService.class);
        assertThat(addressManagerService, is(notNullValue()));
        if (addressManagerService.checkAddressAlreadyExists(address1desc))
        {
            Address addr1 = addressManagerService.getOrCreateAddress(address1desc);
            addr1.delete();
        }
        if (addressManagerService.checkAddressAlreadyExists(address2desc))
        {
            Address addr2 = addressManagerService.getOrCreateAddress(address2desc);       
            addr2.delete();
        }
        if (addressManagerService.checkAddressAlreadyExists(address3desc))
        {
            Address addr3 = addressManagerService.getOrCreateAddress(address3desc);       
            addr3.delete();
        }

        // Remove LinkLayer to test creation.
        String testLinkName = "xmlConfigLinkLayer";
        CustomCommsService customCommsService = IntegrationTestRunner.getService(CustomCommsService.class);
        LinkLayer testLinkLayer = customCommsService.getLinkLayer(testLinkName);
        assertThat(testLinkLayer, is(notNullValue()));
        testLinkLayer.deactivateLayer();
        testLinkLayer.delete();
        
        // Remove Transport layer to test FirstRun setting
        String testTransportLayerName = "xmlConfigTransportLayer";
        TransportLayer transportLayer = customCommsService.getTransportLayer(testTransportLayerName);
        transportLayer.delete();
        
        // Remove PhysicalLink to test IfMissing setting
        String testPhysicalLinkName = "xmlConfigPhysicalLink";
        customCommsService.deletePhysicalLink(testPhysicalLinkName);

        // Remove configuration to test OSGi non-factory configuration creation on second run
        String osgiPid = "example.metatype.XML.ExampleClass";
        ConfigurationAdmin configAdmin = IntegrationTestRunner.getService(ConfigurationAdmin.class);     
        Configuration osgiNonFactoryConfig = configAdmin.getConfiguration(osgiPid);
        osgiNonFactoryConfig.delete();
    }
    
    /**
     * Create asset needed by the ads-test program that will be checked by {@link 
     * TestPersistedMissionPrograms#testAssetDirectoryService()}.
     */
    @Test
    public void createAssetForMissionProgram() throws Exception
    {
        AssetDirectoryService assetDirectoryService = IntegrationTestRunner.getService(AssetDirectoryService.class);
        assetDirectoryService.createAsset(ExampleAsset.class.getName(), "adsTestAsset");
    }
}
