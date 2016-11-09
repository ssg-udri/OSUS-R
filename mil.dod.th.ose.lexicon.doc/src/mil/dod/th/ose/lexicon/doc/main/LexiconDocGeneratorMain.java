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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import mil.dod.th.ose.lexicon.doc.markup.MarkupGenerator;
import mil.dod.th.ose.lexicon.doc.parser.LexiconBase;
import mil.dod.th.ose.lexicon.doc.parser.LexiconComparator;
import mil.dod.th.ose.lexicon.doc.parser.LexiconParser;
import mil.dod.th.ose.utils.FileService;
import mil.dod.th.ose.utils.UtilInjectionModule;

import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

/**
 * Creates Confluence mark-up documentation based on XSD schemas.
 *
 * @author cweisenborn
 */
public class LexiconDocGeneratorMain
{
    private static final Logger LOG = Logger.getLogger(LexiconDocGeneratorMain.class.getName());
    private final FileService m_FileService;
    private final LexiconParser m_LexiconParser;
    private final MarkupGenerator m_MarkdownGenerator;
    
    /**
     * Base constructor.
     * 
     * @param fileService
     *      Service used to retrieve files.
     * @param lexiconParser
     *      Service used to parse XSD lexicon files.
     * @param markdownGenerator
     *      Service used to generate mark-up for lexicon files.
     */
    @Inject
    public LexiconDocGeneratorMain(final FileService fileService, final LexiconParser lexiconParser, 
            final MarkupGenerator markdownGenerator)
    {
        m_FileService = fileService;
        m_LexiconParser = lexiconParser;
        m_MarkdownGenerator = markdownGenerator;
    }
    
    /**
     * Main method responsible for calling all methods and instantiating all objects needed to generate mark-up 
     * documentation for XSD lexicon files in the specified directory.
     * 
     * @param args
     *          The file path where mark-up should be generated to and the file path where to the location of XSD 
     *          lexicon files that should be parsed.
     */
    public static void main(final String[] args)
    { 
        try
        {
            final Injector injector = Guice.createInjector(new UtilInjectionModule(), new LocalInjectionModule());
            final LexiconDocGeneratorMain parser = injector.getInstance(LexiconDocGeneratorMain.class);
            
            parser.processCommandLine(System.out, args);
        }
        catch (final Exception ex)
        {
            LOG.log(Level.SEVERE, "An error occurred attempting to generate lexicon documentation.", ex);
            return;
        }
    }
    
    /**
     * Process the command with the given command line arguments.
     * 
     * @param out 
     *      Where to print out messages to.
     * @param args
     *      Command line arguments from main(), see {@link #getUsage()} for details.
     * @throws ParserConfigurationException
     *      Thrown if an error occurs configuring the lexicon parser.
     * @throws SAXException
     *      Thrown if an error occurs parsing an XSD file.
     * @throws IOException
     *      Thrown if an error occurs reading from an XSD file
     */
    public void processCommandLine(final PrintStream out, final String[] args) throws ParserConfigurationException, 
            SAXException, IOException
    {
        final int outputFile = 0;
        final int xsdFileDir = 1;
        final int expectedNumArgs = 2;
        
        if (args.length < expectedNumArgs)
        {
            out.println(getUsage());
            return;
        }
        
        generateDoc(m_FileService.getFile(args[outputFile]), m_FileService.getFile(args[xsdFileDir]));
    }
    
    /**
     * Returns the application line usage help when not enough arguments are passed to the program.
     * 
     * @return 
     *      command line usage info as a string    
     */
    private String getUsage()
    {
        final StringBuilder usage = new StringBuilder(400);
        
        usage.append("Invalid number of arguments!\n"
                    + "Usage: java -jar mil.dod.th.ose.lexicon.doc.jar "
                    + "<Output File>"
                    + "<Schema Directory Path> ");
        
        return usage.toString();
    }
    
    /**
     * Generates mark-up documentation for the XSD files in the specified directory.
     * 
     * @param outputFile
     *      The file the generated mark-up should be written to.
     * @param xsdFileDir
     *      The directory where XSD files are located.
     * @throws ParserConfigurationException
     *      Thrown if an error occurs configuring the lexicon parser.
     * @throws SAXException
     *      Thrown if an error occurs parsing an XSD file.
     * @throws IOException
     *      Thrown if an error occurs reading from an XSD file
     */
    private void generateDoc(final File outputFile, final File xsdFileDir) throws ParserConfigurationException, 
            SAXException, IOException
    {
        LOG.log(Level.INFO, String.format("Lexicon schema location: %s", xsdFileDir.getAbsolutePath()));
        LOG.log(Level.INFO, String.format("Generating lexion markup documentation to: %s", 
                outputFile.getAbsolutePath()));
        
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
        
        final List<File> xsdFiles = createXsdFileList(xsdFileDir);
        final List<LexiconBase> types = m_LexiconParser.parserXsdFiles(xsdFiles, docBuilder);
        
        Collections.sort(types, new LexiconComparator());
        m_MarkdownGenerator.generateMarkup(outputFile, types);
    }
    
    /**
     * Creates a list of all XSD files contained within the specified directory.
     * 
     * @param xsdFilesDir
     *      The directory where XSD files are located.
     * @return
     *      A list of files which represents all XSD files within the specified directory.
     */
    private List<File> createXsdFileList(final File xsdFilesDir)
    {
        if (!xsdFilesDir.exists() || !xsdFilesDir.isDirectory())
        {
            throw new IllegalArgumentException(String.format("Invalid path specified for XSD file location: %s", 
                    xsdFilesDir.getAbsolutePath()));
        }
        
        @SuppressWarnings("unchecked")
        final List<File> xsdFiles = new ArrayList<File>(FileUtils.listFiles(xsdFilesDir, new String[]{"xsd"}, true));
        
        return xsdFiles;
    }
}
