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
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.base.Function;

/**
 * Class provides functionality to perform actions specific to the 
 * channels page.
 * @author nickmarcucci
 */
public class ChannelsHelper
{
    /**
     * Host for connecting to controller.
     */
    public static final String HOST_NAME = "localhost";

    /**
     * String representation of the default port for connecting to a controller.
     * *This is a string because it is used in the add channel input boxes.*
     */
    public static final String DEFAULT_REMOTE_CONNECTION_PORT = "4000";
    
    /**
     * String representation of the default port for connecting to the gui.
     */
    public static final String DEFAULT_GUI_CONNECTION_PORT = "4001";

    /**
     * String representation of the SSL button when disabled.
     */
    public static final String DISABLE_SSL = "No";

    /**
     * String representation of the SSL button when enabled.
     */
    public static final String ENABLE_SSL = "Yes";

    /**
     * CSS selector that represents the socket channel table on the channels page.
     */
    public static final By SOCKET_TABLE_SELECTOR = By.cssSelector("tbody[id$='tcps_data']");
    
    /**
     * CSS selector that represents the transport channel table on the channels page.
     */
    public static final By TRANSPORT_TABLE_SELECTOR = By.cssSelector("tbody[id$='transTable_data']");
    
    /**
     * Creates a socket channel for the controller with the id and name specified with 
     * the specified host name and port and SSL disabled. This function assumes currently
     * on channels page.
     * @param driver
     *  the web driver performing the tests
     * @param host
     *  the host name of the socket to be created
     * @param port
     *  the port of the socket to be created 
     */
    public static void createSocketChannel(final WebDriver driver, final String host, final String port) 
            throws InterruptedException, ExecutionException, TimeoutException
    {
        //create channel
        createSocketChannel(driver, host, port, DISABLE_SSL);
    }

    /**
     * Creates a socket channel for the controller with the id and name specified with 
     * the specified host name, port and SSL state. This function assumes currently on
     * channels page.
     * @param driver
     *  the web driver performing the tests
     * @param host
     *  the host name of the socket to be created
     * @param port
     *  the port of the socket to be created 
     * @param ssl
     *  Yes or No to indicate whether an SSL socket should be used
     */
    public static void createSocketChannel(final WebDriver driver, final String host, final String port,
            final String ssl) throws InterruptedException, ExecutionException, TimeoutException
    {
        //navigate to the channels page if not already there.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS, null);

        //collapse sidebars
        NavigationHelper.collapseSideBars(driver);
        
        final WebDriverWait wait = new WebDriverWait(driver, 5);
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='addChannel']")));
        final WebElement addChannel = driver.findElement(By.cssSelector("button[id*='addChannel']"));
        assertThat(addChannel, is(notNullValue()));
        addChannel.click();
        
        //now we are on the enter page
        WebElement propsOutputPanel = driver.findElement(By.id("propsDlg"));
        
        WebElement hostName = propsOutputPanel.findElement(By.xpath("//input[contains(@id, 'hostName')]"));
        WebElement hostPort = propsOutputPanel.findElement(By.xpath("//input[contains(@id, 'hostPort')]"));
        WebElement enableSsl = propsOutputPanel.findElement(By.cssSelector("div[id$='enableSsl']"));
        WebElement createButton = propsOutputPanel.findElement(By.xpath("//button[contains(@id, 'createChanButton')]"));
        
        assertThat(hostName, is(notNullValue()));
        assertThat(hostPort, is(notNullValue()));
        assertThat(enableSsl, is(notNullValue()));
        assertThat(createButton, is(notNullValue()));
        
        //change host if not default
        if (!hostName.equals("localhost"))
        {
            hostName.clear();
            hostName.sendKeys(host);
        }
        
        //change port if not default
        if (!hostPort.equals("4000"))
        {
            hostPort.clear();
            hostPort.sendKeys(port);
        }
        
        //change SSL if not equal to desired state
        if (!enableSsl.findElement(By.cssSelector("span")).getText().equals(ssl))
        {
            //toggle SSL button
            enableSsl.click();
        }

        //Create channel and verify controller added growl message is displayed.
        GrowlVerifier.verifyAndWaitToDisappear(25, createButton, "Controller Info:");
        
        final Wait<WebDriver> fwait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS).
                pollingEvery(5, TimeUnit.SECONDS).ignoring(NoSuchElementException.class, 
                        StaleElementReferenceException.class);
        
        //confirm that socket with specified ID and port is there
        final Boolean foundSocket = fwait.until(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(final WebDriver driver)
            {
                final WebElement socketTable = driver.findElement(SOCKET_TABLE_SELECTOR);
                for (final WebElement element : socketTable.findElements(By.tagName("tr")))
                {
                    List<WebElement> rowCells = element.findElements(By.tagName("td"));
                    WebElement hostDiv = rowCells.get(1).findElement(By.tagName("div"));
                    WebElement portDiv = rowCells.get(2).findElement(By.tagName("div"));
                    WebElement enableSslDiv = rowCells.get(3).findElement(By.tagName("div"));
                    
                    //confirm that socket with specified ID and port is there
                    if (hostDiv.getText().equals(host) && portDiv.getText().equals(port)
                            && enableSslDiv.getText().equals(Boolean.toString(ssl.equals(ENABLE_SSL))))
                    {
                        return true;
                    }
                }
                return false;
            }
        });
        
        assertThat(foundSocket, notNullValue());
        assertThat(foundSocket, is(true));
        
        //return to initial state
        NavigationHelper.expandLeftSideBarOnly(driver);
    }
    
    /**
     * Creates a socket channel to a controller using default values. This method
     * will navigate to the channels page and add the needed channel.
     * @param driver
     *  the web driver performing the tests
     */
    public static void createSocketChannel(final WebDriver driver) throws InterruptedException, ExecutionException, 
        TimeoutException
    {
        //create channel
        createSocketChannel(driver, HOST_NAME, DEFAULT_REMOTE_CONNECTION_PORT);
    }
    
    /**
     * Removes a specified socket based on the controller name and id and the specified host
     * name and port. Assumes currently on the channels page.
     * 
     * Will wait for the channel removed message and optionally the no active controller message.
     * 
     * @param driver
     *  the web driver performing the test
     * @param controllerId
     *  the string hex representation of the controller id
     * @param controllerName
     *  the name of the controller
     * @param host
     *  the host name of the socket to be removed
     * @param port
     *  the port of the socket to be removed
     * @param checkNoActiveControllerMsg
     *  whether to check for the No active controller growl message in addition to the channel remove message.
     */
    public static void removeSocketChannel(final WebDriver driver, final String controllerId, 
            final String controllerName, final String host, final String port, 
            final boolean checkNoActiveControllerMsg) throws InterruptedException, ExecutionException, TimeoutException
    {
        //navigate to the channels page if not already there.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS, null);
        
        List<WebElement> socketRows = retrieveChannelsList(driver, SOCKET_TABLE_SELECTOR);
        boolean found = false;
        
        for(WebElement row: socketRows)
        {
            List<WebElement> rowCells = row.findElements(By.tagName("td"));
            
            if (!channelRowContainsProperty(rowCells, controllerName + " - " + controllerId))
            {
                continue;
            }
            if (!channelRowContainsProperty(rowCells, host))
            {
                continue;
            }
            if (!channelRowContainsProperty(rowCells, port))
            {
                continue;
            }
            
            WebElement removeButton = row.findElement(By.cssSelector("button[id*='removeSocketButton']"));
            assertThat(removeButton, is(notNullValue()));
            
            //Remove the controller and verify that channel removed growl message is displayed.
            String[] growlTitles;
            if (checkNoActiveControllerMsg)
            {
                growlTitles = new String[]{"Channel Removed", "No active controller"};
            }
            else
            {
                growlTitles = new String[]{"Channel Removed"};
            }
            GrowlVerifier.verifyAndWaitToDisappear(20, removeButton, growlTitles);
            found = true;
            
            break;
        }
        
        assertThat(String.format("Unable to find [%s - %s], host=[%s] port=[%s]", 
                controllerName, controllerId, host, port), found, is(true));
    }
    
    /**
     * Checks a list of cells for the specified value and returns true if it was found. Otherwise returns false.
     * 
     * @param rowCells
     *          List of {@link WebElement}s that represent the cells of a table row.
     * @param prop
     *          The value to be determined if any of the cells contain.
     * @return
     *          True if the value is found within the row and false otherwise.
     */
    public static boolean channelRowContainsProperty(final List<WebElement> rowCells, final String prop)
    {
        for (WebElement cell: rowCells)
        {
            if (cell.findElement(By.tagName("div")).getText().equals(prop))
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Function to remove a socket channel based on the default controller id and controller name and
     * a specified host name and port. Assumes currently on the channels page.
     * 
     * Will wait for the channel removed message and optionally the no active controller message.
     * 
     * @param driver
     *  the web driver performing the test
     * @param host
     *  the host name of the socket that is to be removed
     * @param port
     *  the port of the socket that is to be removed
     * @param checkNoActiveControllerMsg
     *  whether to check for the No active controller growl message in addition to the channel remove message.
     */
    public static void removeSocketChannel(final WebDriver driver, final String host, final String port,
            final boolean checkNoActiveControllerMsg) 
        throws InterruptedException, ExecutionException, TimeoutException
    {
        removeSocketChannel(driver, ControllerHelper.getControllerId(), 
                ControllerHelper.getControllerName(), host, port, checkNoActiveControllerMsg);
    }
    
    /**
     * Method that creates a transport channel that connects the gui to the specified controller.
     * 
     * @param driver
     *  the web driver performing the test
     * @param transportName
     *  name the type of transport channel to create
     * @param controllerId
     *  id of the controller the transport channel is to connect to
     * @param localAddress
     *  local address for the transport channel
     * @param remoteAddress
     *  remote address for the transport channel
     */
    public static void createTransportChannel(final WebDriver driver, final String transportName, 
        final String controllerId, final String localAddress, final String remoteAddress) 
        throws InterruptedException
    {
        //navigate to the channels page if not already there.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS, null);
        
        //collapse sidebars
        NavigationHelper.collapseSideBars(driver);
        
        // click on add channel button
        final WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("button[id*='addChannel']")));
        final WebElement addChannel = driver.findElement(By.cssSelector("button[id*='addChannel']"));
        assertThat(addChannel, is(notNullValue()));
        addChannel.click();
        
        // wait for dialog to show
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("propsDlg")));
        
        // need to change channel type
        final WebElement changeChannelType = driver.findElement(By.cssSelector("button[id*='changeChannelType']"));
        changeChannelType.click();
        
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("div[id*='chooseChannelTable']")));
        final WebElement newChannelTable = driver.findElement(By.cssSelector("tbody[id$='chooseChannelTable_data']"));
        
        List<WebElement> channelTypeRows = newChannelTable.findElements(By.tagName("tr"));
        for (WebElement channelTypeRow : channelTypeRows)
        {
            List<WebElement> channelTypeCells = channelTypeRow.findElements(By.tagName("td"));
            if (channelTypeCells.get(1).findElement(By.tagName("div")).getText().equals(transportName))
            {
                // found channel type
                final WebElement selectButton = 
                        channelTypeRow.findElement(By.cssSelector("button[id*='selectChannelType']"));
                selectButton.click();
                break;
            }
        }
        
        //Wait till the new transport channel properties dialog is visible.
        final WebElement propertiesDialog = driver.findElement(By.cssSelector("div[id='propsDlg']"));
        wait.until(ExpectedConditions.visibilityOf(propertiesDialog));
        
        //Set the ID of the controller the transport channel will connect to.
        final WebElement controllerIdInput = propertiesDialog.findElement(
                By.cssSelector("div[id$='cIds']")).findElement(By.tagName("input"));
        controllerIdInput.clear();
        controllerIdInput.sendKeys(controllerId);
        
        //Set the local address for the transport channel.
        final WebElement localAddressInput = propertiesDialog.findElement(
                By.cssSelector("div[id$='localAddress']")).findElement(By.tagName("input"));
        localAddressInput.clear();
        localAddressInput.sendKeys(localAddress);
        
        //Set the remote address for the transport channel.
        final WebElement remoteAddressInput = propertiesDialog.findElement(
                By.cssSelector("div[id$='remoteAddress']")).findElement(By.tagName("input"));
        remoteAddressInput.clear();
        remoteAddressInput.sendKeys(remoteAddress);
        
        //Create the transport channel.
        final WebElement createChannelButton = propertiesDialog.findElement(
                By.cssSelector("button[id$='createChanButton']"));
        createChannelButton.click();
        
        //return to initial state
        NavigationHelper.expandLeftSideBarOnly(driver);
    }
    
    /**
     * Method that removes a transport channel.
     * 
     * Will wait for the channel removed message and optionally the no active controller message.
     * 
     * @param driver
     *  the web driver performing the test
     * @param controllerId
     *  the string hex representation of the controller id
     * @param controllerName
     *  the name of the controller
     * @param transportName
     *  the name of the transport layer of transport channel to be removed
     * @param localAdress
     *  the local address of the transport channel to be removed
     * @param remoteAddress
     *  the remote address of the transport channel to be removed
     * @param checkNoActiveControllerMsg
     *  whether to check for the No active controller growl message in addition to the channel remove message.
     */
    public static void removeTransportChannel(final WebDriver driver, final String controllerId, 
            final String controllerName, final String transportName, final String localAdress, 
            final String remoteAddress, final boolean checkNoActiveControllerMsg) throws InterruptedException, 
            ExecutionException, TimeoutException
    {
        //navigate to the channels page if not already there.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS, null);

        List<WebElement> transportRows = retrieveChannelsList(driver, TRANSPORT_TABLE_SELECTOR);
        boolean found = false;
        
        for(WebElement row: transportRows)
        {
            List<WebElement> rowCells = row.findElements(By.tagName("td"));
            
            if (!channelRowContainsProperty(rowCells, controllerName + " - " + controllerId))
            {
                continue;
            }
            if (!channelRowContainsProperty(rowCells, transportName))
            {
                continue;
            }
            if (!channelRowContainsProperty(rowCells, localAdress))
            {
                continue;
            }
            if (!channelRowContainsProperty(rowCells, remoteAddress))
            {
                continue;
            }
            
            WebElement removeButton = row.findElement(By.cssSelector("button[id*='removeTransportButton']"));
            assertThat(removeButton, is(notNullValue()));
            
            //Remove the controller and verify that channel removed growl message is displayed.
            String[] growlTitles;
            if (checkNoActiveControllerMsg)
            {
                growlTitles = new String[]{"Channel Removed", "No active controller"};
            }
            else
            {
                growlTitles = new String[]{"Channel Removed"};
            }
            GrowlVerifier.verifyAndWaitToDisappear(20, removeButton, growlTitles);
            found = true;
            break;
        }
        
        assertThat(found, is(true));
    }
    
    /**
     * Method that retrieves a list of channels for the specified channels table (socket or transport).
     * 
     * @param driver
     *      Current web driver.
     * @param channelTableSelector
     *      Selector that represents the table that contains the list of channels to be retrieved.
     * @return
     *      A list of {@link WebElement} that represent the channels found.
     * @throws InterruptedException
     *      Thrown if there is an error trying to navigate to the channels page.
     */
    public static List<WebElement> retrieveChannelsList(final WebDriver driver, final By channelTableSelector) 
        throws InterruptedException
    {
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_CHANNELS, null);
        
        return driver.findElement(channelTableSelector).findElements(By.tagName("tr"));
    }
}
