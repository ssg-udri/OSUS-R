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
package mil.dod.th.ose.lexicon.doc.main;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import mil.dod.th.ose.lexicon.doc.markup.MarkupGenerator;
import mil.dod.th.ose.lexicon.doc.parser.LexiconBase;
import mil.dod.th.ose.lexicon.doc.parser.LexiconParser;
import mil.dod.th.ose.utils.FileService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.xml.sax.SAXException;

/**
 * Test class for {@link LexiconDocGeneratorMain}.
 * 
 * @author cweisenborn
 */
public class TestLexiconDocGeneratorMain
{
    private LexiconDocGeneratorMain m_SUT;
    private @Mock FileService m_FileService;
    private @Mock File m_XsdDir;
    private @Mock File m_OutputFile;
    private @Mock MarkupGenerator m_MarkupGen;
    private @Mock LexiconParser m_LexiconParser;
    private @Mock PrintStream m_PrintStream;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new LexiconDocGeneratorMain(m_FileService, m_LexiconParser, m_MarkupGen);
    }
    
    /**
     * Verify that usage statement is printed appropriately.
     */
    @Test
    public void testUsage() throws ParserConfigurationException, SAXException, IOException
    {
        m_SUT.processCommandLine(m_PrintStream, new String[0]);
        
        verify(m_PrintStream).println("Invalid number of arguments!\n"
                    + "Usage: java -jar mil.dod.th.ose.lexicon.doc.jar "
                    + "<Output File>"
                    + "<Schema Directory Path> ");
    }
    
    /**
     * Verify that the appropriate calls are made based off the arguments passed.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testProcessCommandLine() throws ParserConfigurationException, SAXException, IOException
    {
        final List<LexiconBase> types = new ArrayList<>();
        
        when(m_FileService.getFile("output")).thenReturn(m_OutputFile);
        when(m_FileService.getFile("xsdDir")).thenReturn(m_XsdDir);
        
        when(m_XsdDir.exists()).thenReturn(true);
        when(m_XsdDir.isDirectory()).thenReturn(true);
        
        when(m_LexiconParser.parserXsdFiles(Mockito.anyList(), Mockito.any(DocumentBuilder.class))).thenReturn(types);
        
        final String[] args = new String[]{ "output", "xsdDir"};
        m_SUT.processCommandLine(m_PrintStream, args);
        
        verify(m_LexiconParser).parserXsdFiles(Mockito.anyList(), Mockito.any(DocumentBuilder.class));
        verify(m_MarkupGen).generateMarkup(m_OutputFile, types);
    }
}
