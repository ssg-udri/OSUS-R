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
package mil.dod.th.ose.gui.webapp.asset;

import javax.ejb.Singleton;

import mil.dod.th.core.asset.capability.AssetCapabilities;
import mil.dod.th.ose.gui.webapp.general.AbstractImageDisplay;

/**
 * Class to get images that pertain to an asset model.
 * @author matt gleason
 *
 */
@Singleton
public class AssetImage extends AbstractImageDisplay
{
    /**
     * Method to get an image for an asset model.
     * @param assetCapabils
     *      the capabilities of the asset
     * @return
     *      the string URL of the asset image
     */
    public String getImage(final AssetCapabilities assetCapabils)
    {
        final String primaryImage = getPrimaryImage(assetCapabils);
        
        if (primaryImage != null)
        {
            return primaryImage;
        }
        
        if (assetCapabils != null && assetCapabils.getModalities() != null && assetCapabils.getModalities().size() != 0)
        {
            final String pic = assetCapabils.getModalities().get(0).getValue().
                    toString().toLowerCase();
            
            return "thoseIcons/sensingModality/" + pic + ".png";
        }
        
        return getImage();
    }
}
