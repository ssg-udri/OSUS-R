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

import java.sql.SQLException;

/**
 * Interface for an H2 based data store.
 * 
 * @author dhumeniuk
 */
public interface H2DataStore
{
    /**
     * Debug method to execute SQL.
     * 
     * @param statement
     *      generic SQL statement to execute
     * @throws SQLException
     *      if there is an error executing the statement\
     * @return
     *      String representing the results
     */
    String executeSql(String statement) throws SQLException;
    
    /**
     * Get the maximum amount the H2 database can use for caching.  Value is the actual value as queried from the 
     * database, not the same as the configuration value.
     * 
     * @return
     *      max amount of caching in KB (1024 bytes)
     */
    int getMaxDatabaseCacheSize();
    
    /**
     * Get the property of the database.
     * 
     * @param key
     *      property key
     * @return
     *      value as a string of the property
     */
    String getDatabaseProp(String key);
}
