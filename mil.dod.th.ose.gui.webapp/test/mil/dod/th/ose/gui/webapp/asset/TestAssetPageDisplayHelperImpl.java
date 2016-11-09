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

import static org.mockito.Mockito.mock;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.component.UIComponent;

import org.junit.Before;
import org.junit.Test;
import org.primefaces.component.tabview.Tab;
import org.primefaces.event.TabChangeEvent;

/**
 * Test class for the {@link AssetPageDisplayHelperImpl} class.  
 * @author bachmakm
 *
 */
public class TestAssetPageDisplayHelperImpl
{
    private AssetPageDisplayHelperImpl m_SUT;
    
    @Before
    public void setup()
    {
        m_SUT = new AssetPageDisplayHelperImpl();
    }
    
    /**
     * Verify get/set asset page index works correctly.
     */
    @Test
    public void testAssetPageIndex()
    {
        assertThat(m_SUT.getAssetPageIndex(), is(0));
        
        m_SUT.setAssetPageIndex(1);
        assertThat(m_SUT.getAssetPageIndex(), is(1));
    }
    
    /**
     * Verify new command index is added to the map.
     * Verify command index is updated if there's a new uuid.
     * Verify command index can be retrieved successfully. 
     */
    @Test
    public void testGetAssetCommandIndex()
    {
        UUID uuid = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        //test new controller and new uuid
        ActiveCommandIndexHolder commandIndex = m_SUT.getAssetCommandIndexHolder(123, uuid);
        assertThat(commandIndex.getActiveCommandPanel().getIndex(), is(-1));
        assertThat(commandIndex.getActiveGetCommandPanel().getIndex(), is(-1));
        assertThat(commandIndex.getActiveGetCommandTab().getIndex(), is(0));
        
        //test same controller, new uuid
        commandIndex = m_SUT.getAssetCommandIndexHolder(123, uuid2);
        assertThat(commandIndex.getActiveCommandPanel().getIndex(), is(-1));
        assertThat(commandIndex.getActiveGetCommandPanel().getIndex(), is(-1));
        assertThat(commandIndex.getActiveGetCommandTab().getIndex(), is(0));
        commandIndex.getActiveCommandPanel().setIndex(3); //reset index of object
        
        //test same controller, same uuid - verify returned object maintains set value
        commandIndex = m_SUT.getAssetCommandIndexHolder(123, uuid2);
        assertThat(commandIndex.getActiveCommandPanel().getIndex(), is(3));
    }
    
    /**
     * Verify correct behavior of resetting active index
     * and active index name in the event of a tab view change.
     */
    @Test
    public void testAssetTabViewChange()
    {
        TabChangeEvent event = mock(TabChangeEvent.class);
        Tab tab = mock(Tab.class);
        UIComponent component = mock(UIComponent.class);
        Tab childComp1 = mock(Tab.class);
        Tab childComp2 = mock(Tab.class);
        
        List<UIComponent> UiChillins = new ArrayList<UIComponent>();
        UiChillins.add(childComp1);
        UiChillins.add(childComp2);
        
        when(event.getTab()).thenReturn(tab);
        when(tab.getTitle()).thenReturn("SirTitle");
        
        when(event.getComponent()).thenReturn(component);
        when(component.getChildCount()).thenReturn(2); //number of children in UiChillins
        when(component.getChildren()).thenReturn(UiChillins);
        
        when(childComp2.getTitle()).thenReturn("SirTitle");
        
        m_SUT.assetTabViewChange(event);
        assertThat(m_SUT.getAssetPageTabName(), is("SirTitle"));
        assertThat(m_SUT.getAssetPageIndex(), is(0));
    }
    
    /**
     * Verify correct behavior of setting the active index for the observation page tools.
     * Test setting of active index to 1 if tab change was for Filter tab.
     */
    @Test
    public void testObservationToolsTabChangeFilter()
    {
        TabChangeEvent event = mock(TabChangeEvent.class);
        Tab tab = mock(Tab.class);
        UIComponent component = mock(UIComponent.class);
        Tab childComp1 = mock(Tab.class);
        Tab childComp2 = mock(Tab.class);
        
        List<UIComponent> UiChillins = new ArrayList<UIComponent>();
        UiChillins.add(childComp1);
        UiChillins.add(childComp2);
        
        when(event.getTab()).thenReturn(tab);
        when(tab.getTitle()).thenReturn("Filter");
        
        when(event.getComponent()).thenReturn(component);
        when(component.getChildren()).thenReturn(UiChillins);
        
        when(childComp2.getTitle()).thenReturn("Filter");
        when(childComp1.getTitle()).thenReturn("candy");

        when(event.getTab()).thenReturn(tab);

        m_SUT.observationToolsTabChange(event);

        assertThat(m_SUT.getObservationToolsActiveIndex(), is(1));
    }
    
    /**
     * Verify correct behavior of setting the active index for the observation page tools.
     * Test setting of active index to 0 if tab change was for another tab.
     */
    @Test
    public void testObservationToolsTabChangeRetrieve()
    {
        TabChangeEvent event = mock(TabChangeEvent.class);
        Tab tab = mock(Tab.class);
        UIComponent component = mock(UIComponent.class);
        Tab childComp1 = mock(Tab.class);
        Tab childComp2 = mock(Tab.class);
        
        List<UIComponent> UiChillins = new ArrayList<UIComponent>();
        UiChillins.add(childComp1);
        UiChillins.add(childComp2);
        
        when(event.getTab()).thenReturn(tab);
        when(tab.getTitle()).thenReturn("hot sauce");
        
        when(event.getComponent()).thenReturn(component);
        when(component.getChildren()).thenReturn(UiChillins);
        
        when(childComp2.getTitle()).thenReturn("Filter");
        when(childComp1.getTitle()).thenReturn("hot sauce");

        when(event.getTab()).thenReturn(tab);
        
        m_SUT.observationToolsTabChange(event);

        assertThat(m_SUT.getObservationToolsActiveIndex(), is(0));
    }
}
