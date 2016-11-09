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

/**
 * Class created to help test asset command classes.
 * 
 * @author cweisenborn
 */
public class ExampleCommandObject
{
    /**
     * A primitive int field.
     */
    private int intField;
    
    /**
     * An optional Integer field.
     */
    private Integer wrapIntField;
     
    /**
     * A primitive bool field.
     */
    private boolean boolField;
    
    /**
     * A wrapped bool field.
     */
    private Boolean wrapBoolField;
     
    /**
     * A primitive double field.
     */
    private double doubleField;
     
    /**
     * An enumeration field.
     */
    private TestEnum enumField;
    
    /**
     * A wrapped float field used to test setting a null value.
     */
    private Float nullableField;
    
    /**
     * A list field.
     */
    private List<Integer> listField;
     
    /**
     * Public constructor.
     */
    public ExampleCommandObject(final int intFieldN, final boolean boolFieldN, final Boolean wrapBoolFieldN, 
            final double doubleFieldN, final TestEnum enumFieldN, final Float nullableFieldN, 
            final List<Integer> listFieldN)
    {
        intField = intFieldN;
        boolField = boolFieldN;
        wrapBoolField = wrapBoolFieldN;
        doubleField = doubleFieldN;
        enumField = enumFieldN;
        nullableField = nullableFieldN;
        listField = listFieldN;
    }
     
    public int getIntField()
    {
        return intField;
    }

    public void setIntField(int intFieldN)
    {
        intField = intFieldN;
    }
    
    public int getWrapIntField()
    {
        return wrapIntField;
    }
    
    public void setWrapIntField(int intFieldN)
    {
        wrapIntField = intFieldN;
    }
    
    public boolean isSetWrapIntField()
    {
        if (wrapIntField == null)
        {
            return false;
        }
        return true;
    }
    
    public void unsetWrapIntField()
    {
        wrapIntField = null;
    }

    public void setBoolField(boolean boolFieldN)
    {
        boolField = boolFieldN;
    }
    
    public boolean isBoolField()
    {
        return boolField;
    }

    public double getDoubleField()
    {
        return doubleField;
    }

    public void setDoubleField(double doubleFieldN)
    {
        doubleField = doubleFieldN;
    }
    
    public void setEnumField(TestEnum enumFieldN)
    {
        enumField = enumFieldN;
    }
     
    public TestEnum getEnumField()
    {
        return enumField;
    }
    
    public Float getNullableField()
    {
        return nullableField;
    }
    
    public void setNullableField(Float nullableFieldN)
    {
        nullableField = nullableFieldN;
    }
    
    public List<Integer> getListField()
    {
        if (listField == null)
        {
            listField = new ArrayList<Integer>();
        }
        return listField;
    }
    
    public void setListField(List<Integer> listFieldN)
    {
        listField = listFieldN;
    }
    
    public Boolean isWrapBoolField()
    {
        return wrapBoolField;
    }
    
    public void setWrapBoolField(Boolean boolFieldN)
    {
        wrapBoolField = boolFieldN;
    }
    
    /**
     * Enumeration created solely for use with this test class.
     */
    public enum TestEnum
    {
        TestEnum1,
        TestEnum2,
        TestEnum3
    }
}
