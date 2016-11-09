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
package mil.dod.th.ose.remote.osgi;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;

import com.google.protobuf.Message;

import mil.dod.th.core.log.LoggingService;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.ErrorCode;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminErrorCode;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespace.ConfigAdminMessageType;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigAdminNamespaceErrorData;
import mil.dod.th.core.remote.proto.ConfigMessages.ConfigurationInfoType;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.CreateFactoryConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.DeleteConfigurationResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetConfigurationInfoResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyKeysResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyRequestData;
import mil.dod.th.core.remote.proto.ConfigMessages.GetPropertyResponseData;
import mil.dod.th.core.remote.proto.ConfigMessages.SetPropertyRequestData;
import mil.dod.th.core.remote.proto.MapTypes.SimpleTypesMapEntry;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.core.remote.proto.SharedMessages.Multitype;
import mil.dod.th.ose.remote.MessageRouterInternal;
import mil.dod.th.ose.remote.MessageService;
import mil.dod.th.ose.remote.util.RemoteInterfaceUtilities;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;


/**
 * Message service for the {@link ConfigAdminNamespace} messages.
 * @author callen
 *
 */
//service is not provided as this would create a cycle, instead this class registers with the message router
@Component(immediate = true, provide = { }) //NOCHECKSTYLE Max cyclomatic complexity reached. Need to handle all 
public class ConfigAdminMessageService implements MessageService //messages for the namespace.
{
    /**
     * Service for logging messages.
     */
    private LoggingService m_Logging;
    
    /**
     * Service for creating messages to send through the remote interface.
     */
    private MessageFactory m_MessageFactory;
    
    /**
     * Service that handles configurable aspects of the system.
     */
    private ConfigurationAdmin m_ConfigAdmin;
    
    /**
     * Reference to the event admin service, used to post events that a message was received.
     */
    private EventAdmin m_EventAdmin;
    
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
     * Bind the {@link ConfigurationAdmin} service.
     * 
     * @param configurationAdmin
     *      service that handles persisting bundle configuration data
     */
    @Reference
    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin)
    {
        m_ConfigAdmin = configurationAdmin;
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
        return Namespace.ConfigAdmin;
    }

    @Override //NOCHECKSTYLE Max cyclomatic complexity reached. Need to handle all messages for the namespace.
    public void handleMessage(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
        final RemoteChannel channel) throws IOException  
    {
        //parse config admin message
        final ConfigAdminNamespace configMessage = ConfigAdminNamespace.parseFrom(payload.getNamespaceMessage());
        final Message dataMessage;
        
        switch (configMessage.getType())
        {
            case GetPropertyKeysRequest:
                dataMessage = getPropertyKeys(message, configMessage, channel);
                break;
            case GetPropertyKeysResponse:
                dataMessage = GetPropertyKeysResponseData.parseFrom(configMessage.getData());
                break;
            case GetPropertyRequest:
                dataMessage = getProperty(message, configMessage, channel);
                break;
            case GetPropertyResponse:
                dataMessage = GetPropertyResponseData.parseFrom(configMessage.getData());
                break;
            case SetPropertyRequest:
                dataMessage = setProperty(message, configMessage, channel);
                break;
            case SetPropertyResponse:
                dataMessage = null;
                break;
            case GetConfigurationInfoRequest:
                dataMessage = getConfiguration(message, configMessage, channel);
                break;
            case GetConfigurationInfoResponse:
                dataMessage = GetConfigurationInfoResponseData.parseFrom(configMessage.getData());
                break;
            case CreateFactoryConfigurationRequest:
                dataMessage = createFactoryConfig(message, configMessage, channel);
                break;
            case CreateFactoryConfigurationResponse:
                dataMessage = CreateFactoryConfigurationResponseData.parseFrom(configMessage.getData());
                break;
            case DeleteConfigurationRequest:
                dataMessage = deleteConfig(message, configMessage, channel);
                break;
            case DeleteConfigurationResponse:
                dataMessage = DeleteConfigurationResponseData.parseFrom(configMessage.getData());
                break;
            case ConfigAdminNamespaceError:
                dataMessage = ConfigAdminNamespaceErrorData.parseFrom(configMessage.getData());
                break;
            default:
                throw new UnsupportedOperationException(
                        String.format("Message type: %s is not a supported type for"
                                + " the ConfigAdminMessageService namespace.", configMessage.getType()));
        }
        
        // locally post event that message was received
        final Event event = RemoteInterfaceUtilities.createMessageReceivedEvent(message, payload, configMessage, 
                configMessage.getType(), dataMessage, channel);
        m_EventAdmin.postEvent(event);
    }
    
    /**
     * Handle the get property keys request.
     * 
     * @param request
     *      entire remote message for the request
     * @param message
     *      the message containing the get property keys message
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      thrown if the message cannot be parsed correctly 
     * @return
     *      the data message for this request
     */
    private Message getPropertyKeys(final TerraHarvestMessage request, final ConfigAdminNamespace message, 
            final RemoteChannel channel) throws IOException
    {
        final GetPropertyKeysRequestData propMessage = GetPropertyKeysRequestData.parseFrom(message.getData());
        final String pid = propMessage.getPid();
        final Configuration configuration;
        
        //get or create configuration
        try
        {
            //call to configuration admin to retrieve the configuration for a pid must use
            //the null location in the case that it is retrieving a configuration that has
            //not yet been bound to a service with the corresponding pid. If there happens to
            //be such a configuration, then the call without the null location leads to 
            //the configuration being assigned to the calling bundle. In this case,
            //it is assigned to the remote interface. Calling with null avoids this situation.
            configuration = m_ConfigAdmin.getConfiguration(pid, null);
        }
        catch (final IOException exception)
        {
            final String errorDesc = String.format("Unable to find a configuration for %s.", pid);
            m_Logging.error(exception, errorDesc);
            final ConfigAdminNamespaceErrorData error = ConfigAdminNamespaceErrorData.newBuilder().
                setError(ConfigAdminErrorCode.ConfigurationPersistentStorageError).
                setErrorDescription(errorDesc + exception.getMessage()).build();
            m_MessageFactory.createConfigAdminResponseMessage(request, 
                ConfigAdminMessageType.ConfigAdminNamespaceError, error).queue(channel);
            return propMessage;
        }
        
        final Dictionary<String, Object> dictionary = configuration.getProperties();

        //message for response
        final GetPropertyKeysResponseData.Builder response = GetPropertyKeysResponseData.newBuilder().
            setPid(propMessage.getPid()); 
        
        //get properties can return null
        if (dictionary == null)
        {
            m_Logging.info("Properties were not found for the PID:%s", propMessage.getPid());
        }
        else
        {        
            final Enumeration<String> keys = dictionary.keys();
        
            while (keys.hasMoreElements())
            {
                response.addKey(keys.nextElement());
            }
        }
        
        //send response
        m_MessageFactory.createConfigAdminResponseMessage(request, ConfigAdminMessageType.GetPropertyKeysResponse, 
                response.build()).queue(channel);
        
        return propMessage;
    }    
    
    /**
     * Handle the get property request.
     * @param request
     *      entire remote message for the request
     * @param message
     *      the message that contains the get property request
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      thrown if the message cannot be parsed correctly
     * @return
     *      the data message for this request 
     */
    private Message getProperty(final TerraHarvestMessage request, final ConfigAdminNamespace message, 
            final RemoteChannel channel) throws IOException
    {
        final GetPropertyRequestData propMessage = GetPropertyRequestData.parseFrom(message.getData());
        final String pid = propMessage.getPid();
        final Configuration configuration;
        
        //get or create configuration
        try
        {
            //call to configuration admin to retrieve the configuration for a pid must use
            //the null location in the case that it is retrieving a configuration that has
            //not yet been bound to a service with the corresponding pid. If there happens to
            //be such a configuration, then the call without the null location leads to 
            //the configuration being assigned to the calling bundle. In this case,
            //it is assigned to the remote interface. Calling with null avoids this situation.
            configuration = m_ConfigAdmin.getConfiguration(pid, null);
        }
        catch (final IOException exception)
        {
            final String errorDesc = String.format("Unable to find a configuration with PID %s.", pid);
            m_Logging.error(exception, errorDesc);
            final ConfigAdminNamespaceErrorData error = ConfigAdminNamespaceErrorData.newBuilder().
                setError(ConfigAdminErrorCode.ConfigurationPersistentStorageError).
                setErrorDescription(errorDesc + exception.getMessage()).build();
            m_MessageFactory.createConfigAdminResponseMessage(request, 
                ConfigAdminMessageType.ConfigAdminNamespaceError, error).queue(channel);
            return propMessage;
        }
            
        final Dictionary<String, Object> dictionary = configuration.getProperties();
            
        Object value = null;
        if (dictionary != null)
        {
            value = dictionary.get(propMessage.getKey());
        }
        if (value == null)
        {
            final String errorString = String.format("The key [%s] was not found for configuration with pid %s", 
                propMessage.getKey(), pid);
            m_Logging.error(errorString);
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                errorString).queue(channel);
            return propMessage;
        }
        final Multitype multitypeValue = SharedMessageUtils.convertObjectToMultitype(value);
        m_MessageFactory.createConfigAdminResponseMessage(request, ConfigAdminMessageType.GetPropertyResponse,
                GetPropertyResponseData.newBuilder().
                    setKey(propMessage.getKey()).
                    setValue(multitypeValue).
                    setPid(pid).build()).queue(channel);
        
        return propMessage;
    } 
    
    /**
     * Handle the set property request.
     * @param request
     *      entire remote message for the request
     * @param message
     *      the config admin message containing the data to process
     * @param channel
     *      channel to use for sending a response
     * @throws IOException 
     *      thrown if the message cannot be parsed correctly
     * @return
     *      the data message for this request
     */
    private Message setProperty(final TerraHarvestMessage request, final ConfigAdminNamespace message, 
            final RemoteChannel channel) throws IOException
    {  
        final SetPropertyRequestData propMessage = SetPropertyRequestData.parseFrom(message.getData());
        final String pid = propMessage.getPid();
        final Configuration configuration;
        
        //get or create configuration
        try
        {
            //call to configuration admin to retrieve the configuration for a pid must use
            //the null location in the case that it is retrieving a configuration that has
            //not yet been bound to a service with the corresponding pid. If there happens to
            //be such a configuration, then the call without the null location leads to 
            //the configuration being assigned to the calling bundle. In this case,
            //it is assigned to the remote interface. Calling with null avoids this situation.
            configuration = m_ConfigAdmin.getConfiguration(pid, null);
        }
        catch (final IOException exception)
        {
            final String errorDesc = String.format("Unable to create/update configuration with PID %s.", pid);
            m_Logging.error(exception, errorDesc);
            final ConfigAdminNamespaceErrorData error = ConfigAdminNamespaceErrorData.newBuilder().
                setError(ConfigAdminErrorCode.ConfigurationPersistentStorageError).
                setErrorDescription(errorDesc + exception.getMessage()).build();
            m_MessageFactory.createConfigAdminResponseMessage(request, 
                ConfigAdminMessageType.ConfigAdminNamespaceError, error).queue(channel);
            return propMessage;
        }

        Dictionary<String, Object> dictionary = configuration.getProperties();
             
        if (dictionary == null)
        {
            dictionary = new Hashtable<String, Object>();
        }

        //Loops through the list of all properties within the received SetPropertyRequestData message and sets
        //the associated configuration property values.
        final StringBuilder changedProperties = new StringBuilder();
        for (SimpleTypesMapEntry property: propMessage.getPropertiesList())   
        {
            final Object value = SharedMessageUtils.convertMultitypeToObject(property.getValue());
            dictionary.put(property.getKey(), value);
            changedProperties.append(String.format("{%s=%s} ", property.getKey(), value.toString()));
        }
        configuration.update(dictionary);
        m_Logging.info("Configuration %s updated with property values %s", propMessage.getPid(), changedProperties);
        m_MessageFactory.createConfigAdminResponseMessage(request, ConfigAdminMessageType.SetPropertyResponse, 
                null).queue(channel);
        return propMessage;
    }
    
    /**
     * Handle the get configuration information request.
     * @param request
     *      entire remote message for the request
     * @param message
     *      the config admin message that contains the request for configuration information
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      thrown if the message cannot be parsed correctly 
     * @return
     *      the data message for this request
     */
    private Message getConfiguration(final TerraHarvestMessage request, final ConfigAdminNamespace message, 
            final RemoteChannel channel) throws IOException
    {
        final GetConfigurationInfoRequestData propMessage = GetConfigurationInfoRequestData.parseFrom(
            message.getData());
        Configuration[] configurations = null;
        final String filter = propMessage.getFilter().equals("") ? null : propMessage.getFilter(); //NOPMD: assigning
        try       //to null - done because if the filter is not set need to use null to return all configurations.
        {
            configurations = m_ConfigAdmin.listConfigurations(filter);
        }
        catch (final InvalidSyntaxException exception) 
        {
            final String errorDescription = String.format(
                "The filter's syntax contained in the the message from %d is invalid", request.getSourceId());

            m_Logging.error(exception, errorDescription);
 
            m_MessageFactory.createBaseErrorMessage(request, ErrorCode.INVALID_VALUE, 
                String.format("The syntax of the filter %s is not correct %s", filter, exception.getMessage())).
                queue(channel);
            return propMessage;
        }
        final GetConfigurationInfoResponseData.Builder response = GetConfigurationInfoResponseData.newBuilder();
        if (configurations != null)
        {  
            for (Configuration config : configurations)
            {
                final String location = config.getBundleLocation();
                final String factoryPid = config.getFactoryPid();
                final String pid = config.getPid();
                final ConfigurationInfoType.Builder configTypeBuilder = ConfigurationInfoType.newBuilder();
                // to avoid a null pointer in the event that the factory pid is not applicable
                if (factoryPid != null)
                {
                    //if factory pid is null do not add it to the configuration message
                    configTypeBuilder.setFactoryPid(factoryPid);
                }
                if (location != null)
                {
                    configTypeBuilder.setBundleLocation(location);
                }
                //add configuration properties to message if desired.
                if (propMessage.getIncludeProperties())
                {
                    final Dictionary<String, Object> dictionary = config.getProperties();
                    final Enumeration<String> keyEnum = dictionary.keys();
                    while (keyEnum.hasMoreElements())
                    {
                        final String key = keyEnum.nextElement();
                        final Multitype value = SharedMessageUtils.convertObjectToMultitype(dictionary.get(key));
                        final SimpleTypesMapEntry property = 
                                SimpleTypesMapEntry.newBuilder().setKey(key).setValue(value).build();
                        configTypeBuilder.addProperties(property);
                    }
                }
                
                // add other fields to message
                configTypeBuilder.setPid(pid).build();
                //add to message's list of configurations
                response.addConfigurations(configTypeBuilder);
            }
        }

        m_MessageFactory.createConfigAdminResponseMessage(request, 
                ConfigAdminMessageType.GetConfigurationInfoResponse, response.build()).queue(channel);
        
        return propMessage;
    }
    
    /**
     * Handle the request to create a factory configuration.
     * @param request
     *      entire remote message for the request
     * @param message
     *      the config admin message that contains the request to create a factory configuration
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      thrown if the message cannot be parsed correctly 
     * @return
     *     the data message associated with the request
     */ 
    private Message createFactoryConfig(final TerraHarvestMessage request, final ConfigAdminNamespace message, 
        final RemoteChannel channel) throws IOException
    {
        //parse request
        final CreateFactoryConfigurationRequestData propMessage = CreateFactoryConfigurationRequestData.parseFrom(
            message.getData());
        final String factoryPid = propMessage.getFactoryPid();
        final List<SimpleTypesMapEntry> listProps = propMessage.getFactoryPropertyList();    

        if (listProps.isEmpty())
        {
            final String errorDescription = 
                    String.format("Missing properties, unable to create a factory configuration");
            
            final ConfigAdminNamespaceErrorData error = ConfigAdminNamespaceErrorData.newBuilder().
                    setError(ConfigAdminErrorCode.MissingPropertyError).
                    setErrorDescription(errorDescription).build();

            //send message
            m_MessageFactory.createConfigAdminResponseMessage(request, 
                    ConfigAdminMessageType.ConfigAdminNamespaceError, error).queue(channel);
            
            return propMessage;         
        }
        final Dictionary<String, Object> props = SharedMessageUtils.convertMaptoDictionary(listProps);

        //response builder
        final CreateFactoryConfigurationResponseData.Builder responseBuilder = 
            CreateFactoryConfigurationResponseData.newBuilder();
        
        //create the configuration
        try
        {
            final Configuration config = m_ConfigAdmin.createFactoryConfiguration(factoryPid, null);
            config.update(props);
            responseBuilder.setPid(config.getPid());
        }
        catch (final IOException exception)
        {
            final String errorDescription = String.format("Unable to create a configuration from Factory PID %s", 
                factoryPid);

            m_Logging.error(exception, errorDescription);
            final ConfigAdminNamespaceErrorData error = ConfigAdminNamespaceErrorData.newBuilder().
                setError(ConfigAdminErrorCode.ConfigurationPersistentStorageError).
                setErrorDescription(errorDescription + exception.getMessage()).build();

            //send message
            m_MessageFactory.createConfigAdminResponseMessage(request, 
                ConfigAdminMessageType.ConfigAdminNamespaceError, error).queue(channel);
            return propMessage;
        }

        //send response
        m_MessageFactory.createConfigAdminResponseMessage(request, 
            ConfigAdminMessageType.CreateFactoryConfigurationResponse, responseBuilder.build()).queue(channel);
        return propMessage;
    }
    
    /**
     * Handle the request to remove a configuration.
     * @param request
     *      entire remote message for the request
     * @param message
     *      the config admin message that contains the request to remove a configuration
     * @param channel
     *      channel to use for sending a response
     * @throws IOException
     *      thrown if the message cannot be parsed correctly 
     * @return
     *     the data message associated with the request
     */ 
    private Message deleteConfig(final TerraHarvestMessage request, final ConfigAdminNamespace message, 
        final RemoteChannel channel) throws IOException
    {
        //parse the request data
        final DeleteConfigurationRequestData removeData = DeleteConfigurationRequestData.parseFrom(message.getData());
        final String pid = removeData.getPid();
        
        //remove the configuration
        try
        {
            //call to configuration admin to retrieve the configuration for a pid must use
            //the null location in the case that it is retrieving a configuration that has
            //not yet been bound to a service with the corresponding pid. If there happens to
            //be such a configuration, then the call without the null location leads to 
            //the configuration being assigned to the calling bundle. In this case,
            //it is assigned to the remote interface. Calling with null avoids this situation.
            final Configuration config = m_ConfigAdmin.getConfiguration(pid, null);
            config.delete();
        }
        catch (final IOException exception)
        {
            final String errorDescription = String.format("Unable to delete configuration with PID %s", pid);

            m_Logging.error(exception, errorDescription);
            final ConfigAdminNamespaceErrorData error = ConfigAdminNamespaceErrorData.newBuilder().
                setError(ConfigAdminErrorCode.ConfigurationPersistentStorageError).
                setErrorDescription(errorDescription + exception.getMessage()).build();

            //send message
            m_MessageFactory.createConfigAdminResponseMessage(request, 
                ConfigAdminMessageType.ConfigAdminNamespaceError, error).queue(channel);
            return removeData;
        }
        
        //send response
        final DeleteConfigurationResponseData response = DeleteConfigurationResponseData.newBuilder().
            setPid(pid).build();
        m_MessageFactory.createConfigAdminResponseMessage(request, 
            ConfigAdminMessageType.DeleteConfigurationResponse, response).queue(channel);
        return removeData;
    }
}
