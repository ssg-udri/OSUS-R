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
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.event.EventAdmin;

/**
 * @author jconn
 *
 */
public class TestAbstractDataStore
{
    private AbstractDataStore<Long> m_SUT;
    private EventAdmin m_EventAdmin;
    private PersistenceManagerFactoryCreator m_PersistenceManagerFactoryCreator;
    private PersistenceManagerFactory m_PersistenceManagerFactory;
    private PersistenceManager m_PersistenceManager;
    private Transaction m_Transaction;
    private Query m_Query;
    private Extent<?> m_Extent;
    private Long m_Long = Long.valueOf(10);
    private Map<String, Object> m_Properties = new HashMap<String, Object>();
    private Statement m_Statement;
    private FetchPlan m_FetchPlan;
    
    @Before
    public void setUp()
        throws Exception
    {
        m_SUT = new AbstractDataStore<Long>(Long.class, "timestamp")
        {
            @Override
            public long getUsableSpace()
            {
                return 1024L * 1024L * 10L;
            }

            @Override
            public void merge(Long observation) throws IllegalArgumentException, PersistenceFailedException
            {
                
            }

            @Override
            protected void deleteCleanup()
            {
                
            }
        };
        m_EventAdmin = mock(EventAdmin.class);
        m_PersistenceManagerFactoryCreator = mock(PersistenceManagerFactoryCreator.class);
        m_PersistenceManagerFactory = mock(PersistenceManagerFactory.class);
        m_PersistenceManager = mock(PersistenceManager.class);
        m_Transaction = mock(Transaction.class);
        m_Query = mock(Query.class);
        m_Properties.put("javax.jdo.option.ConnectionURL", "jdbc:h2:TestStore.db");
        
        when(m_Query.execute()).thenReturn(Collections.emptyList());
        
        when(m_PersistenceManagerFactoryCreator.createPersistenceManagerFactory(Long.class, "test")).
            thenReturn(m_PersistenceManagerFactory);
        when(m_PersistenceManagerFactory.getPersistenceManager()).thenReturn(m_PersistenceManager);
        when(m_PersistenceManager.currentTransaction()).thenReturn(m_Transaction);
        m_Extent = m_PersistenceManager.getExtent(Long.class, true);
        when(m_PersistenceManager.newQuery(m_Extent)).thenReturn(m_Query);
        m_FetchPlan = mock(FetchPlan.class);
        when(m_PersistenceManager.getFetchPlan()).thenReturn(m_FetchPlan);
        m_SUT.setEventAdmin(m_EventAdmin);
        m_SUT.setPersistenceManagerFactoryCreator(m_PersistenceManagerFactoryCreator);
        
        Map<String, Object> props = new HashMap<String, Object>();
        m_SUT.activateStore("test", props);
    }

    @After
    public void tearDown()
        throws Exception
    {
        m_SUT.deactivateStore();
    }
    
    /**
     * Test activation of store, verify max fetch depth is set.
     */
    @Test
    public void testActivateStore()
    {
        verify(m_FetchPlan).setMaxFetchDepth(10);
    }

    @Test
    public void testPostEvent()
    {
        m_SUT.postEvent("TestTopicName", null);
    }
    
    @Test
    public void testUpdateProps()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        m_SUT.updateProps(props);
        
        // verify default values are correct
        assertThat(m_SUT.getMinUsableSpace(), is(1024L*1024L));
        
        // replay
        props.put("minUsableSpace", 50L);
        m_SUT.updateProps(props);
        
        // verify updated
        assertThat(m_SUT.getMinUsableSpace(), is(50L));
    }
    
    @Test
    public void testMinUsableSpace()
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minUsableSpace", 5L);
        m_SUT.updateProps(props);
        
        assertThat(m_SUT.getMinUsableSpace(), is(5L));
    }

    @Test
    public void testPersist() throws PersistenceFailedException
    {
        m_SUT.makePersistent(m_Long);
        
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("minUsableSpace", m_SUT.getUsableSpace() + 5);
        m_SUT.updateProps(props);
        try
        {
            m_SUT.makePersistent(m_Long);
            fail("Expecting exception");
        }
        catch (PersistenceFailedException e)
        {
            
        }
    }
    
    /**
     * Verify the query object is returned by the new method.
     */
    @Test
    public void testNewJdoQuery()
    {
        Query query = m_SUT.newJdoQuery();
        assertThat(query, is(m_Query));
    }
    
    /**
     * Verify that can get a count.
     */
    @Test
    public void testExecuteGetCount()
    {
        when(m_Query.execute()).thenReturn(222L);
        Query query = m_SUT.newJdoQuery();
        assertThat(m_SUT.executeGetCount(query), is(222L));
        
        verify(query).closeAll();
    }
    
    @Test
    public void testQueryOnFilter()
    {
        final Collection<Long> collection = new ArrayList<Long>();
        when(m_Query.execute()).thenReturn(collection);
        
        assertThat(m_SUT.queryOnFilter("").size(), is(0));
        verify(m_FetchPlan).removeGroup("extendedDataGroup");
        verify(m_FetchPlan).addGroup("extendedDataGroup");
    }
    
    @Test
    public void testRemoveOnFilter()
    {
        
        when(m_Query.deletePersistentAll()).thenReturn(Long.valueOf(10));
        
        assertThat(m_SUT.removeOnFilter(""), is(Long.valueOf(10)));
    }

    @Test
    public void testDelete()
    {
        m_SUT.delete(null);
        m_SUT.delete(m_Long);
    }

    @Test
    public void testValidateArgumentStartStopTimes()
    {
        final Date startDate = new Date();
        final Date stopDate = new Date(startDate.getTime() + 1);
        
        Throwable e = null;
        try { m_SUT.validateArgumentStartStopTimes(null, null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        e = null;
        try { m_SUT.validateArgumentStartStopTimes(startDate, null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));

        e = null;
        try { m_SUT.validateArgumentStartStopTimes(null, stopDate); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));

        e = null;
        try { m_SUT.validateArgumentStartStopTimes(stopDate, startDate); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        m_SUT.validateArgumentStartStopTimes(startDate, stopDate);
    }

    @Test
    public void testValidateArgumentUuid()
    {
        Throwable e = null;
        try { m_SUT.validateArgumentUuid(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        m_SUT.validateArgumentUuid(new UUID(0L, 0L));
    }
    
    @Test
    public final void testContains()
    {
        Throwable e = null;
        try { m_SUT.contains(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        UUID uuid = UUID.randomUUID();
        assertThat(m_SUT.contains(uuid), is(false));
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                when(m_Query.execute()).thenReturn(Arrays.asList(new Long[] {5L}));
                return null;
            }
        }).when(m_Query).setFilter("uuid == '" + uuid.toString() + "'");
        
        assertThat(m_SUT.contains(uuid), is(true));
    }

    @Test
    public final void testFind()
    {
        Throwable e = null;
        try { m_SUT.find(null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        UUID uuid = UUID.randomUUID();
        assertThat(m_SUT.find(uuid), is(nullValue()));
        
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                when(m_Query.execute()).thenReturn(Arrays.asList(new Long[] {5L}));
                return null;
            }
        }).when(m_Query).setFilter("uuid == '" + uuid.toString() + "'");
        
        assertThat(m_SUT.find(uuid), is(notNullValue()));
        verify(m_FetchPlan, times(2)).removeGroup("extendedDataGroup");
        verify(m_FetchPlan, times(2)).addGroup("extendedDataGroup");
    }
    
    /**
     * Verify that a null uuid and an invalid uuid throw exceptions, and that the object associated with a valid 
     * uuid is removed.
     */
    @Test
    public final void testRemoveUUID()
    {
        //verify that a null uuid throws an exception
        Throwable e = null;
        try { m_SUT.remove((UUID)null); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        //verify that an invalid uuid throws an exception
        UUID uuid = UUID.randomUUID();
        e = null;
        try { m_SUT.remove(uuid); }
        catch (Throwable ex) { e = ex; }
        assertThat(e, instanceOf(IllegalArgumentException.class));
        
        //verify that the object associated with a valid uuid is removed
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                when(m_Query.execute()).thenReturn(Arrays.asList(new Long[] {5L}));
                return null;
            }
        }).when(m_Query).setFilter("uuid == '" + uuid.toString() + "'");
        
        m_SUT.remove(uuid);
        verify(m_PersistenceManager).deletePersistent(5L);
        verify(m_FetchPlan, times(2)).removeGroup("extendedDataGroup");
        verify(m_FetchPlan, times(2)).addGroup("extendedDataGroup");
    }

    @Test
    public final void testRemoveDateDate()
    {
        Date startTime = new Date(10);
        Date stopTime = new Date(100);
        
        // replay
        m_SUT.remove(startTime, stopTime);
        
        // verify
        verify(m_Query).setFilter("timestamp >= 10 && timestamp <= 100");
        verify(m_Query).deletePersistentAll();
    }
    
    @Test
    public void testCompact() throws PersistenceFailedException, SQLException
    {
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        
        // replay
        m_SUT.compact();
        
        // verify
        verify(statement).execute("SHUTDOWN COMPACT");
        verify(statement).close();
        
        // change mock to throw exception
        when(statement.execute(anyString())).thenThrow(new SQLException());
        
        // replay
        try
        {
            m_SUT.compact();
            fail("expecting exception");
        }
        catch (PersistenceFailedException e)
        {
            assertThat(e.getCause(), is(instanceOf(SQLException.class)));
        }
    }
    
    @Test
    public void testIsCompactingSupport()
    {
        assertThat(m_SUT.isCompactingSupported(), is(true));
    }
    

    @Test
    public void testExecuteSql() throws SQLException
    {
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        
        Statement statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(statement);
        when(statement.execute("test2")).thenReturn(true);
        
        Object rv1 = m_SUT.executeSql("test", null);
        assertThat(rv1, is(nullValue()));
        verify(statement).close();
        verify(sqlConn).close();
        
        Boolean rv2 = m_SUT.executeSql("test2", new AbstractDataStore.ResultHandler<Boolean>()
        {
            @Override
            public Boolean statementExecuted(boolean result, Statement statement)
            {
                if (result)
                {
                    m_Statement = statement;
                }
                return result;
            }
        });
        
        assertThat(rv2, is(true));
        verify(statement, times(2)).close();
        verify(sqlConn, times(2)).close();
        assertThat(m_Statement, is(statement));
    }
}
