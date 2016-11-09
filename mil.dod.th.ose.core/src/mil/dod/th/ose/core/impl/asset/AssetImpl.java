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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.AssetAttributes;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.asset.AssetProxy;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.GetPositionCommand;
import mil.dod.th.core.asset.commands.GetPositionResponse;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.asset.commands.SetPositionCommand;
import mil.dod.th.core.asset.commands.SetPositionResponse;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.ose.core.factory.api.AbstractFactoryObject;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryRegistry;
import mil.dod.th.ose.core.factory.api.data.FactoryObjectInformationException;
import mil.dod.th.ose.core.impl.asset.data.AssetFactoryObjectDataManager;
import mil.dod.th.ose.core.pm.api.PowerManagerInternal;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.EventAdmin;

/**
 * Asset implementation of a {@link mil.dod.th.core.factory.FactoryObject}.
 */
@Component(factory = AssetInternal.COMPONENT_FACTORY_REG_ID) // NOCHECKSTYLE - Class fan-out complexity max has been
                                                             // reached
public class AssetImpl extends AbstractFactoryObject implements AssetInternal
{
    /**
     * Interface representing the controller environment.
     */
    private TerraHarvestController m_Controller;

    /**
     * The {@link ObservationStore} service to use.
     */
    private ObservationStore m_ObservationStore;

    /**
     * The last known status Observation belonging to this asset.
     */
    private Observation m_LastStatusObservation;

    /**
     * True if the Asset is capturing data.
     */
    private Boolean m_CapturingData = false;

    /**
     * True if the Asset is performing the built-in test (BIT).
     */
    private Boolean m_PerformingBit = false;

    /**
     * Coordinates object representing the location of the represented asset.
     */
    private Coordinates m_AssetLocation;

    /**
     * Orientation object representing the orientation of the represented asset.
     */
    private Orientation m_AssetOrientation;

    /**
     * The {@link AssetProxy} that is associated with this implementation.
     */
    private AssetProxy m_AssetProxy;

    /**
     * The {@link AssetFactoryObjectDataManager} for this implementation.
     */
    private AssetFactoryObjectDataManager m_AssetDataManager;

    /**
     * The validator used to verify commands and observations have the correct fields filled in.
     */
    private Validator m_Validator;

    /**
     * Asset Active Status to hold the status of this particular Asset.
     */
    private AssetActiveStatus m_AssetActiveStatus = AssetActiveStatus.DEACTIVATED;

    /**
     * Boolean value representing if the represented asset possesses the ability to handle it's own position. True means
     * it has implementation for doing so, false means the implementation for handling the position is handled herein.
     */
    private boolean m_PluginOverridesPosition;

    private LoggingService m_Log;

    @Reference
    public void setLoggingService(final LoggingService loggingService)
    {
        m_Log = loggingService;
    }

    /**
     * Method to set the {@link TerraHarvestController} to use.
     * 
     * @param controller
     *            interface representing the controller environment
     */
    @Reference
    public void setTerraHarvestController(final TerraHarvestController controller)
    {
        m_Controller = controller;
    }

    /**
     * Method to set the {@link ObservationStore} to use.
     * 
     * @param store
     *            the observation store to use
     */
    @Reference
    public void setObservationStore(final ObservationStore store)
    {
        m_ObservationStore = store;
    }

    /**
     * Method to set the {@link Validator} to use.
     * 
     * @param validator
     *            the command validator to use
     */
    @Reference
    public void setValidator(final Validator validator)
    {
        m_Validator = validator;
    }

    /**
     * Method to set the {@link AssetFactoryObjectDataManager} to use.
     * 
     * @param manager
     *            the asset factory obeject data manager to use
     */
    @Reference
    public void setFactoryObjectDataManager(final AssetFactoryObjectDataManager manager)
    {
        m_AssetDataManager = manager;
    }
    
    @Override
    public void initialize(final FactoryRegistry<?> registry, final FactoryObjectProxy proxy, 
            final FactoryInternal factory, final ConfigurationAdmin configAdmin, final EventAdmin eventAdmin, 
            final PowerManagerInternal powerMgr, final UUID uuid, final String name, final String pid,
            final String baseType) 
            throws IllegalStateException
    {
        super.initialize(registry, proxy, factory, configAdmin, eventAdmin, powerMgr, uuid, name, pid, baseType);
        m_AssetProxy = (AssetProxy)proxy;
        m_PluginOverridesPosition = getConfig().pluginOverridesPosition();

        // MUST RESTORE THE LOCATION BEFORE POSTING OBSERVATIONS
        // depending on if the position override is true or not the location could be accessed before
        // it is set
        restoreLocation();

        setStatus(SummaryStatusEnum.UNKNOWN, "A status has not been established.");
    }

    @Override
    public synchronized void activateAsync() throws IllegalStateException
    {
        if (getActiveStatus() != AssetActiveStatus.DEACTIVATED)
        {
            throw new IllegalStateException(
                    String.format("Asset %s has an Active Status of [%s], must be %s in order to Activate.", getName(),
                            getActiveStatus(), AssetActiveStatus.DEACTIVATED));
        }

        setActiveStatus(Asset.AssetActiveStatus.ACTIVATING);

        postEvent(TOPIC_WILL_BE_ACTIVATED, null);

        // kick off thread to try and activate
        final Activator runnable = new Activator(this, new ActivationListenerBridge(m_Log));
        final Thread thread = new Thread(runnable);
        thread.setName(getName() + " Activator");
        thread.start();
    }

    @Override
    public void deactivateAsync() throws IllegalStateException
    {
        internalDeactivate(new AssetActivationListener[] {new ActivationListenerBridge(m_Log)});
    }

    @Override
    public void onActivate() throws AssetException
    {
        m_AssetProxy.onActivate();
    }

    @Override
    public void onDeactivate() throws AssetException
    {
        m_AssetProxy.onDeactivate();
    }

    @Override
    public Observation captureData() throws AssetException
    {
        m_Log.debug("Asset %s has begun to capture data.", getName());

        m_CapturingData = true;

        try
        {
            final Observation observation = m_AssetProxy.onCaptureData();

            persistObservation(observation);

            final Map<String, Object> props = new HashMap<>();
            props.put(EVENT_PROP_ASSET_OBSERVATION_UUID, observation.getUuid());
            postEvent(TOPIC_DATA_CAPTURED, props);

            return observation;
        }
        catch (final PersistenceFailedException ex)
        {
            setStatus(SummaryStatusEnum.BAD, "Failed to persist captured data");
            final String errorString = String.format("Unable to persist observation %s while capturing data.",
                    getUuid().toString());
            m_Log.error(errorString);
            throw new AssetException(errorString, ex);

        }
        catch (final ValidationFailedException ex)
        {
            setStatus(SummaryStatusEnum.BAD, "Invalid captured data");
            final String errorString = String.format("Unable to validate observation %s while capturing data.",
                    getUuid().toString());
            m_Log.error(errorString);
            throw new AssetException(errorString, ex);
        }
        finally
        {
            m_CapturingData = false;
        }
    }

    @Override
    public Observation getLastStatus()
    {
        return m_LastStatusObservation;
    }

    @Override
    public boolean isCapturingData()
    {
        return m_CapturingData;
    }

    @Override
    public boolean isPerformingBit()
    {
        return m_PerformingBit;
    }

    @Override
    public Observation performBit()
    {
        m_PerformingBit = true;

        try
        {
            final Status status = m_AssetProxy.onPerformBit();
            m_Log.debug("Performed BIT for Asset %s. Status: %s", getName(), status);

            setStatus(status);
        }
        catch (final AssetException e)
        {
            m_Log.warning(e, "Failed to perform BIT for: %s", getName());

            setStatus(SummaryStatusEnum.BAD, "Failed to perform BIT");
        }
        catch (final ValidationFailedException e)
        {
            m_Log.error(e, "Status received from BIT is invalid for asset: [%s]", getName());
            setStatus(SummaryStatusEnum.BAD, "Invalid data received from perform BIT");
        }

        m_PerformingBit = false;

        return getLastStatus();
    }

    @Override
    public Response executeCommand(final Command command) throws CommandExecutionException, InterruptedException
    {
        m_Log.debug("Executing command [%s] for asset [%s]", command.getClass().getName(), getName());

        try
        {
            m_Validator.validate(command);
        }
        catch (final ValidationFailedException exception)
        {
            throw new CommandExecutionException(String.format("Asset %s cannot execute command [%s], it is not valid.",
                    getName(), command.getClass().getName()), exception);
        }

        // command response
        final Response commandResponse;

        if (command instanceof SetPositionCommand)
        {
            if (m_PluginOverridesPosition)
            {
                commandResponse = m_AssetProxy.onExecuteCommand(command);
            }
            // position is not handled by proxy, just store away the new position
            else
            {
                final SetPositionCommand positionCommand = (SetPositionCommand)command;
                commandResponse = updatePosition(positionCommand.getLocation(), positionCommand.getOrientation());
            }
        }
        else if (command instanceof GetPositionCommand)
        {
            if (m_PluginOverridesPosition)
            {
                commandResponse = m_AssetProxy.onExecuteCommand(command);
            }
            // position is not handled in a special way by the asset proxy, so retrieve what we got cached
            else
            {
                final GetPositionResponse response = new GetPositionResponse();
                if (m_AssetLocation != null)
                {
                    response.withLocation(cloneCoordinates(m_AssetLocation));
                }
                if (m_AssetOrientation != null)
                {
                    response.withOrientation(cloneOrientation(m_AssetOrientation));
                }
                commandResponse = response;
            }
        }
        else
        {
            commandResponse = m_AssetProxy.onExecuteCommand(command);
        }

        m_Log.debug("Completed executing command [%s] for asset [%s]", command.getClass().getName(), getName());

        final Map<String, Object> props = new HashMap<>();
        props.put(EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, commandResponse.getClass().getName());
        props.put(EVENT_PROP_ASSET_COMMAND_RESPONSE, commandResponse);
        postEvent(TOPIC_COMMAND_RESPONSE, props);

        return commandResponse;
    }

    @Override
    public void setActiveStatus(final AssetActiveStatus status)
    {
        m_AssetActiveStatus = status;
    }

    @Override
    public AssetActiveStatus getActiveStatus()
    {
        return m_AssetActiveStatus;
    }

    @Override
    public String toString()
    {
        final StringBuilder builder = new StringBuilder();

        builder.append(super.toString());

        if (getLastStatus() != null)
        {
            final OperatingStatus summaryStatus = getLastStatus().getStatus().getSummaryStatus();
            builder.append(": ");
            builder.append(summaryStatus.getSummary());
            builder.append(":");
            builder.append(summaryStatus.getDescription());
        }

        return builder.toString();
    }

    @Override
    public void setStatus(final SummaryStatusEnum summaryStatus, final String summaryString)
    {
        try
        {
            setStatus(new Status().withSummaryStatus(new OperatingStatus(summaryStatus, summaryString)));
        }
        catch (final ValidationFailedException | PersistenceFailedException ex)
        {
            m_Log.error(ex, "Unable to set status for asset: [%s]", getName());
        }
    }

    @Override
    public void setStatus(final Status status) throws ValidationFailedException
    {
        if (status == null)
        {
            m_Log.error("New status for asset: [%s]. Status is null", getName());
        }
        else if (status.getSummaryStatus() == null)
        {
            m_Log.error("New status for asset: [%s]. Summary Status is null", getName());
        }
        else
        {
            m_Log.info("New status for asset: [%s]. Summary Status is [%s:%s]", getName(),
                    status.getSummaryStatus().getSummary(), status.getSummaryStatus().getDescription());
        }

        final Observation observation = new Observation();
        observation.setStatus(status);
        postStatusObservation(observation);
    }

    @Override
    public void persistObservation(final Observation observation)
            throws ValidationFailedException, PersistenceFailedException
    {
        setBaseObservationFields(observation);

        m_ObservationStore.persist(observation);
    }

    @Override
    public void postResponseUpdate(final Response response)
    {
        final Map<String, Object> props = new HashMap<>();
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE, response.getClass().getName());
        props.put(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE, response);

        postEvent(Asset.TOPIC_COMMAND_RESPONSE_UPDATED, props);
    }

    @Override
    public AssetAttributes getConfig()
    {
        return Configurable.createConfigurable(AssetAttributes.class, getProperties());
    }

    @Override
    public void configUpdated(final Map<String, Object> props) throws ConfigurationException
    {
        super.configUpdated(props);

        // update the position override property
        final boolean overrideProp = getConfig().pluginOverridesPosition();
        m_PluginOverridesPosition = overrideProp;
    }

    @Override
    public void setActivateOnStartUp(final boolean activate) throws FactoryException
    {
        final Map<String, Object> props = getProperties();
        props.put(AssetAttributes.CONFIG_PROP_ACTIVATE_ON_STARTUP, activate);
        setProperties(props);

        m_Log.info("Asset [%s] activate on startup successfully set to: %b", getName(), activate);
    }

    @Override
    public void setPluginOverridesPosition(final boolean override) throws FactoryException
    {
        final Map<String, Object> props = getProperties();
        props.put(AssetAttributes.CONFIG_PROP_PLUGIN_OVERRIDES_POSITION, override);
        setProperties(props);

        m_Log.info("Asset [%s] plug-in overrides position successfully set to: %b", getName(), override);
    }

    @Override
    public void delete()
    {
        if (getActiveStatus() != AssetActiveStatus.DEACTIVATED)
        {
            throw new IllegalStateException(String.format(
                    "Asset Active Status is %s, not DEACTIVATED, cannot remove %s.", getActiveStatus(), getName()));
        }

        super.delete();
    }

    /**
     * Method that posts that a status observation has occurred.
     * 
     * @param observation
     *            the observation that has the status to post
     * @throws ValidationFailedException
     *             thrown if the status observation is not valid
     * @throws PersistenceFailedException
     *             thrown if the status observations cannot be persisted by the observation store
     */
    private void postStatusObservation(final Observation observation)
            throws PersistenceFailedException, ValidationFailedException
    {
        persistObservation(observation);

        m_LastStatusObservation = observation;

        final Map<String, Object> props = new HashMap<>();
        if (observation.getStatus() != null && observation.getStatus().getSummaryStatus() != null)
        {
            final OperatingStatus summaryStatus = observation.getStatus().getSummaryStatus();
            props.put(EVENT_PROP_ASSET_STATUS_SUMMARY, summaryStatus.getSummary().toString());

            final String desc = summaryStatus.getDescription();
            if (desc != null)
            {
                props.put(EVENT_PROP_ASSET_STATUS_DESCRIPTION, desc);
            }
        }
        else
        {
            props.put(EVENT_PROP_ASSET_STATUS_SUMMARY, SummaryStatusEnum.UNKNOWN.toString());
        }

        props.put(EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID, observation.getUuid());

        postEvent(TOPIC_STATUS_CHANGED, props);
    }

    /**
     * Set the location.
     */
    private void restoreLocation()
    {
        try
        {
            m_AssetLocation = m_AssetDataManager.getCoordinates(getUuid());
            m_AssetOrientation = m_AssetDataManager.getOrientation(getUuid());
        }
        catch (final FactoryObjectInformationException e)
        {
            m_Log.warning("Unable to restore asset with UUID %s location.", getUuid().toString());
        }
    }

    /**
     * Clone a coordinates object.
     * 
     * @param coordinates
     *            the coordinates object to copy
     * @return cloned coordinates
     */
    private Coordinates cloneCoordinates(final Coordinates coordinates)
    {
        return new Coordinates(coordinates.getLongitude(), coordinates.getLatitude(), coordinates.getAltitude(),
                coordinates.getEllipseRegion());
    }

    /**
     * Clone an orientation object.
     * 
     * @param orientation
     *            the orientation object to copy
     * @return cloned orientation
     */
    private Orientation cloneOrientation(final Orientation orientation)
    {
        return new Orientation(orientation.getHeading(), orientation.getElevation(), orientation.getBank());
    }

    /**
     * Update the position of the asset.
     * 
     * @param location
     *            the coordinates information to update the asset's location to
     * @param orientation
     *            the orientation information to update the asset's orientation to
     * @return set position command response
     * @throws CommandExecutionException
     *             if the new location information could not be persisted
     */
    private synchronized Response updatePosition(final Coordinates location, final Orientation orientation)
            throws CommandExecutionException
    {

        // pull out position objects
        if (location != null)
        {
            m_AssetLocation = location;
        }
        if (orientation != null)
        {
            m_AssetOrientation = orientation;
        }

        try
        {
            if (m_AssetOrientation != null)
            {
                m_AssetDataManager.setOrientation(getUuid(), m_AssetOrientation);
            }
            if (m_AssetLocation != null)
            {
                m_AssetDataManager.setCoordinates(getUuid(), m_AssetLocation);
            }
        }
        catch (final FactoryObjectInformationException e)
        {
            throw new CommandExecutionException(
                    String.format("Unable to update asset %s for new location information!", getName()), e);
        }

        return new SetPositionResponse();
    }

    /**
     * Sets all the basic fields for an existing observations. Any of the basic fields that have already been filled out
     * will be overwritten excluding the observation UUID set via {@link Observation#setUuid(java.util.UUID)} and the
     * observation timestamp set via {@link Observation#setCreatedTimestamp(Long)}. Basic set accessors called are:
     * 
     * <ul>
     * <li>{@link Observation#setSystemId(int)} - set based on {@link TerraHarvestController#getId()}
     * <li>{@link Observation#setVersion(mil.dod.th.core.types.Version)} - set based on
     * {@link mil.dod.th.core.persistence.ObservationStore#getObservationVersion()}
     * <li>{@link Observation#setSystemInTestMode(boolean)} - set based on
     * {@link TerraHarvestController#getOperationMode()}
     * <li>{@link Observation#setAssetUuid(java.util.UUID)} - set to the asset's UUID
     * <li>{@link Observation#setAssetName(String)} - set to the current asset name
     * <li>{@link Observation#setAssetType(String)} - set to the
     * {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()}
     * <li>{@link Observation#setCreatedTimestamp(Long)} - set to the current time
     * <li>{@link Observation#setUuid(java.util.UUID)} - set to a random UUID
     * <li>{@link Observation#setAssetLocation(mil.dod.th.core.types.spatial.Coordinates)} - set if
     * {@link AssetAttributes#CONFIG_PROP_ACTIVATE_ON_STARTUP} is enabled
     * <li>{@link Observation#setAssetOrientation(mil.dod.th.core.types.spatial.Orientation)} - set if
     * {@link AssetAttributes#CONFIG_PROP_PLUGIN_OVERRIDES_POSITION} is enabled
     * </ul>
     * 
     * @param observation
     *            observation to set the basic fields of
     */
    private void setBaseObservationFields(final Observation observation)
    {
        observation.setSystemId(m_Controller.getId());
        observation.setVersion(m_ObservationStore.getObservationVersion());
        observation.setSystemInTestMode(m_Controller.getOperationMode() == OperationMode.TEST_MODE);
        observation.setAssetUuid(getUuid());

        if (!observation.isSetUuid())
        {
            observation.setUuid(UUID.randomUUID());
        }

        observation.setAssetName(getName());
        observation.setAssetType(getFactory().getProductType());

        if (!observation.isSetCreatedTimestamp())
        {
            observation.setCreatedTimestamp(System.currentTimeMillis());
        }

        if (m_AssetLocation != null && !m_PluginOverridesPosition)
        {
            observation.setAssetLocation(cloneCoordinates(m_AssetLocation));
        }

        if (m_AssetOrientation != null && !m_PluginOverridesPosition)
        {
            observation.setAssetOrientation(cloneOrientation(m_AssetOrientation));
        }
    }

    /**
     * Common deactivate asset functionality used by an on demand deactivation and during deactivation of the directory
     * service component.
     * 
     * @param listeners
     *            Listeners for the deactivation
     * @return Thread that has the {@link Deactivator} running
     */
    @Override
    public synchronized Thread internalDeactivate(final AssetActivationListener[] listeners)
    {
        if (getActiveStatus() != AssetActiveStatus.ACTIVATED)
        {
            throw new IllegalStateException(
                    String.format("Asset %s has an Active Status of [%s], must be %s in order to Deactivate.",
                            getName(), getActiveStatus(), AssetActiveStatus.ACTIVATED));
        }

        setActiveStatus(AssetActiveStatus.DEACTIVATING);

        postEvent(TOPIC_WILL_BE_DEACTIVATED, null);

        // kick off thread to deactivate
        final Deactivator runnable = new Deactivator(this, listeners);
        final Thread thread = new Thread(runnable);
        thread.setName(getName() + " Deactivator");
        thread.start();
        return thread;
    }
}
