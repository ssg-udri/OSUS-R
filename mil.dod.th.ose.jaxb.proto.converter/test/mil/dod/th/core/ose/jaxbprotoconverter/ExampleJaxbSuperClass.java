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
 * @author cweisenborn
 */
public class ExampleJaxbSuperClass
{
    @XmlAttribute(name = "extTestStr")
    private String extTestStr;
    @XmlElement
    private int extTestInt;
    @XmlValue
    private boolean extTestBool;
    @XmlElement
    private long extTestLong;
    @XmlAttribute(name = "extTestDouble")
    private double extTestDouble;
    @XmlValue
    private float extTestFloat;
    @XmlAttribute(name = "extTestByteArray")
    private byte[] extTestByteArray;
}