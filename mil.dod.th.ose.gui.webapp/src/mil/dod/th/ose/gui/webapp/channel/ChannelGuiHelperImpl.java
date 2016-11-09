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

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Implementation of {@link ChannelGuiHelper}.
 * 
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "channelGuiHelper")
@SessionScoped
public class ChannelGuiHelperImpl implements ChannelGuiHelper
{
    /**
     * Variable which holds the status of the radio button choice
     * to display channels either by channel type or controller.
     */
    private ControllerOption m_RenderOption;

    /**
     * Constructor.
     */
    public ChannelGuiHelperImpl()
    {
        m_RenderOption = ControllerOption.CHANNEL;
    }
    
    

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiHelper#getRenderOption()
     */
    @Override
    public ControllerOption getRenderOption()
    {
        return m_RenderOption;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.channel.ChannelGuiHelper#setRenderOption
     * (mil.dod.th.ose.gui.webapp.channel.ChannelGuiHelper.ControllerOption)
     */
    @Override
    public void setRenderOption(final ControllerOption option)
    {
        m_RenderOption = option;
    }
}
