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

import mil.dod.th.core.asset.commands.SetPanTiltCommand;
import mil.dod.th.core.commands.CommandExecutionException;

/**
 * This class processes the supported commands for the {@link edu.udayton.udri.asset.axis.AxisAsset}.
 * @author allenchl
 *
 */
public interface CommandProcessor
{
    /**
     * Process a command asking to adjust the pan or tilt aspects of the cameras orientation.
     * @param command
     *      the command containing the pan and/or tilt data
     * @param ipAddr
     *      the IP address to which the command will ultimately be sent
     * @return
     *      string representing a URL to send to perform the desired action
     * @throws CommandExecutionException
     *      if the data is not complete, or contains illegal values
     */
    String processSetPanTilt(final SetPanTiltCommand command, final String ipAddr) 
            throws CommandExecutionException;
    
    /**
     * Process still image request.
     * @param ipAddr
     *      the IP address to which the command will ultimately be sent
     * @return
     *      string representing a URL to send to perform the desired action
     */
    String processStillImageRequest(final String ipAddr);
    
    /**
     * Process a command requesting the current pan/tilt aspects of the cameras orientation.
     * @param ipAddr
     *      the IP address to which the command will ultimately be sent
     * @return
     *      string representing a URL to send to perform the desired action
     */
    String processGetPanTilt(final String ipAddr);
}
