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
package mil.dod.th.ose.gui.webapp.advanced;

import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.BundleMessages;
import mil.dod.th.core.remote.proto.BundleMessages.BundleNamespace.BundleMessageType;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link BundleConfigurationMgr} interface.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "bundleConfigurationMgr")
@ViewScoped
public class BundleConfigurationMgrImpl implements BundleConfigurationMgr
{
    /**
     * Reference to the currently active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the bundle manager.
     */
    @ManagedProperty(value = "#{bundleManager}")
    private BundleMgr bundleManager; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Stores the information for the bundle being viewed in the bundle information dialog.
     */
    private BundleModel m_InfoBundle;
    
    /**
     * Stores the information for the bundle being unistalled through the uninstall bundle dialog.
     */
    private BundleModel m_UninstallBundle;
    
    /**
     * Stores a list of filtered bundles.
     */
    private List<BundleModel> m_FilteredBundles;
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
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
     * Set the bundle Manager service to use. 
     * @param bundleMgr
     *     the bundle Manager service
     */
    public void setBundleManager(final BundleMgr bundleMgr)
    {
        bundleManager =  bundleMgr;
    }
    
    /**
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr
     * #setUninstallBundle(mil.dod.th.ose.gui.webapp.advanced.BundleModel)
     */
    @Override
    public void setUninstallBundle(final BundleModel bundle)
    {
        m_UninstallBundle = bundle;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#getUninstallBundle()
     */
    @Override
    public BundleModel getUninstallBundle()
    {
        return m_UninstallBundle;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr
     * #setInfoBundle(mil.dod.th.ose.gui.webapp.advanced.BundleModel)
     */
    @Override
    public void setInfoBundle(final BundleModel bundle)
    {
        m_InfoBundle = bundle;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#getInfoBundle()
     */
    @Override
    public BundleModel getInfoBundle()
    {
        return m_InfoBundle;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#setFilteredBundles(java.util.List)
     */
    @Override
    public void setFilteredBundles(final List<BundleModel> filteredBundles)
    {
        m_FilteredBundles = filteredBundles;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#getFilteredBundles()
     */
    @Override
    public List<BundleModel> getFilteredBundles()
    {
        return m_FilteredBundles;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#getBundles()
     */
    @Override
    public List<BundleModel> getBundles()
    {
        return bundleManager.getBundlesAsync(activeController.getActiveController().getId());
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#startBundle(long)
     */
    @Override
    public void startBundle(final long bundleId)
    {
        final int sysId = activeController.getActiveController().getId();
        final BundleMessages.StartRequestData startRequest = 
                BundleMessages.StartRequestData.newBuilder().setBundleId(bundleId).build();
        m_MessageFactory.createBundleMessage(BundleMessageType.StartRequest, startRequest).
            queue(sysId, null);
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#stopBundle(long)
     */
    @Override
    public void stopBundle(final long bundleId)
    {
        final int sysId = activeController.getActiveController().getId();
        final BundleMessages.StopRequestData stopRequest = 
                BundleMessages.StopRequestData.newBuilder().setBundleId(bundleId).build();
        m_MessageFactory.createBundleMessage(BundleMessageType.StopRequest, stopRequest).
            queue(sysId, null); 
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.advanced.BundleConfigurationMgr#uninstallBundle(long)
     */
    @Override
    public void uninstallBundle(final long bundleId)
    {
        final int sysId = activeController.getActiveController().getId();
        final BundleMessages.UninstallRequestData uninstallRequest = 
                BundleMessages.UninstallRequestData.newBuilder().setBundleId(bundleId).build();
        m_MessageFactory.createBundleMessage(BundleMessageType.UninstallRequest, uninstallRequest).
            queue(sysId, null);
    }
}
