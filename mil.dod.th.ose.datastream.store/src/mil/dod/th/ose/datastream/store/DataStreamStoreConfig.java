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
package mil.dod.th.ose.datastream.store;


import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;

/**
 * Defines metadata of properties for the {@link mil.dod.th.core.datastream.store.DataStreamStore} 
 * reference implementation.
 * 
 * @author jmiller
 *
 */
@OCD
public interface DataStreamStoreConfig
{
    /** Configuration property key for {@link #filestoreTopDir()}.  */
    String CONFIG_PROP_FILESTORE_TOP_DIR = "filestore.top.dir";

    /**
     * Configuration property for the top level directory of the streaming data filestore.
     * 
     * @return top level directory as a {@link String}
     */
    @AD(required = true, id = CONFIG_PROP_FILESTORE_TOP_DIR, description = "Top level directory of"
            + " filestore which contains all the archived streaming data in sub-directories and files")
    String filestoreTopDir();

}
