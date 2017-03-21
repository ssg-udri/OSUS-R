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
    void setStatus(final SummaryStatusEnum summaryStatus, final String statusMessage);

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
    void setStatus(final String sensorId, final SummaryStatusEnum summaryStatus, final String statusMessage);

    /**
     * Update the current asset status.  Will post an OSGi event with the topic {@link Asset#TOPIC_STATUS_CHANGED} and 
     * persist an observation containing the provided {@link Status} object plus basic fields (see {@link AssetProxy}).
     * 
     * @param status
     *      complete details of the asset's status
     * @throws ValidationFailedException
     *      if the status observation contains invalid data
     */
    void setStatus(final Status status) throws ValidationFailedException;

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
    void setStatus(final String sensorId, final Status status) throws ValidationFailedException;

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
    void postResponseUpdate(final Response response);
}
