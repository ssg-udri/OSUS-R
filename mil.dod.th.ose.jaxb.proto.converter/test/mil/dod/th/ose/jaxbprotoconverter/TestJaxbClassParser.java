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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import mil.dod.th.core.ose.jaxbprotoconverter.ExampleAnotherJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbSuperClass;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoEnum;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoFile;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoMessage;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.ProtoType;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoField.Rule;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdModel;

import org.junit.Before;
import org.junit.Test;

/**
 * @author cweisenborn
 */
public class TestJaxbClassParser
{
    private JaxbClassParser m_SUT;
    private File m_XsdFile1;
    private File m_XsdFile2;
    private File m_XsdFilesDir;
    
    private Path m_XsdFile1Path;
    private Path m_XsdFile2Path;
    private Path m_XsdFilesDirPath;
    
    @Before
    public void setup()
    {        
        m_XsdFile1 = mock(File.class);
        m_XsdFile2 = mock(File.class);
        m_XsdFilesDir = mock(File.class);
        m_XsdFile1Path = mock(Path.class);
        m_XsdFile2Path = mock(Path.class);
        m_XsdFilesDirPath = mock(Path.class);
        
        when(m_XsdFile1.toPath()).thenReturn(m_XsdFile1Path);
        when(m_XsdFile2.toPath()).thenReturn(m_XsdFile2Path);
        when(m_XsdFilesDir.toPath()).thenReturn(m_XsdFilesDirPath);
        when(m_XsdFilesDirPath.relativize(m_XsdFile1Path)).thenReturn(m_XsdFile1Path);
        when(m_XsdFilesDirPath.relativize(m_XsdFile2Path)).thenReturn(m_XsdFile2Path);
        
        when(m_XsdFile1Path.toString())
            .thenReturn("some" + File.separator + "path1" + File.separator + "Messages1.xsd");
        when(m_XsdFile2Path.toString())
            .thenReturn("some" + File.separator + "path2" + File.separator + "Messages2.xsd");
        
        m_SUT = new JaxbClassParser();
    }
    
    @Test
    public void testParseJaxbClasses() throws Exception
    {
        Map<String, String> importableMessages = new HashMap<String, String>();
        XsdModel xsdModel = ConverterTestUtils.createXsdModel(m_XsdFile1, m_XsdFile2);
        ProtoModel test = m_SUT.parseJaxbClasses(xsdModel, importableMessages, m_XsdFilesDir);
        ProtoFile protoFile = test.getProtoFileMap().get(m_XsdFile1);
        Map<String, ProtoMessage> testMessages = protoFile.getMessageMap();
        ProtoMessage subMessage = testMessages.get(ExampleJaxbClass.class.getSimpleName());
        Map<String, ProtoField> testFields = subMessage.getFields();
        
        assertThat(protoFile.getImports().size(), is(1));
        assertThat(protoFile.getImports(), contains("some/path2/Messages2.proto"));
        
        assertThat(testFields.containsKey("testStr"), equalTo(true));
        assertThat(testFields.get("testStr").getType(), equalTo(ProtoType.String));
        assertThat(testFields.get("testStr").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("testINT"), equalTo(true));
        assertThat(testFields.get("testINT").getType(), equalTo(ProtoType.Int32));
        assertThat(testFields.get("testINT").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("testBool"), equalTo(true));
        assertThat(testFields.get("testBool").getType(), equalTo(ProtoType.Boolean));
        assertThat(testFields.get("testBool").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields.containsKey("testLong"), equalTo(true));
        assertThat(testFields.get("testLong").getType(), equalTo(ProtoType.Int64));
        assertThat(testFields.get("testLong").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("testDouble"), equalTo(true));
        assertThat(testFields.get("testDouble").getType(), equalTo(ProtoType.Double));
        assertThat(testFields.get("testDouble").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("testFloat"), equalTo(true));
        assertThat(testFields.get("testFloat").getType(), equalTo(ProtoType.Float));
        assertThat(testFields.get("testFloat").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields.containsKey("testByteArray"), equalTo(true));
        assertThat(testFields.get("testByteArray").getType(), equalTo(ProtoType.Bytes));
        assertThat(testFields.get("testByteArray").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("testList"), equalTo(true));
        assertThat(testFields.get("testList").getType(), equalTo(ProtoType.String));
        assertThat(testFields.get("testList").getRule(), equalTo(Rule.Repeated));
        
        assertThat(testFields.containsKey("exampleReference"), equalTo(true));
        assertThat(testFields.get("exampleReference").getType(), equalTo(ProtoType.Reference));
        assertThat(testFields.get("exampleReference").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields, hasKey("exampleEnumReference"));
        assertThat(testFields.get("exampleEnumReference").getType(), equalTo(ProtoType.Enum));
        assertThat(testFields.get("exampleEnumReference").getRule(), equalTo(Rule.Optional));
        
        // Verify deprecated fields do not show up in the model.
        assertThat(testFields.containsKey("_DEPRECATED_testOldStr"), equalTo(false));
        //Verify that super class fields are not in the model.
        assertThat(testFields.containsKey("extTestStr"), equalTo(false));
        assertThat(testFields.containsKey("extTestInt"), equalTo(false));
        assertThat(testFields.containsKey("extTestBool"), equalTo(false));
        assertThat(testFields.containsKey("extTestLong"), equalTo(false));
        assertThat(testFields.containsKey("extTestDouble"), equalTo(false));
        assertThat(testFields.containsKey("extTestFloat"), equalTo(false));
        assertThat(testFields.containsKey("extTestByteArray"), equalTo(false));
        
        ProtoMessage superMessage = testMessages.get(ExampleJaxbSuperClass.class.getSimpleName());
        assertThat(subMessage.getBaseMessage(), is(superMessage));
        
        testFields = superMessage.getFields();
        assertThat(testFields.containsKey("extTestStr"), equalTo(true));
        assertThat(testFields.get("extTestStr").getType(), equalTo(ProtoType.String));
        assertThat(testFields.get("extTestStr").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("extTestInt"), equalTo(true));
        assertThat(testFields.get("extTestInt").getType(), equalTo(ProtoType.Int32));
        assertThat(testFields.get("extTestInt").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("extTestBool"), equalTo(true));
        assertThat(testFields.get("extTestBool").getType(), equalTo(ProtoType.Boolean));
        assertThat(testFields.get("extTestBool").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields.containsKey("extTestLong"), equalTo(true));
        assertThat(testFields.get("extTestLong").getType(), equalTo(ProtoType.Int64));
        assertThat(testFields.get("extTestLong").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("extTestDouble"), equalTo(true));
        assertThat(testFields.get("extTestDouble").getType(), equalTo(ProtoType.Double));
        assertThat(testFields.get("extTestDouble").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("extTestFloat"), equalTo(true));
        assertThat(testFields.get("extTestFloat").getType(), equalTo(ProtoType.Float));
        assertThat(testFields.get("extTestFloat").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields.containsKey("extTestByteArray"), equalTo(true));
        assertThat(testFields.get("extTestByteArray").getType(), equalTo(ProtoType.Bytes));
        assertThat(testFields.get("extTestByteArray").getRule(), equalTo(Rule.Optional));
        
        protoFile = test.getProtoFileMap().get(m_XsdFile2);
        testMessages = protoFile.getMessageMap();
        testFields = testMessages.get(ExampleAnotherJaxbClass.class.getSimpleName()).getFields();
        assertThat(protoFile.getImports().size(), is(0));
        
        assertThat(testFields.containsKey("addTestStr"), equalTo(true));
        assertThat(testFields.get("addTestStr").getType(), equalTo(ProtoType.String));
        assertThat(testFields.get("addTestStr").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("addTestInt"), equalTo(true));
        assertThat(testFields.get("addTestInt").getType(), equalTo(ProtoType.Int32));
        assertThat(testFields.get("addTestInt").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("addTestBool"), equalTo(true));
        assertThat(testFields.get("addTestBool").getType(), equalTo(ProtoType.Boolean));
        assertThat(testFields.get("addTestBool").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields.containsKey("addTestLong"), equalTo(true));
        assertThat(testFields.get("addTestLong").getType(), equalTo(ProtoType.Int64));
        assertThat(testFields.get("addTestLong").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("addTestDouble"), equalTo(true));
        assertThat(testFields.get("addTestDouble").getType(), equalTo(ProtoType.Double));
        assertThat(testFields.get("addTestDouble").getRule(), equalTo(Rule.Optional));
        
        assertThat(testFields.containsKey("addTestFloat"), equalTo(true));
        assertThat(testFields.get("addTestFloat").getType(), equalTo(ProtoType.Float));
        assertThat(testFields.get("addTestFloat").getRule(), equalTo(Rule.Required));
        
        assertThat(testFields.containsKey("addTestByteArray"), equalTo(true));
        assertThat(testFields.get("addTestByteArray").getType(), equalTo(ProtoType.Bytes));
        assertThat(testFields.get("addTestByteArray").getRule(), equalTo(Rule.Optional));
        
        ProtoMessage enumMessage = testMessages.get("ExampleJaxb");
        assertThat(enumMessage.getFields().size(), is(0));
        ProtoEnum protoEnum = enumMessage.getEnumeration();
        assertThat(protoEnum.getValues(), contains("VALUE_1", "VALUE_2"));
    }
}
