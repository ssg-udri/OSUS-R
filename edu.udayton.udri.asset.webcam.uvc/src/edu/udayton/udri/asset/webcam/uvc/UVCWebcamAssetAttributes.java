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
package edu.udayton.udri.asset.webcam.uvc;

import java.awt.Dimension;

import aQute.bnd.annotation.metatype.Meta.AD;
import aQute.bnd.annotation.metatype.Meta.OCD;
import mil.dod.th.core.ConfigurationConstants;
import mil.dod.th.core.asset.AssetAttributes;

/**
 * Interface which defines the configurable properties for UVC Webcam asset.
 */
@OCD (description = ConfigurationConstants.PARTIAL_OBJECT_CLASS_DEFINITION)
public interface UVCWebcamAssetAttributes extends AssetAttributes
{
    /**
     * Configuration property for the resolution of the webcam.
     * @return the dimensions of the resolutions
     */
    @AD (required = true, 
            name = "Resolution", 
            description = "The resolution to capture images at. Check the README for information on resolutions. ")
    Resolution resolution();
    
    /**
     * An enumeration of the available resolutions.
     */
    enum Resolution
    {
        /**
         * All available resolution names and their dimensions.
         */
        
        /** QQVGA resolution. */
        QQVGA(176, 144),
        
        /** QVGA resolution. */
        QVGA(320, 240),
        
        /** CIF resolution. */
        CIF(352, 288),
        
        /** HVGA resolution. */
        HVGA(480, 400),
        
        /** VGA resolution. */
        VGA(640, 480),
        
        /** PAL resolution. */
        PAL(768, 576),
        
        /** SVGA resolution. */
        SVGA(800, 600),
        
        /** HD720 resolution. */
        HD720(1280, 720),
        
        /** WXGA resolution. */
        WXGA(1280, 768),
        
        /** SXVGA resolution. */
        SXGA(1280, 1024),
        
        /** UXGA resolution. */
        UXGA(1600, 1200),
        
        /** HD1080 resolution. */
        HD1080(1920, 1080),
        
        /** QXGA resolution. */
        QXGA(2048, 1536);
        
        /**
         * The variable size holds the dimensions of each resolution.
         */
        private Dimension m_Size;

        /**
         * Constructor for a resolution.
         * 
         * @param width the resolution width
         * @param height the resolution height
         */
        Resolution(final int width, final int height) 
        {
            this.m_Size = new Dimension(width, height);
        }

        /**
         * Get resolution size.
         * 
         * @return Dimension object
         */
        public Dimension getSize() 
        {
            return m_Size;
        }
        
    }
}
