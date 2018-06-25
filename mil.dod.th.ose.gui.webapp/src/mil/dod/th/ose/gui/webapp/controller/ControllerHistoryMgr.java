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
package mil.dod.th.ose.gui.webapp.controller;

import java.net.InetAddress;
import java.util.Collection;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.inject.Inject;

import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.core.persistence.PersistentData;
import mil.dod.th.core.persistence.PersistentDataStore;
import mil.dod.th.core.remote.RemoteChannel;
import mil.dod.th.core.remote.RemoteChannelLookup;
import mil.dod.th.core.remote.RemoteConstants;
import mil.dod.th.core.remote.SocketChannel;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BaseMessages.BaseNamespace.BaseMessageType;
import mil.dod.th.core.remote.proto.BaseMessages.ControllerInfoData;
import mil.dod.th.core.remote.proto.RemoteBase.Namespace;
import mil.dod.th.core.validator.ValidationFailedException;
import mil.dod.th.ose.gui.webapp.channel.ChannelMgr;
import mil.dod.th.ose.gui.webapp.controller.history.ControllerHistory;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import org.glassfish.osgicdi.OSGiService;

/**
 * Class that maintains the history of all controllers that have been connected to by the system.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "controllerHistMgr")
@ApplicationScoped
public class ControllerHistoryMgr
{
    private static final String ERROR_TITLE = "Error Updating data.";
    private static final String ERROR_MSG = "Unable to update controller history data.";
    
    @Inject @OSGiService
    private PersistentDataStore m_PersistentDataStore;
    
    @Inject @OSGiService
    private RemoteChannelLookup m_RmtChnLkp;
    
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    private ControllerInfoHelper m_ControllerInfoHelper;
    private ChannelEventHelper m_ChannelEventHelper;
    
    public void setPersistentDataStore(final PersistentDataStore persistentDataStore)
    {
        m_PersistentDataStore = persistentDataStore;
    }
    
    public void setRemoteChannelLookup(final RemoteChannelLookup rmtChnLkp)
    {
        m_RmtChnLkp = rmtChnLkp;
    }
    
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    public void setBundleUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    public void setGrowlUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Instantiates and registered needed listeners.
     */
    @PostConstruct
    public void setupHistoryManager()
    {
        m_ControllerInfoHelper = new ControllerInfoHelper();
        m_ChannelEventHelper = new ChannelEventHelper();
        
        m_ControllerInfoHelper.registerControllerInfoEvents();
        m_ChannelEventHelper.registerChannelEvents();
        
        for (Entry<Integer, Set<RemoteChannel>> controllerEntry: m_RmtChnLkp.getAllChannels().entrySet())
        {
            for (RemoteChannel channel : controllerEntry.getValue())
            {
                if (channel instanceof SocketChannel)
                {
                    handleSocketData(controllerEntry.getKey(), (SocketChannel)channel);
                    m_MessageFactory.createBaseMessage(BaseMessageType.RequestControllerInfo, null)
                        .queue(controllerEntry.getKey(), null);
                }
            }
        }
    }
    
    /**
     * Unregisters all listeners that have been registered by the controller history manager.
     */
    @PreDestroy
    public void unregisterEventHelpers()
    {
        m_ControllerInfoHelper.unregisterListener();
        m_ChannelEventHelper.unregisterListener();
    }
    
    /**
     * Returns a map containing information on the last 9 controllers the system has been connected to.
     * 
     * @return
     *      Map containing the information on the last 9 controllers the system has been connected to. The key is
     *      the name that should be displayed for the controller and the value is the specified information needed
     *      to reconnect to the controller.
     */
    public Map<String, ControllerHistory> getControllerHistory()
    {
        final Collection<PersistentData> retrievedHistory = m_PersistentDataStore.query(ControllerHistory.class);
        final Iterator<PersistentData> iter = retrievedHistory.iterator();
        final Set<ControllerHistory> sortedControllerHistory = new TreeSet<>(new Comparator<ControllerHistory>()
        {
            @Override
            public int compare(final ControllerHistory o1, final ControllerHistory o2)
            {
                if (o2.getLastConnected() > o1.getLastConnected())
                {
                    return 1;
                }
                else if (o2.getLastConnected() < o1.getLastConnected())
                {
                    return -1;
                }
                return 0;
            }
        });
        final String localhostName = "localhost";
        ControllerHistory localhost = null;
        while (iter.hasNext())
        {
            final PersistentData data = iter.next();
            final ControllerHistory controllerData = (ControllerHistory)data.getEntity();
            sortedControllerHistory.add(controllerData);
            
            if ((controllerData.getHostName().equals(localhostName) 
                    || controllerData.getHostName().equals(InetAddress.getLoopbackAddress().getHostAddress()))
                    && controllerData.getPort() == ChannelMgr.DEFAULT_PORT)
            {
                localhost = controllerData;
            }
        }
        
        final Map<String, ControllerHistory> historyMap = new LinkedHashMap<>();
        if (localhost == null)
        {
            localhost = new ControllerHistory();
            localhost.setHostName(localhostName);
            localhost.setPort(ChannelMgr.DEFAULT_PORT);
        }
        historyMap.put(createDisplayName(localhost), localhost);
        
        final Iterator<ControllerHistory> sortedIter = sortedControllerHistory.iterator();
        final int listMax = 9;
        int count = 0;
        while (sortedIter.hasNext() && count < listMax)
        {
            final ControllerHistory controllerData = sortedIter.next();
            if (!controllerData.equals(localhost))
            {
                historyMap.put(createDisplayName(controllerData), controllerData);
                count++;
            }
        }
        return historyMap;
    }
    
    /**
     * Method that creates the name that should be used to display the controller history.
     * 
     * @param controllerHistory
     *      The controller to create a display name for.
     * @return
     *      The name to be displayed for the controller.
     */
    private String createDisplayName(final ControllerHistory controllerHistory)
    {
        final String controllerName = 
                controllerHistory.getControllerName() == null ? "" : ":" + controllerHistory.getControllerName();
        return String.format("%s%s", controllerHistory.getHostName(), controllerName);
    }
    
    /**
     * Method that creates the description that is unique to each persisted controller history.
     * 
     * @param socket
     *      Socket that contains host and port associated with the controller history.
     * @return
     *      String that represents the unique description for the controller history associated with the socket.
     */
    private String createDataDescription(final SocketChannel socket)
    {
        return String.format("%s:%s", socket.getHost(), socket.getPort());
    }
    
    /**
     * Method that handles adding or updating controller history for the specified socket channel.
     * 
     * @param controllerId
     *      Controller ID the socket channel is associated with.
     * @param socketChannel
     *      Socket channel to update or create a controller history record for.
     */
    private void handleSocketData(final int controllerId, final SocketChannel socketChannel)
    {
        final Collection<PersistentData> controllerCollection = 
                m_PersistentDataStore.query(ControllerHistory.class, createDataDescription(socketChannel));
        if (controllerCollection.size() == 1)
        {
            final PersistentData data = controllerCollection.iterator().next();
            final ControllerHistory controllerData = (ControllerHistory)data.getEntity();
            controllerData.setControllerId(controllerId);
            controllerData.setSslEnabled(socketChannel.isSslEnabled());
            controllerData.setLastConnected(System.currentTimeMillis());
            data.setEntity(controllerData);
            try
            {
                m_PersistentDataStore.merge(data);
            }
            catch (final IllegalArgumentException | PersistenceFailedException | ValidationFailedException ex)
            {
                m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, ERROR_TITLE, 
                        ERROR_MSG, ex);
            }
        }
        else if (controllerCollection.isEmpty())
        {
            final ControllerHistory controllerData = new ControllerHistory();
            controllerData.setControllerId(controllerId);
            controllerData.setHostName(socketChannel.getHost());
            controllerData.setPort(socketChannel.getPort());
            controllerData.setSslEnabled(socketChannel.isSslEnabled());
            controllerData.setLastConnected(System.currentTimeMillis());
            m_PersistentDataStore.persist(ControllerHistory.class, UUID.randomUUID(), 
                    createDataDescription(socketChannel), controllerData);
        }
    }
    
    /**
     * Listener that listens for controller information events and updates controller history records based on the 
     * received information.
     */
    class ControllerInfoHelper implements EventHandler
    {        
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler for the message received topic.
         */
        public void registerControllerInfoEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to required remote responses
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, RemoteConstants.TOPIC_MESSAGE_RECEIVED);
            final String filterString = String.format("(&(%s=%s)(|(%s=%s)))", 
                RemoteConstants.EVENT_PROP_NAMESPACE, Namespace.Base.toString(), 
                RemoteConstants.EVENT_PROP_MESSAGE_TYPE, BaseMessageType.ControllerInfo.toString());
            props.put(EventConstants.EVENT_FILTER, filterString);
            
            //register the event handler 
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {  
            //get the controller id from the message that triggered this event
            final RemoteChannel remoteChannel = 
                    (RemoteChannel)event.getProperty(RemoteConstants.EVENT_PROP_CHANNEL);
            if (remoteChannel instanceof SocketChannel)
            {
                final SocketChannel socketChannel = (SocketChannel)remoteChannel;
                final Collection<PersistentData> controllerCollection = 
                        m_PersistentDataStore.query(ControllerHistory.class, createDataDescription(socketChannel));
                
                if (controllerCollection.size() == 1)
                {
                    final ControllerInfoData infoMessage = 
                            (ControllerInfoData)event.getProperty(RemoteConstants.EVENT_PROP_DATA_MESSAGE);
                    final PersistentData data = controllerCollection.iterator().next();
                    final ControllerHistory controllerData = (ControllerHistory)data.getEntity();
                    controllerData.setControllerName(infoMessage.getName());
                    data.setEntity(controllerData);
                    try
                    {
                        m_PersistentDataStore.merge(data);
                    }
                    catch (final IllegalArgumentException | PersistenceFailedException | ValidationFailedException ex)
                    {
                        m_GrowlUtil.createGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, ERROR_TITLE, 
                                ERROR_MSG, ex);
                    }
                }
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
     * Class to listen for channel added events and creates a channel history record for the connected system.
     */
    class ChannelEventHelper implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register this event handler for the message received topic.
         */
        public void registerChannelEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen to channel updated or removed events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] channelAddRemove = {RemoteChannelLookup.TOPIC_CHANNEL_UPDATED};
            props.put(EventConstants.EVENT_TOPIC, channelAddRemove);
            
            //register the event handler
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final RemoteChannel remoteChannel = 
                    (RemoteChannel)event.getProperty(RemoteConstants.EVENT_PROP_CHANNEL);
            final int controllerId = (Integer)event.getProperty(RemoteConstants.EVENT_PROP_SYS_ID);
           
            if (remoteChannel instanceof SocketChannel && controllerId != Integer.MAX_VALUE)
            {
                final SocketChannel socketChannel = (SocketChannel)remoteChannel;
                handleSocketData(controllerId, socketChannel);
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
}
