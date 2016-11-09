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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class containing helper method that can be used across multiple types of tests.  
 * @author bachmakm
 *
 */
public class GeneralHelper
{
    /**
     * Maximum wait time, in seconds, to wait on stale elements in the DOM.
     */
    public static final int MAXIMUM_WAIT_TIME_SECS = 10;

    /**
     * Maximum number of retries to attempt when waiting on stale elements in the DOM.
     */
    public static final int MAX_STALE_ELEMENT_RETRIES = 5;

    /**
     * Name of the attribute that contains whether an accordion toggle is expanded.
     */
    private static final String EXPANDED_ATTR_KEY = "aria-expanded";

    private static final Logger LOG = Logger.getLogger("general.helper");
    
    /**
     * Helper method for returning the index associated with a particular element that
     * belongs to a list of elements.  Examples include elements in the configurations table
     * or elements part of a list of known controllers.  
     * @param attributeValue
     *      An attribute value containing the index of the desired element.  Can usually
     *      be obtained from an element's ID attribute.
     * @return
     *      the index of the desired element 
     */
    public static String getElementIndex(String attributeValue)
    {
        //value should look something like 'systemConfigTab:factoryConfigTable:10:j_idt71_header'
        //where 10 is the row index of the configurations table in this example
        String[] tokens = attributeValue.split(":");
        return tokens[2]; //contains index of row
    }
    
    /**
     * This is required because of https://code.google.com/p/selenium/issues/detail?id=4446.
     * 
     * @param element
     *      element to send keys to
     */
    public static void retrySendKeys(final WebDriver driver, final WebElement element, final CharSequence value, 
            final int timeoutSecs)
    {
        // give some time to be sure the clear will work
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        
        Wait<WebDriver> wait = new FluentWait<>(driver).withTimeout(timeoutSecs, TimeUnit.SECONDS);
        
        wait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver) 
            {
                element.clear();
                element.sendKeys(value);
                return element.getAttribute("value").equals(value);
            }
        });
    }
    
    /**
     * Toggle a given accordion element.
     * 
     * @param accordion
     *      accordion to toggle
     * @param panelText
     *      text of the panel that needs to be toggled
     * @param choice
     *      whether to expand or collapse panel
     */
    public static void toggleAccordion(final WebElement accordion, final String panelText, final ExpandCollapse choice)
    {
        List<WebElement> accordionHeaders = accordion.findElements(By.tagName("h3"));
        
        final String desiredExpandedState = choice == ExpandCollapse.EXPAND ? "true" : "false";
        
        WebElement desiredHeader = null;
        for (final WebElement accordionHeader : accordionHeaders)
        {
            WebElement linkElement = safeFindElement(accordionHeader, By.cssSelector("a"));

            if (linkElement.getText().equals(panelText))
            {
                desiredHeader = accordionHeader;
                break;
            }
        }
        
        assertThat(desiredHeader, is(notNullValue()));
                
        if (!desiredHeader.getAttribute(EXPANDED_ATTR_KEY).equals(desiredExpandedState)) 
        {
            LOG.log(Level.INFO, "Clicked accordion link [{0}] to: {1}", new Object[] {panelText, choice});
            desiredHeader.findElement(By.cssSelector("a")).click();
        }
        
        safeWaitUntil(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver arg0)
            {
                // find element again as the toggle can update the DOM
                List<WebElement> updatedHeaders = accordion.findElements(By.tagName("h3"));
                WebElement updatedHeader = null;
                for (final WebElement accordionHeader : updatedHeaders)
                {
                    WebElement linkElement = safeFindElement(accordionHeader, By.cssSelector("a"));

                    if (linkElement.getText().equals(panelText))
                    {
                        updatedHeader = accordionHeader;
                        break;
                    }
                }
                
                String actualExpandedState = updatedHeader.getAttribute(EXPANDED_ATTR_KEY);
                LOG.log(Level.INFO, "Expecting accordion state of [{0}] and is [{1}]", 
                        new Object[] {desiredExpandedState, actualExpandedState});
                return actualExpandedState.equals(desiredExpandedState);
            }
        });
    }
    
    /**
     * Debug method to highlight a given element to test whether the correct one is selected.
     * 
     * @param element
     *      element to highlight
     */
    public static void highlightElement(WebElement element) 
    { 
        for (int i = 0; i < 2; i++) 
        { 
            JavascriptExecutor js = (JavascriptExecutor)WebDriverFactory.retrieveWebDriver(); 
            js.executeScript("arguments[0].setAttribute('style', arguments[1]);",
                    element, "color: yellow; border: 2px solid yellow;"); 
            js.executeScript("arguments[0].setAttribute('style', arguments[1]);", element, ""); 
        }
    }
    
    /**
     * Sleep for the given time while placing the give message in the debug area.  Debug area will count down as well.
     */
    public static void sleepWithText(int seconds, String msg) throws IllegalStateException
    {
        LOG.log(Level.INFO, "{0} for {1} seconds...", new Object[] {msg, seconds});
        DebugText.pushText(msg + " " + seconds);
        while (seconds > 0)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new IllegalStateException(e);
            }
            seconds--;
            DebugText.replaceText(msg + " " + seconds);
        }
        DebugText.popText();
        LOG.info("Done waiting");
    }

    /**
     * Try to click the given element and allow the button to be stale, wait up to 10 seconds and retry up to 5 times.
     */
    public static void safeClick(final WebElement clickElement)
    {
        WebDriverWait wait = new WebDriverWait(WebDriverFactory.retrieveWebDriver(), MAXIMUM_WAIT_TIME_SECS);
        int retries = 0;
        while (true)
        {
            try
            {
                wait.until(ExpectedConditions.elementToBeClickable(clickElement)).click();
                return;
            }
            catch (StaleElementReferenceException e)
            {
                if (retries < MAX_STALE_ELEMENT_RETRIES)
                {
                    retries++;
                    continue;
                }
                else
                {
                    throw e;
                }
            }
        }
    }
    
    /**
     * Try to click the element retrieved by the given selector and allow the element to be stale or non-existent. 
     * Waits up to 10 seconds and retry every second.
     * 
     * @param selector
     *      {@link By} selector that specifies the element to be retrieved directly from the {@link WebDriver}.
     */
    public static void safeClickBySelector(final By selector)
    {
        final WebDriver driver = WebDriverFactory.retrieveWebDriver();
        final Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(MAXIMUM_WAIT_TIME_SECS, 
                TimeUnit.SECONDS).pollingEvery(1, TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, 
                        NoSuchElementException.class);
        
        // make sure element is visible first
        fwait.until(ExpectedConditions.elementToBeClickable(selector));
        
        final boolean clicked = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(final WebDriver driver)
            {
                driver.findElement(selector).click();
                return true;
            }
        });
        assertThat(clicked, is(true));
    }
    
    /**
     * Convenience method to find an element, but allow it to be stale before finding the one.
     * 
     * @param parent
     *      parent element that the element to find is contained in or null if the base web driver should be used to
     *      find the element.
     * @param by
     *      the locating mechanism
     * @return
     *      the element that has been found
     */
    public static WebElement safeFindElement(final WebElement parent, final By by)
    {
        DebugText.pushText("Safe wait for [%s]", by);
        
        try
        {
            WebDriver driver = WebDriverFactory.retrieveWebDriver();
            return new FluentWait<WebDriver>(driver)
                .withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS)
                .ignoring(StaleElementReferenceException.class)
                .until(new ExpectedCondition<WebElement>()
                {
                    @Override
                    public WebElement apply(WebDriver driver)
                    {   
                        if (parent == null)
                        {
                            return driver.findElement(by);
                        }
                        return parent.findElement(by);
                    }
                });
        }
        finally
        {
            DebugText.popText();
        }
    }

    /**
     * Convenience method to wait for the given condition (10 seconds) allowing for stale elements.
     * 
     * @param condition
     *      condition to wait for
     */
    public static <T> T safeWaitUntil(ExpectedCondition<T> condition)
    {
        DebugText.pushText("Wait 10 seconds for condition [%s]", condition);
        
        try
        {
            T value = new FluentWait<WebDriver>(WebDriverFactory.retrieveWebDriver())
                .withTimeout(10, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS)
                .ignoring(StaleElementReferenceException.class)
                .until(condition);
            
            return value;
        }
        finally
        {
            DebugText.popText();
        }
    }
    
    /**
     * Scroll the given element into view and click the element to keep it in view.
     * 
     * @param by
     *      selector for the element to find
     * @return
     *      element to scroll to after it has been scrolled to (after DOM update)
     */
    public static WebElement scrollElementIntoView(final By by)
    {
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        new FluentWait<WebDriver>(driver)
            .withTimeout(30, TimeUnit.SECONDS)
            .pollingEvery(1, TimeUnit.SECONDS)
            .ignoring(WebDriverException.class) // sometimes arguments[0] is not found so allow retries
            .until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", 
                            driver.findElement(by));
                    return true;
                }
            });

        // click to keep the element in view
        final WebElement webElement = driver.findElement(by);
        safeClick(webElement);

        // get element again as the DOM might have updated after the click
        return driver.findElement(by);
    }
}
