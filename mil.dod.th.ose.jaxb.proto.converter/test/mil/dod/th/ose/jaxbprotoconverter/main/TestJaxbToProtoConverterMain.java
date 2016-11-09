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
package mil.dod.th.ose.jaxbprotoconverter.main;

import static org.mockito.Mockito.*;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import mil.dod.th.ose.jaxbprotoconverter.JaxbClassParser;
import mil.dod.th.ose.jaxbprotoconverter.JaxbProtoConvertException;
import mil.dod.th.ose.jaxbprotoconverter.EnumConverterFileGenerator;
import mil.dod.th.ose.jaxbprotoconverter.ProtoFileGenerator;
import mil.dod.th.ose.jaxbprotoconverter.ProtoFileParser;
import mil.dod.th.ose.jaxbprotoconverter.proto.ProtoModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdModel;
import mil.dod.th.ose.jaxbprotoconverter.xsd.XsdParser;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

/**
 * @author dhumeniuk
 */
public class TestJaxbToProtoConverterMain
{
    private JaxbToProtoConverterMain m_SUT;
    private EnumConverterFileGenerator m_EnumConverterFileGenerator;
    private ProtoFileGenerator m_ProtoFileGenerator;
    private XsdParser m_XsdParser;
    private JaxbClassParser m_JaxbClassParser;
    private FileService m_FileService;
    private ProtoFileParser m_ProtoFileParser;
    
    @Before
    public void setUp()
    {
        m_ProtoFileGenerator = mock(ProtoFileGenerator.class);
        m_EnumConverterFileGenerator = mock(EnumConverterFileGenerator.class);
        m_FileService = mock(FileService.class);
        m_XsdParser = mock(XsdParser.class);
        m_JaxbClassParser = mock(JaxbClassParser.class);
        m_ProtoFileParser = mock(ProtoFileParser.class);
        m_SUT = new JaxbToProtoConverterMain(m_ProtoFileGenerator, m_XsdParser, m_EnumConverterFileGenerator, 
                m_FileService, m_JaxbClassParser, m_ProtoFileParser);
    }
    
    /**
     * Test that providing no args will cause the usage statement to be printed.
     */
    @Test
    public void testProcessCommandLineNoArgs() throws JaxbProtoConvertException
    {
        PrintStream out = mock(PrintStream.class);
        m_SUT.processCommandLine(out, new String[]{});
        
        verify(out).println(JaxbToProtoConverterMain.getUsage());
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testProcess() throws JaxbProtoConvertException, IOException, ClassNotFoundException, 
        ParserConfigurationException, SAXException
    {
        File xsdFilesDir = mock(File.class);
        File protoDestPath = mock(File.class);
        File javaDestPath = mock(File.class);
        File enumConvertersDir = mock(File.class);
        XsdModel xsdModel = new XsdModel();
        ProtoModel protoModel = new ProtoModel();
        String enumPath = "some" + File.separator + "path" + File.separator + "enum" + File.separator + "pack" 
                + File.separator + "name";
        String javaPackName = "java.pack.name";
        String enumPackName = "enum.pack.name";
        String importPath = "importPath";
        
        Map<String, String> imports = new HashMap<String, String>();
        imports.put("msgName1", "file1");
        imports.put("msgName2", "file2");
        
        when(javaDestPath.getPath()).thenReturn("some" + File.separator + "path");
        when(xsdFilesDir.exists()).thenReturn(true);
        when(xsdFilesDir.isDirectory()).thenReturn(true);
        when(m_XsdParser.parseXsdFiles(Mockito.any(DocumentBuilder.class), Mockito.any(File.class), 
                Mockito.anyList())).thenReturn(xsdModel);
        when(m_ProtoFileParser.parseProtoFilesForTypes(importPath)).thenReturn(imports);
        when(m_JaxbClassParser.parseJaxbClasses(xsdModel, imports, xsdFilesDir)).thenReturn(protoModel);
        when(m_FileService.getFile(enumPath)).thenReturn(enumConvertersDir);
        
        m_SUT.process(xsdFilesDir, protoDestPath, javaDestPath, javaPackName, enumPackName, importPath);
        
        verify(m_XsdParser).parseXsdFiles(Mockito.any(DocumentBuilder.class), Mockito.any(File.class), 
                Mockito.anyList());
        verify(m_ProtoFileParser).parseProtoFilesForTypes(importPath);
        verify(m_JaxbClassParser).parseJaxbClasses(xsdModel, imports, xsdFilesDir);
        verify(m_ProtoFileGenerator).generateProtoFiles(protoModel, xsdFilesDir, protoDestPath, javaPackName);
        verify(m_FileService).getFile(enumPath);
        verify(m_EnumConverterFileGenerator).generateEnumConverters(protoModel, enumConvertersDir, xsdFilesDir, 
                "EnumConverter", javaPackName, enumPackName);
    }
    
    @Test
    public void testProcessInvalidXsdDir() throws JaxbProtoConvertException
    {
        File xsdFileDir = mock(File.class);
        File protoDestPath = mock(File.class);
        File javaDestPath = mock(File.class);
        
        when(xsdFileDir.exists()).thenReturn(true);
        when(xsdFileDir.isDirectory()).thenReturn(false);
        
        try
        {
            m_SUT.process(xsdFileDir, protoDestPath, javaDestPath, "protoPackName", "enumPackName", "importPath");
            fail("Expecting illegal argument exception");
        }
        catch (final IllegalArgumentException ex)
        {
            //Expected exception
        }
        
        when(xsdFileDir.exists()).thenReturn(false);
        try
        {
            m_SUT.process(xsdFileDir, protoDestPath, javaDestPath, "protoPackName", "enumPackName", "importPath");
            fail("Expecting illegal argument exception");
        }
        catch (final IllegalArgumentException ex)
        {
            //Expected exception
        }
    }
}
