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
package mil.dod.th.core.datastream;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.asset.Asset;

/**
 * <p>
 * The Data Stream Service is responsible for managing configuration profiles from assets
 * that support streaming data.
 * 
 * <p>
 * Using the service, clients can do the following:
 *
 * <ul>
 * <li>Get set of streaming configuration profiles from all assets
 * <li>Get set of streaming configuration profiles from a particular asset
 * <li>Enable or disable streaming data for a particular streaming configuration profile
 * </ul>
 *  
 * 
 * @author jmiller
 *
 */
@ProviderType
public interface DataStreamService 
{
    
    
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/datastream/DataStreamService/";
    
    /** Event property key for the {@link StreamProfile} instance. */
    String EVENT_PROP_STREAM_PROFILE = "stream.profile";
    
    /** Event property key for the enabled state of the {@link StreamProfile} instance,
     * as a {@link java.lang.Boolean}. */
    String EVENT_PROP_STREAM_PROFILE_ENABLED = "stream.profile.enabled";
    
    /** 
     * Topic used for when the state of a {@link StreamProfile} has changed (i.e. from
     * enabled to disabled, or vice versa)
     * The following properties will be included in the event as applicable:
     * <ul>
     * <li>{@link #EVENT_PROP_STREAM_PROFILE} - the {@link StreamProfile} object
     * <li>{@link #EVENT_PROP_STREAM_PROFILE_ENABLED} - the enabled state of the 
     * {@link StreamProfile} object as a {@link java.lang.Boolean}
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the UUID of the associated
     * Asset, as a String.
     * </ul>
     */
    String TOPIC_STREAM_PROFILE_STATE_CHANGED = TOPIC_PREFIX + "STREAM_PROFILE_STATE_CHANGED";
    
    /**
     * Create, name, set properties and add a new stream profile to the directory. This directory will 
     * create a new stream profile instance and return it if one could be created.
     * 
     * @param productType
     *      Fully qualified class name of the type of stream profile to be created as returned
     *      by {@link StreamProfileFactory#getProductType()}.
     * @param name
     *      The name to give to the stream profile. If the name is a duplicate of another 
     *      stream profile's name then an exception  will be thrown and the stream profile will not be created.
     * @param properties
     *      Properties to use for the new {@link StreamProfile}.
     * @return
     *      The newly created stream profile.
     * @throws StreamProfileException
     *      If the service is unable to add the given stream profile.
     * @throws IllegalArgumentException
     *      If invalid or duplicate name is given.
     */
    StreamProfile createStreamProfile(String productType, String name, Map<String, Object> properties)
            throws StreamProfileException, IllegalArgumentException;

    /**
     * Returns the streaming configuration profile associated with a given unique ID.
     * Note: Calling the {@link StreamProfile#getUuid()} on the returned object will
     *       yield the same value as the id parameter for this method.
     * 
     * @param profileId the UUID of the profile
     * @return the profile object
     * @throws IllegalArgumentException if no profile with a matching UUID is found.
     */
    StreamProfile getStreamProfile(UUID profileId) throws IllegalArgumentException;
    
    /**
     * Retrieves all streaming configuration profiles associated with a particular asset.
     * 
     * @param asset the reference to the asset
     * @return set of streaming configuration profiles
     */
    Set<StreamProfile> getStreamProfiles(Asset asset);
    
    /**
     * Retrieves all streaming configuration profiles from all assets.
     * 
     * @return set of streaming configuration profiles
     */
    Set<StreamProfile> getStreamProfiles();
    
    /**
     * Get a set of factories that describe StreamProfile plugins registered with the directory service.
     * 
     * @return Set of all StreamProfile factory descriptors.
     */
    Set<StreamProfileFactory> getStreamProfileFactories();
}
