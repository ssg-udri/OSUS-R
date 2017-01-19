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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.jdo.Extent;
import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.datastore.JDOConnection;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.observation.types.Biological;
import mil.dod.th.core.observation.types.CbrneTrigger;
import mil.dod.th.core.observation.types.Chemical;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Power;
import mil.dod.th.core.observation.types.WaterQuality;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.core.validator.Validator;
import mil.dod.th.ose.shared.JdoDataStore;
import mil.dod.th.ose.shared.SystemConfigurationConstants;
import mil.dod.th.ose.test.PropertyRetrieverMocker;
import mil.dod.th.ose.utils.ClassService;
import mil.dod.th.ose.utils.PropertyRetriever;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class TestObservationStoreImpl
{
    private static final String ASSET_UUID_FILTER = "assetUuid == '%s'";
    
    private ObservationStoreImpl m_SUT;
    private EventAdmin m_EventAdmin;
    private PersistenceManagerFactoryCreator m_PersistenceManagerFactoryCreator;
    private PersistenceManagerFactory m_PersistenceManagerFactory;
    private PersistenceManager m_PersistenceManager;
    private Transaction m_Transaction;
    private Query m_Query;
    private Extent<?> m_Extent;
    private Observation m_Observation = new Observation();
    private Validator m_ObsValidator;
    private List<Observation> m_ObservationCollection;
    private FetchPlan m_FetchPlan;
    private PropertyRetriever m_Retriever;
    private BundleContext m_Context;
    private PowerManager m_PowerManager;
    private WakeLock m_WakeLock;
       
    @Before
    public void setUp()
        throws Exception
    {
        m_SUT = new ObservationStoreImpl();
        m_EventAdmin = mock(EventAdmin.class);
        m_PersistenceManagerFactoryCreator = mock(PersistenceManagerFactoryCreator.class);
        m_PersistenceManagerFactory = mock(PersistenceManagerFactory.class);
        m_PersistenceManager = mock(PersistenceManager.class);
        m_Transaction = mock(Transaction.class);
        m_ObsValidator = mock(Validator.class);
        m_Query = mock(Query.class);
        
        when(m_PersistenceManagerFactoryCreator
                .createPersistenceManagerFactory(Observation.class,
                                                 "jdbc:h2:file:data-dir/datastores/ObservationStore"))
                .thenReturn(m_PersistenceManagerFactory);
        when(m_PersistenceManagerFactory.getPersistenceManager()).thenReturn(m_PersistenceManager);
        when(m_PersistenceManager.currentTransaction()).thenReturn(m_Transaction);
        m_Extent = m_PersistenceManager.getExtent(PersistentData.class, true);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(m_Query);
        
        m_ObservationCollection = new ArrayList<Observation>();
        when(m_Query.execute()).thenReturn(m_ObservationCollection);
        
        m_PowerManager = mock(PowerManager.class);
        m_WakeLock = mock(WakeLock.class);
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "coreDataStore")).thenReturn(m_WakeLock);

        m_SUT.setObservationValidator(m_ObsValidator);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setPowerManager(m_PowerManager);
        m_SUT.setPersistenceManagerFactoryCreator(m_PersistenceManagerFactoryCreator);
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        m_FetchPlan = mock(FetchPlan.class);
        when(m_PersistenceManager.getFetchPlan()).thenReturn(m_FetchPlan);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minUsableSpace", 1024L);
        props.put(H2DataStoreConfig.MAX_CACHE_SIZE_KEY, 100);
        
        Properties properties = new Properties();
        properties.setProperty("majorNum", "1");
        properties.setProperty("minorNum", "0");
        
        ClassService classService = mock(ClassService.class);
        m_SUT.setClassService(classService);
        URL url = new URL("file:/blah");
        when(classService.getResource(Observation.class, "version.properties")).thenReturn(url);
        m_Retriever = PropertyRetrieverMocker.mockIt(url, properties);
        m_SUT.setPropertyRetriever(m_Retriever);
        
        m_Context = mock(BundleContext.class);
        when(m_Context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY)).thenReturn("data-dir");

        m_SUT.activate(Collections.unmodifiableMap(props), m_Context);
    }

    @After
    public void tearDown()
        throws Exception
    {
        m_SUT.deactivate();
        verify(m_WakeLock).delete();
    }
    
    /**
     * Test that if component and framework properties are not set, the default is used which means SQL statement is not
     * executed.
     */
    @Test
    public final void testNoPropertiesSet() throws IOException, SQLException
    {
        // mock
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        
        Map<String, Object> props = new HashMap<String, Object>();

        m_SUT.activate(Collections.unmodifiableMap(props), m_Context);
        
        // SET CACHE_SIZE should not be called at all as default will be used
        verify(statement, never()).execute(anyString());
    }
    
    /**
     * Test that if component properties are set, the framework property is ignored.
     */
    @Test
    public final void testComponentPropertiesSet() throws IOException, SQLException
    {
        // mock
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minUsableSpace", 500L);
        props.put(H2DataStoreConfig.MAX_CACHE_SIZE_KEY, 100);

        when(m_Context.getProperty(ObservationStoreImpl.MAX_CACHE_SIZE)).thenReturn("200");

        m_SUT.activate(Collections.unmodifiableMap(props), m_Context);
        
        // value should be set based on framework property
        verify(statement).execute("SET CACHE_SIZE 100");
    }

    /**
     * Test that if no component properties are set, the framework property is used.
     */
    @Test
    public final void testNoComponentPropertiesSet() throws IOException, SQLException
    {
        // mock
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        
        Map<String, Object> props = new HashMap<String, Object>();
        
        when(m_Context.getProperty(ObservationStoreImpl.MAX_CACHE_SIZE)).thenReturn("200");

        m_SUT.activate(Collections.unmodifiableMap(props), m_Context);
        
        // value should be set based on framework property
        verify(statement).execute("SET CACHE_SIZE 200");
    }

    @Test
    public final void testSetProps()
    {
        assertThat(m_SUT.getMinUsableSpace(), is(1024L));
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(ComponentConstants.COMPONENT_NAME, "blah");
        props.put(ComponentConstants.COMPONENT_ID, "blah");
        props.put("minUsableSpace", 500L);
        props.put(H2DataStoreConfig.MAX_CACHE_SIZE_KEY, 100);
        m_SUT.modified(props);
        
        assertThat(m_SUT.getMinUsableSpace(), is(500L));        
    }
    
    /**
     * Test that observations can be persisted.
     * Verify events, one without an observation and the other with the actual observation as a property.
     */
    @Test
    public final void testPersistValidatedObservation() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        m_Observation.setAssetUuid(UUID.randomUUID());
        
        Throwable e = null;
        try {m_SUT.persist(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        m_SUT.persist(m_Observation);

        //verify
        verify(m_ObsValidator).validate(m_Observation);
        verify(m_PersistenceManager).makePersistent(m_Observation);
        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(event.capture());
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
 
        //check first event does not observation
        assertThat(event.getAllValues().get(0).getTopic(), is(ObservationStore.TOPIC_OBSERVATION_PERSISTED));
        assertThat((String)event.getAllValues().get(0).getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID), 
                is(m_Observation.getAssetUuid().toString()));
        assertThat((UUID)event.getAllValues().get(0).getProperty(ObservationStore.EVENT_PROP_OBSERVATION_UUID), 
                is(m_Observation.getUuid()));
        assertThat(event.getAllValues().get(0).getProperty(ObservationStore.EVENT_PROP_OBSERVATION), is(nullValue()));

        //second event should have the observation
        assertThat(event.getAllValues().get(1).getTopic(), is(ObservationStore.TOPIC_OBSERVATION_PERSISTED_WITH_OBS));
        assertThat((String)event.getAllValues().get(0).getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID), 
                is(m_Observation.getAssetUuid().toString()));
        assertThat((UUID)event.getAllValues().get(1).getProperty(ObservationStore.EVENT_PROP_OBSERVATION_UUID), 
                is(m_Observation.getUuid()));
        assertThat((Observation)event.getAllValues().get(1).getProperty(ObservationStore.EVENT_PROP_OBSERVATION), 
                is(m_Observation));
    }

    /**
     * Test that observations can be merged.
     * Verify events, one without an observation and the other with the actual observation as a property.
     */
    @Test
    public final void testMergeObservation() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        m_Observation.setAssetUuid(UUID.randomUUID());
        when(m_PersistenceManager.makePersistent(m_Observation)).thenReturn(m_Observation);
        m_SUT.merge(m_Observation);

        //verify
        ArgumentCaptor<Event> event = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(event.capture());
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
 
        //check first event does not observation
        assertThat(event.getAllValues().get(0).getTopic(), is(ObservationStore.TOPIC_OBSERVATION_MERGED));
        assertThat(event.getAllValues().get(0).getProperty(ObservationStore.EVENT_PROP_OBSERVATION), 
            is(nullValue()));
        
        //second event should have the observation
        assertThat(event.getAllValues().get(1).getTopic(), is(ObservationStore.TOPIC_OBSERVATION_MERGED_WITH_OBS));
        assertThat((Observation)event.getAllValues().get(1).getProperty(ObservationStore.EVENT_PROP_OBSERVATION), 
                is(m_Observation));
    }
    
    /**
     * Verify a new query object can be obtained.
     */
    @Test
    public final void testNewQuery()
    {
        ObservationQuery query = m_SUT.newQuery();
        assertThat(query, is(notNullValue()));
        // must be of the impl type
        assertThat(query, instanceOf(ObservationQueryImpl.class));
    }
    
    /**
     * Verify removing by asset sets correct filter.
     */
    @Test
    public final void testRemoveByAsset()
    {
        // mock
        Asset asset = mock(Asset.class);
        UUID assetUuid = UUID.randomUUID();
        when(asset.getUuid()).thenReturn(assetUuid);
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        Long obsRemoved = m_SUT.removeByAsset(asset);
        
        verify(jdoQuery).setFilter(String.format(ASSET_UUID_FILTER, assetUuid));
        verify(jdoQuery).deletePersistentAll();
        assertThat(obsRemoved, is(3827L));
        verify(m_WakeLock, times(5)).activate();
        verify(m_WakeLock, times(5)).cancel();
    }

    /**
     * Verify removing by asset type sets correct filter.
     */
    @Test
    public final void testRemoveByAssetType()
    {
        // mock
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        Long obsRemoved = m_SUT.removeByAssetType("blah");
        
        verify(jdoQuery).setFilter("assetType == 'blah'");
        verify(jdoQuery).deletePersistentAll();
        assertThat(obsRemoved, is(3827L));
        verify(m_WakeLock, times(5)).activate();
        verify(m_WakeLock, times(5)).cancel();
    }

    /**
     * Verify removing by asset UUID sets correct filter.
     */
    @Test
    public final void testRemoveByAssetUuid()
    {
        // mock
        UUID assetUuid = UUID.randomUUID();
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        Long obsRemoved = m_SUT.removeByAssetUuid(assetUuid);
        
        verify(jdoQuery).setFilter(String.format(ASSET_UUID_FILTER, assetUuid));
        verify(jdoQuery).deletePersistentAll();
        assertThat(obsRemoved, is(3827L));
        verify(m_WakeLock, times(5)).activate();
        verify(m_WakeLock, times(5)).cancel();
    }


    /**
     * Verify removing by observation sub-type sets correct filter.
     */
    @Test
    public final void testRemoveBySubType()
    {
        // mock
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        Long obsRemoved = m_SUT.removeBySubType(ObservationSubTypeEnum.DETECTION);
        
        verify(jdoQuery).setFilter("this.detection.id > 0");
        verify(jdoQuery).deletePersistentAll();
        assertThat(obsRemoved, is(3827L));
        verify(m_WakeLock, times(5)).activate();
        verify(m_WakeLock, times(5)).cancel();
    }
    
    /**
     * Verify removing by system ID sets correct filter.
     */
    @Test
    public final void testRemoveBySystemId()
    {
        // mock
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        Long obsRemoved = m_SUT.removeBySystemId(42);
        
        verify(jdoQuery).setFilter("systemId == 42");
        verify(jdoQuery).deletePersistentAll();
        assertThat(obsRemoved, is(3827L));
        verify(m_WakeLock, times(5)).activate();
        verify(m_WakeLock, times(5)).cancel();
    }

    @Test
    public final void testRemoveValidatedObservation()
    {
        Throwable e = null;
        try { m_SUT.remove((Observation)null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        m_SUT.remove(m_Observation);
        verify(m_WakeLock, times(5)).activate();
        verify(m_WakeLock, times(5)).cancel();
    }

    @Test(expected = PersistenceFailedException.class)
    public final void testRemoveCleanupException() throws SQLException
    {
        // mock
        Asset asset = mock(Asset.class);
        UUID assetUuid = UUID.randomUUID();
        when(asset.getUuid()).thenReturn(assetUuid);
        Connection sqlConn = (Connection)m_PersistenceManager.getDataStoreConnection();
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        when(statement.execute(anyString())).thenThrow(new SQLException());
        
        m_SUT.removeByAsset(asset);
    }

    /**
     * Verify querying by asset sets the correct filter and returns the results. 
     */
    @Test
    public final void testQueryByAsset()
    {
        // mock query
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        // mock asset
        Asset asset = mock(Asset.class);
        UUID assetUuid = UUID.randomUUID();
        when(asset.getUuid()).thenReturn(assetUuid);
        
        // mock returned obs
        Observation obs1 = new Observation().withCreatedTimestamp(100L);
        Observation obs2 = new Observation().withCreatedTimestamp(200L);
        Collection<Observation> expectedObs = new ArrayList<>(Arrays.asList(obs1, obs2));
        when(jdoQuery.execute()).thenReturn(expectedObs);
        
        Collection<Observation> actualObs = m_SUT.queryByAsset(asset);
        
        verify(jdoQuery).setFilter(String.format(ASSET_UUID_FILTER, assetUuid));
        assertThat(actualObs, contains(obs1, obs2));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    /**
     * Verify querying by asset type sets the correct filter and returns the results. 
     */
    @Test
    public final void testQueryByAssetType()
    {
        // mock query
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        // mock returned obs
        Observation obs1 = new Observation().withCreatedTimestamp(100L);
        Observation obs2 = new Observation().withCreatedTimestamp(200L);
        Collection<Observation> expectedObs = new ArrayList<>(Arrays.asList(obs1, obs2));
        when(jdoQuery.execute()).thenReturn(expectedObs);
        
        Collection<Observation> actualObs = m_SUT.queryByAssetType("blah");
        
        verify(jdoQuery).setFilter("assetType == 'blah'");
        assertThat(actualObs, contains(obs1, obs2));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /**
     * Verify querying by asset UUID sets the correct filter and returns the results. 
     */
    @Test
    public final void testQueryByAssetUuid()
    {
        // mock query
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        // mock asset
        Asset asset = mock(Asset.class);
        UUID assetUuid = UUID.randomUUID();
        when(asset.getUuid()).thenReturn(assetUuid);
        
        // mock returned obs
        Observation obs1 = new Observation().withCreatedTimestamp(100L);
        Observation obs2 = new Observation().withCreatedTimestamp(200L);
        Collection<Observation> expectedObs = new ArrayList<>(Arrays.asList(obs1, obs2));
        when(jdoQuery.execute()).thenReturn(expectedObs);
        
        Collection<Observation> actualObs = m_SUT.queryByAssetUuid(assetUuid);
        
        verify(jdoQuery).setFilter(String.format(ASSET_UUID_FILTER, assetUuid));
        assertThat(actualObs, contains(obs1, obs2));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    /**
     * Verify querying by observation sub type sets the correct filter and returns the results. 
     */
    @Test
    public final void testQueryBySubType()
    {
        // mock
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        // mock returned obs
        Observation obs1 = new Observation().withCreatedTimestamp(100L);
        Observation obs2 = new Observation().withCreatedTimestamp(200L);
        Collection<Observation> expectedObs = new ArrayList<>(Arrays.asList(obs1, obs2));
        when(jdoQuery.execute()).thenReturn(expectedObs);
        
        Collection<Observation> actualObs = m_SUT.queryBySubType(ObservationSubTypeEnum.DETECTION);
        
        verify(jdoQuery).setFilter("this.detection.id > 0");
        assertThat(actualObs, contains(obs1, obs2));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /**
     * Verify querying by observation system ID sets the correct filter and returns the results.
     */
    @Test
    public final void testQueryBySystemId()
    {
        //Mock
        Query jdoQuery = mock(Query.class);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(jdoQuery);
        when(jdoQuery.deletePersistentAll()).thenReturn(3827L);
        
        //mock returned obs
        Observation obs1 = new Observation().withCreatedTimestamp(100L);
        Observation obs2 = new Observation().withCreatedTimestamp(200L);
        Collection<Observation> expectedObs = new ArrayList<>(Arrays.asList(obs1, obs2));
        when(jdoQuery.execute()).thenReturn(expectedObs);
        
        Collection<Observation> actualObs = m_SUT.queryBySystemId(42);
        
        verify(jdoQuery).setFilter("systemId == 42");
        assertThat(actualObs, contains(obs1, obs2));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /**
     * Verify that after an observation is persisted that an event is posted containing the correct 
     * observation sub type.
     */
    @Test
    public final void testPersistChem() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //observation to persist
        Observation obs =  createObsWithReqFields();
        obs.setChemical(new Chemical());
        
        m_SUT.persist(obs);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat((String)eventCaptor.getValue().getProperty(ObservationStore.EVENT_PROP_OBSERVATION_TYPE), 
                is(ObservationSubTypeEnum.CHEMICAL.toString()));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /**
     * Verify that after an observation is persisted that an event is posted containing the correct 
     * observation sub type.
     */
    @Test
    public final void testPersistWithBio() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //observation to persist
        Observation obs = createObsWithReqFields();
        obs.setBiological(new Biological());
        
        m_SUT.persist(obs);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat((String)eventCaptor.getValue().getProperty(ObservationStore.EVENT_PROP_OBSERVATION_TYPE), 
                is(ObservationSubTypeEnum.BIOLOGICAL.toString()));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    /**
     * Verify that after an observation is persisted that an event is posted containing the correct 
     * observation sub type.
     */
    @Test
    public final void testPersistWithCbrne() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //observation to persist
        Observation obs = createObsWithReqFields();
        obs.setCbrneTrigger(new CbrneTrigger());
        
        m_SUT.persist(obs);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat((String)eventCaptor.getValue().getProperty(ObservationStore.EVENT_PROP_OBSERVATION_TYPE), 
                is(ObservationSubTypeEnum.CBRNE_TRIGGER.toString()));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }
    
    /**
     * Verify that after an observation is persisted that an event is posted containing the correct 
     * observation sub type.
     */
    @Test
    public final void testPersistWaterQuality() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //observation to persist
        Observation obs = createObsWithReqFields();
        obs.setWaterQuality(new WaterQuality());
        
        m_SUT.persist(obs);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat((String)eventCaptor.getValue().getProperty(ObservationStore.EVENT_PROP_OBSERVATION_TYPE), 
                is(ObservationSubTypeEnum.WATER_QUALITY.toString()));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    /**
     * Verify that after an observation is persisted that an event is posted containing the correct 
     * observation sub type.
     */
    @Test
    public final void testPersistPower() throws IllegalArgumentException, PersistenceFailedException, 
        ValidationFailedException
    {
        //observation to persist
        Observation obs = createObsWithReqFields();
        obs.setPower(new Power());
        
        m_SUT.persist(obs);
        
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(m_EventAdmin, times(2)).postEvent(eventCaptor.capture());
        
        assertThat((String)eventCaptor.getValue().getProperty(ObservationStore.EVENT_PROP_OBSERVATION_TYPE), 
                is(ObservationSubTypeEnum.POWER.toString()));
        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    @Test
    public void testGetUsableSpace()
    {
        assertThat(m_SUT.getUsableSpace(), is(greaterThan(0L)));
    }
    
    /**
     * Test getting the version.
     */
    @Test
    public void testDefaultVersion()
    {
        assertThat(m_SUT.getObservationVersion().getMajorNumber(), is(1));
        assertThat(m_SUT.getObservationVersion().getMinorNumber(), is(0));
    }
    
    /**
     * Test that the JDO property label type is the same as this implementations simple name.
     */
    @Test
    public void testDatastoreType()
    {
        assertThat(JdoDataStore.PROP_OBSERVATION_STORE, is(ObservationStore.class.getSimpleName()));
    }  
    
    /**
     * Create an observation with event fields set. 
     */
    private Observation createObsWithReqFields()
    {
        Observation obs = new Observation();
        obs.setAssetType("monkey");
        obs.setAssetUuid(UUID.randomUUID());
        obs.setAssetName("ape");
        obs.setSystemId(1);
        obs.setSystemInTestMode(true);
        return obs;
    }
}
