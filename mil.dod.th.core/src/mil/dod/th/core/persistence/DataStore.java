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

import java.util.Date;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.validator.ValidationFailedException;

/**
 * Common interface for persistent data storage services that support persisting data with a {@link UUID}.  Items can be
 * persisted, updated through a call to {@link #merge(Object)}, removed and queried.
 * <p>
 * In addition, this store supports the ability to determine how much space is available in the store and to compact the
 * size of the store if possible.
 * 
 * @param <T>
 *      the type of data that will be stored by this service
 *      
 * @author dhumeniuk
 *
 */
@ProviderType
public interface DataStore<T>
{
    /**
     * Check if the specified UUID correlates to any of the persistent data entries.
     * 
     * @param uuid
     *            the specified universally unique identifier
     * @return true if the specified UUID is found in the data store, false otherwise
     * @throws IllegalArgumentException
     *             if the UUID is null
     */
    boolean contains(UUID uuid) throws IllegalArgumentException;

    /**
     * Returns the managed data instance for the specified UUID.
     * 
     * @param uuid
     *            the specified universally unique identifier
     * @return the persistence managed data instance for the specified UUID, null if not found
     * @throws IllegalArgumentException
     *             if the UUID is null
     */
    T find(UUID uuid) throws IllegalArgumentException;

    /**
     * Merges the changes to this managed persistent data instance into memory. 
     * 
     * @param data
     *            the specified persistent data
     * @throws IllegalArgumentException
     *             if the specified data is null
     * @throws PersistenceFailedException
     *             if the merge failed
     * @throws ValidationFailedException
     *             if the data to be merged fails validation 
     */
    void merge(T data) throws IllegalArgumentException, PersistenceFailedException, ValidationFailedException;

    /**
     * Removes the specified persistence managed data instance from memory.
     * 
     * @param data
     *            the specified data instance
     * @throws IllegalArgumentException
     *             if data is null
     */
    void remove(T data) throws IllegalArgumentException;

    /**
     * Removes the specified persistence managed data instance from memory for the specified UUID.
     * 
     * @param uuid
     *          the specified universally unique identifier
     * @throws IllegalArgumentException
     *          if the UUID is null or no entry is found matching the UUID
     */
    void remove(UUID uuid) throws IllegalArgumentException;

    /**
     * Removes data instances stored during the time period framed by the specified start and stop times.
     * 
     * @param startTime
     *            the specified start time
     * @param stopTime
     *            the specified stop time
     * @throws IllegalArgumentException
     *             if the startTime, or stopTime is null, or if startTime is after stopTime
     */
    void remove(Date startTime, Date stopTime) throws IllegalArgumentException;

    /**
     * Get the usable space left for this persistent store.
     *   
     * @return
     *      the usable space (in bytes) left in the device/partition where data is persisted, return {@link 
     *      Long#MAX_VALUE} if infinite
     */
    long getUsableSpace();

    /**
     * Get how much usable space must be available for something to be persisted.  if {@link #getUsableSpace()} is less
     * then this amount, will not be able to persist data.  It is possible for the usable space to be less then this
     * amount if an item is persisted when the usable space is above the minimum bringing the space below the threshold.
     * 
     * @return
     *      how many bytes of space is required 
     */
    long getMinUsableSpace();
    
    /**
     * Compact the size of the backing data store.  This method is needed for data stores that don't automatically 
     * compact themselves and usable space is needed on the device containing the data store.
     * <p>
     * Method is <b>NOT</b> thread safe and should only be called when no other interaction with store is ongoing.
     * 
     * @throws PersistenceFailedException
     *      if the operation fails
     * @throws UnsupportedOperationException
     *      if the data store does not support compacting of data
     */
    void compact() throws PersistenceFailedException, UnsupportedOperationException;

    /**
     * Check if the {@link #compact()} operation is supported.
     * 
     * @return
     *      true if the compact operation is supported by the store, false if not
     */
    boolean isCompactingSupported();
}
