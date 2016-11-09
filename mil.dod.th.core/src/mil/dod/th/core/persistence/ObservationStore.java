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
package mil.dod.th.core.persistence;

import java.util.Collection;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.types.Version;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.validator.ValidationFailedException;

/**
 * <p>
 * Interface used to interact with the persistence context. The implementation of this should be registered as an OSGi
 * service.
 * </p>
 * <p>
 * An ObservationStore instance is associated with a persistence context. A persistence context is a set of observation
 * instances in which for any persistent observation identity there is a unique observation instance. Within the
 * persistence context, the observation instances and their life-cycle are managed. The ObservationStore API is used to
 * persist and remove persistent observation instances, to find observation instances by their UUID (universally unique
 * identifier), which should only be generated from a call to {@link java.util.UUID#randomUUID()}, and
 * to query over observation instances.
 * </p>
 * 
 * @author jconn
 */
@ProviderType
public interface ObservationStore extends DataStore<Observation>
{
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/persistence/ObservationStore/";
    
    /** 
     * Topic used for when the observation is successfully persisted. The following are the properties 
     * that will be set on the event with this topic.
     * <ul>
     * <li>{@link #EVENT_PROP_OBSERVATION_UUID} - the UUID of the persisted observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - the product type 
     *     of the asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the UUID of the 
     *     asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the asset 
     *     that produced the observation
     * <li>{@link #EVENT_PROP_SYS_ID} - the id of the system on which the observation was generated
     * <li>{@link #EVENT_PROP_OBSERVATION_TYPE} - the string representation of the {@link ObservationSubTypeEnum}
     *     of the observation that was persisted
     * <li>{@link #EVENT_PROP_SENSOR_ID} - the id of the sensor that generated the observation
     * <li>{@link #EVENT_PROP_SYS_IN_TEST_MODE} - whether or not the system was in test mode when the 
     *     observation is generated
     * </ul>
     */
    String TOPIC_OBSERVATION_PERSISTED = TOPIC_PREFIX + "OBSERVATION_PERSISTED";
    
    /** 
     *  Topic used for when the observation is successfully persisted and the observation is contained 
     *  in the event. The following are the properties that will be set on the event with this topic.
     * <ul>
     * <li>{@link #EVENT_PROP_OBSERVATION_UUID} - the UUID of the persisted observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - the product type 
     *     of the asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the UUID of the 
     *     asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the asset 
     *     that produced the observation
     * <li>{@link #EVENT_PROP_SYS_ID} - the id of the system on which the observation was generated
     * <li>{@link #EVENT_PROP_OBSERVATION_TYPE} - the string representation of the {@link ObservationSubTypeEnum}
     *     of the observation that was persisted
     * <li>{@link #EVENT_PROP_SENSOR_ID} - the id of the sensor that generated the observation
     * <li>{@link #EVENT_PROP_SYS_IN_TEST_MODE} - whether or not the system was in test mode when the 
     *     observation is generated
     * <li>{@link #EVENT_PROP_OBSERVATION} - the observation that was persisted
     * </ul>
     */
    String TOPIC_OBSERVATION_PERSISTED_WITH_OBS = TOPIC_PREFIX + "OBSERVATION_PERSISTED_WITH_OBS";
    
    /** 
     * Topic used for when the observation is successfully merged. The following are the properties 
     * that will be set on the event with this topic.
     *
     * <ul>
     * <li>{@link #EVENT_PROP_OBSERVATION_UUID} - the UUID of the merged observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - the product type 
     *     of the asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the UUID of the 
     *     asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the asset 
     *     that produced the observation
     * <li>{@link #EVENT_PROP_SYS_ID} - the id of the system on which the observation was generated
     * <li>{@link #EVENT_PROP_OBSERVATION_TYPE} - the string representation of the {@link ObservationSubTypeEnum}
     *     of the observation that was merged
     * <li>{@link #EVENT_PROP_SENSOR_ID} - the id of the sensor that generated the observation
     * <li>{@link #EVENT_PROP_SYS_IN_TEST_MODE} - whether or not the system was in test mode when the 
     *     observation is generated
     * </ul>
     */
    String TOPIC_OBSERVATION_MERGED = TOPIC_PREFIX + "OBSERVATION_MERGED";
    
    /** 
     * Topic used for when the observation is successfully merged and the observation is contained in the event. 
     * The following are the properties that will be set on the event with this topic.
     * 
     * <ul>
     * <li>{@link #EVENT_PROP_OBSERVATION_UUID} - the UUID of the merged observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_TYPE} - the product type 
     *     of the asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_UUID} - the UUID of the 
     *     asset that produced the observation
     * <li>{@link mil.dod.th.core.factory.FactoryDescriptor#EVENT_PROP_OBJ_NAME} - the name of the asset 
     *     that produced the observation
     * <li>{@link #EVENT_PROP_SYS_ID} - the id of the system on which the observation was generated
     * <li>{@link #EVENT_PROP_OBSERVATION_TYPE} - the string representation of the {@link ObservationSubTypeEnum}
     *     of the observation that was merged
     * <li>{@link #EVENT_PROP_SENSOR_ID} - the id of the sensor that generated the observation
     * <li>{@link #EVENT_PROP_SYS_IN_TEST_MODE} - whether or not the system was in test mode when the 
     *     observation is generated
     * <li>{@link #EVENT_PROP_OBSERVATION} - the observation that was merged
     * 
     * </ul>
     */
    String TOPIC_OBSERVATION_MERGED_WITH_OBS = TOPIC_PREFIX + "OBSERVATION_MERGED_WITH_OBS";
    
    /**
     * Event property key for observation {@link UUID}.
     */
    String EVENT_PROP_OBSERVATION_UUID = "observation.uuid";
    
    /** 
     * Event property key for an {@link Observation}. 
     */
    String EVENT_PROP_OBSERVATION = "observation";

    /**
     * Event property key for the integer ID of the system that the observation has been generated on.
     */
    String EVENT_PROP_SYS_ID = "system.id";
    
    /**
     * Event property key for the string representation of {@link ObservationSubTypeEnum}. If no type applies
     * the value of this property will be {@link ObservationSubTypeEnum#NONE}.
     */
    String EVENT_PROP_OBSERVATION_TYPE = "observation.type";
    
    /**
     * Event property key for the string id of the sensor.
     */
    String EVENT_PROP_SENSOR_ID = "sensor.id";
    
    /**
     * Event property key for a boolean which indicates whether or not the system was in test mode when the 
     * observation was made.
     */
    String EVENT_PROP_SYS_IN_TEST_MODE = "system.in.test.mode";

    /**
     * Persists an observation instance from the specified observation. The observation is validated against a schema
     * before being persisted.
     * 
     * @param observation
     *            the asset observation to be validated and persisted
     * @throws IllegalArgumentException
     *            if the specified validated observation is null
     * @throws PersistenceFailedException
     *            if the persist failed
     * @throws ValidationFailedException
     *            if the observation is not valid
     */
    void persist(Observation observation) throws IllegalArgumentException, ValidationFailedException,
            PersistenceFailedException;
    
    /**
     * Create a new query object to query or remove observations.  Initially the query will return observations in 
     * descending order (using {@link Observation#getCreatedTimestamp()} and can be changed by calling {@link 
     * ObservationQuery#withOrder(mil.dod.th.core.persistence.ObservationQuery.SortField,
     * mil.dod.th.core.persistence.ObservationQuery.SortOrder)}.
     *  
     * @return
     *      new query object
     */
    ObservationQuery newQuery();
    
    /**
     * Removes all persistence managed observation instances from memory for the specified asset instance.
     * 
     * @param asset
     *            the specified asset instance
     * @return
     *            number of observations removed
     * @throws NullPointerException
     *             if the asset is null
     */
    long removeByAsset(Asset asset) throws NullPointerException;

    /**
     * Removes all persistence managed observation instances from memory for a specific asset type.
     * 
     * @param assetType
     *            the fully qualified class name string of the originating Asset
     * @return
     *            number of observations removed
     * @throws NullPointerException
     *             if the assetType is null
     */
    long removeByAssetType(String assetType) throws NullPointerException;

    /**
     * Removes all persistence managed observation instances from memory for a specific asset.
     * 
     * @param assetUuid
     *            the asset UUID of the originating Asset
     * @return
     *            number of observations removed
     * @throws NullPointerException
     *             if the assetUuid is null
     */
    long removeByAssetUuid(UUID assetUuid) throws NullPointerException;

    /**
     * Removes all persistence managed observation instances from memory for a specific observation sub-type.
     * 
     * @param observationSubType
     *            the specified {@link mil.dod.th.core.types.observation.ObservationSubTypeEnum}
     * @return
     *            number of observations removed
     * @throws NullPointerException
     *             if the observationSubType is null
     */
    long removeBySubType(ObservationSubTypeEnum observationSubType) throws NullPointerException;
    
    /**
     * Removes all persistence managed observation instances from memory for a specific system ID.
     * 
     * @param systemId
     *      the specified system ID
     * @return
     *      the number of observations removed
     * @throws NullPointerException
     *      if the systemId is null
     */
    long removeBySystemId(Integer systemId) throws NullPointerException;

    /**
     * Query for managed observation instances by the specified asset instance. If the observation instances are
     * contained in the persistence context, they are returned from there. The returned number of observations
     * are ordered by time stamp and newest observations are in the beginning of the list.
     * 
     * @param asset
     *            the asset instance
     * @return the collection of managed persistence observation instances for the specified asset instance
     * @throws NullPointerException
     *             if the asset is null
     */
    Collection<Observation> queryByAsset(Asset asset) throws NullPointerException;

    /**
     * Query for managed observation instances by the specified asset type. If the observation instances are contained
     * in the persistence context, they are returned from there. The returned number of observations
     * are ordered by time stamp and newest observations are in the beginning of the list
     * 
     * @param assetType
     *            the fully qualified class name of the asset
     * @return the collection of managed persistent observation instances for the specified asset type
     * @throws NullPointerException
     *             if the assetType is null
     */
    Collection<Observation> queryByAssetType(String assetType) throws NullPointerException;
    
    /**
     * Query for managed observation instances by the specified asset UUID. If the observation instances are contained 
     * in the persistence context, they are returned from there. The returned number of observations are ordered by time
     * stamp and newest observations are in the beginning of the list
     * 
     * @param assetUuid
     *            the UUID of the originating Asset
     * @return the collection of managed persistent observation instances for the specific asset
     * @throws NullPointerException
     *             if the assetUuid is null
     */
    Collection<Observation> queryByAssetUuid(UUID assetUuid) throws NullPointerException;
    
    /**
     * Query for managed observation instances by the specified observation sub-type. If the observation instances are
     * contained in the persistence context, they are returned from there. The returned number of observations
     * are ordered by time stamp and newest observations are in the beginning of the list.
     * 
     * @param observationSubType
     *            the specified {@link mil.dod.th.core.types.observation.ObservationSubTypeEnum}
     * @return the collection of managed persistent observation instances for the specified observation type
     * @throws NullPointerException
     *             if the observationSubType is null
     */
    Collection<Observation> queryBySubType(ObservationSubTypeEnum observationSubType) throws NullPointerException;
    
    /**
     * Query for managed observation instances by the specified system ID. If the observation instances are contained in
     * the persistence context, they are returned from there. The returned number of observations are ordered by time 
     * stamp and newest observations are in the beginning of the list.
     * @param systemId
     *      the system id 
     * @return
     *      the collection of managed persistent observation instances for the specified system id.
     * @throws NullPointerException
     *      if the systemID is null
     */
    Collection<Observation> queryBySystemId(Integer systemId) throws NullPointerException;

    /**
     * Get the current version of all new observations. The store will attempt to maintain old observations, as
     * such queries may contain observations that are an older version. 
     * Version number: 
     *     - Major number will increase if there have been new required fields added.
     *     - Minor number will increase if additional optional fields are added.
     * @return
     *     an object which contains the major and minor version numbers
     */
    Version getObservationVersion();
}
