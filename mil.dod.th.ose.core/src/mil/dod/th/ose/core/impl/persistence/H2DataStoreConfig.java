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

import aQute.bnd.annotation.metatype.Meta;
import aQute.bnd.annotation.metatype.Meta.AD;

/**
 * Configuration interface for {@link mil.dod.th.core.persistence.DataStore} implementations.
 *  
 * @author dhumeniuk
 *
 */
public interface H2DataStoreConfig extends DataStoreConfig
{
    /**
     * Key to use for the id of the {@link #maxDatabaseCacheSize()}.
     */
    String MAX_CACHE_SIZE_KEY = "max.database.cache.size";

    /**
     * The maximum size of the cache to use with the H2 database.
     * 
     * @return
     *      size in KB of the cache, value of -1 means to use the value set initially by the database
     */
    @AD(id = MAX_CACHE_SIZE_KEY, required = false, deflt = "-1")
    int maxDatabaseCacheSize();

    // TODO: TH-122: this is copied from the base interface because annotations are not scanned in base interfaces
    // need to handler some other way
    /**
     * Property for {@link mil.dod.th.core.persistence.DataStore#getMinUsableSpace()}.
     * 
     * @return
     *      current configuration value for the property, default if not configured yet
     */
    @Override
    @Meta.AD(required = false, deflt = "1048576") // default to 1MB
    Long minUsableSpace();
}
