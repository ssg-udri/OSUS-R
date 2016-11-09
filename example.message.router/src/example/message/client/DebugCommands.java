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
package example.message.client;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.apache.felix.service.command.Descriptor;

/**
 * Contains debug commands to demonstrate example message client functions.
 * 
 * @author jlatham
 */
@Component(provide = DebugCommands.class, 
    properties = { "osgi.command.scope=mess", 
    "osgi.command.function=connect|disconnect|sendTestMessage|sendGetAssetsMessage|sendCreateAssetMessage"})
public class DebugCommands
{
    private ExampleMessageClient m_MessageClient;
    
    @Reference
    public void setExampleMessageClient(final ExampleMessageClient client)
    {
        m_MessageClient = client;
    }
    
    /* DEBUG COMMANDS */
    
    /**
     * Connects to an ExampleMessageRouter server running on the given address and port.
     * 
     * @param ipAddress
     *      The ip address of the server
     * @param port
     *      The port number for the server
     */
    @Descriptor("Connects to an ExampleMessageRouter server running on the given address and port")
    public void connect(
            @Descriptor("The server's ip address")final String ipAddress, 
            @Descriptor("The server's port")final int port)
    {
        m_MessageClient.connect(ipAddress, port);
    }
    
    /**
     * Disconnect the client from the ExampleMessageRouter server.
     */
    @Descriptor("Disconnects the client from the ExampleMessageRouter server")
    public void disconnect()
    {
        m_MessageClient.disconnect();
    }
    
    /**
     * Connects to an ExampleMessageRouter running on the current clients's configuration ip address and port.
     */
    @Descriptor("Connects to an ExampleMessageRouter server running on the current client configuration's "
            + "ip address and port values.")
    public void connect()
    {
        m_MessageClient.connect(null, null);
    }
    
    /**
     * Sends a test message of type RequestControllerInfo.
     * 
     * @param destId
     *      ID of the system the message should be sent to.
     */
    @Descriptor("Sends a test message of type RequestControllerInfo")
    public void sendTestMessage(
            @Descriptor("ID of the system the message should be sent to.")
            final long destId)
    {
        m_MessageClient.sendTestMessage(destId);
    }
    
    /**
     * Sends a test message of type GetAssetsRequest.
     * 
     * @param destId
     *      ID of the system the message should be sent to.
     */
    @Descriptor("Sends a test message of type GetAssetsRequest")
    public void sendGetAssetsMessage(
            @Descriptor("ID of the system the message should be sent to.")
            final long destId)
    {
        m_MessageClient.sendGetAssetsMessage(destId);
    }
    
    /**
     * Sends a test message of type CreateAssetRequest.
     * 
     * @param destId
     *      ID of the system the message should be sent to.
     */
    @Descriptor("Sends a test message of type CreateAssetRequest")
    public void sendCreateAssetMessage(
            @Descriptor("ID of the system the message should be sent to.")
            final long destId)
    {
        m_MessageClient.sendCreateAssetMessage(destId);
    }
}
