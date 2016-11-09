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
package mil.dod.th.ose.remote.integration;

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
        File testBin;
        try
        {
            testBin = new File(ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return new File(testBin, "../../").getCanonicalFile();
        }
        catch (URISyntaxException | IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Get the path to the base integration project.
     */
    public static File getBaseIntegrationPath()
    {
        File file = new File(getWorkspacePath(), "mil.dod.th.ose.integration");
        assertThat(file, isDirectory());
        return file;
    }
    
    /**
     * Get the path to the remote integration project.
     */
    public static File getExampleProjectBundleFile()
    {
        File file = new File(getWorkspacePath(), "example.project/generated/example.project.jar");
        assertThat(file, isFile());
        return file;
    }
}
