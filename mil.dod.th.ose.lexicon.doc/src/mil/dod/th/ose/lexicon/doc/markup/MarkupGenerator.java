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
package mil.dod.th.ose.lexicon.doc.markup;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import com.google.inject.Inject;

import mil.dod.th.ose.lexicon.doc.parser.LexiconAttribute;
import mil.dod.th.ose.lexicon.doc.parser.LexiconBase;
import mil.dod.th.ose.lexicon.doc.parser.LexiconComplexType;
import mil.dod.th.ose.lexicon.doc.parser.LexiconElement;
import mil.dod.th.ose.lexicon.doc.parser.LexiconEnum;
import mil.dod.th.ose.lexicon.doc.parser.LexiconSimpleType;
import mil.dod.th.ose.utils.FileService;

/**
 * Class that generates confluence mark-up for lexicon types.
 * 
 * @author cweisenborn
 */
public class MarkupGenerator
{
    private static final String SPACE_CHAR = " ";
    
    /**
     * Service used to handle file operations.
     */
    private FileService m_FileService;
    
    /**
     * Base constructor.
     * 
     * @param fileService
     *      Service used to handle file operations.
     */      
    @Inject
    public MarkupGenerator(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    /**
     * Generates Confluence mark-up to a file for the specified lexicon types.
     * 
     * @param outputfile
     *      File the generated mark-up should be printed to.
     * @param types
     *      The lexicon types mark-up should be generated for.
     * @throws IOException
     *      Thrown if there is an issue writing the mark-up to the specified file.
     */
    public void generateMarkup(final File outputfile, final List<LexiconBase> types) throws IOException
    {
        
        try (final PrintStream stream = m_FileService.createPrintStream(outputfile))
        {
            for (LexiconBase type: types)
            {
                stream.format("h2. +%s+", type.getName());
                stream.println();
                
                if (type instanceof LexiconComplexType)
                {
                    handleComplexType(stream, (LexiconComplexType)type);
                }
                else
                {
                    handleSimpleType(stream, (LexiconSimpleType)type);
                }
                
                stream.println("----"); //NOCHECKSTYLE Repeating string is easier to read.
                stream.println("----");
            }
        }
    }
    
    /**
     * Method that handles generating mark-up for a complex type.
     * 
     * @param stream
     *      Print stream the mark-up should be written to.
     * @param complexType
     *      The complex type mark-up should be generated for.
     * @throws IOException
     *      Thrown if an exception occurs writing the mark-up to the buffered writer.
     */
    private void handleComplexType(final PrintStream stream, final LexiconComplexType complexType) throws IOException
    {
        if (complexType.getExtension() != null)
        {
            stream.format("*Extends*: _%s_", complexType.getExtension());
            stream.println();
        }
        if (complexType.getDescription() != null)
        {
            stream.println(complexType.getDescription());
        }
        
        final List<LexiconAttribute> attributes = complexType.getAttributes();
        handleAttributes(stream, attributes);
        
        final List<LexiconElement> elements = complexType.getElements();
        handleElements(stream, elements);
    }
    
    /**
     * Method that handles generating mark-up for a simple type.
     * 
     * @param stream
     *      Print stream the mark-up should be written to.
     * @param simpleType
     *      The simple type mark-up should be generated for.
     * @throws IOException
     *      Thrown if an exception occurs writing the mark-up to the buffered writer.
     */
    private void handleSimpleType(final PrintStream stream, final LexiconSimpleType simpleType) throws IOException
    {
        if (simpleType.getType() != null)
        {
            stream.format("*Type*: _%s_", simpleType.getType());
            stream.println();
        }
        if (simpleType.getMinInclusive() != null)
        {
            stream.format("*Min Inclusive*: %s", simpleType.getMinInclusive());
            stream.println();
        }
        if (simpleType.getMaxInclusive() != null)
        {
            stream.format("*Max Inclusive*: %s", simpleType.getMaxInclusive());
            stream.println();
        }
        if (simpleType.getDescription() != null)
        {
            stream.println(simpleType.getDescription());
        }
        
        handleEnumerations(stream, simpleType.getEnumerations());
    }
    
    /**
     * Method that generates mark-up for a list of attributes.
     * 
     * @param stream
     *      Print stream the mark-up should be written to.
     * @param attributes
     *      List of attributes that mark-up should be generated for.
     * @throws IOException
     *      Thrown if an exception occurs writing the mark-up to the buffered writer.
     */
    private void handleAttributes(final PrintStream stream, final List<LexiconAttribute> attributes) throws IOException
    {
        if (attributes.size() > 0)
        {
            stream.println("h4. *Attributes*");
            stream.println("||Name||Type||Use||Description||");
            for (LexiconAttribute attribute: attributes)
            {
                final String use = attribute.getUse().isEmpty()  ? SPACE_CHAR : attribute.getUse();
                final String description = attribute.getDescription() == null ? SPACE_CHAR : attribute.getDescription();
                stream.format("|%s|%s|%s|%s|", attribute.getName(), attribute.getType(), 
                        use, description);
                stream.println();
            }
        }
    }
    
    /**
     * Method that generates mark-up for a list of elements.
     * 
     * @param stream
     *      Print stream the mark-up should be written to.
     * @param elements
     *      List of elements that mark-up should be generated for.
     * @throws IOException
     *      Thrown if an exception occurs writing the mark-up to the buffered writer.
     */
    private void handleElements(final PrintStream stream, final List<LexiconElement> elements) throws IOException
    {
        if (elements.size() > 0)
        {
            stream.println("h4. *Elements*");
            stream.println("||Name||Type||Min Occurs||Max Occurs||Description||");
            for (LexiconElement element: elements)
            {
                final String description = element.getDescription() == null ? SPACE_CHAR : element.getDescription();
                stream.format("|%s|%s|%s|%s|%s|", element.getName(), element.getType(), 
                        element.getMinOccurs(), element.getMaxOccurs(), description);
                stream.println();
            }
        }
    }
    
    /**
     * Method that generates mark-up for a list of enumeration values.
     * 
     * @param stream
     *      Print stream the mark-up should be written to.
     * @param enums
     *      List of enumeration values that mark-up should be generated for.
     * @throws IOException
     *      Thrown if an exception occurs writing the mark-up to the buffered writer.
     */
    private void handleEnumerations(final PrintStream stream, final List<LexiconEnum> enums) throws IOException
    {
        if (enums.size() > 0)
        {
            stream.println("h4. *Enum Values*");
            stream.println("||Value||Description||");
            for (LexiconEnum lexEnum: enums)
            {
                final String description = lexEnum.getDescription() == null ? SPACE_CHAR : lexEnum.getDescription();
                stream.format("|%s|%s|", lexEnum.getName(), description);
                stream.println();
            }
        }
    }
}
