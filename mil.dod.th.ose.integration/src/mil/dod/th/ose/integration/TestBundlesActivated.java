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
package mil.dod.th.ose.integration;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import mil.dod.th.ose.junit4xmltestrunner.IntegrationTestRunner;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * @author dhumeniuk
 *
 */
public class TestBundlesActivated
{
    @SuppressWarnings("unchecked")
    @Test
    public void testBundlesStarted()
    {
        BundleContext context = IntegrationTestRunner.getBundleContext();
        for (Bundle bundle : context.getBundles())
        {
            assertThat(bundle.getSymbolicName(), bundle.getState(), anyOf(is(Bundle.ACTIVE)));
        }
    }
}
