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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import aQute.bnd.annotation.metatype.Configurable;

import mil.dod.th.ose.shared.H2DataStore;

/**
 * Extends an {@link AbstractDataStore} and assumes an H2 database connection.  This class provides access to H2 
 * specific functionality like caching.
 * 
 * @param <T>
 *      type stored by this data store
 *      
 * @author dhumeniuk
 *
 */
public abstract class AbstractH2DataStore<T> extends AbstractDataStore<T> implements H2DataStore
{
    /**
     * Default constructor. Sets the extent class type.
     * 
     * @param extentClass
     *            the extent class type
     * @param extentClassTimeField
     *            name of the default time field used for common queries and result ordering
     */
    protected AbstractH2DataStore(final Class<? extends T> extentClass, final String extentClassTimeField)
    {
        super(extentClass, extentClassTimeField);
    }
    
    @Override
    public String executeSql(final String statement) throws SQLException
    {
        return executeSql(statement, new ResultHandler<String>()
        {

            @Override
            public String statementExecuted(final boolean result, final Statement statement) 
                    throws SQLException
            {
                if (!result)
                {
                    return "Non-singular value not supported, got: " + statement.getUpdateCount();
                }
                
                final StringBuilder resultsString = new StringBuilder();
                final ResultSet results = statement.getResultSet();
                try
                {
                    while (results.next())
                    {
                        final ResultSetMetaData metaData = results.getMetaData();
                        for (int i = 0; i < metaData.getColumnCount(); i++)
                        {
                            if (i > 0)
                            {
                                resultsString.append(", ");
                            }
                            resultsString.append(results.getString(i + 1));
                        }
                        resultsString.append(System.getProperty("line.separator"));
                    }
                    return resultsString.toString();
                }
                finally
                {
                    results.close();
                }
            }
            
        });
    }
    
    @Override
    public int getMaxDatabaseCacheSize()
    {
        return Integer.parseInt(getDatabaseProp("info.CACHE_MAX_SIZE"));
    }
    
    @Override
    public String getDatabaseProp(final String key)
    {
        try
        {
            return executeSql("SELECT * FROM INFORMATION_SCHEMA.SETTINGS WHERE NAME = '" + key + "'",
                    new ResultHandler<String>()
                    {
                        @Override
                        public String statementExecuted(final boolean result, final Statement statement) 
                                throws SQLException
                        {
                            if (!result)
                            {
                                return "Invalid prop: " + key;
                            }
                                
                            final ResultSet results = statement.getResultSet();
                            try
                            {
                                results.next();
                                // key is column 1, value is column 2
                                return results.getString(2);
                            }
                            finally
                            {
                                results.close();
                            }
                        }
                    });
        }
        catch (final SQLException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    protected void updateProps(final Map<String, Object> props)
    {
        super.updateProps(props);
        final H2DataStoreConfig config = Configurable.createConfigurable(H2DataStoreConfig.class, props);
        
        setMaxDatabaseCacheSize(config.maxDatabaseCacheSize());
    }

    /**
     * Update the max size in the database.
     * 
     * @param size
     *      new size to use for the cache in KB, -1 means don't set value because the default should be used instead
     */
    private void setMaxDatabaseCacheSize(final int size)
    {
        if (size == -1)
        {
            return;
        }
        
        try
        {
            executeSql("SET CACHE_SIZE " + size, null);
        }
        catch (final SQLException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
