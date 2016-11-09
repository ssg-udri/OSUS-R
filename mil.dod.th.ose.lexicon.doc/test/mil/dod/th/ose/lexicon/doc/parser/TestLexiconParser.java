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
package mil.dod.th.ose.lexicon.doc.parser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Test class for {@link LexiconParser}.
 * 
 * @author cweisenborn
 */
public class TestLexiconParser
{
    private LexiconParser m_SUT;
    private @Mock DocumentBuilder m_DocBuilder;
    private @Mock File m_XsdFile1;
    private @Mock File m_XsdFile2;
    
    @Before
    public void setup() throws SAXException, IOException
    {
        MockitoAnnotations.initMocks(this);
        
        final Document doc1 = createDoc1();
        final Document doc2 = createDoc2();
        when(m_DocBuilder.parse(m_XsdFile1)).thenReturn(doc1);
        when(m_DocBuilder.parse(m_XsdFile2)).thenReturn(doc2);
        
        m_SUT = new LexiconParser();
    }
    
    /**
     * Verify that mocked files are parsed correctly and have the correct structure.
     */
    @Test
    public void testParseXsdFiles() throws ParserConfigurationException, SAXException, IOException
    {
        final List<File> xsdFiles = new ArrayList<>();
        xsdFiles.add(m_XsdFile1);
        xsdFiles.add(m_XsdFile2);
        
        List<LexiconBase> types = m_SUT.parserXsdFiles(xsdFiles, m_DocBuilder);
        Collections.sort(types, new LexiconComparator());
        
        LexiconComplexType complexType = (LexiconComplexType)types.get(0);
        assertThat(complexType.getName(), is("CompType"));
        assertThat(complexType.getExtension(), is("Base"));
        assertThat(complexType.getDescription(), is("Some description of the complex type!"));
        
        LexiconAttribute attribute = complexType.getAttributes().get(0);
        assertThat(attribute.getName(), is("attrb1"));
        assertThat(attribute.getType(), is("boolean"));
        assertThat(attribute.getUse(), is("optional"));
        assertThat(attribute.getDescription(), is("Some description of the attribute!"));
        
        LexiconElement element = complexType.getElements().get(0);
        assertThat(element.getName(), is("element1"));
        assertThat(element.getType(), is("SomeType"));
        assertThat(element.getMinOccurs(), is("0"));
        assertThat(element.getMaxOccurs(), is("1"));
        assertThat(element.getDescription(), is("Some description of the element!"));
        
        LexiconSimpleType simpleType = (LexiconSimpleType)types.get(1);
        assertThat(simpleType.getName(), is("SimpType"));
        assertThat(simpleType.getType(), is("double"));
        assertThat(simpleType.getDescription(), is("Some description of the simple type!"));
        assertThat(simpleType.getMinInclusive(), is("0.0"));
        assertThat(simpleType.getMaxInclusive(), is("360.0"));
        
        LexiconSimpleType simpleTypeEnum = (LexiconSimpleType)types.get(2);
        assertThat(simpleTypeEnum.getName(), is("SimpTypeEnum"));
        assertThat(simpleTypeEnum.getType(), is("string"));
        assertThat(simpleTypeEnum.getDescription(), is("Some description of the enum simple type!"));
        
        LexiconEnum enum1 = simpleTypeEnum.getEnumerations().get(0);
        assertThat(enum1.getName(), is("ONE"));
        assertThat(enum1.getDescription(), is("Some description of the enum ONE!"));
        
        LexiconEnum enum2 = simpleTypeEnum.getEnumerations().get(1);
        assertThat(enum2.getName(), is("TWO"));
        assertThat(enum2.getDescription(), is("Some description of the enum TWO!"));
    }
    
    private Document createDoc1()
    {
        final NodeList emptyNodeList = mock(NodeList.class);
        when(emptyNodeList.getLength()).thenReturn(0);
        
        final Node complexType = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexType.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)complexType).getNodeName()).thenReturn("xs:complexType");
        when(((Element)complexType).getAttribute("name")).thenReturn("CompType");
        
        final NodeList complexTypeNodeList = mock(NodeList.class);
        when(complexTypeNodeList.getLength()).thenReturn(1);
        when(complexType.getChildNodes()).thenReturn(complexTypeNodeList);
        
        final Node extensionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexTypeNodeList.item(0)).thenReturn(extensionNode);
        when(extensionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)extensionNode).getNodeName()).thenReturn("xs:extension");
        when(((Element)extensionNode).getAttribute("base")).thenReturn("something:Base");
        
        final NodeList extensionNodeList = mock(NodeList.class);
        when(extensionNodeList.getLength()).thenReturn(3);
        when(extensionNode.getChildNodes()).thenReturn(extensionNodeList);
        
        final Node complexDocNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(0)).thenReturn(complexDocNode);
        when(complexDocNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(complexDocNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)complexDocNode).getNodeName()).thenReturn("xs:documentation");
        when(((Element)complexDocNode).getTextContent()).thenReturn("Some description of the complex type!");
        
        final Node elementNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(1)).thenReturn(elementNode);
        when(elementNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)elementNode).getNodeName()).thenReturn("xs:element");
        when(((Element)elementNode).getAttribute("name")).thenReturn("element1");
        when(((Element)elementNode).getAttribute("type")).thenReturn("shared:SomeType");
        when(((Element)elementNode).getAttribute("minOccurs")).thenReturn("0");
        when(((Element)elementNode).getAttribute("maxOccurs")).thenReturn("1");
        
        final NodeList elementNodeList = mock(NodeList.class);
        when(elementNodeList.getLength()).thenReturn(1);
        when(elementNode.getChildNodes()).thenReturn(elementNodeList);
        
        final Node elementDocNode = mock(Node.class, withSettings().extraInterfaces(Element.class));  
        when(elementNodeList.item(0)).thenReturn(elementDocNode);
        when(elementDocNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(elementDocNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)elementDocNode).getNodeName()).thenReturn("xs:documentation");
        when(((Element)elementDocNode).getTextContent()).thenReturn("Some description of the element!");
        
        final Node attributeNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(2)).thenReturn(attributeNode);
        when(attributeNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)attributeNode).getNodeName()).thenReturn("xs:attribute");
        when(((Element)attributeNode).getAttribute("name")).thenReturn("attrb1");
        when(((Element)attributeNode).getAttribute("type")).thenReturn("xs:boolean");
        when(((Element)attributeNode).getAttribute("use")).thenReturn("optional");
        
        final NodeList attributeNodeList = mock(NodeList.class);
        when(attributeNodeList.getLength()).thenReturn(1);
        when(attributeNode.getChildNodes()).thenReturn(attributeNodeList);
        
        final Node attributeDocNode = mock(Node.class, withSettings().extraInterfaces(Element.class));  
        when(attributeNodeList.item(0)).thenReturn(attributeDocNode);
        when(attributeDocNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(attributeDocNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)attributeDocNode).getNodeName()).thenReturn("xs:documentation");
        when(((Element)attributeDocNode).getTextContent()).thenReturn("Some description of the attribute!");
        
        final Node simpleTypeNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)simpleTypeNode).getNodeName()).thenReturn("xs:simpleType");
        when(((Element)simpleTypeNode).getAttribute("name")).thenReturn("SimpType");
        
        final NodeList simpleTypeNodeList = mock(NodeList.class);
        when(simpleTypeNodeList.getLength()).thenReturn(2);
        when(simpleTypeNode.getChildNodes()).thenReturn(simpleTypeNodeList);
        
        final Node simpleTypeDocNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNodeList.item(0)).thenReturn(simpleTypeDocNode);
        when(simpleTypeDocNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(simpleTypeDocNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)simpleTypeDocNode).getNodeName()).thenReturn("xs:documentation");
        when(((Element)simpleTypeDocNode).getTextContent()).thenReturn("Some description of the simple type!");
        
        final Node restrictionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNodeList.item(1)).thenReturn(restrictionNode);
        when(restrictionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)restrictionNode).getNodeName()).thenReturn("xs:restriction");
        when(((Element)restrictionNode).getAttribute("base")).thenReturn("xs:double");
        
        final NodeList restrictionNodeList = mock(NodeList.class);
        when(restrictionNodeList.getLength()).thenReturn(2);
        when(restrictionNode.getChildNodes()).thenReturn(restrictionNodeList);
        
        final Node minInclusiveNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(restrictionNodeList.item(0)).thenReturn(minInclusiveNode);
        when(minInclusiveNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(minInclusiveNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)minInclusiveNode).getNodeName()).thenReturn("xs:minInclusive");
        when(((Element)minInclusiveNode).getAttribute("value")).thenReturn("0.0");
        
        final Node maxInclusiveNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(restrictionNodeList.item(1)).thenReturn(maxInclusiveNode);
        when(maxInclusiveNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(maxInclusiveNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)maxInclusiveNode).getNodeName()).thenReturn("xs:maxInclusive");
        when(((Element)maxInclusiveNode).getAttribute("value")).thenReturn("360.0");
        
        final NodeList baseNodeList = mock(NodeList.class);
        when(baseNodeList.getLength()).thenReturn(2);
        when(baseNodeList.item(0)).thenReturn(complexType);
        when(baseNodeList.item(1)).thenReturn(simpleTypeNode);
        
        final Document doc = mock(Document.class);
        final Element baseElement = mock(Element.class);
        when(doc.getDocumentElement()).thenReturn(baseElement);
        when(baseElement.getChildNodes()).thenReturn(baseNodeList);
        
        return doc;
    }
    
    private Document createDoc2()
    {
        final NodeList emptyNodeList = mock(NodeList.class);
        when(emptyNodeList.getLength()).thenReturn(0);
        
        final Node simpleTypeNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)simpleTypeNode).getNodeName()).thenReturn("xs:simpleType");
        when(((Element)simpleTypeNode).getAttribute("name")).thenReturn("SimpTypeEnum");
        
        final NodeList simpleTypeNodeList = mock(NodeList.class);
        when(simpleTypeNodeList.getLength()).thenReturn(2);
        when(simpleTypeNode.getChildNodes()).thenReturn(simpleTypeNodeList);
        
        final Node simpleTypeDocNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNodeList.item(0)).thenReturn(simpleTypeDocNode);
        when(simpleTypeDocNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(simpleTypeDocNode.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)simpleTypeDocNode).getNodeName()).thenReturn("xs:documentation");
        when(((Element)simpleTypeDocNode).getTextContent()).thenReturn("Some description of the enum simple type!");
        
        final Node restrictionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNodeList.item(1)).thenReturn(restrictionNode);
        when(restrictionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)restrictionNode).getNodeName()).thenReturn("xs:restriction");
        when(((Element)restrictionNode).getAttribute("base")).thenReturn("xs:string");
        
        final NodeList restrictionNodeList = mock(NodeList.class);
        when(restrictionNodeList.getLength()).thenReturn(2);
        when(restrictionNode.getChildNodes()).thenReturn(restrictionNodeList);
        
        final Node enumNode1 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(restrictionNodeList.item(0)).thenReturn(enumNode1);
        when(enumNode1.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)enumNode1).getNodeName()).thenReturn("xs:enumeration");
        when(((Element)enumNode1).getAttribute("value")).thenReturn("ONE");
        
        final NodeList enum1NodeList = mock(NodeList.class);
        when(enum1NodeList.getLength()).thenReturn(1);
        when(enumNode1.getChildNodes()).thenReturn(enum1NodeList);
        
        final Node enumNode1Doc = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(enum1NodeList.item(0)).thenReturn(enumNode1Doc);
        when(enumNode1Doc.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(enumNode1Doc.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)enumNode1Doc).getNodeName()).thenReturn("xs:documentation");
        when(((Element)enumNode1Doc).getTextContent()).thenReturn("Some description of the enum ONE!");
        
        final Node enumNode2 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(restrictionNodeList.item(1)).thenReturn(enumNode2);
        when(enumNode2.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)enumNode2).getNodeName()).thenReturn("xs:enumeration");
        when(((Element)enumNode2).getAttribute("value")).thenReturn("TWO");
        
        final NodeList enum2NodeList = mock(NodeList.class);
        when(enum2NodeList.getLength()).thenReturn(1);
        when(enumNode2.getChildNodes()).thenReturn(enum2NodeList);
        
        final Node enumNode2Doc = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(enum2NodeList.item(0)).thenReturn(enumNode2Doc);
        when(enumNode2Doc.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(enumNode2Doc.getChildNodes()).thenReturn(emptyNodeList);
        when(((Element)enumNode2Doc).getNodeName()).thenReturn("xs:documentation");
        when(((Element)enumNode2Doc).getTextContent()).thenReturn("Some description of the enum TWO!");
        
        final NodeList baseNodeList = mock(NodeList.class);
        when(baseNodeList.getLength()).thenReturn(1);
        when(baseNodeList.item(0)).thenReturn(simpleTypeNode);
        
        final Document doc = mock(Document.class);
        final Element baseElement = mock(Element.class);
        when(doc.getDocumentElement()).thenReturn(baseElement);
        when(baseElement.getChildNodes()).thenReturn(baseNodeList);
        
        return doc;
    }
}
