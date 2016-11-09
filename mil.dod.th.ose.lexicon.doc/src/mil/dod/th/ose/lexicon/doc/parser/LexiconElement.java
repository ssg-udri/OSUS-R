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
package mil.dod.th.ose.lexicon.doc.parser;

/**
 * Model that represents an XSD element.
 * 
 * @author cweisenborn
 */
public class LexiconElement extends LexiconBase
{
    private String m_MinOccurs;
    private String m_MaxOccurs;
    private String m_Type;
    
    public String getMinOccurs()
    {
        return m_MinOccurs;
    }

    public void setMinOccurs(final String minOccurs)
    {
        m_MinOccurs = minOccurs;
    }

    public String getMaxOccurs()
    {
        return m_MaxOccurs;
    }

    public void setMaxOccurs(final String maxOccurs)
    {
        m_MaxOccurs = maxOccurs;
    }
    
    public String getType()
    {
        return m_Type;
    }

    public void setType(final String type)
    {
        m_Type = type;
    }
}
