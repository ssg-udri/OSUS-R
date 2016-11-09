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
package mil.dod.th.ose.gui.webapp.controller;

import javax.ejb.Singleton;

import mil.dod.th.core.controller.capability.ControllerCapabilities;
import mil.dod.th.ose.gui.webapp.general.AbstractImageDisplay;

/**
 * Class to get images that pertain to a controller.
 * @author matt gleason
 */
@Singleton
public class ControllerImage extends AbstractImageDisplay
{
    /**
     * Method to get an image for a controller.
     * @param controllerCaps
     *      the capabilities of the controller
     * @return
     *      the string URL of the controller
     */
    public String getImage(final ControllerCapabilities controllerCaps)
    {
        final String primaryImage = super.getPrimaryImage(controllerCaps);
        
        if (primaryImage != null)
        {
            return primaryImage;
        }
        
        return getImage();
    }
    
    /**
     * Get the default controller image.
     * @return
     *      the string URL of the image
     */
    @Override
    public String getImage()
    {
        return "thoseIcons/default/controller.png";
    }
}
