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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import mil.dod.th.ose.gui.integration.helpers.ChannelsHelper;
import mil.dod.th.ose.gui.integration.helpers.ControllerHelper;
import mil.dod.th.ose.gui.integration.helpers.GeneralHelper;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Class verifies the functionality of the channels page.
 * @author nickmarcucci
 *
 */
public class TestChannelSetup
{
    private static WebDriver m_Driver;
    
    @BeforeClass
    public static void beforeClass()
    {
        m_Driver = WebDriverFactory.retrieveWebDriver();
        NavigationHelper.openBaseUrl();
    }
    
    @AfterClass
    public static void afterClass() throws InterruptedException, ExecutionException, TimeoutException
    {
        //Insures that the default controller has been added back and that all other controllers have been removed.
        ControllerHelper.cleanupControllerCheck(m_Driver);
    }
    
    /**
     * Verify that a socket channel exists and that it can be removed. This test assume that a socket is already there
     * when the test starts. It is safe to assume this because a socket is added before integration tests begin running
     * and any integration test that removes a socket should at add the socket back before the end of the test.
     */
    @Test
    public void testRemoveSocketChannel() throws InterruptedException, ExecutionException, TimeoutException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        //collapse side bars for next test
        NavigationHelper.collapseRightSideBarOnly(m_Driver);
        
        //Verify the channel exists and has the correct information.
        List<WebElement> channelsList = ChannelsHelper.retrieveChannelsList(m_Driver, 
                ChannelsHelper.SOCKET_TABLE_SELECTOR);
        assertThat(channelsList.size(), is(1));
        WebElement channel = channelsList.get(0);
        List<WebElement> channelInfo = channel.findElements(By.cssSelector("div[class='ui-dt-c']"));
        WebElement hexId = channelInfo.get(0).findElement(By.cssSelector("span[id*='hexId']"));
        assertThat(hexId.getText(), is("0x0000007D"));
        WebElement hostName = channelInfo.get(1);
        assertThat(hostName.getText(), is(ChannelsHelper.HOST_NAME));
        WebElement hostPort = channelInfo.get(2);
        assertThat(hostPort.getText(), is(ChannelsHelper.DEFAULT_REMOTE_CONNECTION_PORT));
        
        //Remove the channel.
        ChannelsHelper.removeSocketChannel(m_Driver, "localhost", "4000", true);
        
        //get table again. confirm that there is only two tr now (th and no records found row)
        WebElement socketTable = m_Driver.findElement(By.xpath("//div[contains(@id, 'tcps')]"));
        
        List<WebElement> rowsAfter = socketTable.findElements(By.tagName("tr"));
        
        int size = rowsAfter.size();
        assertThat(size, is(2));
        
        WebElement noRecords = rowsAfter.get(1);
        
        WebElement div = noRecords.findElement(By.tagName("div"));
        assertThat(div, is(notNullValue()));
        assertThat(div.getText(), is("No records found."));
        
        //make sure that controller side bar is empty by verifying that no table elements can be found.
        WebElement controllerList = m_Driver.findElement(By.cssSelector("div[id*='controllerList_content']"));
        assertThat(controllerList, is(notNullValue()));
        
        List<WebElement> elements = controllerList.findElements(By.cssSelector("table[class='controller']"));
        
        assertThat(elements.size(), is(0));
    }
    
    /**
     * Verify socket image is correct when choosing the channel type in the add channel dialog.
     */
    @Test
    public void testCreateChannelImage() throws InterruptedException
    {
        //navigate to the channels page
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);

        //collapse sidebars
        NavigationHelper.collapseSideBars(m_Driver);
        
        WebDriverWait wait = new WebDriverWait(m_Driver, 5);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='addChannel']")));
        WebElement addChannel = m_Driver.findElement(By.cssSelector("button[id*='addChannel']"));
        assertThat(addChannel, is(notNullValue()));
        addChannel.click();
        
        // wait for dialog to show
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("propsDlg")));
        
        // need to change channel type
        final WebElement changeChannelType = m_Driver.findElement(By.cssSelector("button[id*='changeChannelType']"));
        changeChannelType.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[id*='chooseChannelTable']")));
        final WebElement newChannelTable = m_Driver.findElement(By.cssSelector("tbody[id$='chooseChannelTable_data']"));
        
        WebElement channelImg = newChannelTable.findElement(By.tagName("img"));
        assertThat(channelImg.isDisplayed(), is(true));
        
        String source = channelImg.getAttribute("src");
        String img = ImageHelper.getPictureOrIconName(source, ".png");
        
        assertThat(img, is("socket.png"));
        
        //close the dialog
        WebElement closeButton = m_Driver.findElement(By.cssSelector("span[class='ui-icon ui-icon-closethick']"));
        closeButton.click();
    }
    
    /**
     * Verify that a socket channel can be created.
     */
    @Test
    public void testCreateSocketChannel() throws InterruptedException, ExecutionException, TimeoutException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        
        //Remove the channel if existent
        List<WebElement> channels = ChannelsHelper.retrieveChannelsList(m_Driver, ChannelsHelper.SOCKET_TABLE_SELECTOR);
        if (channels.size() >= 1)
        {
            for (WebElement element: channels)
            {
                List<WebElement> channelInfo = element.findElements(By.cssSelector("div[class='ui-dt-c']"));
                //occasionally due to the speed and order of tests the driver will say there are elements, but
                //there aren't, so verify that the element has data, ie, an actual channel, before checking fields 
                if (channelInfo.size() > 2 && channelInfo.get(1).getText().equals(ChannelsHelper.HOST_NAME))
                {
                    ChannelsHelper.removeSocketChannel(m_Driver, "localhost", "4000", false);
                }
            }
        }
        
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        ChannelsHelper.createSocketChannel(m_Driver);
        
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        
        // wait for the controller to respond
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException exception)
        {
            exception.printStackTrace();
        }
        
        //check that the controller side bar has the controller now
        WebElement newController = ControllerHelper.getControllerListElement(
            m_Driver, ControllerHelper.DEFAULT_CONTROLLER_NAME);
        assertThat("Controller with name " + ControllerHelper.DEFAULT_CONTROLLER_NAME + " could not be found.", 
                newController, is(notNullValue()));
        assertThat(newController.getText(), containsString(ControllerHelper.getControllerName()));
    }
    
    /**
     * Verify that localhost can be connected to using the controller history quick connect button.
     */
    @Test
    public void testControllerHistoryConnectButton() throws InterruptedException, ExecutionException, TimeoutException
    {
        NavigationHelper.navigateToPage(m_Driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS);
        
        //Remove the channel if existent
        List<WebElement> channels = ChannelsHelper.retrieveChannelsList(m_Driver, ChannelsHelper.SOCKET_TABLE_SELECTOR);
        if (channels.size() >= 1)
        {
            for (WebElement element: channels)
            {
                List<WebElement> channelInfo = element.findElements(By.cssSelector("div[class='ui-dt-c']"));
                //occasionally due to the speed and order of tests the driver will say there are elements, but
                //there aren't, so verify that the element has data, ie, an actual channel, before checking fields 
                if (channelInfo.size() > 2 && channelInfo.get(1).getText().equals(ChannelsHelper.HOST_NAME))
                {
                    ChannelsHelper.removeSocketChannel(m_Driver, "localhost", "4000", false);
                }
            }
        }
        
        final By quickConnectBtnSelector = By.cssSelector("button[id$='quickConnectBtn']");
        NavigationHelper.expandRightSideBarOnly(m_Driver);
        GeneralHelper.safeClickBySelector(quickConnectBtnSelector);
        
        // wait for the controller to respond
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException exception)
        {
            exception.printStackTrace();
        }
        
        //check that the controller side bar has the controller now
        WebElement newController = ControllerHelper.getControllerListElement(
            m_Driver, ControllerHelper.DEFAULT_CONTROLLER_NAME);
        assertThat("Controller with name " + ControllerHelper.DEFAULT_CONTROLLER_NAME + " could not be found.", 
                newController, is(notNullValue()));
        assertThat(newController.getText(), containsString(ControllerHelper.getControllerName()));
    }
}
