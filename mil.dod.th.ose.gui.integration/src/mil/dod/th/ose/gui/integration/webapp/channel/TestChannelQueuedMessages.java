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
package mil.dod.th.ose.gui.integration.webapp.channel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.core.controller.TerraHarvestController.OperationMode;
import mil.dod.th.ose.gui.integration.helpers.BundleManagerUtil;
import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.CommsAddDialogHelper;
import mil.dod.th.ose.gui.integration.helpers.CommsGeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.GrowlVerifier;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Class verifies the functionality of the queued messages column on the channels setup page.
 * 
 * @author cweisenborn
 */
public class TestChannelQueuedMessages
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void setup()
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    @AfterClass
    public static void cleanup() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Insures that the default controller has been added back and that all other controllers have been removed.
        ControllerHelper.cleanupControllerCheck(m_Driver);
    }
    
    /**
     * Test the queued messages feature of the channels page.
     * Verify that the queue message count increments and can be cleared.
     */
    @Test
    public void testQueuedMessages() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Navigate to the channels page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        
        //Create a socket channel connecting to the gui.
        ChannelsHelper.createSocketChannel(m_Driver, ChannelsHelper.HOST_NAME, 
                ChannelsHelper.DEFAULT_GUI_CONNECTION_PORT);
        
        //Set the GUI as the active controller.
        ControllerHelper.setActiveControllerByName(m_Driver, ControllerHelper.DEFAULT_GUI_NAME);
        
        //Navigate to the system configuration page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_SYS_CONFIG);
        
        GeneralHelper.sleepWithText(5, "Waiting for AJAX to update with list of all web GUI bundles");
        
        BundleManagerUtil.startBundle(m_Driver, "example.ccomm.main");
        BundleManagerUtil.startBundle(m_Driver, "example.ccomm.serial");
        
        //Navigate to comms page to create a comms stack.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS);
        
        CommsAddDialogHelper.forceAddPhysicalLink(m_Driver, "forcedPlink");
        
        //Create the comms stack needed to create a transport channel.
        CommsGeneralHelper.createStack(m_Driver, "forcedPlink", "testLink", "example.ccomms.ExampleLinkLayer", 
                "testTrans", "example.ccomms.DirectTransport");
        
        //Create the transport channel.
        ChannelsHelper.createTransportChannel(m_Driver, "testTrans", "0x00000001", "prefix:local", "prefix:remote");
        
        //Remove the controller socket channel.
        ChannelsHelper.removeSocketChannel(m_Driver, "0x0000007D", 
                ControllerHelper.DEFAULT_CONTROLLER_NAME, ChannelsHelper.HOST_NAME, "4000", false);
        //Remove the gui socket channel.
        ChannelsHelper.removeSocketChannel(m_Driver, "0x00000001", ControllerHelper.DEFAULT_GUI_NAME, 
                ChannelsHelper.HOST_NAME, "4001", false);
        
        //Expand the right side bar so that the test can access the controller mode.
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        
        //Change the mode 5 times to queue up messages.
        for (int i = 0; i < 5; i++)
        {
            //Bring up the controller mode selection dialog.
            ControllerHelper.changeControllerOpMode(m_Driver, ControllerHelper.DEFAULT_GUI_NAME, 
                OperationMode.OPERATIONAL_MODE, false);
            
            //Thread sleep needed since loops tend to move too quickly in selenium.
            Thread.sleep(1000);
        }
        
        //Refresh channels page by navigating to it. Needed to updated queued message count.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        
        //Check the queued message count and assert that it is greater than zero.
        WebElement transChannelTable = m_Driver.findElement(By.cssSelector("tbody[id$='transTable_data']"));
        WebElement transChannel = transChannelTable.findElements(By.tagName("tr")).get(0);
        String queuedMsgCountStr = transChannel.findElements(By.tagName("td")).get(5).getText().split("\\r?\\n")[0];
        assertThat(Integer.parseInt(queuedMsgCountStr), greaterThan(0));
        
        //Clear the queued messages.
        final WebElement clearQueuedMessages = m_Driver.findElement(
                By.cssSelector("button[id$='clearTransportQueue']"));
        GrowlVerifier.verifyAndWaitToDisappear(20, clearQueuedMessages, "Channel Queue Cleared");
        
        //Refresh channels page by navigating to it. Needed to updated queued message count.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        NavigationHelper.collapseRightSideBarOnly(m_Driver);
        
        //Check queued message count after clearing. Should be 0.
        transChannelTable = m_Driver.findElement(By.cssSelector("tbody[id$='transTable_data']"));
        transChannel = transChannelTable.findElements(By.tagName("tr")).get(0);
        queuedMsgCountStr = transChannel.findElements(By.tagName("td")).get(5).getText().split("\\r?\\n")[0];
        assertThat(Integer.parseInt(queuedMsgCountStr), is(0));
        
        //Remove the transport channel.
        ChannelsHelper.removeTransportChannel(m_Driver, "0x00000001", ControllerHelper.DEFAULT_GUI_NAME, 
                "testTrans", "prefix:local", "prefix:remote", true);
        
        //Add back the socket channel for the gui.
        ChannelsHelper.createSocketChannel(m_Driver, ChannelsHelper.HOST_NAME, "4001");
        
        //Navigate to the comms page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_COMMS);
        
        //Remove the comms stack needed for the transport channel.
        CommsGeneralHelper.removeStack(m_Driver, "testTrans", "Layer Deleted");
        
        //Current issue with comms layer and stacks not being complete will cause additional growl messages to appear.
        //Sleep needed to allow growl messages to full disappear.
        Thread.sleep(7000);
        
        //Navigate to the channels page.
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        
        //Remove the gui socket channel.
        ChannelsHelper.removeSocketChannel(m_Driver, "0x00000001", ControllerHelper.DEFAULT_GUI_NAME, 
                ChannelsHelper.HOST_NAME, "4001", true);
        
        //Add back the socket channel for the controller.
        ChannelsHelper.createSocketChannel(m_Driver, ChannelsHelper.HOST_NAME, "4000");
    }
}
