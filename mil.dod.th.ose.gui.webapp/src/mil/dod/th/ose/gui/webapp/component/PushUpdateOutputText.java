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
package mil.dod.th.ose.gui.webapp.component;

import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;

/**
 * Backing component class for push updates to OutputText components.
 *
 */
@FacesComponent(value = "mil.dod.th.ose.gui.webapp.component.PushUpdateOutputText")
public class PushUpdateOutputText extends UINamingContainer 
{
    /**
     * Get the complete id of the "for" element.
     * 
     * @return
     *  the string id for the given updatable text box.
     */
    public String getForId()
    {
        final String forValue = (String)getAttributes().get("for");
        
        final int lastColon = getClientId().lastIndexOf(":");
        
        if (lastColon == -1)
        {
            return forValue; 
        }
        else
        {
            // there are multiple components, prepend the client id prefix at the beginning
            return getClientId().subSequence(0, lastColon + 1) + forValue;
        }
    }
}
