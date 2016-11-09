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
package mil.dod.th.ose.gui.integration.helpers.observation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper.AssetTabConstants;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.TimeUtil;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class provides helper methods to enable retrieval and deletion
 * of observations on the Observations Tab.
 * @author nickmarcucci
 *
 */
public class ObservationFilterHelper
{
    /**
     * Method that retrieves the WebElement that represents the filter accordion on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @return
     *      {@link WebElement} that represents the accordion panel.
     */
    public static WebElement retrieveFilterAccordionPanel(final WebDriver driver)
    {
        AssetHelper.chooseAssetTab(driver, AssetTabConstants.OBSERVATION_TAB);
        
        WebElement element = driver.findElement(By.cssSelector("div[id*='tabView:obsQueryPanel:obsAccordion']" +
                "[role='tablist']"));
        
        return element;
    }
    
    /**
     * Method that expands or collapses the "Filter" accordion panel on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param choice
     *      {@link ExpandCollapse} used to determine whether accordion should be expanded or collapsed.
     */
    public static void clickAccordionPanel(final WebDriver driver, 
            final String panelText, final ExpandCollapse choice)
    {
        WebElement accordion = retrieveFilterAccordionPanel(driver);

        GeneralHelper.toggleAccordion(accordion, panelText, choice);
    }
    
    /**
     * Method used to retrieve observation on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     */
    public static void executeObservationRetrieval(final WebDriver driver) 
        throws InterruptedException
    {
        //checks to see if retrieve button is visible. if not expand the accordion
        WebElement retrieveButton = isWebElementVisible(driver, 
                "button[id$='tabView:obsQueryPanel:obsAccordion:retrieveObs']");
       
        GrowlVerifier.verifyAndWaitToDisappear(20, retrieveButton, "Observation Retrieval Complete");
    }
    
    /**
     * Method used to delete observations on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     */
    public static void executeObservationDeletion(final WebDriver driver) throws InterruptedException
    {
        Thread.sleep(1000);
        //check if the element is visible
        WebElement deleteButton = isWebElementVisible(
                driver, "button[id$='tabView:obsQueryPanel:obsAccordion:deleteObs']");
        
        deleteButton.click();
        
        WebDriverWait wait = new WebDriverWait(driver, 10);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id='obsRemoveDlg']")));
        
        WebElement deleteDialog = driver.findElement(By.cssSelector("div[id='obsRemoveDlg']"));
        
        WebElement removeButton = deleteDialog.findElement(By.cssSelector("button[id$='confirmDeleteButton']"));
        
        assertThat(removeButton, notNullValue());
        
        GrowlVerifier.verifyAndWaitToDisappear(20, removeButton, "Observations Removed");
    }
    
    /**
     * Method checks the filter by number check box on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     */
    public static void clickNumberFilterCheckBox(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        
        WebElement checkBoxDiv = isWebElementVisible(driver, 
                "div[id$='tabView:obsQueryPanel:obsAccordion:retrieveMaxNumBox']");
        
        List<WebElement> obsNum = driver.findElements(By.cssSelector(
                "input[id$='tabView:obsQueryPanel:obsAccordion:obsNum']"));
        wait.until(ExpectedConditions.visibilityOf(checkBoxDiv));
        WebElement numberCheckBox = checkBoxDiv.findElement(By.cssSelector("div[class*='ui-chkbox-box']"));
        numberCheckBox.click();
        
        if (obsNum.isEmpty())
        {
            //If empty then the filter by  number is not currently selected and therefore should
            //wait till the filter by number panel is displayed before continuing.
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id$='obsNum']")));
        }
        else
        {
            //If not empty then filter by number is currently selected and therefore should wait till the filter by
            //number panel is no longer displayed before continuing.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("input[id$='obsNum']")));
        }
    }
    
    /**
     * Method that checks the filter by number check box on the observation tab and enters a number to filter by.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param numObs
     *      Integer that represents the number of observation that should be displayed.
     */
    public static void clickNumberCheckBoxAndEnterNumberOfObs(final WebDriver driver, final String numObs) 
        throws InterruptedException
    {
        clickNumberFilterCheckBox(driver);
        
        WebElement accordion = retrieveFilterAccordionPanel(driver);
        
        WebElement numObsTextBox = accordion.findElement(
                By.cssSelector("input[id$='tabView:obsQueryPanel:obsAccordion:obsNum']"));
        
        numObsTextBox.clear();
        numObsTextBox.sendKeys(numObs);
    }
    
    /**
     * Method that checks the filter by date check box on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     */
    public static void clickDateFilterCheckBox(final WebDriver driver) throws InterruptedException
    {
        WebElement checkBoxDiv = isWebElementVisible(driver, 
                "div[id$='tabView:obsQueryPanel:obsAccordion:retrieveDateBox']");
        WebDriverWait wait = new WebDriverWait(driver, 45);
        
        List<WebElement> startInput = driver.findElements(By.cssSelector("input[id$='startDateRetrieve_input']"));
        List<WebElement> endInput = driver.findElements(By.cssSelector("input[id$='endDateRetrieve_input']"));
        wait.until(ExpectedConditions.visibilityOf(checkBoxDiv));
        WebElement dateCheckBox = checkBoxDiv.findElement(By.cssSelector("div[class*='ui-chkbox-box']"));
        dateCheckBox.click();
        
        if (startInput.isEmpty() || endInput.isEmpty())
        {
            //If empty then the filter by date is not currently selected and therefore should
            //wait till the filter by date panel is displayed before continuing.
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[id$='startDateRetrieve_input']")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[id$='endDateRetrieve_input']")));
        }
        else
        {
            //If not empty then filter by date is currently selected and therefore should wait till the filter by
            //date panel is no longer displayed before continuing.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("input[id$='startDateRetrieve_input']")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("input[id$='endDateRetrieve_input']")));
        }
    }
    
    /**
     * Method that checks the filter by date check box on the observation tabs and enters a start and end date
     * with which to filter by.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param startDate
     *      {@link Calendar} that represents the start date to filter by.
     * @param endDate
     *      {@link Calendar} that represents the end date to filter by.
     */
    public static void clickDateCheckBoxAndEnterDateRange(final WebDriver driver, final Calendar startDate, 
            final Calendar endDate) throws InterruptedException
    {       
        clickDateFilterCheckBox(driver);
        setDateRetieveRange(driver, startDate, endDate);
    }
    
    /**
     * Method that sets the start date to retrieve by on observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param startDate
     *      {@link Calendar} that represents the start date to filter by.
     * @param endDate
     *      {@link Calendar} that represents the end date to filter by.
     */
    public static void setDateRetieveRange(final WebDriver driver, final Calendar startDate, 
            final Calendar endDate)
    {
        WebElement startDateInput = driver.findElement(By.cssSelector("input[id$='startDateRetrieve_input']"));
        GeneralHelper.retrySendKeys(driver, startDateInput, TimeUtil.getFormattedTime(startDate), 5);
        
        WebElement endDateInput = driver.findElement(By.cssSelector("input[id$='endDateRetrieve_input']"));
        GeneralHelper.retrySendKeys(driver, endDateInput, TimeUtil.getFormattedTime(endDate), 5);
    }
    
    /**
     * Method that sets the start date to filter by on observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param startDate
     *      {@link Calendar} that represents the start date to filter by.
     * @param endDate
     *      {@link Calendar} that represents the end date to filter by.
     */
    public static void setDateFilterRange(final WebDriver driver, final Calendar startDate, 
            final Calendar endDate)
    {
        WebElement startDateInput = driver.findElement(By.cssSelector("input[id$='startDateFilter_input']"));
        startDateInput.clear();
        startDateInput.sendKeys(TimeUtil.getFormattedTime(startDate));
        
        WebElement endDateInput = driver.findElement(By.cssSelector("input[id$='endDateFilter_input']"));
        endDateInput.clear();
        endDateInput.sendKeys(TimeUtil.getFormattedTime(endDate));
    }
    
    /**
     * Method that waits till the observation table displays a certain number of observation. Used to wait till the 
     * observation panel has been updated after clicking the retrieve observation button or a filter has been enacted.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param numOfObs
     *      Integer that represents the number of observations that should be displayed in the observation panel after
     *      either the retrieve observations button has been clicked or a filter has been enacted.
     */
    public static void waitTillObsTableUpdates(final WebDriver driver, final int numOfObs, int timeoutSecs) 
        throws InterruptedException
    {
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(timeoutSecs, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);
        fwait.until(new ExpectedCondition<Boolean>()
        {
            private int lastSize;
            
            @Override
            public Boolean apply(WebDriver driver)
            {
                List<WebElement> panels = driver.findElements(
                        By.cssSelector("div[id^='tabView'][id$='observationPanel']"));
                lastSize = panels.size();
                return panels.size() == numOfObs;
            }
            
            @Override
            public String toString()
            {
                return String.format("panel size expected is %d and was %d", numOfObs, lastSize);
            }
        });
    }
    
    public static void waitTillObsTableUpdatesWithAtLeast(final WebDriver driver, final int numOfObs, int timeoutSecs) 
        throws InterruptedException
    {
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(timeoutSecs, TimeUnit.SECONDS).
                pollingEvery(1, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);
        fwait.until(new ExpectedCondition<Boolean>()
        {
            private int lastSize;
            
            @Override
            public Boolean apply(WebDriver driver)
            {
                List<WebElement> panels = driver.findElements(
                        By.cssSelector("div[id^='tabView'][id$='observationPanel']"));
                lastSize = panels.size();
                return panels.size() >= numOfObs;
            }
            
            @Override
            public String toString()
            {
                return String.format("panel size expected is at least %d and was %d", numOfObs, lastSize);
            }
        });
    }
    
    /**
     * Method that checks the filter by expression box and enters the string provided
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param filterExpression
     *      the filter expression to enter into the text box for the filter expression
     */
    public static void clickFilterByExpressionAndEnterExpression(final WebDriver driver, 
            final String filterExpression)
    {
        clickFilterAccordionPanel(driver, ExpandCollapse.EXPAND);
        clickFilterByExpressionCheckBox(driver);
        enterFilterExpression(driver, filterExpression);
    }

    /**
     * Enter the filter expression assuming the text box is already available.
     * 
     * @param filterExpression
     *      expression to enter
     */
    public static void enterFilterExpression(final WebDriver driver, final String filterExpression)
    {
        WebElement accordion = retrieveFilterAccordionPanel(driver);

        WebElement filterTextBox = accordion.findElement(By.cssSelector("input[id$='obsFilter']"));
        
        filterTextBox.clear();
        filterTextBox.sendKeys(filterExpression);
        
        //click filter button
        accordion.findElement(By.cssSelector("button[id$='filterButton']")).click();
        
        GeneralHelper.sleepWithText(3, "Wait for DOM to update after AJAX update based on filtering");
    }
    
    /**
     * Click the filter button on the filter tab.
     * @param driver
     *      the current driver instance
     */
    public static void executeFiltering(final WebDriver driver) throws InterruptedException
    {
        //now get the accordion panel tab
        final WebElement filterTab = retrieveFilterAccordionPanel(driver);
        
        assertThat(filterTab, is(notNullValue()));
        
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        //click filter button
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                final WebElement button = 
                    filterTab.findElement(By.cssSelector(
                            "button[id$='tabView:obsQueryPanel:obsAccordion:filterButton']"));
                button.click();
                return true;
            }
        });
    }
    
    /**
     * Method that checks the filter by date box and enters the start/end date to filter by on observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param startDate
     *      {@link Calendar} that represents the start date to filter by.
     * @param endDate
     *      {@link Calendar} that represents the end date to filter by.
     */
    public static void clickFilterByDateAndEnterDates(final WebDriver driver, 
            final Calendar startDate, final Calendar endDate) throws InterruptedException
    {
        clickFilterAccordionPanel(driver, ExpandCollapse.EXPAND);
        clickFilterByDateCheckBox(driver);
        
        setDateFilterRange(driver, startDate, endDate);
    }
    
    /**
     * Method checks the filter by expression check box on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     */
    public static void clickFilterByExpressionCheckBox(final WebDriver driver)
    {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='filteringPanel']")));
        WebElement checkBoxDiv = 
                driver.findElement(By.cssSelector("div[id$='tabView:obsQueryPanel:obsAccordion:filterStringBox']"));
        
        List<WebElement> obsFilterText = driver.findElements(By.cssSelector("input[id$='obsFilter']"));

        WebElement numberCheckBox = checkBoxDiv.findElement(By.cssSelector("div[class*='ui-chkbox-box']"));
        GeneralHelper.safeClick(numberCheckBox);
        
        if (obsFilterText.isEmpty())
        {
            //If empty then the filter by expression was not selected, wait until text box is visible
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[id$='obsFilter']")));
        }
        else
        {
            //the filter by expression was selected, wait until text box is invisible
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("input[id$='obsFilter']")));
        }
    }
    
    /**
     * Clears the expression and clicks the checkbox of the expression filter.
     * Assumes that the panel is open and the box is currently checked.
     * @param driver
     *      The selenium web driver.
     */
    public static void clearFilterByExpression(final WebDriver driver)
    {
        //Clear and remove the filter.
        enterFilterExpression(driver, "");
        clickFilterByExpressionCheckBox(driver);
    }
    
    /**
     * Method checks the filter by date check box on the observation tab.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     */
    public static void clickFilterByDateCheckBox(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 30);
        
        wait.until(ExpectedConditions.visibilityOf(driver.findElement(By.cssSelector("div[id$='filterByDate']"))));
        WebElement checkBoxDiv = driver.findElement(By.cssSelector("div[id$='filterByDate']"));
        
        //collect possible elements
        List<WebElement> startInput = driver.findElements(By.cssSelector("input[id$='startDateFilter_input']"));
        List<WebElement> endInput = driver.findElements(By.cssSelector("input[id$='endDateFilter_input']"));
        //click the checkbox
        WebElement dateCheckBox = checkBoxDiv.findElement(By.cssSelector("div[class*='ui-chkbox-box']"));
        dateCheckBox.click();
        
        if (startInput.isEmpty() || endInput.isEmpty())
        {
            //If empty then the filter by date is not currently selected and therefore should
            //wait till the filter by date panel is displayed before continuing.
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[id$='startDateFilter_input']")));
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input[id$='endDateFilter_input']")));
        }
        else
        {
            //If not empty then filter by date is currently selected and therefore should wait till the filter by
            //date panel is no longer displayed before continuing.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("input[id$='startDateFilter_input']")));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("input[id$='endDateFilter_input']")));
        }
    }
    
    /**
     * Method used to determine if the specified element is currently visible.
     * 
     * @param driver
     *      {@link WebDriver} currently in use for running the tests.
     * @param elementId
     *      The string value used to retrieve an element with a css selector.
     * @return
     *      The web element that was being checked for visibility.
     */
    private static WebElement isWebElementVisible(final WebDriver driver, 
            final String elementId) throws InterruptedException
    {
        final WebElement accordion = retrieveFilterAccordionPanel(driver);
        
        WebElement elementToCheck = accordion.findElement(By.cssSelector(elementId));
        
        //checks to see if retrieve button is visible. if not expand the accordion
        if (!elementToCheck.isDisplayed())
        {
            Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                    pollingEvery(2, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                            NoSuchElementException.class);
            
            //click filter button
            fwait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    try
                    {
                        clickRetrieveDeleteAccordionPanel(driver, ExpandCollapse.EXPAND);
                    }
                    catch (InterruptedException e)
                    {
                        //might happen, this is an inner class def gotta catch it
                    }
                    return  accordion.findElement(By.cssSelector(elementId)).isDisplayed();
                }
            });
            
            return accordion.findElement(By.cssSelector(elementId));
        }
        
        return elementToCheck;
    }
    
    public enum ExpandCollapse
    {
        EXPAND,
        COLLAPSE
    }
    
    public static void clickFilterAccordionPanel(WebDriver driver, ExpandCollapse expand)
    {
        clickAccordionPanel(driver, "Filter", expand);
    }

    public static void clickRetrieveDeleteAccordionPanel(WebDriver driver, ExpandCollapse expand) 
        throws InterruptedException
    {
        clickAccordionPanel(driver, "Retrieve or Delete Observations", expand);
    }
}
