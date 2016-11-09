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
import java.util.Date;
import java.util.UUID;

/**
 * <p>
 * The PersistentData interface is the public interface to the underlying non public implementation.
 * </p>
 * <p>
 * Exposed are the following members:
 * </p>
 * <ul>
 * <li>The timestamp relative to the origination of the data.</li>
 * <li>The universally unique identifier used as a key for persistence managed data.</li>
 * <li>The data description string used to describe the data.</li>
 * <li>The context string which is the fully qualified class name that uniquely identifies the data.<li>
 * <li>The serializable persistent entity.</li>
 * </ul>
 * 
 * @author jconn
 * 
 */
public class PersistentData
{
    /**
     * The persistence entity.
     */
    private Serializable entity; // NOCHECKSTYLE: field name used in store

    /**
     * The queryable description string of the persistence managed data.
     */
    private String description; // NOCHECKSTYLE: field name used in store
    
    /**
     * The queryable fully qualified class name which uniquely identifies the data.
     */
    private String context; // NOCHECKSTYLE: field name used in store; NOPMD: cannot be final breaks persistence

    /**
     * The universally unique identifier.
     */
    private UUID uuid; // NOCHECKSTYLE: field name used in store; NOPMD: cannot be final breaks persistence

    /**
     * Long timestamp is used for data persistence instead of the Date, and represents the Date's timestamp in
     * milliseconds.
     */
    private Long timestamp; // NOCHECKSTYLE: field name used in store; NOPMD: cannot be final breaks persistence

    /**
     * Constructor.
     * 
     * @param uuid
     *            the specified universally unique identifier
     * @param description
     *            the queryable description string of the persistence managed data
     * @param context
     *            the queryable symbolic name of the data's originating OSGi bundle
     * @param entity
     *            the persistent entity
     */
    public PersistentData(final UUID uuid, // NOCHECKSTYLE: hides a field, but this name makes most sense
                       final String description, // NOCHECKSTYLE: hides a field, but this name makes most sense
                       final String context, // NOCHECKSTYLE: hides a field, but this name makes most sense
                       final Serializable entity) // NOCHECKSTYLE: hides a field, but this name makes most sense
    {
        timestamp = new Date().getTime();
        this.uuid = uuid;
        this.description = description;
        this.context = context;
        this.entity = entity;
    }

    /**
     * Returns the time relative to the origination of the data for the backing object instance.
     * 
     * @return the time relative to the origination of the data for the backing object instance
     */
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Returns this data instance's universally unique identifier used as a key for persistence managed data.
     * 
     * @return - the value of this AssetData instance's UUID
     */
    public UUID getUUID()
    {
        return uuid;
    }

    /**
     * Returns the data description set within this persistence managed data instance.
     * 
     * @return the queryable description string of the persistence managed data
     */
    public String getDescription()
    {
        return description;
    }
    
    /**
     * Returns the queryable fully qualified class name which uniquely identifies the data.
     * 
     * @return the queryable fully qualified class name which uniquely identifies the data
     */
    public String getContext()
    {
        return context; 
    }

    /**
     * Returns the corresponding persistence entry reference within this persistence managed data instance.
     * 
     * @return the serializable persistence entry
     */
    public Serializable getEntity()
    {
        return entity;
    }
    
    /**
     * Used to set the data description within this persistence managed data instance.
     * 
     * @param description
     *          the specified description
     */
    public void setDescription(final String description) // NOCHECKSTYLE: hides a field, but this name makes most sense
    {
        this.description = description;
    }
    
    /**
     * Used to set the corresponding persistence entry reference within this persistence managed data instance.
     * 
     * @param entity
     *          the serializable persistence entry
     */
    public void setEntity(final Serializable entity) // NOCHECKSTYLE: hides a field, but this name makes most sense
    {
        this.entity = entity;
    }
}
