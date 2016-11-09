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
package mil.dod.th.ose.logging;

import aQute.bnd.annotation.metatype.Meta;

import mil.dod.th.ose.shared.LogLevel;

/**
 * Interface for configurable properties of the LogWriter.  
 * 
 * @author callen
 */
@Meta.OCD
public interface LogWriterConfig
{
    /**
    * Holds the buffer size metadata.
    * 
    * @return
    *    current buffer size
    */
    @Meta.AD(required = false, deflt = "1", min = "1", 
            description = "The file buffer size is the amount of data that will be held in memory before being written"
            + " to the log file. The larger the size the more latency there will be between seeing console logging and"
            + " that same information in the log file. Furthermore, the smaller the buffer the slower the performance"
            + " of system could be because of the more frequent flushing of data to the log file.")
    int fileBufferSize();
    
    /**
     * This is the metadata describing the number of days to keep a single log file alive, if not interrupted by the
     * entire system resetting. In the event of a system reset the log file will not be appended to, but a new log will
     * be started. 
     * 
     * @return
     *    number of days to keep a single instance of a log file alive.
     */
    @Meta.AD(required = false, deflt = "14", max = "45", description = "The value entered is the number of days to"
             + " keep a single log file active before a new one is started.")
    int logDaysAlive();
    
    /**
     * The log size limit metadata restricts the size that a log file can be before a new one is started. 
     * 
     * @return
     *     The maximum size in MB that a log file is allowed to be
     */
    @Meta.AD(required = false, deflt = "1", max = "32", description = "The value entered is the maximum size in"
            + " MB(MiB) that a log file is allowed to be before a new one is started.")
    long logMBSizeLimit();
    
    /**
     * Log level configuration.
     * 
     * @return
     *     The log level for which logs will be recorded.
     * 
     */
    @Meta.AD(required = false, deflt = "Debug", description = "The log level to log for.")
    LogLevel logLevel();
}
