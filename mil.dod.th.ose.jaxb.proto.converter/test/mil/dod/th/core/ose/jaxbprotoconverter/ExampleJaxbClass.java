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

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author  cweisenborn
 */
public class ExampleJaxbClass extends ExampleJaxbSuperClass
{
    @XmlAttribute(name = "testStr")
    private String testStr;
    @XmlElement(name="testINT")
    private int testInt;
    @XmlValue
    private boolean testBool;
    @XmlElement
    private long testLong;
    @XmlAttribute(name = "testDouble")
    private double testDouble;
    @XmlValue
    private float testFloat;
    @XmlAttribute(name = "testByteArray")
    private byte[] testByteArray;
    @XmlElement
    private List<String> testList;
    @XmlElement(namespace = "http://th.dod.mil/ose/jaxbprotoconverter", required = true)
    private ExampleAnotherJaxbClass exampleReference;
    @XmlAttribute(name = "exampleEnumReference")
    private ExampleJaxbEnum exampleEnumReference;
    @XmlElement
    private String _DEPRECATED_testStrOld;
}