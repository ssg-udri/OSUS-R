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
 * Class holds the active indexes for components on the command panel.
 * @author nickmarcucci
 *
 */
public class ActiveCommandIndexHolder
{
    /**
     * Holds the active index of the open command panel.
     */
    private final ActiveIndexHolder m_ActiveCommandPanel;
    
    /**
     * Holds the active index of the get command panel.
     */
    private final ActiveIndexHolder m_ActiveGetCommandPanel;
    
    /**
     * Holds the active get command selected tab.
     */
    private final ActiveIndexHolder m_ActiveGetCommandTab;
    
    /**
     * Constructor.
     */
    public ActiveCommandIndexHolder()
    {
        m_ActiveCommandPanel = new ActiveIndexHolder();
        m_ActiveGetCommandPanel = new ActiveIndexHolder();
        m_ActiveGetCommandTab = new ActiveIndexHolder();
        m_ActiveGetCommandTab.setIndex(0);
    }
    
    /**
     * Retrieve the {@link ActiveIndexHolder} for the active command panel
     * index.
     * @return
     *              the structure which holds the active command panel index
     */
    public ActiveIndexHolder getActiveCommandPanel()
    {
        return m_ActiveCommandPanel;
    }
    
    /**
     * Retrieve the {@link ActiveIndexHolder} for the active get command panel
     * index.
     * @return
     *              the structure which holds the active get command panel index
     */
    public ActiveIndexHolder getActiveGetCommandPanel()
    {
        return m_ActiveGetCommandPanel;
    }
    
    /**
     * Retrieve the {@link ActiveIndexHolder} for the active get command tab
     * index.
     * @return
     *              the structure which holds the active get command panel tab index
     */
    public ActiveIndexHolder getActiveGetCommandTab()
    {
        return m_ActiveGetCommandTab;
    }
}