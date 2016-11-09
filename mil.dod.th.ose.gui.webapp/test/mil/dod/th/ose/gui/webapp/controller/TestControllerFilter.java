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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.ose.gui.webapp.channel.ChannelMgrImpl;
import mil.dod.th.ose.gui.webapp.controller.ControllerFilter.Filter;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests filtering the list of controllers.
* @author matt
*/
public class TestControllerFilter
{
    private ControllerFilterImpl m_SUT;
    private ControllerMgrImpl m_ControllerMgr;
    private ChannelMgrImpl m_ChannelMgr;
    
    @Before
    public void setUp()
    {
        m_ControllerMgr = mock(ControllerMgrImpl.class);
        m_ChannelMgr = mock(ChannelMgrImpl.class);
        
        m_SUT = new ControllerFilterImpl();

        m_SUT.setChannelManager(m_ChannelMgr);
        m_SUT.setControllerManager(m_ControllerMgr);
    }
    
    /**
    * Test setting the filter to all filter types.
    */
    @Test
    public void testSetFilter()
    {
        m_SUT.setFilter(Filter.CommsDown);
        assertThat(m_SUT.getFilter(), is(Filter.CommsDown));
        
        m_SUT.setFilter(Filter.CommsUp);
        assertThat(m_SUT.getFilter(), is(Filter.CommsUp));
        
        m_SUT.setFilter(Filter.All);
        assertThat(m_SUT.getFilter(), is(Filter.All));
    }
    
    /**
    * Verify that the expected list of controllers is returned when getting the filtered list.
    */
    @Test
    public void testGetFilteredList()
    {
        //mock controllers that will have different statuses
        ControllerModel model1 = mock(ControllerModel.class);
        ControllerModel model2 = mock(ControllerModel.class);
        ControllerModel model3 = mock(ControllerModel.class);
        
        List<ControllerModel> controllerList = new ArrayList<ControllerModel>();
        
        controllerList.add(model1);
        controllerList.add(model2);
        controllerList.add(model3);
        
        when(m_ControllerMgr.getAllControllers()).thenReturn(controllerList);
        
        when(model1.getId()).thenReturn(111);
        when(model2.getId()).thenReturn(222);
        when(model3.getId()).thenReturn(333);
        
        //set a different status for each controller
        when(m_ChannelMgr.getStatusForController(111)).thenReturn(ControllerStatus.Good);
        when(m_ChannelMgr.getStatusForController(222)).thenReturn(ControllerStatus.Bad);
        when(m_ChannelMgr.getStatusForController(333)).thenReturn(ControllerStatus.Degraded);
        
        //verify default all filter value was set and returns all controllers
        controllerList = m_SUT.getFilterList();
        assertThat(controllerList.size(), is(3));
        assertThat(controllerList.contains(model1), is(true));
        
        //verify filter option of comms up returns controllers with good status
        m_SUT.setFilter(ControllerFilter.Filter.CommsUp);
        controllerList = m_SUT.getFilterList();
        assertThat(controllerList.size(), is(1));
        assertThat(controllerList.contains(model1), is(true));
        
        //verify filter option of comms down returns controllers with bad status
        m_SUT.setFilter(ControllerFilter.Filter.CommsDown);
        controllerList = m_SUT.getFilterList();
        assertThat(controllerList.size(), is(1));
        assertThat(controllerList.contains(model2), is(true));
        
        //verify filter option of degraded returns controller with degraded status
        m_SUT.setFilter(ControllerFilter.Filter.Degraded);
        controllerList = m_SUT.getFilterList();
        assertThat(controllerList.size(), is(1));
        assertThat(controllerList.contains(model3), is(true));
        
        //verify filter option of all returns all controllers
        m_SUT.setFilter(ControllerFilter.Filter.All);
        controllerList = m_SUT.getFilterList();
        assertThat(controllerList.size(), is(3));
        assertThat(controllerList.contains(model1), is(true));
    }
}
