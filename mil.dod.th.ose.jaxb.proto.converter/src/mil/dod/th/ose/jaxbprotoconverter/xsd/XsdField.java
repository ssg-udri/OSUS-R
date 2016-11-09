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
package mil.dod.th.ose.jaxbprotoconverter.xsd;

/**
 * Class that stores information about a field within in an XSD type.
 * 
 * @author cweisenborn
 */
public class XsdField
{
    /**
     * Reference to the XSD type the field is associated with.
     */
    private final XsdType m_XsdType;
    
    /**
     * The index value for the XSD field.
     */
    private final int m_FieldIndex;
    
    /**
     * Whether or not the fields index has been overridden.
     */
    private final boolean m_Overridden;
    
    /**
     * Constructor that sets the associated type and the index value.
     * 
     * @param xsdType
     *      The XSD type the field is associated with.
     * @param fieldIndex
     *      Integer that represents the index of the field.
     * @param overridden
     *      Whether or not the fields index has been overridden.
     */
    public XsdField(final XsdType xsdType, final int fieldIndex, final boolean overridden)
    {
        m_XsdType = xsdType;
        m_FieldIndex = fieldIndex;
        m_Overridden = overridden;
    }
    
    /**
     * Get the XSD type the field is associated with.
     * 
     * @return
     *      The XSD type the field is associated with.
     */
    public XsdType getXsdType()
    {
        return m_XsdType;
    }
    
    /**
     * Get the index value for the XSD field.
     * 
     * @return
     *      Integer that represents the index of the field.
     */
    public int getIndex()
    {
        return m_FieldIndex;
    }
    
    /**
     * Returns whether or not the index for the field has been overridden.
     * 
     * @return
     *      True if the fields index has been overridden and false otherwise.
     */
    public boolean isIndexOverridden()
    {
        return m_Overridden;
    }
}
