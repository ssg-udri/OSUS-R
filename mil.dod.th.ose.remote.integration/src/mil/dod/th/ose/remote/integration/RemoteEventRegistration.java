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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationRequestData;
import mil.dod.th.core.remote.proto.EventMessages.EventRegistrationResponseData;
import mil.dod.th.core.remote.proto.EventMessages.EventAdminNamespace.EventAdminMessageType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.ose.remote.integration.namespace.TestEventAdminNamespace;
import mil.dod.th.ose.test.remote.RemoteUtils;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import com.google.protobuf.Message;

/**
 * This class is designed to send registration messages for remote events. 
 * Class contains methods that sends messages from main system id and alternative system id
 * @author frenchpd
 *
 */
public final class RemoteEventRegistration
{

    private RemoteEventRegistration()
    {
        //Constructor to avoid instantiating when used in other classes
    }
    
    /**
     * Method send registration message listening for remote message events on the given communication avenue.
     * To start the listening see 
     * {@link mil.dod.th.ose.remote.integration.MessageListener#waitForMessage}.
     * @param socket 
     *     socket that is sending remote registration event messages
     * @param topic
     *     the topic to listen for
     * @param filter
     *     the filter to add to the topic
     * @return
     *     the remote registration id for remote events, used to unregister for remote events
     * @throws IOException 
     *     if cannot get output stream due to socket issues
     */
    public static int regRemoteEventMessages(Socket socket, String[] topic, String filter, 
            RemoteTypesGen.LexiconFormat.Enum format) throws 
        IOException
    {
        //create registration message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addAllTopic(Arrays.asList(topic));

        //append required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, true);
        
        //append filter if filter was sent
        if (filter != null)
        {
            requestMessage.setFilter(filter);
        }
        
        if (format != null)
        {
            requestMessage.setObjectFormat(format);
        }

        //create TH message to send
        TerraHarvestMessage message = TestEventAdminNamespace.createEventAdminMessage(
            EventAdminMessageType.EventRegistrationRequest, requestMessage.build());
        try
        {
            //send out message
            message.writeDelimitedTo(socket.getOutputStream());
        }
        catch (IOException exception)
        {
            fail("Unable to send request to receive remote events" + exception.getMessage());
        }
        //used to return reg id
        int registrationId = -1;
        
        MessageListener listener = new MessageListener(socket);
        //start thread message listener
        Message messageRcvd = 
            listener.waitForMessage(Namespace.EventAdmin, EventAdminMessageType.EventRegistrationResponse, 1000);
            
        try
        {
            EventAdminNamespace namespaceResponse = (EventAdminNamespace) messageRcvd;
            assertThat(namespaceResponse.getType(), is(EventAdminMessageType.EventRegistrationResponse));
            EventRegistrationResponseData typeResponse = EventRegistrationResponseData.
                parseFrom(namespaceResponse.getData());
            //save the reg id for later deregistration
            registrationId = typeResponse.getId();
        }
        catch (IOException exception)
        {
            fail("Unable to parse response to receive remote events" + exception.getMessage());
        }
        return registrationId;
    }
    
    public static int regRemoteEventMessages(Socket socket, String[] topic) throws IOException
    {
        return regRemoteEventMessages(socket, topic, null, null);
    }
    
    public static int regRemoteEventMessages(Socket socket, String topic) throws IOException
    {
        return regRemoteEventMessages(socket, new String[] {topic}, null, null);
    }

    public static int regRemoteEventMessages(Socket socket, String topic, String filter) throws IOException
    {
        return regRemoteEventMessages(socket, new String[] {topic}, filter, null);
    }
    
    public static int regRemoteEventMessages(Socket socket, String topic, 
            RemoteTypesGen.LexiconFormat.Enum format) 
            throws IOException
    {
        return regRemoteEventMessages(socket, new String[] {topic}, null, format);
    }
    
    /**
     * Method send registration message listening for remote message events using the defined in the 
     * {@link TerraHarvestMessageHelper#ADDITIONAL_SYSTEM_ID}.
     * To start the listening see {@link mil.dod.th.ose.remote.integration.MessageListener#waitForMessage}.
     * @param socket 
     *     socket that is sending remote registration event messages
     * @param topic
     *     the topic to listen for
     * @param filter
     *     the filter to add to the topic
     * @return
     *     the remote registration id for remote events, used to unregister for remote events
     * @throws IOException 
     *     if cannot get output stream due to socket issues
     */
    public static int regRemoteEventMessagesAlternavtiveSystem(Socket socket, String[] topic, String filter) throws 
        IOException, InterruptedException
    {
        //create registration message
        EventRegistrationRequestData.Builder requestMessage = EventRegistrationRequestData.newBuilder()
                .addAllTopic(Arrays.asList(topic));

        //append required fields
        requestMessage = RemoteUtils.appendRequiredFields(requestMessage, true);
        
        //append filter if filter was sent
        if (filter != null)
        {
            requestMessage.setFilter(filter);
        }

        //create TH message to send
        EventAdminNamespace.Builder eventMessageBuilder = EventAdminNamespace.newBuilder().
                setType(EventAdminMessageType.EventRegistrationRequest).
                setData(requestMessage.build().toByteString());
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.
                createAdditionalSystemTerraHarvestMsg(Namespace.EventAdmin, eventMessageBuilder);
        
        try
        {
            //send out message
            thMessage.writeDelimitedTo(socket.getOutputStream());
        }
        catch (IOException exception)
        {
            fail("Unable to send request to receive remote events" + exception.getMessage());
        }
        //used to return reg id
        int registrationId = -1;

        MessageListener listener = new MessageListener(socket);
        //start thread message listener
        Message messageRcvd = listener.waitForMessage(Namespace.EventAdmin, 
                EventAdminMessageType.EventRegistrationResponse, 800);
            
        try
        {
            EventAdminNamespace namespaceResponse = (EventAdminNamespace) messageRcvd;
            assertThat(namespaceResponse.getType(), is(EventAdminMessageType.EventRegistrationResponse));
            EventRegistrationResponseData typeResponse = EventRegistrationResponseData.
                parseFrom(namespaceResponse.getData());
            //save the reg id for later deregistration
            registrationId = typeResponse.getId();
        }
        catch (IOException exception)
        {
            fail("Unable to parse response to receive remote events" + exception.getMessage());
        }
        return registrationId;
    }
}
