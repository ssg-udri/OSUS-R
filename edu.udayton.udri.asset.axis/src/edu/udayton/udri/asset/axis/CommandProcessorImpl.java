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
package edu.udayton.udri.asset.axis;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.commands.CommandExecutionException;

/**
 * Implementation of the {@link CommandProcessor} interface.
 * @author allenchl
 *
 */
@Component
public class CommandProcessorImpl implements CommandProcessor
{
    /**
     * URL string used for accessing HTTP AXIS commands.
     */
    private static final String AXIS_CGI_PREFIX = "http://%s/axis-cgi/com/ptz.cgi?";

    /**
     * String used for Pan operation with AXIS camera.
     */
    private static final String PAN_COMMAND = "pan=%f";
    
    /**
     * String used for Tilt operation with AXIS camera.
     */
    private static final String TILT_COMMAND = "tilt=%f";
    
    /**
     * String used for an AXIS camera. TD:should be configurable to support multiple instances
     */
    private static final String CAM_ID_SUFFIX = "camera=1";

    /**
     * URL string used for taking still images from an AXIS camera.
     */
    private static final String STILL_IMAGE_URL = "http://%s/jpg/image.jpg";
    
    /**
     * URL string to get the current pan/tilt value from an AXIS camera.
     */
    private static final String GET_PAN_TILT_URL = "http://%s/axis-cgi/com/ptz.cgi?query=position";
    
    /**
     * Command appender.
     */
    private static final String APPENDER = "&";

    @Override
    public String processSetPanTilt(final SetPanTiltCommand command, final String ipAddr)
            throws CommandExecutionException 
    {
        final StringBuilder builder = new StringBuilder(AXIS_CGI_PREFIX);
        
        Double pan = null;
        Double tilt = null;
        
        if (command.getPanTilt().isSetAzimuth())
        {
            pan = command.getPanTilt().getAzimuth().getValue();
        }
        if (command.getPanTilt().isSetElevation())
        {
            tilt = command.getPanTilt().getElevation().getValue();
        }
        
        getUrlString(pan, tilt, builder);

        //return build command
        return String.format(builder.append(CAM_ID_SUFFIX).toString(), ipAddr);
    }

    @Override
    public String processStillImageRequest(final String ipAddr)
    {
        return String.format(STILL_IMAGE_URL, ipAddr);
    }

    @Override
    public String processGetPanTilt(final String ipAddr)
    {
        return String.format(GET_PAN_TILT_URL, ipAddr);
    }
    
    /**
     * Get the appropriately formatted URL string for the desired set pan/tilt action.
     * @param pan
     *      the pan value to set, or <code> null </code> if no pan value should be included in the string
     * @param tilt
     *      the tilt value to set, or <code> null </code> if no tilt value should be included in the string
     * @param builder
     *      the string builder to add to
     * @throws CommandExecutionException
     *      if the data is not complete, or contains illegal values
     */
    private void getUrlString(final Double pan, final Double tilt, final StringBuilder builder) 
            throws CommandExecutionException
    {
        if (pan != null)
        {
            builder.
                append(String.format(PAN_COMMAND, pan)).
                append(APPENDER);
        }
        if (tilt != null)
        {
            builder.
                append(String.format(TILT_COMMAND, tilt)).
                append(APPENDER);
        }
    }
}
