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
package mil.dod.th.ose.jaxbprotoconverter.xsd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import mil.dod.th.core.ose.jaxbprotoconverter.ExampleAnotherJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbEnum;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbNotParsedSuperClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbRestrictedClass;
import mil.dod.th.core.ose.jaxbprotoconverter.ExampleJaxbSuperClass;
import mil.dod.th.ose.jaxbprotoconverter.JaxbProtoConvertException;

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
 * @author cweisenborn
 */
public class TestXsdParser
{
    private XsdParser m_SUT;
    @Mock private DocumentBuilder m_DocBuilder;
    @Mock private File m_XsdFilesDir;
    @Mock private File m_XsdFile1;
    @Mock private File m_XsdFile2;
    @Mock private File m_ParentXsdFile1;
    @Mock private File m_ParentXsdFile2;
    @Mock private Path m_FilePath;
    @Mock private Path m_BaseXsdPath;
    @Mock private Path m_RelativePath;
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        
        m_SUT = new XsdParser();
    }
    
    /**
     * Verify that the model returned by the XSD parser contains the appropriate objects.
     */
    @Test
    public void testParseXsdFiles() throws ClassNotFoundException, ParserConfigurationException, SAXException, 
        IOException, JaxbProtoConvertException
    {
        List<File> xsdFiles = new ArrayList<File>();
        xsdFiles.add(m_XsdFile1);
        xsdFiles.add(m_XsdFile2);
        
        final String baseSchemaLoc = "some location";
        final String absPath = "schemas" + File.separator + "core" + File.separator + "ose" + File.separator 
                + "jaxbprotoconverter";
        when(m_XsdFile1.getParentFile()).thenReturn(m_ParentXsdFile1);
        when(m_XsdFile2.getParentFile()).thenReturn(m_ParentXsdFile2);
        when(m_ParentXsdFile1.getAbsolutePath()).thenReturn(absPath);
        when(m_ParentXsdFile2.getAbsolutePath()).thenReturn(absPath);
        when(m_ParentXsdFile1.toPath()).thenReturn(m_FilePath);
        when(m_FilePath.resolve(baseSchemaLoc)).thenReturn(m_BaseXsdPath);
        when(m_BaseXsdPath.normalize()).thenReturn(m_BaseXsdPath);
        when(m_BaseXsdPath.toFile()).thenReturn(m_XsdFile2);
        when(m_XsdFile2.exists()).thenReturn(true);
        when(m_XsdFile2.isFile()).thenReturn(true);
        
        when(m_ParentXsdFile1.toPath()).thenReturn(m_FilePath);
        when(m_ParentXsdFile2.toPath()).thenReturn(m_FilePath);
        when(m_XsdFilesDir.toPath()).thenReturn(m_BaseXsdPath);
        when(m_BaseXsdPath.relativize(m_FilePath)).thenReturn(m_RelativePath);
        when(m_RelativePath.toString()).thenReturn("ose" + File.separator 
                + "jaxbprotoconverter");
        
        Document doc1 = createMockDoc1();
        Document doc2 = createMockDoc2();
        when(m_DocBuilder.parse(m_XsdFile1)).thenReturn(doc1);
        when(m_DocBuilder.parse(m_XsdFile2)).thenReturn(doc2);
        
        final XsdModel xsdModel = m_SUT.parseXsdFiles(m_DocBuilder, m_XsdFilesDir, xsdFiles);
        final Map<String, XsdNamespace> namespaceMap = xsdModel.getNamespacesMap();
        assertThat(namespaceMap.size(), is(2));
        
        XsdNamespace namespace = namespaceMap.get("namespace1");
        assertThat(namespace, is(notNullValue()));
        
        Map<Class<?>, XsdType> typesMap = namespace.getTypesMap();
        assertThat(typesMap.size(), is(3));
        
        XsdType type = typesMap.get(ExampleJaxbClass.class);
        assertThat(type, is(notNullValue()));
        assertThat(type.getXsdFile(), is(m_XsdFile1));
        assertThat(type.getJaxbType(), is((Object)ExampleJaxbClass.class));
        assertThat(type.isComplexRestriction(), is(false));
        XsdType baseType = type.getBaseType();
        assertThat(baseType, is(notNullValue()));
        assertThat(baseType.getJaxbType(), is((Object)ExampleJaxbSuperClass.class));
        
        Map<String, XsdField> fieldMap = type.getFieldsMap();
        assertThat(fieldMap.size(), is(3));
        XsdField field = fieldMap.get("refField");
        assertThat(field, is(notNullValue()));
        assertThat(field.getIndex(), is(1));
        assertThat(field.getXsdType(), is(type));
        
        field = fieldMap.get("stringField");
        assertThat(field, is(notNullValue()));
        assertThat(field.getIndex(), is(15));
        assertThat(field.getXsdType(), is(type));
        
        field = fieldMap.get("enumField");
        assertThat(field, is(notNullValue()));
        assertThat(field.getIndex(), is(2));
        assertThat(field.getXsdType(), is(type));
        
        type = typesMap.get(ExampleJaxbEnum.class);
        assertThat(type, is(notNullValue()));
        assertThat(type.getXsdFile(), is(m_XsdFile1));
        assertThat(type.getJaxbType(), is((Object)ExampleJaxbEnum.class));
        assertThat(type.getFieldsMap().size(), is(0));
        assertThat(type.isComplexRestriction(), is(false));
        
        type = typesMap.get(ExampleJaxbRestrictedClass.class);
        assertThat(type, is(notNullValue()));
        assertThat(type.getXsdFile(), is(m_XsdFile1));
        assertThat(type.getJaxbType(), is((Object)ExampleJaxbRestrictedClass.class));
        assertThat(type.getFieldsMap().size(), is(0));
        assertThat(type.isComplexRestriction(), is(true));
        
        type = typesMap.get(ExampleJaxbNotParsedSuperClass.class);
        assertThat(type, is(nullValue()));
        
        namespace = namespaceMap.get("namespace2");
        assertThat(namespace, is(notNullValue()));
        
        typesMap = namespace.getTypesMap();
        assertThat(typesMap.size(), is(2));
        
        type = typesMap.get(ExampleJaxbSuperClass.class);
        assertThat(type, is(notNullValue()));
        assertThat(type.getXsdFile(), is(m_XsdFile2));
        assertThat(type.getJaxbType(), is((Object)ExampleJaxbSuperClass.class));
        assertThat(type.isComplexRestriction(), is(false));
        
        fieldMap = type.getFieldsMap();
        assertThat(fieldMap.size(), is(1));
        field = fieldMap.get("superStrField");
        assertThat(field, is(notNullValue()));
        assertThat(field.getIndex(), is(1));
        assertThat(field.getXsdType(), is(type));
        
        type = typesMap.get(ExampleAnotherJaxbClass.class);
        assertThat(type, is(notNullValue()));
        assertThat(type.getXsdFile(), is(m_XsdFile2));
        assertThat(type.getJaxbType(), is((Object)ExampleAnotherJaxbClass.class));
        assertThat(type.isComplexRestriction(), is(false));
        
        fieldMap = type.getFieldsMap();
        assertThat(fieldMap.size(), is(1));
        field = fieldMap.get("someStrField");
        assertThat(field, is(notNullValue()));
        assertThat(field.getIndex(), is(1));
        assertThat(field.getXsdType(), is(type));
        
        type = typesMap.get(ExampleJaxbNotParsedSuperClass.class);
        assertThat(type, is(nullValue()));
    }
    
    private Document createMockDoc1()
    {
        final NodeList emptyNodeList = mock(NodeList.class);
        when(emptyNodeList.getLength()).thenReturn(0);
        
        final Node importNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(importNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)importNode).getNodeName()).thenReturn("xs:import");
        when(importNode.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node complexElementNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexElementNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)complexElementNode).getNodeName()).thenReturn("xs:element");
        when(((Element)complexElementNode).getAttribute("name")).thenReturn(ExampleJaxbClass.class.getSimpleName());
        
        final NodeList complexElementNodeList = mock(NodeList.class);
        when(complexElementNode.getChildNodes()).thenReturn(complexElementNodeList);
        when(complexElementNodeList.getLength()).thenReturn(1);
        
        final Node complexTypeNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexElementNodeList.item(0)).thenReturn(complexTypeNode);
        when(complexTypeNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)complexTypeNode).getAttribute("name")).thenReturn("");
        when(((Element)complexTypeNode).getNodeName()).thenReturn("xs:complexType");
        
        final NodeList complexTypeNodeList = mock(NodeList.class);
        when(complexTypeNode.getChildNodes()).thenReturn(complexTypeNodeList);
        when(complexTypeNodeList.getLength()).thenReturn(1);
        
        final Node extensionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexTypeNodeList.item(0)).thenReturn(extensionNode);
        when(extensionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)extensionNode).getNodeName()).thenReturn("xs:extension");
        when(((Element)extensionNode).getAttribute("base"))
            .thenReturn("test:" + ExampleJaxbSuperClass.class.getSimpleName());
        
        final NodeList extensionNodeList = mock(NodeList.class);
        when(extensionNode.getChildNodes()).thenReturn(extensionNodeList);
        when(extensionNodeList.getLength()).thenReturn(3);
        
        final Node elementNode1 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(0)).thenReturn(elementNode1);
        when(elementNode1.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)elementNode1).getNodeName()).thenReturn("xs:element");
        when(((Element)elementNode1).getAttribute("name")).thenReturn("refField");
        when(((Element)elementNode1).getAttribute("type"))
            .thenReturn("test:" + ExampleAnotherJaxbClass.class.getSimpleName());
        when(elementNode1.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node elementNode2 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(1)).thenReturn(elementNode2);
        when(elementNode2.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)elementNode2).getNodeName()).thenReturn("xs:element");
        when(((Element)elementNode2).getAttribute("name")).thenReturn("stringField");
        when(((Element)elementNode2).getAttribute("type")).thenReturn("xs:string");
        
        final NodeList elementNode2List = mock(NodeList.class);
        when(elementNode2.getChildNodes()).thenReturn(elementNode2List);
        when(elementNode2List.getLength()).thenReturn(1);
        
        final Node annotationNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(elementNode2List.item(0)).thenReturn(annotationNode);
        when(annotationNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)annotationNode).getNodeName()).thenReturn("xs:annotation");
        
        final NodeList annotationNodeList = mock(NodeList.class);
        when(annotationNode.getChildNodes()).thenReturn(annotationNodeList);
        when(annotationNodeList.getLength()).thenReturn(1);
        
        final Node appInfoNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(annotationNodeList.item(0)).thenReturn(appInfoNode);
        when(appInfoNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)appInfoNode).getNodeName()).thenReturn("xs:appinfo");
        when(((Element)appInfoNode).getTextContent()).thenReturn("index=15");
        when(appInfoNode.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node elementNode3 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(2)).thenReturn(elementNode3);
        when(elementNode3.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)elementNode3).getNodeName()).thenReturn("xs:element");
        when(((Element)elementNode3).getAttribute("name")).thenReturn("enumField");
        when(((Element)elementNode3).getAttribute("type")).thenReturn(ExampleJaxbEnum.class.getSimpleName());
        when(elementNode3.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node enumNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(enumNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)enumNode).getNodeName()).thenReturn("xs:simpleType");
        when(((Element)enumNode).getAttribute("name")).thenReturn(ExampleJaxbEnum.class.getSimpleName());
        
        final NodeList enumNodeList = mock(NodeList.class);
        when(enumNode.getChildNodes()).thenReturn(enumNodeList);
        when(enumNodeList.getLength()).thenReturn(2);
        
        final Node enumValueNode1 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(enumNodeList.item(0)).thenReturn(enumValueNode1);
        when(enumValueNode1.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)enumValueNode1).getNodeName()).thenReturn("xs:enumeration");
        when(((Element)enumValueNode1).getAttribute("value")).thenReturn("Value_1");
        when(enumValueNode1.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node enumValueNode2 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(enumNodeList.item(0)).thenReturn(enumValueNode2);
        when(enumValueNode2.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)enumValueNode2).getNodeName()).thenReturn("xs:enumeration");
        when(((Element)enumValueNode2).getAttribute("value")).thenReturn("Value_2");
        when(enumValueNode2.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node simpleTypeNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)simpleTypeNode).getNodeName()).thenReturn("xs:simpleType");
        when(((Element)simpleTypeNode).getAttribute("name")).thenReturn("SomeSimpleType");
        
        final NodeList simpleTypeNodeList = mock(NodeList.class);
        when(simpleTypeNode.getChildNodes()).thenReturn(simpleTypeNodeList);
        when(simpleTypeNodeList.getLength()).thenReturn(1);
        
        final Node simpleTypeRestrictionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleTypeNodeList.item(0)).thenReturn(simpleTypeRestrictionNode);
        when(simpleTypeRestrictionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)simpleTypeRestrictionNode).getNodeName()).thenReturn("xs:restriction");
        when(((Element)simpleTypeRestrictionNode).getAttribute("base")).thenReturn("shared:positiveInt");
        when(simpleTypeRestrictionNode.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node restrictedComplexTypeNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(restrictedComplexTypeNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)restrictedComplexTypeNode).getNodeName()).thenReturn("xs:complexType");
        when(((Element)restrictedComplexTypeNode).getAttribute("name"))
            .thenReturn(ExampleJaxbRestrictedClass.class.getSimpleName());
        
        final NodeList restrictedTypeNodeList = mock(NodeList.class);
        when(restrictedComplexTypeNode.getChildNodes()).thenReturn(restrictedTypeNodeList);
        when(restrictedTypeNodeList.getLength()).thenReturn(1);
        
        final Node simpleContentNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(restrictedTypeNodeList.item(0)).thenReturn(simpleContentNode);
        when(simpleContentNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)simpleContentNode).getNodeName()).thenReturn("xs:simpleContent");
        
        final NodeList simpleContentNodeList = mock(NodeList.class);
        when(simpleContentNode.getChildNodes()).thenReturn(simpleContentNodeList);
        when(simpleContentNodeList.getLength()).thenReturn(1);
        
        final Node complexTypeRestrictionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(simpleContentNodeList.item(0)).thenReturn(complexTypeRestrictionNode);
        when(complexTypeRestrictionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)complexTypeRestrictionNode).getNodeName()).thenReturn("xs:restriction");
        when(((Element)complexTypeRestrictionNode).getAttribute("base"))
            .thenReturn("test:" + ExampleJaxbSuperClass.class.getSimpleName());
        when(complexTypeRestrictionNode.getChildNodes()).thenReturn(emptyNodeList);
        
        final NodeList baseNodeList = mock(NodeList.class);
        when(baseNodeList.getLength()).thenReturn(5);
        when(baseNodeList.item(0)).thenReturn(importNode);
        when(baseNodeList.item(1)).thenReturn(complexElementNode);
        when(baseNodeList.item(2)).thenReturn(enumNode);
        when(baseNodeList.item(3)).thenReturn(simpleTypeNode);
        when(baseNodeList.item(4)).thenReturn(restrictedComplexTypeNode);
        
        final Document doc = mock(Document.class);
        final Element baseElement = mock(Element.class);
        when(doc.getDocumentElement()).thenReturn(baseElement);
        when(baseElement.getAttribute("targetNamespace")).thenReturn("namespace1");
        when(baseElement.getAttribute("xmlns:test")).thenReturn("namespace2");
        when(baseElement.getChildNodes()).thenReturn(baseNodeList);
        
        return doc;
    }
    
    private Document createMockDoc2()
    {
        final NodeList emptyNodeList = mock(NodeList.class);
        when(emptyNodeList.getLength()).thenReturn(0);
        
        final Node complexType1 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexType1.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)complexType1).getNodeName()).thenReturn("xs:complexType");
        when(((Element)complexType1).getAttribute("name")).thenReturn(ExampleJaxbSuperClass.class.getSimpleName());
        
        final NodeList complexType1NodeList = mock(NodeList.class);
        when(complexType1.getChildNodes()).thenReturn(complexType1NodeList);
        when(complexType1NodeList.getLength()).thenReturn(1);
        
        final Node elementNode1 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexType1NodeList.item(0)).thenReturn(elementNode1);
        when(elementNode1.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)elementNode1).getNodeName()).thenReturn("xs:element");
        when(((Element)elementNode1).getAttribute("name")).thenReturn("superStrField");
        when(((Element)elementNode1).getAttribute("type")).thenReturn("xs:string");
        when(elementNode1.getChildNodes()).thenReturn(emptyNodeList);
        
        final Node complexType2 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexType2.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)complexType2).getAttribute("name")).thenReturn(ExampleAnotherJaxbClass.class.getSimpleName());
        when(((Element)complexType2).getNodeName()).thenReturn("xs:complexType");
        
        final NodeList complexType2NodeList = mock(NodeList.class);
        when(complexType2.getChildNodes()).thenReturn(complexType2NodeList);
        when(complexType2NodeList.getLength()).thenReturn(1);
        
        final Node extensionNode = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(complexType2NodeList.item(0)).thenReturn(extensionNode);
        when(extensionNode.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)extensionNode).getNodeName()).thenReturn("xs:extension");
        when(((Element)extensionNode).getAttribute("base"))
            .thenReturn(ExampleJaxbNotParsedSuperClass.class.getSimpleName());
        
        final NodeList extensionNodeList = mock(NodeList.class);
        when(extensionNode.getChildNodes()).thenReturn(extensionNodeList);
        when(extensionNodeList.getLength()).thenReturn(1);
        
        final Node elementNode2 = mock(Node.class, withSettings().extraInterfaces(Element.class));
        when(extensionNodeList.item(0)).thenReturn(elementNode2);
        when(elementNode2.getNodeType()).thenReturn(Node.ELEMENT_NODE);
        when(((Element)elementNode2).getNodeName()).thenReturn("xs:element");
        when(((Element)elementNode2).getAttribute("name")).thenReturn("someStrField");
        when(((Element)elementNode2).getAttribute("type")).thenReturn("xs:string");
        when(elementNode2.getChildNodes()).thenReturn(emptyNodeList);
        
        final NodeList baseNodeList = mock(NodeList.class);
        when(baseNodeList.getLength()).thenReturn(2);
        when(baseNodeList.item(0)).thenReturn(complexType1);
        when(baseNodeList.item(1)).thenReturn(complexType2);
        
        final Document doc = mock(Document.class);
        final Element baseElement = mock(Element.class);
        when(doc.getDocumentElement()).thenReturn(baseElement);
        when(baseElement.getAttribute("targetNamespace")).thenReturn("namespace2");
        when(baseElement.getChildNodes()).thenReturn(baseNodeList);
        
        return doc;
    }
}
