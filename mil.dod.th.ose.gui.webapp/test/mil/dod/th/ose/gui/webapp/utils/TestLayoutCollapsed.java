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
package mil.dod.th.ose.gui.webapp.utils;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.primefaces.component.layout.LayoutUnit;
import org.primefaces.event.ToggleEvent;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author matt
 *
 */
public class TestLayoutCollapsed
{
    /**
     * Verify toggling of layout components is accurately tracked.
     */
    @Test
    public void testLayoutsCollapsed()
    {
        LayoutCollapsed layoutCollapsed = new LayoutCollapsed();
        
        //make sure the collapsed attribute for controllers and navigation is set from instantiating the layoutCollapsed
        //class and make sure they default to false indicating that they default to expanded
        assertThat(layoutCollapsed.isControllerCollapsed(), is(notNullValue()));
        assertThat(layoutCollapsed.isNavigationCollapsed(), is(notNullValue()));
        assertThat(layoutCollapsed.isControllerCollapsed(), is(false));
        assertThat(layoutCollapsed.isNavigationCollapsed(), is(false));
        
        LayoutUnit controllerLayout = new LayoutUnit();
        LayoutUnit navigationLayout = new LayoutUnit();
        
        //ids of the controller and navigation layout are required to be these respectively.. since we get the layout
        //unit object based off their id
        controllerLayout.setId("controllerLayout");
        navigationLayout.setId("navigationLayout");
        
        //mock toggle events and associate the events with the respective layouts
        ToggleEvent toggleEast = mock(ToggleEvent.class);
        when(toggleEast.getComponent()).thenReturn(controllerLayout);
       
        ToggleEvent toggleWest = mock(ToggleEvent.class);
        when(toggleWest.getComponent()).thenReturn(navigationLayout);
        
        //create toggle events for the layouts and associate the mocked toggle events with them
        layoutCollapsed.handleToggle(toggleEast);
        layoutCollapsed.handleToggle(toggleWest);
        
        //make sure that after the toggle events were raised the layout unit's collapsed attributes have now changed
        assertThat(layoutCollapsed.isControllerCollapsed(), is(true));
        assertThat(layoutCollapsed.isNavigationCollapsed(), is(true));
        
        layoutCollapsed.handleToggle(toggleEast);
        layoutCollapsed.handleToggle(toggleWest);
        
        assertThat(layoutCollapsed.isControllerCollapsed(), is(false));
        assertThat(layoutCollapsed.isNavigationCollapsed(), is(false));
    }
}
