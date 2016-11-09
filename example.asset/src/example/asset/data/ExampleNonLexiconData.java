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
package example.asset.data;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author jconn
 *
 */
@XmlRootElement
public class ExampleNonLexiconData implements Serializable
{
    /**
     * 
     */
    private static final long serialVersionUID = 5003206538705988283L;
    private Long m_Number;
    private String m_String;
    
    public ExampleNonLexiconData()
    {
        // required for jaxb serialization
    }
    
    public ExampleNonLexiconData(final Long number, final String string)
    {
        m_Number = number;
        m_String = string;
    }
    
    public void setNumber(final Long number)
    {
        m_Number = number;
    }
    
    public Long getNumber()
    {
        return m_Number;
    }
    
    public void setString(final String string)
    {
        m_String = string;
    }
    
    public String getString()
    {
        return m_String;
    }
}