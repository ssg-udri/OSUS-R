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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.UnmarshalException;

import com.google.common.math.DoubleMath;

import junit.framework.TestCase;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.AssetDirectoryService;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetFactory;
import mil.dod.th.core.asset.Asset.AssetActiveStatus;
import mil.dod.th.core.asset.capability.AudioCapabilities;
import mil.dod.th.core.asset.capability.StatusCapabilities;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.asset.commands.SetPanTiltResponse;
import mil.dod.th.core.asset.commands.SetPositionCommand;
import mil.dod.th.core.asset.commands.SetPositionResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.ComponentTypeEnum;
import mil.dod.th.core.types.audio.AudioRecorder;
import mil.dod.th.core.types.audio.AudioRecorderEnum;
import mil.dod.th.core.types.factory.SpatialTypesFactory;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.LatitudeWgsDegrees;
import mil.dod.th.core.types.spatial.LongitudeWgsDegrees;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.ose.integration.commons.AssetUtils;
import mil.dod.th.ose.integration.commons.EventHandlerSyncer;
import mil.dod.th.ose.integration.commons.FactoryUtils;

import org.knowhowlab.osgi.testing.utils.ServiceUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

import example.asset.ExampleAsset;
import example.asset.ExampleAssetAttributes;
import example.asset.ExampleAssetScanner;
import example.asset.exception.ExampleExceptionAsset;
import example.asset.exception.ExampleObsExAsset;
import example.asset.lexicon.ExampleCommandAsset;

/**
 * Just an initial test to see if Bnd integration testing works.
 * 
 * @author dhumeniuk
 */
public class TestAssetDirectoryService extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    @Override
    public void setUp()
    {
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleAsset.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleExceptionAsset.class, 5000);
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleObsExAsset.class, 5000);
        
        AssetUtils.deleteAllAssets(m_Context);
    }
    
    @Override
    public void tearDown() throws Exception
    {
        AssetUtils.deleteAllAssets(m_Context);
    }
    
    /**
     * Verify that assets can be added.
     */
    public final void testAdd() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));

        assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        Set<Asset> exampleAssets = assetDirectoryService.getAssetsByType(ExampleAsset.class.getName());
        assertThat(exampleAssets.size(), is(1));
    }
    
    /**
     * Verify once asset is added they can be activated/deactivated.
     */
    public final void testActivateDeactivate() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));

        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        Map<String, Object> properties = asset.getProperties();
        properties.put(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME, "exampleA");
        asset.setProperties(properties);
        
        AssetUtils.activateAsset(m_Context, asset, 5);
        
        AssetUtils.deactivateAsset(m_Context, asset);
    }
    
    /**
     * Verify that an Asset can be deleted.
     */
    public final void testDelete() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "AssetToBeDeleted");
        
        asset.delete();
        
        // if correctly removed, should find again in scan
        assertThat(assetDirectoryService.getAssetsByType(ExampleAsset.class.getName()).size(), is(0));
        
        // verify asset can be created again with the same name
        assetDirectoryService.createAsset(ExampleAsset.class.getName(), "AssetToBeDeleted");
    }
    
    /**
     * Tests executing a command for an asset.
     * 
     * Verifies response is actually posted as an event and someone can listen for a specific response by type.
     * Verifies invalid commands can throw exception that is thrown back to the caller.
     */
    public void testAssetExecuteCommand() throws InterruptedException, AssetException, CommandExecutionException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());

        // Test execute for value within range
        // Syncer to listen for Asset Capability Command Response
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Asset.TOPIC_COMMAND_RESPONSE,
                String.format("(%s=%s)", Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, 
                        SetPanTiltResponse.class.getName()));
        final Command setPT = new SetPanTiltCommand().withPanTilt(SpatialTypesFactory.newOrientationOffset(30));
        assertThat(setPT, is(notNullValue()));
        final SetPanTiltResponse setPTResponse = (SetPanTiltResponse)asset.executeCommand(setPT);
        assertThat(setPTResponse.isSetSensorId(), is(false));
        assertThat("PT Response is null", setPTResponse, is(notNullValue()));
        syncer.waitForEvent(5);
        
        // Test validate for value out of range
        final Command badCommand = new SetPanTiltCommand().withPanTilt(SpatialTypesFactory.newOrientationOffset(185));
        try
        {
            asset.executeCommand(badCommand);
            fail("CommandExecutionException should have been thrown");
        }
        catch (CommandExecutionException ex)
        {
            // do nothing, this exception is expected and does not need to be handled
        }
    }
    
    /**
     * Verify that an asset can be requested to capture data.
     * Verify an Observation is posted after the request is made.
     * Verify the correct object base type for the event.
     */
    public final void testAssetCaptureData() throws InterruptedException, IllegalArgumentException, AssetException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, Asset.TOPIC_DATA_CAPTURED,
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid().toString()));
        
        Observation observation = asset.captureData();
        
        Event event = syncer.waitForEvent(5);
        
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        assertThat(observationStore.find(observation.getUuid()), is(notNullValue()));
        
        String baseType = event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_BASE_TYPE).toString();
        assertThat(baseType, is("Asset"));
    }
    
    /*
     * Test that an asset name is correctly set if the name is unique. If the name is not unique it should not be set.
     */
    public void testSetAssetName() throws IllegalArgumentException, AssetException, IOException, InterruptedException, 
        IllegalStateException, FactoryException, PersistenceFailedException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        ConfigurationAdmin configAdmin = ServiceUtils.getService(m_Context, ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, 
            FactoryDescriptor.TOPIC_FACTORY_OBJ_NAME_UPDATED, String.format("(%s=%s)", 
                FactoryDescriptor.EVENT_PROP_OBJ_NAME, "testSetNameAsset"));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "testSetNameAsset");
        asset.setName("testSetNameAsset");
        
        syncer.waitForEvent(10);
        
        Asset asset2 = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        try
        {
            asset2.setName("testSetNameAsset");
            fail("expecting exception");
        }
        catch (IllegalArgumentException e)
        {
            // do nothing, this exception is expected and does not need to be handled
        }

        assertThat(asset.getName(), is("testSetNameAsset"));
        assertThat(asset2.getName(), is(not("testSetNameAsset")));
    }
    
    /*
     * Test that an asset name is correctly set at creation if the name is unique. If the name is not unique 
     * it should not be set and no asset should be created.
     */
    public void testSetAssetNameAtCreation() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        ConfigurationAdmin configAdmin = ServiceUtils.getService(m_Context, ConfigurationAdmin.class);
        assertThat(configAdmin, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "testSetNameAsset");
        int size = assetDirectoryService.getAssets().size();
        
        try
        {
            assetDirectoryService.createAsset(ExampleAsset.class.getName(), "testSetNameAsset");
            fail("expecting exception");
        }
        catch (Exception e)
        {
            // this exception is expected
            assertThat(e.getCause().getMessage(), 
                    containsString("Duplicate name: [testSetNameAsset] is already in use"));
        }

        assertThat(assetDirectoryService.getAssetByName("testSetNameAsset"), is(asset));
        // verify that no new asset was created
        assertThat(assetDirectoryService.getAssets().size(), is(size));
    }
    
    /**
     * Verify the asset scan will pick up assets if scanner is enabled.
     */
    public void testScan() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, 
                AssetDirectoryService.TOPIC_SCANNING_FOR_ASSETS_COMPLETE, 
                String.format("(!(%s=*))", FactoryDescriptor.EVENT_PROP_OBJ_TYPE));
        
        assetDirectoryService.scanForNewAssets();
        
        syncer.waitForEvent(20);
        
        Set<Asset> assets = assetDirectoryService.getAssets();
        assertThat("Scanning did not create assets", assets.size() > 0);
        
        Set<Asset> exampleAssets = assetDirectoryService.getAssetsByType(ExampleAsset.class.getName());
        assertThat(exampleAssets.size(), is(1));
        
        Asset exampleAsset = exampleAssets.iterator().next();
        
        //Verify that name is set to a none default.
        assertThat(exampleAsset.getName(), is(ExampleAssetScanner.ASSET_NAME));
        Map<String, Object> props = exampleAsset.getProperties();
        assertThat((String)props.get(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME), is("exampleB"));

        AssetUtils.activateAsset(m_Context, exampleAsset, 5);

        AssetUtils.deactivateAsset(m_Context, exampleAsset);
    }
    
    /**
     * Verify data can be captured from an asset.
     */
    public void testCaptureData() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        asset.captureData();
    }

    /**
     * Verify data can be captured from an asset when a sensor ID is provided.
     */
    public void testCaptureDataWithSensorId() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        Asset asset = assetDirectoryService.createAsset(ExampleCommandAsset.class.getName());
        
        Observation observation = asset.captureData("sensor-id");
        assertThat(observation.getSensorId(), is("sensor-id"));
    }

    /**
     * Verify exception is thrown if the asset has not implemented captureData with sensor ID.
     */
    public void testCaptureDataWithSensorIdException() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        try
        {
            asset.captureData("sensor-id");
            fail("expecting exception");
        }
        catch (AssetException e)
        {
            // Expected
        }
    }

    /**
     * Verify exception is thrown when invalid Observation is captured from an asset.
     */
    public void testCaptureDataInvalidObs() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        ObservationStore obsStore = ServiceUtils.getService(m_Context, ObservationStore.class);

        Asset asset = assetDirectoryService.createAsset(ExampleObsExAsset.class.getName());
        AssetUtils.activateAsset(m_Context, asset, 5);

        // Capture initial observation
        final Observation captured = asset.captureData();
        try
        {
            // Try to capture again using duplicate data
            obsStore.remove(asset.captureData());

            fail("Expected exception for captureData with invalid Observation");
        }
        catch (final AssetException ex)
        {
            // Expected
        }
        finally
        {
            // Should be able to remove the observation that was successfully persisted with duplicate data between
            // different fields
            obsStore.remove(captured);

            AssetUtils.deactivateAsset(m_Context, asset);
        }
    }
    
    /**
     * Tests executing a position command for an asset.
     * 
     * Verifies response is actually posted as an event and someone can listen for a specific response by type.
     * Verifies invalid commands can throw exception that is thrown back to the caller.
     */
    public void testAssetExecuteCommandPosition() throws InterruptedException, AssetException, 
        CommandExecutionException, IllegalArgumentException, IllegalStateException, FactoryException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        //verify override if false as GPS is not available per caps, default is true if caps aren't available
        assertThat(asset.getConfig().pluginOverridesPosition(), is(false));
        
        // Test execute for value within range
        // Syncer to listen event that data was persisted
        EventHandlerSyncer syncer = new EventHandlerSyncer(m_Context, PersistentDataStore.TOPIC_DATA_MERGED);
        
        //location without sensor ID
        Command setPos = new SetPositionCommand().withLocation(
            new Coordinates().
                withLongitude(
                    new LongitudeWgsDegrees().withValue(31)).
                withLatitude(
                    new LatitudeWgsDegrees().withValue(-15)));
        assertThat(setPos, is(notNullValue()));
        SetPositionResponse setPTResponse = (SetPositionResponse)asset.executeCommand(setPos);
        syncer.waitForEvent(10);
        assertThat("Position Response is null", setPTResponse, is(notNullValue()));
        assertThat(setPTResponse.getSensorId(), is(nullValue()));
        
        // Syncer to listen event that data was persisted
        syncer = new EventHandlerSyncer(m_Context, PersistentDataStore.TOPIC_DATA_MERGED);

        //location with sensor ID
        setPos = new SetPositionCommand().withSensorId("sensor-1").withLocation(
            new Coordinates().
                withLongitude(
                    new LongitudeWgsDegrees().withValue(41)).
                withLatitude(
                    new LatitudeWgsDegrees().withValue(-25)));
        assertThat(setPos, is(notNullValue()));
        setPTResponse = (SetPositionResponse)asset.executeCommand(setPos);
        syncer.waitForEvent(10);
        assertThat("Position Response is null", setPTResponse, is(notNullValue()));
        assertThat(setPTResponse.getSensorId(), is("sensor-1"));
        
        //verify that the value is retrievable when sensor ID is not set
        Command getPos = new GetPositionCommand();
        
        GetPositionResponse getPTResponse = (GetPositionResponse)asset.executeCommand(getPos);
        assertThat("Get Position Response is null", getPTResponse, is(notNullValue()));
        assertThat(getPTResponse.getSensorId(), is(nullValue()));
        
        assertThat(getPTResponse.getLocation().getLongitude().getValue(), is((double)31));
        assertThat(getPTResponse.getLocation().getLatitude().getValue(), is((double)-15));

        //verify that the value is retrievable when sensor ID is set
        getPos = new GetPositionCommand().withSensorId("sensor-1");
        
        getPTResponse = (GetPositionResponse)asset.executeCommand(getPos);
        assertThat("Get Position Response is null", getPTResponse, is(notNullValue()));
        assertThat(getPTResponse.getSensorId(), is("sensor-1"));
        
        assertThat(getPTResponse.getLocation().getLongitude().getValue(), is((double)41));
        assertThat(getPTResponse.getLocation().getLatitude().getValue(), is((double)-25));
    }
    
    /**
     * Verify a command that contains data outside the ranges defined by the schema will result in an exception.
     */
    public void testExecuteCommand_Invalid() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        //verify override if false as GPS is not available per caps, default is true if caps aren't available
        assertThat(asset.getConfig().pluginOverridesPosition(), is(false));
        
        //location
        final Command setPos = new SetPositionCommand().withLocation(SpatialTypesFactory.newCoordinates(3000, -1000));
        try
        {
            asset.executeCommand(setPos);
            fail("Expecting exception as coordinate is out of range");
        }
        catch (CommandExecutionException e) { }
    }
    
    /**
     * Tests executing a position command for an asset that does not use the default behavior.
     * 
     * Verifies response is actually posted as an event and someone can listen for a specific response by type.
     * Verifies invalid commands can throw exception that is thrown back to the caller.
     */
    public void testAssetExecuteCommandPositionOverrideTrue() throws Exception
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        asset.setName("positionAsset");
        
        Map<String, Object> properties = asset.getProperties();
        properties.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, true);
        asset.setProperties(properties);
        
        // Test execute for value within range
        //location, none of which will actually be used but need to make the command valid
        final Command setPos = new SetPositionCommand().withLocation(
            new Coordinates().
                withLongitude(
                    new LongitudeWgsDegrees().withValue(31)).
                withLatitude(
                    new LatitudeWgsDegrees().withValue(-15)));
        assertThat(setPos, is(notNullValue()));
        final SetPositionResponse setPTResponse = (SetPositionResponse)asset.executeCommand(setPos);
        assertThat("Position Response is null", setPTResponse, is(notNullValue()));
        
        //verify that the value is retrievable
        final Command getPos = new GetPositionCommand();
        
        final GetPositionResponse getPTResponse = (GetPositionResponse)asset.executeCommand(getPos);
        assertThat("Get Position Response is null", getPTResponse, is(notNullValue()));
        
        //these values are implemented in the example asset
        assertThat(getPTResponse.getLocation().getLongitude().getValue(), is((double)0));
        assertThat(getPTResponse.getLocation().getLatitude().getValue(), is((double)0));
    }

    /**
     * Tests persistence and removal of observation location data for an asset.
     */
    public void testRemoveObsWithLocation() throws InterruptedException, AssetException, 
        CommandExecutionException, IllegalArgumentException, IllegalStateException, FactoryException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName());
        
        //verify override if false as GPS is not available per caps, default is true if caps aren't available
        assertThat(asset.getConfig().pluginOverridesPosition(), is(false));
        
        EventHandlerSyncer dataSyncer = new EventHandlerSyncer(m_Context, PersistentDataStore.TOPIC_DATA_MERGED);
        
        //set location
        final Command setPos = new SetPositionCommand().withLocation(
            new Coordinates().
                withLongitude(
                    new LongitudeWgsDegrees().withValue(31)).
                withLatitude(
                    new LatitudeWgsDegrees().withValue(-15)));
        assertThat(setPos, is(notNullValue()));
        final SetPositionResponse setPTResponse = (SetPositionResponse)asset.executeCommand(setPos);
        dataSyncer.waitForEvent(10);
        assertThat("Position Response is null", setPTResponse, is(notNullValue()));
        
        //verify that observations can be removed with location data set
        EventHandlerSyncer obsSyncer = new EventHandlerSyncer(m_Context, Asset.TOPIC_DATA_CAPTURED,
                String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid().toString()));
        Observation observation1 = asset.captureData();
        Observation observation2 = asset.captureData();
        Observation observation3 = asset.captureData();
        obsSyncer.waitForEvent(10, 3, 2);
        
        ObservationStore observationStore = ServiceUtils.getService(m_Context, ObservationStore.class);
        observationStore.remove(observation1.getUuid());
        observationStore.remove(observation2.getUuid());
        observationStore.remove(observation3.getUuid());
    }
    
    /**
     * Verify that when an Asset's status changes that an event is posted.
     * Verify the status in the event contains a message, and that the event status data matches that
     * which returned when the status is directly requested.
     */
    public final void testAssetStatusChange() throws InterruptedException, IllegalArgumentException, AssetException
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        Asset asset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), "statusAsset");

        AssetStatusEventHandler statusEventHandler = new AssetStatusEventHandler();
        EventHandlerSyncer statusChangeSync = new EventHandlerSyncer(m_Context, Asset.TOPIC_STATUS_CHANGED,
            String.format("(%s=%s)", FactoryDescriptor.EVENT_PROP_OBJ_UUID, asset.getUuid()),
            statusEventHandler);
        
        AssetUtils.activateAsset(m_Context, asset, 5);
        
        statusChangeSync.waitForEvent(5);
        
        //asset status
        Observation status = asset.getLastStatus();
        Status fromHandler = statusEventHandler.getAssetStatus();
        assertThat(fromHandler, is(notNullValue()));
        assertThat(status.getStatus().getNextStatusDurationMs(), is(100));
        assertThat(status.getStatus().getSummaryStatus().getSummary(), 
                is(fromHandler.getSummaryStatus().getSummary()));
        assertThat(status.getStatus().getSummaryStatus().getDescription(), 
                is(fromHandler.getSummaryStatus().getDescription()));
        
        AssetUtils.deactivateAsset(m_Context, asset);
    }
    
    /**
     * Test that status capabilities are available as filled out in the ExampleAsset capability XML.  Must be updated
     * to match capability file for any further changes.
     */
    public final void testAssetCapabilities() throws UnmarshalException
    {
        AssetFactory testFactory = null;
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        for (AssetFactory factory : assetDirectoryService.getAssetFactories())
        {
            if (factory.getProductType().equals(ExampleAsset.class.getName()))
            {
                testFactory = factory;
            }
        }
        
        assertThat("Example asset factory was not found", testFactory, is(notNullValue()));
        
        StatusCapabilities statusCaps = testFactory.getAssetCapabilities().getStatusCapabilities();
        assertThat("Status capabilities not found", statusCaps, is(notNullValue()));
        
        assertThat(statusCaps.getAvailableComponentStatuses().size(), is(2));
        assertThat(statusCaps.getAvailableComponentStatuses(), 
                hasItem(new ComponentType().withType(ComponentTypeEnum.CPU)));
        assertThat(statusCaps.getAvailableComponentStatuses(), 
                hasItem(new ComponentType(ComponentTypeEnum.OTHER, "blah")));
        assertThat(statusCaps.isSensorFovAvailable(), is(false));
        assertThat(statusCaps.isSensorRangeAvailable(), is(true));
        assertThat(statusCaps.isAnalogAnalogVoltageAvailable(), is(false));
        assertThat(statusCaps.isAnalogDigitalVoltargeAvailable(), is(false));
        assertThat(statusCaps.isAnalogMagVoltageAvailable(), is(false));
        assertThat(statusCaps.isAssetOnTimeAvailable(), is(false));
        assertThat(statusCaps.isBatteryChargeLevelAvailable(), is(false));
        assertThat(statusCaps.isBatteryVoltageAvailable(), is(false));
        assertThat(statusCaps.isPowerConsumptionAvailable(), is(false));
        assertThat(statusCaps.isTemperatureAvailable(), is(true));
        
        AudioCapabilities audioCaps = testFactory.getAssetCapabilities().getAudioCapabilities();
        assertThat("Audio capabilities not found", audioCaps, is(notNullValue()));
        
        assertThat(audioCaps.getRecorders().size(), is(1));
        assertThat(audioCaps.getRecorders(), hasItem(new AudioRecorder(AudioRecorderEnum.MICROPHONE, "none", null)));
        assertThat("Audio sample rate not equal to 14.4",
                   DoubleMath.fuzzyEquals(audioCaps.getSampleRatesKHz().get(0).doubleValue(), 14.4, 0.00001));
    }
    
    /**
     * Test shutdown of the asset directory service.
     * Verify that assets are deactivated before the service is deactivated.
     */
    @SuppressWarnings("unchecked")
    public final void testShutdown() 
        throws IllegalArgumentException, AssetException, IllegalStateException, 
            FactoryException, InterruptedException, BundleException 
    {
        AssetDirectoryService assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class);
        
        final String assetName = "shutdownAsset";
        Asset exampleAsset = assetDirectoryService.createAsset(ExampleAsset.class.getName(), assetName);
        
        Map<String, Object> properties = exampleAsset.getProperties();
        properties.put(ExampleAssetAttributes.CONFIG_PROP_DEVICE_POWER_NAME, "exampleA");
        exampleAsset.setProperties(properties);
        
        for (Asset asset : assetDirectoryService.getAssets())
        {
            if (asset.getActiveStatus() != AssetActiveStatus.ACTIVATED)
            {
                AssetUtils.activateAsset(m_Context, asset, 5);
            }
        }
        
        LogReaderService logReader = ServiceUtils.getService(m_Context, LogReaderService.class);
        
        final Semaphore semaphore = new Semaphore(0);
        
        logReader.addLogListener(new LogListener()
        {
            @Override
            public void logged(LogEntry entry)
            {
                if ("Example asset deactivated".equals(entry.getMessage()))
                {
                    semaphore.release();
                }
            }
        });
        
        ServiceReference<AssetDirectoryService> adsServiceRef = 
                ServiceUtils.getServiceReference(m_Context, AssetDirectoryService.class);
        Bundle adsBundle = adsServiceRef.getBundle();
        adsBundle.stop();
        
        // check if asset was deactivated on service shutdown
        boolean assetDeactivatedOnSeviceShutdown = semaphore.tryAcquire(5, TimeUnit.SECONDS);
        
        adsBundle.start();
        
        // verify asset was shutdown, this check is asserted after restarting service bundle so that if this assert
        // fails, we still restart bundle so other tests will still run
        assertThat("Asset did not deactivate on shutdown", assetDeactivatedOnSeviceShutdown, is(true));
        
        // get service, old one is no longer valid and wait some time as bundle is starting back up
        assetDirectoryService = ServiceUtils.getService(m_Context, AssetDirectoryService.class, 5000);
        assertThat(assetDirectoryService, is(notNullValue()));
        
        // wait for asset factory descriptor to signal core has loaded all old assets
        FactoryUtils.assertFactoryDescriptorAvailable(m_Context, ExampleAsset.class, 5000);
        
        Asset restoredAsset = null;
        try
        {
            restoredAsset = assetDirectoryService.getAssetByName(assetName);
        }
        catch (final IllegalArgumentException exception)
        {
            fail(String.format("AssetDirectory did not contain the restored asset with name: %s", assetName));
        }
        assertThat(restoredAsset, is(notNullValue()));
    }
    
    /**
     * Event handler that will capture the asset's status
     */
    private class AssetStatusEventHandler implements EventHandler 
    {
        private Status m_Status;
        @Override
        public void handleEvent(Event event)
        {
            // summary should be good
            m_Status = new Status().withSummaryStatus(new OperatingStatus(
                    SummaryStatusEnum.valueOf((String)event.getProperty(Asset.EVENT_PROP_ASSET_STATUS_SUMMARY)), 
                    (String)event.getProperty(Asset.EVENT_PROP_ASSET_STATUS_DESCRIPTION)));
        }
        
        /**
         * Get the asset status stored.
         */
        public Status getAssetStatus()
        {
            return m_Status;
        }
    };
}
