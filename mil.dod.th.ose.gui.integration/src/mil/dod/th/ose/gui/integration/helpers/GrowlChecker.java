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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier.GrowlAction;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

/**
 * This class is used to verify that a growl message appears and that the message is of the appropriate type. This class
 * implements {@link Callable} so that growl message can be checked for while performing the action that causes the 
 * growl message that way a timed growl message shouldn't be missed and cause a test failure. This class returns a 
 * true boolean value if the growl message was found and false otherwise.
 * 
 * @author cweisenborn
 */
public class GrowlChecker implements Callable<Boolean>
{
    /**
     * Title of the message that should appear.
     */
    private String m_Title;
    
    /**
     * Timeout in seconds that the checker should wait for the message to appear.
     */
    private int m_Timeout;
    
    private GrowlAction m_Action;
    
    /**
     * Constructor method used to create the growl checker.
     * 
     * @param title
     *          Title of the growl message that should appear.
     * @param action
     *          Action to take with the growl message.
     * @param timeoutInSeconds
     *          If action is {@link GrowlAction#NO_WAIT}, length of time in seconds to wait for the growl message to 
     *          appear. If action is {@link GrowlAction#WAIT_TO_DISAPPEAR}, length of time is split in half between 
     *          waiting for the message to appear and waiting for the message to disappear.
     */
    public GrowlChecker(final String title, final GrowlAction action, 
            final int timeoutInSeconds)
    {
        m_Title = title;
        m_Action = action;
        // if waiting to disappear, the timeout will be used twice, so half the timeout for each wait for a total 
        // equaling the desired timeout
        if (m_Action == GrowlAction.WAIT_TO_DISAPPEAR)
        {
            m_Timeout = timeoutInSeconds / 2;
        }
        else
        {
            m_Timeout = timeoutInSeconds;
        }
    }

    @Override
    public Boolean call() throws Exception
    {        
        final List<Class<? extends Throwable>> exceptionList = new ArrayList<Class<? extends Throwable>>();
        exceptionList.add(NoSuchElementException.class);
        exceptionList.add(StaleElementReferenceException.class);
        exceptionList.add(ElementNotVisibleException.class);
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        final Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(m_Timeout, 
                TimeUnit.SECONDS).pollingEvery(1, TimeUnit.SECONDS).ignoreAll(exceptionList);
        
        // wait for growl message to show up
        final Boolean result = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return findGrowlMessage(driver, m_Title) != null;
            }
            
            @Override
            public String toString()
            {
                return String.format("growl message with title [%s] and timeout=%d", m_Title, 
                        m_Timeout);
            }
        });
        
        if (m_Action == GrowlAction.WAIT_TO_DISAPPEAR)
        {
            fwait.until(new ExpectedCondition<Boolean>()
            {
                @Override
                public Boolean apply(WebDriver driver)
                {
                    return findGrowlMessage(driver, m_Title) == null;
                }
                
                @Override
                public String toString()
                {
                    return String.format("growl message with title to disappear [%s], timeout=%d", 
                            m_Title, m_Timeout);
                }
            });
        }
        return result;
    }
    
    /**
     * Method used to find the element that represents the growl message with the desired title.
     * 
     * @param driver
     *          Current web driver.
     * @param title
     *          Title of the growl message to be found.
     * @return
     *          {@link WebElement} that represents the growl message being searched for.
     */
    public static WebElement findGrowlMessage(final WebDriver driver, final String title)
    {
        //Retrieve a list of all growl messages displayed.
        final List<WebElement> growlMsgs = driver.findElements(By.cssSelector("div[class='ui-growl-item']"));
        
        //Find the growl message with the specified title.
        for (final WebElement growlMsg : growlMsgs)
        {
            final WebElement growlHeader = growlMsg.findElement(By.cssSelector("span[class='ui-growl-title']"));
            
            if (growlHeader.getText().equals(title))
            {
                return growlMsg;
            }
        }
        return null;
    }
}
