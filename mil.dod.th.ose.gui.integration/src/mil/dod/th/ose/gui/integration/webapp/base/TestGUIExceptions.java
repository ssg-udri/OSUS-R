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
package mil.dod.th.ose.gui.integration.webapp.base;

import java.io.File;

import mil.dod.th.ose.gui.integration.helpers.MissionSetupHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

/**
 * @author cweisenborn
 */
public class TestGUIExceptions
{  
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS);
    }
    
    /**
     * Currently there is a known issue with Glassfish where CDI injected OSGi services will not throw the proper
     * exception. This is due to the fact that CDI creates a proxy instance of the injected OSGi class. This causes any
     * exception thrown by an injected OSGi class to be wrapped in a target invocation exception. Because the target
     * invocation exception is not expected it is further wrapped in an undeclared throwable exception. This issue is
     * currently slated to be fixed with the release of Glassfish 4.0, however there is a patch currently available and 
     * that has been implemented. This test is used to assure that the Glassfish patch that has been created to fix this
     * issue is working as intended if any further updates are applied to Glassfish before 4.0 is released. This test 
     * assumes that the MissionImport class calls on the injected TemplateProgramManager service when importing a 
     * template and that when a template with the same name is imported an illegal argument exception should be thrown 
     * by the TempalteProgramManager. This test verifies that the appropriate message is displayed which means that the 
     * correct exception was returned from the proxy class. If no message is displayed then an undeclared throwable 
     * exception was returned and therefore the Glassfish patch is not working as intended. 
     */
    @Test
    public void testException() throws InterruptedException
    {
        NavigationHelper.collapseSideBars(m_Driver);
        
        //This template should already have been imported when the server starts if
        //the test template was placed in the appropriate folder.
        String badTemplate = ResourceLocatorUtil.getResource(new File("badtemplate.xml")).getAbsolutePath();

        String expectedGrowlMessage = "Invalid XML file!";
        
        MissionSetupHelper.importMissionAndVerify(m_Driver, badTemplate, expectedGrowlMessage);
    }
}
