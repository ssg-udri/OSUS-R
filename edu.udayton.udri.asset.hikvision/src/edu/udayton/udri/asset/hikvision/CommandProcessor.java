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
package edu.udayton.udri.asset.hikvision;

import java.io.IOException;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.core.asset.commands.SetCameraSettingsCommand;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;

/**
 * To Process the Commands.
 * @author Noah
 *
 */
@Component (provide = CommandProcessor.class)
public class CommandProcessor
{        
    /**
     * Getting the current camera settings for Zoom.
     * @param setCameraSettings
     *     Needs a SetCameraSettingsCommand object
     * @return
     *     Returns the int value.
     */
    public int getZoom(final SetCameraSettingsCommand setCameraSettings)
    {
        return (int)(setCameraSettings.getZoom().floatValue() * 1000); //NOCHECKSTYLE - Using a constant.
    }
    
    /**
     * Gets the current Azimuth from the gui.
     * @param setPanTilt
     *     Needs a SetPanTiltCommand object
     * @return
     *     returns the int value
     * @throws IOException
     *     in case of Null
     */    
    public int getAzimuth(final SetPanTiltCommand setPanTilt) throws IOException
    {
        int azimuth = (int)setPanTilt.getPanTilt().getAzimuth().getValue();
        if (setPanTilt.getPanTilt().getAzimuth().getValue() < 0) // Check to see if input is negative
        {
                azimuth = 360 + azimuth; // NOCHECKSTYLE - conversion to match the gui range
        }  
        return azimuth;
    }
    
    /**
     * Gets the current Elevation from the gui.
     * @param setPanTilt
     *     Needs a SetPanTiltCommand
     * @return
     *     return the int value
     * @throws IOException
     *     in case of null
     */
    public int getElevation(final SetPanTiltCommand setPanTilt) throws IOException
    {
        final int elevation = (int)setPanTilt.getPanTilt().getElevation().getValue()
            * 10; // NOCHECKSTYLE - to give the user more range of asset 
        return elevation;  
    }
    
    /**
     * Checks to see if the Azimuth is set.
     * @param setPanTilt
     *     Needs a SetPanTiltCommand
     * @return
     *     True if is it set False if is not set
     */
    public boolean isAzimuthSet(final SetPanTiltCommand setPanTilt)
    {        
        return setPanTilt.getPanTilt().isSetAzimuth();
    }
    
    /**
     * Checks to see if the Elevation is set.
     * @param setPanTilt
     *     Needs a SetPanTiltCommand
     * @return
     *     True if is it set False if is not set 
     */
    public boolean isElevationSet(final SetPanTiltCommand setPanTilt)
    {
        return setPanTilt.getPanTilt().isSetElevation();
    }
}
