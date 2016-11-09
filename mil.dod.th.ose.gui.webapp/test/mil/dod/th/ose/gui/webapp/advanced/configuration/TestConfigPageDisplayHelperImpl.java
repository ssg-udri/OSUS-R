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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field; // NOCHECKSTYLE: TD: illegal package, new warning, old code
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;

import org.junit.Before;
import org.junit.Test;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.ToggleEvent;

import mil.dod.th.ose.gui.webapp.advanced.configuration.ConfigPageDisplayHelperImpl.PanelCollapsedStatus;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ControllerModel;

/**
 * Test class for the {@link ConfigPageDisplayHelperImpl} class.
 * 
 * @author cweisenborn
 */
public class TestConfigPageDisplayHelperImpl
{
    private static String CONFIG_HEADER_ID = "configHeader";
    private static String FACTORY_HEADER_ID = "factoryHeader";
    private static String FACTORY_CONFIG_HEADER_ID = "factoryConfigHeader";
    private static int CONTROLLERID = 25;
    
    private ConfigPageDisplayHelperImpl m_SUT;
    private ActiveController m_ActiveController;
    
    @Before
    public void setup()
    {
        m_ActiveController = mock(ActiveController.class);
        
        m_SUT = new ConfigPageDisplayHelperImpl();
        
        m_SUT.setActiveController(m_ActiveController);
    }
    
    /**
     * Test the get/set configuration tab index methods.
     * Verify the appropriate value is returned/set for the configuration tab index.
     */
    @Test
    public void testGetSetConfigTabIndex()
    {
        assertThat(m_SUT.getConfigTabIndex(), is(0));
        
        m_SUT.setConfigTabIndex(5);
        
        assertThat(m_SUT.getConfigTabIndex(), is(5));
    }
    
    /**
     * Test the getPanelCollapsedStatus method.
     * Verify that the appropriate status is returned.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetPanelCollapsedStatus() throws SecurityException, IllegalArgumentException, NoSuchFieldException, 
        IllegalAccessException
    {
        addConfigToMap();
        
        PanelCollapsedStatus status = m_SUT.getPanelCollapsedStatus(CONTROLLERID, "config PID");
        assertThat(status.isCollapsed(), is(false));
        
        Field map = m_SUT.getClass().getDeclaredField("m_ConfigPanelCollapsedStatus");
        map.setAccessible(true);
        Map<Integer, Map<String, PanelCollapsedStatus>> statusMap = 
                (Map<Integer, Map<String, PanelCollapsedStatus>>)map.get(m_SUT);
        
        assertThat(statusMap.get(CONTROLLERID).size(), is(3));
        status = m_SUT.getPanelCollapsedStatus(CONTROLLERID, "doesn't exist... yet");
        assertThat(status.isCollapsed(), is(true));
        assertThat(statusMap.get(CONTROLLERID).size(), is(4));
        
        assertThat(statusMap.containsKey(5000), is(false));
        status = m_SUT.getPanelCollapsedStatus(5000, "doesn't exist either...");
        assertThat(status.isCollapsed(), is(true));
        assertThat(statusMap.containsKey(5000), is(true));
        assertThat(statusMap.get(5000).size(), is(1));
    }
    
    /**
     * Test the configPanelStatusChagned method.
     * Verify that a toggle event changes the status stored for the specified panel.
     */
    @Test
    public void testConfigPanelStatusChanged() throws SecurityException, NoSuchFieldException, 
        IllegalArgumentException, IllegalAccessException
    {
        addConfigToMap();
        
        ControllerModel model = mock(ControllerModel.class);
        ToggleEvent event = mock(ToggleEvent.class);
        UIComponent comp = mock(UIComponent.class);
        UIOutput output = mock(UIOutput.class);
        
        when(m_ActiveController.getActiveController()).thenReturn(model);
        when(model.getId()).thenReturn(CONTROLLERID);
        when(event.getComponent()).thenReturn(comp);
        when(comp.findComponent(CONFIG_HEADER_ID)).thenReturn(output);
        when(comp.findComponent(FACTORY_HEADER_ID)).thenReturn(null);
        when(comp.findComponent(FACTORY_CONFIG_HEADER_ID)).thenReturn(null);
        when(output.getValue()).thenReturn("config PID");
        
        m_SUT.configPanelStatusChanged(event);
        PanelCollapsedStatus status = m_SUT.getPanelCollapsedStatus(CONTROLLERID, "config PID");
        assertThat(status.isCollapsed(), is(true));
        
        when(comp.findComponent(CONFIG_HEADER_ID)).thenReturn(null);
        when(comp.findComponent(FACTORY_HEADER_ID)).thenReturn(output);
        when(comp.findComponent(FACTORY_CONFIG_HEADER_ID)).thenReturn(null);
        when(output.getValue()).thenReturn("factory PID");
        
        m_SUT.configPanelStatusChanged(event);
        status = m_SUT.getPanelCollapsedStatus(CONTROLLERID, "factory PID");
        assertThat(status.isCollapsed(), is(false));
        
        when(comp.findComponent(CONFIG_HEADER_ID)).thenReturn(null);
        when(comp.findComponent(FACTORY_HEADER_ID)).thenReturn(null);
        when(comp.findComponent(FACTORY_CONFIG_HEADER_ID)).thenReturn(output);
        when(output.getValue()).thenReturn("factory config PID");
        
        m_SUT.configPanelStatusChanged(event);
        status = m_SUT.getPanelCollapsedStatus(CONTROLLERID, "factory config PID");
        assertThat(status.isCollapsed(), is(false));
    }
    
    /**
     * Test the configuration tab view change method.
     * Verify that a tab change event is handled appropriately.
     */
    @Test
    public void testConfigTabViewChange()
    {
        assertThat(m_SUT.getConfigTabIndex(), is(0));
        
        TabChangeEvent changeEvent = mock(TabChangeEvent.class);
        Tab newTab = mock(Tab.class);
        Tab tab1 = mock(Tab.class);
        Tab tab2 = mock(Tab.class);
        Tab tab3 = mock(Tab.class);
        UIComponent comp = mock(UIComponent.class);
        
        List<UIComponent> tabList = new ArrayList<UIComponent>();
        tabList.add(tab1);
        tabList.add(tab2);
        tabList.add(tab3);
        
        when(changeEvent.getTab()).thenReturn(newTab);
        when(changeEvent.getComponent()).thenReturn(comp);
        when(comp.getChildren()).thenReturn(tabList);
        when(newTab.getTitle()).thenReturn("tab2");
        when(tab1.getTitle()).thenReturn("tab1");
        when(tab2.getTitle()).thenReturn("tab2");
        when(tab3.getTitle()).thenReturn("tab3");
        
        m_SUT.configTabViewChange(changeEvent);
        
        assertThat(m_SUT.getConfigTabIndex(), is(1));
    }
    
    /**
     * Method that adds values to the map that stores panel statuses.
     */
    @SuppressWarnings({"unchecked"})
    private void addConfigToMap() throws SecurityException, NoSuchFieldException, IllegalArgumentException, 
        IllegalAccessException
    {
        Field map = m_SUT.getClass().getDeclaredField("m_ConfigPanelCollapsedStatus");
        map.setAccessible(true);
        Map<Integer, Map<String, PanelCollapsedStatus>> statusMap = 
                (Map<Integer, Map<String, PanelCollapsedStatus>>)map.get(m_SUT);
        
        PanelCollapsedStatus status = m_SUT.new PanelCollapsedStatus();
        status.setCollapsed(false);
        Map<String, PanelCollapsedStatus> pidMap = new HashMap<String, PanelCollapsedStatus>();
        pidMap.put("config PID", status);
        pidMap.put("factory PID", m_SUT.new PanelCollapsedStatus());
        pidMap.put("factory config PID", m_SUT.new PanelCollapsedStatus());
        statusMap.put(CONTROLLERID, pidMap);
    }
}
