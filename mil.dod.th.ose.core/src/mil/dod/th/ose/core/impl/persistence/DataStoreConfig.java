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

/**
 * Configuration interface for {@link mil.dod.th.core.persistence.DataStore} implementations.
 * 
 * @author dhumeniuk
 *
 */
public interface DataStoreConfig
{
    /**
     * Property for {@link mil.dod.th.core.persistence.DataStore#getMinUsableSpace()}.
     * 
     * @return
     *      current configuration value for the property, default if not configured yet
     */
    @Meta.AD(required = false, deflt = "1048576") // default to 1MB
    Long minUsableSpace();
}
