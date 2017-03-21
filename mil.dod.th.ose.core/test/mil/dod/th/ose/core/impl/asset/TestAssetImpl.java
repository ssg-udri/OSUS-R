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
package mil.dod.th.ose.core.impl.asset;

import static mil.dod.th.ose.test.matchers.Matchers.rawDictionaryHasEntry;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPanTiltResponse;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.asset.commands.GetProfilesCommand;
import mil.dod.th.core.asset.commands.GetProfilesResponse;
import mil.dod.th.core.asset.commands.SetPositionCommand;
import mil.dod.th.core.asset.commands.SetPositionResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Ellipse;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.status.BatteryChargeLevel;
import mil.dod.th.core.types.status.ChargeLevelEnum;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.impl.asset.data.AssetFactoryObjectDataManager;
import mil.dod.th.ose.core.impl.validator.ValidatorImpl;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;
import mil.dod.th.ose.test.EventAdminSyncer;
import mil.dod.th.ose.test.EventAdminVerifier;
import mil.dod.th.ose.test.LoggingServiceMocker;
import mil.dod.th.ose.test.TerraHarvestControllerMocker;
import mil.dod.th.ose.utils.ConfigurationUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestAssetImpl
{
    private static final String PRODUCT_TYPE = "product type";
    private static UUID OBJ_UUID = UUID.randomUUID();
    private static String OBJ_NAME = "Asset1";
    private static String OBJ_PID = "AssetConfig";
    private static final String OBJ_BASETYPE = "ObjBaseType";
    
    private AssetImpl m_SUT;
    private ObservationStore m_ObservationStore;
    private ValidatorImpl m_Validator;
    private ConfigurationAdmin m_ConfigAdmin;
    private TerraHarvestController m_Controller;
    @SuppressWarnings("rawtypes")
    private FactoryRegistry m_FactoryRegistry;
    private AssetProxy m_Proxy;
    private EventAdmin m_EventAdmin;
    private AssetFactoryObjectDataManager m_ObjectDataManager;
    private PowerManagerInternal m_PowerInternal;
    private WakeLock m_WakeLock;
    private Configuration m_Configuration;
    private AssetCapabilities m_AssetCaps;

    @Mock
    private FactoryInternal factoryInternal;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        m_SUT = new AssetImpl();

        // mock
        MockitoAnnotations.initMocks(this);

        m_ObservationStore = mock(ObservationStore.class);
        m_Validator = mock(ValidatorImpl.class);
        m_ConfigAdmin = mock(ConfigurationAdmin.class);
        m_Controller = TerraHarvestControllerMocker.mockIt(10, OperationMode.TEST_MODE, "controllerName");
        m_FactoryRegistry = mock(FactoryRegistry.class);
        m_Proxy = mock(AssetProxy.class);
        m_EventAdmin = mock(EventAdmin.class);
        m_ObjectDataManager = mock(AssetFactoryObjectDataManager.class);
        m_PowerInternal = mock(PowerManagerInternal.class);
        m_Configuration = mock(Configuration.class);
        m_WakeLock = mock(WakeLock.class);
        m_AssetCaps = new AssetCapabilities()
                .withCommandCapabilities(new CommandCapabilities().withCaptureData(true).withPerformBIT(true));

        // stub
        when(factoryInternal.getProductType()).thenReturn(PRODUCT_TYPE);
        when(factoryInternal.getAssetCapabilities()).thenReturn(m_AssetCaps);

        when(m_ObservationStore.getObservationVersion()).thenReturn(new Version(1, 2));

        m_SUT.setTerraHarvestController(m_Controller);
        m_SUT.setValidator(m_Validator);
        m_SUT.setObservationStore(m_ObservationStore);
        m_SUT.setFactoryObjectDataManager(m_ObjectDataManager);
        m_SUT.setLoggingService(LoggingServiceMocker.createMock());

        Map<String, Coordinates> coordsMap = new HashMap<>();
        Ellipse ellipse = SpatialTypesFactory.newEllipse(1, 2, 90);
        coordsMap.put(null, SpatialTypesFactory.newCoordinates(100, 10, 5, ellipse));
        coordsMap.put("example-sensor-id", SpatialTypesFactory.newCoordinates(101, 11, 6, ellipse));
        Map<String, Orientation> orienMap = new HashMap<>();
        orienMap.put(null, SpatialTypesFactory.newOrientation(100, 90, 45));
        orienMap.put("example-sensor-id", SpatialTypesFactory.newOrientation(101, 91, 46));
        when(m_ObjectDataManager.getCoordinates(OBJ_UUID)).thenReturn(coordsMap);
        when(m_ObjectDataManager.getOrientation(OBJ_UUID)).thenReturn(orienMap);

        // by default have core use basic position handling
        Dictionary<String, Object> table = new Hashtable<>();
        table.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, false);

        when(m_Configuration.getProperties()).thenReturn(table);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                Dictionary<String, Object> dictionary = (Dictionary<String, Object>)invocation.getArguments()[0];
                m_SUT.blockingPropsUpdate(ConfigurationUtils.convertDictionaryPropsToMap(dictionary));

                return null;
            }
        }).when(m_Configuration).update(Mockito.any(Dictionary.class));
        when(m_ConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {m_Configuration});

        when(m_PowerInternal.createWakeLock(m_Proxy.getClass(), m_SUT, "coreFactoryObject")).thenReturn(m_WakeLock);
        when(m_PowerInternal.createWakeLock(m_Proxy.getClass(), m_SUT, "coreAsset")).thenReturn(m_WakeLock);

        m_SUT.initialize(m_FactoryRegistry, m_Proxy, factoryInternal, 
                m_ConfigAdmin, m_EventAdmin, m_PowerInternal, OBJ_UUID, OBJ_NAME, OBJ_PID, OBJ_BASETYPE);
    }
    
    /**
     * Verify the initial status is set correctly and sent to the context for persistence.
     */
    @Test
    public void testInitialStatus() throws Exception
    {
        // verify persisted
        ArgumentCaptor<Observation> observationCap = ArgumentCaptor.forClass(Observation.class);
        verify(m_ObservationStore).persist(observationCap.capture());

        // make sure observation is stored as last status
        assertThat(m_SUT.getLastStatus(), is(observationCap.getValue()));

        // verify base fields are set
        assertThat(observationCap.getValue().getAssetName(), is(m_SUT.getName()));
        assertThat(observationCap.getValue().getAssetUuid(), is(m_SUT.getUuid()));
        assertThat(observationCap.getValue().getSystemId(), is(m_Controller.getId()));
    }

    /**
     * Verify that the proxy is invoked on activate.
     */
    @Test
    public void testActivate() throws AssetException
    {
        m_SUT.onActivate();

        verify(m_WakeLock).activate();
        verify(m_Proxy).onActivate();
        verify(m_WakeLock).cancel();
    }

    /**
     * Verify that the proxy is invoked on deactivate.
     */
    @Test
    public void testDeactivate() throws AssetException
    {
        m_SUT.onDeactivate();
        verify(m_WakeLock).activate();
        verify(m_Proxy).onDeactivate();
        verify(m_WakeLock).cancel();
    }

    /**
     * Verify that the correct sequence of calls are performed when capturing data.
     */
    @Test
    public void testCaptureData()
            throws AssetException, IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        Observation observation = new Observation();
        when(m_Proxy.onCaptureData()).thenReturn(observation);
        assertThat(m_SUT.captureData(), is(observation));

        verify(m_WakeLock).activate();
        verify(m_Proxy).onCaptureData();
        verify(m_WakeLock).cancel();

        verify(m_ObservationStore, atLeastOnce()).persist(Mockito.any(Observation.class));

        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_DATA_CAPTURED, m_SUT);
    }

    /**
     * Verify that if validation fails then an asset exception is thrown.
     */
    @Test
    public void testCaptureDataFailsPersisting() throws ValidationFailedException
    {
        Observation observation = new Observation();

        doThrow(new ValidationFailedException()).when(m_ObservationStore).persist(Mockito.any(Observation.class));

        try
        {
            when(m_Proxy.onCaptureData()).thenReturn(observation);
            m_SUT.captureData();
            fail("expected an exception");
        }
        catch (AssetException ex)
        {
            // expected exception
        }

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testCaptureDataWithSensorId()
            throws AssetException, IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        m_AssetCaps.getCommandCapabilities().setCaptureDataBySensor(true);

        Observation observation = new Observation();
        when(m_Proxy.onCaptureData(anyString())).thenReturn(observation);
        assertThat(m_SUT.captureData("example-sensor-id"), is(observation));

        verify(m_WakeLock).activate();
        verify(m_Proxy).onCaptureData(eq("example-sensor-id"));
        verify(m_WakeLock).cancel();

        verify(m_ObservationStore, atLeastOnce()).persist(Mockito.any(Observation.class));

        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_DATA_CAPTURED, m_SUT);
    }

    /**
     * Verify last status is set after initialize
     */
    @Test
    public void testGetLastStatus()
    {
        Observation status = m_SUT.getLastStatus();

        assertThat(status.isSetStatus(), is(true));
        assertThat(status.getStatus().getSummaryStatus().getSummary(), is(SummaryStatusEnum.UNKNOWN));
        assertThat(status.getStatus().getSummaryStatus().getDescription(), is("A status has not been established."));
    }

    /**
     * Verify that an OperatingStatus can be given and will be set on the asset.
     */
    @Test
    public void testSetStatusSummary()
    {
        String statusDescription = "Something";
        SummaryStatusEnum statusSummary = SummaryStatusEnum.DEGRADED;

        m_SUT.setStatus(statusSummary, statusDescription);

        ArgumentCaptor<Event> evtCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(evtCaptor.capture());

        assertThat(evtCaptor.getValue().getTopic(), is(Asset.TOPIC_STATUS_CHANGED));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION),
                is("Something"));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY),
                is(SummaryStatusEnum.DEGRADED.toString()));
        assertThat(evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID), notNullValue());

        Observation observation = m_SUT.getLastStatus();

        assertThat(observation.isSetStatus(), is(true));
        assertThat(observation.isSetSensorId(), is(false));
        assertThat(observation.getStatus().getSummaryStatus().getDescription(), is("Something"));
        assertThat(observation.getStatus().getSummaryStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
    }

    /**
     * Verify that an OperatingStatus can be given and will be set on the asset for a specific sensor.
     */
    @Test
    public void testSetStatusSummaryWithSensorId()
    {
        String sensorId = "testId1";
        String statusDescription = "Something";
        SummaryStatusEnum statusSummary = SummaryStatusEnum.DEGRADED;

        m_SUT.setStatus(sensorId, statusSummary, statusDescription);

        ArgumentCaptor<Event> evtCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(evtCaptor.capture());

        assertThat(evtCaptor.getValue().getTopic(), is(Asset.TOPIC_STATUS_CHANGED));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION),
                is("Something"));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY),
                is(SummaryStatusEnum.DEGRADED.toString()));
        assertThat(evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID), notNullValue());

        Observation observation = m_SUT.getLastStatus();

        assertThat(observation.isSetStatus(), is(true));
        assertThat(observation.getSensorId(), is(sensorId));
        assertThat(observation.getStatus().getSummaryStatus().getDescription(), is("Something"));
        assertThat(observation.getStatus().getSummaryStatus().getSummary(), is(SummaryStatusEnum.DEGRADED));
    }

    /**
     * Verifies that a null status will not throw an exception (data store will throw exception at runtime due to
     * validation error).
     */
    @Test
    public void testNullStatusOnSetStatus() throws ValidationFailedException
    {
        // null status
        Status status = null;
        m_SUT.setStatus(status);
    }

    /**
     * Verifies that a null status will not throw an exception (data store will throw exception at runtime due to
     * validation error).
     */
    @Test
    public void testNullStatusOnSetStatusWithSensorId() throws ValidationFailedException
    {
        // null status
        Status status = null;
        m_SUT.setStatus("s1", status);
    }

    /**
     * Verifies that a status with a missing status summary will not throw an exception (data store will throw exception
     * at runtime due to validation error).
     */
    @Test
    public void testNullSummaryStatusOnSetStatus() throws ValidationFailedException
    {
        Status status = new Status();
        status.withBatteryChargeLevel(new BatteryChargeLevel().withChargeLevel(ChargeLevelEnum.LOW));
        m_SUT.setStatus(status);
    }

    /**
     * Verifies that a status with a missing status summary will not throw an exception (data store will throw exception
     * at runtime due to validation error).
     */
    @Test
    public void testNullSummaryStatusOnSetStatusWithSensorId() throws ValidationFailedException
    {
        Status status = new Status();
        status.withBatteryChargeLevel(new BatteryChargeLevel().withChargeLevel(ChargeLevelEnum.LOW));
        m_SUT.setStatus("s1", status);
    }

    /**
     * Verify that a Status can be given and will be set on the asset.
     */
    @Test
    public void testSetStatus() throws ValidationFailedException
    {
        Status status = new Status().withSummaryStatus(
                new OperatingStatus().withDescription("New description").withSummary(SummaryStatusEnum.GOOD));

        m_SUT.setStatus(status);

        ArgumentCaptor<Event> evtCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(evtCaptor.capture());

        assertThat(evtCaptor.getValue().getTopic(), is(Asset.TOPIC_STATUS_CHANGED));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION),
                is("New description"));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY),
                is(SummaryStatusEnum.GOOD.toString()));
        assertThat(evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID), notNullValue());

        Observation observation = m_SUT.getLastStatus();

        assertThat(observation.isSetStatus(), is(true));
        assertThat(observation.isSetSensorId(), is(false));
        assertThat(observation.getStatus().getSummaryStatus().getDescription(), is("New description"));
        assertThat(observation.getStatus().getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
    }

    /**
     * Verify that a Status can be given and will be set on the asset for a given sensor ID.
     */
    @Test
    public void testSetStatusWithSensorId() throws ValidationFailedException
    {
        String sensorId = "testId2";
        Status status = new Status().withSummaryStatus(
                new OperatingStatus().withDescription("New description").withSummary(SummaryStatusEnum.GOOD));

        m_SUT.setStatus(sensorId, status);

        ArgumentCaptor<Event> evtCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(evtCaptor.capture());

        assertThat(evtCaptor.getValue().getTopic(), is(Asset.TOPIC_STATUS_CHANGED));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION),
                is("New description"));
        assertThat((String)evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY),
                is(SummaryStatusEnum.GOOD.toString()));
        assertThat(evtCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID), notNullValue());

        Observation observation = m_SUT.getLastStatus();

        assertThat(observation.isSetStatus(), is(true));
        assertThat(observation.getSensorId(), is(sensorId));
        assertThat(observation.getStatus().getSummaryStatus().getDescription(), is("New description"));
        assertThat(observation.getStatus().getSummaryStatus().getSummary(), is(SummaryStatusEnum.GOOD));
    }

    /**
     * Verify that performBit can be performed.
     */
    @Test
    public void testPerformBit() throws AssetException
    {
        Status retStatus = new Status().withSummaryStatus(
                new OperatingStatus().withDescription("New description").withSummary(SummaryStatusEnum.GOOD));
        when(m_Proxy.onPerformBit()).thenReturn(retStatus);

        final Observation status = m_SUT.performBit();

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        assertThat(status, is(m_SUT.getLastStatus()));
        final ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, atLeastOnce()).postEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTopic(), is(Asset.TOPIC_STATUS_CHANGED));
        assertThat(
                SummaryStatusEnum.valueOf(
                        (String)eventCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY)),
                is(SummaryStatusEnum.GOOD));
        assertThat((String)eventCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION),
                is("New description"));
        assertThat((UUID)eventCaptor.getValue().getProperty(Asset.EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID),
                is(notNullValue()));
    }

    /**
     * Verify bad status if perform bit obs is invalid.
     */
    @Test
    public void testPerformBitInvalidObs()
            throws AssetException, IllegalArgumentException, PersistenceFailedException, ValidationFailedException
    {
        Status retStatus = new Status().withSummaryStatus(
                new OperatingStatus().withDescription("New description").withSummary(SummaryStatusEnum.GOOD));
        when(m_Proxy.onPerformBit()).thenReturn(retStatus);
        // throw exception for first obs which is bit obs, then do nothing for subsequent bad status
        doThrow(new ValidationFailedException()).doNothing().when(m_ObservationStore).persist(
                Mockito.any(Observation.class));

        m_SUT.performBit();

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        // last event should be bad status
        Map<String, Object> map = new HashMap<>();
        map.put(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION, "Invalid data received from perform BIT");
        map.put(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY, SummaryStatusEnum.BAD.toString());
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_STATUS_CHANGED, map);
    }

    /**
     * Verify an exception is thrown if the asset fails during BIT.
     */
    @Test
    public void testPerformBitAssetException() throws AssetException
    {
        when(m_Proxy.onPerformBit()).thenThrow(new AssetException("exception"));

        m_SUT.performBit();

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Observation status = m_SUT.getLastStatus();

        assertThat(status.isSetStatus(), notNullValue());
        assertThat(status.getStatus().getSummaryStatus().getSummary(), is(SummaryStatusEnum.BAD));
        assertThat(status.getStatus().getSummaryStatus().getDescription(), is("Failed to perform BIT"));
    }

    /**
     * Verify current status flags default to false since not running.
     */
    @Test
    public void testAssetFlags()
    {
        // default to return is false
        assertThat(m_SUT.isPerformingBit(), is(false));
        assertThat(m_SUT.isCapturingData(), is(false));
    }

    /**
     * Verify exception can be thrown when validation of command fails.
     */
    @Test
    public void testExecuteCommandValidationFail()
            throws CommandExecutionException, ValidationFailedException, InterruptedException
    {
        doThrow(new ValidationFailedException("failed")).when(m_Validator).validate(Mockito.any(Object.class));
        Command command = mock(Command.class);

        try
        {
            m_SUT.executeCommand(command);
            fail("expecting exception");
        }
        catch (CommandExecutionException ex)
        {
            // expecting exception
        }

        verify(m_WakeLock, never()).activate();
        verify(m_WakeLock, never()).cancel();
    }

    /**
     * Verify execute setposition command works as intended.
     */
    @Test
    public void testExecuteSetPositionCommand() throws CommandExecutionException, ValidationFailedException,
            InterruptedException, IOException, ConfigurationException
    {
        Dictionary<String, Object> propsUsedForNewProp = new Hashtable<>();
        propsUsedForNewProp.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, true);
        when(m_Configuration.getProperties()).thenReturn(propsUsedForNewProp);
        when(m_ConfigAdmin.getConfiguration(eq(OBJ_PID), Mockito.any(String.class))).thenReturn(m_Configuration);
        // update props
        m_SUT.blockingPropsUpdate(new HashMap<String, Object>());

        SetPositionResponse response = mock(SetPositionResponse.class);
        when(m_Proxy.onExecuteCommand(Mockito.any(Command.class))).thenReturn(response);

        SetPositionCommand command = mock(SetPositionCommand.class);
        m_SUT.executeCommand(command);

        verify(m_Proxy).onExecuteCommand(command);

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, response.getClass().getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);
    }

    /**
     * Verify execute setposition command when not overridden works as intended.
     */
    @Test
    public void testExecuteSetPositionCommandNoPosOverride()
            throws CommandExecutionException, ValidationFailedException, InterruptedException, IOException
    {
        SetPositionCommand command = mock(SetPositionCommand.class);
        when(command.getLocation()).thenReturn(
                SpatialTypesFactory.newCoordinates(88d, 99d, 3d, SpatialTypesFactory.newEllipse(1d, 2d, 3d)));
        when(command.getOrientation()).thenReturn(SpatialTypesFactory.newOrientation(12d, 13d, 14d));
        m_SUT.executeCommand(command);

        verify(m_Proxy, never()).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, SetPositionResponse.class.getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, notNullValue());
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);

        Coordinates setCoords = getCoordsFromCommand(null);
        verifyCoordsWithEllipse(setCoords, 88d, 99d, 3d, 1d, 2d, 3d);

        Orientation setOrient = getOrientationFromCommand(null);
        verifyOrientation(setOrient, 12d, 13d, 14d);
    }

    @Test
    public void testExecuteSetPositionCommandNoPosOverrideBySensorId()
            throws CommandExecutionException, ValidationFailedException, InterruptedException, IOException
    {
        SetPositionCommand command = new SetPositionCommand()
                .withSensorId("example-sensor-id")
                .withLocation(
                    SpatialTypesFactory.newCoordinates(88d, 99d, 3d, SpatialTypesFactory.newEllipse(1d, 2d, 3d)))
                .withOrientation(SpatialTypesFactory.newOrientation(12d, 13d, 14d));
        m_SUT.executeCommand(command);

        verify(m_Proxy, never()).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, SetPositionResponse.class.getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, notNullValue());
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);

        Coordinates setCoords = getCoordsFromCommand("example-sensor-id");
        verifyCoordsWithEllipse(setCoords, 88d, 99d, 3d, 1d, 2d, 3d);

        Orientation setOrient = getOrientationFromCommand("example-sensor-id");
        verifyOrientation(setOrient, 12d, 13d, 14d);
    }

    /**
     * Verify execute {@link SetPositionCommand} with no location or orientation that the position stays unaltered.
     */
    @Test
    public void testExecuteSetPositionCommandNoLocationOrOrientation()
            throws CommandExecutionException, ValidationFailedException, InterruptedException, IOException
    {
        SetPositionCommand command = new SetPositionCommand();
        m_SUT.executeCommand(command);

        verify(m_Proxy, never()).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, SetPositionResponse.class.getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, notNullValue());
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);

        Coordinates setCoords = getCoordsFromCommand(null);
        verifyCoordsWithEllipse(setCoords, 100d, 10d, 5d, 1d, 2d, 90d);

        Orientation setOrient = getOrientationFromCommand(null);
        verifyOrientation(setOrient, 100d, 90d, 45d);
    }

    /**
     * Verify execute getposition command works as intended.
     */
    @Test
    public void testExecuteGetPositionCommand() throws CommandExecutionException, ValidationFailedException,
            InterruptedException, IOException, ConfigurationException
    {
        Dictionary<String, Object> propsUsedForNewProp = new Hashtable<>();
        propsUsedForNewProp.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, true);
        when(m_Configuration.getProperties()).thenReturn(propsUsedForNewProp);
        when(m_ConfigAdmin.getConfiguration(eq(OBJ_PID), Mockito.any(String.class))).thenReturn(m_Configuration);
        // update props
        m_SUT.blockingPropsUpdate(new HashMap<String, Object>());

        GetPositionResponse response = mock(GetPositionResponse.class);
        when(m_Proxy.onExecuteCommand(Mockito.any(Command.class))).thenReturn(response);

        GetPositionCommand command = mock(GetPositionCommand.class);
        m_SUT.executeCommand(command);

        verify(m_Proxy).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, response.getClass().getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);
    }

    /**
     * Verify execute getposition command works as intended when override position is set to false.
     */
    @Test
    public void testExecuteGetPositionCommandNoOverridePos()
            throws CommandExecutionException, ValidationFailedException, InterruptedException, IOException
    {
        GetPositionCommand command = mock(GetPositionCommand.class);
        GetPositionResponse response = (GetPositionResponse)m_SUT.executeCommand(command);

        verify(m_Proxy, never()).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, response.getClass().getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);

        verifyCoordsWithEllipse(response.getLocation(), 100d, 10d, 5d, 1d, 2d, 90d);
        verifyOrientation(response.getOrientation(), 100d, 90d, 45d);
    }

    @Test
    public void testExecuteGetPositionCommandNoOverridePosBySensorId()
            throws CommandExecutionException, ValidationFailedException, InterruptedException, IOException
    {
        GetPositionCommand command = new GetPositionCommand().withSensorId("example-sensor-id");
        GetPositionResponse response = (GetPositionResponse)m_SUT.executeCommand(command);

        verify(m_Proxy, never()).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, response.getClass().getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);

        assertThat(response.getSensorId(), is("example-sensor-id"));
        verifyCoordsWithEllipse(response.getLocation(), 101d, 11d, 6d, 1d, 2d, 90d);
        verifyOrientation(response.getOrientation(), 101d, 91d, 46d);
    }

    /**
     * Verify execute other command works as intended.
     */
    @Test
    public void testOtherCommand()
            throws CommandExecutionException, ValidationFailedException, InterruptedException, IOException
    {
        GetProfilesCommand command = mock(GetProfilesCommand.class);
        GetProfilesResponse response = mock(GetProfilesResponse.class);
        when(m_Proxy.onExecuteCommand(command)).thenReturn(response);
        m_SUT.executeCommand(command);

        verify(m_Proxy).onExecuteCommand(command);
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();

        Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, response.getClass().getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE, props);
    }

    /**
     * Verify that a persisted observation is saved correctly.
     */
    @Test
    public void testPersistObservation() throws PersistenceFailedException, ValidationFailedException
    {
        Observation observation = new Observation();
        m_SUT.persistObservation(observation);

        ArgumentCaptor<Observation> obsCapt = ArgumentCaptor.forClass(Observation.class);
        verify(m_ObservationStore, atLeastOnce()).persist(obsCapt.capture());

        Observation persistedObservation = obsCapt.getValue();
        assertThat(persistedObservation.getAssetName(), is(OBJ_NAME));
        assertThat(persistedObservation.getAssetUuid(), is(OBJ_UUID));
        assertThat(persistedObservation.getSystemId(), is(m_Controller.getId()));
        assertThat(persistedObservation.getVersion(), is(m_ObservationStore.getObservationVersion()));
        assertThat(persistedObservation.isSystemInTestMode(), is(true));
        assertThat(persistedObservation.getUuid(), notNullValue());
        assertThat(persistedObservation.isSetObservedTimestamp(), is(false));
        assertThat(persistedObservation.isSetCreatedTimestamp(), is(true));
        assertThat(persistedObservation.getAssetType(), is(PRODUCT_TYPE));

        verifyCoordsWithEllipse(persistedObservation.getAssetLocation(), 100d, 10d, 5d, 1d, 2d, 90d);
        verifyOrientation(persistedObservation.getAssetOrientation(), 100d, 90d, 45d);
    }

    /**
     * Verify that UUID and timestamp fields do not get overridden if already set and that position is not changed if
     * overridden by the plug-in.
     */
    @Test
    public void testPersistObservationFieldsSet() throws Exception
    {
        Observation observation = new Observation();
        UUID uuid = UUID.randomUUID();
        long time = System.currentTimeMillis();
        Coordinates coords = SpatialTypesFactory.newCoordinates(5, 10);
        Orientation orientation = SpatialTypesFactory.newOrientation(5, 6, 7);
        observation.setUuid(uuid);
        observation.setObservedTimestamp(time - 1000);
        observation.setCreatedTimestamp(time);
        observation.setAssetLocation(coords);
        observation.setAssetOrientation(orientation);

        // have the plug-in override the position
        Dictionary<String, Object> propsUsedForNewProp = new Hashtable<>();
        propsUsedForNewProp.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, true);
        when(m_Configuration.getProperties()).thenReturn(propsUsedForNewProp);
        when(m_ConfigAdmin.getConfiguration(eq(OBJ_PID), Mockito.any(String.class))).thenReturn(m_Configuration);

        // update props
        m_SUT.blockingPropsUpdate(new HashMap<String, Object>());

        m_SUT.persistObservation(observation);

        ArgumentCaptor<Observation> obsCapt = ArgumentCaptor.forClass(Observation.class);
        verify(m_ObservationStore, atLeastOnce()).persist(obsCapt.capture());

        Observation persistedObservation = obsCapt.getValue();
        assertThat(persistedObservation.getAssetName(), is(OBJ_NAME));
        assertThat(persistedObservation.getAssetUuid(), is(OBJ_UUID));
        assertThat(persistedObservation.getSystemId(), is(m_Controller.getId()));
        assertThat(persistedObservation.getVersion(), is(m_ObservationStore.getObservationVersion()));
        assertThat(persistedObservation.isSystemInTestMode(), is(true));
        assertThat(persistedObservation.getUuid(), is(uuid));
        assertThat(persistedObservation.getObservedTimestamp(), is(time - 1000));
        assertThat(persistedObservation.getCreatedTimestamp(), is(time));
        assertThat(persistedObservation.getAssetType(), is(PRODUCT_TYPE));

        assertThat(persistedObservation.getAssetLocation(), is(SpatialTypesFactory.newCoordinates(5, 10)));
        assertThat(persistedObservation.getAssetOrientation(), is(SpatialTypesFactory.newOrientation(5, 6, 7)));
    }

    /**
     * Verify event is posted given the parameters.
     */
    @Test
    public void testPostResponseUpdate()
            throws ValidationFailedException, IllegalArgumentException, PersistenceFailedException
    {
        GetPanTiltResponse response = new GetPanTiltResponse();

        // replay
        m_SUT.postResponseUpdate(response);

        // verify posted
        Map<String, Object> map = new HashMap<>();
        map.put(FactoryDescriptor.EVENT_PROP_OBJ, m_SUT);
        map.put(FactoryDescriptor.EVENT_PROP_OBJ_NAME, m_SUT.getName());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, GetPanTiltResponse.class.getName());
        map.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);
        EventAdminVerifier.assertLastEvent(m_EventAdmin, Asset.TOPIC_COMMAND_RESPONSE_UPDATED, map);
    }

    /**
     * Verify {@link AssetImpl#getConfig()} will return an object with the default properties and set properties.
     */
    @Test
    public void testGetConfig()
    {
        Dictionary<String, Object> table = new Hashtable<>();
        when(m_Configuration.getProperties()).thenReturn(table);

        // verify defaults
        assertThat(m_SUT.getConfig().activateOnStartup(), is(false));
        assertThat(m_SUT.getConfig().pluginOverridesPosition(), is(false));

        // test overrides
        table.put(AssetAttributes.CONFIG_PROP_ACTIVATE_ON_STARTUP, true);
        table.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, true);
        assertThat(m_SUT.getConfig().activateOnStartup(), is(true));
        assertThat(m_SUT.getConfig().pluginOverridesPosition(), is(true));
    }

    public void testObsSystemNotInTestMode() throws PersistenceFailedException, ValidationFailedException
    {
        m_Controller = TerraHarvestControllerMocker.mockIt(10, OperationMode.OPERATIONAL_MODE, "controllerName");
        m_SUT.setTerraHarvestController(m_Controller);

        Observation observation = new Observation();
        m_SUT.persistObservation(observation);

        ArgumentCaptor<Observation> obsCapt = ArgumentCaptor.forClass(Observation.class);
        verify(m_ObservationStore, atLeastOnce()).persist(obsCapt.capture());

        Observation persistedObservation = obsCapt.getValue();
        assertThat(persistedObservation.getAssetName(), is(OBJ_NAME));
        assertThat(persistedObservation.getAssetUuid(), is(OBJ_UUID));
        assertThat(persistedObservation.getSystemId(), is(m_Controller.getId()));
        assertThat(persistedObservation.getVersion(), is(m_ObservationStore.getObservationVersion()));
        assertThat(persistedObservation.isSystemInTestMode(), is(false));
        assertThat(persistedObservation.getUuid(), notNullValue());
        assertThat(persistedObservation.isSetCreatedTimestamp(), is(true));
        assertThat(persistedObservation.isSetObservedTimestamp(), is(false));
        assertThat(persistedObservation.getAssetType(), is(AssetProxy.class.getName()));
    }

    /**
     * Verify correct to string format.
     */
    @Test
    public void testToString()
    {
        assertThat(m_SUT.toString(), is("Asset1 (null): UNKNOWN:A status has not been established."));
    }

    /**
     * Verify activation will update status and invoke proxy.
     */
    @Test
    public void testActivateAsset() throws Exception
    {
        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));

        EventAdminSyncer syncer = new EventAdminSyncer(m_EventAdmin, Asset.TOPIC_ACTIVATION_COMPLETE);

        m_SUT.activateAsync();

        // wait for activation
        syncer.waitFor(5, TimeUnit.SECONDS);

        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.ACTIVATED));

        // make sure events fired with correct properties set
        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_WILL_BE_ACTIVATED, m_SUT);
        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_ACTIVATION_COMPLETE, m_SUT);

        verify(m_Proxy).onActivate();

        try
        {
            m_SUT.activateAsync();
            fail("Expecting exception");
        }
        catch (IllegalStateException e)
        {

        }

        // wait to see if activation start and success are called again
        Thread.sleep(1000);

        // verify nothing happened, still same amount of events
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin, Asset.TOPIC_WILL_BE_ACTIVATED, 1);
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin, Asset.TOPIC_ACTIVATION_COMPLETE, 1);
    }

    /**
     * Verify exception handled from proxy when activating. Verify event if activation fails.
     */
    @Test
    public void testActivateAssetFail() throws Exception
    {
        doThrow(new AssetException("failed")).when(m_Proxy).onActivate();

        EventAdminSyncer failedActivationSync = new EventAdminSyncer(m_EventAdmin, Asset.TOPIC_ACTIVATION_FAILED);

        m_SUT.activateAsync();

        failedActivationSync.waitFor(5, TimeUnit.SECONDS);
        // make sure properties are set as well
        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_ACTIVATION_FAILED, m_SUT);

        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));
    }

    /**
     * Verify deactivation will update status and invoke proxy.
     */
    @Test
    public void testDeactivateAsset() throws Exception
    {
        // try to deactivate before the asset is activated
        try
        {
            m_SUT.deactivateAsync();
            fail("Expected exception");
        }
        catch (IllegalStateException e)
        {

        }

        // wait to see if deactivation starts
        Thread.sleep(1000);

        // verify nothing happened, still same amount of events
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin, Asset.TOPIC_WILL_BE_DEACTIVATED, 0);
        EventAdminVerifier.assertEventByTopicOnly(m_EventAdmin, Asset.TOPIC_DEACTIVATION_COMPLETE, 0);

        // activate asset so it can be deactivated
        EventAdminSyncer actSyncer = new EventAdminSyncer(m_EventAdmin, Asset.TOPIC_ACTIVATION_COMPLETE);
        m_SUT.activateAsync();
        actSyncer.waitFor(5, TimeUnit.SECONDS);

        // deactivate asset
        EventAdminSyncer deactSyncer = new EventAdminSyncer(m_EventAdmin, Asset.TOPIC_DEACTIVATION_COMPLETE);
        m_SUT.deactivateAsync();
        deactSyncer.waitFor(5, TimeUnit.SECONDS);

        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));

        // make sure events fired with correct properties set
        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_WILL_BE_DEACTIVATED, m_SUT);
        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_DEACTIVATION_COMPLETE, m_SUT);

        verify(m_Proxy).onDeactivate();
    }

    /**
     * Verify exception handled from proxy when activating. Verify event if deactivation fails.
     */
    @Test
    public void testDeactivateAssetFail() throws Exception
    {
        // activate asset so it can be deactivated
        EventAdminSyncer actSyncer = new EventAdminSyncer(m_EventAdmin, Asset.TOPIC_ACTIVATION_COMPLETE);
        m_SUT.activateAsync();
        actSyncer.waitFor(5, TimeUnit.SECONDS);

        doThrow(new AssetException("failed")).when(m_Proxy).onDeactivate();

        EventAdminSyncer failedActivationSync = new EventAdminSyncer(m_EventAdmin, Asset.TOPIC_DEACTIVATION_FAILED);

        m_SUT.deactivateAsync();

        failedActivationSync.waitFor(5, TimeUnit.SECONDS);
        // make sure properties are set as well
        EventAdminVerifier.assertEventByTopicAsset(m_EventAdmin, Asset.TOPIC_DEACTIVATION_FAILED, m_SUT);

        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.ACTIVATED));
    }

    /**
     * Verify ACTIVATED status can be set. Verify event is posted for each status change.
     */
    @Test
    public void testSetActiveStatus_Activated()
    {
        m_SUT.setActiveStatus(AssetActiveStatus.ACTIVATED);
        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.ACTIVATED));
    }

    /**
     * Verify DEACTIVATED status can be set. Verify event is posted for each status change.
     */
    @Test
    public void testSetActiveStatus_Deactivated()
    {
        m_SUT.setActiveStatus(AssetActiveStatus.DEACTIVATED);
        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.DEACTIVATED));
    }

    /**
     * Verify ACTIVATING status can be set. Verify event is posted for each status change.
     */
    @Test
    public void testSetActiveStatus_Activating()
    {
        m_SUT.setActiveStatus(AssetActiveStatus.ACTIVATING);
        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.ACTIVATING));
    }

    /**
     * Verify DEACTIVATING status can be set. Verify event is posted for each status change.
     */
    @Test
    public void testSetActiveStatus_Deactivating()
    {
        m_SUT.setActiveStatus(AssetActiveStatus.DEACTIVATING);
        assertThat(m_SUT.getActiveStatus(), is(AssetActiveStatus.DEACTIVATING));
    }

    /**
     * Verify the registry is called to delete the object.
     */
    @Test
    public void testDelete() throws Exception
    {
        m_SUT.delete();
        verify(m_FactoryRegistry).delete(m_SUT);
        verify(m_PowerInternal, times(2)).deleteWakeLock(m_WakeLock);
    }

    /**
     * Verify the registry is not called if asset is active.
     */
    @Test
    public void testDelete_Activated() throws Exception
    {
        m_SUT.activateAsync();

        try
        {
            m_SUT.delete();
            fail("Should fail to remove as asset is activated");
        }
        catch (IllegalStateException e)
        {

        }
        verify(m_FactoryRegistry, never()).delete(Mockito.any(FactoryObjectInternal.class));
        verify(m_PowerInternal, never()).deleteWakeLock(m_WakeLock);
    }

    /**
     * Verify ability to set active on start up property value.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public final void testSetActiveOnStartUp() throws Exception
    {
        // act
        m_SUT.setActivateOnStartUp(true);

        ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Configuration).update(props.capture());
        assertThat(props.getValue(), rawDictionaryHasEntry(AssetAttributes.CONFIG_PROP_ACTIVATE_ON_STARTUP, true));
        // should be there from before
        assertThat(props.getValue(),
                rawDictionaryHasEntry(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, false));
    }

    /**
     * Verify ability to set plug-in overrides position property value.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public final void testSetPluginOverridesPosition() throws Exception
    {
        Dictionary<String, Object> existingProps = new Hashtable<>();
        existingProps.put(AssetAttributes.CONFIG_PROP_ACTIVATE_ON_STARTUP, false);
        when(m_Configuration.getProperties()).thenReturn(existingProps);

        m_SUT.setPluginOverridesPosition(true);

        ArgumentCaptor<Dictionary> props = ArgumentCaptor.forClass(Dictionary.class);
        verify(m_Configuration).update(props.capture());
        assertThat(props.getValue(),
                rawDictionaryHasEntry(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, true));
        // should be there from before
        assertThat(props.getValue(), rawDictionaryHasEntry(AssetAttributes.CONFIG_PROP_ACTIVATE_ON_STARTUP, false));
    }

    private Coordinates getCoordsFromCommand(final String sensorId)
    {
        GetPositionCommand command = new GetPositionCommand().withSensorId(sensorId);
        GetPositionResponse response;
        try
        {
            response = (GetPositionResponse)m_SUT.executeCommand(command);
            assertThat(response.getSensorId(), is(sensorId));
        }
        catch (CommandExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        return response.getLocation();
    }

    private Orientation getOrientationFromCommand(final String sensorId)
    {
        GetPositionCommand command = new GetPositionCommand().withSensorId(sensorId);
        GetPositionResponse response;
        try
        {
            response = (GetPositionResponse)m_SUT.executeCommand(command);
            assertThat(response.getSensorId(), is(sensorId));
        }
        catch (CommandExecutionException | InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        return response.getOrientation();
    }

    private void verifyCoordsWithEllipse(Coordinates coords, double lon, double lat, double alt, double major,
            double minor, double rotation)
    {
        verifyCoordinates(coords, lon, lat, alt);
        verifyEllipse(coords.getEllipseRegion(), major, minor, rotation);
    }

    private void verifyEllipse(Ellipse ellipse, double major, double minor, double rotation)
    {
        assertThat(ellipse.getSemiMajorAxis().getValue(), is(major));
        assertThat(ellipse.getSemiMinorAxis().getValue(), is(minor));
        assertThat(ellipse.getRotation().getValue(), is(rotation));
    }

    private void verifyCoordinates(Coordinates coords, double lon, double lat, double alt)
    {
        assertThat(coords.getAltitude().getValue(), is(alt));
        assertThat(coords.getLatitude().getValue(), is(lat));
        assertThat(coords.getLongitude().getValue(), is(lon));
    }

    private void verifyOrientation(Orientation orient, double head, double elv, double bank)
    {
        assertThat(orient.getBank().getValue(), is(bank));
        assertThat(orient.getElevation().getValue(), is(elv));
        assertThat(orient.getHeading().getValue(), is(head));
    }
}
