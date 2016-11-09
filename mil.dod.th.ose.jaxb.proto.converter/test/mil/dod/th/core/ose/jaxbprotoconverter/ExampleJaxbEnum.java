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
package mil.dod.th.core.ose.jaxbprotoconverter;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * @author cweisenborn
 */
@XmlType(name = "TestEnum")
@XmlEnum
public enum ExampleJaxbEnum
{
    @XmlEnumValue("Value1")
    VALUE_1("Value1"),
    @XmlEnumValue("Value2")
    VALUE_2("Value2");
    
    private final String value;

    ExampleJaxbEnum(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ExampleJaxbEnum fromValue(String v) {
        for (ExampleJaxbEnum c: ExampleJaxbEnum.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}
