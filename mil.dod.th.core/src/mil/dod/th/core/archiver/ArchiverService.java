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
package mil.dod.th.core.archiver;

import java.net.URI;
import java.util.Set;

import aQute.bnd.annotation.ProviderType;

/**
 * This interface defines the high-level methods to control an archiver process. The archiver
 * is used to redirect streaming data to a file.
 * 
 * @author jmiller
 *
 */
@ProviderType
public interface ArchiverService
{

    /**
     * Start the archiver process using the supplied parameters.
     * 
     * @param processId
     *      String identifier which refers to an archiving process
     * @param sourceUri
     *      URI of the source stream
     * @param filePath
     *      String representation of output file path
     * @throws IllegalStateException
     *      if an archiving process with id {@code processId} is already running.
     * @throws ArchiverException
     *      if the underlying archiving process throws an exception
     */
    void start(String processId, URI sourceUri, String filePath) throws IllegalStateException, ArchiverException;
    
    /**
     * Stop a running archiver process.
     * 
     * @param processId
     *      String identifier which refers to an archiving process
     * @throws IllegalArgumentException
     *      if no existing archiving process with id {@code processId} exists.
     */
    void stop(String processId) throws IllegalArgumentException;
    
    /**
     * Get the String identifiers corresponding to active archiving processes.
     * 
     * @return Set of String identifiers. Client/bundle can use each String value as an
     *      input for {@link #stop(String)}.
     */
    Set<String> getActiveProcessIds();

}
