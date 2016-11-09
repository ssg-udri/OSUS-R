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
package mil.dod.th.ose.sdk.those;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.core.ccomm.transport.capability.TransportLayerCapabilities;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.core.types.ccomm.LinkLayerTypeEnum;
import mil.dod.th.ose.utils.xml.XmlUtils;

/**
 * Utility class to create a capabilities xml byte array for the Transport Layer.
 * 
 * @author m.elmo
 */
public final class ProjectTransportLayerCapabilities 
{
    /**
     * Default private constructor to prevent instantiation.
     */
    private ProjectTransportLayerCapabilities() 
    {
    }

    /**
     * Creates a capabilities object and converts that object to xml in the form
     * of a byte array.
     * 
     * @return The byte array containing the capabilities xml.
     */
    public static byte[] getCapabilities() 
    {
        final TransportLayerCapabilities cap = makeCapabilities();
        return XmlUtils.toXML(cap, true);
    }

    /**
     * Creates an object that is populated with placeholder values for a
     * Capabilities object.
     *
     * @return Capabilities object
     */
    private static TransportLayerCapabilities makeCapabilities() 
    {
        final DigitalMedia primaryImage = ProjectBaseCapabilities.makePrimaryImage();
        final List<DigitalMedia> secondaryImages = ProjectBaseCapabilities.makeSecondaryImages();
        
        final String productName = ProjectBaseCapabilities.getProductName();
        final String description = ProjectBaseCapabilities.getDescription();
        final String manufacturer = ProjectBaseCapabilities.getManufacturer();

        final List<LinkLayerTypeEnum> linkLayerModalitiesSupported = makeLinkLayerModalitiesSupportedList();
        
        return new TransportLayerCapabilities(primaryImage, secondaryImages, productName, description, 
                manufacturer, linkLayerModalitiesSupported, false);
    }
    
    /**
     * Creates an List populated with the values from {@link LinkLayerTypeEnum}.
     *
     * @return List.
     */
    private static List<LinkLayerTypeEnum> makeLinkLayerModalitiesSupportedList()
    {
        final List<LinkLayerTypeEnum> linkLayerEnumList = new ArrayList<>();
        for (LinkLayerTypeEnum linkLayerEnum: LinkLayerTypeEnum.values())
        {
            linkLayerEnumList.add(linkLayerEnum);
        }
        return linkLayerEnumList;
    }
}
