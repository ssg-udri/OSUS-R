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
import static org.hamcrest.Matchers.*;

import org.openqa.selenium.WebElement;

/**
 * Helper method for images, particularly finding the picture or icon name given the source attribute of a webelement.
 * 
 * @author nick
 *
 */
public class ImageHelper
{
    /**
     * Function which takes an image source line and parses it to find the icon/picture it pertains to. Function 
     * expects that the last character before the beginning of the icon/picture name is '/'.
     * @param source
     *      the source attribute of an image element
     * @param iconExtension
     *      the extension type of image to get the name of (such as ".png")
     */
    public static String getPictureOrIconName(String source, String iconExtension)
    {
        String found = "";
        
        int indexExt = source.indexOf(iconExtension);
        int lastSlash = source.lastIndexOf('/');
        
        if (indexExt != -1 && lastSlash != -1)
        {
            int width = (indexExt - (lastSlash + 1)) + iconExtension.length();
            
            found = source.substring(lastSlash + 1, (lastSlash + 1) + width);
        }
        
        return found;
    }
    
    /**
     * Function will verify the display and existence of the specified icon/image based on the passed in web element.
     * This function assumes the passed in imageElement is a web element representing an html end img tag.
     * 
     * @param imageElement
     *  the html end img tag that is to be verified
     * @param expectedIconName
     *  the name of the expected icon that should be displayed via the image web element
     * @param extension
     *  the extension type of image to get the name of (such as ".png")
     */
    public static void getPictureOrIconNameAndVerify(final WebElement imageElement, 
            final String expectedIconName, final String extension)
    {
        assertThat(String.format("Expected icon [%s] is not displayed", expectedIconName), 
                imageElement.isDisplayed(), is(true));
        
        String imgSource = imageElement.getAttribute("src");
        
        String imageName = getPictureOrIconName(imgSource, extension);
        
        assertThat(imageName, is(expectedIconName));
    }
}
