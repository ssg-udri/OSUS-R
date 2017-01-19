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
package mil.dod.th.ose.sdk.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import junit.framework.TestCase;

/**
 * Test class used to verify SDK command functionality.
 * 
 * @author cweisenborn
 */
public class TestSdkCommands extends TestCase
{
    private final BundleContext m_Context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    private String m_SdkDir;
    
    @Override
    public void setUp()
    {
        m_SdkDir = m_Context.getProperty("sdk.dir");
    }
    
    /**
     * Verify that the version command works and returns the expected output.
     */
    public void testVersionCommand() throws IOException
    {
        ProcessBuilder pb;
        if (System.getProperty("os.name").startsWith("Windows")) 
        {
            pb = new ProcessBuilder("cmd.exe", "/C", "those.bat", "version");
        } 
        else 
        {
            pb = new ProcessBuilder("sh", "those.sh", "version");
        } 
        pb.redirectErrorStream(true);
        pb.directory(new File(m_SdkDir));
        Process p = pb.start();
        BufferedReader bf = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        final Pattern apiVersionPattern = Pattern.compile("API Version: [0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]{12}+");
        
        boolean found = false;
        String line = bf.readLine();
        while (line != null)
        {
            //Read lines till API version string is found. This is needed as windows prints out the header of the
            //batch file.
            final Matcher apiVersionMatcher = apiVersionPattern.matcher(line);
            if (apiVersionMatcher.matches())
            {
                found = true;
                break;
            }
            line = bf.readLine();
        }
        
        assertThat(found, is(true));
    }
}
