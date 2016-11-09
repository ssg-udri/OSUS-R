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
package mil.dod.th.ose.gui.integration.webapp.advanced;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import mil.dod.th.ose.gui.integration.helpers.BundleManagerUtil;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Test class for the bundles tab on the system configuration page.
 * 
 * @author cweisenborn
 */
public class TestSystemBundleConfiguration
{
    private static WebDriver m_Driver;
    
    //Bundle that is already deployed and stopping/starting will not negatively effect connectivity to the controller.
    private static String m_ExampleMetaTypeBundle = "mil.dod.th.ose.integration.example.metatype";

    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG);
        NavigationHelper.collapseSideBars(m_Driver);
    }
    
    /**
     * Test the search bar functionality of the bundle configuration page.
     * Verify that the appropriate bundle is returned when using the search function.
     */
    @Test
    public void testSearchBar() throws InterruptedException
    {
        String thoseCoreBundleName = "mil.dod.th.ose.core";
        
        //Uses the bundle search bar feature to find the specified bundle.
        BundleManagerUtil.clearBundleFilter(m_Driver);
        BundleManagerUtil.filterBundles(m_Driver, thoseCoreBundleName, true);
    }
    
    /**
     * Tests stopping and starting a bundle. Calls both the stop bundle and start bundle tests since the start bundle
     * relies on the stop bundle test functioning appropriately.
     */
    @Test
    public void testStopStartBundle() throws InterruptedException
    {
        assertStopBundle();
        assertStartBundle();
    }
    
    /**
     * Test stopping an active bundle.
     * Verify that the bundle status changes to resolved.
     */
    public void assertStopBundle() throws InterruptedException
    {           
        WebDriverWait wait = new WebDriverWait(m_Driver, 20);
        
        BundleManagerUtil.stopBundle(m_Driver, m_ExampleMetaTypeBundle);
        
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(
                "tbody[id*='bundleTable_data']>tr>td:first-child+td+td>div>span"), "Resolved"));
        
        WebElement bundleTable = m_Driver.findElement(By.cssSelector("tbody[id*='bundleTable_data']"));
        WebElement bundleState = bundleTable.findElements(By.tagName("span")).get(3);
        assertThat(bundleState.getText(), is("Resolved"));
    }
    
    /**
     * Test starting a resolved bundle.
     * Verify the status of the bundle is active.
     */
    public void assertStartBundle() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 20);
        
        BundleManagerUtil.startBundle(m_Driver, m_ExampleMetaTypeBundle);        
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(
                "tbody[id*='bundleTable_data']>tr>td:first-child+td+td>div>span"), "Active"));
        
        WebElement bundleTable = m_Driver.findElement(By.cssSelector("tbody[id*='bundleTable_data']"));
        WebElement bundleState = bundleTable.findElements(By.tagName("span")).get(3);
        assertThat(bundleState.getText(), is("Active"));
    }
    
    /**
     * Test the bundle information dialog.
     * Verify that the information dialog displays the appropriate information.
     */
    @Test
    public void testInformationDialog() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 20);
        
        //Uses the bundle search bar feature to find the specified bundle.
        BundleManagerUtil.clearBundleFilter(m_Driver);
        WebElement bundleRow = BundleManagerUtil.filterBundles(m_Driver, m_ExampleMetaTypeBundle, true);
        
        WebElement infoButton = bundleRow.findElement(By.cssSelector("button[id*='info']"));
        assertThat(infoButton.isDisplayed(), is(true));
        infoButton.click();
        
        //Verify that the information dialog is displayed after the bundle info button is clicked.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='bundleDialog']")));
        WebElement infoDialog = m_Driver.findElement(By.cssSelector("div[id*='bundleDialog']"));
        assertThat(infoDialog.isDisplayed(), is(true));
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span[id*='bundleInfo']")));
        WebElement infoSpan = infoDialog.findElement(By.cssSelector("span[id*='bundleInfo']"));
        assertThat(infoSpan.isDisplayed(), is(true));
        
        //Make sure that the appropriate bundle name is displayed in the information dialog.
        WebElement infoBundleName = infoSpan.findElement(By.tagName("span"));
        assertThat(infoBundleName.getText(), is("Example MetaType Bundle "
                + "(mil.dod.th.ose.integration.example.metatype)"));
        
        WebElement closeDialog = infoDialog.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
        
        //Verify that the bundle information dialog closed appropriately.
        assertThat(infoDialog.isDisplayed(), is(false));
    }
    
    /**
     * Method that tests both installing and uninstalling a bundle. Calls both the install bundle test and uninstall
     * bundle test due to the fact that the uninstall bundle test relies on the bundle installed in the install bundle
     * test.
     */
    @Test
    public void testInstallUninstallBundle() throws InterruptedException
    {
        assertInstallBundle();
        assertUninstallBundle();
    }
    
    /**
     * Test installing a new bundle.
     * Verify that the bundle is installed and that the status is active.
     */
    public void assertInstallBundle() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 20);
        
        File bundleFile = new File(ResourceLocatorUtil.getWorkspacePath(), 
                "example.project/generated/example.project.jar");
        BundleManagerUtil.installBundle(m_Driver, bundleFile, null);
        
        //Uses the bundle search bar feature to find the specified bundle.
        //Wait needed to make sure install dialog has closed and bundle search bar is visible.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id*='globalFilter']")));
        BundleManagerUtil.clearBundleFilter(m_Driver);
        WebElement bundleRow = BundleManagerUtil.filterBundles(m_Driver, "example.project", true);
        
        //Verify that the table contains the installed bundle.
        WebElement bundleName = bundleRow.findElements(By.tagName("span")).get(1);
        assertThat(bundleName.getText(), is("example.project"));
        
        //Verify that the installed bundle was started automatically.
        WebElement bundleState = bundleRow.findElements(By.tagName("span")).get(3);
        assertThat(bundleState.getText(), is("Active"));
    }
    
    /**
     * Test uninstalling a bundle.
     * Verify that the bundle has been removed.
     */
    public void assertUninstallBundle() throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(m_Driver, 20);
        
        BundleManagerUtil.uninstallBundle(m_Driver, "example.project", null);
        
        //Uses the bundle search bar feature to find the specified bundle.
        //Filter is cleared first to make sure table is up to date.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id*='globalFilter']")));
        BundleManagerUtil.clearBundleFilter(m_Driver);
        BundleManagerUtil.filterBundles(m_Driver, "example.project", false);
    }
}
