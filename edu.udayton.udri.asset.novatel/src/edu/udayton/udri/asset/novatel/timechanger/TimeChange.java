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
package edu.udayton.udri.asset.novatel.timechanger;

import java.io.IOException;

import edu.udayton.udri.asset.novatel.StatusHandler;

import mil.dod.th.core.asset.AssetException;
import mil.dod.th.core.types.status.ComponentStatus;

/**
 * Interface for sending time in milliseconds to a system service that sets the system 
 * time to the specified time. Sending is performed on a separate thread.
 * 
 * @author nickmarcucci
 *
 */
public interface TimeChange
{
    /**
     * Create a connection to an existing time service which syncs the system's
     * time to a desired time.
     * 
     * @param handler
     *  the {@link StatusHandler} which will respond to changes in this service's operational status
     * @param port
     *  the port over which the time service communicates
     * @throws IllegalStateException
     *  if the time service has already been connected to and is currently still running
     */
    void connectTimeService(StatusHandler handler, int port) throws IllegalStateException;
    
    /**
     * Disconnects connections to the time service and stops processing.
     * @throws IOException
     *  if an error occurs closing the client socket or output stream
     * @throws IllegalStateException
     *  if the time service is not currently running and this method is called
     * @throws InterruptedException
     *  if the thread that is running the service cannot be terminated in the specified
     *  timeout.
     */
    void disconnectTimeService() throws IOException, IllegalStateException, InterruptedException;
    
    /**
     * Adds the current time in milliseconds to a queue to be sent to the 
     * time service. The time is only recorded if the current time in milliseconds
     * is greater than a second difference with the current system time.
     * @param milliseconds
     *  the time that is desired to be set
     * @throws AssetException 
     *  if a time cannot be sent over a socket
     *  
     */
    void changeTime(long milliseconds) throws AssetException;
    
    /**
     * Get the current status of this component.
     * @return
     *      the current status of this component
     */
    ComponentStatus getComponentStatus();
}
