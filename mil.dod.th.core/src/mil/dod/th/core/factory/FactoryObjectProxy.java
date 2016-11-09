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
package mil.dod.th.core.factory;

import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.ConsumerType;

import org.osgi.service.cm.ConfigurationException;

/**
 * Base proxy class for each core factory object type. Each plug-in will automatically implement this interface by 
 * implementing the specific factory object type proxy (e.g., {@link mil.dod.th.core.asset.AssetProxy}). 
 * 
 * @author dhumeniuk
 *
 */
@ConsumerType
public interface FactoryObjectProxy
{
    /**
     * Notify object that its configuration properties have been updated via 
     * {@link org.osgi.service.cm.ConfigurationAdmin}. 
     * 
     * If properties are updated using {@link FactoryObject#setProperties(java.util.Map)}, 
     * then those methods will block on separate thread until this method returns.
     * 
     * @param props
     *      updated properties
     * @throws ConfigurationException
     *      if one of the new property values is invalid 
     */
    void updated(Map<String, Object> props) throws ConfigurationException;
    
    /**
     * Called during object initialization to acquire all known {@link Extension}s, this should <strong>NOT</strong> be
     * called each time an extension is requested. Instead, consumers will call 
     * {@link FactoryObject#getExtensionTypes()} to get all extension class types provided by a plug-in, and then the 
     * needed extension's class should be passed to {@link FactoryObject#getExtension(Class)} to get the actual 
     * extension object.
     * @return
     *      the extensions supported or null if no extensions are supported
     */
    Set<Extension<?>> getExtensions();
}
