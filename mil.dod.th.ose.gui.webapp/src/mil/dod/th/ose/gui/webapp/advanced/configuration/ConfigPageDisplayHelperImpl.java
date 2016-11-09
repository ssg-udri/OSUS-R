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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;

import mil.dod.th.ose.gui.webapp.controller.ActiveController;

import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.ToggleEvent;

/**
 * Implementation of {@link ConfigPageDisplayHelper}.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "configPageDisplayHelper")
@SessionScoped
public class ConfigPageDisplayHelperImpl implements ConfigPageDisplayHelper
{
    /**
     * ID of a configuration header component within the systemconfig_configuration_tab.xhtml page.
     */
    private static final String CONFIG_HEADER_ID = "configHeader";
    
    /**
     * ID of a factory header component within the systemconfig_configuration_tab.xhtml page.
     */
    private static final String FACTORY_HEADER_ID = "factoryHeader";
    
    /**
     * ID of a factory configuration header component within the systemconfig_configuration_tab.xhtml page.
     */
    private static final String FACTORY_CONFIG_HEADER_ID = "factoryConfigHeader";
    
    /**
     * Map used to store the index of the open command tab for each asset. Key is controller id value is another map 
     * with a the key being the PID of the configuration or factory and the value being a model that represents the
     * state of the panel that displays the configuration/factory.
     */
    private final Map<Integer, Map<String, PanelCollapsedStatus>> m_ConfigPanelCollapsedStatus = 
            Collections.synchronizedMap(new HashMap<Integer, Map<String, PanelCollapsedStatus>>());
    
    /**
     * Managed bean that contains information about the currently active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Variable used to store the current tab on the system configuration page.
     */
    private Integer m_ConfigTabIndex = 0;
    
    /**
     * Method that sets the reference to the active controller bean.
     * 
     * @param activeCtlr
     *      The {@link ActiveController} to set.
     */
    public void setActiveController(final ActiveController activeCtlr)
    {
        activeController = activeCtlr;
    }
    
    @Override
    public int getConfigTabIndex()
    {
        return m_ConfigTabIndex;
    }
    
    @Override
    public void setConfigTabIndex(final int index)
    {
        m_ConfigTabIndex = index;
    }
    
    @Override
    public PanelCollapsedStatus getPanelCollapsedStatus(final int controllerId, final String pid)
    {
        if (m_ConfigPanelCollapsedStatus.containsKey(controllerId) 
                && m_ConfigPanelCollapsedStatus.get(controllerId).containsKey(pid))
        {
            return m_ConfigPanelCollapsedStatus.get(controllerId).get(pid);
        }
        else
        {
            return createPanelStatus(controllerId, pid);
        }
    }
    
    /**
     * Method that creates a panel status for the specified configuration if it doesn't already exist.
     * 
     * @param controllerId
     *      ID of the controller the configuration is located on.
     * @param pid
     *      PID of the configuration the status will belong to.
     * @return
     *      Newly created {@link PanelCollapsedStatus} object. It is first added to the map before being returned.
     */
    private PanelCollapsedStatus createPanelStatus(final int controllerId, final String pid)
    {
        if (!m_ConfigPanelCollapsedStatus.containsKey(controllerId)) //NOPMD - avoid x!=y 
        {                                                            //arranging order would be less readable
            m_ConfigPanelCollapsedStatus.put(controllerId, new HashMap<String, PanelCollapsedStatus>());
            m_ConfigPanelCollapsedStatus.get(controllerId).put(pid, new PanelCollapsedStatus());
        }
        else if (!m_ConfigPanelCollapsedStatus.get(controllerId).containsKey(pid))
        {   
            m_ConfigPanelCollapsedStatus.get(controllerId).put(pid, new PanelCollapsedStatus());
        }
        return m_ConfigPanelCollapsedStatus.get(controllerId).get(pid);
    }
    
    @Override
    public void configPanelStatusChanged(final ToggleEvent event)
    {
        //Attempt to find the header components that contain the PID of the configuration.
        final UIComponent configHeader = event.getComponent().findComponent(CONFIG_HEADER_ID);
        final UIComponent factoryHeader = event.getComponent().findComponent(FACTORY_HEADER_ID);
        final UIComponent factoryConfigHeader = event.getComponent().findComponent(FACTORY_CONFIG_HEADER_ID);
        
        //Logic checks to see which header was found. An event will contain one of the three headers above.
        //Then convert the header to a UIOutput so that the PID string value can be retrieved from the component.
        UIOutput output = null;
        if (configHeader != null) //NOPMD - avoid x!=y                                         
        {                         //arranging order would be less readable
            output = (UIOutput)configHeader;
        }
        else if (factoryHeader != null) //NOPMD - avoid x!=y
        {                               //arranging order would be less readable
            output = (UIOutput)factoryHeader;
        }
        else if (factoryConfigHeader != null)
        {                                     
            output = (UIOutput)factoryConfigHeader;
        }

        if (output != null)
        {
            final PanelCollapsedStatus status = m_ConfigPanelCollapsedStatus.get(
                    activeController.getActiveController().getId()).get(output.getValue());
            if (status.isCollapsed())
            {
                status.setCollapsed(false);
            }
            else
            {
                status.setCollapsed(true);
            }
        }
    }
    
    @Override
    public void configTabViewChange(final TabChangeEvent event)
    {
        final String title = event.getTab().getTitle();
        final List<UIComponent> tabList = event.getComponent().getChildren();
        
        for (int i = 0; i < tabList.size(); i++)
        {
            final Tab tabComp = (Tab)tabList.get(i);
            if (tabComp.getTitle().equals(title))
            {
                m_ConfigTabIndex = i;
                return;
            }
        }
    }
    
    /**
     * Class that holds information about the expanded/collapsed status of a configuration panel.
     */
    public class PanelCollapsedStatus
    {
        /**
         * Boolean that stores whether the panel is collapsed/expanded.
         */
        private boolean m_Collapsed = true;
        
        /**
         * Method that returns the state of the panel.
         * 
         * @return
         *      Boolean. True if the panel is collapsed and false if expanded.
         */
        public boolean isCollapsed()
        {
            return m_Collapsed;
        }
        
        /**
         * Method that sets the state of the panel.
         * 
         * @param isCollapsed
         *      Boolean that represents the state of the panel. True if the panel is collapsed and false if expanded.
         */
        public void setCollapsed(final boolean isCollapsed)
        {
            m_Collapsed = isCollapsed;
        }
    }
}
