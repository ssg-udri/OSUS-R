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
import static org.hamcrest.Matchers.*;
import static mil.dod.th.ose.test.matchers.Matchers.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import mil.dod.th.ose.gui.integration.by.ByChain;
import mil.dod.th.ose.gui.integration.by.ByChain.MultipleByChain;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.util.Strings;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

/**
 * Class for completing various bundle functions (installing, uninstalling, etc.) to successfully test other features
 * of the THOSE GUI.  
 * @author bachmakm
 *
 */
public class BundleManagerUtil
{
    /**
     * ID for the bundle tab on the system configuration page.
     */
    public static final String BUNDLE_TAB_ID = "bundleTab";
    
    /**
     * Method which navigates to the System Configurations page and automatically installs a bundle
     * with the given JAR file name.
     * @param driver
     *      Instance of the {@link WebDriver} which controls the navigation
     * @param bundleFile
     *      bundle file to install
     * @param returnPageButton
     *      {@link NavigationButtonNameConstants} ID corresponding to the page to navigate back to after
     *      performing bundle installation.
     * @throws InterruptedException
     *      thrown in the event thread sleep is interrupted while waiting for page to render
     */
    public static void installBundle(WebDriver driver, File bundleFile, String returnPageButton) 
        throws InterruptedException
    {
        assertThat(bundleFile, isFile());
        
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, BUNDLE_TAB_ID);
        if (returnPageButton != null)
        {
            sysConfigNavigationHelper(driver);
        }

        WebElement importButton = driver.findElement(By.cssSelector("button[id*='installUpdate']"));
        assertThat(importButton.isDisplayed(), is(true));
        importButton.click();

        //Verify that the install bundle dialog is displayed.
        WebElement installDialog = driver.findElement(By.cssSelector("div[id*='installDialog']"));
        assertThat(installDialog.isDisplayed(), is(true));

        //Set the bundle being installed to start once installed.
        WebElement checkBoxDiv = installDialog.findElement(By.cssSelector("div[id*='startBundle']"));
        WebElement startBundleCheckBox = checkBoxDiv.findElement(By.cssSelector("div[class*='ui-chkbox-box']"));
        startBundleCheckBox.click();

        //Enter the location of the bundle to be uploaded.
        WebElement fileUpload = installDialog.findElements(By.tagName("input")).get(2);
        fileUpload.sendKeys(bundleFile.getAbsolutePath());

        GeneralHelper.safeClickBySelector(By.cssSelector("button[id*='importButton']"));
        
        if (returnPageButton != null)
        {
            NavigationHelper.navigateToPage(driver, returnPageButton);
        }        
    }

    /**
     * Method which navigates to the System Configurations page and automatically uninstalls a bundle
     * with the given bundle name.
     * @param driver
     *      Instance of the {@link WebDriver} which controls the navigation
     * @param bundleSymbolicName
     *      bundle symbolic name as listed in the bundles table on the System Configuration page. 
     *      (e.g. example.asset)
     * @param returnPageButton
     *      {@link NavigationButtonNameConstants} ID corresponding to the page to navigate back to after
     *      performing bundle installation.
     * @throws InterruptedException
     *      thrown in the event thread sleep is interrupted while waiting for page to render
     */
    public static void uninstallBundle(WebDriver driver, String bundleSymbolicName, String returnPageButton) 
        throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, BUNDLE_TAB_ID);
        if (returnPageButton != null)
        {
            sysConfigNavigationHelper(driver);
        }

        //Uses the bundle search bar feature to find the specified bundle.
        clearBundleFilter(driver);
        WebElement bundleRow = filterBundles(driver, bundleSymbolicName, true);

        WebElement uninstall = bundleRow.findElement(By.cssSelector("button[id*='uninstall']"));
        assertThat(uninstall.isDisplayed(), is(true));
        uninstall.click();

        //Verify that the uninstall dialog is displayed.
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='uninstallDialog']")));
        WebElement uninstallDialog = driver.findElement(By.cssSelector("div[id*='uninstallDialog']"));
        assertThat(uninstallDialog.isDisplayed(), is(true));

        WebElement uninstallButton = 
                uninstallDialog.findElement(By.cssSelector("button[id*='uninstallButton']"));
        uninstallButton.click();

        if (returnPageButton != null)
        {
            NavigationHelper.navigateToPage(driver, returnPageButton);
        } 
    }

    /**
     * Retrieves the search bar element and enters the name of the bundle to be searched for then optionally waits for 
     * that entry to be shown.
     * 
     * @param bundleSymbolicName
     *      symbolic name of the bundle being searched for 
     * @param driver
     *      Instance of the {@link WebDriver} which controls the navigation
     * @param shouldBeFound
     *      Whether bundle being filtered should be found. If so, wait for it to show.
     * @throws InterruptedException
     *      thrown in the event thread sleep is interrupted while waiting for page to render
     */
    public static WebElement filterBundles(WebDriver driver, String bundleSymbolicName, boolean shouldBeFound) 
            throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, BUNDLE_TAB_ID);
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id*='globalFilter']")));
        WebElement searchBar = driver.findElement(By.cssSelector("input[id*='globalFilter']"));
        
        GeneralHelper.retrySendKeys(driver, searchBar, bundleSymbolicName, 5);
        
        if (shouldBeFound)
        {
            MultipleByChain rows = waitForBundleFilter(driver, new Function<Collection<String>, Boolean>()
            {
                @Override
                public Boolean apply(Collection<String> bundleSymbolicNames)
                {
                    // wait for exactly 1 bundle being displayed
                    return bundleSymbolicNames.size() == 1;
                }
            });
            
            WebElement bundleNameCell = rows.extendChain()
                    .index(0) // first row (already confirmed index above)
                    .multiple(By.tagName("span"))
                    .index(2)
                    .build()
                    .findElement();
            
            assertThat(bundleNameCell.getText(), is("(" + bundleSymbolicName + ")"));
            
            return rows.extendChain().index(0).build().findElement();
        }
        else
        {
            waitForBundleFilter(driver, new Function<Collection<String>, Boolean>()
            {
                @Override
                public Boolean apply(Collection<String> bundleSymbolicNames)
                {
                    // wait for exactly 0 bundles
                    return bundleSymbolicNames.size() == 0;
                }
            });
            return null;
        }
    }

    /**
     * Retrieves the search bar element and clears it.
     * @param driver
     *      Instance of the {@link WebDriver} which controls the navigation
     * @throws InterruptedException
     *      thrown in the event thread sleep is interrupted while waiting for page to render
     */
    public static void clearBundleFilter(WebDriver driver) throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, BUNDLE_TAB_ID);
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id*='globalFilter']")));
        WebElement searchBar = driver.findElement(By.cssSelector("input[id*='globalFilter']"));
        searchBar.clear();
        searchBar.sendKeys(Keys.BACK_SPACE); // see clear doc, must send key to register event
        
        waitForBundleFilter(driver, new Function<Collection<String>, Boolean>()
        {
            @Override
            public Boolean apply(Collection<String> bundleSymbolicNames)
            {
                // wait for multiple bundles, don't know what the exact number should be, but more than would show in 
                // search results
                return bundleSymbolicNames.size() > 10;
            }
        });
    }
    
    /**
     * Method that either starts the specified bundle or ensures that it has been already started.
     * @param driver
     *      Instance of the {@link WebDriver} to be used.
     * @param bundleSymbolicName
     *      Symbolic name of the bundle to start.
     */
    public static void startBundle(WebDriver driver, String bundleSymbolicName) throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, BUNDLE_TAB_ID);
        
        //Uses the bundle search bar feature to find the specified bundle.
        clearBundleFilter(driver);
        filterBundles(driver, bundleSymbolicName, true);
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        DebugText.pushText("Attempting to click start button until active");
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement bundleTable = driver.findElement(By.cssSelector("tbody[id*='bundleTable_data']"));
                WebElement bundleState = bundleTable.findElements(By.tagName("span")).get(3);
                
                //Check if bundle is resolved or installed.
                if (bundleState.getText().equals("Resolved") || bundleState.getText().equals("Installed"))
                {
                    //Start the bundle.
                    bundleTable.findElement(By.cssSelector("button[id*='start']")).click();
                    return true;
                }
                else if (bundleState.getText().equals("Active")) //bundle has already been started
                {
                    return true;
                }
                return false;
            }
        });
        DebugText.popText();
        
        //wait until filtered bundle reaches an active state
        DebugText.pushText("Waiting for active state update");
        fwait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tbody span.bundleStateActive")));
        DebugText.popText();
        
        //Needed to give the bundle table time to update.
        GeneralHelper.sleepWithText(5, "Waiting for bundle table to completely update");
    }
    
    /**
     * Method that stops the specified bundle.
     * @param driver
     *      Instance of the {@link WebDriver} to be used.
     * @param bundleSymbolicName
     *      Symbolic name of the bundle to stop.
     */
    public static void stopBundle(WebDriver driver, String bundleSymbolicName) throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG, BUNDLE_TAB_ID);
        
        //Uses the bundle search bar feature to find the specified bundle.
        clearBundleFilter(driver);
        filterBundles(driver, bundleSymbolicName, true);
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        DebugText.pushText("Attempting to click stop button until active");
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement bundleTable = driver.findElement(By.cssSelector("tbody[id*='bundleTable_data']"));
                WebElement bundleState = bundleTable.findElements(By.tagName("span")).get(3);
        
                //Check if bundle is resolved. If it isn't resolved then return.
                if (bundleState.getText().equals("Active"))
                {
                    //Stop the bundle.
                    bundleTable.findElement(By.cssSelector("button[id*='stop']")).click();
                    return true;
                }
                return false;
            }
        });
        DebugText.popText();
        
        //wait until filtered bundle reaches a resolved state
        DebugText.pushText("Waiting for resolved state update");
        fwait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tbody span.bundleStateResolved")));
        DebugText.popText();
        
        //Needed to give the bundle table time to update.
        GeneralHelper.sleepWithText(5, "Waiting for bundle table to completely update");
    }

    /**
     * Helper method for navigating to the system configurations page.  
     * @param driver
     *      Instance of the {@link WebDriver} which controls the navigation
     * @throws InterruptedException
     *      thrown in the event thread sleep is interrupted while waiting for page to render
     */
    private static void sysConfigNavigationHelper(WebDriver driver) throws InterruptedException
    {
        NavigationHelper.navigateToPage(driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG);
        NavigationHelper.collapseSideBars(driver);
    }
    
    /**
     * Helper method used to verify that a bundle had been found after using the the search bar to filter bundles. 
     * @param driver
     *      Instance of the {@link WebDriver} which controls the navigation
     */
    private static MultipleByChain waitForBundleFilter(WebDriver driver, 
            final Function<Collection<String>, Boolean> expectedCondition)
    {
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(15, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        final MultipleByChain bundleRowsChain = ByChain.newBuilder()
                .single(By.cssSelector("tbody[id$='bundleTable_data']"))
                .multiple(By.tagName("tr"))
                .build();
        
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                List<WebElement> bundles = bundleRowsChain.findElements();
                
                List<String> bundleSymbolicNames = Lists.transform(bundles, new Function<WebElement, String>()
                {
                    @Override
                    public String apply(WebElement bundleRow)
                    {
                        List<WebElement> spans = bundleRow.findElements(By.tagName("span"));
                        if (spans.size() >= 2)  // sometimes there will be a table row that is empty with no spans
                        {
                            // remove parenthesis
                            return spans.get(2).getText().replace("(", "").replace(")", "");
                        }
                        else
                        {
                            return "";
                        }
                    }
                });
                Collection<String> filteredNames = Collections2.filter(bundleSymbolicNames, new Predicate<String>() 
                {
                    @Override
                    public boolean apply(String input)
                    {
                        return !Strings.isNullOrEmpty(input); // sometimes there will be a table row that is empty
                    }
                });
                
                return expectedCondition.apply(filteredNames);
            }
        });
        
        return bundleRowsChain;
    }
}
