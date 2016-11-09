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
import java.util.Date;
import java.util.UUID;

import aQute.bnd.annotation.ProviderType;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;

/**
 * <p>
 * Query interface to find observations using {@link ObservationStore#newQuery()}. The interface provides different 
 * methods to refine the search.
 * </p>
 * For example, to get the 10th thru 19th (zero-based) oldest observations (10 total) from the asset 
 * <code>queryAsset</code>:
 * <pre>
 * Asset queryAsset; // get from directory service
 * ObservationStore obsStore; // inject service
 * Collection<Observation> obs = 
 *  obsStore.newQuery().withAsset(queryAsset).withRange(10, 19).withOrder(SortOrder.Ascending).execute();
 * </pre>
 * 
 * @author dhumeniuk
 *
 */
@ProviderType
public interface ObservationQuery
{
    /**
     * This is the equivalent of calling <code>withAssetUuid(asset.getUuid())</code>.
     * 
     * @param asset
     *      the asset that all observations queried will be produced by
     * @return
     *      the updated query object
     * @throws IllegalStateException
     *      if the query is already restricted by another asset or asset type
     */
    ObservationQuery withAsset(Asset asset) throws IllegalStateException;
    
    /**
     * Restrict all observations queried to the asset with a specific UUID.
     * 
     * @param assetUuid
     *      the asset UUID as returned by {@link Asset#getUuid()} that all observations queried will be produced by
     * @return
     *      the updated query object
     * @throws IllegalStateException
     *      if the query is already restricted by another asset or asset type
     */
    ObservationQuery withAssetUuid(UUID assetUuid) throws IllegalStateException;
    
    /**
     * Restrict all observations queried to the assets of the given type.
     * 
     * @param assetType
     *      product type of the asset as returned by {@link mil.dod.th.core.factory.FactoryDescriptor#getProductType()} 
     *      that all observations queried will be produced by
     * @return
     *      the updated query object
     * @throws IllegalStateException
     *      if the query is already restricted by a specific asset or another asset type
     */
    ObservationQuery withAssetType(String assetType) throws IllegalStateException;
    
    /**
     * <p>
     * Restrict all observations queried to the observations containing the given sub-type. If a query previously 
     * restricted to a sub-type can be further restricted by calling this method again.
     * </p>
     * For example, to query for detections containing the asset's own coordinates, use:
     * <pre>
     * ObservationQuery query;
     * query = query.withSubType(ObservationSubType.DETECTION).withSubType(ObservationSubType.COORDINATES);
     * </pre>
     * 
     * @param subType
     *      specific subType of observation that must be included in the observation
     * @return
     *      the updated query object
     */
    ObservationQuery withSubType(ObservationSubTypeEnum subType);

    /**
     * <p>
     * Limit the range of observations retrieved from the store to the given observed time range.  The observed time
     * stamp is an optional field set by plug-ins and only observations with that field set can be returned. Uses
     * {@link mil.dod.th.core.observation.types.Observation#getObservedTimestamp()} to compare observations.
     * </p>
     * <p>
     * This can be used in combination with {@link #withTimeCreatedRange(Date, Date)} if both ranges should be used.
     * </p>
     * <p>
     * Subsequent calls to this method will replace the previous range.
     * </p>
     * 
     * @param start
     *      earliest observed time of observation to include
     * @param stop
     *      latest observed time of observation to include
     * @return
     *      the updated query object
     * @throws IllegalArgumentException
     *      if the start time is after the stop time
     */
    ObservationQuery withTimeObservedRange(Date start, Date stop) throws IllegalArgumentException;

    /**
     * <p>
     * Limit the range of observations retrieved from the store to the given created time range. Uses {@link 
     * mil.dod.th.core.observation.types.Observation#getCreatedTimestamp()} to compare observations.
     * </p>
     * <p>
     * This can be used in combination with {@link #withTimeObservedRange(Date, Date)} if both ranges should be used.
     * </p>
     * <p>
     * Subsequent calls to this method will replace the previous range.
     * </p>
     * 
     * @param start
     *      earliest created time of observation to include
     * @param stop
     *      latest created time of observation to include
     * @return
     *      the updated query object
     * @throws IllegalArgumentException
     *      if the start time is after the stop time
     */
    ObservationQuery withTimeCreatedRange(Date start, Date stop) throws IllegalArgumentException;

    /**
     * <p>
     * Limit the range of observations retrieved from the store to the given range based on {@link 
     * #withOrder(SortField, SortOrder)}.
     * </p>
     * The following would skip the 10 newest observations and retrieve the next 10 newest (as descending order is 
     * default): 
     * <pre>
     * ObservationQuery query;
     * query.withRange(10, 20);
     * </pre>
     * The following would skip the 5 oldest observations and retrieve the next 5 oldest:
     * <pre>
     * ObservationQuery query;
     * query.withRange(5, 10).withOrder(SortField.CreatedTimestamp, SortOrder.Ascending);
     * </pre>
     * <p>
     * Subsequent calls to this method will replace the previous range. If {@link #withMaxObservations(int)} was 
     * previously called, the max will no longer apply.
     * </p>
     * Since this method gets a specific range of observations, the observations returned could be different for the 
     * same requested range as there could be newer observations.  Therefore, you would not be able to guarantee the
     * following will retrieve the 20 newest observations as their might be additional observations made between query 
     * calls.
     * <pre>
     * ObservationQuery query1;
     * ObservationQuery query2;
     * query1.withRange(0,10).execute();
     * // some additional observations could be made here, second query could contain some of the same observations
     * query2.withRange(10,20).execute();
     * </pre>
     * <p>
     * The range should not be set if the query is used to remove {@link Observation}s and will result in an exception 
     * (see {@link #remove()}).
     * </p>
     * 
     * @param fromInclusive
     *      zero based inclusive index of the first observation to retrieve
     * @param toExclusive
     *      zero based exclusive index of the last observation to retrieve, or {@link Long#MAX_VALUE} for no limit
     * @return
     *      the updated query object
     * @throws IllegalArgumentException
     *      if the start value is larger than the stop value
     */
    ObservationQuery withRange(int fromInclusive, int toExclusive) throws IllegalArgumentException;
    
    /**
     * <p>
     * Limit the number of observations retrieved from the store based on {@link #withOrder(SortField, SortOrder)}.
     * </p>
     * The following would retrieve the 100 newest observations (as descending order is default): 
     * <pre>
     * ObservationQuery query;
     * query.withMaxObservations(100);
     * </pre>
     * The following would retrieve the 20 oldest observations: 
     * <pre>
     * ObservationQuery query;
     * query.withMaxObservations(20).withOrder(SortOrder.Ascending);
     * </pre>
     * <p>
     * Subsequent calls to this method will replace the previous max.  If {@link #withRange(int, int)} was previously 
     * called, the range will no longer apply.
     * </p>
     * <p>
     * The max should not be set if the query is used to remove {@link Observation}s and will result in an exception 
     * (see {@link #remove()}).
     * </p>
     * 
     * @param number
     *      maximum number of observations to retrieve
     * @return
     *      the updated query object
     */
    ObservationQuery withMaxObservations(int number);
    
    /**
     * <p>
     * Set the order of the retrieved observations using the given field name parameter. This will affect which
     * observations are returned if used in combination with {@link #withRange(int, int)} or
     * {@link #withMaxObservations(int)}.
     * </p>
     * <p>
     * By default, the order will be descending (newest observations first) on the created timestamp field.
     * </p>
     * <p>
     * Subsequent calls to this method will replace the previous sort order.
     * </p>
     * <p>
     * The order should not be set if the query is used to remove {@link Observation}s and will result in an exception 
     * (see {@link #remove()}).
     * </p>
     * 
     * @param field
     *      attribute from {@link Observation} to sort on
     * @param order
     *      order to sort the provided {@link Observation} attribute
     * @return
     *      the updated query object
     */
    ObservationQuery withOrder(SortField field, SortOrder order);
    
    /** 
     * <p>
     * Restrict all observations queried to the observations containing the given system ID.
     * </p>
     * For example, to query for observations from the controller with system id 42, use:
     * <pre>
     * ObservationQuery query;
     * query = query.withSystemId(42);
     * </pre>
     * 
     * @param systemId
     *      the system ID to restrict retrieved observations from
     * @return
     *      the updated query object
     */
    ObservationQuery withSystemId(int systemId);
    
    /**
     * Run the query.
     * 
     * @return
     *      the observation matching the query
     */
    Collection<Observation> execute();
    
    /**
     * Remove the observations that match the query from the data store.
     * 
     * @return
     *      number of observations removed
     * @throws IllegalStateException
     *      if the order, range or max have been set (which is not allowed)
     */
    long remove() throws IllegalStateException;
    
    /**
     * Returns the total count of all the observations currently known
     * based on the filters set. If count of all observations is desired, 
     * then do not set filters. Also, setting sort order has no bearing on
     * results.
     * 
     * @return
     *  the total number of observations.
     */
    long getCount();

    /**
     * Sorting field used to determine which observation field the returned observations are sorted by.
     */
    enum SortField
    {
        /** Sort by the created timestamp field. */
        CreatedTimestamp("createdTimestamp"),

        /** Sort by the observed timestamp field. */
        ObservedTimestamp("observedTimestamp");

        /** JDO representation of the value. */
        private String m_JdoFieldName;

        /** 
         * Sort field enumeration constructor.
         * 
         * @param jdoFieldName
         *      JDO representation
         */
        SortField(final String jdoFieldName)
        {
            m_JdoFieldName = jdoFieldName;
        }
        
        /**
         * Get the JDO field name of the sort field.
         * 
         * @return
         *      JDO representation
         */
        public String getJdoFieldName()
        {
            return m_JdoFieldName;
        }
    }

    /**
     * Sorting order used for setting the order of the queried items retrieved from the store.
     */
    enum SortOrder
    {
        /** Sort ascending (e.g., 0, 1, 2, 3 or A, B, C). */
        Ascending,
        /** Sort descending (e.g., 9, 8, 7, 6 or Z, Y, X). */
        Descending
    }
}
