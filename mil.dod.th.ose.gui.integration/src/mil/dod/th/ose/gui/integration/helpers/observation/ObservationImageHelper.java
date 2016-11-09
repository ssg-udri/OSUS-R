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
import static org.hamcrest.Matchers.is;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import mil.dod.th.core.types.SensingModalityEnum;
import mil.dod.th.core.types.detection.TargetClassificationTypeEnum;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;

/**
 * Contains functions to retrieve images for observations.
 * 
 * @author nickmarcucci
 *
 */
public class ObservationImageHelper
{
    /**
     * Returns observation type image based on the given type.
     * @param type
     *  the given type of observation
     * @return
     *  the image name for the given observation type
     */
    public static String getObservationSubTypeEnumImage(ObservationSubTypeEnum type)
    {
        if (type != null)
        {
            return type.toString().toLowerCase() + ".png";
        }
        
        return "unknown.png";
    }
    
    /**
     * Returns target classification image based on the given type.
     * @param tgt
     *  the target classification to retrieve an image for
     * @return
     *  the image name for the given classification type
     */
    public static String getTargetClassificationImage(TargetClassificationTypeEnum tgt)
    {
        return tgt.toString().toLowerCase() + ".png";
    }
    
    /**
     * Returns sensing modality image based on the given type.
     * @param mod
     *  the sensing modality to retrieve an image for
     * @return
     *  the image name for the given modality type
     */
    public static String getSensingModalityImage(SensingModalityEnum mod)
    {
        return mod.toString().toLowerCase() + ".png";
    }
    
    /**
     * Verify that the image dialogue is displayed correctly. Assumes the view image button of
     * observation has already been clicked and will detect the elements of the digital media viewer.
     *  
     * @param driver
     *      Selenium Web Driver
     */
    public static void verifyImageIsDisplayed(WebDriver driver)
    {
        WebDriverWait wait = new WebDriverWait(driver, 60);
        
        //Wait for the observation view dialog to be visible.
        WebElement viewerDialog = driver.findElement(By.cssSelector("div[id*='digitalMediaViewer']"));
        wait.until(ExpectedConditions.visibilityOf(viewerDialog));
        
        //Verify that the download button on the observation view dialog is displayed.
        WebElement dlImage = viewerDialog.findElement(By.cssSelector("button[id*='dlImage']"));
        assertThat(dlImage.isDisplayed(), is(true));
        
        //Verify that the image is displayed and that the alternative information is correct.
        WebElement image = viewerDialog.findElement(By.cssSelector("img[id*='obsImageHasRes']"));
        assertThat(image.isDisplayed(), is(true));
        assertThat(image.getAttribute("alt").
                equals("Image type not supported by browser. Please download image to view."), is(true));
        
        //Close the observation viewer dialog.
        WebElement closeDialog = viewerDialog.findElement(By.cssSelector(
                "span[class*='ui-icon ui-icon-closethick']"));
        closeDialog.click();
        
        //Wait till the observation viewer dialog is no longer visible.
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.cssSelector("div[id*='observationViewer']")));
        assertThat(viewerDialog.isDisplayed(), is(false));
    }
}
