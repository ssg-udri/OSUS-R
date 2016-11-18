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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import javax.jdo.Extent;
import javax.jdo.JDOException;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;
import javax.jdo.datastore.JDOConnection;

import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.core.persistence.DataStore;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.pm.PowerManager;
import mil.dod.th.core.pm.WakeLock;
import mil.dod.th.ose.shared.JdoDataStore;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Common base class for the persistence store services that work with the JDO persistence library.
 * 
 * @author jconn
 * 
 * @param <DataType>
 *            the type that the Class object is representing
 */
abstract class AbstractDataStore<DataType> implements DataStore<DataType>, JdoDataStore<DataType>
{
    /** And. */
    final static protected String AND = " && ";

    /** Query by Time filter. */
    final static protected String TIME_FILTER = "%s >= %d && %s <= %d";
    
    /**
     * Property constant value used for representing the extended data fetch group. This fetch group is used to retrieve
     * large data fields such a digital media after an initial query.
     */
    final static private String PROP_EXTENDED_FETCH_GROUP = "extendedDataGroup";

    /**
     * Instances of the Extent class represent the entire collection of instances in the data store of the candidate
     * class or interface possibly including its subclasses or subinterfaces.
     */
    private Extent<? extends DataType> m_Extent;

    /**
     * PersistenceManagerFactoryCreator reference.
     */
    private PersistenceManagerFactoryCreator m_PersistenceManagerFactoryCreator;

    /**
     * Holds the extent class type.
     */
    private final Class<? extends DataType> m_ExtentClass;

    /**
     * Holds the field name associated with the time for the extent.
     */
    private final String m_ExtentClassTimeField;

    /**
     * Event Admin service reference.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Minimum required usable space in order to persist additional items.
     */
    private long m_MinUsableSpace;

    /**
     * Single persistence manager used to access data store.
     */
    private PersistenceManager m_PersistenceManager;

    /**
     * Service used to create power management wake locks.
     */
    private PowerManager m_PowerManager;

    /**
     * Wake lock used for database operations.
     */
    private WakeLock m_WakeLock;

    /**
     * Default constructor, sets the extent class type.
     * 
     * @param extentClass
     *            the extent class type
     * @param extentClassTimeField
     *            name of the default time field used for common queries and result ordering
     */
    protected AbstractDataStore(final Class<? extends DataType> extentClass, final String extentClassTimeField)
    {
        m_ExtentClass = extentClass;
        m_ExtentClassTimeField = extentClassTimeField;
    }

    /**
     * The service component activation method.
     * 
     * Creates the JDO Persistence Manager Factory, gets and sets up the Persistence Manager, and initializes the Extent
     * to be used for queries.
     * 
     * @param url
     *            connection URL of the persistent store
     * @param props
     *            component properties to use once store has been initialized
     */
    protected void activateStore(final String url, final Map<String, Object> props)
    {
        m_WakeLock = m_PowerManager.createWakeLock(getClass(), "coreDataStore");

        final PersistenceManagerFactory persistenceManagerFactory;
        persistenceManagerFactory = 
                m_PersistenceManagerFactoryCreator.createPersistenceManagerFactory(m_ExtentClass, url);
        persistenceManagerFactory.setOptimistic(false);
        persistenceManagerFactory.setMultithreaded(true);
        persistenceManagerFactory.setDetachAllOnCommit(true);
        m_PersistenceManager = persistenceManagerFactory.getPersistenceManager();
        m_Extent = m_PersistenceManager.getExtent(m_ExtentClass, true);
        // Limit the depth of fetching to a larger number instead of the default of 1. Otherwise objects will not be 
        // completely detached from the data store when persisted or queried.  Instead, an exception will be thrown if a
        // field is accessed that hasn't been detached. Have to be careful not to have objects that link to other 
        // objects like observation references in the default fetch group or else objects fetch will pull out too much 
        // from the database.  This setting assumes the fetch group setup contains only a single object with few 
        // linkages.
        m_PersistenceManager.getFetchPlan().setMaxFetchDepth(10); // NOCHECKSTYLE: magic #, accessor defines the meaning 
        
        updateProps(props);
    }

    /**
     * The service component deactivation method.
     * 
     * Closes the Persistence Manager and Persistence Manager Factory.
     */
    protected void deactivateStore()
    {
        m_PersistenceManager.close();
        m_WakeLock.delete();
    }

    /**
     * Binds the EventAdmin service to this component.
     * 
     * @param eventAdmin
     *            EventAdmin service to use for posting events
     */
    protected void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }

    /**
     * Binds the PersistenceManagerFactoryCreator service to this component.
     * 
     * @param persistenceManagerFactoryCreator
     *            PersistenceManagerFactoryCreator service to use for creating the PersistenceManagerFactory
     */
    protected void setPersistenceManagerFactoryCreator(
        final PersistenceManagerFactoryCreator persistenceManagerFactoryCreator)
    {
        m_PersistenceManagerFactoryCreator = persistenceManagerFactoryCreator;
    }
    
    /**
     * Binds the PowerManager service to this component.
     * 
     * @param powerManager
     *            service used to create wake locks for power management
     */
    protected void setPowerManager(final PowerManager powerManager)
    {
        m_PowerManager = powerManager;
    }

    /**
     * Set the threshold for minimum usable space needed to persist data.
     * 
     * @param value
     *      new threshold in bytes
     */
    private void setMinUsableSpace(final long value)
    {
        m_MinUsableSpace = value;
    }
    
    @Override
    public long getMinUsableSpace()
    {
        return m_MinUsableSpace;
    }

    @Override
    public DataType find(final UUID uuid)
            throws IllegalArgumentException
    {
        validateArgumentUuid(uuid);
        
        return findByUUID(uuid);
    }

    @Override
    public boolean contains(final UUID uuid)
            throws IllegalArgumentException
    {
        validateArgumentUuid(uuid);
        
        if (findByUUID(uuid) == null)
        {
            return false;
        }
        
        return true;
    }
    
    @Override
    public void remove(final DataType data)
            throws IllegalArgumentException
    {
        if (data == null)
        {
            throw new IllegalArgumentException("data must not be null");
        }

        delete(data);
    }

    @Override
    public void remove(final UUID uuid)
            throws IllegalArgumentException
    {        
        final DataType uuidFound = find(uuid);
        
        if (uuidFound == null)
        {
            throw new IllegalArgumentException("Could not remove managed observation, UUID not found");
        }
        
        delete(uuidFound);
    }

    @Override
    public void remove(final Date startTime,
                       final Date stopTime)
            throws IllegalArgumentException
    {
        validateArgumentStartStopTimes(startTime, stopTime);

        removeOnFilter(TIME_FILTER, m_ExtentClassTimeField, startTime.getTime(), m_ExtentClassTimeField,
                stopTime.getTime());
    }

    /**
     * Method that executes a jdo query with a given filter string and arguments.
     * @param filterString
     *  the filter string that is to be used.
     * @param args
     *  the arguments that make up the filter string
     * @return
     *  the collection of objects that satisfy the given query criteria
     */
    protected Collection<DataType> queryOnFilter(final String filterString, final Object ... args)
    {
        synchronized (this)
        {
            final Query newQuery = newJdoQuery();
            newQuery.setFilter(String.format(filterString, args));
            newQuery.setOrdering(String.format("%s descending", m_ExtentClassTimeField));

            return Collections.unmodifiableCollection(executeJdoQuery(newQuery));
        }
    }
    
    @Override
    public void compact() throws PersistenceFailedException
    {
        synchronized (this)
        {
            final JDOConnection conn = getPersistenceManager().getDataStoreConnection();
            
            try (final java.sql.Connection sqlConn = (java.sql.Connection)conn)
            {
                m_WakeLock.activate();

                try (final Statement statement = sqlConn.createStatement())
                {
                    // shutdown will close the connection so need to do so explicitly
                    statement.execute("SHUTDOWN COMPACT");
                }
            }
            catch (final SQLException e)
            {
                throw new PersistenceFailedException(e);
            }
            finally
            {
                m_WakeLock.cancel();
            }
        }
        
    }
    
    @Override
    public boolean isCompactingSupported() // NOPMD: PMD thinks this is empty method, but returns true
    {
        return true;
    }
    
    /**
     * Post an event to the EventAdmin.
     * 
     * @param topic
     *            Topic to use for the event
     * @param props
     *            Properties to associate with the event
     */
    protected void postEvent(final String topic,
                             final Map<String, Object> props)
    {
        m_EventAdmin.postEvent(new Event(topic, props));
    }

    /**
     * Persist the class object T to the data store.
     * 
     * @param object
     *            the class T object to be persisted
     * @return the persistence managed object reference
     * @throws PersistenceFailedException
     *          if the call to make persistent fails or there is not enough usable space
     */
    protected DataType makePersistent(final DataType object)
            throws PersistenceFailedException
    {
        if (getUsableSpace() < getMinUsableSpace())
        {
            throw new PersistenceFailedException(String.format("Not enough usable space, require %d, only %d", 
                    getMinUsableSpace(), getUsableSpace()));
        }
        
        synchronized (this)
        {
            final PersistenceManager persistenceManager = getPersistenceManager();
            final Transaction transaction = persistenceManager.currentTransaction();
            final DataType persistedObject;
            try
            {
                m_WakeLock.activate();

                transaction.begin();
                persistedObject = persistenceManager.makePersistent(object);
                transaction.commit();
            }
            catch (final JDOException exception)
            {
                throw new PersistenceFailedException("Persist failed.", exception);
            }
            finally
            {
                if (transaction.isActive())
                {
                    transaction.rollback();
                }
                
                m_WakeLock.cancel();
            }

            return persistedObject;
        }
    }
    
    /**
     * Returns the class object T for the UUID if managed in the data store.
     * 
     * @param uuid
     *          the specified unique identifier
     * @return the class object T for the specified UUID if managed in the data store, else null
     */
    protected DataType findByUUID(final UUID uuid)
    {
        final Collection<DataType> pDataCollection = queryOnFilter("uuid == '%s'", uuid);
        if (pDataCollection.isEmpty())
        {
            return null;
        }
        return pDataCollection.iterator().next();
    }
    
    @Override
    public Query newJdoQuery()
    {
        final PersistenceManager persistenceManager = getPersistenceManager();
        return persistenceManager.newQuery(m_Extent);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<DataType> executeJdoQuery(final Query query)
    {
        final PersistenceManager manager = getPersistenceManager();
        manager.getFetchPlan().removeGroup(PROP_EXTENDED_FETCH_GROUP);
        query.compile();
        
        final Collection<DataType> results;
        synchronized (this)
        {
            final Transaction transaction = manager.currentTransaction();
            try
            {
                m_WakeLock.activate();

                transaction.begin();
                // copy into new list so it is accessible after query is closed
                results = new ArrayList<DataType>((Collection<DataType>)query.execute());
                
                manager.getFetchPlan().addGroup(PROP_EXTENDED_FETCH_GROUP);
                
                transaction.commit();
            }
            finally
            {
                if (transaction.isActive())
                {
                    transaction.rollback();
                }
                query.closeAll();

                m_WakeLock.cancel();
            }
        }
        
        return Collections.unmodifiableCollection(results);
    }
    
    /**
     * Helper method to remove based on a JDO query.
     * @param query
     *          the query used to remove
     * @return the number of items deleted
     */
    @Override
    public long removeOnJdoQuery(final Query query)
    {
        long numberDeleted;//NOCHECKSTYLE gets assigned in try/catch
        synchronized (this)
        {
            final PersistenceManager persistenceManager = getPersistenceManager();
            final Transaction transaction = persistenceManager.currentTransaction();
            try
            {
                m_WakeLock.activate();

                transaction.begin();
                numberDeleted = query.deletePersistentAll();
                transaction.commit();

                transaction.begin();
                deleteCleanup();
                transaction.commit();
            }
            finally
            {
                if (transaction.isActive())
                {
                    transaction.rollback();
                }

                m_WakeLock.cancel();
            }
        }

        return numberDeleted;
    }
    
    /**
     * Helper method to get the total count of items of a table.
     * @param query
     *  the specified query
     * 
     * @return
     *  the number of elements
     */
    @Override
    public long executeGetCount(final Query query)
    {
        final long resultCount;
        synchronized (this)
        {
            try
            {
                m_WakeLock.activate();

                query.setResult("count(this)");

                resultCount = (long)query.execute();
            }
            finally
            {
                query.closeAll();

                m_WakeLock.cancel();
            }
        }
        
        return resultCount;
    }

    /**
     * Helper method to remove based on a JDOQL filter string.
     * @param filterString
     *          the specified JDOQL filter string
     * @param args
     *          the specified JDOQL filter string arguments
     * @return the number of items deleted
     */
    protected long removeOnFilter(final String filterString, final Object ... args)
    {
        final Query newQuery = newJdoQuery();
        newQuery.setFilter(String.format(filterString, args));
            
        return removeOnJdoQuery(newQuery);
    }
    
    /**
     * Helper method for deleting the specified persistent T class objects; wraps PersistenceManager calls.
     * 
     * @param object
     *            the specified persistent T class object
     */
    protected void delete(final DataType object)
    {
        if (object == null)
        {
            return;
        }

        synchronized (this)
        {
            final PersistenceManager persistenceManager = getPersistenceManager();
            final Transaction transaction = persistenceManager.currentTransaction();
            try
            {
                m_WakeLock.activate();

                transaction.begin();
                persistenceManager.deletePersistent(object);
                transaction.commit();

                transaction.begin();
                deleteCleanup();
                transaction.commit();
            }
            finally
            {
                if (transaction.isActive())
                {
                    transaction.rollback();
                }

                m_WakeLock.cancel();
            }
        }
        
    }

    /**
     * Perform any manual cleanup required by the persistent object(s) during a deletion from the database.
     */
    abstract protected void deleteCleanup();

    /**
     * Helper method to validate start/stop time arguments.
     * 
     * @param startTime
     *            the specified startTime
     * @param stopTime
     *            the specified stopTime
     * @throws IllegalArgumentException
     *             if either argument is null, or startTime is after stopTime
     */
    protected void validateArgumentStartStopTimes(final Date startTime,
                                                  final Date stopTime)
            throws IllegalArgumentException
    {
        if (startTime == null)
        {
            throw new IllegalArgumentException("startTime must not be null");
        }
        else if (stopTime == null)
        {
            throw new IllegalArgumentException("stopTime must not be null");
        }
        else if (stopTime.getTime() < startTime.getTime())
        {
            throw new IllegalArgumentException("startTime must not be after stopTime");
        }
    }

    /**
     * Helper method to validate the uuid argument.
     * 
     * @param uuid
     *            the specified universally unique identifier
     * @throws IllegalArgumentException
     *             if the uuid is null
     */
    protected void validateArgumentUuid(final UUID uuid)
            throws IllegalArgumentException
    {
        if (uuid == null)
        {
            throw new IllegalArgumentException("uuid must not be null");
        }
    }
    
    /**
     * Use updated component properties.
     * 
     * @param props
     *      map of new component properties
     */
    protected void updateProps(final Map<String, Object> props)
    {
        final DataStoreConfig config = Configurable.createConfigurable(DataStoreConfig.class, props);
        setMinUsableSpace(config.minUsableSpace());
    }
    
    /**
     * Execute an arbitrary SQL statement and handle the results with the given handler.
     * 
     * @param <T>
     *      type of the value return by this method, handler must be able to get this value from the statement results
     * @param statementText
     *      text of SQL statement to execute
     * @param handler
     *      handles the results of the statement execution, null if nothing to handle
     * @return
     *      data returned from the execute statement, handler dependent
     * @throws SQLException
     *      if their is a failure to execute the SQL statement
     */
    protected <T> T executeSql(final String statementText, final ResultHandler<T> handler) throws SQLException
    {
        synchronized (this)
        {
            final JDOConnection conn = getPersistenceManager().getDataStoreConnection();
            
            try
            {
                m_WakeLock.activate();

                @SuppressWarnings("resource") // compiler thinks sqlConn is not closed but it is by conn defined above
                final java.sql.Connection sqlConn = (java.sql.Connection)conn;
                
                try (final Statement statement = sqlConn.createStatement())
                {
                    final boolean result = statement.execute(statementText);
                    if (handler == null)
                    {
                        return null;
                    }
                    else
                    {
                        return handler.statementExecuted(result, statement);
                    }
                }
            }
            finally
            {
                conn.close();

                m_WakeLock.cancel();
            }
        }
    }
    
    /**
     * Helper method returns the PersistenceManager for the data store.
     * 
     * @return the PersistenceManager for the data store.
     */
    private PersistenceManager getPersistenceManager()
    {
        return m_PersistenceManager;
    }
    
    /**
     * Handles results of an executed statement.
     * 
     * @author dhumeniuk
     *
     * @param <T>
     *      type of data to return when looking at the results
     */
    public interface ResultHandler<T>
    {
        /**
         * Called when the given statement has been executed by {@link AbstractDataStore#executeSql(String, 
         * ResultHandler)}.
         * 
         * @param result
         *      result of {@link Statement#execute(String)}
         * @param statement
         *      statement object that was executed, call {@link Statement#getResultSet()} or {@link 
         *      Statement#getUpdateCount()}.
         * @return
         *      result of the executed statement in a more usable object
         * @throws SQLException
         *      if there is a failure in processing results from the statement
         */
        T statementExecuted(boolean result, Statement statement) throws SQLException;

    }
}

