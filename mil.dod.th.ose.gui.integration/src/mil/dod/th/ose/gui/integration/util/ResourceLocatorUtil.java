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
package mil.dod.th.ose.gui.integration.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * 
 * Utility class for finding resources in the project at runtime.
 * 
 * @author cweisenborn
 *
 */
public final class ResourceLocatorUtil
{
    /**
     * Hidden constructor to prevent instantiation.
     */
    private ResourceLocatorUtil()
    {
        
    }
    
    /**
     * Get the path to the root of the project.
     * 
     * @return path to the workspace root
     */
    public static File getWorkspacePath()
    {
        File testBin;
        try
        {
            testBin = new File(ResourceLocatorUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File path = new File(testBin, "../../").getCanonicalFile();
            assertThat(path, isDirectory());
            return path;
        }
        catch (final URISyntaxException | IOException e)
        {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Get the path to the example custom comm plug-ins
     * 
     * @return
     *      path to the base integration project
     */
    public static File getExampleCustomCommPluginFile()
    {
        File path = new File(getWorkspacePath(), "example.ccomm/generated/example.ccomm.main.jar");
        assertThat(path, isFile());
        return path;
    }
    
    /**
     * Get the path to the base integration project.
     * 
     * @return
     *      path to the base integration project
     */
    public static File getBaseIntegrationPath()
    {
        File path = new File(getWorkspacePath(), "mil.dod.th.ose.integration");
        assertThat(path, isDirectory());
        return path;
    }
    
    /**
     * Get the path to the GUI integration project.
     * 
     * @return
     *      path to the GUI integration project
     */
    public static File getGuiIntegrationPath()
    {
        File path = new File(getWorkspacePath(), "mil.dod.th.ose.gui.integration");
        assertThat(path, isDirectory());
        return path;
    }
    
    /**
     * Get the path to the GUI test data within the integration project.
     * 
     * @return
     *      path to the GUI test data
     */
    public static File getGuiTestDataPath()
    {
        return new File(getGuiIntegrationPath(), "generated/test-data");
    }
    
    /**
     * Method to retrieve a file or sub folder from the resource file.
     * 
     * @param resourceFile
     *      the file or sub-folder to be retrieved from the resource folder
     * @return
     *      the desired resource file
     */
    public static File getResource(final File resourceFile)
    {
        final File root = new File(getGuiIntegrationPath(), "/resource");
        assertThat(root, isDirectory());
        final File resource = new File(root, resourceFile.getPath());
        assertThat(resource, exists()); // could be directory or file
        return resource;
    }
}
