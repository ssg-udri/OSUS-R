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
package mil.dod.th.ose.core.impl.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.jdo.Query;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.shared.JdoDataStore;

/**
 * Implementation for observation queries.
 * 
 * @author dhumeniuk
 *
 */
public class ObservationQueryImpl implements ObservationQuery
{
    /**
     * Format string used for building a time range query.
     */
    private static String TIME_RANGE_FILTER_FORMAT = "%s >= %d && %s <= %d";

    /**
     * Data store used for {@link Observation}s.
     */
    private JdoDataStore<Observation> m_DataStore;

    /**
     * Asset UUID to use for the filter.
     */
    private UUID m_AssetUuid;

    /**
     * Fully qualified class name of the asset type to include in the query.
     */
    private String m_AssetType;

    /**
     * Set of observation sub types to filter on.
     */
    private Set<ObservationSubTypeEnum> m_SubTypes = new TreeSet<>();
    
    /**
     * OSUS System ID to use for the filter.
     */
    private Integer m_SystemId;

    /**
     * Start observed date to filter on.
     */
    private Date m_ObservedStartDate;

    /**
     * Stop observed date to filter on.
     */
    private Date m_ObservedStopDate;

    /**
     * Start created date to filter on.
     */
    private Date m_CreatedStartDate;

    /**
     * Stop created date to filter on.
     */
    private Date m_CreatedStopDate;

    /**
     * Start range to include results for the query.
     */
    private Integer m_FromRangeInclusive;

    /**
     * Stop range to include results for the query.
     */
    private Integer m_ToRangeExclusive;

    /**
     * Field to use for ordering the query, not valid for {@link #remove()}.
     */
    private SortField m_SortField;

    /**
     * Order to use for the query, not valid for {@link #remove()}.
     */
    private SortOrder m_SortOrder;
    
    /**
     * Base constructor to inject a persistence manager and extent.
     * 
     * @param dataStore
     *      data store to use for querying
     */
    public ObservationQueryImpl(final JdoDataStore<Observation> dataStore)
    {
        Preconditions.checkNotNull(dataStore);
        m_DataStore = dataStore;
    }

    @Override
    public ObservationQuery withAsset(final Asset asset) throws IllegalStateException
    {
        Preconditions.checkNotNull(asset);
        return withAssetUuid(asset.getUuid());
    }

    @Override
    public ObservationQuery withAssetUuid(final UUID assetUuid) throws IllegalStateException
    {
        Preconditions.checkNotNull(assetUuid);
        Preconditions.checkState(m_AssetUuid == null || m_AssetUuid == assetUuid, 
                "Query already restricted to a different asset UUID: " + assetUuid);
        Preconditions.checkState(m_AssetType == null, "Query already restriced to an asset type: " + m_AssetType);
        
        m_AssetUuid = assetUuid;
        return this;
    }

    @Override
    public ObservationQuery withAssetType(final String assetType) throws IllegalStateException
    {
        Preconditions.checkNotNull(assetType);
        Preconditions.checkState(m_AssetUuid == null, 
                "Query already restricted to a specific asset UUID: " + m_AssetUuid);
        Preconditions.checkState(m_AssetType == null || m_AssetType == assetType,
                "Query already restricted to a different asset type: " + assetType);
        
        m_AssetType = assetType;
        return this;
    }

    @Override
    public ObservationQuery withSubType(final ObservationSubTypeEnum subType)
    {
        Preconditions.checkNotNull(subType);
        m_SubTypes.add(subType);
        return this;
    }

    @Override
    public ObservationQuery withTimeObservedRange(final Date start, final Date stop)
    {
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(stop);
        Preconditions.checkArgument(validateDataRange(start, stop));
        
        m_ObservedStartDate = start;
        m_ObservedStopDate = stop;
        return this;
    }

    @Override
    public ObservationQuery withTimeCreatedRange(final Date start, final Date stop)
    {
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(stop);
        Preconditions.checkArgument(validateDataRange(start, stop));
        
        m_CreatedStartDate = start;
        m_CreatedStopDate = stop;
        return this;
    }

    @Override
    public ObservationQuery withRange(final int fromInclusive, final int toExclusive) throws IllegalStateException
    {
        Preconditions.checkArgument(fromInclusive <= toExclusive);
        
        m_FromRangeInclusive = fromInclusive;
        m_ToRangeExclusive = toExclusive;
        return this;
    }

    @Override
    public ObservationQuery withMaxObservations(final int number) throws IllegalStateException
    {
        m_FromRangeInclusive = 0;
        m_ToRangeExclusive = number;
        return this;
    }

    @Override
    public ObservationQuery withOrder(final SortField field, final SortOrder order)
    {
        Preconditions.checkNotNull(field);
        Preconditions.checkNotNull(order);
        
        m_SortField = field;
        m_SortOrder = order;
        return this;
    }
    
    @Override
    public ObservationQuery withSystemId(final int systemId)
    {
        m_SystemId = systemId;
        return this;
    }
    
    @Override
    public long getCount()
    {
        final Query query = getBaseJdoQuery();
        
        if (m_FromRangeInclusive != null && m_ToRangeExclusive != null)
        {
            query.setRange(m_FromRangeInclusive, m_ToRangeExclusive);
        }
        
        return m_DataStore.executeGetCount(query);
    }

    @Override
    public Collection<Observation> execute()
    {
        final Query query = getBaseJdoQuery();
        // default to created timestamp field
        final SortField field = Objects.firstNonNull(m_SortField, SortField.CreatedTimestamp);
        // default to descending
        final SortOrder order = Objects.firstNonNull(m_SortOrder, SortOrder.Descending);
        query.setOrdering(String.format("%s %s", field.getJdoFieldName(), order.toString().toLowerCase()));
        
        if (m_FromRangeInclusive != null && m_ToRangeExclusive != null)
        {
            query.setRange(m_FromRangeInclusive, m_ToRangeExclusive);
        }
        
        return m_DataStore.executeJdoQuery(query);
    }

    @Override
    public long remove()
    {
        // order and ranges are not allowed when removing
        Preconditions.checkState(m_SortField == null);
        Preconditions.checkState(m_SortOrder == null);
        Preconditions.checkState(m_FromRangeInclusive == null);
        Preconditions.checkState(m_ToRangeExclusive == null);
        
        return m_DataStore.removeOnJdoQuery(getBaseJdoQuery());
    }
    
    /**
     * Get the JDO query given the observation query.
     * 
     * @return
     *      JDO query
     */
    private Query getBaseJdoQuery()
    {
        final Query query = m_DataStore.newJdoQuery();
        
        final List<String> filterStrings = new ArrayList<>();
        if (m_AssetUuid != null)
        {
            filterStrings.add(String.format("assetUuid == '%s'", m_AssetUuid));
        }
        else if (m_AssetType != null)
        {
            filterStrings.add(String.format("assetType == '%s'", m_AssetType));
        }
        
        for (ObservationSubTypeEnum subType : m_SubTypes)
        {
            filterStrings.add(String.format("this.%s.id > 0", subType.value()));
        }

        if (m_ObservedStartDate != null && m_ObservedStopDate != null)
        {
            final String jdoFieldName = SortField.ObservedTimestamp.getJdoFieldName();
            filterStrings.add(String.format(TIME_RANGE_FILTER_FORMAT, jdoFieldName, m_ObservedStartDate.getTime(),
                    jdoFieldName, m_ObservedStopDate.getTime()));
        }

        if (m_CreatedStartDate != null && m_CreatedStopDate != null)
        {
            final String jdoFieldName = SortField.CreatedTimestamp.getJdoFieldName();
            filterStrings.add(String.format(TIME_RANGE_FILTER_FORMAT, jdoFieldName, m_CreatedStartDate.getTime(),
                    jdoFieldName, m_CreatedStopDate.getTime()));
        }

        if (m_SystemId != null)
        {
            filterStrings.add(String.format("systemId == %s", m_SystemId));
        }
        
        
        if (filterStrings.size() > 0)
        {
            final String completeFilter = Joiner.on(" && ").join(filterStrings);
            query.setFilter(completeFilter);
        }
        
        return query;
    }
    
    /**
     * Verifies that the date range specified is valid. Start date must be less than or equal to the stop date.
     * 
     * @param start
     *      Start date.
     * @param stop
     *      Stop date.
     * @return
     *      True if the start date is less than or equal to the stop date. False otherwise.
     */
    private boolean validateDataRange(final Date start, final Date stop)
    {
        if (start.before(stop) || start.equals(stop))
        {
            return true;
        }
        return false;
    }
}
