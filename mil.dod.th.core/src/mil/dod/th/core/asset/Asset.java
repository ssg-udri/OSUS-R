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
package mil.dod.th.core.asset;

import aQute.bnd.annotation.ProviderType;
import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.FactoryDescriptor; // NOCHECKSTYLE: TH-2930 reference needed for event properties which 
import mil.dod.th.core.factory.FactoryException;  //               will be moved into base class of this interface
import mil.dod.th.core.factory.FactoryObject;
import mil.dod.th.core.observation.types.Observation;

/**
 * <p>
 * Interface for interacting with an asset (a physical device that typically produces data or responds to a command), 
 * such as a motion detector, camera, UAV, security gate, valve actuator, engine or radio. One asset may have multiple 
 * sensors. Every asset will have some kind of comms (radio, serial port, Ethernet port, ...) that the plug-in uses.
 * </p>
 *
 * <p>
 * All functionality of an asset needed by the consumer should be available through this interface. It should never be
 * necessary to use the {@link AssetProxy} interface implemented by the plug-in. For example, if SensorAsset implements
 * {@link AssetProxy}, consumers should not directly access SensorAsset methods, but instead access this interface.
 * </p>
 *
 * <p>
 * The main jobs of an asset are:
 * </p>
 * <ol>
 * <li>Turn on comms and generate/consume data when activated.</li>
 * <li>Handle all the protocol(s) for talking to the asset comms/hardware.</li>
 * <li>Provide items-of-interest to the rest of the system when collected.</li>
 * <li>Capture data to file on demand.</li>
 * <li>Turn off comms and sit when de-activated.</li>
 * </ol>
 *
 * <p>
 * Instances of an Asset are managed (created, tracked, deleted) by the core. This interface should never be implemented
 * by a plug-in. Instead, a plug-in implements {@link AssetProxy} to define custom behavior that is invoked when 
 * consumers use this interface. To interact with an asset, use the {@link AssetDirectoryService}.
 * </p>
 */
@ProviderType
public interface Asset extends FactoryObject
{
    /** 
     * Each {@link AssetProxy} implementation must provide a {@link org.osgi.service.component.ComponentFactory} with 
     * the factory attribute set to this constant.
     * 
     * <p>
     * For example:
     * 
     * <pre>
     * {@literal @}Component(factory = Asset.FACTORY)
     * public class MyAsset implements AssetProxy
     * {
     *     ...
     * </pre>
     */
    String FACTORY = "mil.dod.th.core.asset.Asset";
    
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/asset/Asset/";
    
    /** 
     * Topic used for when the asset will become activated.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this Asset represents 
     * (e.g., Asset)
     * </ul>
     */
    String TOPIC_WILL_BE_ACTIVATED = TOPIC_PREFIX + "WILL_BE_ACTIVATED";
    
    /** 
     * Topic used for when the asset has completed activation.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this Asset represents 
     * (e.g., Asset)
     * </ul>
     */
    String TOPIC_ACTIVATION_COMPLETE = TOPIC_PREFIX + "ACTIVATION_COMPLETE";
    
    /** 
     * Topic used for when the asset failed to activate.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this Asset represents 
     * (e.g., Asset)
     * </ul>
     */
    String TOPIC_ACTIVATION_FAILED = TOPIC_PREFIX + "ACTIVATION_FAILED";
    
    /** 
     * Topic used for when the asset will become deactivated.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this Asset represents 
     * (e.g., Asset)
     * </ul>
     */
    
    String TOPIC_WILL_BE_DEACTIVATED = TOPIC_PREFIX + "WILL_BE_DEACTIVATED";
    
    /** 
     * Topic used for when the asset has completed deactivation.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this Asset represents 
     * (e.g., Asset)
     * </ul>
     */
    String TOPIC_DEACTIVATION_COMPLETE = TOPIC_PREFIX + "DEACTIVATION_COMPLETE";
    
    /** Topic used for when the asset failed to deactivate.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this Asset represents 
     * (e.g., Asset)
     * </ul>
     */
    String TOPIC_DEACTIVATION_FAILED = TOPIC_PREFIX + "DEACTIVATION_FAILED";

    /** 
     * Topic used for when the asset captures data.
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object 
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Asset represents (e.g., Asset)
     * <li>{@link Asset#EVENT_PROP_ASSET_OBSERVATION_UUID} - the {@link java.util.UUID} of the {@link Observation} that
     * was created
     * </ul>
     */
    String TOPIC_DATA_CAPTURED = TOPIC_PREFIX + "DATA_CAPTURED";

    /** 
     * Topic used for when the asset status has changed. 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object 
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Asset represents (e.g., Asset)
     * <li>{@link Asset#EVENT_PROP_ASSET_STATUS_SUMMARY} - the Asset's
     * {@link mil.dod.th.core.types.status.SummaryStatusEnum} 
     * as a String
     * <li>{@link Asset#EVENT_PROP_ASSET_STATUS_DESCRIPTION} - the description of the status, if available, as a 
     * {@link String}
     * <li>{@link Asset#EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID} - the {@link java.util.UUID} of the status 
     * {@link Observation}
     * </ul>
     */
    String TOPIC_STATUS_CHANGED = TOPIC_PREFIX + "STATUS_CHANGED";
    
    /** 
     * Topic used for when the asset executes a command through a call to {@link #executeCommand(Command)}.
     * 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object 
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Asset represents (e.g., Asset)
     * <li>{@link Asset#EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE} - response type matching the executed command type
     * <li>{@link Asset#EVENT_PROP_ASSET_COMMAND_RESPONSE} - the actual command {@link Response}, the type of which is 
     * specified by the {@link Asset#EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE} event property
     * </ul>
     */
    String TOPIC_COMMAND_RESPONSE = TOPIC_PREFIX + "COMMAND_RESPONSE";
    
    /** 
     * Topic used for when the data within a {@link Response} has been updated. Typically this event only applies to 
     * "get" command responses as they will contain data while others don't.
     * 
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ} - the {@link Asset} object 
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the Asset
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - fully qualified class type of asset as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the {@link java.util.UUID} of the 
     * Asset object as a String
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_PID} - the PID of the Asset as a String,
     * may not be included if the Asset has no PID
     * <li>{@link FactoryDescriptor#EVENT_PROP_OBJ_BASE_TYPE} - the physical type that this 
     * Asset represents (e.g., Asset)
     * <li>{@link Asset#EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE} - response type that has updated data
     * <li>{@link Asset#EVENT_PROP_ASSET_COMMAND_RESPONSE} - the actual {@link Response} object containing updated data,
     * the type of which is specified by the {@link Asset#EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE} event property
     * </ul>
     * 
     * @see AssetContext#postResponseUpdate(Response)
     */
    String TOPIC_COMMAND_RESPONSE_UPDATED = TOPIC_PREFIX + "COMMAND_RESPONSE_UPDATED";

    /**
     * Event property key that contains the Asset's {@link mil.dod.th.core.types.status.SummaryStatusEnum} as a String.
     */
    String EVENT_PROP_ASSET_STATUS_SUMMARY = "asset.status.summary";

    /** 
     * Event property key for the asset status description. This property is a <code> String </code> and may not be 
     * present if there is no description for the status. 
     */
    String EVENT_PROP_ASSET_STATUS_DESCRIPTION = "asset.status.description";
    
    /**
     * Event property key containing the {@link java.util.UUID} of the latest status {@link Observation} created
     * by the Asset.
     */
    String EVENT_PROP_ASSET_STATUS_OBSERVATION_UUID = "asset.status.obs.uuid";
    
    /** Event property key for asset capability command response. */
    String EVENT_PROP_ASSET_COMMAND_RESPONSE = "asset.command.response";
    
    /** 
     * Event property key containing the fully qualified class name of {@link Response} type.
     */
    String EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE = "asset.command.response.type";

    /**
     * Event property key for the asset observation UUID. Only set for captured data, recorded data and made
     * observation.
     */
    String EVENT_PROP_ASSET_OBSERVATION_UUID = "asset.observation.uuid";

    /**
     * Request the asset to capture data. Capture data can mean something different for different assets. For example,
     * capture data for a camera would mean take a snapshot, where capture data for a motion detector would mean store
     * the current signal signature.
     *
     * <p>
     * This method will block until the data has been captured and returned by {@link AssetProxy#onCaptureData()} which
     * could take some time. If wanting to get data asynchronously, register for the {@link #TOPIC_DATA_CAPTURED} event 
     * instead.
     * 
     * <p>
     * It is not required for an asset to be activated in order to capture data. The exact behavior is left up to the 
     * plug-in implementation.
     * 
     * @return Observation that was captured, should not be null
     * @throws AssetException
     *      if the asset fails to create an observation from the data capture operation
     */
    Observation captureData() throws AssetException;
    
    /**
     * Get the last persisted status {@link Observation} containing information about the status of this asset.
     * If the status has not been established, a status {@link Observation} containing a 
     * {@link mil.dod.th.core.types.status.SummaryStatusEnum} with the value of 
     * {@link mil.dod.th.core.types.status.SummaryStatusEnum#UNKNOWN} will be returned.
     * 
     * @return
     *     latest status {@link Observation}
     */
    Observation getLastStatus();

    /**
     * Indicate if the asset is currently capturing data.
     *
     * @return true if the asset is capturing data, false otherwise
     * @see #captureData()
     */
    boolean isCapturingData();

   /**
     * Indicates if the asset is currently performing its BIT.
     *
     * @return true if the logical device is performing BIT, false otherwise
     * @see #performBit()
     */
    boolean isPerformingBit();

    /**
     * Request that the Asset performs a built-in test (BIT). The BIT is something performed on the Asset
     * itself to ensure the device is working properly.
     * 
     * <p>
     * This method will block until {@link AssetProxy#onPerformBit()} returns.
     *
     * @return 
     *      an Observation containing the status of the asset after BIT has been performed
     */
    Observation performBit();

    /**
     * Request the asset to execute an operation based on the specified command.
     * 
     * <p>
     * This method will block until the command has been executed and response returned by {@link 
     * AssetProxy#onExecuteCommand(Command)} which could take some time. If wanting to get the command response 
     * asynchronously, register for the {@link #TOPIC_COMMAND_RESPONSE} event instead.
     * 
     * <p>
     * It is not required for an asset to be activated in order to execute a command. The exact behavior is left up to 
     * the plug-in implementation.
     * 
     * @param command
     *          the command to execute
     * @return 
     *          response to the executed command
     * @throws CommandExecutionException
     *          if the execution of a command fails
     * @throws InterruptedException
     *          if during the execution of the command the calling thread is interrupted while processing the command
     */
    Response executeCommand(Command command) throws CommandExecutionException, InterruptedException;
    
    /**
     * Same as {@link FactoryObject#getFactory()}, but returns the asset specific factory.
     * 
     * @return
     *      factory for the asset
     */
    @Override
    AssetFactory getFactory();
    
    /**
     * Get the configuration for the asset.
     * 
     * <p>
     * <b>NOTE: the returned interface uses reflection to retrieve configuration and so values should be cached once 
     * retrieved</b>
     * 
     * @return
     *      configuration attributes for the asset
     */
    AssetAttributes getConfig();
    
    /**
     * Getter method for the {@link AssetActiveStatus}.
     * Get the activation status of a given asset.
     * 
     * <p>
     * The following list describes the possible asset states:
     * <ul>
     * <li>{@link AssetActiveStatus#DEACTIVATED}</li>
     * <li>{@link AssetActiveStatus#ACTIVATING}</li>
     * <li>{@link AssetActiveStatus#ACTIVATED}</li>
     * <li>{@link AssetActiveStatus#DEACTIVATING}</li>
     * </ul>
     * </p>
     * 
     * <p>
     * Status changes are also posted as OSGi events with the following topics:
     * <ul>
     * <li>{@link Asset#TOPIC_WILL_BE_ACTIVATED}</li>
     * <li>{@link Asset#TOPIC_ACTIVATION_COMPLETE}</li>
     * <li>{@link Asset#TOPIC_ACTIVATION_FAILED}</li>
     * <li>{@link Asset#TOPIC_WILL_BE_DEACTIVATED}</li>
     * <li>{@link Asset#TOPIC_DEACTIVATION_COMPLETE}</li>
     * <li>{@link Asset#TOPIC_DEACTIVATION_FAILED}</li>
     * </ul>
     * </p>
     * 
     * @return 
     *      the activation status of the Asset.
     */
    AssetActiveStatus getActiveStatus();
    
    /**
     * Activate the asset. Since activation may take some time, activation will take place on another thread. The asset 
     * must be in the {@link Asset.AssetActiveStatus#DEACTIVATED} state for this method to have any effect. The asset 
     * will post events as the status of the given asset changes. Assuming the asset can be activated, the first state 
     * the asset enters is the {@link Asset.AssetActiveStatus#ACTIVATING} state. If the asset completes activation, 
     * then it enters the {@link Asset.AssetActiveStatus#ACTIVATED} state. If instead the asset failed to activate, the 
     * asset returns to the {@link Asset.AssetActiveStatus#DEACTIVATED} state.
     * 
     * @throws IllegalStateException
     *      Asset is already activated so can't be activated again
     */
    void activateAsync() throws IllegalStateException;
    
    /**
     * Deactivate the asset. Since deactivation may take some time, deactivation will take place on another thread. The 
     * asset must be in the {@link Asset.AssetActiveStatus#ACTIVATED} state for this method to have any effect. The 
     * asset will notify registered listeners as the status of the given asset changes. Assuming the asset can be 
     * deactivated, the first state the asset enters is the {@link Asset.AssetActiveStatus#DEACTIVATING} state. If the 
     * asset completes deactivation, then it enters the {@link Asset.AssetActiveStatus#DEACTIVATED} state. If instead 
     * the asset failed to deactivated, the asset returns to the {@link Asset.AssetActiveStatus#ACTIVATED} state.
     * 
     * An asset must be in the {@link Asset.AssetActiveStatus#DEACTIVATED} state before calling {@link #delete()}.
     * 
     * @throws IllegalStateException
     *      Asset is currently deactivated so can't deactivate
     */
    void deactivateAsync() throws IllegalStateException;
    
    /**
     * Set whether the asset should be activated during directory initialization during startup.
     * 
     * @param activate
     *      true if the asset should be activated on startup.
     * @throws FactoryException
     *      if the core fails to store the property
     */
    void setActivateOnStartUp(boolean activate) throws FactoryException;
    
    /**
     * Set whether the plug-in provides its position.
     * 
     * @param override
     *     true if the plug-in provides its position, false if the core manages the position
     * @throws FactoryException
     *      if the core fails to store the property
     */
    void setPluginOverridesPosition(boolean override) throws FactoryException;
    
    /**
     * This enumeration defines each state that an asset may be in.
     */
    enum AssetActiveStatus
    {
        /**
         * The asset object is communicating with the physical asset.
         *
         * Asset is currently activated, {@link AssetProxy#onActivate()} completed successfully.
         */
        ACTIVATED,

        /**
         * The asset object is attempting to commence communications with its physical asset (this could take a while).
         */
        ACTIVATING,

        /**
         * The asset object is not communicating with the physical asset.
         *
         * Asset is currently deactivated, {@link AssetProxy#onDeactivate()} completed successfully.
         */
        DEACTIVATED,

        /**
         * The asset object is attempting to cease communications with its physical asset.
         */
        DEACTIVATING;
    }
}
