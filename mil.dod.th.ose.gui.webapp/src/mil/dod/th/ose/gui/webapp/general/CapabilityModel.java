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
package mil.dod.th.ose.gui.webapp.general;

/**
 * The model behind showing each capability element.
 * @author jgold
 *
 */
public class CapabilityModel 
{ 
    
    /**
     * The name of this capability.
     */
    private String m_Name;
    
    /**
     * The value of this capability.
     */
    private String m_Value;

    /**
     * Get the name of this capability.
     * @return the m_Name
     */
    public String getName() 
    {
        return m_Name;
    }

    /**
     * Set the name of this capability.
     * @param name the m_Name to set
     */
    public void setName(final String name) 
    {
        this.m_Name = name;
    }

    /**
     * Return the value of this capability.
     * @return the value
     */
    public String getValue() 
    {
        return m_Value;
    }

    /**
     * Set the value of this capability.
     * @param value the value to set
     */
    public void setValue(final String value) 
    {
        this.m_Value = value;
    }
      
    @Override
    public String toString()
    {
        return m_Name + " / " + m_Value;
    }
}
