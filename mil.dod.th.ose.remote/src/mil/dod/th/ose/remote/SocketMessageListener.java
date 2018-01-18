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
package mil.dod.th.ose.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.messaging.MessageRouter;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.api.RemoteSettings;

import org.apache.commons.io.HexDump;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Reads in messages from a socket on a thread.  Will pass received message to a {@link MessageRouter} and sync with the
 * {@link mil.dod.th.core.remote.RemoteChannelLookup}.
 * 
 * @author Dave Humeniuk
 *
 */
@Component(factory = SocketMessageListener.FACTORY_NAME)
public class SocketMessageListener implements Runnable
{
    /**
     * Name of the OSGi component factory, used for filtering.
     */
    public final static String FACTORY_NAME = "mil.dod.th.ose.remote.SocketMessageListener";
    
    /**
     * Component property key containing the channel associated with this listener. 
     */
    public static final String CHANNEL_PROP_KEY = "channel";

    /**
     * Component property key containing the socket associated with this listener.
     */
    public static final String SOCKET_PROP_KEY = "socket";
    
    /**
     * Whether the runner should continue.
     */
    private boolean m_Running = true;
    
    /**
     * {@link mil.dod.th.core.system.TerraHarvestSystem} id of the remote system this message listener is listening to.
     */
    private Integer m_RemoteId;
    
    /**
     * Reference to the socket for reading in messages from.
     */
    private Socket m_Socket;
    
    /**
     * Channel used to receive messages by this listener.
     */
    private AbstractSocketChannel m_Channel;

    /**
     * Stream to read in messages from.
     */
    private InputStream m_InputStream;

    /**
     * Routes incoming messages.
     */
    private MessageRouter m_MessageRouter;

    /**
     * Wrapper service for logging.
     */
    private LoggingService m_Logging;
    
    /**
     * Reference to the event admin service.  Used for local messages within event admin service.
     */
    private EventAdmin m_EventAdmin;

    /**
     * Running count of all bytes received for this message listener, not just a single message.
     */
    private int m_AllBytesReceived;

    /**
     * Service contains current settings from config admin.
     */
    private RemoteSettings m_RemoteSettings;
    
    /**
     * Bind a message route to handler message read in from socket.
     * 
     * @param messageRouter
     *      router to handle incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouter messageRouter)
    {
        m_MessageRouter = messageRouter;
    }
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Logging = logging;
    }
    
    /**
     * Bind the event admin service.
     * 
     * @param eventAdmin
     *      service used to post events
     */
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Bind the settings for the remote interface.
     * 
     * @param remoteSettings
     *      interface containing remote settings from config admin
     */
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    /**
     * Activate this runner.
     * 
     * @param props
     *      properties of the component including the "socket" to listen on
     * @throws IOException
     *      if unable to get socket stream
     */
    @Activate
    public void activate(final Map<String, Object> props) throws IOException
    {
        m_Socket = (Socket)props.get(SOCKET_PROP_KEY);
        m_Channel = (AbstractSocketChannel)props.get(CHANNEL_PROP_KEY);
        m_InputStream = m_Socket.getInputStream();
    }
    
    /**
     * Stop listening to incoming messages.
     */
    @Deactivate
    public void deactivate()
    {
        m_Running = false;
    }
    
    @Override
    public void run()
    {
        // continuously read messages from socket
        try
        {
            while (m_Running)
            {
                byte[] messageBuffer = null;
                try
                {
                    messageBuffer = readMessage();
                }
                catch (final RemoteInterfaceException rse)
                {                    
                    m_Logging.error("Error reading message: %s", rse.getMessage());                   
                    return;
                }
                
                if (messageBuffer == null)
                {
                    // if buffer is null, issue with socket so quit thread
                    return;
                }
                
                TerraHarvestMessage message = null;
                try
                {
                    message = TerraHarvestMessage.parseFrom(messageBuffer);
                }
                catch (final InvalidProtocolBufferException e)
                {
                    // if failure occurs, then sender sent bad data, continue running to accept further messages
                    m_Logging.warning("Tried to parse message of size %d from socket %s: %s", messageBuffer.length, 
                            m_Socket.getRemoteSocketAddress(), e.getMessage());
                    m_Logging.debug(hexDump(messageBuffer));
                }
                
                if (message != null)
                {
                    if (m_RemoteSettings.isLogRemoteMessagesEnabled())
                    {
                        m_Logging.debug("Socket %s received remote message%n%s", m_Socket.getRemoteSocketAddress(), 
                            message);
                    }
                    
                    if (m_RemoteId == null || m_RemoteId == Integer.MAX_VALUE)
                    {
                        m_RemoteId = message.getSourceId();
                        final Map<String, Object> props = new HashMap<String, Object>();
                        props.put(RemoteConstants.EVENT_PROP_SYS_ID, message.getSourceId());
                        props.put(RemoteConstants.EVENT_PROP_CHANNEL, m_Channel);
                        final Event event = new Event(RemoteConstants.TOPIC_NEW_OR_CHANGED_CHANNEL_ID, props);
                        m_EventAdmin.sendEvent(event);
                    }
                    
                    // send message on to router if parsed
                    m_MessageRouter.handleMessage(message, m_Channel);
                }
            }
        }
        finally
        {
            //do thread cleanup
            final Map<String, Object> props = new HashMap<String, Object>();
            props.put(RemoteConstants.EVENT_PROP_CHANNEL, m_Channel);
            final Event event = new Event(RemoteConstants.TOPIC_REMOVE_CHANNEL, props);
            m_EventAdmin.postEvent(event);
            
            // try to close input stream and socket, ignoring all exceptions to ensure both are closed
            try
            {
                m_InputStream.close();
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to close input stream");
            }
            
            try
            {
                m_Socket.close();
            }
            catch (final Exception e)
            {
                m_Logging.error(e, "Unable to close socket");
            }
        }
        
        m_Logging.debug("Socket listener thread completed");
    }

    /**
     * Read a single {@link TerraHarvestMessage} from the input stream with an octet count of the message size before
     * the message.
     * 
     * @return
     *      array containing the {@link TerraHarvestMessage} (does not include the octet count) or null if no message
     * @throws RemoteInterfaceException An error with the size of the message.
     */
    private byte[] readMessage() throws RemoteInterfaceException
    {
        final byte[] messageBuffer;
        try
        {
            // first read size of message coming in
            final int firstByte;
            try
            {
                firstByte = m_InputStream.read();
            }
            catch (final SocketException e)
            {
                // socket has been closed, just return so the thread can exit
                return null;
            }
                        
            if (firstByte == -1)
            {
                // reached EOF
                m_Logging.info("Socket %s closed, will stop reading from it", 
                        m_Socket.getRemoteSocketAddress());
                return null;
            }
            final int messageSize = CodedInputStream.readRawVarint32(firstByte, m_InputStream);
            
            if (messageSize > m_RemoteSettings.getMaxMessageSize())
            {
                throw new RemoteInterfaceException(
                        String.format("Message over Max Size: %s > %s", 
                                messageSize, m_RemoteSettings.getMaxMessageSize()));
            }
            
            if (m_RemoteSettings.isLogRemoteMessagesEnabled())
            {
                m_Logging.debug("New message of size %d for socket %s", messageSize,
                    m_Socket.getRemoteSocketAddress());
            }
            
            // now read in message
            messageBuffer = new byte[messageSize];
            int bytesRemaining = messageSize;
            while (bytesRemaining > 0)
            {
                final int bytesRead = m_InputStream.read(messageBuffer, messageSize - bytesRemaining, 
                        bytesRemaining);
                if (bytesRead == -1)
                {
                    // reached EOF
                    m_Logging.error("Socket %s closed while reading message, will stop reading from it", 
                            m_Socket.getRemoteSocketAddress());
                    return null;
                }
                m_AllBytesReceived += bytesRead;
                m_Channel.setBytesReceived(m_AllBytesReceived);
                bytesRemaining -= bytesRead;
            }
            
        }
        catch (final IOException e)
        {
            m_Logging.error(e, "Failed to read message from the input stream for socket %s", 
                    m_Socket.getRemoteSocketAddress());
            return null;
        }
        return messageBuffer;
    }

    /**
     * Create a string containing the hex dump of a given byte array.
     * 
     * @param array
     *      array of bytes to dump
     * @return
     *      string representation of the binary data
     */
    private String hexDump(final byte[] array)
    {
        if (array.length == 0)
        {
            return "(empty array)";
        }
        
        final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        try
        {
            HexDump.dump(array, 0, outStream, 0);
        }
        catch (final IOException e) 
        {
            throw new IllegalStateException(e);
        }
        
        return new String(outStream.toByteArray()).replace("%", "%%");
    }
}
