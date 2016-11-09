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
package mil.dod.th.ose.jaxbprotoconverter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mil.dod.th.core.ose.jaxbprotoconverter.ExampleAnotherJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbEnum;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbSuperClass;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoEnum;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoFile;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoMessage;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.ProtoType;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdField;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdNamespace;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdType;

/**
 * Utility class that creates XSD and proto models to be used for testing purposes.
 * 
 * @author cweisenborn
 */
public class ConverterTestUtils
{
    private ConverterTestUtils()
    {
        
    }
    
    /**
     * Creates and {@link XsdModel} based off of {@link ExampleJaxbClass}, {@link ExampleJaxbSuperClass}, 
     * {@link ExampleAnotherJaxbClass}, and {@link ExampleJaxbEnum}.
     */
    public static XsdModel createXsdModel(final File xsdFile1, final File xsdFile2)
    {
        final XsdModel xsdModel = new XsdModel();
        final XsdNamespace xsdNamespace = new XsdNamespace(xsdModel);
        
        xsdModel.getNamespacesMap().put("namespace1", xsdNamespace);
        
        final XsdType jaxbType = new XsdType();
        jaxbType.setXsdNamespace(xsdNamespace);
        jaxbType.setXsdFile(xsdFile1);
        jaxbType.setJaxbType(ExampleJaxbClass.class);
        Map<String, XsdField> fieldMap = jaxbType.getFieldsMap();
        fieldMap.put("testStr", new XsdField(jaxbType, 1, false));
        fieldMap.put("testINT", new XsdField(jaxbType, 2, false));
        fieldMap.put("testBool", new XsdField(jaxbType, 3, false));
        fieldMap.put("_DEPRECATED_testStrOld", new XsdField(jaxbType, 4, false));
        fieldMap.put("testLong", new XsdField(jaxbType, 5, false));
        fieldMap.put("testDouble", new XsdField(jaxbType, 6, false));
        fieldMap.put("testFloat", new XsdField(jaxbType, 7, false));
        fieldMap.put("testByteArray", new XsdField(jaxbType, 8, false));
        fieldMap.put("testList", new XsdField(jaxbType, 9, false));
        fieldMap.put("testOverride", new XsdField(jaxbType, 25, true));
        fieldMap.put("exampleReference", new XsdField(jaxbType, 10, false));
        fieldMap.put("exampleEnumReference", new XsdField(jaxbType, 11, false));
        fieldMap.put("testRepeatedU64", new XsdField(jaxbType, 12, false));
        xsdNamespace.getTypesMap().put(ExampleJaxbClass.class, jaxbType);

        final XsdType jaxbSuperType = new XsdType();
        jaxbSuperType.setXsdNamespace(xsdNamespace);
        jaxbSuperType.setXsdFile(xsdFile1);
        jaxbSuperType.setJaxbType(ExampleJaxbSuperClass.class);
        fieldMap = jaxbSuperType.getFieldsMap();
        fieldMap.put("extTestStr", new XsdField(jaxbSuperType, 1, false));
        fieldMap.put("extTestInt", new XsdField(jaxbSuperType, 2, false));
        fieldMap.put("extTestBool", new XsdField(jaxbSuperType, 3, false));
        fieldMap.put("extTestLong", new XsdField(jaxbSuperType, 4, false));
        fieldMap.put("extTestDouble", new XsdField(jaxbSuperType, 5, false));
        fieldMap.put("extTestFloat", new XsdField(jaxbSuperType, 6, false));
        fieldMap.put("extTestByteArray", new XsdField(jaxbSuperType, 7, false));
        jaxbType.setBaseType(jaxbSuperType);
        xsdNamespace.getTypesMap().put(ExampleJaxbSuperClass.class, jaxbSuperType);
        
        final XsdType jaxbAnotherType = new XsdType();
        jaxbAnotherType.setXsdNamespace(xsdNamespace);
        jaxbAnotherType.setXsdFile(xsdFile2);
        jaxbAnotherType.setJaxbType(ExampleAnotherJaxbClass.class);
        fieldMap = jaxbAnotherType.getFieldsMap();
        fieldMap.put("addTestStr", new XsdField(jaxbAnotherType, 1, false));
        fieldMap.put("addTestInt", new XsdField(jaxbAnotherType, 2, false));
        fieldMap.put("addTestBool", new XsdField(jaxbAnotherType, 3, false));
        fieldMap.put("addTestLong", new XsdField(jaxbAnotherType, 4, false));
        fieldMap.put("addTestDouble", new XsdField(jaxbAnotherType, 5, false));
        fieldMap.put("addTestFloat", new XsdField(jaxbAnotherType, 6, false));
        fieldMap.put("addTestByteArray", new XsdField(jaxbAnotherType, 7, false));
        xsdNamespace.getTypesMap().put(ExampleAnotherJaxbClass.class, jaxbAnotherType);
        
        final XsdType jaxbEnumType = new XsdType();
        jaxbEnumType.setXsdNamespace(xsdNamespace);
        jaxbEnumType.setXsdFile(xsdFile2);
        jaxbEnumType.setJaxbType(ExampleJaxbEnum.class);
        xsdNamespace.getTypesMap().put(ExampleJaxbEnum.class, jaxbEnumType);
        
        return xsdModel;
    }
    
    /**
     * Creates and {@link ProtoModel} based off of {@link ExampleJaxbClass}, {@link ExampleJaxbSuperClass}, 
     * {@link ExampleAnotherJaxbClass}, and {@link ExampleJaxbEnum}.
     */
    public static ProtoModel createProtoModel(final File xsdFile1, final File xsdFile2)
    {
        XsdModel xsdModel = createXsdModel(xsdFile1, xsdFile2);
        XsdNamespace xsdNamespace = xsdModel.getNamespacesMap().get("namespace1");
        
        ProtoModel protoModel = new ProtoModel();
        ProtoFile protoFile1 = new ProtoFile(protoModel, xsdFile1);
        ProtoFile protoFile2 = new ProtoFile(protoModel, xsdFile2);
        
        protoModel.getProtoFileMap().put(xsdFile1, protoFile1);
        protoModel.getProtoFileMap().put(xsdFile2, protoFile2);
        
        XsdType xsdType = xsdNamespace.getTypesMap().get(ExampleJaxbClass.class);
        ProtoMessage jaxbClassMsg = new ProtoMessage(protoFile1, ExampleJaxbClass.class.getSimpleName(), 
                ExampleJaxbClass.class, xsdType); 
        Map<String, ProtoField> fields = jaxbClassMsg.getFields();
        
        ProtoField jaxbField1 = new ProtoField(jaxbClassMsg);
        jaxbField1.setName("testStr");
        jaxbField1.setRule(Rule.Optional);
        jaxbField1.setType(ProtoType.String);
        fields.put(jaxbField1.getName(), jaxbField1);
        
        ProtoField jaxbField2 = new ProtoField(jaxbClassMsg);
        jaxbField2.setName("testINT");
        jaxbField2.setRule(Rule.Optional);
        jaxbField2.setType(ProtoType.Int32);
        fields.put(jaxbField2.getName(), jaxbField2);
        
        ProtoField jaxbField3 = new ProtoField(jaxbClassMsg);
        jaxbField3.setName("testBool");
        jaxbField3.setRule(Rule.Required);
        jaxbField3.setType(ProtoType.Boolean);
        fields.put(jaxbField3.getName(), jaxbField3);
        
        ProtoField jaxbField4 = new ProtoField(jaxbClassMsg);
        jaxbField4.setName("testLong");
        jaxbField4.setRule(Rule.Optional);
        jaxbField4.setType(ProtoType.Int64);
        fields.put(jaxbField4.getName(), jaxbField4);
        
        ProtoField jaxbField5 = new ProtoField(jaxbClassMsg);
        jaxbField5.setName("testDouble");
        jaxbField5.setRule(Rule.Optional);
        jaxbField5.setType(ProtoType.Double);
        fields.put(jaxbField5.getName(), jaxbField5);
        
        ProtoField jaxbField6 = new ProtoField(jaxbClassMsg);
        jaxbField6.setName("testFloat");
        jaxbField6.setRule(Rule.Required);
        jaxbField6.setType(ProtoType.Float);
        fields.put(jaxbField6.getName(), jaxbField6);
        
        ProtoField jaxbField7 = new ProtoField(jaxbClassMsg);
        jaxbField7.setName("testByteArray");
        jaxbField7.setRule(Rule.Optional);
        jaxbField7.setType(ProtoType.Bytes);
        fields.put(jaxbField7.getName(), jaxbField7);
        
        ProtoField jaxbFieldRepeatedString = new ProtoField(jaxbClassMsg);
        jaxbFieldRepeatedString.setName("testList");
        jaxbFieldRepeatedString.setRule(Rule.Repeated);
        jaxbFieldRepeatedString.setType(ProtoType.String);
        fields.put(jaxbFieldRepeatedString.getName(), jaxbFieldRepeatedString);
        
        ProtoField jaxbOverrideField = new ProtoField(jaxbClassMsg);
        jaxbOverrideField.setName("testOverride");
        jaxbOverrideField.setRule(Rule.Optional);
        jaxbOverrideField.setType(ProtoType.UInt32);
        fields.put(jaxbOverrideField.getName(), jaxbOverrideField);
        
        xsdType = xsdNamespace.getTypesMap().get(ExampleJaxbEnum.class);
        ProtoEnum protoEnum = new ProtoEnum(jaxbClassMsg, protoFile2, "Enum", ExampleJaxbEnum.class, xsdType);
        final List<String> enumValues = new ArrayList<String>();
        for (ExampleJaxbEnum value: ExampleJaxbEnum.values())
        {
            enumValues.add(value.name());
        }
        protoEnum.setValues(enumValues);
        protoModel.getEnums().add(protoEnum);
        jaxbClassMsg.setEnumeration(protoEnum);
        ProtoField jaxbField9 = new ProtoField(jaxbClassMsg);
        jaxbField9.setName("exampleEnumReference");
        jaxbField9.setRule(Rule.Required);
        jaxbField9.setType(ProtoType.Enum);
        jaxbField9.setTypeRef(protoEnum);
        fields.put(jaxbField9.getName(), jaxbField9);
        
        xsdType = xsdNamespace.getTypesMap().get(ExampleAnotherJaxbClass.class);
        ProtoMessage jaxbAnotherClassMsg = new ProtoMessage(protoFile2, ExampleAnotherJaxbClass.class.getSimpleName(), 
                ExampleAnotherJaxbClass.class, xsdType);
        ProtoField jaxbField10 = new ProtoField(jaxbClassMsg);
        jaxbField10.setName("exampleReference");
        jaxbField10.setRule(Rule.Required);
        jaxbField10.setType(ProtoType.Reference);
        jaxbField10.setTypeRef(jaxbAnotherClassMsg);
        fields.put(jaxbField10.getName(), jaxbField10);
        
        ProtoField jaxbRepeatedIntField = new ProtoField(jaxbClassMsg);
        jaxbRepeatedIntField.setName("testRepeatedU64");
        jaxbRepeatedIntField.setRule(Rule.Repeated);
        jaxbRepeatedIntField.setType(ProtoType.UInt64);
        fields.put(jaxbRepeatedIntField.getName(), jaxbRepeatedIntField);
        
        protoFile1.getMessageMap().put(ExampleJaxbClass.class.getSimpleName(), jaxbClassMsg);
        
        //Setup reference messages fields.
        fields = jaxbAnotherClassMsg.getFields();
        ProtoField refField1 = new ProtoField(jaxbAnotherClassMsg);
        refField1.setName("addTestStr");
        refField1.setRule(Rule.Optional);
        refField1.setType(ProtoType.String);
        fields.put(refField1.getName(), refField1);
        
        ProtoField refField2 = new ProtoField(jaxbAnotherClassMsg);
        refField2.setName("addTestInt");
        refField2.setRule(Rule.Optional);
        refField2.setType(ProtoType.Int32);
        fields.put(refField2.getName(), refField2);
        
        ProtoField refField3 = new ProtoField(jaxbAnotherClassMsg);
        refField3.setName("addTestBool");
        refField3.setRule(Rule.Required);
        refField3.setType(ProtoType.Boolean);
        fields.put(refField3.getName(), refField3);
        
        ProtoField refField4 = new ProtoField(jaxbAnotherClassMsg);
        refField4.setName("addTestLong");
        refField4.setRule(Rule.Optional);
        refField4.setType(ProtoType.Int64);
        fields.put(refField4.getName(), refField4);
        
        ProtoField refField5 = new ProtoField(jaxbAnotherClassMsg);
        refField5.setName("addTestDouble");
        refField5.setRule(Rule.Optional);
        refField5.setType(ProtoType.Double);
        fields.put(refField5.getName(), refField5);
        
        ProtoField refField6 = new ProtoField(jaxbAnotherClassMsg);
        refField6.setName("addTestFloat");
        refField6.setRule(Rule.Required);
        refField6.setType(ProtoType.Float);
        fields.put(refField6.getName(), refField6);
        
        ProtoField refField7 = new ProtoField(jaxbAnotherClassMsg);
        refField7.setName("addTestByteArray");
        refField7.setRule(Rule.Optional);
        refField7.setType(ProtoType.Bytes);
        fields.put(refField7.getName(), refField7);
        
        protoFile2.getMessageMap().put(ExampleAnotherJaxbClass.class.getSimpleName(), jaxbAnotherClassMsg);
        
        //Setup the super class message.
        xsdType = xsdNamespace.getTypesMap().get(ExampleJaxbSuperClass.class);
        ProtoMessage jaxbSuperClassMsg = new ProtoMessage(protoFile1, ExampleJaxbSuperClass.class.getSimpleName(), 
                ExampleJaxbSuperClass.class, xsdType);
        jaxbClassMsg.setBaseMessage(jaxbSuperClassMsg);
        fields = jaxbSuperClassMsg.getFields();
        
        ProtoField superField1 = new ProtoField(jaxbSuperClassMsg);
        superField1.setName("extTestStr");
        superField1.setRule(Rule.Optional);
        superField1.setType(ProtoType.String);
        fields.put(superField1.getName(), superField1);
        
        ProtoField superField2 = new ProtoField(jaxbSuperClassMsg);
        superField2.setName("extTestInt");
        superField2.setRule(Rule.Optional);
        superField2.setType(ProtoType.Int32);
        fields.put(superField2.getName(), superField2);
        
        ProtoField superField3 = new ProtoField(jaxbSuperClassMsg);
        superField3.setName("extTestBool");
        superField3.setRule(Rule.Required);
        superField3.setType(ProtoType.Boolean);
        fields.put(superField3.getName(), superField3);
        
        ProtoField superField4 = new ProtoField(jaxbSuperClassMsg);
        superField4.setName("extTestLong");
        superField4.setRule(Rule.Optional);
        superField4.setType(ProtoType.Int64);
        fields.put(superField4.getName(), superField4);
        
        ProtoField superField5 = new ProtoField(jaxbSuperClassMsg);
        superField5.setName("extTestDouble");
        superField5.setRule(Rule.Optional);
        superField5.setType(ProtoType.Double);
        fields.put(superField5.getName(), superField5);
        
        ProtoField superField6 = new ProtoField(jaxbSuperClassMsg);
        superField6.setName("extTestFloat");
        superField6.setRule(Rule.Required);
        superField6.setType(ProtoType.Float);
        fields.put(superField6.getName(), superField6);
        
        ProtoField superField7 = new ProtoField(jaxbSuperClassMsg);
        superField7.setName("extTestByteArray");
        superField7.setRule(Rule.Optional);
        superField7.setType(ProtoType.Bytes);
        fields.put(superField7.getName(), superField7);
        
        protoFile1.getMessageMap().put(ExampleJaxbSuperClass.class.getSimpleName(), jaxbSuperClassMsg);
        
        return protoModel;
    }
}
