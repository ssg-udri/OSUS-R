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

/**
 * Class used to store the index of the currently open command for an asset on the command and control tab of the 
 * assets page.
 */
public class ActiveIndexHolder
{
    /**
     * Integer that represents the index of the currently open command.
     */
    private Integer m_ActiveIndex;
    
    /**
     * Constructor.
     */
    public ActiveIndexHolder()
    {
        m_ActiveIndex = -1;
    }
    
    /**
     * Method used to retrieve the index of the currently open command for the asset.
     * 
     * @return
     *          Integer that represents the currently open command tab.
     */
    public Integer getIndex()
    {
        return m_ActiveIndex;
    }
    
    /**
     * Method used to set the index of the currently open command for the asset.
     * 
     * @param index
     *          Integer that represents the currently open command tab.
     */
    public void setIndex(final Integer index)
    {
        m_ActiveIndex = index;
    }
}

