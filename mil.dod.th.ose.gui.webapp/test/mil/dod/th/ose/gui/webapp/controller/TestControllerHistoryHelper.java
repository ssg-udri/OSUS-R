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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import mil.dod.th.ose.gui.webapp.controller.history.ControllerHistory;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * 
 * 
 * @author cweisenborn
 */
public class TestControllerHistoryHelper
{
    private @Mock ControllerHistoryMgr m_ControllerHistMgr;
    private ControllerHistoryHelper m_SUT;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        mockControllerHistory();
        
        m_SUT = new ControllerHistoryHelper();
        m_SUT.setControllerHistMgr(m_ControllerHistMgr);
    }
    
    @Test
    public void testSelectedController()
    {
        final String controller = "10.5.5.5:test-system";
        m_SUT.setSelectedController(controller);
        assertThat(m_SUT.getSelectedController(), is(controller));
    }
    
    @Test
    public void testGetControllerHistoryList()
    {
        final Set<String> historyList = m_SUT.getControllerHistoryList();
        
        assertThat(historyList.size(), is(3));
        assertThat(historyList, containsInAnyOrder("localhost:generic-controller", "10.110.7.90:Roof", 
                "192.168.2.1:House"));
    }
    
    @Test
    public void testGetSelectedControllerHistory()
    {
        ControllerHistory selectedController = m_SUT.getSelectedControllerHistory();
        assertThat(selectedController.getControllerId(), is(0));
        assertThat(selectedController.getControllerName(), is("generic-controller"));
        assertThat(selectedController.getHostName(), is("localhost"));
        assertThat(selectedController.getPort(), is(4000));
        assertThat(selectedController.getLastConnected(), is(19000L));
        
        m_SUT.setSelectedController("10.110.7.90:Roof");
        selectedController = m_SUT.getSelectedControllerHistory();
        
        assertThat(selectedController.getControllerId(), is(5));
        assertThat(selectedController.getControllerName(), is("Roof"));
        assertThat(selectedController.getHostName(), is("10.110.7.90"));
        assertThat(selectedController.getPort(), is(3001));
        assertThat(selectedController.getLastConnected(), is(5000L));
    }
    
    private void mockControllerHistory()
    {
        final ControllerHistory localhost = new ControllerHistory();
        localhost.setControllerId(0);
        localhost.setControllerName("generic-controller");
        localhost.setHostName("localhost");
        localhost.setLastConnected(19000);
        localhost.setPort(4000);
        
        final ControllerHistory controller1 = new ControllerHistory();
        controller1.setControllerId(5);
        controller1.setControllerName("Roof");
        controller1.setHostName("10.110.7.90");
        controller1.setLastConnected(5000);
        controller1.setPort(3001);
        
        final ControllerHistory controller2 = new ControllerHistory();
        controller2.setControllerId(5);
        controller2.setControllerName("House");
        controller2.setHostName("192.168.2.1");
        controller2.setLastConnected(75000);
        controller2.setPort(8976);
        
        final Map<String, ControllerHistory> historyMap = new LinkedHashMap<>();
        historyMap.put("localhost:generic-controller", localhost);
        historyMap.put("10.110.7.90:Roof", controller1);
        historyMap.put("192.168.2.1:House", controller2);
        
        when(m_ControllerHistMgr.getControllerHistory()).thenReturn(historyMap);
    }
}
