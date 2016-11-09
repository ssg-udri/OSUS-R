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
package mil.dod.th.core.transcoder;

import java.net.URI;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * This interface defines the high-level methods to control a transcoder process. The
 * transcoder is used to decode and re-encode streaming data to a new bitrate and/or
 * output format.
 * 
 * Providers of this service should use the {@code SERVICE_RANKING} property in
 * order to have the OSGi service registry select a particular implementation.
 * 
 * @see org.osgi.framework.Constants#SERVICE_RANKING
 * 
 * @author jmiller
 *
 */
@ProviderType
public interface TranscoderService
{
    /** Configuration property key for target bitrate in kilobits per seconds. */
    String CONFIG_PROP_BITRATE_KBPS = "bitrate.kbps";
    
    /** Configuration property key for output stream format. */
    String CONFIG_PROP_FORMAT = "format";
      
    
    /**
     * Start the transcoder process using the supplied parameters.
     * 
     * @param processId
     *      String identifier which refers to a transcoding process
     * @param sourceUri 
     *      URI of the source stream
     * @param outputUri 
     *      destination URI for the transcoded data
     * @param configParams 
     *      Map of key-value pairs to specify additional transcoder parameters. The set
     *      of acceptable key names are defined in {@link TranscoderService}.
     * @throws IllegalStateException
     *      if a transcoding process with id {@code processId} is already running. 
     * @throws TranscoderException
     *      if the underlying transcoding process throws an exception    
     */
    void start(String processId, URI sourceUri, URI outputUri, Map<String, Object> configParams) 
            throws IllegalStateException, TranscoderException;
    
    /**
     * Stop a running transcoder process.
     * 
     * @param processId
     *      String identifier which refers to a transcoding process
     * @throws IllegalArgumentException
     *      if no existing transcoding process with id {@code processId} exists.
     */
    void stop(String processId) throws IllegalArgumentException;

}
