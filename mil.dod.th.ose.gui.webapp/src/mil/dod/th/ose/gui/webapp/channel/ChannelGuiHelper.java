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
package mil.dod.th.ose.gui.webapp.channel;

/**
 * Class handles general gui display interactions for the channels.xhtml page. 
 * 
 * @author nickmarcucci
 *
 */

public interface ChannelGuiHelper
{
    /**
     * Getter function for radio button variable.
     * @return
     *  the {@link ControllerOption} type which dictates how 
     *  things are displayed.
     */
    ControllerOption getRenderOption();
   
    /**
     * Setter function for the variable which holds the radio button's 
     * state. The radio button determines which style the channels should
     * be displayed to the user.
     * @param option
     *  a {@link ControllerOption} option which specifies to display channels 
     *  by type or by controller.
     */
    void setRenderOption(ControllerOption option);
    
    /**
     * Enumeration class which identifies overall format of how channels should be displayed 
     * on the main channels page. Specifically, it is used by the radio button which allows the 
     * user to switch between displaying channels by controller or by channel.
     * 
     * @author nickmarcucci
     *
     */
    enum ControllerOption
    {
        /**
         * Indicates that channels should be shown by the controller that they are associated with.
         */
        CONTROLLER,
        /**
         * Indicates that channels should be shown by types of channels regardless of the controller
         * they are associated with.
         */
        CHANNEL
    }
}
