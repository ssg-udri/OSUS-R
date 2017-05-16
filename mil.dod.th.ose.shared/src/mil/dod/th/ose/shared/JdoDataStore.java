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
package mil.dod.th.ose.shared;

import java.util.Collection;

import javax.jdo.Query;

/**
 * A datastore that is backed by JDO (Java Data Objects).
 * 
 * @param <T>
 *      type of data persisted by the datastore
 *      
 * @author dhumeniuk
 */
public interface JdoDataStore<T>
{
    /**
     * Property constant key used for representing the type of datastore providing this interface.
     */
    String PROP_KEY_DATASTORE_TYPE = "datastore.type";
    
    /**
     * Property constant value used for representing the type of datastore providing this interface.
     */
    String PROP_PERSISTENT_STORE = "PersistentDataStore";
    
    /**
     * Property constant value used for representing the type of datastore providing this interface.
     */
    String PROP_OBSERVATION_STORE = "ObservationStore";
    
    /**
     * Get a new query object for the given data store. 
     * @return
     *      query to run
     */
    Query newJdoQuery();
    
    /**
     * Run the given JDO query.
     * 
     * @param query
     *            the specified query
     * @return the collection of T class objects returned by the query
     */
    Collection<T> executeJdoQuery(Query query);
    
    /**
     * Retrieves the total count of all rows of the specified T type.
     * 
     * @param query 
     *  the specified query
     * 
     * @return
     *  the number of rows that the data store has for the type T
     */
    long executeGetCount(Query query);
    
    /**
     * Remove based on a JDO query.
     * 
     * @param query
     *          the query used to remove
     * @return the number of items deleted
     */
    long removeOnJdoQuery(Query query);
}
