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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.concurrent.TimeUnit;

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import mil.dod.th.ose.shared.StringUtils;

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
 * Comms helper to assist with add comms dialog functions with assumptions that an active controller with the example
 * platform bundle.
 * 
 * @author bachmakm
 *
 */
public class CommsAddDialogHelper
{
    /**
     * Method that finds and returns the WebElement that represents the specified comms layer type on the add comms
     * stack dialog. This method returns null if the specified comms layer type cannot be found.
     * 
     * @param driver
     *            Instance of the {@link WebDriver} the comm type will be retrieved from.
     * @param layerTypeName
     *            Name of the layer type to be retrieved.
     * @return {@link WebElement} that represents the specified layer type or <code>null</code> if it cannot be found.
     */
    public static WebElement retrieveCommsLayerType(final WebDriver driver, final String layerTypeName)
    {
        // Retrieve the list of all layer types and search for an layer type with the specified name.
        // tbody contains the <tr>s of layer types, the second <td> child of parent <tr> should contain
        // the layer type.
        for (WebElement layerType : driver.findElements(
                By.cssSelector("tbody[class='ui-datatable-data ui-widget-content']>tr>td:nth-child(2)")))
        {
            if (layerType.getText().contains(layerTypeName))
            {
                return layerType;
            }
        }
        return null;
    }

    /**
     * Method that finds and return the WebElement that represents the specified comms layer type image on the add comms
     * stack dialog. This method returns null if the specified comms layer image cannot be found.
     * 
     * @param driver
     *            the web driver to use.
     * @param layerTypeName
     *            the name of the layer type to be retrieved.
     * @return the element that represents the specified layer type image or <code>null</code> if it cannot be found.
     */
    public static WebElement retrieveCommsImageForLayerType(final WebDriver driver, final String layerTypeName)
    {
        for (WebElement table : driver.findElements(
                By.cssSelector("tbody[class='ui-datatable-data ui-widget-content']")))
        {
            for (WebElement rowName : table.findElements(By.cssSelector("tr")))
            {
                List<WebElement> tableCellsWithName = rowName.findElements(By.cssSelector("td:nth-child(2)"));

                for (WebElement cell : tableCellsWithName)
                {
                    if (cell.getText().contains(layerTypeName))
                    {
                        WebElement tableImage = rowName.findElement(By.cssSelector("td:nth-child(1)"));

                        return tableImage.findElement(By.tagName("img"));
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gets the name of the top-most available physical link available for use.
     */
    public static String getTopPhysicalLinkName(final WebDriver driver)
    {
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(2,
                TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, NoSuchElementException.class);

        fwait.until(
                ExpectedConditions.presenceOfElementLocated(By.cssSelector("tbody[id$='physicalData_data']>tr>td")));
        try
        {
            WebElement topPhysElement = driver.findElement(
                    By.cssSelector("tbody[id$='physicalData_data']>tr>td:nth-child(2)"));
            return CommsGeneralHelper.getHiddenText(driver, topPhysElement.findElement(By.tagName("div"))).trim();
        }
        catch (NoSuchElementException e)
        {
            return null;
        }
    }

    /**
     * Helper method for determining whether or not a tab is open.
     * 
     * @param driver
     *            Instance of the {@link WebDriver} on which the comms stack will be made.
     * @param tabText
     *            Header text for a tab. Used to identify which tab content to check.
     * @return <code>true</code>
     */
    private static boolean isTabActive(WebDriver driver, String tabText)
    {
        if (tabText.contains("Physical Link"))
        {
            return driver.findElement(By.cssSelector("div[id$='physTab']")).isDisplayed();
        }
        else if (tabText.contains("Link Layer"))
        {
            return driver.findElement(By.cssSelector("div[id$='linkTab']")).isDisplayed();
        }
        return driver.findElement(By.cssSelector("div[id$='transTab']")).isDisplayed();
    }

    /**
     * Helper method opens layer tab and selects a layer on the Add Comms Stack Dialog. If given layer type is
     * <code>null</code>, method only opens layer tab.
     * 
     * @param driver
     *            Instance of the {@link WebDriver} on which the comms stack will be made.
     * @param tabText
     *            Header text for a tab. Used to identify and open one of the three tabs on the add comms dialog.
     * @param layerType
     *            type of the layer to be selected from the list of available types
     */
    public static WebElement layerSelectHelper(WebDriver driver, String tabText, final String layerType)
            throws InterruptedException
    {
        if (!isTabActive(driver, tabText))
        {
            for (WebElement tab : driver.findElements(By.cssSelector("h3[role='tab']")))
            {
                if (tab.getText().contains(tabText))
                {
                    tab.click();
                    // Thread sleep needed to allow for the tab panel to open.
                    Thread.sleep(3000);
                    break;
                }
            }
        }
        if (layerType != null)
        {
            // Return the specified comm type.
            return retrieveCommsLayerType(driver, layerType);
        }
        return null;
    }

    /**
     * Helper to select the next page in the Add Comms Stack Dialog.
     * 
     * @param driver
     *            Instance of the {@link WebDriver} on which the comms stack will be made.
     */
    public static void selectNextCommsLayerPage(final WebDriver driver)
    {
        WebElement here = driver.findElement(
                By.cssSelector("th[id*='addCommsForm:addAccordion:transportData_paginator_top']"));
        int counter = 0;

        for (WebElement looking : here.findElements(By.tagName("span")))
        {
            if (counter == 6)
            {
                looking.click();

                // Opening the Specs page
                WebElement specsButton = driver.findElement(
                        By.cssSelector("button[id*='addCommsForm:addAccordion:transportData:3:transSpecs']"));
                specsButton.click();

                // Closing the Specs page
                WebElement capabilHeader = driver.findElement(By.cssSelector("div[id*='CapabilitiesHeader']"));
                WebElement closeDialog = capabilHeader.findElement(
                        By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
                closeDialog.click();
                break;
            }
            counter++;
        }
    }
    
    /**
     * Helper to select back page in the Add Comms Stack Dialog.
     * 
     * @param driver
     *            Instance of the {@link WebDriver} on which the comms stack will be made.
     */
    public static void selectBackCommsLayerPage(final WebDriver driver)
    {
        WebElement here = driver.findElement(
                By.cssSelector("th[id*='addCommsForm:addAccordion:transportData_paginator_top']"));
        int counter = 0;

        for (WebElement looking : here.findElements(By.tagName("span")))
        {
            if (counter == 2)
            {
                looking.click();

                // Opening the Specs page
                WebElement specsButton = driver.findElement(
                        By.cssSelector("button[id*='addCommsForm:addAccordion:transportData:0:transSpecs']"));
                specsButton.click();
                
                // Closing the Specs page
                WebElement capabilHeader = driver.findElement(By.cssSelector("div[id*='CapabilitiesHeader']"));
                WebElement closeDialog = capabilHeader.findElement(
                        By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
                closeDialog.click();
                break;
            }
            counter++;
        }
    }

    public static void forceAddPhysicalLink(final WebDriver driver, final String name) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 5);

        // Bring up the add comms dialog.
        WebElement addCommsButton = driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        addCommsButton.click();

        // Wait until the add comms dialog is visible.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div[id$='addAccordion']")));

        // open physical link tab and select force-add checkbox
        CommsAddDialogHelper.layerSelectHelper(driver, "Physical Link", null);

        WebElement forceAddCheckBox = driver.findElement(
                By.cssSelector("input[id$='addAccordion:forceAddCheckBox_input']"));
        wait.until(ExpectedConditions.visibilityOf(forceAddCheckBox));
        forceAddCheckBox.click();

        // Thread sleep needed to allow add comms stack time to change to force add physical link dialog.
        Thread.sleep(1000);

        // ensure that physical link tab is open click on the link type
        CommsAddDialogHelper.layerSelectHelper(driver, "Physical Link", null);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='physicalTypesData']")));
        WebElement layerType = CommsGeneralHelper.retrieveCommsLayerType(driver, PhysicalLinkTypeEnum.I_2_C.toString());
        layerType.click();

        // create physical link with name specified
        WebElement physicalName = driver.findElement(By.cssSelector("input[id$='physicalName']"));
        physicalName.clear();
        physicalName.sendKeys(name);

        // click on confirm stack button
        WebElement confirmStackButton = driver.findElement(By.cssSelector("button[id$='confirmStackButton']"));
        confirmStackButton.click();

        // wait until the confirm stack table is shown and assert that the information shown is correct
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span[id$='confirmStack']")));
        List<WebElement> physicalConfirmInfo = driver.findElements(By.cssSelector("tr[class='ui-widget-content']>td"));
        assertThat(physicalConfirmInfo.get(1).getText(),
                containsString(StringUtils.splitCamelCase(PhysicalLinkTypeEnum.I_2_C.value())));
        assertThat(physicalConfirmInfo.get(0).getText(), containsString(name));

        // submit the form
        WebElement submitButton = driver.findElement(By.cssSelector("button[id$='confirmSubmitButton']"));
        submitButton.click();

        // instantiate wait objects - fluent wait needs to be 2 seconds
        Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(2,
                TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, NoSuchElementException.class);

        // Wait for the created physical link to appear in the stack list
        Boolean result = fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                return CommsGeneralHelper.retrieveStack(driver, name) != null;
            }
        });

        assertThat(result, is(true));

        // Thread sleep needed. This is due to the fact that the stack may have been created and appear in the list
        // but the list may not be done rendering. This means that any attempt to use the stack directly after this
        // method may encounter a stale element since rendering is not complete.
        Thread.sleep(2000);
    }

    /**
     * Method used to put the create comm stack dialog into the force add physical link mode.
     * 
     * @param driver
     *            Web driver to be used.
     */
    public static void putInForceAddPhysicalLinkMode(final WebDriver driver) throws InterruptedException
    {
        WebDriverWait wait = new WebDriverWait(driver, 10);

        // ensure that the Physical Link tab is open
        CommsAddDialogHelper.layerSelectHelper(driver, "Physical Link", null);

        // go to force-add physical link type list
        WebElement forceAddCheckBox = driver.findElement(
                By.cssSelector("input[id$='addAccordion:forceAddCheckBox_input']"));
        wait.until(ExpectedConditions.visibilityOf(forceAddCheckBox));
        forceAddCheckBox.click();

        // Wait till the link tab is no longer available. This means that force add physical link dialog is displayed.
        Wait<WebDriver> linkTabWait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(
                2, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, StaleElementReferenceException.class);

        linkTabWait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                try
                {
                    driver.findElement(By.id("addCommsForm:addAccordion:linkTab"));
                }
                catch (NoSuchElementException exception)
                {
                    return true;
                }
                return false;
            }
        });
    }
}
