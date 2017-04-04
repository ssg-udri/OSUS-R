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
package mil.dod.th.ose.gui.webapp.asset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.inject.Inject;

import com.google.protobuf.InvalidProtocolBufferException;

import mil.dod.th.core.asset.Asset;
import mil.dod.th.core.asset.capability.CommandCapabilities;
import mil.dod.th.core.asset.commands.Response;
import mil.dod.th.core.factory.FactoryDescriptor;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.ExecuteCommandResponseData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.types.command.CommandResponseEnum;
import mil.dod.th.core.types.command.CommandTypeEnum;
import mil.dod.th.ose.gui.webapp.utils.AssetCommandUtil;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.ose.remote.api.CommandConverter;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.CommandResponseEnumConverter;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Command manager that handles management of {@link AssetGetCommandModel}s and their command responses.
 * 
 * @author nickmarcucci
 */
@ManagedBean(name = "assetGetCommandMgr")
@ApplicationScoped
public class AssetGetCommandMgrImpl implements AssetGetCommandMgr
{
    /**
     * Reference to the {@link AssetTypesMgr} bean.
     */
    @ManagedProperty(value = "#{assetTypesMgr}")
    private AssetTypesMgr assetTypesMgr; //NOCHECKSTYLE must match exactly with the bean name.
    
    /**
     * Reference to the OSGi event admin service.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Reference to service which provides ultilities to aid in command processing
     * operations.
     */
    @Inject @OSGiService
    private CommandConverter m_CommandConverterService;
    
    /**
     * Reference to the growl message utility.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Reference to the bundle context utility.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Reference to the command event handler to be used.
     */
    private CommandEventHandler m_CommandEventHandler;
    
    /**
     * Reference to the get response command event handler to be used.
     */
    private AsyncCommandResponseEventHandler m_CommandResponseEventHandler;
    
    /**
     * Map that is used to store the {@link AssetSyncableCommandModel}s 
     * that represent commands supported by assets. The key is
     * an integer which represents the controller ID where the asset is contained. The value is another map with a key
     * that is the UUID of the asset and the value is the 
     * {@link AssetSyncableCommandModel} that represents the supported commands for the asset.
     */
    private final Map<Integer, Map<UUID, AssetGetCommandModel>> m_AssetCommands = 
            Collections.synchronizedMap(new HashMap<Integer, Map<UUID, AssetGetCommandModel>>());
    
    /**
     * Post construct method that instantiates the required response handler and data converters.
     */
    @PostConstruct
    public void postConstruct()
    {
        m_CommandEventHandler = new CommandEventHandler();
        m_CommandResponseEventHandler = new AsyncCommandResponseEventHandler();
        
        //Register Event Handler.
        m_CommandEventHandler.registerCommandEvents();
        m_CommandResponseEventHandler.registerCommandEvents();
    }
    
    /**
     * Pre destroy method to handle cleaning up registered event handlers.
     */
    @PreDestroy
    public void cleanup()
    {
        m_CommandEventHandler.unregisterListener();
        m_CommandResponseEventHandler.unregisterListener();
    }
    
    /**
     * Method that sets the asset types manager to be used.
     * 
     * @param assetTypeMgr
     *          {@link AssetTypesMgr} to be set.
     */
    public void setAssetTypesMgr(final AssetTypesMgr assetTypeMgr)
    {
        this.assetTypesMgr = assetTypeMgr;
    }
    
    /**
     * Method that sets the command converter service to be used.
     * @param converter
     *          the command converter service to use
     */
    public void setCommandConverter(final CommandConverter converter)
    {
        m_CommandConverterService = converter;
    }
    
    /**
     * Method that sets the event admin service to be used.
     * 
     * @param eventAdmin
     *          The event admin service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method that sets the growl message utility to use.
     * 
     * @param growlUtil
     *          The growl message utility to be used.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Method that sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          The bundle context utility to be used.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    @Override
    public AssetGetCommandModel getAssetCommands(final AssetModel model)
    {
        final int controllerId = model.getControllerId();
       
        final CommandCapabilities capabilities = assetTypesMgr.getAssetFactoryForClassAsync(controllerId, 
                model.getType()).getFactoryCaps().getCommandCapabilities();
        
        final List<CommandTypeEnum> getCommands = retrieveGetOnlyCommands(capabilities);
        
        if (getCommands.size() > 0)
        {
            final UUID uuid = model.getUuid();
            
            if (!m_AssetCommands.containsKey(controllerId))  //NOPMD: Confusing Ternary.  Check to create the hash map
            {                                                //if it doesn't exist. or add the model if there isn't one.
                m_AssetCommands.put(controllerId, new HashMap<UUID, AssetGetCommandModel>());
                m_AssetCommands.get(controllerId).put(uuid, new AssetGetCommandModel(uuid, getCommands));
            }
            else if (!m_AssetCommands.get(controllerId).containsKey(uuid))
            {
                m_AssetCommands.get(controllerId).put(uuid, new AssetGetCommandModel(uuid, getCommands));
            }
            else
            {
                m_AssetCommands.get(controllerId).get(uuid).updateSupportedCommands(getCommands);
            }
            
            return m_AssetCommands.get(controllerId).get(uuid);
        }
        
        return null;
    }
    
    /**
     * Method finds the get commands that do not have corresponding set commands.
     * @param capabilities
     *  the given command capabilities that might contain supported commands which
     *  are get commands
     * @return
     *  the list of get commands that do not have corresponding set commands; if no 
     *  get commands exist are empty
     */
    private List<CommandTypeEnum> retrieveGetOnlyCommands(final CommandCapabilities capabilities)
    {
        final List<CommandTypeEnum> list = new ArrayList<CommandTypeEnum>();
        for (CommandTypeEnum commandType : capabilities.getSupportedCommands())
        {
            if (commandType.toString().startsWith(AssetCommandUtil.GET_COMMAND_PREFIX))
            {
                final String type = commandType.toString().replaceFirst(AssetCommandUtil.GET_COMMAND_PREFIX,
                        AssetCommandUtil.SET_COMMAND_PREFIX);
                
                try
                {
                    //if can't find set command then want to use it.
                    final CommandTypeEnum command = CommandTypeEnum.valueOf(type);

                    //include commands that are complex or don't follow the standard get/set command format
                    if (command.equals(CommandTypeEnum.SET_TUNE_SETTINGS_COMMAND)
                            || command.equals(CommandTypeEnum.SET_LIFT_COMMAND))
                    {
                        list.add(commandType);
                    }
                }
                catch (final IllegalArgumentException exception)
                {
                    list.add(commandType);
                }
            }
        }
        
        return list;
    }

    /**
     * Event handler that handles all execute command responses. This handler posts a growl message for any successfully
     * executed command.
     */
    class CommandEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers the event handler to listen for execute command response messages received.
         */
        public void registerCommandEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for message received events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(%s=%s))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Asset.toString(),
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, AssetMessageType.ExecuteCommandResponse.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler that listens for messages received.
            m_Registration = context.registerService(EventHandler.class, this, props);    
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final ExecuteCommandResponseData response = 
                    (ExecuteCommandResponseData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
            
            final UUID assetUuid = SharedMessageUtils.convertProtoUUIDtoUUID(response.getUuid());
            final int controllerId = (int)event.getProperty(RemoteConstants.EVENT_PROP_SOURCE_ID);
            
            final Map<UUID, AssetGetCommandModel> map = m_AssetCommands.get(controllerId);
            
            if (map == null)
            {
                //Since handle event will be called in the event any command is executed,
                //it may be possible that the map does not have any assets with get commands
                //currently. This is why no log message is created.
                return;
            }
            
            final AssetGetCommandModel model = map.get(assetUuid);
            if (model == null)
            {
                //Since handle event will be called in the event any command is executed,
                //it may be possible that the map does not have any assets with get commands
                //currently. This is why no log message is created.
                return;
            }
            
            final CommandResponseEnum responseType = CommandResponseEnumConverter.convertProtoEnumToJava(
                response.getResponseType());
            final CommandTypeEnum commandType = m_CommandConverterService.
                    getCommandTypeFromResponseType(responseType);
            
            try
            {
                final Date date = new Date(System.currentTimeMillis());
                final Response theResponse =  m_CommandConverterService.
                        getJavaResponseType(response.getResponse().toByteArray(), responseType);
                if (model.trySetResponseByType(commandType, theResponse, date))
                {
                    m_EventAdmin.postEvent(new Event(TOPIC_GET_RESPONSE_RECEIVED, 
                            new HashMap<String, Object>()));
                }
            }
            catch (final InvalidProtocolBufferException | ObjectConverterException exception)
            {
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, 
                        "Unable to handle Get Command Response", 
                        String.format("Received a get command response of type " 
                                + "%s for system 0x%08x for asset with uuid" 
                                + " %s but could not convert that response.", 
                                responseType, controllerId, assetUuid));
                return;
            }
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
    
    /**
     * Event handler that handles all get command responses. These are received as events which are not related
     * to a direct request to execute a command.
     */
    class AsyncCommandResponseEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers the event handler to listen for update command response messages received.
         */
        public void registerCommandEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, 
                    Asset.TOPIC_COMMAND_RESPONSE_UPDATED + RemoteConstants.REMOTE_TOPIC_SUFFIX);
            
            //register the event handler
            m_Registration = context.registerService(EventHandler.class, this, props);    
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final String uuidString = (String)event.getProperty(FactoryDescriptor.EVENT_PROP_OBJ_UUID);
            final UUID assetUuid = UUID.fromString(uuidString);
            final int systemId = (Integer)event.getProperty(RemoteConstants.REMOTE_EVENT_PROP_CONTROLLER_ID);
            
            final Map<UUID, AssetGetCommandModel> map = m_AssetCommands.get(systemId);
            
            if (map == null)
            {
                //no asset information is known for this system Id
                return;
            }
            
            final AssetGetCommandModel model = map.get(assetUuid);
            if (model == null)
            {
                //event content may be for a non-get type command, so ignore
                return;
            }

            final Date date = new Date(System.currentTimeMillis());
            final CommandResponseEnum responseEnum = m_CommandConverterService.getCommandResponseEnumFromClassName(
                    (String)event.getProperty(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE_TYPE));
            final CommandTypeEnum commandType = m_CommandConverterService.getCommandTypeFromResponseType(responseEnum);
            final Response response = (Response)event.getProperty(Asset.EVENT_PROP_ASSET_COMMAND_RESPONSE);
            model.trySetResponseByType(commandType, response, date);
        }
        
        /**
         * Unregister the event listener.
         */
        public void unregisterListener()
        {
            m_Registration.unregister();
        }
    }
}
