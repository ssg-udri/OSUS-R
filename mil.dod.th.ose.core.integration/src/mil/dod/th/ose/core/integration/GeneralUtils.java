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

package mil.dod.th.ose.core.integration;

import static mil.dod.th.ose.test.matchers.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;

import mil.dod.th.ose.shared.SystemConfigurationConstants;

import org.osgi.framework.BundleContext;

/**
 * General utilities for the core integration tests.
 * 
 * @author dhumeniuk
 *
 */
public class GeneralUtils
{
    /**
     * Check if setup target has been run.
     */
    public static void assertSetupHasRun(BundleContext context)
    {
        final File dataDir = new File(context.getProperty(SystemConfigurationConstants.DATA_DIR_PROPERTY));
        File templateDir = new File(dataDir, "templates");
        assertThat("Must run 'setup' build target to copy in template files", templateDir, isDirectory());
    }
}
