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

import java.util.Map;

import mil.dod.th.core.asset.commands.Command;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.commands.CommandExecutionException;
import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;

/**
 * An asset plug-in implements this interface providing any custom behavior of the plug-in that is needed to interact
 * with the physical asset. The core will perform some functions automatically such as validating incoming commands and 
 * observations produced by the plug-in.
 * 
 * <p>
 * For observations produced by the plug-in, the following accessors will be called automatically to set basic fields:
 * 
 * <ul>
 * <li>{@link Observation#setSystemId(int)} - set based on {@link mil.dod.th.core.system.TerraHarvestSystem#getId()}
 * <li>{@link Observation#setVersion(mil.dod.th.core.types.Version)} - set based on {@link 
 * mil.dod.th.core.persistence.ObservationStore#getObservationVersion()}
 * <li>{@link Observation#setSystemInTestMode(boolean)} - set based on {@link 
 * mil.dod.th.core.controller.TerraHarvestController#getOperationMode()}
 * <li>{@link Observation#setAssetUuid(java.util.UUID)} - set to the asset's UUID
 * <li>{@link Observation#setAssetName(String)} - set to the current asset name
 * <li>{@link Observation#setAssetType(String)} - set to the {@link 
 * mil.dod.th.core.factory.FactoryDescriptor#getProductType()}
 * <li>{@link Observation#setCreatedTimestamp(Long)} - set to the current time <b>IF</b> not already set by the plug-in
 * <li>{@link Observation#setUuid(java.util.UUID)} - set to a random UUID <b>IF</b> not already set by the plug-in
 * <li>{@link Observation#setAssetLocation(mil.dod.th.core.types.spatial.Coordinates)} - set if {@link 
 * AssetAttributes#pluginOverridesPosition()} is <b>NOT</b> set
 * <li>{@link Observation#setAssetOrientation(mil.dod.th.core.types.spatial.Orientation)} - set if {@link 
 * AssetAttributes#pluginOverridesPosition()} is <b>NOT</b> set
 * </ul>
 * 
 * @author dhumeniuk
 */
public interface AssetProxy extends FactoryObjectProxy
{
    /**
     * Called to initialize the object and provide the plug-in with the {@link AssetContext} to interact with the rest
     * of the system.
     * 
     * @param context
     *      context specific to the {@link Asset} instance
     * @param props
     *      the asset's configuration properties, available as a convenience so {@link AssetContext#getProperties()}
     *      does not have to be called
     * @throws FactoryException
     *      if there is an error initializing the object.
     */
    void initialize(AssetContext context, Map<String, Object> props) throws FactoryException;
    
    /**
     * The {@link AssetDirectoryService} will call this method to activate this asset as necessary. Activation may have
     * different meanings to various assets, but generally, the object should begin communications with the physical 
     * asset to initiate control and receive status.  It is assumed that threading will be handled outside of this 
     * method call, do not use another thread to perform the activation. It is okay to take a while to activate, but 
     * this method should return in a reasonable amount of time ( &lt;10 seconds). 
     * 
     * @throws AssetException
     *      if the asset could not be activated
     */
    void onActivate() throws AssetException;

    /**
     * The {@link AssetDirectoryService} will call this method to deactivate this asset as necessary. Deactivation may
     * have different meanings to various assets, but generally, the object should cease communications with the 
     * physical asset.  It is assumed that threading will be handled outside of this method call, do not use another 
     * thread to perform the deactivation. It is okay to take a while to deactivate, but this method should return in a 
     * reasonable amount of time ( &lt;10 seconds). 
     * 
     * @throws AssetException
     *      if the asset could not be deactivated
     */
    void onDeactivate() throws AssetException;
    
    /**
     * Implement to perform data capture for the asset.
     * 
     * @return
     *      observation that was captured, core will set additional fields and validate data
     * @throws AssetException
     *      if the asset fails to capture data or the operation is not supported
     * @see Asset#captureData()
     */
    Observation onCaptureData() throws AssetException;

    /**
     * Implement to perform data capture for the asset based on the given sensor ID.
     * 
     * @param sensorId
     *      capture data for sensor corresponding to this ID
     * @return
     *      observation that was captured, core will set additional fields and validate data
     * @throws AssetException
     *      if the asset fails to capture data or the operation is not supported
     * @see Asset#captureData(String)
     */
    default Observation onCaptureData(String sensorId) throws AssetException
    {
        throw new AssetException(
            new UnsupportedOperationException("Asset does not support capturing data by sensorId."));
    }

    /**
     * Override to perform built-in test (BIT) on the asset.
     * 
     * @return
     *      the status of the asset after performing BIT (must not be null)
     * @throws AssetException
     *      if the asset fails to perform BIT or the operation is not supported
     * @see Asset#performBit()
     */
    Status onPerformBit() throws AssetException; 
    
    /**
     * Implement to execute commands received by the asset.  Command passed has already been validated against a schema.
     * 
     * @param command
     *      the specified validated command to execute
     * @return
     *      the command response
     * @throws CommandExecutionException
     *      if the execution of the command fails
     * @throws InterruptedException 
     *      if during the execution of the command the calling thread is interrupted while processing the command
     * @see Asset#executeCommand(Command)
     */
    Response onExecuteCommand(Command command)
            throws CommandExecutionException, InterruptedException;
}
