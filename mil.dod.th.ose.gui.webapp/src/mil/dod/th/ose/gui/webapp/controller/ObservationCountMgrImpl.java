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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.asset.AssetMgr;
import mil.dod.th.ose.gui.webapp.asset.AssetMgrImpl;
import mil.dod.th.ose.gui.webapp.asset.AssetModel;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Implementations of the {@link ObservationCountMgr} class.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "observationCountMgr", eager = true)
@SessionScoped
public class ObservationCountMgrImpl implements ObservationCountMgr
{
    /**
     * Reference to the OSGi event admin service.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Reference to the OSGi observation store service.
     */
    @Inject @OSGiService
    private ObservationStore m_ObsStore;

    /**
     * AssetManager service.
     */
    @ManagedProperty(value = "#{assetMgr}")
    private AssetMgrImpl assetMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Reference to the bundle context utility.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Map that stores the observation count for any known controllers.
     */
    private final Map<Integer, Integer> m_ControllerObsCount = 
            Collections.synchronizedMap(new HashMap<Integer, Integer>());
    
    /**
     * Reference to an instance of the {@link ObservationEventHandler} class.
     */
    private ObservationEventHandler m_ObsHandler;
    
    /**
     * Reference to an instance of the {@link ControllerEventHandler} class.
     */
    private ControllerEventHandler m_ControllerHandler;
    
    /**
     * Post construct method that instantiates the observation event handler and registers the events.
     */
    @PostConstruct
    public void postConstruct()
    {
        m_ObsHandler = new ObservationEventHandler();
        m_ObsHandler.registerForEvents();
        m_ControllerHandler = new ControllerEventHandler();
        m_ControllerHandler.registerControllerEvents();
    }
    
    /**
     * Pre destroy method that unregister events for the observation event handler.
     */
    @PreDestroy
    public void preDestroy()
    {
        m_ObsHandler.unregisterListener();
        m_ControllerHandler.unregisterListener();
    }
    
    /**
     * Method that sets the event admin service.
     * 
     * @param eventAdmin
     *          {@link EventAdmin} service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Method that sets the observation store service.
     * 
     * @param obsStore
     *          {@link ObservationStore} service to be set.
     */
    public void setObservationStore(final ObservationStore obsStore)
    {
        m_ObsStore = obsStore;
    }

    /**
     * Set the asset manager service to use.
     * 
     * @param assetManager
     *     the asset manager service
     */
    public void setAssetMgr(final AssetMgrImpl assetManager)
    {
        assetMgr = assetManager;
    }

    /**
     * Method that sets the bundle context utility.
     * 
     * @param bundleUtil
     *          {@link BundleContextUtil} to be set.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    @Override
    public synchronized int getObservationCount(final int controllerId)
    {
        if (m_ControllerObsCount.containsKey(controllerId))
        {
            return m_ControllerObsCount.get(controllerId);
        }
        else
        {
            m_ControllerObsCount.put(controllerId, 0);
            return m_ControllerObsCount.get(controllerId);
        }
    }
    
    /**
     * Method that increments the number of unread observation for the specified controller by one.
     * 
     * @param controllerId
     *          ID of the controller to increment the number of unread observation by one.
     */
    private synchronized void incrementObsCount(final int controllerId)
    {
        if (m_ControllerObsCount.containsKey(controllerId))
        {
            m_ControllerObsCount.put(controllerId, m_ControllerObsCount.get(controllerId) + 1);
        }
        else
        {
            m_ControllerObsCount.put(controllerId, 1);
        }
        
        //Post observation count updated event.
        postObsCntUpdatedEvent(controllerId);
    }
    
    @Override
    public synchronized void clearObsCount(final int controllerId)
    {
        if (m_ControllerObsCount.containsKey(controllerId))
        {
            m_ControllerObsCount.put(controllerId, 0);
            
            //Post observation count updated event.
            postObsCntUpdatedEvent(controllerId);
        }
    }
    
    /**
     * Method that post an observation count updated event used to trigger push to update the page.
     * @param controllerId
     *  the controller id
     */
    private void postObsCntUpdatedEvent(final int controllerId)
    {
        final HashMap<String, Object> map = new HashMap<>();
        map.put(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID, controllerId);
        map.put(EVENT_PROP_OBS_COUNT, m_ControllerObsCount.get(controllerId));
        
        final Event obsCntUpdated = 
                new Event(TOPIC_OBSERVATION_COUNT_UPDATED, map);
        m_EventAdmin.postEvent(obsCntUpdated);
    }
    
    /**
     * Event handler that listens for observations to be received and then increments the current number of unread 
     * unread observations for the corresponding controller by one.
     */
    class ObservationEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method to register events to listen for..
         */
        public void registerForEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for observation creted events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            //All event topics of interest
            final String[] topics = 
            {
                ObservationStore.TOPIC_OBSERVATION_PERSISTED,
                ObservationStore.TOPIC_OBSERVATION_MERGED
            };
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            //register the event handler that listens for observation created events.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {
            final UUID obsUuid = (UUID)event.getProperty(ObservationStore.EVENT_PROP_OBSERVATION_UUID);
            final Observation obs = m_ObsStore.find(obsUuid);
            final int systemId = obs.getSystemId();
            incrementObsCount(systemId);

            final String sensorId = (String)event.getProperty(ObservationStore.EVENT_PROP_SENSOR_ID);
            final AssetModel asset = assetMgr.getAssetModelByUuid(obs.getAssetUuid(), systemId);
            if (sensorId != null && asset != null)
            {
                // If the sensor ID does not currently exist, add to the model and send event to update the view
                final List<String> sensorIdList = asset.getSensorIds();
                if (!sensorIdList.contains(sensorId))
                {
                    sensorIdList.add(sensorId);

                    final Map<String, Object> props = new HashMap<>();
                    props.put(AssetModel.EVENT_PROP_UUID, obsUuid.toString());

                    final Event assetEvent = new Event(AssetMgr.TOPIC_ASSET_SENSOR_IDS_UPDATED, props);
                    m_EventAdmin.postEvent(assetEvent);
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
     * Event handler that handles remove controller events posted to the event admin service. This handler will remove
     * all meta type information stored for the controller specified in the remove controller event.
     */
    class ControllerEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers the listener to listen for controller events.
         */
        public void registerControllerEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for controller removed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, ControllerMgr.TOPIC_CONTROLLER_REMOVED);
            
            //register the event handler that listens for controllers being removed.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            if (m_ControllerObsCount.containsKey(controllerId))
            {
                m_ControllerObsCount.remove(controllerId);
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
