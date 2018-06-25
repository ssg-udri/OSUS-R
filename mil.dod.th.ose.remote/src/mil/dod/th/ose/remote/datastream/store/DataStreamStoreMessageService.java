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
package mil.dod.th.ose.remote.datastream.store;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import mil.dod.th.core.datastream.DataStreamService;
import mil.dod.th.core.datastream.StreamProfile;
import mil.dod.th.core.datastream.store.DataStreamStore;
import mil.dod.th.core.datastream.store.DateRange;
import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.ClientAckData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DataStreamStoreNamespace.DataStreamStoreMessageType;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.DisableArchivingRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.EnableArchivingRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsResponseData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivePeriodsResponseData.Builder;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivedDataRequestData;
import mil.dod.th.core.remote.proto.DataStreamStoreMessages.GetArchivedDataResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * This class is responsible for receiving and responding to messages from the DataStreamStore 
 * namespace using the remote interface.
 * 
 * @author jmiller
 *
 */
@Component(immediate = true, provide = { })
public class DataStreamStoreMessageService implements MessageService
{

    /**
     * Constant error message sent remotely after the occurrence of an exception.
     */
    final private static String GENERIC_ERR_MSG = "Cannot complete request. ";
    
    /**
     * Default block size in bytes for messages containing archived data.
     */
    final private static int DATA_BLOCK_SIZE = 1024 * 1024;
    
    /**
     * Sleep period for thread retrieving archived data, in milliseconds.
     */
    final private static int SLEEP_PERIOD_MS = 10;

    /**
     * Used for logging messages.
     */
    private LoggingService m_Logging;

    /**
     * Reference to the event admin service.  Used for local messages within event admin service.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Local service for managing StreamProfile instances.
     */
    private DataStreamService m_DataStreamService;
    
    /**
     * Local service for storing and retrieving streaming data.
     */
    private DataStreamStore m_DataStreamStore;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Routes incoming messages.
     */
    private MessageRouterInternal m_MessageRouter;
    
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
     * Bind the DataStreamService.
     * 
     * @param dataStreamService
     *      service for managing StreamProfile instances locally
     */
    @Reference(optional = true, dynamic = true)
    public void setDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = dataStreamService;
    }

    /**
     * Unbind the DataStreamService.
     * 
     * @param dataStreamService
     *      service for managing StreamProfile instances locally
     */
    public void unsetDataStreamService(final DataStreamService dataStreamService)
    {
        m_DataStreamService = null; // NOPMD: NullAssignment, Must assign to null if no longer available
    }
    
    /**
     * Bind the DataStreamStore.
     * 
     * @param dataStreamStore
     *      service for storing and retrieving streaming data locally
     */
    @Reference(optional = true, dynamic = true)
    public void setDataStreamStore(final DataStreamStore dataStreamStore)
    {
        m_DataStreamStore = dataStreamStore;
    }
    
    /**
     * Unbind the DataStreamStore.
     * 
     * @param dataStreamStore
     *      service for storing and retrieving streaming data locally
     */
    public void unsetDataStreamStore(final DataStreamStore dataStreamStore)
    {
        m_DataStreamStore = null; // NOPMD: NullAssignment, Must assign to null if no longer available
    }

    /**
     * Bind to the service for creating remote messages.
     * 
     * @param messageFactory
     *      service that create messages
     */
    @Reference
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /**
     * Bind a message router to register.
     * 
     * @param messageRouter
     *      router that handles incoming messages
     */
    @Reference
    public void setMessageRouter(final MessageRouterInternal messageRouter)
    {
        m_MessageRouter = messageRouter;
    }
    
    /**
     * Activate method to bind this service to the message router.
     */
    @Activate
    public void activate()
    {
        m_MessageRouter.bindMessageService(this);
    }
    
    /**
     * Deactivate component by unbinding the service from the message router.
     */
    @Deactivate
    public void deactivate()
    {
        m_MessageRouter.unbindMessageService(this);
    }

    @Override
    public Namespace getNamespace()
    {
        return Namespace.DataStreamStore;
    }


    @Override
    public void handleMessage(final TerraHarvestMessage message, 
            final TerraHarvestPayload payload, final RemoteChannel channel) throws IOException
    {
        final DataStreamStoreNamespace serviceMessage = DataStreamStoreNamespace.
                parseFrom(payload.getNamespaceMessage());
        
        Message dataMessage = null;
        
        switch (serviceMessage.getType())
        {
            case EnableArchivingRequest:
                dataMessage = enableArchiving(serviceMessage, message, channel);
                break;
            case EnableArchivingResponse:
                break;
            case DisableArchivingRequest:
                dataMessage = disableArchiving(serviceMessage, message, channel);
                break;
            case DisableArchivingResponse:
                break;
            case ClientAck:
                clientAck(serviceMessage, message, channel);
                break;
            case GetArchivePeriodsRequest:
                dataMessage = getArchivePeriods(serviceMessage, message, channel);
                break;
            case GetArchivePeriodsResponse:
                dataMessage = GetArchivePeriodsResponseData.parseFrom(serviceMessage.getData());
                break;
            case GetArchivedDataRequest:
                getArchivedData(serviceMessage, message, channel);
                break;
            case GetArchivedDataResponse:
                dataMessage = GetArchivedDataResponseData.parseFrom(serviceMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the DataStreamStoreMessageService namespace.", serviceMessage.getType()));
        }
        
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, serviceMessage,
                serviceMessage.getType(), dataMessage, channel);
        
        m_EventAdmin.postEvent(event);

    }
    
    /**
     * Method responsible for handling a remote request to enable an archiving process based on a
     * stream profile's UUID.
     * 
     * @param message
     *      EnableArchivingRequest message containing a stream profile UUID, a heartbeat period, a
     *      delay value, and a boolean indicating whether to use the source bitrate
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed
     */
    private Message enableArchiving(final DataStreamStoreNamespace message, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final EnableArchivingRequestData enableRequest =
                EnableArchivingRequestData.parseFrom(message.getData());
        
        try
        {
            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(enableRequest.getStreamProfileUuid()));
            
            final boolean useSourceBitrate = enableRequest.getUseSourceBitrate();
            final long heartbeatPeriod = enableRequest.getHeartbeatPeriod();
            final long delay = enableRequest.getDelay();
            
            m_DataStreamStore.enableArchiving(streamProfile, useSourceBitrate, heartbeatPeriod, delay);
            
            m_MessageFactory.createDataStreamStoreResponseMessage(request, 
                    DataStreamStoreMessageType.EnableArchivingResponse, null).queue(channel); 
        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.error(e, "Failed to enable archiving process.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        
        return enableRequest;

    }
    
    /**
     * Method responsible for handling a remote request to disable an archiving process based on a
     * stream profile's UUID.
     * 
     * @param message
     *      DisableArchivingRequest message containing a stream profile UUID
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed
     */
    private Message disableArchiving(final DataStreamStoreNamespace message, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final DisableArchivingRequestData disableRequest =
                DisableArchivingRequestData.parseFrom(message.getData());
        
        try
        {
            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(disableRequest.getStreamProfileUuid()));
            
            m_DataStreamStore.disableArchiving(streamProfile);
            
            m_MessageFactory.createDataStreamStoreResponseMessage(request, 
                    DataStreamStoreMessageType.DisableArchivingResponse, null).queue(channel); 
        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.error(e, "Failed to disable archiving process.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        
        return disableRequest;

    }

    /**
     * Method responsible for resetting the countdown timer for an archiving process.
     * 
     * @param message
     *      ClientAckData message containing a stream profile UUID
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      if message cannot be parsed
     */
    private void clientAck(final DataStreamStoreNamespace message, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final ClientAckData ack = ClientAckData.parseFrom(message.getData());

        try
        {

            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(ack.getStreamProfileUuid()));

            m_DataStreamStore.clientAck(streamProfile);

        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.error(e, "Failed to perform acknowledgment for archiving process.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }

    }
    
    /**
     * Method responsible for retrieving the available time periods of archived data.
     * 
     * @param message
     *      GetArchivePeriodsRequest message containing a stream profile UUID
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @return
     *      the data message for this request
     * @throws IOException
     *      if message cannot be parsed
     */
    private Message getArchivePeriods(final DataStreamStoreNamespace message, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final GetArchivePeriodsRequestData getArchivePeriodsRequest = 
                GetArchivePeriodsRequestData.parseFrom(message.getData());
        
        try
        {
            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(getArchivePeriodsRequest.getStreamProfileUuid()));
            
            final List<DateRange> dateRanges = m_DataStreamStore.getArchivePeriods(streamProfile);
            
            final Builder builder = GetArchivePeriodsResponseData.newBuilder();
            
            for (DateRange range : dateRanges)
            {
                final DataStreamStoreMessages.DateRange.Builder dateRangeBuilder =
                        DataStreamStoreMessages.DateRange.newBuilder();
                
                dateRangeBuilder.setStartTime(range.getStartTime());
                dateRangeBuilder.setStopTime(range.getStopTime());
                
                builder.addDateRange(dateRangeBuilder.build());
            }
            
            m_MessageFactory.createDataStreamStoreResponseMessage(request,
                    DataStreamStoreMessageType.GetArchivePeriodsResponse, builder.build()).queue(channel);
            
        }
        catch (final IllegalArgumentException e)
        {
            m_Logging.error(e, "Failed to retrieve archive periods.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + e.getMessage()).queue(channel);
        }
        
        return getArchivePeriodsRequest;

    }
    
    /**
     * Method responsible for retrieving archived data and sending back a block of data per message.
     * 
     * @param message
     *      GetArchivedDataRequest message containing a stream profile UUID
     * @param request
     *      entire remote message for the request
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      if message cannot be parsed
     */
    private void getArchivedData(final DataStreamStoreNamespace message, final TerraHarvestMessage request,
            final RemoteChannel channel) throws IOException
    {
        final GetArchivedDataRequestData getArchivedDataRequest = 
                GetArchivedDataRequestData.parseFrom(message.getData());
        
        try
        {
            final StreamProfile streamProfile = m_DataStreamService.getStreamProfile(
                    SharedMessageUtils.convertProtoUUIDtoUUID(getArchivedDataRequest.getStreamProfileUuid()));
            
            final mil.dod.th.core.remote.proto.DataStreamStoreMessages.DateRange dateRange =
                    getArchivedDataRequest.getDateRange();
            
            final BufferedInputStream inStream = new BufferedInputStream(m_DataStreamStore.getArchiveStream(
                    streamProfile, new DateRange(dateRange.getStartTime(), dateRange.getStopTime())), 
                    DATA_BLOCK_SIZE);
            
            long seqNum = 0;
            
            while (true)
            {
                byte[] data = new byte[DATA_BLOCK_SIZE];
                boolean isLastResponse = false;

                try
                {
                    final int numBytesRead = inStream.read(data);

                    if (numBytesRead != DATA_BLOCK_SIZE)
                    {
                        
                        if (numBytesRead > 0)
                        {
                            data = Arrays.copyOf(data, numBytesRead);
                        }
                        else
                        {
                            isLastResponse = true;
                            data = new byte[0];
                        }

                    }
                }
                catch (final EOFException eofe)
                {
                    m_Logging.info("End of input reached");
                    isLastResponse = true;
                }
                catch (final IOException ioe)
                {
                    m_Logging.error(ioe, "Error while reading from stream");
                    isLastResponse = true;
                }
                finally
                {
                    final GetArchivedDataResponseData response = GetArchivedDataResponseData.newBuilder().
                            setDataBlock(ByteString.copyFrom(data)).
                            setIsLastResponse(isLastResponse).
                            setSequenceNum(seqNum).build();
                    
                    m_MessageFactory.createDataStreamStoreResponseMessage(request, 
                            DataStreamStoreMessageType.GetArchivedDataResponse, response).queue(channel);

                }
                
                if (isLastResponse)
                {
                    inStream.close();
                    return;
                }
                
                //Update sequence number for next iteration.
                seqNum++;
                
                try
                {
                    Thread.sleep(SLEEP_PERIOD_MS);
                }
                catch (final InterruptedException e)
                {
                    m_Logging.error(e, "Sleep period interrupted.");
                }
            }
            
        }
        catch (final IllegalArgumentException ioe)
        {
            m_Logging.error(ioe, "Error while retrieving archived data.");
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.ILLEGAL_STATE,
                    GENERIC_ERR_MSG + ioe.getMessage()).queue(channel);
        }
        
    }

}
