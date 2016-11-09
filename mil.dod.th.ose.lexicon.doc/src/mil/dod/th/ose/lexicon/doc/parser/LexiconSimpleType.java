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

import java.util.ArrayList;
import java.util.List;

/**
 * Model that represents an XSD simple type.
 * 
 * @author cweisenborn
 */
public class LexiconSimpleType extends LexiconBase
{
    private String m_Type;
    private String m_MinInclusive;
    private String m_MaxInclusive;
    private List<LexiconEnum> m_Enumerations = new ArrayList<>();
    
    public String getType()
    {
        return m_Type;
    }
    
    public void setType(final String type)
    {
        m_Type = type;
    }
    
    public String getMinInclusive()
    {
        return m_MinInclusive;
    }
    
    public void setMinInclusive(final String minInclusive)
    {
        m_MinInclusive = minInclusive;
    }
    
    public String getMaxInclusive()
    {
        return m_MaxInclusive;
    }
    
    public void setMaxInclusive(final String maxInclusive)
    {
        m_MaxInclusive = maxInclusive;
    }
    
    public List<LexiconEnum> getEnumerations()
    {
        return m_Enumerations;
    }
}
