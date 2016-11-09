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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import mil.dod.th.ose.lexicon.doc.parser.LexiconAttribute;
import mil.dod.th.ose.lexicon.doc.parser.LexiconBase;
import mil.dod.th.ose.lexicon.doc.parser.LexiconComplexType;
import mil.dod.th.ose.lexicon.doc.parser.LexiconElement;
import mil.dod.th.ose.lexicon.doc.parser.LexiconEnum;
import mil.dod.th.ose.lexicon.doc.parser.LexiconSimpleType;
import mil.dod.th.ose.utils.FileService;

/**
 * Test class for {@link MarkupGenerator}.
 * 
 * @author cweisenborn
 */
public class TestMarkupGenerator
{
    private MarkupGenerator m_SUT;
    private @Mock File m_OutputFile;
    private @Mock FileService m_FileService;
    private @Mock PrintStream m_PrintSream;
    
    @Before
    public void setup() throws FileNotFoundException
    {
        MockitoAnnotations.initMocks(this);
        
        when(m_FileService.createPrintStream(m_OutputFile)).thenReturn(m_PrintSream);
        
        m_SUT  = new MarkupGenerator(m_FileService);
    }
    
    /**
     * Verify that the appropriate data is printed to the print stream.
     */
    @Test
    public void testGenerateMarkup() throws IOException
    {
        final List<LexiconBase> types = createLexiconData();
        
        m_SUT.generateMarkup(m_OutputFile, types);
        
        verify(m_PrintSream).format("h2. +%s+", "ExtendsOnly");
        verify(m_PrintSream).format("*Extends*: _%s_",  "WorthlessClass");
        verify(m_PrintSream).println("Some description that no one will ever read.");
        verify(m_PrintSream, times(8)).println("----");
        
        verify(m_PrintSream).format("h2. +%s+", "ComplexType1");
        verify(m_PrintSream).println("BLAH");
        verify(m_PrintSream).println("h4. *Attributes*");
        verify(m_PrintSream).println("||Name||Type||Use||Description||");
        verify(m_PrintSream).format("|%s|%s|%s|%s|", "attr1", "string", "optional", " ");
        verify(m_PrintSream).format("|%s|%s|%s|%s|", "attr2", "SomeEnum", "required", "Woohoo! An enum111!!111!");
        verify(m_PrintSream).println("h4. *Elements*");
        verify(m_PrintSream).println("||Name||Type||Min Occurs||Max Occurs||Description||");
        verify(m_PrintSream).format("|%s|%s|%s|%s|%s|", "element1", "foo", "0", "1", 
                    "Insert a description here.");
        verify(m_PrintSream).format("|%s|%s|%s|%s|%s|", "element2", "ImportantStuffz", "1", "unbounded", " ");
        
        verify(m_PrintSream).format("h2. +%s+", "SomethingSimple");
        verify(m_PrintSream).format("*Type*: _%s_", "double");
        verify(m_PrintSream).println("Not so simple sounding description!");
        verify(m_PrintSream).format("*Min Inclusive*: %s", "0.0");
        verify(m_PrintSream).format("*Max Inclusive*: %s", "360.0");
        
        verify(m_PrintSream).format("h2. +%s+", "AwesomeEnum");
        verify(m_PrintSream).format("*Type*: _%s_", "string");
        verify(m_PrintSream).println("||Value||Description||");
        verify(m_PrintSream).format("|%s|%s|", "def", " ");
        verify(m_PrintSream).format("|%s|%s|", "abc", "uuggghhhh");
    }
    
    /**
     * Create fake lexicon data.
     */
    private List<LexiconBase> createLexiconData()
    {
        final List<LexiconBase> types = new ArrayList<>();
        
        final LexiconComplexType extendsOnly = new LexiconComplexType();
        extendsOnly.setName("ExtendsOnly");
        extendsOnly.setDescription("Some description that no one will ever read.");
        extendsOnly.setExtension("WorthlessClass");
        types.add(extendsOnly);
        
        final LexiconComplexType complexType1 = new LexiconComplexType();
        complexType1.setName("ComplexType1");
        complexType1.setDescription("BLAH");
        types.add(complexType1);
        
        final LexiconElement element1 = new LexiconElement();
        element1.setName("element1");
        element1.setMinOccurs("0");
        element1.setMaxOccurs("1");
        element1.setType("foo");
        element1.setDescription("Insert a description here.");
        complexType1.getElements().add(element1);
        
        final LexiconElement element2 = new LexiconElement();
        element2.setName("element2");
        element2.setMinOccurs("1");
        element2.setMaxOccurs("unbounded");
        element2.setType("ImportantStuffz");
        complexType1.getElements().add(element2);
        
        final LexiconAttribute attribute1 = new LexiconAttribute();
        attribute1.setName("attr1");
        attribute1.setType("string");
        attribute1.setUse("optional");
        complexType1.getAttributes().add(attribute1);

        final LexiconAttribute attribute2 = new LexiconAttribute();
        attribute2.setName("attr2");
        attribute2.setType("SomeEnum");
        attribute2.setUse("required");
        attribute2.setDescription("Woohoo! An enum111!!111!");
        complexType1.getAttributes().add(attribute2);
        
        final LexiconSimpleType simpleType1 = new LexiconSimpleType();
        simpleType1.setName("SomethingSimple");
        simpleType1.setDescription("Not so simple sounding description!");
        simpleType1.setType("double");
        simpleType1.setMinInclusive("0.0");
        simpleType1.setMaxInclusive("360.0");
        types.add(simpleType1);
        
        final LexiconSimpleType simpleTypeEnum = new LexiconSimpleType();
        simpleTypeEnum.setName("AwesomeEnum");
        simpleTypeEnum.setType("string");
        types.add(simpleTypeEnum);
        
        final LexiconEnum enum1 = new LexiconEnum();
        enum1.setName("abc");
        enum1.setDescription("uuggghhhh");
        simpleTypeEnum.getEnumerations().add(enum1);
        
        final LexiconEnum enum2 = new LexiconEnum();
        enum2.setName("def");
        simpleTypeEnum.getEnumerations().add(enum2);
        
        return types;
    }
}
