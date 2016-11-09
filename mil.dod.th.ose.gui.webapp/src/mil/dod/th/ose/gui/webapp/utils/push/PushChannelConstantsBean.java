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
package mil.dod.th.ose.gui.webapp.utils.push;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 * Class provides managed bean functionality to access PushChannelConstants properties.
 * 
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "pushChannelConstants")
@RequestScoped
public class PushChannelConstantsBean
{
    
    /**
     * Function to retrieve the generic event and message socket channel from 
     * an xhtml page.
     * Static fields cannot be accessed using JSF without an accessor method.
     * @return
     *  the string that represents the push channel over which generic THOSE GUI
     *  events will be pushed.
     */
    public String getMessageChannel()
    {
        return PushChannelConstants.PUSH_CHANNEL_THOSE_MESSAGES;
    }
}
