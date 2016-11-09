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

import static org.hamcrest.MatcherAssert.assertThat;
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author dhumeniuk
 *
 */
public class ResourceUtils
{
    /**
     * Get the path to the root of the project.
     */
    public static File getWorkspacePath()
    {
        try
        {
            // assume integration is one folder down from workspace
            return new File(getBaseIntegrationPath(), "../").getCanonicalFile();
        }
        catch (IOException e)
        {
            throw new IllegalStateException(e);
        }   
    }
    
    /**
     * Get the path to the base integration project.
     */
    public static File getBaseIntegrationPath()
    {
        File integrationJar;
        try
        {
            integrationJar = new File(ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // assume integration JAR is located at build/deploy/bundle/file.jar within the project folder
            File file = new File(integrationJar, "../../../../").getCanonicalFile();
            assertThat(file, isDirectory());
            return file;
        }
        catch (URISyntaxException | IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
