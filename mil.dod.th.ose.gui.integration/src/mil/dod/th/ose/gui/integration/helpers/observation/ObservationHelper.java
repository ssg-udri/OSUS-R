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
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mil.dod.th.core.types.MapEntry;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.core.types.observation.RelationshipTypeEnum;
import mil.dod.th.ose.gui.integration.helpers.AssetHelper;
import mil.dod.th.ose.gui.integration.helpers.DebugText;
import mil.dod.th.ose.gui.integration.helpers.ImageHelper;
import mil.dod.th.ose.gui.integration.helpers.NavigationButtonNameConstants;
import mil.dod.th.ose.gui.integration.helpers.NavigationHelper;
import mil.dod.th.ose.gui.integration.helpers.WebDriverFactory;
import mil.dod.th.ose.gui.integration.helpers.observation.ObservationFilterHelper.ExpandCollapse;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.util.Strings;

import com.google.common.base.Preconditions;

/**
 * Observation helper which has methods that can be used to verify observations.
 * 
 * @author nickmarcucci
 *
 */
public class ObservationHelper
{
    /**
     * Returns a found observation panel with the given sensor id.
     * @param sensorId
     *  the sensor id to locate the observation by.
     * @return
     *  the web element that is the observation identified by the sensor id.
     */
    public static WebElement grabObservationPanelBySensorId(String sensorId)
    {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(sensorId));
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        
        ObservationFilterHelper.enterFilterExpression(driver, "sensorId == \"" + sensorId + "\"");
        
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div[id$='observationPanel']")));
        
        PanelWaitCondition panelWait = new PanelWaitCondition(String.format("sensorId is [%s]", sensorId));
        wait.until(panelWait);
        
        assertThat(String.format("Unable to find observation by sensor id %s", 
                sensorId), panelWait.getPanel(), notNullValue());
        return panelWait.getPanel();
    }
    
    /**
     * Function used to verify whether the given observation displays the expected information.
     * This function expects that an observation will be found by sensor id. Therefore, sensor id
     * must be set on the expected observation.
     * @param observation
     *  the observation that is to be used to verify the correct information is displayed
     */
    private static void verifyObservationPanelBySensorId(ExpectedObservation observation)
    {
        WebElement panel = grabObservationPanelBySensorId(observation.getSensorId());

        verifyObservationPanel(panel, observation);

        toggleObservationPanel(panel);

        verifyGeneralObservationInformation(observation.getGeneralInfo(), panel);

        if (observation.getObservationSubTypeEnum() != null 
                && !observation.getObservationSubTypeEnum().equals(ObservationSubTypeEnum.DETECTION))
        {
            verifyObservationInformation(panel, observation);
        }

        if (observation.hasSensingModalities())
        {
            verifyObservationSensings(panel, observation);
        }

        if (observation.isDetection())
        {
            verifyObservationInformationDetection(panel, observation);
        }

        if (observation.hasRelatedObs())
        {
            verifyRelatedObsPanel(panel, observation);
        }
    }
    
    /**
     * Verifies general information information is displayed for a given observation panel.
     * @param information
     *  the object which contains the general information that is to be displayed in the given observation panel
     * @param obsPanel 
     *  the observation panel that should hold the given general information
     *  
     */
    private static void verifyGeneralObservationInformation(
            GeneralObservationInformation information, WebElement obsPanel)
    {
        //check the header 
        WebElement infoHeader = obsPanel.findElement(By.cssSelector("span[id$='genHeader']"));
        assertThat(infoHeader.getText(), is("General Information"));
        
        //check uuid (should always be there)
        WebElement uuidHeader = obsPanel.findElement(By.cssSelector("span[id$='genHeaderUuid']"));
        assertThat(uuidHeader.getText(), is("UUID"));
       
        WebElement uuidValue = obsPanel.findElement(By.cssSelector("span[id$='genObsUuid']"));
        assertThat(uuidValue.getText(), not(isEmptyOrNullString()));
        
        //check system id (should always be there)
        WebElement sysIdHeader = obsPanel.findElement(By.cssSelector("span[id$='genHeaderSysId']"));
        assertThat(sysIdHeader.getText(), is("System Id"));
        
        WebElement sysIdValue = obsPanel.findElement(By.cssSelector("span[id$='genSysId']"));
        assertThat(sysIdValue.getText(), not(isEmptyOrNullString()));
        
        if (information != null)
        {
            if (information.isSetAssetLocation())
            {
                //check uuid (should always be there)
                WebElement assetLocHeader = obsPanel.findElement(By.cssSelector("span[id$='genHeaderAssetLoc']"));
                assertThat(assetLocHeader.getText(), is("Asset Location"));
               
                WebElement assetLocValue = obsPanel.findElement(By.cssSelector("span[id$='genInfoAssetLoc']"));
                assertThat(assetLocValue.getText(), not(isEmptyOrNullString()));
            }
            
            if (information.isSetAssetOrientation())
            {
                //check uuid (should always be there)
                WebElement assetOrientationHeader = obsPanel.findElement(
                        By.cssSelector("span[id$='genHeaderAssetOrient']"));
                assertThat(assetOrientationHeader.getText(), is("Asset Orientation"));
               
                WebElement assetOrientValue = obsPanel.findElement(By.cssSelector("span[id$='genInfoAssetOrient']"));
                assertThat(assetOrientValue.getText(), not(isEmptyOrNullString()));
            }
            
            if (information.isSetPlatformOrientation())
            {
                //check uuid (should always be there)
                WebElement pltOrientationHeader = obsPanel.findElement(
                        By.cssSelector("span[id$='genHeaderPltOrient']"));
                assertThat(pltOrientationHeader.getText(), is("Platform Orientation"));
               
                WebElement pltOrientValue = obsPanel.findElement(By.cssSelector("span[id$='genInfoPltOrient']"));
                assertThat(pltOrientValue.getText(), not(isEmptyOrNullString()));
            }
            
            if (information.isSetPointingLocation())
            {
                //check uuid (should always be there)
                WebElement pointingLocHeader = obsPanel.findElement(By.cssSelector("span[id$='genHeaderPointingLoc']"));
                assertThat(pointingLocHeader.getText(), is("Pointing Location"));
               
                WebElement pointingLocValue = obsPanel.findElement(By.cssSelector("span[id$='genInfoPointingLoc']"));
                assertThat(pointingLocValue.getText(), not(isEmptyOrNullString()));
            }
            
            if (information.hasReservedField())
            {
                verifyReservedFieldPanel(obsPanel, information);
            }
        }
    }
    
    /**
     * Function which takes a web element that holds the tables (image and description elements) for an
     * individual relation panel and clicks the first command link from the first entry.
     * @param panel
     *  the panel that contains the image/description lines
     */
    public static void getAndClickFirstRelatedObservation(WebElement panel)
    {
        List<WebElement> listObsAsTables = panel.findElements(By.cssSelector("table[id$='relatedObsGrid']"));
        
        WebElement obsTable = listObsAsTables.get(0);
        List<WebElement> commandEnabledLink = obsTable.findElements(By.cssSelector("span>a"));
        assertThat("Unable to grab the command link for the related observation panel.", 
                commandEnabledLink.size(), greaterThan(0));
        
        WebElement link = commandEnabledLink.get(0);
        link.click();
    }
    
    /**
     * Finds the panel of the related observations section that matches the given type.
     * @param panel
     *  the panel to search for the relation section
     * @param type
     *  the relationship type that corresponds to the section that should be grabbed
     * @return
     *  the web element that is the section based on the given relationship type
     */
    public static WebElement findRelatedObservationSectionByType(WebElement panel, RelationshipTypeEnum type)
    {
        WebElement relatedObsPanel = ObservationHelper.grabRelatedObsPanel(panel);
        List<WebElement> relatedSectionPanels = ObservationHelper.grabRelatedObservationSectionPanels(relatedObsPanel);
        
        WebElement panelToReturn = null;
        
        for (WebElement indivPanel : relatedSectionPanels)
        {
            WebElement panelHeader = indivPanel.findElement(By.cssSelector("span[id$='relationHeader']"));
            assertThat(panelHeader, notNullValue());
            
            if (panelHeader.getText().equals(type.toString().toUpperCase()))
            {
                panelToReturn = indivPanel;
                break;
            }
        }
        
        assertThat(String.format("Could not find relationship section for type %s", type), 
                panelToReturn, notNullValue());
        return panelToReturn;
    }
    
    /**
     * Verifies a related observation panel. (A related observation panel is defined as the panel 
     * which possibly contains all three relationship types, parent, child, and peer).
     * @param panel
     *  the web element which has the related obs panel within it
     * @param observation
     *  the observation which has the expected relationships
     */
    public static void verifyRelatedObsPanel(WebElement panel, ExpectedObservation observation)
    {
        //get the panel grid that holds the related observations
        WebElement relatedPanel = grabRelatedObsPanel(panel);
        
        //verify the header and image are what is expected
        WebElement relationHeader = relatedPanel.findElement(By.cssSelector("span[id$='relatedHeader']"));
        assertThat(relationHeader, notNullValue());
        
        WebElement linkPanelImage = relatedPanel.findElement(By.cssSelector("img[id$='linkObsImg']"));
        String linkSource = linkPanelImage.getAttribute("src");
        String linkPic = ImageHelper.getPictureOrIconName(linkSource, ".png");
        assertThat(linkPic, is(ObservationConstants.IMG_LINK));
        
        List<WebElement> relatedPanels = grabRelatedObservationSectionPanels(relatedPanel);
        assertThat(relatedPanels.size(), is(observation.getRelatedObservation().getNumberOfSections()));
        
        int foundPanelCount = 0;
        //loop through and verify that they are correct
        for (WebElement indivPanel : relatedPanels)
        {
            WebElement panelHeader = indivPanel.findElement(By.cssSelector("span[id$='relationHeader']"));
            assertThat(panelHeader, notNullValue());
            
            if (panelHeader.getText().equals("PARENT"))
            {
                verifyRelatedObsIndividualPanel(indivPanel, observation, RelationshipTypeEnum.PARENT);
                foundPanelCount++;
            }
            else if (panelHeader.getText().equals("CHILD"))
            {
                verifyRelatedObsIndividualPanel(indivPanel, observation, RelationshipTypeEnum.CHILD);
                foundPanelCount++;
            }
            else if (panelHeader.getText().equals("PEER"))
            {
                verifyRelatedObsIndividualPanel(indivPanel, observation, RelationshipTypeEnum.PEER);
                foundPanelCount++;
            }
            else
            {
                fail(String.format("Found an individual observation heading %s " 
                        + "that does not match any of the expected headings (Peer, Child, Parent).",
                        panelHeader.getText()));
            }
        }
        
        assertThat(foundPanelCount, is(observation.getRelatedObservation().getNumberOfSections()));
    }
    
    /**
     * Verify the reserved field data is correct including the key and value data.
     * 
     * @param panel
     *      observation panel to check out
     */
    public static void verifyReservedFieldPanel(WebElement panel, GeneralObservationInformation observation)
    {
        WebElement reservedFieldHeader = panel.findElement(
                By.cssSelector("span[id$='genHeaderReserved']"));
        assertThat(reservedFieldHeader.getText(), is("Reserved Fields"));
       
        WebElement reservedData = panel.findElement(By.cssSelector("span[id$='genInfoReserved']"));
        String reservedStringData = reservedData.getText();
        
        int counterfound = 0;
        for (MapEntry entry : observation.getReservedFields())
        {
            assertThat(reservedStringData, containsString(entry.getKey()));
            assertThat(reservedStringData, containsString((String)entry.getValue()));
            counterfound++;
        }
        
        assertThat(counterfound, is(observation.getReservedFields().size()));
    }
    
    /**
     * Function to verify observation panels. This function takes a panel to search in, a expected mode icon
     * to look for, and a list of expected images to be found.
     */
    public static void verifyObservationPanel(WebElement panel, ExpectedObservation observation)
    {
        //verify asset name
        WebElement wassetName = panel.findElement(By.cssSelector("span[id*='obsAssetName']"));
        assertThat(wassetName.getText(), is(observation.getAssetName()));
        
        //verify mode
        WebElement mode;
        switch (observation.getSystemMode())
        {
            case TEST_MODE:
                mode = panel.findElement(By.cssSelector("img[id*='testMode']"));
                String opSource = mode.getAttribute("src");
                String opPic = ImageHelper.getPictureOrIconName(opSource, ".png");
                assertThat(opPic, is(observation.retireveExpectedSystemModeImage()));
                break;
            case OPERATIONAL_MODE:
                mode = panel.findElement(By.cssSelector("img[id*='opMode']"));
                String testSource = mode.getAttribute("src");
                String testPic = ImageHelper.getPictureOrIconName(testSource, ".png");
                assertThat(testPic, is(observation.retireveExpectedSystemModeImage()));
                break;
        }
        
        //verify icons
        WebElement obImage = panel.findElement(By.cssSelector("table>tbody>tr:nth-child(1)>td:nth-child(3)"));
        List<WebElement> images = obImage.findElements(By.tagName("img"));
        
        if (observation.hasRelatedObs())
        {
            WebElement linkImg = panel.findElement(By.cssSelector("table>tbody>tr:nth-child(1)>td:nth-child(4)"));
            images.add(linkImg.findElement(By.tagName("img")));
        }
        
        verifyImageSet(images, observation.retrieveExpectedHeaderImages());

        // verify created time
        WebElement time = panel.findElement(By.cssSelector("span[id*='obsCreatedTime']"));
        assertThat(time.getText().isEmpty(), is(false));

        // verify observed time if set in observation
        if (observation.hasObservedTime())
        {
            time = panel.findElement(By.cssSelector("span[id*='obsObservedTime']"));
            assertThat(time.getText().isEmpty(), is(false));
        }
    }

    /**
     * Verify the images and text are correct for all information given for the observation that contains all possible
     * sensing modalities.
     */
    private static void verifyObservationSensings(WebElement panel, ExpectedObservation observation)
    {
        //verify headings and content
        if (observation.retrieveExpectedModalityImages().size() > 0)
        {
            WebElement sensingModalitiesHeader = panel.findElement(By.cssSelector("span[id$='modalitiesHeader']"));
            assertThat(sensingModalitiesHeader.getText(), is("Sensing Modalities"));
        }
        else
        {
            //using findElements to avoid exception being thrown
            List<WebElement> sensingModalitiesHeader = panel.findElements(
                    By.cssSelector("span[id$='modalitiesHeader']"));
            assertThat(sensingModalitiesHeader.size(), is(0));
        }

        //verify all found dMods are all of the expected dMods
        List<WebElement> sensings = panel.findElements(By.cssSelector("img[id*='dMod']"));
        assertThat(sensings.size(), is(observation.retrieveExpectedModalityImages().size()));
        
        verifyImageSet(sensings, observation.retrieveExpectedModalityImages());        
    }

    /**
     * Verify the images and text are correct for all information given for the observation that contains all possible
     * target classifications.
     */
    private static void verifyObservationInformationDetection(WebElement panel, 
            ExpectedObservation observation)
    {
        //verify headings and content
        if (observation.retrieveExpectedClassImages().size() > 0)
        {
            WebElement targetClassificationsHeader = panel.findElement(
                    By.cssSelector("span[id$='classificationsHeader']"));
            assertThat(targetClassificationsHeader.getText(), is("Target Classifications"));
        }
        else
        {
            //using findElements to avoid exception being thrown
            List<WebElement> targetClassificationsHeader = panel.findElements(
                    By.cssSelector("span[id$='classificationsHeader']"));
            assertThat(targetClassificationsHeader.size(), is(0));
        }
        
        //verify detection
        WebElement detectionImg = panel.findElement(By.cssSelector("img[id*='detect']"));
        ImageHelper.getPictureOrIconNameAndVerify(detectionImg, ObservationConstants.IMG_DETECTION, ".png");
        
        //verify all found dClasses are all of the expected dMods
        List<WebElement> classifications = panel.findElements(By.cssSelector("img[id*='dClass']"));
        assertThat(classifications.size(), is(observation.retrieveExpectedClassImages().size()));
        
        verifyImageSet(classifications, observation.retrieveExpectedClassImages());
    }
    
    /**
     * Function to verify displayed observation information. This function assumes that the passed in 
     * observation does not contain any detection data.
     */
    private static void verifyObservationInformation(WebElement panel, ExpectedObservation observation)
    {
        //get picture and mime type (mime type is only for the heading in the expanded content. 
        //does not correspond to link!)
        if (observation.isDigitalMedia())
        {
            WebElement type = panel.findElement(By.cssSelector("span[id$='mimeType']"));
            assertThat(type.getText(), is(observation.getMimeType()));
        }
        
        WebElement heading = panel.findElement(By.cssSelector("span[id$='obsHeader']"));
        assertThat(heading.getText(), is(observation.getObservationHeading()));
        
        WebElement pic = panel.findElement(By.cssSelector("img[id$='pic']"));
        String source = pic.getAttribute("src");
        String found = ImageHelper.getPictureOrIconName(source, ".png");
        
        assertThat(found, is(observation.retrieveObservationTypeEnumImage()));
        
        WebElement obsData = panel.findElement(By.cssSelector("span[id$='obsData']"));
        assertThat(obsData.getText().isEmpty(), is(false));
        
        for (String data : observation.getObsData())
        {
            assertThat("Missing: " + data, obsData.getText().contains(data), is(true));
        }
    }
    
    /**
     * Toggles the observation panel.
     * @param panel
     *  the panel to toggle
     */
    public static void toggleObservationPanel(WebElement panel)
    {
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        WebElement toggler = panel.findElement(By.cssSelector("a[id*='toggler']"));
        toggler.click();
        
        WebDriverWait wait = new WebDriverWait(driver, 30);
        
        wait.until(ExpectedConditions.visibilityOf(panel.findElement(
                By.cssSelector("div[id$='observationPanel_content']"))));
    }
    
    /**
     * Verify the given expected observations are visible.
     * 
     * @param expectedObservations 
     *      list of observations that are expected
     */
    public static void verifyExpectedObservations(List<ExpectedObservation> expectedObservations)
    {
        WebDriver driver = WebDriverFactory.retrieveWebDriver();
        ObservationFilterHelper.clickFilterAccordionPanel(driver, ExpandCollapse.EXPAND);
        ObservationFilterHelper.clickFilterByExpressionCheckBox(driver);
        
        for (ExpectedObservation obs : expectedObservations)
        {
            DebugText.pushText("checking observation for sensor id [%s]", obs.getSensorId());
            try
            {
                ObservationHelper.verifyObservationPanelBySensorId(obs);
            }
            finally
            {
                DebugText.popText();
            }
        }
    }
    
    /**
     * Navigates to the observation tab of the asset page and clicks to delete all observations.
     * 
     * @param driver
     *      The selenium web driver.
     *      
     * @throws InterruptedException
     *      when the driver is interrupted while performing the actions.
     */
    public static void deleteAllObservations(WebDriver driver) throws InterruptedException
    {
        //Navigate to the observation tab of the asset page.
        NavigationHelper.pageAndTabCheck(driver, NavigationButtonNameConstants.NAVBUT_PROP_ASSETS, 
                AssetHelper.ASSET_OBERSVATION_TAB_ID);
        
        //Remove all observations.
        ObservationFilterHelper.clickRetrieveDeleteAccordionPanel(driver, ExpandCollapse.EXPAND);
        ObservationFilterHelper.executeObservationDeletion(driver);
        ObservationFilterHelper.waitTillObsTableUpdates(driver, 0, 15);
    }
    
    /**
     * Grabs a related observation panel.
     * @param panel
     *  the observation panel that contains the desired related observation panel.
     * @return
     *  the related observation panel which contains all the related observation links.
     */
    private static WebElement grabRelatedObsPanel(WebElement panel)
    {
      //get the panel grid that holds the related observations
        WebElement relatedPanel = panel.findElement(By.cssSelector("table[class$='relatedLinkPanelGrid']"));
        assertThat(relatedPanel, notNullValue());
        
        return relatedPanel;
    }
    
    /**
     * Retrieves the elements that make up the sections of the related observation panel. e.g. the panels 
     * returned will be the relations that the observation has (Parent, child, peer).
     * @param panel
     *  the panel that is to be searched for the observation panels
     * @return
     *  the list of panels (parent, child, peer) that are found
     */
    private static List<WebElement> grabRelatedObservationSectionPanels(WebElement panel)
    {
        //grab the second table cell which holds the parent, peer, and children headings
        WebElement secondCellObs = panel.findElement(By.cssSelector("tbody>tr>td:nth-child(2)"));
        assertThat(secondCellObs, notNullValue());
        
        List<WebElement> relatedPanels = secondCellObs.findElements(By.cssSelector("span[id$='relatedObs']"));
        
        return relatedPanels;
    }
    
    /**
     * Function verifies a section of the related observation entries (Section is defined as one 
     * that is split by type. e.g. parent, child, peer are three separate sections).
     * @param panel
     *  the web element that holds the section
     * @param observation
     *  the observation which has the related observations
     * @param type
     *  the type of relationship to verify is present
     */
    private static void verifyRelatedObsIndividualPanel(WebElement panel, 
            ExpectedObservation observation, RelationshipTypeEnum type)
    {
        List<RelatedObsStruct> list = observation.getRelatedObservation().getListForRelation(type);
        
        assertThat(list.size(), greaterThan(0));
        List<WebElement> listObsAsTables = panel.findElements(By.cssSelector("table[id$='relatedObsGrid']"));
        
        assertThat("Number of retrieved entries does not match the number of expected observation entries!", 
                listObsAsTables.size(), is(list.size()));
        
        List<RelatedObsStruct> expectedObs = observation.getRelatedObservation().getListForRelation(type);
        
        //iterate through the list of expected related observations
        for (RelatedObsStruct obsRelation : expectedObs)
        {
            boolean found = false;
            for (WebElement table : listObsAsTables)
            {
                //find a command link that is enabled
                List<WebElement> commandEnabledLink = table.findElements(By.cssSelector("span>a"));
                if (commandEnabledLink.size() > 0 )
                {
                    //obsRelation can have null observation (e.g. if observation is unknown/not found)
                    //which would not be for this case
                    if (obsRelation.getObservation() != null)
                    {
                        //get picture 
                        WebElement miniImage = table.findElement(By.cssSelector("img[id$='miniImage']"));
                        assertThat(miniImage, notNullValue());
                        String src = miniImage.getAttribute("src");
                        String pic = ImageHelper.getPictureOrIconName(src, ".png");
                        
                        if (obsRelation.getObservation().retrieveRelatedObservationImage().equals(pic))
                        {
                            String description = commandEnabledLink.get(0).getText();
                            
                            //if description is empty then UUID should be there
                            if (obsRelation.getDescription().isEmpty())
                            {
                                try
                                {
                                    UUID.fromString(description);
                                }
                                catch (IllegalArgumentException exception)
                                {
                                    fail(String.format("Description for an entry in relation type %s " 
                                            + "for observation with sensor id %s is incorrect", type, 
                                            observation.getSensorId()));
                                }
                                
                                found = true;
                                break;
                            }
                            else
                            {
                                //otherwise check string given is in the description found
                                if (description.contains(obsRelation.getDescription()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                        }
                    }
                }
                else
                {
                    //couldn't find an enabled link so entry must be an unknown
                    List<WebElement> commandDisabledLink = table.findElements(By.cssSelector("span"));
                    if (commandDisabledLink.size() > 0)
                    {
                        if (obsRelation.getObservation() == null)
                        {
                            WebElement miniImage = table.findElement(By.cssSelector("img[id$='miniImage']"));
                            assertThat(miniImage, notNullValue());
                            String src = miniImage.getAttribute("src");
                            String pic = ImageHelper.getPictureOrIconName(src, ".png");
                            
                            if (pic.equals(ObservationConstants.IMG_UNKNOWN))
                            {
                                //verify warning icon
                                WebElement warningIcon = table.findElement(
                                        By.cssSelector("span[class='ui-icon ui-icon-alert']"));
                                assertThat(warningIcon, notNullValue());
                                
                                String description = commandDisabledLink.get(0).getText();
                                
                                //make sure description displayed is not empty; comparison should be 
                                //better but since UUID is not available a better comparison cannot be 
                                //made
                                if (obsRelation.getDescription().isEmpty() && !description.isEmpty())
                                {
                                    found = true;
                                    break;
                                }
                                else if (description.contains(obsRelation.getDescription()))
                                {
                                    found = true;
                                    break;
                                }
                            }
                            else
                            {
                                //image should be the unknown image. if it is anything else that is bad
                                fail(String.format("Improper image shown for unknown observation " 
                                        + "in relationship type %s", type));
                            }
                        }
                    }
                    else
                    {
                        //couldn't find any links so that is bad 
                        fail(String.format("Could not find command link for an entry of relationship type %s" 
                                + " for observation with sensor id [%s]",
                                type, observation.getSensorId()));
                    }
                }
            }
            
            //if I get through the list of displayed and not found then fail
            assertThat(String.format("Unable to find relation entry for" 
                    + " observation with sensor id [%s]", observation.getSensorId()), found, is(true));
        }
    }
    
    /**
     * Takes a list of image elements found on the screen and compares them to a list of 
     * expected images.
     * @param imageElements
     *  the image elements that are found on the screen
     * @param expectedImages
     *  the images that are expected to be found
     */
    private static void verifyImageSet(List<WebElement> imageElements, List<String> expectedImages)
    {
        Set<String> imageSet = new HashSet<>();
        for (WebElement image : imageElements)
        {
            imageSet.add(ImageHelper.getPictureOrIconName(image.getAttribute("src"), ".png"));
        }
        
        for (String expectedImage : expectedImages)
        {
            assertThat(imageSet, hasItem(expectedImage));
        }
    }
    
    /**
     * @author dhumeniuk
     *
     */
    private static final class PanelWaitCondition implements ExpectedCondition<Boolean>
    {
        private WebElement m_Panel;
        private String m_Condition;

        PanelWaitCondition(String condition)
        {
            m_Condition = condition;
        }
        
        public WebElement getPanel()
        {
            return m_Panel;
        }
        
        @Override
        public Boolean apply(WebDriver driver) 
        {
            List<WebElement> panels = driver.findElements(By.cssSelector("div[id$='observationPanel']"));
            if (panels.size() == 1)
            {
                m_Panel = panels.get(0);
                return true;
            }
            else
            {
                return false;
            }
        }
        
        @Override
        public String toString()
        {
            return String.format("panel containing %s", m_Condition);
        }
    }
}
