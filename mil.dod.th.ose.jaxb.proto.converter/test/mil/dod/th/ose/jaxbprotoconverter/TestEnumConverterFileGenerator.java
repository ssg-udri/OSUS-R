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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoEnum;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;

/**
 * Test class for the enum converter.
 * @author allenchl
 *
 */
public class TestEnumConverterFileGenerator
{
    private EnumConverterFileGenerator m_SUT;
    private FileService m_FileService;
    private PrintStream m_PrintStream;
    
    @Before
    public void setup()
    {
        m_FileService = mock(FileService.class);
        m_SUT = new EnumConverterFileGenerator(m_FileService);
    }
    
    /**
     * Verify creation of enum converter file based of a proto enum.
     */
    @Test
    public void testGenEnumConverterFile() throws IOException
    {
        //construct enum
        ProtoEnum protoEnum = new ProtoEnum(null, null, null, null, null);
        protoEnum.setType(TestEnum.class);
        protoEnum.setName("Enum");
        String enum1 = "One";
        String enum2 = "Two";
        List<String> enumVals = new ArrayList<String>();
        enumVals.add(enum1);
        enumVals.add(enum2);
        
        //add the enum values
        protoEnum.setValues(enumVals);
        
        ByteArrayOutputStream baot = new ByteArrayOutputStream();
        m_PrintStream = new PrintStream(baot);
        String protoClassName = "ProtoClassName";
        String packageName = "test.package";
        
        m_SUT.genEnumConverterFile(protoEnum, m_PrintStream, protoClassName, "test.proto", packageName);
        
        String expectFileContent = new String(Files.readAllBytes(
                new File("resources/TestProtoConverter.java").toPath()));
        
        //normalize line endings for easy comparison
        String output = normalizeLineEndings(baot.toString());
        
        //verify timestamp generation
        output = assertTimeStampGeneration(output);
        
        //remove header from file input
        expectFileContent = removeFileHeader(expectFileContent);
        //normalize file input
        expectFileContent = normalizeLineEndings(expectFileContent);

        //trim off ending/starting new lines
        assertThat(output.trim(), is(expectFileContent.trim()));
        
        //create clean stream and printstream
        baot = new ByteArrayOutputStream();
        m_PrintStream = new PrintStream(baot);
        
        m_SUT.genEnumConverterWrapperFile(m_PrintStream, "TestEnum", "test.package");

        expectFileContent = new String(Files.readAllBytes(
                new File("resources/TestProtoConverterGenerator.java").toPath()));

        //normalize line endings for easy comparison
        output = normalizeLineEndings(baot.toString());
        
        //verify timestamp generation
        output = assertTimeStampGeneration(output);
        
        //remove header from file input
        expectFileContent = removeFileHeader(expectFileContent);
        //normalize file input
        expectFileContent = normalizeLineEndings(expectFileContent);
        
        //trim off ending/starting new lines
        assertThat(output.trim(), is(expectFileContent.trim())); 
    }
    
    /**
     * Sample enum.
     *
     */
    @XmlType(name = "TestEnum")
    @XmlEnum
    public enum TestEnum {

        @XmlEnumValue("One")
        ONE("One"),
        @XmlEnumValue("Two")
        TWO("Two");
        
        private final String value;

        TestEnum(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static TestEnum fromValue(String v) {
            for (TestEnum c: TestEnum.values()) {
                if (c.value.equals(v)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(v);
        }
    }
       
    /**
     * Normalize line endings of output stream.
     */
    private String normalizeLineEndings(String data)
    {
        Pattern whitespaceCharsPattern = Pattern.compile("\r\n", 
                Pattern.DOTALL | Pattern.MULTILINE);
        Matcher whitespaceCharsMatcher = whitespaceCharsPattern.matcher(data);
        return whitespaceCharsMatcher.replaceAll("\n");
    }
    
    /**
     * Verify and remove generated time stamp data.
     */
    private String assertTimeStampGeneration(String data)
    {
        //verify date stamp is present, and then remove it
        Pattern p = Pattern.compile("(0|1)[0-9][/][0-3][0-9][/](20)\\d\\d.[0-2][0-9][:][0-5][0-9][:][0-5][0-9]", 
                Pattern.DOTALL | Pattern.MULTILINE);
        Matcher m = p.matcher(data);
        assertThat("The generated date is contained in output." + data, m.find(), is(true));
        int indexTrim = data.indexOf("\n"); //it is followed by a new line char
        return data.substring(indexTrim);
    }
    
    /**
     * Remove header from a string of data.
     */
    private String removeFileHeader(String data)
    {
        //three equals signs is the end of the header on the file
        int indexOfHeaderEnding = data.lastIndexOf("===");
        //cut out header including the three equal signs of the comment
        return data.substring(indexOfHeaderEnding + 3);
    }
}
