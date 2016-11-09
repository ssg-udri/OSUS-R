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
package mil.dod.th.ose.remote.integration;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.DataStreamServiceMessages.DataStreamServiceNamespace;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionInfoNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.EventMessages.UnregisterEventRequestData;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.RemoteChannelLookupMessages.RemoteChannelLookupNamespace;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;
import mil.dod.th.ose.remote.integration.MessageMatchers.BasicMessageMatcher;
import mil.dod.th.ose.remote.integration.MessageMatchers.EventMessageMatcher;
import mil.dod.th.ose.remote.integration.MessageMatchers.MessageMatcher;
import mil.dod.th.ose.remote.integration.namespace.TestEventAdminNamespace;


/**
 * This class is designed to listen for multiple messages expected to be received by a TerraHarvest 
 * system during testing. Class contains methods that listen for multiple messages and separates 
 * received messages based on expected namespace type.
 * @author bachmakm
 *
 */
public class MessageListener 
{
    /**
     * Constant for establishing the timeout time for a socket.  
     */
    public static final int SOCK_TIMEOUT = 200;
    
    /**
     * Input stream of socket from which messages are being received.  
     */
    private InputStream m_InputStream;   

    /**
     * Socket responsible for sending and receiving messages. 
     */
    private Socket m_Socket;  

    /**
     * List of all messages received from input stream.
     */
    private List<MessageDetails> m_AllResponses = new ArrayList<MessageDetails>();
    
    /**
     * List of matching messages received.
     */
    private List<MessageDetails> m_Matches = new ArrayList<MessageDetails>();
    
    /**
     * Used to sync between the wait call and the message reader thread.
     */
    private Semaphore m_WaitSem;
    
    /**
     * Constructor that initializes the socket input stream and list of messages to be received.
     * @param socket
     *      socket that is sending/receiving messages.  Timeout of socket is changed to value of
     *      SOCK_TIMEOUT (default is 200 milliseconds) when a new listening thread is created for receiving messages.  
     * @throws IOException 
     *      if cannot get input stream due to socket issues
     */
    public MessageListener(Socket socket) throws IOException
    {
        m_InputStream = socket.getInputStream();
        m_Socket = socket;
        m_Socket.setSoTimeout(SOCK_TIMEOUT);
    }
    
    /**
     * Wait for the first occurrence of the given message.
     * 
     * @param expectedNamespace
     *      wait for a message from this namespace
     * @param expectedType
     *      wait for a message of this type
     * @param timeoutMS
     *      max time to wait for the message
     */
    public Message waitForMessage(Namespace expectedNamespace, ProtocolMessageEnum expectedType, 
            int timeoutMS)
    {
        return waitForMessages(expectedNamespace, expectedType, timeoutMS, MatchCount.atLeastOnce()).
                get(0).namespaceMessage;
    }
    
    /**
     * Wait for the first occurrence of the given message.
     * 
     * @param expectedNamespace
     *      wait for a message from this namespace
     * @param expectedType
     *      wait for a message of this type
     * @param timeoutMS
     *      max time to wait for the message
     * @param matchCount
     *      how many messages to wait for
     */
    public List<MessageDetails> waitForMessages(Namespace expectedNamespace, ProtocolMessageEnum expectedType, 
            int timeoutMS, MatchCount matchCount)
    {
        return waitForMessages(timeoutMS, new BasicMessageMatcher(expectedNamespace, expectedType, matchCount));
    }
    
    /**
     * Wait for the first occurrence of the given message.
     * 
     * @param topic
     *      event topic to wait for
     * @param timeoutMS
     *      max time to wait for the message
     */
    public Message waitForRemoteEvent(final String topic, int timeoutMS) 
        throws InterruptedException
    {
        return waitForRemoteEvents(topic, timeoutMS, MatchCount.atLeastOnce()).get(0).namespaceMessage;
    }
    
    /**
     * Wait for the first occurrence of the given message.
     * 
     * @param topic
     *      event topic to wait for
     * @param timeoutMS
     *      max time to wait for the message
     * @param matchCount
     *      how many messages to wait for
     */
    public List<MessageDetails> waitForRemoteEvents(final String topic, int timeoutMS, MatchCount matchCount) 
        throws InterruptedException
    {
        return waitForMessages(timeoutMS, new EventMessageMatcher(topic, matchCount));
    }
    
    /**
     * Wait for the first occurrence of each given message in any order.
     * 
     * @param timeoutMS
     *      max time to wait for the message
     * @param matchers
     *      matchers that can be used to find a sequence of message.
     * @return
     *      details on the messages that were being waited for, messages that don't match won't be returned
     */
    public List<MessageDetails> waitForMessages(int timeoutMS, MessageMatcher ...matchers)
    {
        return waitForMessages(timeoutMS, true, matchers);
    }
    
    /**
     * Wait for the first occurrence of each given message in any order.
     * 
     * @param timeoutMS
     *      max time to wait for the message
     * @param assertMatches
     *      true if the responses should be checked against the given matchers list
     * @param matchers
     *      matchers that can be used to find a sequence of message.
     * @return
     *      details on the messages that were being waited for, messages that don't match won't be returned
     */
    public List<MessageDetails> waitForMessages(int timeoutMS, boolean assertMatches, MessageMatcher ...matchers)
    {
        m_AllResponses.clear();
        m_Matches.clear();
        m_WaitSem = new Semaphore(0);
        
        MessageReader messageReader = new MessageReader(m_WaitSem, matchers);
        Thread thread = new Thread(messageReader);
        thread.start();
        
        try
        {
            m_WaitSem.tryAcquire(timeoutMS, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        
        messageReader.stop();
        try
        {
            thread.join(1000);
        }
        catch (InterruptedException e)
        {
            throw new IllegalStateException(e);
        }
        
        if (assertMatches)
        {
            for (MessageMatcher matcher : matchers)
            {
                matcher.assertMatch(m_AllResponses);
            }
            
            assertThat("Message reader thread did not stop", thread.isAlive(), is(false));
        }
        
        return m_Matches;
    }
    
    /**
     * Method used to unregister for registered events. 
     * @param regId
     *      registration ID of registered event.
     * @param socket
     *      socket used to send unregister event request
     * @throws IOException 
     *      if cannot get output stream due to socket issues
     */
    public static void unregisterEvent(int regId, Socket socket) throws IOException
    {
        MessageListener listener = new MessageListener(socket); 
        
        //clean up registered events
        UnregisterEventRequestData unreg = UnregisterEventRequestData.newBuilder().setId(regId).build();
        TerraHarvestMessage message = TestEventAdminNamespace.createEventAdminMessage(
                EventAdminMessageType.UnregisterEventRequest, unreg);
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        //listen for potential messages as a result of new request
        //assert unregister event response was received
        listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.UnregisterEventResponse, 1500);
    }

    /**
     * Method used to clean up registrations after a test class runs.
     * @param socket
     *      socket used to send cleanup request
     * @throws IOException 
     *      if cannot get input stream due to socket issues
     */
    public static void unregisterEvent(Socket socket) throws IOException
    {
        //clean up registered events
        TerraHarvestMessage message = TestEventAdminNamespace.createEventAdminMessage(
                EventAdminMessageType.CleanupRequest, null);
        // send out message
        message.writeDelimitedTo(socket.getOutputStream());
        
        MessageListener listener = new MessageListener(socket); 
        listener.waitForMessages(4500, false, 
                new BasicMessageMatcher(Namespace.EventAdmin, EventAdminMessageType.CleanupResponse)); 
    }

    /**
     * Get the list of all responses captured while listening for messages.
     * @return
     *     the list of messages that were captured
     */
    public List<MessageDetails> getCapturedTerraHarvestMessages()
    {
        return m_AllResponses;
    }
    
    public class MessageDetails
    {
        protected Message namespaceMessage;
        protected ProtocolMessageEnum messageType;
        protected TerraHarvestPayload payload;
        protected TerraHarvestMessage message;
        
        public Message getNamespaceMessage()
        {
            return namespaceMessage;
        }
        
        public TerraHarvestMessage getMessage()
        {
            return message;
        }
    }
    
    private MessageDetails getMessageDetails(TerraHarvestMessage response) throws InvalidProtocolBufferException
    {
        MessageDetails details = new MessageDetails();
        details.message = response;
        
        // Check to see if message is encrypted. If so then attempt to decrypt it
        // otherwise just parse the payload.
        if (response.getEncryptType().getNumber() > EncryptType.NONE.getNumber())
        {
            try
            {
                details.payload = EncryptionUtil.decryptECDHMessage(response);
            }
            catch (Exception exception)
            {
                throw new IllegalStateException(exception);
            }
        }
        else
        {
            details.payload = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());
        }
        
        switch (details.payload.getNamespace())
        {
            case Base:
                BaseNamespace baseMessage = BaseNamespace.parseFrom(details.payload.getNamespaceMessage()); 
                details.namespaceMessage = baseMessage;
                details.messageType = baseMessage.getType();
                break;

            case EventAdmin:
                EventAdminNamespace eventMessage = EventAdminNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = eventMessage;
                details.messageType = eventMessage.getType();
                break;

            case ConfigAdmin:
                ConfigAdminNamespace configMessage = 
                    ConfigAdminNamespace.parseFrom(details.payload.getNamespaceMessage()); 
                details.namespaceMessage = configMessage;
                details.messageType = configMessage.getType();
                break;

            case MetaType:
                MetaTypeNamespace metaTypeMessage = MetaTypeNamespace.parseFrom(details.payload.getNamespaceMessage()); 
                details.namespaceMessage = metaTypeMessage;
                details.messageType = metaTypeMessage.getType();
                break;

            case Bundle:
                BundleNamespace bundleMessage = BundleNamespace.parseFrom(details.payload.getNamespaceMessage()); 
                details.namespaceMessage = bundleMessage;
                details.messageType = bundleMessage.getType();
                break;

            case MissionProgramming:
                MissionProgrammingNamespace mpMessage = MissionProgrammingNamespace.
                    parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = mpMessage;
                details.messageType = mpMessage.getType();
                break;
                
            case Asset:
                AssetNamespace assetMessage = AssetNamespace.parseFrom(details.payload.getNamespaceMessage()); 
                details.namespaceMessage = assetMessage;
                details.messageType = assetMessage.getType();
                break; 

            case AssetDirectoryService:
                AssetDirectoryServiceNamespace adsMessage = 
                    AssetDirectoryServiceNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = adsMessage;
                details.messageType = adsMessage.getType();
                break;
            
            case DataStreamService:
                DataStreamServiceNamespace dssMessage =
                    DataStreamServiceNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = dssMessage;
                details.messageType = dssMessage.getType();
                break;
                
            case DataStreamStore:
                DataStreamStoreNamespace dsStoreMessage =
                    DataStreamStoreNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = dsStoreMessage;
                details.messageType = dsStoreMessage.getType();
                break;
                
            case CustomComms:
                CustomCommsNamespace commsMessage = 
                    CustomCommsNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = commsMessage;
                details.messageType = commsMessage.getType();
                break;
                
            case PhysicalLink:
                PhysicalLinkNamespace pLinkMessage = 
                    PhysicalLinkNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = pLinkMessage;
                details.messageType = pLinkMessage.getType();
                break;
            
            case LinkLayer:
                LinkLayerNamespace lLayerMessage = LinkLayerNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = lLayerMessage;
                details.messageType = lLayerMessage.getType();
                break;
            
            case TransportLayer:
                TransportLayerNamespace tLayerMessage = 
                    TransportLayerNamespace.parseFrom(details.payload.getNamespaceMessage());
                details.namespaceMessage = tLayerMessage;
                details.messageType = tLayerMessage.getType();
                break;
           
            case ObservationStore:
                ObservationStoreNamespace obsMessage = ObservationStoreNamespace.parseFrom(
                        details.payload.getNamespaceMessage());
                details.namespaceMessage = obsMessage;
                details.messageType = obsMessage.getType();
                break;
            
            case RemoteChannelLookup:
                RemoteChannelLookupNamespace remoteChannelMessage = RemoteChannelLookupNamespace.parseFrom(
                        details.payload.getNamespaceMessage());
                details.namespaceMessage = remoteChannelMessage;
                details.messageType = remoteChannelMessage.getType();
                break;
            
            case EncryptionInfo:
                EncryptionInfoNamespace encryptionInfoMessage = EncryptionInfoNamespace.parseFrom(
                        details.payload.getNamespaceMessage());
                details.namespaceMessage = encryptionInfoMessage;
                details.messageType = encryptionInfoMessage.getType();
                break;
                
            default:
                throw new UnsupportedOperationException("Cannot complete operation: " + details.payload.getNamespace() 
                        + " namespace has no implementation.");
        }
        
        return details;
    }

    /**
     * Inner class responsible for reading messages and releasing semaphore each time the proper message is received.
     */
    private class MessageReader implements Runnable
    {
        /**
         * Flag used for stopping a thread when a timeout has occurred. 
         */
        private boolean m_Running = true;
        
        final private Semaphore m_WaitSem;
        final private MessageMatcher[] m_Matchers;

        MessageReader(Semaphore waitSem, MessageMatcher[] matchers)
        {
            m_WaitSem = waitSem;
            m_Matchers = matchers;
        }

        /**
         * Method for effectively stopping the execution of the created thread.  
         */
        public void stop()
        {
            m_Running = false;
        }

        @Override
        public void run()
        { 
            while(m_Running)
            {
                try
                {
                    TerraHarvestMessage response = TerraHarvestMessage.parseDelimitedFrom(m_InputStream);
                    MessageDetails details = getMessageDetails(response);
                    m_AllResponses.add(details);
                    
                    // check for a match
                    boolean foundMissingMatch = false;
                    for (MessageMatcher matcher : m_Matchers)
                    {
                        if (matcher.match(details))
                        {
                            m_Matches.add(details);
                        }
                        if (!matcher.hasMatched())
                        {
                            foundMissingMatch = true;
                        }
                    }
                    
                    if (!foundMissingMatch)
                    {
                        m_WaitSem.release();
                    }
                }
                catch(InvalidProtocolBufferException e)
                {
                    //Protobuf 2.6.1 doesn't throw IO exceptions when parsing messages. Instead it catches all
                    //IOExceptions and creates a ProtocolBufferException with the exception messages from the 
                    //IOException. See line 230 of the protobuf AbstractParser class.
                    if (!e.getMessage().equals("Read timed out"))
                    {
                        e.printStackTrace();
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}