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

import mil.dod.th.core.capability.BaseCapabilities;
import mil.dod.th.core.types.DigitalMedia;

/**
 * Abstract class to display images on the GUI.
 * @author matt gleason
 */
public abstract class AbstractImageDisplay implements ImageDisplayInterface
{
    /*
     * (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.general.ImageDisplayInterface#getImage()
     */
    @Override
    public String getImage()
    {
        return DEFAULT_IMAGE;
    }
    
    /**
     * Function to get the primary image to be displayed to the GUI.
     * @param baseCapabils
     *      capabilities object
     * @return
     *      the image to display to the GUI or null if no primary image exists.
     */
    public String getPrimaryImage(final BaseCapabilities baseCapabils)
    {
        //Get the primary image
        if (baseCapabils != null)
        {
            final DigitalMedia primeImage = baseCapabils.getPrimaryImage();
            
            if (primeImage != null)
            {   //NOCHECKSTYLE avoid empty if statements.. to be implemented
                //return the primary image if found.
                //TODO: TH-1089 - functionality needs to be added to
                //display the primary image
            }
        }
        
        //return null if primary image could not be retrieved
        return null;
    }
}
