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
 * Model that represents an XSD complex type.
 * 
 * @author cweisenborn
 */
public class LexiconComplexType extends LexiconBase
{   
    private String m_Extension;
    private List<LexiconElement> m_Elements;
    private List<LexiconAttribute> m_Attributes;
    
    /**
     * Default constructor.
     */
    public LexiconComplexType()
    {
        super();
        m_Elements = new ArrayList<>();
        m_Attributes = new ArrayList<>();
    }
    
    public String getExtension()
    {
        return m_Extension;
    }
    
    public void setExtension(final String extension)
    {
        m_Extension = extension;
    }
    
    public List<LexiconElement> getElements()
    {
        return m_Elements;
    }
    
    public List<LexiconAttribute> getAttributes()
    {
        return m_Attributes;
    }
}
