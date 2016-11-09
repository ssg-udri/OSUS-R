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
package mil.dod.th.ose.gui.webapp.asset;

import javax.xml.bind.annotation.XmlValue;

/**
 * Test class created solely for use with the {@link TestAssetCommandViewImpl} test class.
 * 
 * @author cweisenborn
 */
public class ExampleComplexClass
{
    @XmlValue
    private Integer intWrapperField;
    @XmlValue
    private Double doubleWrapperField;

    public ExampleComplexClass()
    {
        intWrapperField = 25;
        doubleWrapperField = 2.25;
    }
    
    public Integer getIntWrapperField()
    {
        return intWrapperField;
    }

    public void setIntWrapperField(Integer field)
    {
        this.intWrapperField = field;
    }

    public Double getDoubleWrapperField()
    {
        return doubleWrapperField;
    }

    public void setDoubleWrapperField(Double field)
    {
        this.doubleWrapperField = field;
    } 
}
