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

import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

import javax.jdo.Query;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationQuery.SortField;
import mil.dod.th.core.persistence.ObservationQuery.SortOrder;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.shared.JdoDataStore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class TestObservationQueryImpl
{
    private static final String ASSET_UUID_FILTER = "assetUuid == '%s'";
    private ObservationQueryImpl m_SUT;
    private JdoDataStore<Observation> m_DataStore;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception
    {
        // mock query object that will be used
        Query query = mock(Query.class);
        m_DataStore = mock(JdoDataStore.class);
        when(m_DataStore.newJdoQuery()).thenReturn(query);
        
        m_SUT = new ObservationQueryImpl(m_DataStore);
    }
    
    /**
     * Verify execute passes JDO query with the default order of descending and no filtering or range is set.
     */
    @Test
    public void testExecuteDefaultQuery()
    {
        // mock
        Observation obs1 = new Observation().withCreatedTimestamp(100L);
        Observation obs2 = new Observation().withCreatedTimestamp(200L);
        Collection<Observation> expectedObs = new ArrayList<>(Arrays.asList(obs1, obs2));
        when(m_DataStore.executeJdoQuery(Mockito.any(Query.class))).thenReturn(expectedObs);
        
        // replay
        Collection<Observation> actualObs = m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify order
        verify(query.getValue()).setOrdering("createdTimestamp descending");
        // verify no range
        verify(query.getValue(), never()).setRange(anyLong(), anyLong());
        // verify no filter
        verify(query.getValue(), never()).setFilter(anyString());
        
        assertThat(actualObs, contains(obs1, obs2));
    }
    
    /**
     * Verify remove passes JDO query with the default order of descending and no filtering or range is set.
     */
    @Test
    public void testRemoveDefaultQuery()
    {
        // mock
        when(m_DataStore.removeOnJdoQuery(Mockito.any(Query.class))).thenReturn(32828L);
        
        // replay
        long obsRemoved = m_SUT.remove();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).removeOnJdoQuery(query.capture());
        
        // verify no order
        verify(query.getValue(), never()).setOrdering(anyString());
        // verify no range
        verify(query.getValue(), never()).setRange(anyLong(), anyLong());
        // verify no filter
        verify(query.getValue(), never()).setFilter(anyString());
        
        assertThat(obsRemoved, is(32828L));
    }
    
    /**
     * Verify trying to remove with an order set will cause an exception.
     */
    @Test
    public void testRemoveWithOrderSet()
    {
        m_SUT.withOrder(SortField.CreatedTimestamp, SortOrder.Descending);
        
        try
        {
            m_SUT.remove();
            fail("Expecting exception because order was set");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify trying to remove with a range set will cause an exception.
     */
    @Test
    public void testRemoveWithRangeSet()
    {
        m_SUT.withRange(5, 10);
        
        try
        {
            m_SUT.remove();
            fail("Expecting exception because range was set");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify trying to remove with a max set will cause an exception.
     */
    @Test
    public void testRemoveWithMaxSet()
    {
        m_SUT.withMaxObservations(20);
        
        try
        {
            m_SUT.remove();
            fail("Expecting exception because max was set");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
    
    /**
     * Verify setting order to created time and ascending sets the ordering of the JDO query properly.
     */
    @Test
    public void testSetOrderingAscending()
    {
        // replay
        ObservationQuery rv = m_SUT.withOrder(SortField.CreatedTimestamp, SortOrder.Ascending);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setOrdering("createdTimestamp ascending");
    }
    
    /**
     * Verify setting order to created time and descending sets the ordering of the JDO query properly.
     */
    @Test
    public void testSetOrderingDescending()
    {
        // replay
        ObservationQuery rv = m_SUT.withOrder(SortField.CreatedTimestamp, SortOrder.Descending);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setOrdering("createdTimestamp descending");
    }
    
    /**
     * Verify setting order to observed time and ascending sets the ordering of the JDO query properly.
     */
    @Test
    public void testSetOrderingObservedAscending()
    {
        // replay
        ObservationQuery rv = m_SUT.withOrder(SortField.ObservedTimestamp, SortOrder.Ascending);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setOrdering("observedTimestamp ascending");
    }
    
    /**
     * Verify setting order to observed time and descending sets the ordering of the JDO query properly.
     */
    @Test
    public void testSetOrderingObservedDescending()
    {
        // replay
        ObservationQuery rv = m_SUT.withOrder(SortField.ObservedTimestamp, SortOrder.Descending);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setOrdering("observedTimestamp descending");
    }
    
    /**
     * Verify calling with asset will set the filter properly.
     */
    @Test
    public void testWithAsset()
    {
        // mock
        UUID uuid = UUID.randomUUID();
        Asset asset = mock(Asset.class);
        when(asset.getUuid()).thenReturn(uuid);
        
        // replay
        ObservationQuery rv = m_SUT.withAsset(asset);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter(String.format(ASSET_UUID_FILTER, uuid));
    }
    
    /**
     * Verify calling with asset UUID will set the filter properly.
     */
    @Test
    public void testWithAssetUuid()
    {
        // mock
        UUID uuid = UUID.randomUUID();
        
        // replay
        ObservationQuery rv = m_SUT.withAssetUuid(uuid);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter(String.format(ASSET_UUID_FILTER, uuid));
    }
    
    /**
     * Verify if the asset UUID is already set that subsequent calls to set the id or type will fail.
     */
    @Test
    public void testAssetUuidAlreadySet()
    {
        // mock
        UUID uuid1 = UUID.randomUUID();
        Asset asset1 = mock(Asset.class);
        when(asset1.getUuid()).thenReturn(uuid1);
        
        // replay
        m_SUT.withAssetUuid(uuid1);
        
        // replay with the same UUID, should pass
        m_SUT.withAssetUuid(uuid1);
        m_SUT.withAsset(asset1);
        
        // replay each asset method and verify it fails
        UUID uuid2 = UUID.randomUUID();
        try
        {
            m_SUT.withAssetUuid(uuid2);
            fail("Expecting exception since already restriced to a different asset");
        }
        catch (IllegalStateException e)
        {
        }
        
        try
        {
            Asset asset2 = mock(Asset.class);
            when(asset2.getUuid()).thenReturn(uuid2);
            m_SUT.withAsset(asset2);
            fail("Expecting exception since already restriced to a different asset");
        }
        catch (IllegalStateException e)
        {
        }
        
        try
        {
            m_SUT.withAssetType("anything");
            fail("Expecting exception since already restriced to a specific asset, no point in restricting to a type");
        }
        catch (IllegalStateException e)
        {
        }
    }
    
    /**
     * Verify calling with asset type will set the filter properly.
     */
    @Test
    public void testWithAssetType()
    {
        // replay
        ObservationQuery rv = m_SUT.withAssetType("some type");
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter("assetType == 'some type'");
    }
    
    /**
     * Verify if the asset type is already set that subsequent calls to set the id or type will fail.
     */
    @Test
    public void testAssetTypeAlreadySet()
    {
        // replay
        m_SUT.withAssetType("something");
        
        // replay with the same type, should pass
        m_SUT.withAssetType("something");
        
        // replay each asset method and verify it fails
        try
        {
            m_SUT.withAssetType("something else");
            fail("Expecting exception since already restriced to a different asset type");
        }
        catch (IllegalStateException e)
        {
        }
        
        UUID uuid = UUID.randomUUID();
        try
        {
            m_SUT.withAssetUuid(uuid);
            fail("Expecting exception since already restriced to an asset type");
        }
        catch (IllegalStateException e)
        {
        }
        
        try
        {
            Asset asset = mock(Asset.class);
            when(asset.getUuid()).thenReturn(uuid);
            m_SUT.withAsset(asset);
            fail("Expecting exception since already restriced to an asset type");
        }
        catch (IllegalStateException e)
        {
        }
    }
    
    /**
     * Verify calling with observation type will set the filter properly.
     */
    @Test
    public void testWithObservationSubTypeEnum()
    {
        // replay
        ObservationQuery rv = m_SUT.withSubType(ObservationSubTypeEnum.DETECTION);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter("this.detection.id > 0");
    }
    
    /**
     * Verify multiple sub types can be added and the filter has them all.
     */
    @Test
    public void testWithMultipleObservationSubTypeEnums()
    {
        // replay with additional types
        m_SUT.withSubType(ObservationSubTypeEnum.DETECTION)
             .withSubType(ObservationSubTypeEnum.AUDIO_METADATA);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify both types are there anded together
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(query.getValue()).setFilter(stringCaptor.capture());
        // might be in either order
        assertThat(stringCaptor.getValue(), anyOf(is("this.detection.id > 0 && this.audioMetadata.id > 0"),
                                                  is("this.audioMetadata.id > 0 && this.detection.id > 0")));
    }

    /**
     * Verify an observed time range will produce a valid filter.
     */
    @Test
    public void testTimeObservedRange()
    {
        // replay
        ObservationQuery rv = m_SUT.withTimeObservedRange(new Date(200), new Date(300));
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter("observedTimestamp >= 200 && observedTimestamp <= 300");
        
        rv = m_SUT.withTimeObservedRange(new Date(500), new Date(500));
        m_SUT.execute();
        
        query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore, times(2)).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter("observedTimestamp >= 500 && observedTimestamp <= 500");
    }

    /**
     * Verify a created time range will produce a valid filter.
     */
    @Test
    public void testTimeCreatedRange()
    {
        // replay
        ObservationQuery rv = m_SUT.withTimeCreatedRange(new Date(200), new Date(300));
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter("createdTimestamp >= 200 && createdTimestamp <= 300");
        
        rv = m_SUT.withTimeCreatedRange(new Date(500), new Date(500));
        m_SUT.execute();
        
        verify(m_DataStore, times(2)).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setFilter("createdTimestamp >= 500 && createdTimestamp <= 500");
    }

    /**
     * Verify an invalid observed or created time range will cause an exception
     */
    @Test
    public void testInvalidTimeRanges()
    {
        try
        {
            m_SUT.withTimeObservedRange(new Date(500), new Date(499));
            fail("Expecting exception as observed start date is after the stop date");
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            m_SUT.withTimeCreatedRange(new Date(500), new Date(499));
            fail("Expecting exception as created start date is after the stop date");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Verify an entity range will produce a valid filter.
     */
    @Test
    public void testEntityRange()
    {
        // replay
        ObservationQuery rv = m_SUT.withRange(200, 300);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setRange(200, 300);
        
        // replay to get exactly one
        m_SUT.withRange(300, 300);
        m_SUT.execute();
        
        verify(m_DataStore, times(2)).executeJdoQuery(query.capture());
        
        // verify range is set again
        verify(query.getValue()).setRange(300, 300);
    }
    
    /**
     * Verify an invalid entity range will cause an exception
     */
    @Test
    public void testInvalidEntityRange()
    {
        try
        {
            m_SUT.withRange(301, 300);
            fail("Expecting exception as start value is larger than the stop value");
        }
        catch (IllegalArgumentException e)
        {
        }
    }
    
    /**
     * Verify max observations setting will produce a valid filter.
     */
    @Test
    public void testMaxObservations()
    {
        // replay
        ObservationQuery rv = m_SUT.withMaxObservations(300);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        verify(query.getValue()).setRange(0, 300);
        
        // replay to get exactly one
        m_SUT.withMaxObservations(400);
        m_SUT.execute();
        
        verify(m_DataStore, times(2)).executeJdoQuery(query.capture());
        
        // verify range is set again
        verify(query.getValue()).setRange(0, 400);
    }
    
    @Test
    public void testSystemId()
    {
        ObservationQuery returnValue = m_SUT.withSystemId(42);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object
        assertThat((ObservationQueryImpl)returnValue, is(m_SUT));
        
        //verify
        verify(query.getValue()).setFilter("systemId == 42");
    }
    
    /**
     * Verify calling with multiple filters different filter types combines the properties.
     */
    @Test
    public void testWithMultipleFilters()
    {
        // replay
        ObservationQuery rv = m_SUT.withAssetType("blah").withSubType(ObservationSubTypeEnum.DETECTION);
        m_SUT.execute();
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeJdoQuery(query.capture());
        
        // verify return value is the same object for method chaining
        assertThat((ObservationQueryImpl)rv, is(m_SUT));
        
        // verify
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(query.getValue()).setFilter(stringCaptor.capture());
        // might be in either order
        assertThat(stringCaptor.getValue(), anyOf(is("assetType == 'blah' && this.detection.id > 0"),
                                                  is("this.detection.id > 0 && assetType == 'blah'")));
    }
    
    /**
     * Verify that a count can be retrieved.
     */
    @Test
    public void testGetCount()
    {
        when(m_DataStore.executeGetCount(Mockito.any(Query.class))).thenReturn(10L);
        
        long count = m_SUT.getCount();
        
        assertThat(count, is(10L));
    }
        
    
    /**
     * Verify that a count with query parameters properly executed.
     */
    @Test
    public void testGetCountWithQuery()
    {
        when(m_DataStore.executeGetCount(Mockito.any(Query.class))).thenReturn(10L);
        
        ObservationQuery rv = m_SUT.withAssetType("blah").withSubType(ObservationSubTypeEnum.DETECTION);
        
        long count = rv.getCount();
        
        assertThat(count, is(10L));
        
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(m_DataStore).executeGetCount(query.capture());
        
        // verify
        ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
        verify(query.getValue()).setFilter(stringCaptor.capture());
        
        // might be in either order
        assertThat(stringCaptor.getValue(), anyOf(is("assetType == 'blah' && this.detection.id > 0"),
                                                  is("this.detection.id > 0 && assetType == 'blah'")));
    }
}
