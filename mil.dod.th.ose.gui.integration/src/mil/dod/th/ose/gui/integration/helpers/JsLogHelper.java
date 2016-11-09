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
package mil.dod.th.ose.gui.integration.helpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;
import net.jsourcerer.webdriver.jserrorcollector.JavaScriptError;

/**
 * Helper class responsible creating Javascript logs.
 * 
 * @author cweisenborn
 */
public class JsLogHelper
{
    public static void updateJSLog(final WebDriver driver)
    {
        //Script to retrieve the logged Javascript methods. Verifies the function is there before calling. Method
        //won't be there if the driver is not on a THOSE page (i.e. when firefox is initially opened).
        final String script = "if (typeof getLoggedMessages == \"function\") "
                + "{ return getLoggedMessages(); } return [];";
        @SuppressWarnings("unchecked")
        final List<Object> logEntries = 
                (List<Object>)((JavascriptExecutor)driver).executeScript(script);
        final File reportsDir = new File(ResourceLocatorUtil.getWorkspacePath(), "/reports");
        if (!logEntries.isEmpty())
        {
            final File jsLog = new File(reportsDir, "jsLog.log");
            //Opens file in append mode so that logged messages may be added on as further tests are ran.
            try (FileWriter fw = new FileWriter(jsLog, true))
            {
                for (Object entry: logEntries)
                {
                    fw.write(entry.toString() + System.lineSeparator());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        final List<JavaScriptError> jsErrors = JavaScriptError.readErrors(driver);
        if (!jsErrors.isEmpty())
        {
            final File jsErrorLog = new File(reportsDir, "jsErrors.log");
            //Opens file in append mode so that logged errors may be added on as further tests are ran.
            try (FileWriter fw = new FileWriter(jsErrorLog, true))
            {
                for (JavaScriptError error: jsErrors)
                {
                    fw.write(error.toString() + System.lineSeparator());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
