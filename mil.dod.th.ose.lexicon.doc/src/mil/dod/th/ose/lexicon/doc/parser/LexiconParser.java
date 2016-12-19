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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import com.google.common.base.Strings;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class that parsers XSD lexicon files for documentation information.
 * 
 * @author cweisenborn
 */
public class LexiconParser
{
    private static final String NAME_ATTR = "name";
    private static final String VALUE_ATTR = "value";
    private DocumentBuilder m_DocBuilder;
    
    /**
     * List of simple and complex types found within lexicon files.
     */
    private List<LexiconBase> m_Types;
    
    /**
     * Parses the list of XSD files for lexicon information.
     * 
     * @param xsdFiles
     *      List of XSD lexicon files to be parsed.
     * @param docBuilder
     *      Document builder that should be used to parse XSD files.
     * @return
     *      List of simple and complex types found within XSD lexicon files.
     * @throws ParserConfigurationException
     *      Thrown if an error occurs configuring the parser.
     * @throws SAXException
     *      Thrown if an error occurs parsing an XSD file.
     * @throws IOException
     *      Thrown if an error occurs reading from an XSD file.
     */
    public List<LexiconBase> parserXsdFiles(final List<File> xsdFiles, final DocumentBuilder docBuilder) 
            throws ParserConfigurationException, SAXException, IOException
    {
        m_DocBuilder = docBuilder;
        m_Types = new ArrayList<>();
        
        for (File xsdFile: xsdFiles)
        {
            processXsdFile(xsdFile);
        }
        
        return m_Types;
    }
    
    /**
     * Processes a single XSD file for lexicon information.
     * 
     * @param xsdFile
     *      XSD file to be parsed.
     * @throws ParserConfigurationException
     *      Thrown if an error occurs configuring the parser.
     * @throws SAXException
     *      Thrown if an error occurs parsing the XSD file.
     * @throws IOException
     *      Thrown if an error occurs reading from the XSD file
     */
    private void processXsdFile(final File xsdFile) 
            throws ParserConfigurationException, SAXException, IOException
    {
        Document document = m_DocBuilder.parse(xsdFile);
        Element baseElement = document.getDocumentElement();
        baseElement.normalize();
        
        document = m_DocBuilder.parse(xsdFile);
        baseElement = document.getDocumentElement();
        baseElement.normalize();
        
        final NodeList childList = baseElement.getChildNodes();
        traverseNodes(childList, baseElement, null);
    }
    
    /**
     * Traverse the list of specified nodes.
     * 
     * @param nodeList
     *      List of nodes to parse.
     * @param parentElement
     *      Parent element of the list of nodes.
     * @param lexiconField
     *      Lexicon object the nodes belong to.
     */
    private void traverseNodes(final NodeList nodeList, final Element parentElement, final LexiconBase lexiconField)
    {
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            final Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element element = (Element)node;
                final String nodeName = element.getNodeName();
                final String elementName = 
                        element.getAttribute(NAME_ATTR);
                
                if (nodeName.equals("xs:complexType"))
                {
                    handleComplexType(element, elementName, parentElement);
                }
                else if (nodeName.equals("xs:simpleType") && !Strings.isNullOrEmpty(elementName))
                {
                    handleSimpleType(element, elementName);
                }
                else if (nodeName.equals("xs:element") && lexiconField instanceof LexiconComplexType)
                {
                    handleElement(element, elementName, (LexiconComplexType)lexiconField);
                }
                else if (nodeName.equals("xs:attribute") && lexiconField instanceof LexiconComplexType)
                {
                    handleAttribute(element, elementName, (LexiconComplexType)lexiconField);
                }
                else if (nodeName.equals("xs:enumeration") && lexiconField instanceof LexiconSimpleType)
                {
                    handleEnumeration(element, (LexiconSimpleType)lexiconField);
                }
                else
                {
                    handleOtherElement(element, nodeName, lexiconField);
                }
            }
        }
    }
    
    /**
     * Method that determines how to handle an XSD element.
     * 
     * @param element
     *      Element to be handled.
     * @param nodeName
     *      Node name of the element.
     * @param lexiconField
     *      Lexicon field the element belongs to.
     */
    private void handleOtherElement(final Element element, final String nodeName, final LexiconBase lexiconField)
    {
        if ((nodeName.equals("xs:extension") || nodeName.equals("xs:restriction")) //NOCHECKSTYLE repeated string
                && lexiconField instanceof LexiconComplexType)                     //makes code more readable.
        {
            final String base = getBaseAttribute(element);
            ((LexiconComplexType)lexiconField).setExtension(base);
        }
        else if (nodeName.equals("xs:restriction") && lexiconField instanceof LexiconSimpleType)
        {
            final String base = getBaseAttribute(element);
            ((LexiconSimpleType)lexiconField).setType(base);
        }
        else if (nodeName.equals("xs:minInclusive") && lexiconField instanceof LexiconSimpleType)
        {
            final String value = element.getAttribute(VALUE_ATTR);
            ((LexiconSimpleType)lexiconField).setMinInclusive(value);
        }
        else if (nodeName.equals("xs:maxInclusive") && lexiconField instanceof LexiconSimpleType)
        {
            final String value = element.getAttribute(VALUE_ATTR);
            ((LexiconSimpleType)lexiconField).setMaxInclusive(value);
        }
        else if (nodeName.equals("xs:documentation") && lexiconField != null)
        {
            lexiconField.setDescription(element.getTextContent().trim().replaceAll(
                    System.getProperty("line.separator"), "").replaceAll("\\s+", " ")); //NOCHECKSTYLE repeated empty string
        }                                                                               //makes code more readable.
        traverseNodes(element.getChildNodes(), element, lexiconField);
    }
    
    /**
     * Method that handles parsing a complex type.
     * 
     * @param element
     *      Complex type XSD element.
     * @param elementName
     *      Name of the element.
     * @param parentElement
     *      Parent element of the complex type.
     */
    private void handleComplexType(final Element element, final String elementName, final Element parentElement) 
    {
        final String typeName = elementName.isEmpty() ? parentElement.getAttribute(NAME_ATTR) : elementName;
        
        final LexiconComplexType newLexiconClass = new LexiconComplexType();
        newLexiconClass.setName(typeName);
        m_Types.add(newLexiconClass);
        traverseNodes(element.getChildNodes(), element, newLexiconClass);
    }
    
    /**
     * Method that handles parsing a simple type.
     * 
     * @param element
     *      Simple type XSD element.
     * @param elementName
     *      Name of the simple type XSD element.
     */
    private void handleSimpleType(final Element element, final String elementName)
    {
        final LexiconSimpleType lexSimpleType = new LexiconSimpleType();
        lexSimpleType.setName(elementName);
        
        m_Types.add(lexSimpleType);
        traverseNodes(element.getChildNodes(), element, lexSimpleType);
    }
    
    /**
     * Method that handles parsing an element.
     * 
     * @param element
     *      XSD element.
     * @param elementName
     *      Name of the XSD element.
     * @param complexType
     *      Complex type the element belongs to.
     */
    private void  handleElement(final Element element, final String elementName, final LexiconComplexType complexType)
    {
        final String type = getTypeAttribute(element);
        String minOccurs = element.getAttribute("minOccurs");
        if (minOccurs.isEmpty())
        {
            minOccurs = " "; 
        }
        String maxOccurs = element.getAttribute("maxOccurs");
        if (maxOccurs.isEmpty())
        {
            maxOccurs = " ";
        }
        final LexiconElement lexElement = new LexiconElement();
        
        lexElement.setName(elementName);
        lexElement.setType(type);
        lexElement.setMinOccurs(minOccurs);
        lexElement.setMaxOccurs(maxOccurs);
        
        complexType.getElements().add(lexElement);
        traverseNodes(element.getChildNodes(), element, lexElement);
    }
    
    /**
     * Method that handles parsing an attribute.
     * 
     * @param element
     *      XSD attribute.
     * @param elementName
     *      Name of the attribute.
     * @param complexType
     *      Complex type the attribute belongs to.
     */
    private void handleAttribute(final Element element, final String elementName, final LexiconComplexType complexType)
    {
        String type = getTypeAttribute(element);
        if (type.isEmpty())
        {
            type = " ";
        }
        final String use = element.getAttribute("use");
        final LexiconAttribute lexAttribute = new LexiconAttribute();
        
        lexAttribute.setName(elementName);
        lexAttribute.setType(type);
        lexAttribute.setUse(use);
        
        complexType.getAttributes().add(lexAttribute);
        traverseNodes(element.getChildNodes(), element, lexAttribute);
    }
    
    /**
     * Method that handles parsing an enumeration.
     * 
     * @param element
     *      Enumeration XSD element.
     * @param simpleType
     *      Simple type the enumeration belongs to.
     */
    private void handleEnumeration(final Element element, final LexiconSimpleType simpleType)
    {
        final String enumName = element.getAttribute(VALUE_ATTR);
        final LexiconEnum lexEnum = new LexiconEnum();
        lexEnum.setName(enumName);
        
        simpleType.getEnumerations().add(lexEnum);
        traverseNodes(element.getChildNodes(), element, lexEnum);
    }
    
    /**
     * Retrieves the type attribute from the specified element and removes the namespace identifier if there is one.
     * 
     * @param element
     *      Element to retrieve the type attribute from.
     * @return
     *      String that represents the type.
     */
    private String getTypeAttribute(final Element element)
    {
        final String type = element.getAttribute("type");
        return removeNamespace(type);
    }
    
    /**
     * Retrieves the base attribute from the specified element and removes the namespace identifier if there is one.
     * 
     * @param element
     *      Element to retrieve the base attribute from.
     * @return
     *      String that represents the base.
     */
    private String getBaseAttribute(final Element element)
    {
        final String base = element.getAttribute("base");
        return removeNamespace(base);
    }
    
    /**
     * Removes the namespace identifier from the specified string.
     * 
     * @param fqType
     *      Fully qualified name of the type.
     * @return
     *      Type string without namespace.
     */
    private String removeNamespace(final String fqType)
    {
        final String[] namespaceSplit = fqType.split(":");
        final String type = namespaceSplit.length > 1 ? namespaceSplit[1] : namespaceSplit[0];
        return type;
    }
}
