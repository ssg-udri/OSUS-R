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
package mil.dod.th.ose.gui.webapp.comms;

import javax.ejb.Singleton;

import mil.dod.th.core.ccomm.link.capability.LinkLayerCapabilities;
import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.ose.gui.webapp.general.AbstractImageDisplay;

/**
 * Class to get images that pertain to a comms layer base.
 * @author matt gleason
 *
 */
@Singleton
public class CommsImage extends AbstractImageDisplay
{
    /**
     * Default comms directory for images.
     */
    private static final String COMMS_IMAGE_DIR = "thoseIcons/comms/comms_";
    
    /**
     * Default file extension for comm images.
     */
    private static final String FILE_EXT = ".png";
    
    /**
     * Method to get an image for a transport layer.
     * @param transCapabils
     *      the capabilities of the transport layer
     * @return
     *      the string URL of the image
     */
    public String getTransportImage(final TransportLayerCapabilities transCapabils)
    {
        final String primaryImage = getPrimaryImage(transCapabils);
        
        if (primaryImage != null)
        {
            return primaryImage;
        }
        
        if (transCapabils != null && transCapabils.getLinkLayerModalitiesSupported() != null 
                && transCapabils.getLinkLayerModalitiesSupported().size() != 0)
        {
            return COMMS_IMAGE_DIR 
                    + transCapabils.getLinkLayerModalitiesSupported().get(0).toString().toLowerCase() + FILE_EXT;
        }
        
        return getImage();
    }
    
    /**
     * Method to get an image for a link layer.
     * @param linkCaps
     *      the capabilities of the link layer
     * @return
     *      the string URL of the image
     */
    public String getLinkLayerImage(final LinkLayerCapabilities linkCaps)
    {
        final String primaryImage = getPrimaryImage(linkCaps);
        
        if (primaryImage != null)
        {
            return primaryImage;
        }
        
        if (linkCaps != null && linkCaps.getModality() != null)
        {
            return COMMS_IMAGE_DIR + linkCaps.getModality().toString().toLowerCase() + FILE_EXT;
        }
        
        return getImage();
    }
    
    /**
     * Get the comms serial image.
     * @return
     *      the string URL of the comms serial image.
     */
    public String getPhysicalLinkImage()
    {
        return COMMS_IMAGE_DIR + "serial" + FILE_EXT;
    }
    
    /**
     * Get the generic comms image.
     * @return
     *      the string URL of the generic comms image.
     */
    @Override
    public String getImage()
    {
        return COMMS_IMAGE_DIR + "generic" + FILE_EXT;
    }
    
    /**
     * Get the comms socket image.
     * @return
     *      the string URL of the comms socket image.
     */
    public String getSocketImage()
    {
        return "thoseIcons/comms/socket" + FILE_EXT;
    }
}
