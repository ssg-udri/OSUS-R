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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;

import static org.junit.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.LinkLayerMessages.LinkLayerNamespace;
import mil.dod.th.core.remote.proto.MetaTypeMessages.MetaTypeNamespace;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace;
import mil.dod.th.core.remote.proto.PhysicalLinkMessages.PhysicalLinkNamespace;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.TransportLayerMessages.TransportLayerNamespace;

/**
 * This class is designed to listen for multiple messages expected to be received by a TerraHarvest 
 * system during testing from two sockets. Class contains methods that listen for multiple messages and separates 
 * received messages based on expected namespace type.
 * @author frenchpd
 *
 */
public class MessageDualListener 
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
     * Input stream of an optional additional socket from which messages are being received.  
     */
    private InputStream m_AdditionalInputStream;   

    /**
     * Additional socket that is sending and receiving messages. 
     */
    private Socket m_AdditionalSocket;

    /**
     * List of messages received from input stream.
     */
    private List<TerraHarvestMessage> m_Responses;
    
    /**
     * List of messages received from the additional input stream.
     */
    private List<TerraHarvestMessage> m_AdditionalResponses;

    /**
     * Constructor that initializes the socket input streams and lists of messages to be received.
     * @param socket
     *      socket that is sending/receiving messages.  Timeout of socket is changed to value of
     *      SOCK_TIMEOUT (default is 200 milliseconds) when a new listening thread is created for receiving messages.  
     * @param additionalSocket
     *      additional socket that is sending/receiving messages in parallel with the other socket.  
     *      Timeout of socket is changed to value of
     *      SOCK_TIMEOUT (default is 200 milliseconds) when a new listening thread is created for receiving messages. 
     * @throws IOException 
     *      if cannot get input stream due to socket issues
     */
    public MessageDualListener(Socket socket, Socket additionalSocket) throws IOException
    {
        m_InputStream = socket.getInputStream();
        m_AdditionalInputStream = additionalSocket.getInputStream();
        m_Socket = socket;
        m_AdditionalSocket = additionalSocket;
        m_Responses = new ArrayList<TerraHarvestMessage>();
        m_AdditionalResponses = new ArrayList<TerraHarvestMessage>();
    }
    
    /**
     * Method starts message listening thread and stops the thread after a specific amount of time. 
     * @param timeoutMS
     *      Maximum amount of time to wait (in milliseconds) for the listening thread to finish listening 
     *      for incoming messages.  The listening thread will be terminated in the event that execution
     *      cannot be completed in the allotted time.  
     */
    public void waitForMessages(int timeoutMS)
    {       
        m_Responses.clear();
        MessageThread messageThread = new MessageThread();
        try
        {
            Thread myThread = new Thread(messageThread);
            myThread.start();
            Thread.sleep(timeoutMS);
            messageThread.stop();
            myThread.join(SOCK_TIMEOUT+200); //give time for message thread to finish stopping
            assertThat(myThread.isAlive(), is(false));            
        }
        catch (InterruptedException e)
        {
            fail("Thread was interrupted while waiting for messages...\n" + e.getMessage());
        }
    }
    
    /**
     * Method starts message listening thread for two sockets that must be specified in the constructor of this
     * listener instance, and stops the thread after a specific amount of time. 
     * @param timeoutMS
     *      Maximum amount of time to wait (in milliseconds) for the listening threads to finish listening 
     *      for incoming messages.  The listening threads will be terminated in the event that execution
     *      cannot be completed in the allotted time.  
     */
    public void waitForMessagesTwoSockets(int timeoutMS)
    {       
        m_Responses.clear();
        m_AdditionalResponses.clear();
        AdditionalMessageThread additionalMessageThread = new AdditionalMessageThread();
        try
        {
            Thread messageThread = new Thread(additionalMessageThread);
            messageThread.start();
            Thread.sleep(timeoutMS);
            additionalMessageThread.stop();
            messageThread.join(SOCK_TIMEOUT+200); //give time for message thread to finish stopping
            assertThat(messageThread.isAlive(), is(false));
        }
        catch (InterruptedException e)
        {
            fail("Thread was interrupted while waiting for messages...\n" + e.getMessage());
        }
    }

    /**
     * Method asserts that a response of a specific message type has been received from the second socket.  
     * In addition, the method returns an array list of messages that match the input type.  
     * This is in case any further assertions need to be made on data appended to the message.
     * @param expectedNamespace
     *      Namespace of remote interface message type. 
     * @param expectedType
     *      Specific type of message expected to be received.
     * @return
     *      a list of messages matching the input message type.
     * @throws InvalidProtocolBufferException 
     *      if message cannot be properly parsed
     */
    public List<Message> assertMessagesReceivedByTypeSocketTwo(
            Namespace expectedNamespace, ProtocolMessageEnum expectedType) 
        throws InvalidProtocolBufferException
    {
        return assertMessagesReceivedByType(expectedNamespace, expectedType, true);
    }

    /**
     * Method asserts that a response of a specific message type has been received from first socket.  
     * In addition, the method returns an array list of messages that match the input type.  
     * This is in case any further assertions need to be made on data appended to the message.
     * @param expectedNamespace
     *      Namespace of remote interface message type. 
     * @param expectedType
     *      Specific type of message expected to be received.
     * @return
     *      a list of messages matching the input message type.
     * @throws InvalidProtocolBufferException 
     *      if message cannot be properly parsed
     */
    public List<Message> assertMessagesReceivedByTypeSocketOne(
            Namespace expectedNamespace, ProtocolMessageEnum expectedType) 
        throws InvalidProtocolBufferException
    {
        return assertMessagesReceivedByType(expectedNamespace, expectedType, false);
    }
    
    /**
     * Method asserts that a response of a specific message type has been received.  In addition, the method 
     * returns an array list of messages that match the input type.  This is in case
     * any further assertions need to be made on data appended to the message.
     * @param expectedNamespace
     *      Namespace of remote interface message type. 
     * @param expectedType
     *      Specific type of message expected to be received.
     * @param additionalSocket
     *      True if this request is to check the additional socket responses
     * @return
     *      a list of messages matching the input message type.
     * @throws InvalidProtocolBufferException 
     *      if message cannot be properly parsed
     */
    public List<Message> assertMessagesReceivedByType(Namespace expectedNamespace, ProtocolMessageEnum expectedType, 
        boolean additionalSocket) 
        throws InvalidProtocolBufferException
    {
        List<String> messageTypes = new ArrayList<String>();
        List<Message> namespaceMessages = new ArrayList<Message>();
        List<TerraHarvestMessage> responses = additionalSocket ? m_AdditionalResponses : m_Responses;
        for (TerraHarvestMessage response : responses)
        {
            TerraHarvestPayload payload = TerraHarvestPayload.parseFrom(response.getTerraHarvestPayload());

            Message namespaceMessage;
            ProtocolMessageEnum messageType;
            
            switch (payload.getNamespace())
            {
                case Base:
                    BaseNamespace baseMessage = BaseNamespace.parseFrom(payload.getNamespaceMessage()); 
                    namespaceMessage = baseMessage;
                    messageType = baseMessage.getType();
                    break;

                case EventAdmin:
                    EventAdminNamespace eventMessage = EventAdminNamespace.parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = eventMessage;
                    messageType = eventMessage.getType();
                    break;

                case ConfigAdmin:
                    ConfigAdminNamespace configMessage = ConfigAdminNamespace.parseFrom(payload.getNamespaceMessage()); 
                    namespaceMessage = configMessage;
                    messageType = configMessage.getType();
                    break;

                case MetaType:
                    MetaTypeNamespace metaTypeMessage = MetaTypeNamespace.parseFrom(payload.getNamespaceMessage()); 
                    namespaceMessage = metaTypeMessage;
                    messageType = metaTypeMessage.getType();
                    break;

                case Bundle:
                    BundleNamespace bundleMessage = BundleNamespace.parseFrom(payload.getNamespaceMessage()); 
                    namespaceMessage = bundleMessage;
                    messageType = bundleMessage.getType();
                    break;

                case MissionProgramming:
                    MissionProgrammingNamespace mpMessage = MissionProgrammingNamespace.
                        parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = mpMessage;
                    messageType = mpMessage.getType();
                    break;
                    
                case Asset:
                    AssetNamespace assetMessage = AssetNamespace.parseFrom(payload.getNamespaceMessage()); 
                    namespaceMessage = assetMessage;
                    messageType = assetMessage.getType();
                    break; 

                case AssetDirectoryService:
                    AssetDirectoryServiceNamespace adsMessage = 
                        AssetDirectoryServiceNamespace.parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = adsMessage;
                    messageType = adsMessage.getType();
                    break;
                
                case CustomComms:
                    CustomCommsNamespace commsMessage = CustomCommsNamespace.parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = commsMessage;
                    messageType = commsMessage.getType();
                    break;
                    
                case PhysicalLink:
                    PhysicalLinkNamespace pLinkMessage = PhysicalLinkNamespace.parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = pLinkMessage;
                    messageType = pLinkMessage.getType();
                    break;
                
                case LinkLayer:
                    LinkLayerNamespace lLayerMessage = LinkLayerNamespace.parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = lLayerMessage;
                    messageType = lLayerMessage.getType();
                    break;
                
                case TransportLayer:
                    TransportLayerNamespace tLayerMessage = 
                        TransportLayerNamespace.parseFrom(payload.getNamespaceMessage());
                    namespaceMessage = tLayerMessage;
                    messageType = tLayerMessage.getType();
                    break;
               
                case ObservationStore:
                    ObservationStoreNamespace obsMessage = ObservationStoreNamespace.parseFrom(
                            payload.getNamespaceMessage());
                    namespaceMessage = obsMessage;
                    messageType = obsMessage.getType();
                    break;
                
                default:
                    throw new UnsupportedOperationException("Cannot complete operation: " + payload.getNamespace() 
                            + " namespace has no implementation.");
            }
                
            namespaceMessages.add(namespaceMessage);
            messageTypes.add(payload.getNamespace() + ":" + messageType);
        }

        assertThat("Expected message type is received", messageTypes, hasItem(expectedNamespace + ":" + expectedType));
        List<Message> messageMatch = new ArrayList<Message>();
        for(int i = 0; i < messageTypes.size(); i++)
        {
            if(messageTypes.get(i).equals(expectedNamespace + ":" + expectedType))
            {
                messageMatch.add(namespaceMessages.get(i));
            }
        }
        return messageMatch;
    }
 
    /**
     * Inner class responsible for creating separate threads for receiving
     * and queuing incoming messages. 
     * @author bachmakm
     *
     */
    private class MessageThread implements Runnable
    {
        /**
         * Flag used for stopping a thread when a timeout has occurred. 
         */
        private boolean m_Running = true;

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
            try
            {
                m_Socket.setSoTimeout(SOCK_TIMEOUT);
            }
            catch (SocketException e1)
            {
                System.err.println("Unable to set socket timeout. Stopping thread.");
                e1.printStackTrace();
                stop();
            }
           
            TerraHarvestMessage response = null;
            while(m_Running)
            {
                try
                {
                    response = TerraHarvestMessage.parseDelimitedFrom(m_InputStream);
                    m_Responses.add(response);
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
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Inner class responsible for creating separate threads for receiving
     * and queuing incoming messages, used where there is an additional socket that needs
     * to be listened on in parallel. 
     * @author callen
     *
     */
    private class AdditionalMessageThread implements Runnable
    {
        /**
         * Flag used for stopping a thread when a timeout has occurred. 
         */
        private boolean m_Running = true;

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
            try
            {
                m_AdditionalSocket.setSoTimeout(SOCK_TIMEOUT);
                m_Socket.setSoTimeout(SOCK_TIMEOUT);
            }
            catch (SocketException e1)
            {
                System.err.println("Unable to set socket timeout. Stopping thread.");
                e1.printStackTrace();
                stop();
            }
           
            TerraHarvestMessage response = null;
            while(m_Running)
            {
                try
                {
                    response = TerraHarvestMessage.parseDelimitedFrom(m_AdditionalInputStream);
                    m_AdditionalResponses.add(response);
                    response = TerraHarvestMessage.parseDelimitedFrom(m_InputStream);
                    m_Responses.add(response);
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
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}


