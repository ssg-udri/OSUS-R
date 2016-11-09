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
package mil.dod.th.ose.remote.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Test class for the XML parsing service. This service takes a URI and parses the XML document at that location.
 * @author callen
 *
 */
public class TestMetaTypeXMLParsingService
{
    private MetaTypeXMLParsingService m_SUT;
    private DocumentBuilderFactoryService m_DocBuilderFactory;
    private DocumentBuilder m_DocBuilder;
    

    @Before
    public void setUp() throws IOException, SAXException, ParserConfigurationException, URISyntaxException
    {
        m_DocBuilderFactory = mock(DocumentBuilderFactoryService.class);
        m_SUT = new MetaTypeXMLParsingService();
        m_SUT.setDocumentBuilderFactory(m_DocBuilderFactory);
        
        //Document builder
        m_DocBuilder = mock(DocumentBuilder.class);
        when(m_DocBuilderFactory.getDocumentBuilder()).thenReturn(m_DocBuilder);

        //activate
        m_SUT.activate();
    }

    /**
     * Test getting an attribute from a given URI.
     */
    @Test
    public void testGetAttribute() throws URISyntaxException, IOException, SAXException
    {
        //mock objects
        URI uri = new URI("file://file");
        Document doc = mock(Document.class);
        
        //mock behavior for the document builder
        when(m_DocBuilder.parse(uri.toString())).thenReturn(doc);

        //document lay out
        Element element = mock(Element.class);
        when(doc.getDocumentElement()).thenReturn(element);
        NodeList list = mock(NodeList.class);
        when(list.getLength()).thenReturn(2);
        when(doc.getElementsByTagName("OCD")).thenReturn(list);
        Node node = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(list.item(0)).thenReturn(node);
        when(node.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)node).getAttribute("id")).thenReturn("metatype.bundle.SWEETNESS");
        
        String attrib = m_SUT.getPidAttribute(uri);

        //verify
        assertThat(attrib, is("metatype.bundle.SWEETNESS"));
    }

    /**
     * Test getting an attribute from a given URI.
     */
    @Test
    public void testGetNullAttribute() throws URISyntaxException, IOException, SAXException
    {
        //mock objects
        URI uri = new URI("file://file");
        Document doc = mock(Document.class);
        
        //mock behavior for the document builder
        when(m_DocBuilder.parse(uri.toString())).thenReturn(doc);

        //document lay out
        Element element = mock(Element.class);
        when(doc.getDocumentElement()).thenReturn(element);
        NodeList list = mock(NodeList.class);
        when(list.getLength()).thenReturn(2);
        when(doc.getElementsByTagName("OCD")).thenReturn(list);
        Node node = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(list.item(0)).thenReturn(node);
        when(node.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        
        String attrib = m_SUT.getPidAttribute(uri);

        //verify
        assertThat(attrib, is(nullValue()));
    }
}
