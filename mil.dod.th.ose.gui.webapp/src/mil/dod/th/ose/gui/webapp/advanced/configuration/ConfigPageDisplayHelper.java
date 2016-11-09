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

import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigPageDisplayHelperImpl.PanelCollapsedStatus;

import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.ToggleEvent;

/**
 * Class that handles maintaining which configurations on the configuration page are currently expanded/collapsed.
 * 
 * @author cweisenborn
 */
public interface ConfigPageDisplayHelper
{
    /**
     * Method used to retrieve the index of the current tab on the system configuration page.
     * 
     * @return
     *          The index that represents the current tab on the system configuration page.
     */
    int getConfigTabIndex();
    
    /**
     * Method used to set the current tab on the system configuration page. While this method is not actually used it 
     * is needed. This is due to the fact that the tab view component is not in an XHTML form tag on the configuration 
     * page. This means that the index value can be retrieved but cannot be set, however the setter method is needed or 
     * an error will be thrown by the tab view component when retrieving the value. The tab changed event is listened 
     * for and the current index is set when that event is fired.
     * 
     * @param index
     *          The index of the current tab.
     */
    void setConfigTabIndex(int index);
    
    /**
     * Method that retrieves the collapsed status of the specified configuration panel.
     * 
     * @param controllerId
     *      ID of the controller the configuration is located on.
     * @param pid
     *      PID of the configuration to retrieve the status of.
     * @return
     *      {@link PanelCollapsedStatus} object that contains a boolean value used to determine if the panel is 
     *      collapsed or not.
     */
    PanelCollapsedStatus getPanelCollapsedStatus(int controllerId, String pid);

    /**
     * Method that is called when a panel is either collapsed or expanded.
     * 
     * @param event
     *      {@link ToggleEvent} that contains information on which panel was collapsed/expanded.
     */
    void configPanelStatusChanged(ToggleEvent event);

    /**
     * Method that is called when changing tabs on the system configuration page. This method is used to log which tab
     * the user was on last so if they navigate away and then back they will be on same tab as when they left the page.
     * 
     * @param event
     *      {@link TabChangeEvent} that contains information on which tab the was switched to.
     */
    void configTabViewChange(TabChangeEvent event);
}
