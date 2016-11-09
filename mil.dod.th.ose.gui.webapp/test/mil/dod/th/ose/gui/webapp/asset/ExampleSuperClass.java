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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlValue;

/**
 * Test class created solely for use with the {@link TestAssetCommandViewImpl} test class.
 * 
 * @author cweisenborn
 */
public class ExampleSuperClass
{
    @XmlValue
    private Integer intField;
    @XmlValue
    private ExampleComplexClass complexField;
    @XmlValue
    private List<Integer> intWrapperListField;

    @SuppressWarnings("serial")
    public ExampleSuperClass()
    {
        complexField = new ExampleComplexClass();
        intWrapperListField = new ArrayList<Integer>()
        {
            {
                add(25); 
                add(14); 
                add(45);
            }
        };
    }
    
    public int getIntField()
    {
        return intField;
    }

    public void setIntField(int field)
    {
        this.intField = field;
    }
    
    public boolean isSetIntField()
    {
        return intField != null;
    }

    public ExampleComplexClass getComplexField()
    {
        return complexField;
    }

    public void setComplexField(ExampleComplexClass field)
    {
        this.complexField = field;
    }
    
    public boolean isSetComplexField()
    {
        return complexField != null;
    }

    public List<Integer> getIntWrapperListField()
    {
        return intWrapperListField;
    }
    
    public void setIntWrapperListField(List<Integer> field)
    {
        this.intWrapperListField = field;
    }
    
    public boolean isSetIntWrapperListField()
    {
        return (intWrapperListField != null) && (!intWrapperListField.isEmpty());
    }
}
