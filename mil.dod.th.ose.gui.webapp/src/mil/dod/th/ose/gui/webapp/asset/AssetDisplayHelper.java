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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.inject.Inject;

import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.factory.AbstractFactoryObjectDisplayHelper;
import mil.dod.th.ose.gui.webapp.utils.BundleContextUtil;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

/**
 * Implementation of the {@link AbstractFactoryObjectDisplayHelper} bean for the asset feature.
 * @author callen
 *
 */
@ManagedBean (name = "assetDisplay")
@SessionScoped
public class AssetDisplayHelper extends AbstractFactoryObjectDisplayHelper 
{
    /**
     * The asset manager service used to get the list of asset models.
     */
    @ManagedProperty(value = "#{assetMgr}")
    private AssetMgr assetMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the active controller. Used to render the list of assets for only that controller that is
     * set as active.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference the bundle context utility.
     */
    @Inject
    private BundleContextUtil m_BundleUtil;
    
    /**
     * Event handler that listens for active controller changed events.
     */
    private ActiveControllerEventHandler m_ActiveControllerListener;

    /**
     * Get the active controller and asset manager service.
     */
    @PostConstruct
    public void setupDependencies()
    {   
        m_ActiveControllerListener = new ActiveControllerEventHandler();
        m_ActiveControllerListener.registerActiveControllerEvents();
    }
    
    /**
     * Pre destroy method that unregisters all event handlers before the bean is destroyed.
     */
    @PreDestroy
    public void unregisterEvents()
    {
        m_ActiveControllerListener.unregisterListener();
    }
    
    /**
     * Set the asset manager service to use. 
     * @param assetManager
     *     the asset manager service
     */
    public void setAssetMgr(final AssetMgr assetManager)
    {
        assetMgr = assetManager;
    }
    
    
    /**
     * Set the active controller service to use. 
     * @param activeControll
     *     the active controller service
     */
    public void setActiveController(final ActiveController activeControll)
    {
        activeController = activeControll;
    }
    
    /**
     * Set the bundle context utility service to use.
     * @param bundleUtil
     *      the bundle context utility service.
     */
    public void setBundleContextUtil(final BundleContextUtil bundleUtil)
    {
        m_BundleUtil = bundleUtil;
    }

    @Override
    public List<AssetModel> getFactoryObjectListAsync()
    {
        return assetMgr.getAssetsForControllerAsync(activeController.getActiveController().getId());
    }

    @Override
    public String getFeatureTitle()
    {
        return "Asset";
    }
    
    /**
     * Event handler that listens for active controller changed events and then resets the currently selected asset to
     * null.
     */
    class ActiveControllerEventHandler implements EventHandler
    {
        /**
         * Service registration for the listener service. Saved for unregistering the service when the bean is 
         * destroyed.
         */
        @SuppressWarnings("rawtypes") //TODO TH-534: investigate the issue with parameterizing the service reg.
        private ServiceRegistration m_Registration;
        
        /**
         * Method that registers all controller events to listen for.
         */
        public void registerActiveControllerEvents()
        {
            final BundleContext context = m_BundleUtil.getBundleContext();
            // register to listen for active controller changed events.
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put(EventConstants.EVENT_TOPIC, ActiveController.TOPIC_ACTIVE_CONTROLLER_CHANGED);
            
            //register the event handler that listens for active controller changing.
            m_Registration = context.registerService(EventHandler.class, this, props);
        }

        @Override
        public void handleEvent(final Event event)
        {
            setSelectedFactoryObject(null);
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
