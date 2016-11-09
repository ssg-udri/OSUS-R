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
package edu.udayton.udri.asset.novatel.connection;

import java.io.IOException;

import mil.dod.th.core.asset.AssetException;


/**
 * Service responsible for handling connecting and disconnecting to the serial port needed for the NovAtel asset.
 * 
 * @author cweisenborn
 */
public interface NovatelConnectionMgr
{    
    /**
     * Method used to obtain, open, and start reading from the specified serial port.
     * 
     * @param physPort
     *      Name of the serial port to create a connection with.
     * @param baudRate
     *      Integer that represents the baud rate to set for the serial port.
     * @throws AssetException
     *      if the serial port cannot be created or opened
     * @throws IllegalStateException
     *      if the serial port is already processing data
     */
    void startProcessing(String physPort, int baudRate) throws AssetException, IllegalStateException;
    
    /**
     * Method used to close and release the serial port.
     * @throws IllegalStateException
     *      if the serial port is already processing data
     */
    void stopProcessing() throws IllegalStateException;
    
    /**
     * Method that determine if the NovAtel asset is currently connect.
     * 
     * @return
     *      true if the asset is currently connected and false otherwise
     */
    boolean isProcessing();
    
    /**
     * Method that retrieves the next message from the {@link mil.dod.th.core.ccomm.physical.SerialPort}'s input stream.
     * Returns null if a message is not able to be retrieved from the input stream.
     * 
     * @return
     *      string that represents the data received, or <code>null</code> if a new message is not available
     * @throws AssetException
     *      if there is not a buffered reader to read from
     * @throws IOException 
     *      if the read operation times out after trying for 1000ms
     */
    String readMessage() throws AssetException, IOException;
    
    /**
     * Try reconnecting with the last known properties. This is a single attempt. Callers are responsible for
     * re-occurring calls if the reconnect is unsuccessful.
     * 
     * @throws AssetException
     *      if the serial port cannot be released and/or re-acquired and opened
     * @throws IOException
     *      if the serial port can be opened, but the input stream cannot be read from
     */
    void reconnect() throws AssetException, IOException;
}
