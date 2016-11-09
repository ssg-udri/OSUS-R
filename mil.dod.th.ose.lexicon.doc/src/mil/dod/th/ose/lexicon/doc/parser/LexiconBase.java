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
 * Class the represents the base fields a lexicon object should have.
 * 
 * @author cweisenborn
 */
public abstract class LexiconBase
{   
    private String m_Name;
    private String m_Description;
    
    public String getName()
    {
        return m_Name;
    }
    
    public void setName(final String name)
    {
        m_Name = name;
    }
    
    public String getDescription()
    {
        return m_Description;
    }
    
    public void setDescription(final String description)
    {
        m_Description = description;
    }
}
