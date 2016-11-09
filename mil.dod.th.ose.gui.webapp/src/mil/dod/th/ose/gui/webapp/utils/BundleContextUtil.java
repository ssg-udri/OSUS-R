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
package mil.dod.th.ose.gui.webapp.utils;

import javax.ejb.Startup;
import javax.inject.Singleton;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * Service that retrieves the bundle context from the framework.
 * @author callen
 *
 */
@Startup
@Singleton
public class BundleContextUtil
{
    /**
     * Get the bundle context for the application.
     * @return
     *    the bundle context for the application's bundle
     */
    public BundleContext getBundleContext()
    {
        final Bundle bundle = FrameworkUtil.getBundle(BundleContextUtil.class); 
        return  bundle.getBundleContext();
    }
}
