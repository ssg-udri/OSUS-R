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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlValue;

/**
 * @author  cweisenborn
 */
public class ExampleAnotherJaxbClass extends ExampleJaxbNotParsedSuperClass
{
    @XmlAttribute(name = "addTestStr")
    private String addTestStr;
    @XmlElement
    private int addTestInt;
    @XmlValue
    private boolean addTestBool;
    @XmlElement
    private long addTestLong;
    @XmlAttribute(name = "addTestDouble")
    private double addTestDouble;
    @XmlValue
    private float addTestFloat;
    @XmlAttribute(name = "addTestByteArray")
    private byte[] addTestByteArray;
}