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

/**
 * This class handles the configuration of all bundles on the currently active controller. This bean does not store
 * any data and is just used to display data and perform actions on the configuration tab on system configuration page.
 * 
 * @author cweisenborn
 */
public interface BundleConfigurationMgr
{   
    /**
     * Method that sets the bundle model of bundle being uninstalled.
     * 
     * @param bundle
     *          bundle model being set.
     */
    void setUninstallBundle(BundleModel bundle);
    
    /**
     * Method that returns the bundle model of the bundle being uninstalled.
     *          
     * @return
     *          the bundle model of the bundle being uninstalled.
     */
    BundleModel getUninstallBundle();
    
    /**
     * Set the bundle model to be displayed in the information dialog.
     * 
     * @param bundle
     *          Bundle to be set and displayed in the information dialog.
     */
    void setInfoBundle(BundleModel bundle);

    /**
     * Retrieve the bundle model that is to be displayed in the information dialog.
     * 
     * @return
     *          The bundle model to be displayed.
     */
    BundleModel getInfoBundle();

    /**
     * Sets the lists of bundles to be displayed when a filter is applied.
     * 
     * @param filteredBundles
     *          List of filtered bundles to be set.
     */
    void setFilteredBundles(List<BundleModel> filteredBundles);

    /**
     * Retrieves the list of filtered bundles.
     * 
     * @return
     *          List of filtered bundles.
     */
    List<BundleModel> getFilteredBundles();

    /**
     * Retrieves all bundles associated with the current active controller.
     * 
     * @return
     *          List of all bundles associated with the active controller.
     */
    List<BundleModel> getBundles();

    /**
     * Method that handles sending a request to start the specified bundle.
     * 
     * @param bundleId
     *          ID of the bundle to be started.
     */
    void startBundle(long bundleId);

    /**
     * Method that handles sending a request to stop the specified bundle.
     *  
     * @param bundleId
     *          ID of the bundle to be stopped.
     */
    void stopBundle(long bundleId);

    /**
     * Method that handles sending a request to uninstall the specified bundle.
     * 
     * @param bundleId
     *          ID of the bundle to be uninstalled.
     */
    void uninstallBundle(long bundleId);
}
