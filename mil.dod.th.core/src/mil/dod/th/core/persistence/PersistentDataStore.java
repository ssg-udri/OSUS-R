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

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

/**
 * <p>
 * Interface used to interact with the persistence context. The implementations of this should be registered as an OSGi
 * service.
 * </p>
 * <p>
 * A PersistentDataStore instance is associated with a persistence context. A persistence context is a set of managed
 * persistent data instances in which for any persistent data identity there is a unique data instance. Within the
 * persistence context, the persistent data instances and their life-cycle are managed. The PersistentDataStore API is
 * used to persist and remove persistent data instances, to find persistent data instances by their their primary key
 * uuid), and to query over persistent data instances.
 * </p>
 * <p>
 * The description string field of the persistent data is not only used to describe the date upon inspection, but is
 * also used to query against. It is recommended these strings be as unique as possible, to provide better filtering
 * when querying from the PersistentDataStore.
 * </p>
 * 
 * @author jconn
 */
@ProviderType
public interface PersistentDataStore extends DataStore<PersistentData>
{
    /** Event topic prefix to use for all topics in this interface. */
    String TOPIC_PREFIX = "mil/dod/th/core/persistence/PersistentDataStore/";

    /** Topic used for when persistent data is successfully persisted. */
    String TOPIC_DATA_PERSISTED = TOPIC_PREFIX + "DATA_PERSISTED";
    
    /** Topic used for when persistent data is successfully merged. */
    String TOPIC_DATA_MERGED = TOPIC_PREFIX + "DATA_MERGED";

    /** Event property key for data context. */
    String EVENT_PROP_DATA_CONTEXT = "data.context";

    /** Event property key for data description. */
    String EVENT_PROP_DATA_DESCRIPTION = "data.description";

    /** Event property key for data uuid. */
    String EVENT_PROP_DATA_UUID = "data.uuid";

    /**
     * Create and persist a {@link PersistentData} instance from the specified context, UUID, description and persistent
     * entity.
     * <p>
     * The uuid parameter should be generated using {@link java.util.UUID#randomUUID()}.
     * </p>
     * @param context
     *            the specified context which uniquely identifies the data being persisted (shouldn't use Object.class)
     * @param uuid
     *            the specified universally unique identifier generated from {@link java.util.UUID#randomUUID()}
     * @param description
     *            the queryable description string of the managed persistent data
     * @param entity
     *            the persistent entity
     * @return the managed persistent data reference
     * @throws IllegalArgumentException
     *             if any argument is null
     * @throws PersistenceFailedException
     *             if the persist failed
     */
    PersistentData persist(Class<?> context,
                           UUID uuid,
                           String description,
                           Serializable entity)
            throws IllegalArgumentException, PersistenceFailedException;

    /**
     * Removes all managed persistent data instances from memory matching the specified context.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @throws IllegalArgumentException
     *             if the description is null
     */
    void removeMatching(Class<?> context)
            throws IllegalArgumentException;

    /**
     * Removes all managed persistent data instances from memory matching the specified context.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @param startTime
     *            the specified start time
     * @param stopTime
     *            the specified stop time
     * @throws IllegalArgumentException
     *             if the description is null
     */
    void removeMatching(Class<?> context,
                        Date startTime,
                        Date stopTime)
            throws IllegalArgumentException;

    /**
     * Removes all managed persistent data instances from memory matching the specified context.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @param description
     *            the specified description string of the managed persistent data
     * @throws IllegalArgumentException
     *             if the description is null
     */
    void removeMatching(Class<?> context,
                        String description)
            throws IllegalArgumentException;

    /**
     * Removes all managed persistent data instances from memory matching the specified context.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @param description
     *            the specified description string of the managed persistent data
     * @param startTime
     *            the specified start time
     * @param stopTime
     *            the specified stop time
     * @throws IllegalArgumentException
     *             if the description is null
     */
    void removeMatching(Class<?> context,
                        String description,
                        Date startTime,
                        Date stopTime)
            throws IllegalArgumentException;
    
    /**
     * Query for managed persistent data instances by the specified description.
     * 
     * If the persistent data instances are contained in the persistence context, and match based on the defined data
     * context, they are returned.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @return the collection of persistence managed persistent data instances for the specified persistent type
     * @throws IllegalArgumentException
     *             if the context is null
     */
    Collection<PersistentData> query(Class<?> context)
            throws IllegalArgumentException;

    /**
     * Query for managed persistent data instances by the specified data context and description.
     * 
     * If the persistent data instances are contained in the persistence context, and match based on the defined data
     * description, they are returned.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @param description
     *            the queryable description string of the managed persistent data
     * @return the collection of persistence managed persistent data instances for the specified persistent type
     * @throws IllegalArgumentException
     *             if the context, or description, is null
     */
    Collection<PersistentData> query(Class<?> context,
                                               String description)
            throws IllegalArgumentException;
    
    /**
     * Query for managed persistent data instances by the specified data context, stored during the time period
     * framed by the specified start and stop times.
     * 
     * If the persistent data instances are contained in the persistence context, and match based on the defined data
     * context and time range, they are returned.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @param startTime
     *            the specified start time
     * @param stopTime
     *            the specified stop time
     * @return the collection of persistence managed persistent data instances for the specified context
     * @throws IllegalArgumentException
     *             if any argument is null, or startTime is after stopTime
     */
    Collection<PersistentData> query(Class<?> context,
                                               Date startTime,
                                               Date stopTime)
            throws IllegalArgumentException;

    /**
     * Query for managed persistent data instances by the specified context and description, stored during the time
     * period framed by the specified start and stop times.
     * 
     * If the persistent data instances are contained in the persistence context, and match based on the defined
     * context, description, and time range, they are returned.
     * 
     * @param context
     *            the specified context which uniquely identifies the data being persisted
     * @param description
     *            the queryable description string of the managed persistent data
     * @param startTime
     *            the specified start time
     * @param stopTime
     *            the specified stop time
     * @return the collection of persistence managed persistent data instances for the specified context and description
     * @throws IllegalArgumentException
     *             if any argument is null, or startTime is after stopTime
     */
    Collection<PersistentData> query(Class<?> context,
                                               String description,
                                               Date startTime,
                                               Date stopTime)
            throws IllegalArgumentException;


}
