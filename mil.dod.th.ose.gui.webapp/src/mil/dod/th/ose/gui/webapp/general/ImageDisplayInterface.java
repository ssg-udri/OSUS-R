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
package mil.dod.th.ose.gui.webapp.general;

/**
 * Interface to enable the display of images on the GUI.
 * 
 * @author nickmarcucci
 */
public interface ImageDisplayInterface
{
    /**
     * Default image string. This image should be used as a fallback image.
     */
    String DEFAULT_IMAGE = "thoseIcons/default/defaultImage.png";
    
    /**
     * Function that returns the default image.
     * @return
     *  the default image
     */
    String getImage();
}
