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
package mil.dod.th.core.datastream;

import java.util.Map;

import mil.dod.th.core.factory.FactoryException;
import mil.dod.th.core.factory.FactoryObjectProxy;

/**
 * A stream profile plug-in implements this interface providing any custom behavior of the plug-in
 * that is needed to interact with the physical asset.
 * 
 * @author jmiller
 *
 */
public interface StreamProfileProxy extends FactoryObjectProxy
{
    /**
     * Called to initialize the object and provide the plug-in with the {@link StreamProfileContext} 
     * to interact with the rest of the system.
     * 
     * @param context
     *      context specific to the {@link StreamProfile} instance
     * @param props
     *      the stream profiles's configuration properties, available as a convenience so 
     *      {@link StreamProfileContext#getConfig()} does not have to be called
     * @throws FactoryException
     *      if there is an error initializing the object.
     */
    void initialize(StreamProfileContext context, Map<String, Object> props) throws FactoryException;
    
    /**
     * The {@link DataStreamService} will call this method to enabled this stream profile as necessary. 
     * 
     * @throws StreamProfileException
     *      if the stream profile could not be enabled
     */
    void onEnabled() throws StreamProfileException;
    
    /**
     * The {@link DataStreamService} will call this method to disable this stream profile as necessary.
     * 
     * @throws StreamProfileException
     *      if the stream profile could not be disabled
     */
    void onDisabled() throws StreamProfileException;

}
