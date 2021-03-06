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

import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.factory.FactoryObjectContext;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.types.spatial.Coordinates;
import mil.dod.th.core.types.spatial.Orientation;
import mil.dod.th.core.types.status.SummaryStatusEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * This is the context of the {@link Asset} that is made available to implementors of {@link AssetProxy}. Each instance 
 * of an {@link Asset} will have a matching context to allow the plug-in to interact with the rest of the system.
 * 
 * @author dhumeniuk
 */
@ProviderType
public interface AssetContext extends Asset, FactoryObjectContext
{
    /**
     * Returns the current asset location. This method is only valid if the asset plug-in does not override position.
     * 
     * @return asset {@link Coordinates} or null if location has not been set
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    Coordinates getPositionLocation() throws IllegalStateException;

    /**
     * Returns the current location of a specific sensor. This method is only valid if the asset plug-in does not
     * override position.
     * 
     * @param sensorId
     *      sensor identifier for a sensor provided by the asset
     * @return {@link Coordinates} of a sensor provided by the asset or null if location has not been set
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    Coordinates getPositionLocation(String sensorId) throws IllegalStateException;

    /**
     * Update the current asset location. This method is only valid if the asset plug-in does not override position.
     * 
     * @param location
     *      new location of the asset
     * @throws AssetException
     *      thrown if the new location could not be persisted
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    void setPositionLocation(Coordinates location) throws AssetException, IllegalStateException;

    /**
     * Update the current location of a specific sensor. This method is only valid if the asset plug-in does not
     * override position.
     * 
     * @param sensorId
     *      sensor identifier for a sensor provided by the asset
     * @param location
     *      new location for a sensor
     * @throws AssetException
     *      thrown if the new location could not be persisted
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    void setPositionLocation(String sensorId, Coordinates location) throws AssetException, IllegalStateException;

    /**
     * Returns the current asset orientation. This method is only valid if the asset plug-in does not override position.
     * 
     * @return asset {@link Orientation} or null if orientation has not been set
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    Orientation getPositionOrientation() throws IllegalStateException;

    /**
     * Returns the current orientation of a specific sensor. This method is only valid if the asset plug-in does not
     * override position.
     * 
     * @param sensorId
     *      sensor identifier for a sensor provided by the asset
     * @return {@link Orientation} of a sensor provided by the asset or null if orientation has not been set
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    Orientation getPositionOrientation(String sensorId) throws IllegalStateException;

    /**
     * Update the current asset orientation. This method is only valid if the asset plug-in does not override position.
     * 
     * @param orientation
     *      new orientation of the asset
     * @throws AssetException
     *      thrown if the new orientation could not be persisted
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    void setPositionOrientation(Orientation orientation) throws AssetException, IllegalStateException;

    /**
     * Update the current orientation of a specific sensor. This method is only valid if the asset plug-in does not
     * override position.
     * 
     * @param sensorId
     *      sensor identifier for a sensor provided by the asset
     * @param orientation
     *      new orientation for a sensor
     * @throws AssetException
     *      thrown if the new orientation could not be persisted
     * @throws IllegalStateException
     *      thrown if the asset plug-in overrides position data managed by the core
     */
    void setPositionOrientation(String sensorId, Orientation orientation) throws AssetException, IllegalStateException;

    /**
     * Update the current asset status.  Will post an OSGi event with the topic {@link Asset#TOPIC_STATUS_CHANGED} and 
     * persist an observation containing a {@link Status} object with only the summary status included plus basic 
     * fields (see {@link AssetProxy}).
     * 
     * @param summaryStatus
     *      high level summary of the asset's status
     * @param statusMessage
     *      descriptive string relating to the summaryStatus
     *      
     * @see #setStatus(Status)
     */
    void setStatus(SummaryStatusEnum summaryStatus, String statusMessage);

    /**
     * Update the current asset status for a specific sensor.  Will post an OSGi event with the topic
     * {@link Asset#TOPIC_STATUS_CHANGED} and persist an observation containing a {@link Status} object with only the
     * summary status included plus basic fields (see {@link AssetProxy}).
     * 
     * @param sensorId
     *      sensor identifier for a sensor provided by the asset
     * @param summaryStatus
     *      high level summary of the asset's status
     * @param statusMessage
     *      descriptive string relating to the summaryStatus
     *      
     * @see #setStatus(String, Status)
     */
    void setStatus(String sensorId, SummaryStatusEnum summaryStatus, String statusMessage);

    /**
     * Update the current asset status.  Will post an OSGi event with the topic {@link Asset#TOPIC_STATUS_CHANGED} and 
     * persist an observation containing the provided {@link Status} object plus basic fields (see {@link AssetProxy}).
     * 
     * @param status
     *      complete details of the asset's status
     * @throws ValidationFailedException
     *      if the status observation contains invalid data
     */
    void setStatus(Status status) throws ValidationFailedException;

    /**
     * Update the current asset status for a specific sensor.  Will post an OSGi event with the topic
     * {@link Asset#TOPIC_STATUS_CHANGED} and persist an observation containing the provided {@link Status} object plus
     * basic fields (see {@link AssetProxy}).
     * 
     * @param sensorId
     *      sensor identifier for a sensor provided by the asset
     * @param status
     *      complete details of the asset's status
     * @throws ValidationFailedException
     *      if the status observation contains invalid data
     */
    void setStatus(String sensorId, Status status) throws ValidationFailedException;

    /**
     * Persist the observation to the {@link mil.dod.th.core.persistence.ObservationStore}.
     * 
     * @param observation
     *      observation to persist, core will set additional fields (see {@link AssetProxy}) and validate data
     * @throws ValidationFailedException
     *      thrown observation to be persisted fails validation
     * @throws PersistenceFailedException
     *      thrown if the observation cannot be persisted
     */
    void persistObservation(Observation observation) throws ValidationFailedException, PersistenceFailedException;

    /**
     * Merge the observation to the {@link mil.dod.th.core.persistence.ObservationStore}.
     * 
     * @param observation
     *      observation to merge, no additional fields are set or updated
     * @throws ValidationFailedException
     *      thrown if the observation to be merged fails validation
     * @throws PersistenceFailedException
     *      thrown if the observation cannot be merged
     */
    void mergeObservation(Observation observation) throws ValidationFailedException, PersistenceFailedException;

    /**
     * If data contained within a {@link Response} object changes, this method can be called to post the change 
     * asynchronously.  For example, if the pan of a camera changes without being commanded, a {@link 
     * mil.dod.th.core.asset.commands.GetPanTiltResponse} would be posted.
     * 
     * <p>
     * Will post an event with the topic {@link Asset#TOPIC_COMMAND_RESPONSE_UPDATED}. 
     * 
     * @param response
     *      response data to use for the update post
     */
    void postResponseUpdate(Response response);
}
