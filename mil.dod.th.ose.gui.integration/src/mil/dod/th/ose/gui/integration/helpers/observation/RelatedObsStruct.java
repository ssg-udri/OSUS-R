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
package mil.dod.th.ose.gui.integration.helpers.observation;

/**
 * Structure which represents a single related observation entry.
 * 
 * @author nickmarcucci
 *
 */
public class RelatedObsStruct
{
    /**
     * The observation for the entry.
     */
    private ExpectedObservation m_Expected;
    
    /**
     * The description that is displayed for this entry.
     */
    private String m_Desc;
    
    /**
     * Constructor.
     * @param expected
     *  the observation for this entry
     * @param desc
     *  the description for this entry
     */
    public RelatedObsStruct(ExpectedObservation expected, String desc)
    {
        m_Expected = expected;
        m_Desc = desc;
    }
    
    /**
     * The observation for this entry.
     * @return
     *  the observation
     */
    public ExpectedObservation getObservation()
    {
        return m_Expected;
    }
    
    /**
     * The description for this entry.
     * @return
     *  the description
     */
    public String getDescription()
    {
        return m_Desc;
    }
}
