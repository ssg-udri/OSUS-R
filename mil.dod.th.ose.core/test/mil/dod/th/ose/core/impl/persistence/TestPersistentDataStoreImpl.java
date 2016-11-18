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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Extent;
import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.datastore.JDOConnection;

import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.shared.JdoDataStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventAdmin;
import org.powermock.api.mockito.PowerMockito;

/**
 * @author jconn
 *
 */
public class TestPersistentDataStoreImpl
{
    private PersistentDataStoreImpl m_SUT;
    private EventAdmin m_EventAdmin;
    private PersistenceManagerFactoryCreator m_PersistenceManagerFactoryCreator;
    private PersistenceManagerFactory m_PersistenceManagerFactory;
    private PersistenceManager m_PersistenceManager;
    private Transaction m_Transaction;
    private Query m_Query;
    private Extent<?> m_Extent;
    private UUID m_UUID = UUID.randomUUID();
    private PersistentData m_PersistentData;
    private Collection<PersistentData> m_PersistentDataCollection;
    private FetchPlan m_FetchPlan;
    private BundleContext m_Context;
    private PowerManager m_PowerManager;
    private WakeLock m_WakeLock;

    @Before
    public void setUp()
        throws Exception
    {
        m_SUT = new PersistentDataStoreImpl();
        m_EventAdmin = mock(EventAdmin.class);
        m_PersistenceManagerFactoryCreator = mock(PersistenceManagerFactoryCreator.class);
        m_PersistenceManagerFactory = mock(PersistenceManagerFactory.class);
        m_PersistenceManager = mock(PersistenceManager.class);
        m_Transaction = mock(Transaction.class);
        m_Query = mock(Query.class);
        m_PersistentData = new PersistentData(m_UUID, "TestDescription", this.getClass().getName(), Long.valueOf(10));
        
        PowerMockito.whenNew(PersistentData.class).withArguments(m_PersistentData.getUUID(),
                m_PersistentData.getDescription(), this.getClass().getName(), m_PersistentData.getEntity())
                .thenReturn(m_PersistentData);
        
        when(m_PersistenceManagerFactoryCreator
              .createPersistenceManagerFactory(PersistentData.class, "jdbc:h2:file:datastores/PersistentDataStore"))
              .thenReturn(m_PersistenceManagerFactory);
        when(m_PersistenceManagerFactory.getPersistenceManager()).thenReturn(m_PersistenceManager);
        when(m_PersistenceManager.currentTransaction()).thenReturn(m_Transaction);
        m_Extent = m_PersistenceManager.getExtent(PersistentData.class, true);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(m_Query);
        m_PersistentDataCollection = new ArrayList<PersistentData>();
        when(m_Query.execute()).thenReturn(m_PersistentDataCollection);
        m_FetchPlan = mock(FetchPlan.class);
        when(m_PersistenceManager.getFetchPlan()).thenReturn(m_FetchPlan);
        
        m_PowerManager = mock(PowerManager.class);
        m_WakeLock = mock(WakeLock.class);
        when(m_PowerManager.createWakeLock(m_SUT.getClass(), "coreDataStore")).thenReturn(m_WakeLock);

        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setPersistenceManagerFactoryCreator(m_PersistenceManagerFactoryCreator);
        m_SUT.setPowerManager(m_PowerManager);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minUsableSpace", 1024L);
        
        m_Context = mock(BundleContext.class);
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

        when(m_Context.getProperty(PersistentDataStoreImpl.MAX_CACHE_SIZE)).thenReturn("200");

        m_SUT.activate(Collections.unmodifiableMap(props), m_Context);
        
        // value should be set based on framework property
        verify(statement).execute("SET CACHE_SIZE 100");
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
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
        
        when(m_Context.getProperty(PersistentDataStoreImpl.MAX_CACHE_SIZE)).thenReturn("200");

        m_SUT.activate(Collections.unmodifiableMap(props), m_Context);
        
        // value should be set based on framework property
        verify(statement).execute("SET CACHE_SIZE 200");
        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public final void testSetProps()
    {
        assertThat(m_SUT.getMinUsableSpace(), is(1024L));
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minUsableSpace", 500L);
        m_SUT.modified(props);
        
        assertThat(m_SUT.getMinUsableSpace(), is(500L));
    }


    @Test
    public void testContains()
    {
        Throwable e = null;
        try { m_SUT.contains(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        assertThat(m_SUT.contains(m_PersistentData.getUUID()), is(false));
        
        addMockEntry();
        
        assertThat(m_SUT.contains(m_PersistentData.getUUID()), is(true));

        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    @Test
    public void testFind()
    {
        Throwable e = null;
        try { m_SUT.find(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        assertThat(m_SUT.find(m_PersistentData.getUUID()), is(nullValue()));
        
        addMockEntry();
        
        assertThat(m_SUT.find(m_PersistentData.getUUID()), is(notNullValue()));

        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    @Test
    public void testPersistContextUUIDStringSerializable() throws IllegalArgumentException, PersistenceFailedException
    {
        Throwable e = null;
        try { m_SUT.persist(null, null, null, null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        e = null;
        try { m_SUT.persist(this.getClass(), null, null, null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));

        e = null;
        try { m_SUT.persist(this.getClass(), m_PersistentData.getUUID(), null, null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        e = null;
        try 
        { 
            m_SUT.persist(this.getClass(), m_PersistentData.getUUID(), m_PersistentData.getDescription(), null);
        }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        when(m_PersistenceManager.makePersistent(Mockito.any(PersistentData.class))).thenReturn(m_PersistentData);
        
        m_SUT.persist(this.getClass(), m_PersistentData.getUUID(), m_PersistentData.getDescription(), 
                m_PersistentData.getEntity());

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testMergePersistentData() throws IllegalArgumentException, PersistenceFailedException
    {
        Throwable e = null;
        try { m_SUT.merge(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        when(m_PersistenceManager.makePersistent(m_PersistentData)).thenReturn(m_PersistentData);
        
        m_SUT.merge(m_PersistentData);

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testRemoveMatchingClass()
    {
        m_SUT.removeMatching(this.getClass());

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }
    
    @Test
    public void testRemoveMatchingClassDateDate()
    {
        m_SUT.removeMatching(this.getClass(), new Date(), new Date());

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testRemoveMatchingClassString()
    {
        m_SUT.removeMatching(this.getClass(), m_PersistentData.getDescription());

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testRemoveMatchingClassStringDateDate()
    {
        m_SUT.removeMatching(this.getClass(), m_PersistentData.getDescription(), new Date(), new Date());

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testRemovePersistentData()
    {
        Throwable e = null;
        try { m_SUT.remove((PersistentData)null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, is(instanceOf(IllegalArgumentException.class)));
        
        m_SUT.remove(m_PersistentData);

        verify(m_WakeLock).activate();
        verify(m_WakeLock).cancel();
    }

    @Test
    public void testQueryClassDateDate()
    {
        Date startTime = new Date(m_PersistentData.getTimestamp());
        Date stopTime = new Date(startTime.getTime()+1);
        
        assertThat(m_SUT.query(this.getClass(), startTime, stopTime).size(), is(0));
        
        addMockEntry();
        
        assertThat(m_SUT.query(this.getClass(), startTime, stopTime).size(), is(1));

        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }

    @Test
    public void testQueryClassStringDateDate()
    {
        Date startTime = new Date(m_PersistentData.getTimestamp());
        Date stopTime = new Date(startTime.getTime()+1);
        
        assertThat(m_SUT.query(this.getClass(), m_PersistentData.getDescription(), startTime, stopTime).size(), 
                is(0));
        
        addMockEntry();
        
        assertThat(m_SUT.query(this.getClass(), m_PersistentData.getDescription(), startTime, stopTime).size(), 
                is(1));

        verify(m_WakeLock, times(2)).activate();
        verify(m_WakeLock, times(2)).cancel();
    }
    
    @Test
    public void testGetUsableSpace()
    {
        assertThat(m_SUT.getUsableSpace(), is(greaterThan(0L)));
    }

    /**
     * Test that the JDO property label type is the same as this implementations simple name.
     */
    @Test
    public void testDatastoreType()
    {
        assertThat(JdoDataStore.PROP_PERSISTENT_STORE, is(PersistentDataStore.class.getSimpleName()));
    }

    /**
     * Adds a sample PersistentData to a collection and returns.
     */
    private void addMockEntry()
    {
        m_PersistentDataCollection.add(m_PersistentData);
    }
}
