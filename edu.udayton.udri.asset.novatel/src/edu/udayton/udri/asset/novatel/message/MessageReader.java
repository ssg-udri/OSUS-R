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
package edu.udayton.udri.asset.novatel.message;

/**
 * This interface allows the latest message of interest to be retrieved, and calls the {@link MessageReceiver}
 * when a data message is due to be handled.
 * 
 * @author allenchl
 *
 */
public interface MessageReader
{
    /**
     * Start the message read thread. Every message will be requested to be handled by the 
     * {@link MessageReceiver#handleDataString(String)}.
     * @param receiver
     *      the receiver which is called when data messages need to be handled
     * @throws IllegalStateException
     *      if retrieving has already started
     */
    void startRetreiving(MessageReceiver receiver) throws IllegalStateException;
    
    /**
     * Stop the retrieval of data messages.
     * @throws IllegalStateException
     *      if retrieving has already stopped
     */
    void stopRetrieving() throws IllegalStateException;
}
