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
package mil.dod.th.ose.gui.webapp.advanced.configuration;

/**
 * Model representing a configuration option.
 * 
 * @author dhumeniuk
 *
 */
public class OptionModel
{
    private final String m_Label;
    private final String m_Value;

    /**
     * Construct model.
     * 
     * @param label
     *      human readable label for the option
     * @param value
     *      value that backs the label
     */
    public OptionModel(final String label, final String value)
    {
        m_Label = label;
        m_Value = value;
    }
    
    /**
     * Retrieves the human readable label for the option.
     * 
     * @return
     *      label of the option
     */
    public String getLabel()
    {
        return m_Label;
    }
    
    /**
     * Retrieves the internal value for the option when selected.
     * 
     * @return
     *      value of the option
     */
    public String getValue()
    {
        return m_Value;
    }
}
