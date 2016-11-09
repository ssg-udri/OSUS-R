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

package mil.dod.th.ose.gui.webapp.theme;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;


/**
 * Holds the information for the currently displayed theme for the 
 * GUI. This bean is used on the mainScreenTemplate.xhtml page and 
 * in the web.xml. For correct string names for theme jars refer to 
 * the Primefaces showcase.
 */
@ManagedBean
@SessionScoped
public class ThemeManagerBean
{
    /**
     * String which holds the name of the currently selected 
     * GUI theme. 
     */
    private String m_Theme;
    
    /**
     * Constructor for ThemeManagerBean.
     */
    public ThemeManagerBean()
    {
        //set the current default theme
        m_Theme = "TH-Day";
    }
    
    /**
     * Sets the theme variable to the currently chosen 
     * theme string. 
     * 
     * @param theTheme
     *  the string that identifies the currently selected theme
     */
    public void setTheme(final String theTheme)
    {
        if (theTheme != null)
        {
            m_Theme = theTheme; 
        }
        
    }
    
    /**
     * Returns the currently selected string representation of the theme.
     * @return
     *  the string which represents the currently selected theme
     */
    public String getTheme()
    {
        return m_Theme;
    }
}
