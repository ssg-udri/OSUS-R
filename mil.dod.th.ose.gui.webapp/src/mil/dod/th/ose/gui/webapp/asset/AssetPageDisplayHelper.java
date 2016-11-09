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

import java.util.UUID;

import org.primefaces.event.TabChangeEvent;

/**
 * Session scoped bean that helps with displaying the assets page. This bean stores which tab the user is currently on
 * for the assets page. This bean also stored which command is currently open for each asset displayed on the command
 * and control tab of the assets page.
 * 
 * @author cweisenborn
 */
public interface AssetPageDisplayHelper
{

    /**
     * Method used to set the current tab on the asset page. While this method is not actually used it is needed. This
     * is due to the fact that the tab view component is not in an XHTML form tag on the assets page. This means that 
     * the index value can be retrieved but cannot be set, however the setter method is needed or an error will be 
     * thrown by the tab view component when retrieving the value. The tab changed event is listened for and the current
     * index is set when that event is fired.
     * 
     * @param index
     *          The index of the current tab.
     */
    void setAssetPageIndex(int index);

    /**
     * Method used to retrieve the index of the current tab on the asset page.
     * 
     * @return
     *          The index that represents the current tab on the asset page.
     */
    int getAssetPageIndex();

    /**
     * Method used to retrieve the {@link ActiveCommandIndexHolder} which holds the 
     * active indexes of components for an asset.
     * @param controllerId
     *          ID of the controller where the asset is located.
     * @param uuid
     *          UUID of the asset to retrieve the {@link ActiveCommandIndexHolder} object.
     * @return
     *          the {@link ActiveCommandIndexHolder} for the given asset on the given 
     *          controller
     */
    ActiveCommandIndexHolder getAssetCommandIndexHolder(int controllerId, UUID uuid);

    /**
     * Method that is called when an {@link TabChangeEvent} occurs for the tab view component on 
     * the assets page. This method sets the index of the currently open tab so that action/navigation does not reset 
     * the page back to the first tab. This method also determines when the observation tab is being navigated to or 
     * from and clears the the observation count for the currently active controller.
     * 
     * @param event
     *          {@link TabChangeEvent} fired by the tab view component on the assets page.
     */
    void assetTabViewChange(TabChangeEvent event);

    /**
     * Method used to retrieve the name of currently selected tab on the assets page.
     * 
     * @return
     *          String that represents the name of the currently selected tab on the assets page.
     */
    String getAssetPageTabName();
    
    /**
     * Method to set the current tab of the observation tab's tools. While this method is not actually used it is needed
     * to prevent page errors.
     * 
     * @param index
     *          The index of the current tab.
     */
    void setObservationToolsActiveIndex(int index);

    /**
     * Method used to retrieve the active index for the observation tab's tools.
     * 
     * @return
     *          Integer representing which of the tabs in the tools accordion panel should be closed.
     */
    int getObservationToolsActiveIndex();
    
    /**
     * Method that is called when an {@link TabChangeEvent} occurs because the observation tab's tools accordion panel 
     * switches its active tab.
     * 
     * @param event
     *          {@link TabChangeEvent} fired by the accordion panel component on the observation tab.
     */
    void observationToolsTabChange(TabChangeEvent event);
}
