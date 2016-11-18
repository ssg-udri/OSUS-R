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

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import mil.dod.th.ose.gui.integration.helpers.CommsGeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.ConfigurationHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

/**
 * Class for testing the successful operation of different functionalities associated with the 
 * comms config dialog.  
 * 
 * @author ncalderon
 */
public class TestCommsConfig 
{

    private static WebDriver m_Driver;
    private static WebDriverWait m_Wait;
    
    @BeforeClass
    public static void setup() throws InterruptedException
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
        
        m_Wait = new WebDriverWait(m_Driver, 30);        
        m_Wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='channelsButton']")));
        
        CommsGeneralHelper.commsTestSetupHelper(m_Driver);
        createCommsStack();
    }
    
    @After
    public void cleanUp() throws InterruptedException, ExecutionException, TimeoutException
    {
        CommsGeneralHelper.commsTestCleanUpHelper(m_Driver);
    }
    
    private static void createCommsStack() throws InterruptedException
    {
        String transportPhyName = CommsGeneralHelper.getAvailablePhysicalLink(m_Driver);
        
        CommsGeneralHelper.createStack(m_Driver, transportPhyName, "iousD", 
                "example.ccomms.ExampleLinkLayer", "Tenac", "example.ccomms.DirectTransport");
    }
    
    @Test
    public void testPhysicalLayerConfig()
    {
        //Bring up the add comms dialog
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='physicalConfigButton']"));
        addCommsButton.click();
        
        //Wait until the add comms dialog is visible
        m_Wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[id$='commsEditConfiguration']")));
        
        WebElement textDataBiteInput = m_Driver.findElement(
                By.cssSelector("input[id*='compositeConfigTable:0:compositeConfigInput']"));
        assertThat(textDataBiteInput, is(notNullValue()));
        assertThat(textDataBiteInput.getText(), is(""));
        
        //---close the configuration---
        WebElement closeButton = m_Driver.findElement(By.cssSelector("button[id*='cancelConfig']"));
        closeButton.click();
        
        m_Wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div[id$='commsEditConfiguration']")));
    }
    
    @Test
    public void testLinkLayerConfig()
    {
        //Bring up the comms config dialog
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='linkConfigButton']"));
        addCommsButton.click();
        
        //Wait until the comms config dialog is visible
        m_Wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='commsEditConfiguration']")));
      
        //Test for text
        WebElement configDialog = m_Driver.findElement(By.id("commsEditConfiguration"));
        WebElement configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "Read timeout ms"),
                is("0"));        
        
        //Changing the text in the input box
        ConfigurationHelper.updateValue(configTable, "Read timeout ms", "1");
        
        //Saving the configuration
        WebElement saveConfigButton = m_Driver.findElement(By.cssSelector("button[id*='saveConfig']"));
        saveConfigButton.click();
        
        //Wait until the comms config is invisible
        m_Wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div[id$='commsEditConfiguration']")));
        
        //Bring up the comms config dialog
        addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='linkConfigButton']"));
        addCommsButton.click();
        
        //Wait until the comms config dialog is visible
        m_Wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='commsEditConfiguration']")));
        
        //Test for text
        WebElement newLayerConfigDialog = m_Driver.findElement(By.id("commsEditConfiguration"));
        WebElement newLayerConfigTable = newLayerConfigDialog.findElement(
                By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        assertThat(ConfigurationHelper.getInputBoxValue(newLayerConfigTable, "Read timeout ms"),
                is("1")); 
        
        //---close the configuration---
        WebElement closeButton = m_Driver.findElement(By.cssSelector("button[id$='cancelConfig']"));
        closeButton.click();
        
        m_Wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div[id$='commsEditConfiguration']")));
    }
    
    @Test
    public void testTransportLayerConfig()
    {
        //Bring up the add comms dialog
        WebElement addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='transportConfigButton']"));
        addCommsButton.click();
        
        //Wait until the add comms dialog is visible
        m_Wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='commsEditConfiguration']")));
        
        WebElement configDialog = m_Driver.findElement(By.id("commsEditConfiguration"));
        WebElement configTable = configDialog.findElement(By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        assertThat(ConfigurationHelper.getInputBoxValue(configTable, "Read timeout ms"),
                is("0"));        
        
        //Changing the text in the input box
        ConfigurationHelper.updateValue(configTable, "Read timeout ms", "1");
        
        //Saving the configuration
        WebElement saveConfigButton = m_Driver.findElement(By.cssSelector("button[id*='saveConfig']"));
        saveConfigButton.click();
        
        //Wait until the comms config is invisible
        m_Wait.until(ExpectedConditions.invisibilityOfElementLocated(
                 By.cssSelector("div[id$='commsEditConfiguration']")));
        
        //Bring up the comms config dialog
        addCommsButton = m_Driver.findElement(By.cssSelector("button[id*='transportConfigButton']"));
        addCommsButton.click();
        
        //Wait until the comms config dialog is visible
        m_Wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div[id$='commsEditConfiguration']")));
      
        //Test for text        
        WebElement newTransportConfigDialog = m_Driver.findElement(By.id("commsEditConfiguration"));
        WebElement newTransportConfigTable = newTransportConfigDialog.findElement(
                By.cssSelector("tbody[id$='compositeConfigTable_data']"));
        
        assertThat(ConfigurationHelper.getInputBoxValue(newTransportConfigTable, "Read timeout ms"),
                is("1")); 
        
        //---close the configuration---
        WebElement closeButton = m_Driver.findElement(By.cssSelector("button[id*='cancelConfig']"));
        closeButton.click();
        
        m_Wait.until(ExpectedConditions.invisibilityOfElementLocated(
                 By.cssSelector("div[id$='commsEditConfiguration']")));
        
        //---deleting the comms stack---
        WebElement removeCommsStack = m_Driver.findElement(
                By.cssSelector("button[id$='commsTable:0:removeButton']"));
        removeCommsStack.click();
        
        m_Wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("span[id$='removeDlgPanel']")));
        
        //---confirming the removal---
        WebElement confirmRemoveCommsStack = m_Driver.findElement(
                By.cssSelector("button[id$='confirmDeleteButton']"));
        confirmRemoveCommsStack.click();

    }
  

}
