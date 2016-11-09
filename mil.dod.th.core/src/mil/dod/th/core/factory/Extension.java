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

/**
 * This interface should be implemented when a plug-in (e.g., {@link mil.dod.th.core.ccomm.physical.PhysicalLink},
 * {@link mil.dod.th.core.asset.Asset}, etc.) requires additional publicly accessible methods not available through the
 * plug-in's API. Plug-ins are responsible for handling whether an extension is a POJO or an OSGi component service and
 * should manage creation, if needed, internally within the plug-in before or during initialization of a plug-in 
 * instance. This is because all available extensions must be returned by {@link FactoryObjectProxy#getExtensions()} 
 * which is called during object initialization.
 * 
 * External services should access extensions of a plug-in object by calling 
 * {@link FactoryObject#getExtension(Class)}. Additionally, all available extension types can be fetched by calling 
 * {@link FactoryObject#getExtensionTypes()} upon a plug-in instance. 
 * 
 * 
 * @author allenchl
 * 
 * @param <T>
 *      the interface type that defines extension methods of a plug-in, for example for a routing table type extension 
 *      the type may be <code> RoutingTable </code>
 */
public interface Extension<T>
{
    /**
     * Get the extension type.
     * @return
     *      the class that provides this extension
     */
    Class<T> getType();
    
    /**
     * Get the object to make calls upon.
     * @return
     *      the extension object
     */
    T getObject();
}
