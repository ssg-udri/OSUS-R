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
package mil.dod.th.ose.controller.integration.utils;

import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * This class holds bundle utility functions for testing purposes.
 * 
 * @author Josh
 *
 */
public class BundleTestUtils
{
    /*
     * Private constructor
     */
    private BundleTestUtils()
    {
        
    }
    
    /**
     * Finds a specific {@link Bundle} given the symbolic name of the bundle to find.
     * @param symName
     *      the symbolic name of the bundle to find
     * @return
     *      the located bundle or null if the bundle isn't found in this context
     */
    public static Bundle getBundleBySymName(String symName)
    {
        BundleContext context = IntegrationTestRunner.getBundleContext();
        Bundle[] allBundles = context.getBundles();
        for (Bundle thisBundle : allBundles)
        {
            if(thisBundle.getSymbolicName().equals(symName))
            {
                // found the right bundle, return it
                return thisBundle;
            }
        }
        
        // bundle not found, return null
        return null;
    }
}
