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
import java.net.Socket;
import java.util.List;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.ClientAckData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace.DataStreamStoreMessageType;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DateRange;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DisableArchivingRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.EnableArchivingRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.SharedMessages;

/**
 * @author jmiller
 *
 */
public final class DataStreamStoreNamespaceUtils
{
    /**
     * Timeout value used to wait for message responses.
     */
    private static final int TIMEOUT = 5000;
    
    /**
     * Private constructor to prevent instantiation.
     */
    private DataStreamStoreNamespaceUtils()
    {
        
    }
    
    /**
     * Enable archiving for a given stream profile instance.
     */
    public static void enableArchiving(Socket socket, SharedMessages.UUID uuid, 
            long heartbeatPeriod, boolean errorExpected) throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        EnableArchivingRequestData request = EnableArchivingRequestData.newBuilder().
                setStreamProfileUuid(uuid).
                setUseSourceBitrate(true).
                setHeartbeatPeriod(heartbeatPeriod).
                setDelay(0L).build();
        
        TerraHarvestMessage message = DataStreamStoreNamespaceUtils.createDataStreamStoreNamespaceMessage(
                DataStreamStoreMessageType.EnableArchivingRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());

        if (errorExpected)
        {
            listener.waitForMessage(Namespace.Base, BaseMessageType.GenericErrorResponse, TIMEOUT);
        }
        else
        {
            listener.waitForMessage(Namespace.DataStreamStore, DataStreamStoreMessageType.EnableArchivingResponse, 
                    TIMEOUT);
        }
    }
    
    /**
     * Disable archiving for a given stream profile instance.
     */
    public static void disableArchiving(Socket socket, SharedMessages.UUID uuid, boolean errorExpected) 
            throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        DisableArchivingRequestData request = DisableArchivingRequestData.newBuilder().
                setStreamProfileUuid(uuid).build();
        
        TerraHarvestMessage message = DataStreamStoreNamespaceUtils.createDataStreamStoreNamespaceMessage(
                DataStreamStoreMessageType.DisableArchivingRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());

        if (errorExpected)
        {
            listener.waitForMessage(Namespace.Base, BaseMessageType.GenericErrorResponse, TIMEOUT);
        }
        else
        {
            listener.waitForMessage(Namespace.DataStreamStore, DataStreamStoreMessageType.DisableArchivingResponse, 
                    TIMEOUT);
        }
    }
    
    public static void clientAck(Socket socket, SharedMessages.UUID uuid, boolean errorExpected) throws IOException
    {              
        MessageListener listener = new MessageListener(socket);
        
        ClientAckData request = ClientAckData.newBuilder().
                setStreamProfileUuid(uuid).build();
        
        TerraHarvestMessage message = DataStreamStoreNamespaceUtils.createDataStreamStoreNamespaceMessage(
                DataStreamStoreMessageType.ClientAck, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        if (errorExpected)
        {
            listener.waitForMessage(Namespace.Base, BaseMessageType.GenericErrorResponse, TIMEOUT);
        }       
    }
    
    public static List<DateRange> getArchivePeriods(Socket socket, SharedMessages.UUID uuid, boolean errorExpected)
            throws IOException
    {
        MessageListener listener = new MessageListener(socket);
        
        GetArchivePeriodsRequestData request = GetArchivePeriodsRequestData.newBuilder().
                setStreamProfileUuid(uuid).build();
        
        TerraHarvestMessage message = DataStreamStoreNamespaceUtils.createDataStreamStoreNamespaceMessage(
                DataStreamStoreMessageType.GetArchivePeriodsRequest, request);
        message.writeDelimitedTo(socket.getOutputStream());
        
        if (errorExpected)
        {
            listener.waitForMessage(Namespace.Base, BaseMessageType.GenericErrorResponse, TIMEOUT);
            return null;
        }
        else
        {
            DataStreamStoreNamespace response = (DataStreamStoreNamespace)listener.waitForMessage(
                    Namespace.DataStreamStore, DataStreamStoreMessageType.GetArchivePeriodsResponse, TIMEOUT);
            
            GetArchivePeriodsResponseData dataResponse = GetArchivePeriodsResponseData.parseFrom(response.getData());
            
            return dataResponse.getDateRangeList();
        }
    }

    public static TerraHarvestMessage createDataStreamStoreNamespaceMessage(final DataStreamStoreMessageType type,
            final Message message)
    {
        DataStreamStoreNamespace.Builder dataStreamStoreMessageBuilder = DataStreamStoreNamespace.newBuilder().
                setType(type);
        
        if (message != null)
        {
            dataStreamStoreMessageBuilder.setData(message.toByteString());
        }
        
        TerraHarvestMessage thMessage = TerraHarvestMessageHelper.createTerraHarvestMsg(
                Namespace.DataStreamStore, dataStreamStoreMessageBuilder);
        
        return thMessage;
    }
}
