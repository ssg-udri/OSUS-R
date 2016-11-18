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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import javax.jdo.FetchPlan;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.datastore.JDOConnection;

import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class TestAbstractH2DataStore
{
    private AbstractH2DataStore<Byte> m_SUT;
    private PersistenceManagerFactoryCreator m_PersistenceManagerFactoryCreator;
    private PersistenceManagerFactory m_PersistenceManagerFactory;
    private PersistenceManager m_PersistenceManager;
    private PowerManager m_PowerManager;
    private ResultSet m_ResultSet;
    private Statement m_Statement;
    private FetchPlan m_FetchPlan;
    private WakeLock m_WakeLock;

    @Before
    public void setUp() throws SQLException
    {
        m_SUT = new AbstractH2DataStore<Byte>(Byte.class, "timestamp")
        {
            @Override
            public void merge(Byte observation) throws IllegalArgumentException, PersistenceFailedException
            {
                
            }

            @Override
            public long getUsableSpace()
            {
                return 100L;
            }

            @Override
            protected void deleteCleanup()
            {
                
            }
        };
        
        m_PersistenceManagerFactoryCreator = mock(PersistenceManagerFactoryCreator.class);
        m_PersistenceManagerFactory = mock(PersistenceManagerFactory.class);
        m_PersistenceManager = mock(PersistenceManager.class);
        m_PowerManager = mock(PowerManager.class);
        m_WakeLock = mock(WakeLock.class);
        when(m_PersistenceManagerFactoryCreator.createPersistenceManagerFactory(Byte.class, "test")).
            thenReturn(m_PersistenceManagerFactory);
        when(m_PersistenceManagerFactory.getPersistenceManager()).thenReturn(m_PersistenceManager);
        m_SUT.setPersistenceManagerFactoryCreator(m_PersistenceManagerFactoryCreator);
        m_FetchPlan = mock(FetchPlan.class);
        when(m_PersistenceManager.getFetchPlan()).thenReturn(m_FetchPlan);
        when(m_PowerManager.createWakeLock(anyObject(), anyString())).thenReturn(m_WakeLock);
        m_SUT.setPowerManager(m_PowerManager);
        
        Map<String, Object> props = new HashMap<String, Object>();
        m_SUT.activateStore("test", props);
        
        // mock
        Connection sqlConn = mock(java.sql.Connection.class, withSettings().extraInterfaces(JDOConnection.class));
        when(m_PersistenceManager.getDataStoreConnection()).thenReturn((JDOConnection)sqlConn);
        
        m_Statement = mock(Statement.class);
        when(sqlConn.createStatement()).thenReturn(m_Statement);
        when(m_Statement.execute("SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'info.CACHE_MAX_SIZE'")).
            thenAnswer(
                new Answer<Boolean>()
                {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable
                    {
                        ResultSet resultSet = mock(ResultSet.class);
                        when(m_Statement.getResultSet()).thenReturn(resultSet);
                        when(resultSet.getString(2)).thenReturn("38");
                        return true;
                    }
                });
    }
    

    @Test
    public void testGetDatabaseProp() throws SQLException
    {
        // replay with invalid value
        String value = m_SUT.getDatabaseProp("invalid prop");
        
        // verify
        assertThat(value, is("Invalid prop: invalid prop"));
        verify(m_Statement).execute("SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'invalid prop'");
        
        // mock
        when(m_Statement.execute("SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'prop'")).thenAnswer(
                new Answer<Boolean>()
                {
                    @Override
                    public Boolean answer(InvocationOnMock invocation) throws Throwable
                    {
                        m_ResultSet = mock(ResultSet.class);
                        when(m_Statement.getResultSet()).thenReturn(m_ResultSet);
                        when(m_ResultSet.getString(2)).thenReturn("test value");
                        return true;
                    }
                });
        
        // replay
        value = m_SUT.getDatabaseProp("prop");
        
        // verify
        assertThat(value, is("test value"));
        verify(m_Statement).execute("SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = 'prop'");
        verify(m_ResultSet).close();
    }
    
    @Test
    public void testGetMaxDatabaseCacheSize() throws SQLException
    {
        // verify default values are correct
        assertThat(m_SUT.getMaxDatabaseCacheSize(), is(38));
        
        when(m_Statement.execute(anyString())).thenThrow(new SQLException());
        
        try
        {
            m_SUT.getMaxDatabaseCacheSize();
            fail("Expecting exception");
        }
        catch (IllegalStateException e)
        {
            
        }
    }

    @Test
    public void testUpdateProps() throws SQLException
    {
        // replay
        Map<String, Object> props = new HashMap<String, Object>();
        m_SUT.updateProps(props);
        
        // verify cache value not updated
        verify(m_Statement, never()).execute(anyString());
        // verify default values are correct
        assertThat(m_SUT.getMinUsableSpace(), is(1024L*1024L));
        assertThat(m_SUT.getMaxDatabaseCacheSize(), is(38));
        
        // replay
        props.put("minUsableSpace", 50L);
        props.put(H2DataStoreConfig.MAX_CACHE_SIZE_KEY, 100);
        m_SUT.updateProps(props);
        
        // verify updated
        assertThat(m_SUT.getMinUsableSpace(), is(50L));
        verify(m_Statement).execute("SET CACHE_SIZE 100");
        
        // mock to throw exception
        when(m_Statement.execute(anyString())).thenThrow(new SQLException());
        
        try
        {
            m_SUT.updateProps(props);
            fail("Expecting exception");
        }
        catch (IllegalStateException e)
        {
            
        }
    }
}
