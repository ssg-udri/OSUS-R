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
package mil.dod.th.ose.gui.integration.webapp.comms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.types.ccomm.PhysicalLinkTypeEnum;

import mil.dod.th.ose.gui.integration.helpers.BundleManagerUtil;
import mil.dod.th.ose.gui.integration.helpers.CommsAddDialogHelper;
import mil.dod.th.ose.gui.integration.helpers.CommsGeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.util.ResourceLocatorUtil;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
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
 * Class for testing the successful operation of different functionalities associated with the add comms dialog.
 * 
 * @author bachmakm
 *
 */
public class TestAddCommsDlg
{
    private static WebDriver m_Driver;
    private static WebDriverWait wait;

    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();

        wait = new WebDriverWait(m_Driver, 30);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='channelsButton']")));

        CommsGeneralHelper.commsTestSetupHelper(m_Driver);
    }

    @After
    public void cleanUp() throws InterruptedException, ExecutionException, TimeoutException
    {
        CommsGeneralHelper.commsTestCleanUpHelper(m_Driver);
    }

    /**
     * Assuming default controller is the active controller, assuming example platform and example good plugin bundles
     * are in the controller. Verify the example physical link, example link layer, echo transport, and direct transport
     * layers show up in the layer type lists.
     */
    @Test
    public void testCreateStackDialog() throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);

        // Bring up the add comms dialog
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        addCommsButton.click();

        // Wait until the add comms dialog is visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='addAccordion']")));

        // Test ExampleLinkLayer
        WebElement layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Link Layer",
                "example.ccomms.ExampleLinkLayer");
        assertThat(layer, is(notNullValue()));

        WebElement layerIcon = CommsAddDialogHelper.retrieveCommsImageForLayerType(m_Driver,
                "example.ccomms.ExampleLinkLayer");
        assertThat(layerIcon, notNullValue());

        ImageHelper.getPictureOrIconNameAndVerify(layerIcon, "comms_line_of_sight.png", ".png");

        // Assert Link Layer Description, Specs, Images
        layer.click();

        // wait for text to change
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("div[id*='llDescription']"), "Select a Layer to see Description.")));
        WebElement desc = m_Driver.findElement(By.cssSelector("div[id*='llDescription']"));
        assertThat(desc.getText(), containsString("Example Link Layer"));
        assertSpecsButton("example.ccomms.ExampleLinkLayer", "ExampleLinkLayer");

        // Test DirectTransport
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.DirectTransport");
        if (layer == null)
        {
            CommsAddDialogHelper.selectNextCommsLayerPage(m_Driver);
            layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                    "example.ccomms.DirectTransport");
        }
        assertThat(layer, is(notNullValue()));

        layerIcon = CommsAddDialogHelper.retrieveCommsImageForLayerType(m_Driver, "example.ccomms.DirectTransport");
        assertThat(layerIcon, notNullValue());

        ImageHelper.getPictureOrIconNameAndVerify(layerIcon, "comms_generic.png", ".png");

        // Assert Description, Specs, Images
        layer.click();

        // wait for text to change
        wait.until(ExpectedConditions.not(ExpectedConditions.textToBePresentInElementLocated(
                By.cssSelector("div[id*='transDesc']"), "Select a Layer to see Description.")));
        desc = m_Driver.findElement(By.cssSelector("div[id*='transDesc']"));
        assertThat(desc.getText(),
                containsString("An example transport layer that sends messages to another Direct transport layer"));
        assertSpecsButton("example.ccomms.DirectTransport", "Direct Transport");

        // Test EchoTransport
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.EchoTransport");

        // If there are more then 3 in the list, select next page, then check again
        if (layer == null)
        {
            CommsAddDialogHelper.selectNextCommsLayerPage(m_Driver);
            layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.EchoTransport");
            if (layer == null)
            {
                CommsAddDialogHelper.selectBackCommsLayerPage(m_Driver);
                layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                        "example.ccomms.EchoTransport");
            }
        }
        assertThat(layer, is(notNullValue()));

        layerIcon = CommsAddDialogHelper.retrieveCommsImageForLayerType(m_Driver, "example.ccomms.EchoTransport");
        assertThat(layerIcon, notNullValue());

        ImageHelper.getPictureOrIconNameAndVerify(layerIcon, "comms_generic.png", ".png");

        // Assert Description, Specs, Images
        layer.click();

        // wait for text to change
        wait.until(ExpectedConditions.not(
                ExpectedConditions.textToBePresentInElementLocated(By.cssSelector("div[id*='transDesc']"),
                        "An example transport layer that sends messages to another Direct transport layer")));
        desc = m_Driver.findElement(By.cssSelector("div[id*='transDesc']"));
        assertThat(desc.getText(), containsString(
                "An example transport layer that echoes messages back through the same transport layer"));
        assertSpecsButton("example.ccomms.EchoTransport", "Echo Transport");

        // TODO: TH-1150 - Need to add tests for description and specs button, once those capabilities is working.

        // open the force add physical link dialog.
        CommsAddDialogHelper.putInForceAddPhysicalLinkMode(m_Driver);

        // ensure that link tab is open
        CommsAddDialogHelper.layerSelectHelper(m_Driver, "Physical Link", null);

        // wait until list of physical types is visible
        wait.until(
                ExpectedConditions.visibilityOf(m_Driver.findElement(By.cssSelector("div[id$='physicalTypesData']"))));

        // assert that known physical types exist and that specs contain proper info
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Physical Link",
                PhysicalLinkTypeEnum.I_2_C.toString());
        assertThat(layer, is(notNullValue()));
        assertSpecsButton(PhysicalLinkTypeEnum.I_2_C.toString(), "ExamplePhysicalLink");

        layerIcon = CommsAddDialogHelper.retrieveCommsImageForLayerType(m_Driver,
                PhysicalLinkTypeEnum.I_2_C.toString());
        assertThat(layerIcon, notNullValue());

        ImageHelper.getPictureOrIconNameAndVerify(layerIcon, "comms_serial.png", ".png");

        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Physical Link",
                PhysicalLinkTypeEnum.SERIAL_PORT.toString());
        assertThat(layer, is(notNullValue()));
        assertSpecsButton(PhysicalLinkTypeEnum.SERIAL_PORT.toString(), "Example Serial Port");

        layerIcon = CommsAddDialogHelper.retrieveCommsImageForLayerType(m_Driver,
                PhysicalLinkTypeEnum.SERIAL_PORT.toString());
        assertThat(layerIcon, notNullValue());

        ImageHelper.getPictureOrIconNameAndVerify(layerIcon, "comms_serial.png", ".png");

        // go back to original add comms panel
        WebElement forceAddCheckBox = m_Driver.findElement(
                By.cssSelector("input[id$='addAccordion:forceAddCheckBox_input']"));
        wait.until(ExpectedConditions.visibilityOf(forceAddCheckBox));
        forceAddCheckBox.click();

        // Wait till the link tab available. This means that force add physical link dialog is no longer displayed.
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("addCommsForm:addAccordion:linkTab")));

        // ---close the dialog---
        WebElement closeButton = m_Driver.findElement(By.cssSelector("button[id*='cancelStackButton']"));
        closeButton.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id$='addCommsDialog']")));
    }

    /**
     * For a given partial id, find the element in the add comms accordion.
     * 
     * @param layerType
     *            layer type as listed in the layer tables (e.g. example.ccomms.ExampleLinkLayer)
     * @param productName
     *            the product name, from the Capabilities document; used for validation.
     */
    private void assertSpecsButton(String layerType, String productName)
    {
        String selector = "tr[data-rk='" + layerType + "']>td:nth-child(3)>div>button";

        // assert that specs buttons are working for each layer
        WebElement button = m_Driver.findElement(By.cssSelector(selector));
        wait.until(ExpectedConditions.visibilityOf(button));
        button.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id*='CapabilitiesHeader']")));
        WebElement capabilHeader = m_Driver.findElement(By.cssSelector("div[id*='CapabilitiesHeader']"));
        WebElement header = capabilHeader.findElement(By.cssSelector("span[class='ui-dialog-title']"));
        assertThat(header.getText(), containsString("Capabilities Document"));

        // --verify capabilities are displayed---
        WebElement caps = capabilHeader.findElement(By.cssSelector("div[id*='TreeTable']"));
        assertThat(caps.getText().isEmpty(), is(false));

        // --verify specific item to capabilities
        // TODO: TH-1274 Improve testing.
        assertThat(caps.getText(), containsString(productName));

        // ---close the dialog---
        WebElement closeDialog = capabilHeader.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        assertThat(closeDialog.isDisplayed(), is(true));
        closeDialog.click();
    }

    /**
     * Assuming default controller is the active controller, assuming example platform and example good plugin bundles
     * are in the controller. Verify stack is created and layers are displayed on GUI.
     */
    @Test
    public void testCreateStack() throws InterruptedException, ExecutionException, TimeoutException
    {
        NavigationHelper.pageAndTabCheck(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        String physName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);

        // create stack containing transport
        CommsGeneralHelper.createStack(m_Driver, physName, "duck", "example.ccomms.ExampleLinkLayer", "goose",
                "example.ccomms.DirectTransport");

        // wait for elements to appear before 'grabbing' them
        wait.until(ExpectedConditions.visibilityOf(CommsGeneralHelper.retrieveStack(m_Driver, "goose")));

        WebElement table = CommsGeneralHelper.retrieveStack(m_Driver, "goose");
        List<WebElement> commsTableElements = table.findElements(By.cssSelector("tbody>tr+tr>td+td>table>tbody>tr"));

        // verify
        assertThat(commsTableElements.size(), is(3));
        assertThat(commsTableElements.get(0).getText(), containsString("goose"));
        assertThat(commsTableElements.get(1).getText(), containsString("duck"));
        assertThat(commsTableElements.get(2).getText(), containsString(physName));

        CommsGeneralHelper.removeStack(m_Driver, "goose");
    }

    /**
     * Verify all expected validation errors are displayed.
     */
    @Test
    public void testCreateStackValidationFail() throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);

        // Bring up the add comms dialog
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        addCommsButton.click();

        // Wait til the add comms dialog is visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='addAccordion']")));

        // select a transport link to test validation of transport layer inputs
        WebElement layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                "example.ccomms.DirectTransport");
        // If there are more then 3 in the list, select next page, then check again
        if (layer == null)
        {
            CommsAddDialogHelper.selectNextCommsLayerPage(m_Driver);
            layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                    "example.ccomms.DirectTransport");
        }
        assertThat(layer, is(notNullValue()));
        layer.click();
        wait.until(ExpectedConditions.textToBePresentInElementValue(
                By.cssSelector("input[name$='transportData_selection']"), "example.ccomms.DirectTransport"));

        // click confirm stack to create new stack
        WebElement confirmStackButton = m_Driver.findElement(By.cssSelector("button[id$='confirmStackButton']"));
        confirmStackButton.click();

        // wait until close validation dialog button appears then click it
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='closeValidationButton']")));
        WebElement closeButton = m_Driver.findElement(By.cssSelector("button[id*='closeValidationButton']"));
        closeButton.click();

        // grab all validation errors
        List<WebElement> validationErrors = m_Driver.findElements(
                By.cssSelector("span[class='ui-message-error-detail']"));

        // add each validation error from dialog to an array list
        List<String> dialogErrors = new ArrayList<String>();
        for (WebElement error : validationErrors)
        {
            dialogErrors.add(CommsGeneralHelper.getHiddenText(m_Driver, error));
        }

        // assert that all of the dialog errors match the known errors
        assertThat(dialogErrors, hasItems("Type must be selected.", "Enter a name for the new link layer.",
                "Physical link must be selected.", "Enter a name for the new transport layer."));

        // open the force add physical link dialog
        CommsAddDialogHelper.putInForceAddPhysicalLinkMode(m_Driver);

        // ensure that the Physical Link tab is still open
        CommsAddDialogHelper.layerSelectHelper(m_Driver, "Physical Link", null);

        // assert that validation errors have been cleared
        validationErrors = m_Driver.findElements(By.cssSelector("span[class='ui-message-error-detail']"));
        assertThat(validationErrors.size(), is(0));

        // click confirm
        confirmStackButton = m_Driver.findElement(By.cssSelector("button[id$='confirmStackButton']"));
        confirmStackButton.click();

        // wait until validation error dlg button appears and click it
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='closeValidationButton']")));
        closeButton = m_Driver.findElement(By.cssSelector("button[id*='closeValidationButton']"));
        closeButton.click();

        // retrieve new list of validation errors from dialog
        validationErrors = m_Driver.findElements(By.cssSelector("span[class='ui-message-error-detail']"));

        // add each validation error from dialog to an array list
        dialogErrors.clear();
        for (WebElement error : validationErrors)
        {
            dialogErrors.add(CommsGeneralHelper.getHiddenText(m_Driver, error));
        }

        // assert that all of the dialog errors match the known errors
        assertThat(dialogErrors, hasItems("Type must be selected.", "Enter a name for the new physical layer."));

        // ---close the dialog---
        closeButton = m_Driver.findElement(By.cssSelector("button[id*='cancelStackButton']"));
        closeButton.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id$='addCommsDialog']")));
        m_Driver.navigate().refresh(); // used to reset values and validation state for next test
    }

    /**
     * Verify canceling confirm dialog will go back to add comms dialog. Verify canceling will not create stack.
     */
    @Test
    public void testConfirmStackCancel() throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        String physName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);

        // Bring up the add comms dialog.
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        addCommsButton.click();

        // Wait til the add comms dialog is visible.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='addAccordion']")));

        // select link layer and wait until the dialog reflects that the link type has been selected
        WebElement layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Link Layer",
                "example.ccomms.ExampleLinkLayer");
        assertThat(layer, is(notNullValue()));
        layer.click();
        wait.until(ExpectedConditions.textToBePresentInElementValue(By.cssSelector("input[name$='linkData_selection']"),
                "example.ccomms.ExampleLinkLayer"));

        // enter the layer name
        WebElement input = m_Driver.findElement(By.cssSelector("input[id$='linkName']"));
        GeneralHelper.retrySendKeys(m_Driver, input, "testLink", 5);

        // select physical link and wait until the dialog reflects that the physical link has been selected
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Physical Link", physName);
        assertThat(layer, is(notNullValue()));
        layer.click();
        wait.until(ExpectedConditions.textToBePresentInElementValue(
                By.cssSelector("input[name$='physicalData_selection']"), physName));

        // click confirm stack
        WebElement confirmStackButton = m_Driver.findElement(By.cssSelector("button[id$='confirmStackButton']"));
        confirmStackButton.click();

        // wait until the confirm stack table is shown and then click cancel button
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("span[id$='confirmStack']")));
        confirmStackButton = m_Driver.findElement(By.cssSelector("button[id$='confirmCancelButton']"));
        confirmStackButton.click();

        WebElement addStack = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='addAccordion']")));
        assertThat(addStack, is(notNullValue())); // ensure that add stack dialog was found

        // ---close the dialog---
        WebElement closeButton = m_Driver.findElement(By.cssSelector("button[id*='cancelStackButton']"));
        closeButton.click();

        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div[id$='addCommsDialog']")));
        assertThat(CommsGeneralHelper.retrieveStack(m_Driver, "testLink"), is(nullValue()));
    }

    /**
     * Verify physical link can be force added and appears in the list of comms layers.
     */
    @Test
    public void testForceAddPhysicalLink() throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS, null);
        CommsAddDialogHelper.forceAddPhysicalLink(m_Driver, "testPlink");
    }

    /**
     * Verify types of usable layers are successfully refreshed after removal of a bundle.
     */
    @Test
    public void testRefreshTypes() throws InterruptedException
    {
        // uninstall ccomm bundle and open up add comms dialog
        BundleManagerUtil.uninstallBundle(m_Driver, "example.ccomm.main",
                NavigationButtonNameConstants.NAVBUT_PROP_COMMS);
        CommsGeneralHelper.setBundleInstalled(false);
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        addCommsButton.click();

        // verify that ccomm layer types still exist even after bundle uninstalled
        WebElement layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Link Layer",
                "example.ccomms.ExampleLinkLayer");
        assertThat(layer, is(notNullValue()));
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.EchoTransport");

        // If there are more then 3 in the list, select next page, then check again
        if (layer == null)
        {
            CommsAddDialogHelper.selectNextCommsLayerPage(m_Driver);
            layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.EchoTransport");
        }

        assertThat(layer, is(notNullValue()));
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.DirectTransport");

        // If there are more then 3 in the list, select next page, then check again
        if (layer == null)
        {
            CommsAddDialogHelper.selectNextCommsLayerPage(m_Driver);
            layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                    "example.ccomms.DirectTransport");
            if(layer == null)
            {
                CommsAddDialogHelper.selectBackCommsLayerPage(m_Driver);
                layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                        "example.ccomms.DirectTransport");
            }
        }
        assertThat(layer, is(notNullValue()));

        // refresh the layer types
        WebElement refreshTypesButton = m_Driver.findElement(By.cssSelector("button[id*='refreshCommsTypes']"));
        refreshTypesButton.click();

        wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("tbody[id$='transportData_data']>tr>td>div")));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("tbody[id$='linkData_data']>tr>td>div")));

        Wait<WebDriver> fwait = new FluentWait<WebDriver>(m_Driver).withTimeout(30, TimeUnit.SECONDS).pollingEvery(1,
                TimeUnit.SECONDS).ignoring(StaleElementReferenceException.class, NoSuchElementException.class);

        // assert that old transport layer types do not exist since example ccomm bundle was deleted
        // Use fluent wait to check first one since the refresh types push update might not be finished.
        fwait.until(new ExpectedCondition<Boolean>()
        {
            @Override
            public Boolean apply(WebDriver driver)
            {
                WebElement layer = null;
                try
                {
                    layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type",
                            "example.ccomms.EchoTransport");
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                assertThat(layer, is(nullValue()));
                return true;
            }
        });

        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Transport Type", "example.ccomms.DirectTransport");
        assertThat(layer, is(nullValue()));

        // open link layer tab and assert that old link layer types do not exist since example ccomm bundle was deleted
        layer = CommsAddDialogHelper.layerSelectHelper(m_Driver, "Link Layer", "example.ccomms.ExampleLinkLayer");
        assertThat(layer, is(nullValue()));

        // re-install bundle
        BundleManagerUtil.installBundle(m_Driver, ResourceLocatorUtil.getExampleCustomCommPluginFile(),
                NavigationButtonNameConstants.NAVBUT_PROP_COMMS);
        CommsGeneralHelper.setBundleInstalled(true);

        // open up add comms dialog and refresh the types again so they are available for the next test
        addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='addCommsButton']"));
        wait.until(ExpectedConditions.visibilityOf(addCommsButton));
        addCommsButton.click();

        refreshTypesButton = m_Driver.findElement(By.cssSelector("button[id*='refreshCommsTypes']"));
        wait.until(ExpectedConditions.visibilityOf(refreshTypesButton));
        refreshTypesButton.click();

        GeneralHelper.sleepWithText(5, "Waiting for AJAX update of the dialog as it has been refreshed");

        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[id*='cancelStackButton']")));

        GeneralHelper.safeClickBySelector(By.cssSelector("button[id*='cancelStackButton']"));
    }
}
