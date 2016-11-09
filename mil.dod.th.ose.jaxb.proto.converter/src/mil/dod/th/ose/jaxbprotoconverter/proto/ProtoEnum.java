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
package mil.dod.th.ose.jaxbprotoconverter.proto;

import java.util.ArrayList;
import java.util.List;

import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;

/**
 * This class is responsible for storing all information needed to generate a Google Protocol Buffer 
 * enumeration.
 */
public class ProtoEnum extends ProtoElement
{
    /**
     * String representation of enum.
     */
    public static final String ENUM = "Enum";
    
    /**
     * Reference to the proto message the enumeration is associated with.
     */
    private ProtoMessage m_ProtoMsg;
    
    /**
     * List used to store all the values within an enumeration.
     */
    private  List<String> m_Values = new ArrayList<>();
    
    /**
     * Constructor that accepts the proto message the enumeration is associated with.
     * 
     * @param protoMsg
     *      The proto message the enumeration is associated with.
     * @param protoFile
     *      The proto file this proto element is contained within.
     * @param enumName
     *      Name of the proto element.
     * @param type
     *      Enumeration type the proto enumeration represents.
     * @param xsdType
     *      XSD type associated with the enumeration.
     */
    public ProtoEnum(final ProtoMessage protoMsg, final ProtoFile protoFile, final String enumName, 
            final Class<?> type, final XsdType xsdType)
    {
        super(protoFile, enumName, type, xsdType);
        m_ProtoMsg = protoMsg;
    }
    
    /**
     * Gets the proto message the enumeration is associated with.
     * 
     * @return
     *      The proto message model the proto enumeration is associated with.
     */
    public ProtoMessage getProtoMessage()
    {
        return m_ProtoMsg;
    }
    
    /**
     * Gets the list used to store all values associated with the enumeration.
     * 
     * @return
     *      The enumeration values.
     */
    public List<String> getValues()
    {
        return m_Values;
    }

    /**
     * Sets the list used to store all values associated with the enumeration.
     * 
     * @param values
     *      The values to set.
     */
    public void setValues(final List<String> values)
    {
        this.m_Values = values;
    }
}
