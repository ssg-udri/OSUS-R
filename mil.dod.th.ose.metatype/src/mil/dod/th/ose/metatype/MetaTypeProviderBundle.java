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
package mil.dod.th.ose.metatype;

import org.osgi.framework.Bundle;


/**
 * Defines a service that can be used to acquire a bundle that can be associated with a {@link 
 * org.osgi.service.metatype.MetaTypeProvider}. The Felix implementation does not allow providing the service if a 
 * bundle also has metatype XML resources. Therefore, this separate bundle is used for bundles with metatype XML 
 * resources so they can provide the service.
 * 
 * @author dhumeniuk
 *
 */
public interface MetaTypeProviderBundle 
{
    /**
     * Get the bundle associated with this service.
     * 
     * @return
     *      instance of the bundle
     */
    Bundle getBundle();
}
