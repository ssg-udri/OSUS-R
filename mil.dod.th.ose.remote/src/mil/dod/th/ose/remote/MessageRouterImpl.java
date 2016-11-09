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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.UnmarshalException;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.messaging.MessageRouter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.EncryptionInfoMessages.EncryptionErrorCode;
import mil.dod.th.core.remote.proto.RemoteBase.EncryptType;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.system.TerraHarvestSystem;
import mil.dod.th.ose.remote.api.EnumConverter;
import mil.dod.th.ose.remote.api.RemoteSettings;
import mil.dod.th.ose.remote.encryption.EncryptMessageService;
import mil.dod.th.ose.remote.encryption.InvalidKeySignatureException;
import mil.dod.th.ose.remote.messaging.TerraHarvestMessageUtil;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * Implementation of the MessageRouter interface.
 * 
 * @author Dave Humeniuk
 *
 */
@Component(immediate = true) // TODO: TH-700 - shouldn't need to be immediate, but helps keep this component active
public class MessageRouterImpl implements MessageRouter, MessageRouterInternal
{
    /**
     * Map containing all messages services by their namespace.
     */
    final private Map<Namespace, MessageService> m_MessageServices = new HashMap<Namespace, MessageService>();
    
    /**
     * Service for logging messages.
     */
    private LoggingService m_Log;
    
    /**
     * Service for this instance of a Terra Harvest system.
     */
    private TerraHarvestSystem m_TerraHarvestSystem;
    
    /**
     * Encryption service.
     */
    private EncryptMessageService m_EncryptService;
    
    /**
     * Remote configuration property service.
     */
    private RemoteSettings m_RemoteSettings;
    
    /**
     * Event admin service used to post events to the system.
     */
    private EventAdmin m_EventAdmin;
    
    /**
     * Remote channel lookup used to route messages.
     */
    private RemoteChannelLookup m_RemoteChannelLookup;
    
    /**
     * Binds the logging service for logging messages.
     * 
     * @param logging
     *            Logging service object
     */
    @Reference
    public void setLoggingService(final LoggingService logging)
    {
        m_Log = logging;
    }
    
    /**
     * Bind the service.
     * 
     * @param terraHarvestSystem
     *      service to use for querying about the system
     */
    @Reference
    public void setTerraHarvestSystem(final TerraHarvestSystem terraHarvestSystem)
    {
        m_TerraHarvestSystem = terraHarvestSystem;
    }
    
    /**
     * Bind the encryption service.
     * 
     * @param encryptionService
     *      service to use for unencrypting messages
     */
    @Reference
    public void setEncryptMessageService(final EncryptMessageService encryptionService)
    {
        m_EncryptService = encryptionService;
    }

    /**
     * Bind the remote settings service.
     * 
     * @param remoteSettings
     *     service that contains configurations specific to the remote interface
     */
    @Reference
    public void setRemoteSettings(final RemoteSettings remoteSettings)
    {
        m_RemoteSettings = remoteSettings;
    }
    
    @Reference
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    @Override
    public void bindMessageService(final MessageService messageService)
    {
        m_MessageServices.put(messageService.getNamespace(), messageService);
        m_Log.info("Remote message service bound for %s namespace", messageService.getNamespace());
    }
    
    @Override
    public void unbindMessageService(final MessageService messageService)
    {
        m_MessageServices.remove(messageService.getNamespace());
        m_Log.info("Remote message service unbound for %s namespace", messageService.getNamespace());
    }
    
    @Override
    public void bindRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        if (m_RemoteChannelLookup == null)
        {
            m_RemoteChannelLookup = lookup;
        }
    }
    
    @Override
    public void unbindRemoteChannelLookup(final RemoteChannelLookup lookup)
    {
        if (lookup != null && lookup.equals(m_RemoteChannelLookup))
        {
            m_RemoteChannelLookup = null; //NOPMD: explicit null set, set to null to fully release ownership
        }
    }
    
    @Override
    public void handleMessage(final TerraHarvestMessage message)
    {
        handleMessage(message, null);
    }
    
    @Override
    public void handleMessage(final TerraHarvestMessage message, final RemoteChannel channel)
    {
        //Verify that the message is intended for this system.
        if (message.getDestId() != m_TerraHarvestSystem.getId() && message.getDestId() != Integer.MAX_VALUE)
        {
            handleInvalidDestination(message);
            return;
        }
        
        //payLoad variable holds namespace and message itself, error message is used to describe the 
        //error code and description
        final TerraHarvestPayload payload;
        TerraHarvestMessage errorMessage;
        
        final RemoteChannel destChannel;
        if (channel == null)
        {
            destChannel = new EventChannel(message.getSourceId(), m_EventAdmin);
        }
        else
        {
            destChannel = channel;
        }
        
        if (!confirmSafeDecryption(message, destChannel))
        {
            return;
        }
        
        //get the payload
        try 
        {
            payload = m_EncryptService.decryptRemoteMessage(message);
        } 
        catch (final InvalidProtocolBufferException e) 
        {
            m_Log.error(e, "Invalid TerraHarvestPayload with messageID, channel info: %d %s", //NOCHECKSTYLE 
                    message.getMessageId(), channel); //repeated string literal, clearly describes the error that 
                                                      //can occur.
            errorMessage = TerraHarvestMessageUtil.buildErrorResponseMessage(message, ErrorCode.INVALID_REQUEST, 
                    "Invalid payload exception when handling a message"); //NOCHECKSTYLE repeated string literal,
            destChannel.queueMessage(errorMessage); //clearly describes the error that can occur.
            return;
        }
        catch (final InvalidKeySignatureException ex)
        {         
            m_Log.error(ex, "Invalid encrypted TerraHarvestPayload with messageID, channel info: %d %s", 
                    message.getMessageId(), channel);
            errorMessage = TerraHarvestMessageUtil.buildEncryptionErrorResponseMessage(message, 
                    EncryptionErrorCode.INVALID_SIGNATURE_KEY, ex.getMessage(), m_RemoteSettings.getEncryptionMode());
            destChannel.queueMessage(errorMessage);
            return;
        }
        
        tryCallMessageServiceHandleMessage(message, payload, destChannel);
    }
    
    /**
     * Confirm that it is safe to decrypt a message. If a request comes through for information from this system
     * that information must be encrypted to a level that meets or exceeds the encryption level of this system.
     *  **NOTE that the 'level' is correlated to the proto enum ordinal value.
     * @param message
     *      the terra harvest message that needs to be evaluated
     * @param channel
     *      the {@link RemoteChannel} used to send error messages
     * @return
     *      True if the message is safe to decrypt and false otherwise.
     */
    private boolean confirmSafeDecryption(final TerraHarvestMessage message, final RemoteChannel channel)
    {
        final EncryptType currentSysEncryptType = 
                EnumConverter.convertEncryptionModeToEncryptType(m_RemoteSettings.getEncryptionMode());
        final EncryptType messageEncryptType = message.getEncryptType();
        
        if (currentSysEncryptType.getNumber() > messageEncryptType.getNumber())
        { 
            try
            {
                if (checkMessageForEncryptionInfo(message, messageEncryptType))
                {
                    return true;
                }
            }
            catch (final InvalidProtocolBufferException ex)
            {
                m_Log.error(ex, "Invalid TerraHarvestPayload with messageID, channel info: %d %s", 
                        message.getMessageId(), channel);
                final TerraHarvestMessage errorMessage = TerraHarvestMessageUtil.buildErrorResponseMessage(message, 
                        ErrorCode.INVALID_REQUEST, 
                        "Invalid payload exception when handling a message");
                channel.queueMessage(errorMessage);
                return false;
            }
            
            //not supported the level of the message must meet or exceed the current security/encryption level
            //of this system.
            m_Log.error("Received message from %d, the message had invalid encryption data", 
                    message.getSourceId());
            final TerraHarvestMessage errorMessage = TerraHarvestMessageUtil.buildEncryptionErrorResponseMessage(
                    message, EncryptionErrorCode.INVALID_ENCRYPTION_LEVEL, String.format("System %d does not support " 
                            + "type %s encryption.", m_TerraHarvestSystem.getId(), message.getEncryptType().toString()),
                            m_RemoteSettings.getEncryptionMode());
            channel.queueMessage(errorMessage);
            return false;
        }
        return true;
    }
    
    /**
     * Method that checks the encryption level and namespace of the message. If the encryption level is none and the
     * message belongs to the {@link Namespace#EncryptionInfo} it should always be acknowledged by the system.
     * 
     * @param message
     *      The message with the namespace to be checked.
     * @param messageEncryptType
     *      The encryption level of the message.
     * @return
     *      True if the message is not encrypted and belongs to the {@link Namespace#EncryptionInfo} namespace.
     *      Otherwise false is returned.
     * @throws InvalidProtocolBufferException
     *      If the payload cannot be parsed from the message while trying to check what namespace the message pertains
     *      to. 
     */
    private boolean checkMessageForEncryptionInfo(final TerraHarvestMessage message, 
            final EncryptType messageEncryptType) throws InvalidProtocolBufferException
    {
        if (messageEncryptType == EncryptType.NONE)
        {
            final TerraHarvestPayload unencryptedPayload = 
                    TerraHarvestPayload.parseFrom(message.getTerraHarvestPayload());
            final Namespace messageNamespace = unencryptedPayload.getNamespace();
            

            //Check if there is no encryption type and if the message is from the encryption information namespace.
            //If it meets these conditions then the message should always be acknowledged.
            if (messageNamespace == Namespace.EncryptionInfo)
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Method that retrieves the message service for the given namespace and then attempts to call the handle message
     * method.
     * 
     * @param message
     *      the message that was received
     * @param payload
     *      the payload that was received
     * @param channel
     *      the remote channel that should be used to send any error messages
     */
    private void tryCallMessageServiceHandleMessage(final TerraHarvestMessage message, 
            final TerraHarvestPayload payload, final RemoteChannel channel)
    {
        TerraHarvestMessage errorMessage;//NOCHECKSTYLE will get assigned in if statement
        final MessageService messageService = m_MessageServices.get(payload.getNamespace());       
        if (messageService == null)
        {
            //no service found
            m_Log.warning("No remote message service found for namespace %s", payload.getNamespace());
            
            errorMessage = TerraHarvestMessageUtil.buildErrorResponseMessage(message, ErrorCode.NO_MESSAGE_SERVICE, 
                    "No remote message service found to handle the message: " + payload.getNamespace());
            channel.queueMessage(errorMessage);
            return;
        }

        ErrorCode errorCode;
        String errorMsg;
        Exception exception;
        try
        {
            messageService.handleMessage(message, payload, channel);
            return;
        }
        catch (final IOException e)
        {
            exception = e;
            errorMsg = String.format("Failed IO operation when handling [%s] namespace message: %s", 
                    payload.getNamespace(), e.getMessage());
            errorCode = ErrorCode.INVALID_REQUEST;
        }
        catch (final IllegalArgumentException e)
        {
            exception = e;
            errorMsg = String.format("Invalid value when handling [%s] namespace message: %s", 
                    payload.getNamespace(), e.getMessage());
            errorCode = ErrorCode.INVALID_VALUE;
        }
        catch (final IllegalStateException e)
        {
            exception = e;
            errorMsg = String.format("Illegal state when handling [%s] namespace message: %s", 
                    payload.getNamespace(), e.getMessage());
            errorCode = ErrorCode.ILLEGAL_STATE;
        }
        catch (final UnmarshalException e)
        {
            exception = e;
            errorMsg = String.format("JAXB error when handling [%s] namespace message: %s", 
                    payload.getNamespace(), e.getMessage());
            errorCode = ErrorCode.JAXB_ERROR;
        }
        catch (final ObjectConverterException e)
        {
            exception = e;
            errorMsg = String.format("Proto converter error when handling [%s] namespace message: %s", 
                    payload.getNamespace(), e.getMessage());
            errorCode = ErrorCode.CONVERTER_ERROR;
        }
        catch (final Exception e)
        {
            exception = e;
            errorMsg = String.format("%s when handling [%s] namespace message: %s", 
                    e.getClass().getSimpleName(), payload.getNamespace(), e.getMessage());
            errorCode = ErrorCode.INTERNAL_ERROR;
        }
        
        m_Log.error(exception, errorMsg);
        errorMessage = TerraHarvestMessageUtil.buildErrorResponseMessage(message, errorCode, errorMsg);
        channel.queueMessage(errorMessage);
    }
    
    /**
     * Method that handles a terra harvest message with a destination other than that of the receiving system. Will try
     * to forward a message if a remote channel is available. If unable to use a remote channel to forward, then an 
     * event of type {@link mil.dod.th.core.remote.RemoteConstants#TOPIC_MESSAGE_RECEIVED_UNREACHABLE_DEST} will be 
     * posted.
     * 
     * @param message
     *          The message to be handled.
     */
    private void handleInvalidDestination(final TerraHarvestMessage message)
    {
        if (m_RemoteChannelLookup != null)
        {
            final List<RemoteChannel> channels = m_RemoteChannelLookup.getChannels(message.getDestId());
            for (RemoteChannel destChannel : channels)
            {
                if (destChannel.queueMessage(message))
                {
                    return;
                }
            }
        }
        final Event invalidDest = RemoteInterfaceUtilities.createMessageUnreachableReceivedEvent(message);
        m_EventAdmin.postEvent(invalidDest);
    }
}
