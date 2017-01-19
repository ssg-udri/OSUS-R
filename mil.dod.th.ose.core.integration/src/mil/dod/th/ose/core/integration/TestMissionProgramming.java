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
package mil.dod.th.ose.core.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

import org.junit.Test;
import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import example.asset.ExampleAsset;
import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPanTiltCommand;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.mp.EventHandlerHelper;
import mil.dod.th.core.mp.ManagedExecutors;
import mil.dod.th.core.mp.MissionProgramException;
import mil.dod.th.core.mp.MissionProgramManager;
import mil.dod.th.core.mp.Program;
import mil.dod.th.core.mp.MissionScript.TestResult;
import mil.dod.th.core.mp.Program.ProgramStatus;
import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.FactoryUtils;
import mil.dod.th.ose.integration.commons.MissionProgramUtils;
import mil.dod.th.ose.shared.MapTranslator;
import junit.framework.TestCase;

/**
 * Test execution of missions.
 * 
 * @author dhumeniuk
 *
 */
public class TestMissionProgramming extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    private TemplateProgramManager m_TemplateProgramManager;
    private MissionProgramManager m_MissionProgramManager;

    @Override
    public void setUp()
    {
        GeneralUtils.assertSetupHasRun(m_Context);
        
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleAsset.class, 5000);
        
        AssetUtils.deleteAllAssets(m_Context);
        
        MissionProgramUtils.removeAllPrograms(m_Context);
        
        m_MissionProgramManager = ServiceUtils.getService(m_Context, MissionProgramManager.class);
        m_TemplateProgramManager = ServiceUtils.getService(m_Context, TemplateProgramManager.class);
    }
    
    @Override
    public void tearDown() throws Exception
    {
        AssetUtils.deleteAllAssets(m_Context);
        
        MissionProgramUtils.removeAllPrograms(m_Context);
    }
    
    /**
     * Test that class from the core API can be access directly from a mission program. 
     */
    public void testCoreApi() throws Exception
    {
        //variable data
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(true).withActive(true);
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("core-api-template")
            .withSchedule(schedule)
            .withProgramName("core-api-test");

        EventHandlerSyncer executionSyncer = new EventHandlerSyncer(m_Context, 
                new String[] { Program.TOPIC_PROGRAM_EXECUTED, Program.TOPIC_PROGRAM_EXECUTED_FAILURE }, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "core-api-test"));
        
        //the test actually happens from within the script
        Program program = m_MissionProgramManager.loadParameters(params);
        
        executionSyncer.waitForEvent(10);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
    }
    
    /**
     * Test that multiple core services (see mission program bindings) can work from within a script. 
     */
    public void testCoreService() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName()); 
        asset.setName("coreServicesAsset");
   
        //variable data
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(true).withActive(true);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("msg", "test message from script");
        parameters.put("thName", "test name");
        parameters.put("assetName", "coreServicesAsset");
        
        List<MapEntry> mapParameters = MapTranslator.translateMap(parameters);
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("core-services-template")
            .withSchedule(schedule)
            .withParameters(mapParameters)
            .withProgramName("core-services-test");

        EventHandlerSyncer executionSyncer = new EventHandlerSyncer(m_Context, 
                new String[] { Program.TOPIC_PROGRAM_EXECUTED, Program.TOPIC_PROGRAM_EXECUTED_FAILURE }, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "core-services-test"));
        
        //the test actually happens from within the script
        Program program = m_MissionProgramManager.loadParameters(params);
        
        executionSyncer.waitForEvent(10);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
    }
    
    /**
     * This tests that a simple script can execute with a schedule.
     * Verify that the program executes.
     * Verify that execution is complete before shutdown is initialized.
     */
    @Test
    public void testSimpleScriptSchedule() throws IllegalArgumentException, MissionProgramException, IOException,
        PersistenceFailedException, InterruptedException
    {
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule(true, false, null, 
            System.currentTimeMillis() + 500L, null, System.currentTimeMillis() + 1000L);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 99)).withProgramName("TestSimpleTestSchedule");

        EventHandlerSyncer executionSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                        String.format("(%s=%s)", 
                                Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleTestSchedule"));
        EventHandlerSyncer shutdownSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_STATUS_CHANGED,
                        String.format("(&(%s=%s)(%s=%s))", 
                                Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleTestSchedule",
                                Program.EVENT_PROP_PROGRAM_STATUS, ProgramStatus.SHUTDOWN.toString()));

        Program program = m_MissionProgramManager.loadParameters(params);

        executionSyncer.waitForEvent(5);
        shutdownSyncer.waitForEvent(20);

        program.remove();
    }
    
    /**
     * Tests the ability for scripts to receive events from event admin service. 
     */
    @Test
    public void testAssetEvent() throws Exception
    {
        //syncer used to listen for executed event, mission's execute on a separate thread
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED,
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "ads-event-test"));
        
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());       
        
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(true);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("asset-event-template").
            withParameters(new MapEntry("assetUuid", asset.getUuid())).
                withProgramName("ads-event-test").withSchedule(schedule);
        
        Program program = m_MissionProgramManager.loadParameters(params);
        program.execute();
        //wait for execute event
        syncer.waitForEvent(5);
        
        EventHandlerSyncer persistedDataSyncer = 
                new EventHandlerSyncer(m_Context, PersistentDataStore.TOPIC_DATA_PERSISTED);
        Observation o = asset.captureData();
        persistedDataSyncer.waitForEvent(5);
        
        PersistentDataStore persistentDataStore = ServiceUtils.getService(m_Context, PersistentDataStore.class);
        PersistentData data = persistentDataStore.find(o.getUuid());
        assertThat((String)data.getEntity(), is("mil/dod/th/core/asset/Asset/DATA_CAPTURED"));
        
        EventHandlerHelper eventHandlerHelper = ServiceUtils.getService(m_Context, EventHandlerHelper.class);
        assertThat(eventHandlerHelper, is(notNullValue()));
        
        eventHandlerHelper.unregisterAllHandlers();
        
        syncer = new EventHandlerSyncer(m_Context, PersistentDataStore.TOPIC_DATA_PERSISTED);
        o = asset.captureData();
        syncer.waitForEvent(2, 0, 0); // wait for 2 seconds to verify event not posted
        
        assertThat(persistentDataStore.contains(o.getUuid()), is(false));
    }
    
    /**
     * Test that missions running on different threads will wait for the other to complete. 
     */
    @Test
    public void testMultipleThreads() throws Exception
    {
        final SemaphoreService service = new SemaphoreService();
        
        m_Context.registerService(SemaphoreService.class, service, null);
        String serviceName = SemaphoreService.class.getName();
        ServiceReference<?> serviceRef = m_Context.getServiceReference(serviceName);
        //verify that service works
        SemaphoreService serv = (SemaphoreService) m_Context.getService(serviceRef);
        //verify service is registered and returning what is expected
        assertThat(serv.getSemaphoreA().getClass().equals(Semaphore.class), is(true));
        
        EventHandlerSyncer executionSyncerA = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "threadA"));
        EventHandlerSyncer executionSyncerB = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "threadB"));
        
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
                withImmediately(false).withActive(true).withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("thread-a-template").
            withProgramName("threadA").
            withSchedule(schedule).withParameters(new MapEntry("semServiceRef", serviceName));
        Program programA = m_MissionProgramManager.loadParameters(params);
        programA.execute();

        schedule = new MissionProgramSchedule().withIndefiniteInterval(false).withImmediately(false).withActive(true).
            withAtReset(false);
        params = new MissionProgramParameters().withTemplateName("thread-b-template").withProgramName("threadB").
                withSchedule(schedule).withParameters(new MapEntry("semServiceRef", serviceName));
        Program programB = m_MissionProgramManager.loadParameters(params);
        programB.execute();
        
        executionSyncerA.waitForEvent(5);
        executionSyncerB.waitForEvent(5);
    }
    
    /**
     * Service for accessing semaphores from within the thread scripts.
     * @author callen
     *
     */
    public class SemaphoreService
    {
        private Semaphore semA = new Semaphore(0);
        private Semaphore semB = new Semaphore(0);
        public Semaphore getSemaphoreA()
        {
            return semA;
        }
        public Semaphore getSemaphoreB()
        {
            return semB;
        }
    }
    
    /**
     * Verify metadata enum list has all the enum values defined in {@link SensingModalityEnum}.
     */
    public void testTriggeredDataCapture_Metadata()
    {
        MissionProgramTemplate template = m_TemplateProgramManager.getTemplate("triggered-data-captured");
        List<String> missionModalityTypes = 
                Maps.uniqueIndex(template.getVariableMetaData(), new Function<MissionVariableMetaData, String>()
                {
        
                    @Override
                    public String apply(MissionVariableMetaData metaData)
                    {
                        return metaData.getName();
                    }
                }).get("modalityType").getOptionValues();
        
        List<String> enumValues = new ArrayList<>(
                Lists.transform(Arrays.asList(SensingModalityEnum.values()), new Function<SensingModalityEnum, String>()
                {
                    @Override
                    public String apply(SensingModalityEnum enumValue)
                    {
                        return enumValue.value();
                    }
                }));
        // should match except metadata should have Any value in addition, but not Unknown
        enumValues.add("Any");
        enumValues.remove(SensingModalityEnum.UNKNOWN.value());
        
        assertThat(new HashSet<>(missionModalityTypes), is(new HashSet<>(enumValues)));
    }
    
    /**
     * Test running of the triggered-data-capture template.  When the trigger asset produces a detection, the capture 
     * asset should capture data.
     */
    @Test
    public void testTriggeredDataCapture_Any() throws Exception
    {
        //create assets
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset triggerAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "triggerAsset");
        Asset captureAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "captureAsset");
         
        //schedule and params to use for execution of the template
        MissionProgramSchedule schedule = new MissionProgramSchedule()
            .withIndefiniteInterval(true)
            .withImmediately(true)
            .withActive(true);
        final MapEntry entrySubEvent = new MapEntry("subscribeEvent", "New");
        //assets created earlier
        final MapEntry entryTrigger = new MapEntry("triggerAsset", "triggerAsset");
        final MapEntry entryCapture = new MapEntry("captureAsset", "captureAsset"); 
        final MapEntry entryModType = new MapEntry("modalityType", "Any");
        final MapEntry entrySenId = new MapEntry("triggerSensorId", "n/a");
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                        String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "triggered-data-captured-any"));
        
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("triggered-data-captured")
            .withSchedule(schedule)
            .withParameters(entrySubEvent, entryTrigger, entryCapture, entryModType, entrySenId)
            .withProgramName("triggered-data-captured-any");
        
        //load the parameters
        Program program = m_MissionProgramManager.loadParameters(params);
        
        syncer.waitForEvent(10);
                
        // the mission should have executed, registers an event handler and captures data for each observation 
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        // set up to wait for an observation persisted from the capture asset
        syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, captureAsset.getUuid().toString()));
        
        // have trigger asset persist observation to trigger other asset to capture data
        triggerAsset.captureData();
        
        syncer.waitForEvent(5);
    }
        
    /**
     * Test running of the triggered-slew-data-capture template.  When the trigger asset produces a detection,
     *  the capture asset should capture data after the specified pan/tilt is done.
     */
    @Test
    public void testTriggeredSlewDataCapture() throws Exception
    {
        //create assets
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset triggerAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "triggerAsset");
        Asset captureAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "captureAsset");
         
        //schedule and params to use for execution of the template
        MissionProgramSchedule schedule = new MissionProgramSchedule()
            .withIndefiniteInterval(true)
            .withImmediately(true)
            .withActive(true);
        final MapEntry entrySubEvent = new MapEntry("subscribeEvent", "New");
        //assets created earlier
        final MapEntry entryTrigger = new MapEntry("triggerAsset", "triggerAsset");
        final MapEntry entryCapture = new MapEntry("captureAsset", "captureAsset"); 
        final MapEntry entryPan = new MapEntry("pan", 30.0f);
        final MapEntry entryTilt = new MapEntry("tilt", 30.0f);
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                        String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "triggered-slew-data-capture-any"));
        
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("triggered-slew-data-capture")
            .withSchedule(schedule)
            .withParameters(entrySubEvent, entryTrigger, entryCapture, entryPan, entryTilt)
            .withProgramName("triggered-slew-data-capture-any");
        
        //load the parameters
        Program program = m_MissionProgramManager.loadParameters(params);
        
        syncer.waitForEvent(5);
                
        // the mission should have executed, registers an event handler and captures data for each observation 
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        // set up to wait for an observation persisted from the capture asset
        syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, captureAsset.getUuid().toString()));
        
        // have trigger asset persist observation to trigger other asset to capture data
        triggerAsset.captureData();
        
        syncer.waitForEvent(5);
        //check if captureAsset captured data
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        Collection<Observation> observations = observationStore.newQuery()
                .withAsset(captureAsset)
                .withSubType(ObservationSubTypeEnum.DETECTION)
                .execute();
        assertThat(observations.size(), is(1));
        
        //since the capture asset has already panned and tilted during the onexecutecommand,
        //our aim is to get back that pan and tilt and verify that it really panned by that value
        //and tilted by that value.
        syncer = new EventHandlerSyncer(m_Context, Asset.TOPIC_COMMAND_RESPONSE,
            String.format("(%s=%s)", Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, 
                GetPanTiltResponse.class.getName()));
        final Command getPT = new GetPanTiltCommand();
        final GetPanTiltResponse getPTResponse = (GetPanTiltResponse)captureAsset.executeCommand(getPT);
        assertThat("PT Response is null",getPTResponse, is(notNullValue()));
        assertThat(getPTResponse.getPanTilt().getAzimuth().getValue(), is(30.0));
        assertThat(getPTResponse.getPanTilt().isSetElevation(), is(false));
    }
    
    /**
     * Test running of the triggered-data-capture-with-multiple-sensors template.  When the first trigger asset
     * produces a detection, if the second delta trigger asset produces a detection with a specified time interval
     * then the capture asset should capture data. if the second delta trigger asset produces a detection after
     * delta interval time, then the capture asset does not capture data. 
     */
    @Test
    public void testTriggeredDataCaptureWithMultipleSensors() throws IllegalArgumentException, 
        PersistenceFailedException, InterruptedException, AssetException, FactoryException, IOException
    {
        //create assets
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset triggerAssetA = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "triggerAssetA");
        Asset triggerAssetB = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "triggerAssetB");
        Asset captureAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "captureAsset");
         
        //schedule and params to use for execution of the template
        MissionProgramSchedule schedule = new MissionProgramSchedule()
            .withIndefiniteInterval(true)
            .withImmediately(true)
            .withActive(true);
        final MapEntry entry1 = new MapEntry("subscribeEvent", "Both");
        //assets created earlier
        final MapEntry entry2 = new MapEntry("triggerAssetA", "triggerAssetA");
        final MapEntry entry3 = new MapEntry("triggerAssetB", "triggerAssetB"); 
        final MapEntry entry4 = new MapEntry("captureAsset", "captureAsset"); 
        final MapEntry entry5 = new MapEntry("deltaTimeBetweenTriggers", 5L);
        final MapEntry entry6 = new MapEntry("standOffTime", 1L);
        final MapEntry entry7 = new MapEntry("timeUnitStr", "Second");
        final MapEntry entry8 = new MapEntry("scenarioEvent", "BothDirection");
        final MapEntry entry9 = new MapEntry("useObservedTime", false);
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                 String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, 
                 "triggered-data-capture-with-multiple-sensors-both"));
        
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("triggered-data-capture-with-multiple-sensors")
            .withSchedule(schedule)
            .withParameters(entry1, entry2, entry3, entry4, entry5, entry6, entry7, entry8, entry9)
            .withProgramName("triggered-data-capture-with-multiple-sensors-both");
        
        //load the parameters
        Program program = m_MissionProgramManager.loadParameters(params);
        
        syncer.waitForEvent(5);
                
        // the mission should have executed, registers an event handler and captures data for each observation 
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        // set up to wait for an observation persisted from the capture asset
        syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, captureAsset.getUuid().toString()));
        
        // TriggerA followed by TriggerB followed by TriggerA should in all result in two captures.,
        //the delta interval time is 5 secs, so the capture asset will be triggered.
        triggerAssetA.captureData();
        Thread.sleep(1000);
        triggerAssetB.captureData();
        syncer.waitForEvent(5);
        
        syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, captureAsset.getUuid().toString()));
        Thread.sleep(1000);
        triggerAssetA.captureData();
        syncer.waitForEvent(10);

        //check if captureAsset captured data
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        Collection<Observation> observations1 = observationStore.newQuery()
                .withAsset(captureAsset)
                .withSubType(ObservationSubTypeEnum.DETECTION)
                .execute();
        assertThat(observations1.size(), is(2));
        
        //make sure the mission is shutdown properly.
        EventHandlerSyncer shutdownSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_SHUTDOWN);
        
        program.executeShutdown();
        
        shutdownSyncer.waitForEvent(15);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.SHUTDOWN));
    }
    
    /**
     * Test running of the triggered-data-capture template, but with all modality types, not "Any".  When 
     * the trigger asset produces a detection, the capture asset should capture data.
     */
    @Test
    public void testTriggeredDataCapture_AllModalities() throws Exception
    {
        //create assets
        AssetDirectoryService assetDirectoryService = 
            ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset triggerAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "triggerAsset");
        Asset captureAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "captureAsset");
        
        for (SensingModalityEnum modality : SensingModalityEnum.values())
        {
            //schedule and params to use for execution of the template
            MissionProgramSchedule schedule = new MissionProgramSchedule()
                .withIndefiniteInterval(true)
                .withImmediately(true)
                .withActive(true);
            final MapEntry entrySubEvent = new MapEntry("subscribeEvent", "Both");  // just to be different from above
            //assets created earlier
            final MapEntry entryTrigger = new MapEntry("triggerAsset", "triggerAsset");
            final MapEntry entryCapture = new MapEntry("captureAsset", "captureAsset");
            // this time do a specific modality, results should be the same as that is what the asset produces
            final MapEntry entryModType = new MapEntry("modalityType", modality.value()); 
            final MapEntry entrySenId = new MapEntry("triggerSensorId", "n/a");
            
            String programName = "triggered-data-captured-" + modality.value();
            EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                        String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, programName));
            
            MissionProgramParameters params = new MissionProgramParameters()
                .withTemplateName("triggered-data-captured")
                .withSchedule(schedule)
                .withParameters(entrySubEvent, entryTrigger, entryCapture, entryModType, entrySenId)
                .withProgramName(programName);
            
            //load the parameters
            Program program = m_MissionProgramManager.loadParameters(params);
            
            syncer.waitForEvent(10);
                    
            // the mission should have executed, registers an event handler and captures data for each observation 
            assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
            
            // mission will only be triggered if trigger asset supports the modality
            if (triggerAsset.getFactory().getAssetCapabilities().getModalities().contains(modality))
            {
                // set up to wait for an observation persisted from the capture asset
                syncer = new EventHandlerSyncer(m_Context, ObservationStore.TOPIC_OBSERVATION_PERSISTED, 
                    String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, captureAsset.getUuid().toString()));
                
                // have trigger asset persist observation to trigger other asset to capture data
                triggerAsset.captureData();
                
                syncer.waitForEvent(5);
            }
        }
    }
    
    /**
     * Test running of the timed-data-capture-loop template.  Test that an asset will produce assets based on the 
     * interval.
     */
    @Test
    public void testTimedDataCaptureLoop() throws IllegalArgumentException, PersistenceFailedException, 
        InterruptedException, AssetException, FactoryException, IOException
    {
        AssetDirectoryService assetDirectoryService = 
                ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset loopAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "loopAsset");
        
        //schedule and params to use for execution of the template
        MissionProgramSchedule schedule = new MissionProgramSchedule()
            .withIndefiniteInterval(true)
            .withImmediately(true)
            .withActive(true);
        final MapEntry entryAsset = new MapEntry("asset", "loopAsset");
        final MapEntry entryInitDelay = new MapEntry("initTimeDelay", 0);
        final MapEntry entryDelayInt = new MapEntry("timeDelayInterval", 4); 
        final MapEntry entryTimeUnit = new MapEntry("timeUnitStr", "Second");
        
        EventHandlerSyncer executeMissionSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                        String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "timed-data-loop-test"));
        
        MissionProgramParameters params = new MissionProgramParameters()
            .withTemplateName("timed-data-capture-loop")
            .withSchedule(schedule)
            .withParameters(entryAsset, entryInitDelay, entryDelayInt, entryTimeUnit)
            .withProgramName("timed-data-loop-test");
        
        //load the parameters
        Program program = m_MissionProgramManager.loadParameters(params);
        
        executeMissionSyncer.waitForEvent(5);
        
        // the mission should execute immediately, it only sets up to run on a scheduled interval
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));
        
        // allow 2 observations to be made
        Thread.sleep(6000);
        
        // stop executor used by mission        
        ManagedExecutors managedExecutors = ServiceUtils.getService(m_Context, ManagedExecutors.class);
        managedExecutors.shutdownAllExecutorServices();
        
        // allow current iteration to finish
        Thread.sleep(1500);
        
        // should have capture 1 every 4 seconds, for a total of 2
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        Collection<Observation> observations = observationStore.newQuery()
                .withAsset(loopAsset)
                .withSubType(ObservationSubTypeEnum.DETECTION)
                .execute();
        assertThat(observations.size(), is(2));
    }

    /**
     * Test running of the timed-observation-removal-loop template.  Test will periodically remove all observations.
     */
    @Test
    public void testTimedObservationRemovalLoop() throws IllegalArgumentException, PersistenceFailedException, 
        InterruptedException, AssetException, FactoryException, IOException
    {
        // Create asset
        AssetDirectoryService assetDirectoryService = 
            ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        Asset testAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "testDataCapturer");
        
        // Capture observations
        testAsset.captureData();
        testAsset.captureData();
        
        // Check that the observations do exist
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        Collection<Observation> observations = observationStore.newQuery()
                .withAsset(testAsset)
                .withSubType(ObservationSubTypeEnum.DETECTION)
                .execute();
        assertThat(observations.size(), is(2));
        
        // Schedule and parameters to use for execution of the removal template
        MissionProgramSchedule removeLoopSchedule = new MissionProgramSchedule()
            .withIndefiniteInterval(true)
            .withImmediately(true)
            .withActive(true);
        final MapEntry param1 = new MapEntry("initTimeDelay", 0);
        final MapEntry param2 = new MapEntry("timeDelayInterval", 1);
        final MapEntry param3 = new MapEntry("removeTime", 0);
        final MapEntry param4 = new MapEntry("timeUnitStr", "Second");
        
        // Sync the event handler to the Executed state of the mission
        EventHandlerSyncer executeMissionSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                      String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "timed-observation-removal-loop-test"));
        
        // Mission parameters are filled with values set above
        MissionProgramParameters parameters = new MissionProgramParameters()
            .withTemplateName("timed-observation-removal-loop")
            .withSchedule(removeLoopSchedule)
            .withParameters(param1, param2, param3, param4)
            .withProgramName("timed-observation-removal-loop-test");   
    
        // Load the mission
        Program program = m_MissionProgramManager.loadParameters(parameters);
        
        // Wait up to 5 seconds for the Executed state of the mission to occur
        executeMissionSyncer.waitForEvent(5);
                
        // After waiting, make sure the mission status is Executed
        assertThat(program.getProgramStatus(), is(ProgramStatus.EXECUTED));  
        
        // Since the initial time delay is set to 0, the mission will immediately remove the observations.
        // This sleep ensures that there is time to actually delete the observations before it checks that they 
        // have been deleted.
        Thread.sleep(500);
        
        // Check that observations have been deleted
        observations = observationStore.queryByAsset(testAsset);
        assertThat(observations.size(), is(0));
        
        // Create more observations
        testAsset.captureData();
        testAsset.captureData();
        
        // Sleep long enough for the next removal to occur
        Thread.sleep(2000);
       
        // Check that observations have been deleted again
        observations = observationStore.queryByAsset(testAsset);
        assertThat(observations.size(), is(0));
        
        // Sync the event handler to the Shutdown state of the mission
        executeMissionSyncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_SHUTDOWN, 
                "(name=timed-observation-removal-loop-test)");
        
        // Stop the mission 
        program.executeShutdown();
        
        // Wait up to 5 seconds for the Shutdown state of the mission to occur
        executeMissionSyncer.waitForEvent(5);
        
        // After waiting, make sure that the mission status is Shutdown
        assertThat(program.getProgramStatus(), is(ProgramStatus.SHUTDOWN));
        
        // Remove the mission
        program.remove();
    }
    
    /**
     * This tests that a simple script can execute its test function.
     */
    @Test
    public void testSimpleScriptTestExecution() throws IllegalArgumentException, MissionProgramException, IOException,
        PersistenceFailedException, InterruptedException, ExecutionException
    {
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 5)).withProgramName("TestSimpleTest2");

        //wait for tested event
        EventHandlerSyncer syncer = 
                new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_TESTED, "(name=TestSimpleTest2)");

        Program program = m_MissionProgramManager.loadParameters(params);
        Future<TestResult> results = program.executeTest();

        //verify
        syncer.waitForEvent(10);

        //verify that the parameters were loaded successfully and created a valid Mission program that executed 
        //successfully
        assertThat(program.getProgramStatus(), is(ProgramStatus.WAITING_INITIALIZED));
        assertThat(program.getLastTestResult(), is(TestResult.PASSED));
        assertThat(results.get(), is(TestResult.PASSED));
        program.remove();
    }
    
    /**
     * This tests that a simple script can execute its test function and that the event contains the exception.
     */
    @Test
    public void testSimpleScriptTestExecutionFailed() throws Exception
    {
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-fail-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 5)).withProgramName("TestSimpleTest3");

        //wait for tested event
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_TESTED, 
            String.format(String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleTest3")));

        Program program = m_MissionProgramManager.loadParameters(params);

        program.executeTest();

        //verify
        Event lastEvent = syncer.waitForEvent(15);

        //verify that the parameters were loaded successfully and created a valid Mission program that did not 
        //successfully execute
        assertThat(program.getProgramStatus(), is(ProgramStatus.WAITING_INITIALIZED));
        assertThat(program.getLastTestResult(), is(TestResult.FAILED));
        assertThat(lastEvent, is(notNullValue()));
        assertThat((String)lastEvent.getProperty(Program.EVENT_PROP_PROGRAM_TEST_RESULT_EXCEPTION), 
            containsString("I don't work"));
    }
    
    /**
     * Tests that a status changed event is thrown as part of a simple script's execution. 
     */
    @Test
    public void testSimpleStatusChange() throws Exception
    {
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule().
            withIndefiniteInterval(false).
            withImmediately(false).
            withActive(true).
            withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 5)).withProgramName("TestSimpleStatusTest");
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)",
                        Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleStatusTest"));
        
        Program program = m_MissionProgramManager.loadParameters(params);
        program.execute();
        
        syncer.waitForEvent(5);

        syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_SHUTDOWN, String.format("(%s=%s)",
                Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleStatusTest"));
        
        //shutdown
        program.executeShutdown();
        
        //waiting... have to wait for events so we can remove the program
        syncer.waitForEvent(5);

        //cleanup
        program.remove();
    }
    
    /**
     * Tests the execution of the shutdown method in a simple script.
     */
    @Test
    public void testShutdown() throws Exception
    {
        //create parameters
        MissionProgramSchedule schedule = new MissionProgramSchedule().withIndefiniteInterval(false).
            withImmediately(false).withActive(true).withAtReset(false);
        MissionProgramParameters params = new MissionProgramParameters().withTemplateName("simple-template").
            withSchedule(schedule).withParameters(new MapEntry("a", 5)).withProgramName("TestSimpleShutdown");
        
        Program program = m_MissionProgramManager.loadParameters(params);
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Program.TOPIC_PROGRAM_EXECUTED, 
                String.format("(%s=%s)", Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleShutdown"));
        
        program.execute();
        
        //verify that the parameters were loaded successfully and created a valid Mission program that executed 
        //successfully
        syncer.waitForEvent(5);
        
        syncer = new EventHandlerSyncer(m_Context, 
                Program.TOPIC_PROGRAM_SHUTDOWN, String.format("(%s=%s)",
                        Program.EVENT_PROP_PROGRAM_NAME, "TestSimpleShutdown"));
        
        program.executeShutdown();

        syncer.waitForEvent(5);
        
        assertThat(program.getProgramStatus(), is(ProgramStatus.SHUTDOWN));
    }
}

