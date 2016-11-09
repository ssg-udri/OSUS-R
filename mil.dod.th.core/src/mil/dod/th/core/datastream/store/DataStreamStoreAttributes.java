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
package mil.dod.th.core.datastream.store;


import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;

/**
 * Defines metadata of properties that are available for {@link DataStreamStore} configuration.
 * 
 * @author jmiller
 *
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface DataStreamStoreAttributes
{
    
    /** Configuration property key for {@link #maxArchiveTimeSeconds()}. */
    String CONFIG_PROP_MAX_ARCHIVE_TIME_SECONDS = "max.archive.time";
    
    /** Configuration property key for {@link #overwriteWhenFull()}. */
    String CONFIG_PROP_OVERWRITE_WHEN_FULL = "overwrite.when.full";
    
    /**
     * Configuration property for the maximum archiving time.
     * 
     * @return time in seconds
     */
    @AD(required = false, deflt = "3600", id = CONFIG_PROP_MAX_ARCHIVE_TIME_SECONDS,
            description = "Maximum amount of continuous time (in seconds) for the streaming data"
                    + " to be archived")
    long maxArchiveTimeSeconds();
    
    /**
     * Configuration property for setting whether new data should overwrite old data when
     * the archiving time limit has been reached.
     * 
     * @return true to overwrite old data; false to stop archiving any new data.
     */
    @AD(required = false, deflt = "false", id = CONFIG_PROP_OVERWRITE_WHEN_FULL,
            description = "Policy that determines whether new data"
            + " should overwrite old data when the maximum archive time has been reached")
    boolean overwriteWhenFull();

}
