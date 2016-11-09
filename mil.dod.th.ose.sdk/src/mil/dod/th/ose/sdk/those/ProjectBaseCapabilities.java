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

import mil.dod.th.core.types.DigitalMedia;

/**
 * static base capability variables.
 * 
 * @author m.elmo
 *
 */
public final class ProjectBaseCapabilities 
{
     /**
     * Variable to populate the MIME image type encoding.
     */
    public static final String MIME_IMAGE = "image/jpeg";

    /**
     * Variable to populate the MIME unknown type encoding.
     */
    public static final String MIME_UNKNOWN = "Unknown";

    /**
     * Default private constructor to prevent instantiation.
     */
    private ProjectBaseCapabilities()
    { 
    }
    
     /**
     * Creates a primary image object placeholder.
     * @return DigitalMedia object
     */
    public static DigitalMedia makePrimaryImage() 
    {
        return makeDigitalMedia(MIME_IMAGE);
    }
    
    /**
     * Creates a list of secondary images populated with placeholder values.
     * @return List of DigitalMedia objects
     */
    public static List<DigitalMedia> makeSecondaryImages() 
    {
        final List<DigitalMedia> secondaryImages = new ArrayList<DigitalMedia>();
        secondaryImages.add(makeDigitalMedia(MIME_IMAGE));
        secondaryImages.add(makeDigitalMedia(MIME_UNKNOWN));
        return secondaryImages;
    }
    
    /**
     * Creates a DigitalMedia object.
     * @param encoding string used to create the DigitalMedia object
     * @return DigitalMedia object
     */
    public static DigitalMedia makeDigitalMedia(final String encoding)
    {
        final int size = 3;
        final byte[] value = new byte[size];
        for (int i = 0; i < size; i++)
        {
            value[i] = (byte)i;
        }
        return new DigitalMedia(value, encoding);
    }
    
    /**
     * Get product name.
     * 
     * @return String.
     */
    public static String getProductName()
    {
        return "Enter product name here";
    }
    
    /**
     * Get description.
     * 
     * @return String.
     */
    public static String getDescription()
    {
        return "Enter product description here";
    }
    
    /**
     * get Manufacturer.
     * 
     * @return String.
     */
    public static String getManufacturer()
    {
        return "Enter product manufacturer here if known";
    }
    

}
