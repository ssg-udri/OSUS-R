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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class performs overall navigation of the web gui. This pertains to elements located on the 
 * mainscreenTemplate. Therefore, this class performs navigation between pages and overall template
 * manipulation.
 * 
 * @author cweisenborn
 */
public class NavigationHelper
{
    /**
     * Mapped used to map the navigation buttons to the page name constants that are associate with page that the 
     * button will navigate to when pressed.
     */
    @SuppressWarnings("serial")
    private static Map<String, String> m_NavMap = new HashMap<String,String>() {{
            put(NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, PageNameConstants.PAGECONST_PROP_ASSET);
            put(NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS, PageNameConstants.PAGECONST_PROP_CHANNELS);
            put(NavigationButtonNameConstants.NAVBUT_PROP_COMMS, PageNameConstants.PAGECONST_PROP_COMMS);
            put(NavigationButtonNameConstants.NAVBUT_PROP_GUI_CONFIG, PageNameConstants.PAGECONST_PROP_GUI_CONFIG);
            put(NavigationButtonNameConstants.NAVBUT_PROP_MISSIONS, PageNameConstants.PAGECONST_PROP_MISSION);
            put(NavigationButtonNameConstants.NAVBUT_PROP_SETUP_MIS, PageNameConstants.PAGECONST_PROP_MISSION_SETUP);
            put(NavigationButtonNameConstants.NAVBUT_PROP_POWER, PageNameConstants.PAGECONST_PROP_POWER);
            put(NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, PageNameConstants.PAGECONST_PROP_SYS_CONFIG);
        }
    };
    
    /**
     * Navigates the web driver to the base URL. 
     */
    public static void openBaseUrl()
    {
        final WebDriver driver = WebDriverFactory.retrieveWebDriver();
        //Retrieve logged Javascript messages/errors and write to the log. This must be done before navigation or logged
        //messages/errors will be lost.
        JsLogHelper.updateJSLog(driver);
        driver.get(String.format("http://localhost:%d/those", ThoseUrlHelper.getInstancePort()));
    }

    /**
     * Navigates the web driver to a specified web page based on button id. This method will open the left side bar if 
     * it is not already open and will return the side bar back to the state it was in when this method was called.
     * 
     * @param driver
     *  web driver that is performing the navigation
     *  
     * @param buttonId
     *  the id of the button that must be utilized to perform the 
     *  navigation
     */
    public static void navigateToPage(final WebDriver driver, final String buttonId) throws InterruptedException
    {
        String xpathSearch = String.format("//button[contains(@id, '%s')]", buttonId );
        
        WebDriverWait wait = new WebDriverWait(driver, 30);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#navigationLayout")));
        
        boolean leftCollapsed = false;
        //Checks if the left side bar is collapsed. If it is collapsed then it will expand it so that the navigation
        //buttons can be reached.
        final WebElement navBar = 
                GeneralHelper.safeFindElement(null, By.cssSelector("div#navigationLayout"));
        if (!navBar.isDisplayed())
        {
            leftCollapsed = true;
            expandLeftSideBarOnly(driver);
        }
        
        //Retrieve and write javascript log statements before navigating.
        JsLogHelper.updateJSLog(driver);
        
        //Find and click the desired navigation button.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathSearch)));
        WebElement pageButton = driver.findElement(By.xpath(xpathSearch));
        assertThat(pageButton, is(notNullValue()));
        pageButton.click();
        
        //Wait until the page has been navigated to.
        wait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return driver.getCurrentUrl().contains(m_NavMap.get(buttonId));
            }
        });
        
        //Checks to see left side bar was collapsed when this method was called. If it was then this method makes sure
        //that the side bar is collapsed before returning.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#navigationLayout")));
        if (leftCollapsed)
        {
            //Sleep needed since page may have just loaded and side bar may not be able to be interacted with.
            Thread.sleep(1000);
            collapseLeftBarOnly(driver);
        }
        
        // TD: sleep to allow remote commands to perform AJAX updates of the page on loading see $(document).ready 
        // functions in push JavaScript, if those AJAX updates could be removed, this probably wouldn't be necessary
        GeneralHelper.sleepWithText(4, "Waiting for page load");
    }
    
    /**
     * Function to collapse both side bars from the main template.
     * @param driver
     *  the web driver performing the test
     */
    public static void collapseSideBars(final WebDriver driver) throws InterruptedException
    {
        collapseLeftBarOnly(driver);
        collapseRightSideBarOnly(driver);
    }
    
    /**
     * Function to collapse only the left side bar from the main template. 
     * @param driver
     *  the web driver performing the test
     */
    public static void collapseLeftBarOnly(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#navigationLayout")));
        
        //Checks to make sure the left side bar isn't already collapsed. If it is then just return.
        if (!driver.findElement(By.cssSelector("div#navigationLayout")).isDisplayed())
        {
            return;
        }
        
        WebElement leftSideBar = driver.findElement(By.cssSelector("div#navigationLayout"));
        WebElement collapseButton = 
                leftSideBar.findElement(By.cssSelector("span[class='ui-icon ui-icon-triangle-1-w']"));
        assertThat(collapseButton, is(notNullValue()));
        //An action is used instead of a standard click due to the fact that IE throws an element not found exception
        //when using the standard click. Using an action works in both IE and Firefox.
        Actions builder = new Actions(driver);
        builder.moveToElement(collapseButton).click().build().perform();
        //Wait for the left side bar primefaces element to collapse. Cannot wait till element is invisible because it
        //is deemed invisible before it is fully closed.
        Thread.sleep(500);
    }
    
    /**
     * Function to collapse only the right side bar from the main template. 
     * @param driver
     *  the web driver performing the test
     */
    public static void collapseRightSideBarOnly(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#controllerLayout")));
        
        //Checks to make sure the right side bar isn't already collapsed. If it is then just return.
        if (!driver.findElement(By.cssSelector("div#controllerLayout")).isDisplayed())
        {
            return;
        }
        
        WebElement rightSideBar = driver.findElement(By.cssSelector("div#controllerLayout"));
        WebElement collapseButton = 
                rightSideBar.findElement(By.cssSelector("span[class='ui-icon ui-icon-triangle-1-e']"));
        assertThat(collapseButton, is(notNullValue()));
        //An action is used instead of a standard click due to the fact that IE throws an element not found exception
        //when using the standard click. Using an action works in both IE and Firefox.
        Actions builder = new Actions(driver);
        builder.moveToElement(collapseButton).click().build().perform();
        //Wait for the right side bar primefaces element to collapse. Cannot wait till element is invisible because it
        //is deemed invisible before it is fully closed.
        Thread.sleep(500);
    }
    
    /**
     * Function will expand both side bars when side bars are initially collapsed.
     * @param driver
     *  the web driver performing the test
     */
    public static void expandSideBars(final WebDriver driver) throws InterruptedException
    {
        expandRightSideBarOnly(driver);
        expandLeftSideBarOnly(driver);
    }
    
    /**
     * Function to only expand the right side bar when it is collapsed.
     * @param driver
     *  the web driver that is performing the test
     */
    public static void expandRightSideBarOnly(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#controllerLayout")));
        
        //Checks to make sure the right side bar isn't already expanded. If it is then just return.
        if (driver.findElement(By.cssSelector("div[id='controllerLayout']")).isDisplayed())
        {
            return;
        }
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@id, 'controllerLayout-toggler')]")));
        WebElement rightSideBar = driver.findElement(
                By.xpath("//div[contains(@id, 'controllerLayout-toggler')]"));
        
        assertThat(rightSideBar, is(notNullValue()));
        
        WebElement rightButton = rightSideBar.findElement(By.tagName("a"));
        
        assertThat(rightButton, is(notNullValue()));
        //An action is used instead of a standard click due to the fact that IE throws an element not found exception
        //when using the standard click. Using an action works in both IE and Firefox.
        Actions builder = new Actions(driver);
        builder.moveToElement(rightButton).click().build().perform();
        //Wait for the right side bar primefaces element to expand. Cannot wait till element is visible because it
        //is deemed visible before it is fully opened.
        Thread.sleep(500);
    }
    
    /**
     * Function to only expand the left side bar when it is collapsed.
     * @param driver
     *  the web driver that is performing the test
     */
    public static void expandLeftSideBarOnly(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div#navigationLayout")));
        
        //Checks to make sure the left side bar isn't already expanded. If it is then just return.
        if (driver.findElement(By.cssSelector("div[id='navigationLayout']")).isDisplayed())
        {
            return;
        }
        
        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[contains(@id, 'navigationLayout-toggler')]")));
        WebElement leftSideBar = driver.findElement(
                By.xpath("//div[contains(@id, 'navigationLayout-toggler')]"));
        
        assertThat(leftSideBar, is(notNullValue()));
        
        WebElement leftButton = leftSideBar.findElement(By.tagName("a"));
        
        assertThat(leftButton, is(notNullValue()));
        //An action is used instead of a standard click due to the fact that IE throws an element not found exception
        //when using the standard click. Using an action works in both IE and Firefox.
        Actions builder = new Actions(driver);
        builder.moveToElement(leftButton).click().build().perform();
        //Wait for the left side bar primefaces element to expand. Cannot wait till element is visible because it
        //is deemed visible before it is fully opened.
        Thread.sleep(500);
    }
    
    /**
     * Method used to verify that GUI is on the correct page and on the correct tab if that page has multiple tabs.
     * 
     * @param driver
     *      The {@link WebDriver} running the test.
     * @param navButtonConstant
     *      The {@link NavigationButtonNameConstants} that represents the button name of the page the driver should be 
     *      on.
     * @param tabId
     *      ID of the tab the driver should be on within the specified page.
     */
    public static void pageAndTabCheck(final WebDriver driver, final String navButtonConstant, 
            final String tabId) throws InterruptedException
    {
        //Check to see if the current page is the specified page. If not then navigate the specified page.
        if (!driver.getCurrentUrl().contains(m_NavMap.get(navButtonConstant)))
        {
            navigateToPage(driver, navButtonConstant);
        }
        if (tabId != null && !tabId.isEmpty())
        {
            //Check to see which tab the page is on. If it is not on specified tab then navigate to it.
            if (!driver.findElement(By.cssSelector("div[id$='" + tabId + "']")).isDisplayed())
            {
                driver.findElement(By.cssSelector("a[href$='" + tabId + "']")).click();
                
                WebDriverWait wait = new WebDriverWait(driver, 10);
                wait.ignoring(NoSuchElementException.class);
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("div[id$='" + tabId + "']")));
            }
        }
    }
}
