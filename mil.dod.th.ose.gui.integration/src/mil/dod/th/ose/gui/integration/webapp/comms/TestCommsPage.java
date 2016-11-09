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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.CommsGeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selenium test for the comms page. Test creating comms stacks, and various operations on comms stacks that can be
 * performed through the GUI.
 * 
 * @author bachmakm
 */
public class TestCommsPage
{
    private static WebDriver m_Driver;
    private static WebDriverWait wait;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        wait = new WebDriverWait(m_Driver, 10);        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id$='channelsButton']")));
        
        CommsGeneralHelper.commsTestSetupHelper(m_Driver);
    }
    
    @After
    public void cleanUp() throws InterruptedException, ExecutionException, TimeoutException
    {
        CommsGeneralHelper.commsTestCleanUpHelper(m_Driver);
    }
    
    /**
     * Verify images on the comms tab of the assets page are correct.
     */
    @Test
    public void testCommsImages() throws InterruptedException, ExecutionException, TimeoutException
    {
        WebElement commsStack1 = CommsGeneralHelper.retrieveStack(m_Driver, "/dev/serialExample1");
        assertThat(commsStack1, notNullValue());
        WebElement commImage1 = commsStack1.findElement(By.tagName("img"));
        
        ImageHelper.getPictureOrIconNameAndVerify(commImage1, "comms_serial.png", ".png");
        
        WebElement commsStack2 = CommsGeneralHelper.retrieveStack(m_Driver, "/dev/serialExample2");
        assertThat(commsStack2, notNullValue());
        WebElement commImage2 = commsStack2.findElement(By.tagName("img"));
        ImageHelper.getPictureOrIconNameAndVerify(commImage2, "comms_serial.png", ".png");
       
        String physName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);
        
        CommsGeneralHelper.createStack(m_Driver,  physName, "linkyLink", 
                "example.ccomms.ExampleLinkLayer", null, null);
        
        WebElement linkyLinkTable = CommsGeneralHelper.retrieveStack(m_Driver, "linkyLink");
        assertThat(linkyLinkTable, notNullValue());
        WebElement linkyImage = linkyLinkTable.findElement(By.tagName("img"));
        
        ImageHelper.getPictureOrIconNameAndVerify(linkyImage, "comms_line_of_sight.png", ".png");
       
        CommsGeneralHelper.removeStack(m_Driver, "linkyLink");
        
        String transportPhyName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);
        
        CommsGeneralHelper.createStack(m_Driver, transportPhyName, "linkylink", 
                "example.ccomms.ExampleLinkLayer", "transport1", "example.ccomms.DirectTransport");
        
        WebElement transportTable = CommsGeneralHelper.retrieveStack(m_Driver, "transport1");
        WebElement transportImage = transportTable.findElement(By.tagName("img"));
        
        ImageHelper.getPictureOrIconNameAndVerify(transportImage, "comms_line_of_sight.png", ".png");
        
        CommsGeneralHelper.removeStack(m_Driver, "transport1");
    }

    /**
     * Verify the stack's BIT can be run and the status updates appropriately.
     */
    @Test
    public void testStackBITStatusChanged() throws InterruptedException, ExecutionException, TimeoutException
    {        
        String physName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);
        
        CommsGeneralHelper.createStack(m_Driver,  physName, "linkyLink", 
                "example.ccomms.ExampleLinkLayer", null, null);
        
        //---verify status is lost---
        WebElement commsStack = CommsGeneralHelper.retrieveStack(m_Driver, "linkyLink");
        assertThat(commsStack, notNullValue());
        
        WebElement commStatus = commsStack.findElement(By.cssSelector("span[id$='commStatusText']"));
        assertThat(commStatus.getText(), is("Status: LOST"));
        
        //---perform BIT---
        WebElement performBitButton = commsStack.findElement(By.cssSelector("button[id*='bitButton']"));
        
        GrowlVerifier.verifyAndWaitToDisappear(20, performBitButton, "Link Status Changed");
        
        //---verify status changed to GOOD---
        commsStack = null;
        commsStack = CommsGeneralHelper.retrieveStack(m_Driver, "linkyLink");
        assertThat(commsStack, notNullValue());
        WebElement isStatusOk = commsStack.findElement(By.cssSelector("span[id$='commStatusText']"));
        
        assertThat(isStatusOk.getText(), is("Status: OK"));
        
        CommsGeneralHelper.removeStack(m_Driver, "linkyLink");
    }
    
    /**
     * Verify the stack can be activated and deactivated.
     */
    @Test
    public void testActivateDeactivateStack() throws InterruptedException, ExecutionException, TimeoutException
    {        
        String physName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);
        
        CommsGeneralHelper.createStack(m_Driver, physName, "apricots",
                "example.ccomms.ExampleLinkLayer", null, null);
        
        CommsGeneralHelper.activateStack(m_Driver, "apricots");
        
        CommsGeneralHelper.deactivateStack(m_Driver, "apricots");
        
        CommsGeneralHelper.removeStack(m_Driver, "apricots");
    }
    
    /**
     * Verify stacks containing only a link layer can be deleted.
     * Verify stacks containing both link layer and transport layer can be deleted.
     * Verify removing a stack does not remove the physical link.
     * Verify we can cancel removing a stack.
     */
    @Test
    public void testRemoveStack() throws InterruptedException, ExecutionException, TimeoutException
    {        
        String physName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);

        //create stack containing transport
        CommsGeneralHelper.createStack(m_Driver, physName, "beer", 
                "example.ccomms.ExampleLinkLayer", "kibble", "example.ccomms.DirectTransport");

        //remove stack
        m_Driver.findElement(By.cssSelector("button[id*='removeButton']")).click();
        
        //wait until cancel button from remove confirmation dialog is visible
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id$='deleteCancelButton']")));
        
        //cancel removing the stack and verify the stack is still present
        WebElement cancelRemove = m_Driver.findElement(By.cssSelector("button[id$='deleteCancelButton']"));
        cancelRemove.click();
        
        //verify stack is still present
        WebElement stackTable = CommsGeneralHelper.retrieveStack(m_Driver, "kibble");
        assertThat(stackTable, is(notNullValue()));
        
        //verify that both layers are removed from stack - assert is contained in helper method
        CommsGeneralHelper.removeStack(m_Driver, "kibble");    
        
        //verify physical link still exists
        stackTable = CommsGeneralHelper.retrieveStack(m_Driver, physName);
        assertThat(stackTable, is(notNullValue()));
    }
}
