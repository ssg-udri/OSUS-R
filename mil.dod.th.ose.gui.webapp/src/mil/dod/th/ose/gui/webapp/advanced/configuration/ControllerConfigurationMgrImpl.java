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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import mil.dod.th.ose.config.event.constants.ConfigurationEventConstants;
import mil.dod.th.ose.gui.api.SharedPropertyConstants;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Managed bean that handles all calls from the configuration tab page.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "controllerConfigMgr")
@SessionScoped
public class ControllerConfigurationMgrImpl implements ControllerConfigurationMgr
{       
    /**
     * Reference to the system configuration manager.
     */
    @ManagedProperty(value = "#{configAdminMgr}")
    private SystemConfigurationMgr configAdminMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the configuration wrapper bean.
     */
    @ManagedProperty(value = "#{configWrapper}")
    private ConfigurationWrapper configWrapper; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Map of modifiable configuration + metatype information models. Key is the pid of the configuration and value
     * is the model that represents the configuration.
     */
    private final Map<String, ModifiableConfigMetatypeModel> m_ModifiableConfigMetatypeMap = 
            Collections.synchronizedMap(new HashMap<String, ModifiableConfigMetatypeModel>());
    
    /**
     * String reference used to store the PID of the service and is displayed on the remove configuration dialog box.
     */
    private String m_RemoveConfigPid;
    
    /**
     * Event handler that handles configuration and meta type model updated events.
     */
    private UpdateEventHandler m_UpdateEventHandler;
    
    /**
     * Reference to the bundle context utility.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Reference to the OSGi event admin service.
     */
    @Inject @OSGiService
    private EventAdmin m_EventAdmin;
    
    /**
     * Method that sets the system configuration manager to use.
     * @param sysConfigMgr
     *      system configuration manager to be set.
     */
    public void setConfigAdminMgr(final SystemConfigurationMgr sysConfigMgr)
    {
        this.configAdminMgr = sysConfigMgr;
    }
    
    /**
     * Method that sets the configuration wrapper to use.
     * @param wrapper
     *      configuration wrapper to be set.
     */
    public void setConfigWrapper(final ConfigurationWrapper wrapper)
    {
        this.configWrapper = wrapper;
    }
    
    /**
     * Sets the event admin service to use.
     * 
     * @param eventAdmin
     *          Event admin service to be set.
     */
    public void setEventAdmin(final EventAdmin eventAdmin)
    {
        m_EventAdmin = eventAdmin;
    }
    
    /**
     * Sets the bundle context utility to use.
     * 
     * @param bundleUtil
     *          Bundle context utility to be set.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }
    
    /**
     * Registers the event handlers before the bean is available for use.
     */
    @PostConstruct
    public void registerListener()
    {
        m_UpdateEventHandler = new UpdateEventHandler();
        m_UpdateEventHandler.registerEvents();
    }
    
    /**
     * Unregisters all event handlers before the bean is destroyed.
     */
    @PreDestroy
    public void unregisterListener()
    {
        m_UpdateEventHandler.unregisterListener();
    }
    
    @Override
    public void setRemoveConfigPid(final String pid)
    {
        m_RemoveConfigPid = pid;
    }
    
    @Override
    public String getRemoveConfigPid()
    {
        return m_RemoveConfigPid;
    }
    
    @Override
    public List<ConfigAdminModel> getFactoryConfigurationsAsync(final int controllerId, final String factoryPid)
    {
        if (this.configAdminMgr.getFactoryConfigurationsByFactoryPidAsync(controllerId, factoryPid) != null)
        {
            return new ArrayList<ConfigAdminModel>(this.configAdminMgr.getFactoryConfigurationsByFactoryPidAsync(
                    controllerId, factoryPid).values());
        }
        return new ArrayList<ConfigAdminModel>();
    }
    
    @Override
    public void setConfigurationValuesAsync(final int controllerId, final ModifiableConfigMetatypeModel displayModel)
    {
        this.configWrapper.setConfigurationValueAsync(controllerId, displayModel.getPid(), 
                displayModel.getProperties());
    }
    
    @Override
    public synchronized ModifiableConfigMetatypeModel getConfigModelByPidAsync(final int controllerId, final String pid)
    {        
        if (m_ModifiableConfigMetatypeMap.containsKey(pid))
        {
            return m_ModifiableConfigMetatypeMap.get(pid);
        }
        return tryCreateOrUpdateModelAsync(controllerId, pid);
    }
    
    @Override
    public String getConfigBundleLocationAsync(final int controllerId, final String pid)
    {
        final ConfigAdminModel model = this.configAdminMgr.getConfigurationByPidAsync(controllerId, pid);
        if (model == null)
        {
            return null;
        }
        return model.getBundleLocation();
    }
    
    /**
     * Method used to create or update the configuration model to be displayed.
     * 
     *@param controllerId
     *          ID of the controller that contains the configuration.
     * @param pid
     *          PID of the service to create a configuration model for.
     * @return
     *          The {@link ModifiableConfigMetatypeModel} that represents the configuration being requested. May return
     *          null if there is no information available for the configuration being requested.
     */
    private synchronized ModifiableConfigMetatypeModel tryCreateOrUpdateModelAsync(
            final int controllerId, final String pid)
    {
        final UnmodifiableConfigMetatypeModel configuration = configWrapper
                .getConfigurationByPidAsync(controllerId, pid);
        if (configuration == null)
        {
            m_ModifiableConfigMetatypeMap.remove(pid);
            return null;
        }
        else
        {
            final ModifiableConfigMetatypeModel returnModel = new ModifiableConfigMetatypeModel(configuration.getPid());
            //Add properties to the config model.
            for (UnmodifiablePropertyModel property: configuration.getProperties())
            {
                //make a copy of the values that have been returned by the application
                //scoped bean
                final ModifiablePropertyModel modProp = new 
                        ConfigPropModelImpl((ConfigPropModelImpl)property);
                
                returnModel.getProperties().add(modProp);
            }
            //Add configuration model to map.
            m_ModifiableConfigMetatypeMap.put(pid, returnModel);
            return returnModel;
        }
    }
    
    /**
     * Event handler that listens for meta type or configuration models to be updated and then updates the display
     * models accordingly.
     */
    class UpdateEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Registers the listener to listen for all bundle events posted.
         */
        public void registerEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            
            // register to listen for meta type and config model updated events
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            final String[] topics = new String[]
            {
                SystemMetaTypeMgr.TOPIC_METATYPE_MODEL_UPDATED,
                SystemConfigurationMgr.TOPIC_CONFIG_MODEL_UPDATED
            };
            props.put(EventConstants.EVENT_TOPIC, topics);
            
            //register the event handler that listens for events.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }
        
        @Override
        public void handleEvent(final Event event)
        {           
            final int controllerId = (Integer)event.getProperty(SharedPropertyConstants.EVENT_PROP_CONTROLLER_ID);
            final String pid = (String)event.getProperty(ConfigurationEventConstants.EVENT_PROP_PID);
            if (pid == null || pid.isEmpty())
            {
                synchronized (m_ModifiableConfigMetatypeMap)
                {
                    for (ModifiableConfigMetatypeModel model: new ArrayList<>(m_ModifiableConfigMetatypeMap.values()))
                    {
                        tryCreateOrUpdateModelAsync(controllerId, model.getPid());
                    }
                }
            }
            else
            {
                tryCreateOrUpdateModelAsync(controllerId, pid);
            }
            final Event updatedEvent = new Event(ControllerConfigurationMgr.TOPIC_CONFIG_DISPLAY_MODELS_UPDATED, 
                    new HashMap<String, Object>());
            m_EventAdmin.postEvent(updatedEvent);
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
