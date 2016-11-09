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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mil.dod.th.core.ose.jaxbprotoconverter.ExampleAnotherJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbSuperClass;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author cweisenborn
 */
public class TestProtoFileGenerator
{
    private ProtoFileGenerator m_SUT;
    private FileService m_FileService;
    private File m_XsdFile1;
    private File m_XsdFile2;
    private File m_XsdFilesDir;
    private PrintStream m_PrintStream;
    
    @Before
    public void setup()
    {   
        m_FileService = mock(FileService.class);
        m_XsdFile1 = mock(File.class);
        m_XsdFile2 = mock(File.class);
        m_XsdFilesDir = mock(File.class);
        m_SUT = new ProtoFileGenerator(m_FileService);
    }
    
    public static void assertField(String text, String message, String modifier, String type, String name, int index, 
            boolean packed, boolean override)
    {    
        // should be modifier<space>type<space>name<space>=<space><field-id>;
        StringBuilder fieldBuilder = 
                new StringBuilder("\\s+" + modifier + "\\s+" + type + "\\s+" + name + "\\s+=\\s+" + index);
        if (packed)
        {
            fieldBuilder.append("\\s+\\[packed=true\\]");
        }
      
        String field;
        if (override)
        {
            fieldBuilder.append(";\n");
            String overrideStr = "\\s+//------|Index Overridden|------//\n";
            field = overrideStr + fieldBuilder.toString() + overrideStr;
        }
        else
        {
            field = fieldBuilder.toString();
        }
        
        String regex = ".*message\\s+" + message + "\\s+\\{.*" + field + ".*\\}";
        Pattern p = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(text);    
        assertThat(" Pattern " + regex + " is not contained in " + text, m.find(), is(true));
    }
    
    public static void assertEnum(String text, String message, String enumName, String... values)
    {
        StringBuilder enumStrBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++)
        {
            enumStrBuilder.append(values[i] + "\\s+=\\s+" + (i+1) + ";\\s+");
        }
        String enumRegex = "enum\\s+" + enumName + "\\s+\\{\\s+.*" + enumStrBuilder.toString() + ".*\\}";
        String regex = ".*message\\s+" + message + "\\s+\\{.*" + enumRegex + ".*\\}";
        
        Pattern p = Pattern.compile(regex, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(text);  
        assertThat(" Pattern " + regex + " is not contained in " + text, m.find(), is(true));
    }
    
    @Test
    public void testGenProtoFile() throws Exception
    {
        String packageName = "test.package";
        ByteArrayOutputStream baot = new ByteArrayOutputStream();
        m_PrintStream = new PrintStream(baot);
        File outputDir = mock(File.class);
        File parentXsdFile1 = mock(File.class);
        File parentXsdFile2 = mock(File.class);
        Path xsdFilesDirPath = mock(Path.class);
        Path parentXsdFile1Path = mock(Path.class);
        Path parentXsdFile2Path = mock(Path.class);
        
        when(m_XsdFilesDir.toPath()).thenReturn(xsdFilesDirPath);
        when(xsdFilesDirPath.relativize(parentXsdFile1Path)).thenReturn(parentXsdFile1Path);
        when(xsdFilesDirPath.relativize(parentXsdFile2Path)).thenReturn(parentXsdFile2Path);
        when(m_XsdFile1.getName()).thenReturn("Messages1.xsd");
        when(m_XsdFile1.getParentFile()).thenReturn(parentXsdFile1);
        when(parentXsdFile1.toPath()).thenReturn(parentXsdFile1Path);
        when(parentXsdFile1Path.toString()).thenReturn("some" + File.separator + "path1");
        when(m_XsdFile2.getName()).thenReturn("Messages2.xsd");
        when(m_XsdFile2.getParentFile()).thenReturn(parentXsdFile2);
        when(parentXsdFile2.toPath()).thenReturn(parentXsdFile2Path);
        when(parentXsdFile2Path.toString()).thenReturn("some" + File.separator + "path2");
        when(m_FileService.createPrintStream(Mockito.any(File.class))).thenReturn(m_PrintStream);
        
        //This test converter creates the model based on the example jaxb classes
        ProtoModel protoModel = ConverterTestUtils.createProtoModel(m_XsdFile1, m_XsdFile2);
        m_SUT.generateProtoFiles(protoModel, m_XsdFilesDir, outputDir, packageName);
        
        String fullOutput = baot.toString();
        int indexTrim = fullOutput.indexOf("option");
        String output = fullOutput.substring(indexTrim).trim();

        String options = "option java_package = \"test.package.some.path1\";"
                       + System.lineSeparator()+ "option java_outer_classname = \"Messages1Gen\";";
        Pattern p = Pattern.compile(options, Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(output);
        assertThat("Pattern " + options + " is not contained in " + output, m.find(), is(true));
        
        options = "option java_package = \"test.package.some.path2\";"
                + System.lineSeparator()+ "option java_outer_classname = \"Messages2Gen\";";
        p = Pattern.compile(options, Pattern.DOTALL | Pattern.MULTILINE);
        m = p.matcher(output);
        assertThat("Pattern " + options + " is not contained in " + output, m.find(), is(true));
        
        assertEnum(output, "ExampleJaxbClass", "Enum", "VALUE_1", "VALUE_2");

        assertField(output, ExampleJaxbClass.class.getSimpleName(), "required", 
                ExampleJaxbSuperClass.class.getSimpleName(), "_base", 50, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "optional", "string", "testStr", 1, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "optional", "string", "testStr", 1, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "required", "bool", "testBool", 3, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "optional", "int64", "testLong", 5, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), 
                "optional", "double", "testDouble", 6, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "required", "float", "testFloat", 7, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), 
                "optional", "bytes", "testByteArray", 8, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "repeated", "string", "testList", 9, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), 
                "optional", "unint32", "testOverride", 25, false, true);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "required", 
                ExampleAnotherJaxbClass.class.getSimpleName(), "exampleReference", 10, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), "required", 
                "ExampleJaxb.Enum", "exampleEnumReference", 11, false, false);
        assertField(output, ExampleJaxbClass.class.getSimpleName(), 
                "repeated", "uint64", "testRepeatedU64", 12, true, false);
        
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "optional", "string", "addTestStr", 1, false, false);
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "optional", "int32", "addTestInt", 2, false, false);
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "required", "bool", "addTestBool", 3, false, false);
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "optional", "int64", "addTestLong", 4, false, false);
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "optional", "double", "addTestDouble", 5, false, false);
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "required", "float", "addTestFloat", 6, false, false);
        assertField(output, ExampleAnotherJaxbClass.class.getSimpleName(), 
                "optional", "bytes", "addTestByteArray", 7, false, false);
        
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "optional", "string", "extTestStr", 1, false, false);
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "optional", "int32", "extTestInt", 2, false, false);
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "required", "bool", "extTestBool", 3, false, false);
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "optional", "int64", "extTestLong", 4, false, false);
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "optional", "double", "extTestDouble", 5, false, false);
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "required", "float", "extTestFloat", 6, false, false);
        assertField(output, ExampleJaxbSuperClass.class.getSimpleName(), 
                "optional", "bytes", "extTestByteArray", 7, false, false);
    }
}

