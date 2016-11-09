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

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

/**
 * Test class created solely for use with the {@link TestAssetCommandViewImpl} test class.
 * 
 * @author cweisenborn
 */
public class ExampleClass extends ExampleSuperClass
{
    private Integer serialVersionUID;
    @XmlValue
    private List<ExampleComplexClass> complexListField;
    @XmlValue
    private ExampleComplexClass nullComplexField;
    @XmlValue
    private TestEnum enumField;
    @XmlValue
    private Double doubleField;
    @XmlValue
    private Float floatField;
    @XmlValue
    private Long longField;
    @XmlValue
    private Short shortField;
    @XmlValue
    private Character charField;
    @XmlValue
    private Boolean boolField;
    @XmlValue
    private Byte byteField;
    @XmlValue
    private String stringField;
    @XmlValue
    private Integer intWrapperField;
    @XmlValue
    private Double doubleWrapperField;
    @XmlValue
    private Float floatWrapperField;
    @XmlValue
    private Long longWrapperField;
    @XmlValue
    private Short shortWrapperField;
    @XmlValue
    private Character charWrapperField;
    @XmlValue
    private Boolean boolWrapperField;
    @XmlValue
    private Byte byteWrapperField;
    @XmlTransient
    private String stringTransientField;

    public ExampleClass()
    {
        super();
        complexListField = new ArrayList<ExampleComplexClass>();
        complexListField.add(new ExampleComplexClass());
        nullComplexField = null;
        enumField = TestEnum.Value1;
        stringField = "some string";
        intWrapperField = 5;
        doubleWrapperField = 4.55;
        floatWrapperField = 2.1f;
        longWrapperField = (long)454545;
        shortWrapperField = (short)4;
        charWrapperField = 'A';
        boolWrapperField = true;
        byteWrapperField = 75;
        stringTransientField = "DOESNOTMATTER!!!";
    }
    
    public List<ExampleComplexClass> getComplexListField()
    {
        return complexListField;
    }

    public void setComplexListField(List<ExampleComplexClass> field)
    {
        this.complexListField = field;
    }
    
    public boolean isSetComplexListField()
    {
        return (complexListField != null) && (!complexListField.isEmpty());
    }

    public ExampleComplexClass getNullComplexField()
    {
        return nullComplexField;
    }

    public void setNullComplexField(ExampleComplexClass field)
    {
        this.nullComplexField = field;
    }
    
    public boolean isSetNullComplexListField()
    {
        return nullComplexField != null;
    }

    public double getDoubleField()
    {
        return doubleField;
    }

    public void setDoubleField(double field)
    {
        this.doubleField = field;
    }
    
    public boolean isSetDoubleField()
    {
        return doubleField != null;
    }

    public float getFloatField()
    {
        return floatField;
    }

    public void setFloatField(float field)
    {
        this.floatField = field;
    }
    
    public boolean isSetFloatField()
    {
        return floatField != null;
    }

    public long getLongField()
    {
        return longField;
    }

    public void setLongField(long field)
    {
        this.longField = field;
    }
    
    public boolean isSetLongField()
    {
        return longField != null;
    }

    public short getShortField()
    {
        return shortField;
    }

    public void setShortField(short field)
    {
        this.shortField = field;
    }
    
    public boolean isSetShortField()
    {
        return shortField != null;
    }

    public char getCharField()
    {
        return charField;
    }

    public void setCharField(char field)
    {
        this.charField = field;
    }
    
    public boolean isSetCharField()
    {
        return charField != null;
    }

    public boolean isBoolField()
    {
        return boolField;
    }

    public void setBoolField(boolean field)
    {
        this.boolField = field;
    }
    
    public boolean isSetBoolField()
    {
        return boolField != null;
    }

    public byte getByteField()
    {
        return byteField;
    }

    public void setByteField(byte field)
    {
        this.byteField = field;
    }
    
    public boolean isSetByteField()
    {
        return byteField != null;
    }

    public String getStringField()
    {
        return stringField;
    }

    public void setStringField(String field)
    {
        this.stringField = field;
    }
    
    public boolean isSetStringField()
    {
        return stringField != null;
    }

    public Integer getIntWrapperField()
    {
        return intWrapperField;
    }

    public void setIntWrapperField(Integer field)
    {
        this.intWrapperField = field;
    }
    
    public boolean isSetIntWrapperField()
    {
        return intWrapperField != null;
    }

    public Double getDoubleWrapperField()
    {
        return doubleWrapperField;
    }

    public void setDoubleWrapperField(Double field)
    {
        this.doubleWrapperField = field;
    }
    
    public boolean isSetDoubleWrapperField()
    {
        return doubleWrapperField != null;
    }

    public Float getFloatWrapperField()
    {
        return floatWrapperField;
    }

    public void setFloatWrapperField(Float field)
    {
        this.floatWrapperField = field;
    }
    
    public boolean isSetFloatWrapperField()
    {
        return floatWrapperField != null;
    }

    public Long getLongWrapperField()
    {
        return longWrapperField;
    }

    public void setLongWrapperField(Long field)
    {
        this.longWrapperField = field;
    }
    
    public boolean isSetLongWrapperField()
    {
        return longWrapperField != null;
    }

    public Short getShortWrapperField()
    {
        return shortWrapperField;
    }

    public void setShortWrapperField(Short field)
    {
        this.shortWrapperField = field;
    }
    
    public boolean isSetShortWrapperField()
    {
        return shortWrapperField != null;
    }

    public Character getCharWrapperField()
    {
        return charWrapperField;
    }

    public void setCharWrapperField(Character field)
    {
        this.charWrapperField = field;
    }
    
    public boolean isSetCharWrapperField()
    {
        return charWrapperField != null;
    }

    public Boolean isBoolWrapperField()
    {
        return boolWrapperField;
    }

    public void setBoolWrapperField(Boolean field)
    {
        this.boolWrapperField = field;
    }
    
    public boolean isSetBoolWrapperField()
    {
        return boolWrapperField != null;
    }

    public Byte getByteWrapperField()
    {
        return byteWrapperField;
    }

    public void setByteWrapperField(Byte field)
    {
        this.byteWrapperField = field;
    }
    
    public boolean isSetByteWrapperField()
    {
        return byteWrapperField != null;
    }
    
    public TestEnum getEnumField()
    {
        return enumField;
    }

    public void setEnumField(TestEnum field)
    {
        this.enumField = field;
    }
    
    public boolean isSetEnumField()
    {
        return enumField != null;
    }
    
    public Integer getSerialVersionUID()
    {
        return serialVersionUID;
    }

    public void setSerialVersionUID(Integer field)
    {
        this.serialVersionUID = field;
    }
    
    public void setStringTransientField(String field)
    {
        this.stringTransientField = field;
    }
    
    public String getStringTransientField()
    {
        return stringTransientField;
    }

    public enum TestEnum
    {
        Value1,
        Value2,
        Value3;
    }
}
