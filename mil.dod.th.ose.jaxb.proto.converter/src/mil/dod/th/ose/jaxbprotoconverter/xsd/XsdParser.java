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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import mil.dod.th.ose.jaxbprotoconverter.JaxbProtoConvertException;
import mil.dod.th.ose.jaxbprotoconverter.PathUtils;

import org.apache.commons.lang.WordUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class responsible for handling the parsing of XSD files.
 * 
 * @author cweisenborn
 */
public class XsdParser
{   
    private static final String BASE_ATTRIBUTE = "base";
    private static final String XS_PREFIX = "xs:";
    private static final String XS_SIMPLE_TPYE = XS_PREFIX + "simpleType";
    
    /**
     * Map that contains all XSD types that have been discovered but not parsed. The key is the simple class name
     * of the type and the value is XSD type model that represents the discovered type.
     */
    private final Map<String, XsdType> m_DiscoveredTypes = new HashMap<String, XsdType>();
    
    private DocumentBuilder m_DocBuilder;
    
    /**
     * Map of import statements for the XSD file currently being parsed. The key is a string that represents the 
     * namespace being imported and the value is a string that represents the schema location for the namespace.
     */
    private XsdModel m_Model;
    private File m_XsdFilesDir;
    
    /**
     * Method that parses a list of XSD files and returns a model that represents the types and fields within the XSD
     * files.
     * 
     * @param docBuilder
     *      The {@link DocumentBuilder} that will be used to parse the XSD files.
     * @param xsdFilesDir
     *      File that represents the directory where all XSD files are located.
     * @param xsdFiles
     *      List of XSD files to be parsed.
     * @return
     *      Model that represents the parsed XSD files.
     * @throws ParserConfigurationException
     *      Thrown if there is a configuration exception occurs with the parser.
     * @throws SAXException
     *      Thrown if an exception occurs parsing an XSD file.
     * @throws IOException
     *      Thrown if an exception occurs reading an XSD file.
     * @throws ClassNotFoundException
     *      Thrown if an associated JAXB class cannot be found for a type in an XSD file.
     * @throws JaxbProtoConvertException
     *      Thrown if a duplicate overridden ID is encountered.
     */
    public XsdModel parseXsdFiles(final DocumentBuilder docBuilder, final File xsdFilesDir, final List<File> xsdFiles) 
            throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, 
            JaxbProtoConvertException
    {
        m_DocBuilder = docBuilder;
        m_XsdFilesDir = xsdFilesDir;
        m_Model = new XsdModel();
        for (File xsdFile: xsdFiles)
        {
            parseFile(xsdFile);
        }
        return m_Model;
    }
    
    /**
     * Method that parses a single XSD file and adds all types found to the specified XSD model.
     * 
     * @param xsdFile
     *      XSD file to be parsed.
     * @throws ParserConfigurationException
     *      Thrown if there is a configuration exception occurs with the parser.
     * @throws SAXException
     *      Thrown if an exception occurs parsing the XSD file.
     * @throws IOException
     *      Thrown if an exception occurs reading the XSD file.
     * @throws ClassNotFoundException
     *      Thrown if an associated JAXB class cannot be found for a type in the XSD file.
     * @throws JaxbProtoConvertException
     *      Thrown if a duplicate overridden ID is encountered.
     */
    private void parseFile(final File xsdFile) throws ParserConfigurationException, 
            SAXException, IOException, ClassNotFoundException, JaxbProtoConvertException
    {
        final Document document = m_DocBuilder.parse(xsdFile);
        final Element baseElement = document.getDocumentElement();
        baseElement.normalize();
        
        final String namespace = baseElement.getAttribute("targetNamespace");
        XsdNamespace xsdNamespace = m_Model.getNamespacesMap().get(namespace);
        if (xsdNamespace == null)
        {
            xsdNamespace = new XsdNamespace(m_Model);
            m_Model.getNamespacesMap().put(namespace, xsdNamespace);
        }
        
        final NodeList childList = baseElement.getChildNodes();

        traverseNodes(childList, baseElement, xsdNamespace, null, null, xsdFile, 1);
    }
    
    /**
     * Method that traverses all nodes within the specified node list and adds any complex types and fields to the 
     * appropriate models.
     * 
     * @param nodeList
     *      List of nodes to iterate over.
     * @param parentElement
     *      Parent element of the nodes being iterated over.
     * @param xsdNamespace
     *      XSD namespace that new types should be added to.
     * @param jaxbClass
     *      JAXB class of the complex type the nodes are nested within. May be null if nodes are not nested within a
     *      complex type. 
     * @param type
     *      The model that represents the complex type the nodes are nested within. May be null nodes are not nested
     *      within a complex type.
     * @param xsdFile
     *      The XSD file where the nodes are located.
     * @param index
     *      Index value that should be used for a field.
     * @return
     *      Index value after parsing all nodes. Will be incremented for all fields that were found and added to the
     *      model of a complex type. 
     * @throws ClassNotFoundException
     *      Thrown if the JAXB class associated with a complex type cannot be found.
     * @throws JaxbProtoConvertException
     *      Thrown if a duplicate overridden ID is encountered.
     */
    private int traverseNodes(final NodeList nodeList, final Element parentElement, final XsdNamespace xsdNamespace, 
            final Class<?> jaxbClass, final XsdType type, final File xsdFile, final int index) 
                    throws ClassNotFoundException, JaxbProtoConvertException
    {
        int indexInt = index;
        for (int i = 0; i < nodeList.getLength(); i++)
        {
            final Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element element = (Element)node;
                final String nodeName = element.getNodeName();
                final String elementName = 
                        element.getAttribute("name"); //NOCHECKSTYLE Duplicate string is more readable
                
                if ("xs:complexType".equals(nodeName))
                {
                    handleComplexType(element, parentElement, xsdNamespace, type, xsdFile);
                }
                else if (nodeName.equals(XS_SIMPLE_TPYE) && containsEnumElements(node.getChildNodes()))
                {
                    handleSimpleType(element, parentElement, xsdNamespace, xsdFile);
                }
                else 
                {
                    if ((nodeName.equals("xs:attribute") || nodeName.equals("xs:element")) && type != null)
                    {
                        indexInt = handleFieldType(type, element, elementName, indexInt);
                    }
                    else if ("xs:extension".equals(nodeName))
                    {
                        indexInt = handleExtensionType(element, type, indexInt);
                    }
                    else if ("xs:restriction".equals(nodeName))
                    {
                        handleRestrictionType(element, parentElement, type);
                    }
                    indexInt = traverseNodes(element.getChildNodes(), element, xsdNamespace, jaxbClass, type, xsdFile, 
                            indexInt);
                }
            }
        }
        return indexInt;
    }
    
    /**
     * Method that handles an XSD element that is a complex type.
     * 
     * @param element
     *      XML element that represents the complex type.
     * @param parentElement
     *      Parent element of the complex type.
     * @param xsdNamespace
     *      XSD namespace the complex type belongs to.
     * @param type
     *      XSD type the complex type belongs to.
     * @param xsdFile
     *      XSD file the complex type is in.
     * @throws ClassNotFoundException
     *      Thrown if no JAXB class can be found for the complex type.
     * @throws JaxbProtoConvertException
     *      Thrown if a duplicate overridden ID is encountered.
     */
    private void handleComplexType(final Element element, final Element parentElement, final XsdNamespace xsdNamespace, 
            final XsdType type, final File xsdFile) throws ClassNotFoundException, JaxbProtoConvertException
    {
        final String elementName = 
                element.getAttribute("name"); //NOCHECKSTYLE Duplicate string is more readable
        final String typeName = elementName.isEmpty() ? parentElement.getAttribute("name") : elementName;
        
        if (type == null)
        {
            final Class<?> newJaxbClass = determineJaxbClass(xsdFile, WordUtils.capitalize(typeName));
            final XsdType newType = getOrCreateXsdType(xsdNamespace, xsdFile, newJaxbClass);
            traverseNodes(element.getChildNodes(), element, xsdNamespace, newJaxbClass, newType, xsdFile, 1);
        }
        else
        {
            throw new IllegalArgumentException(String.format("XSD parser does not handle nested complex "
                    + "types. Consider refactoring the XSD file. Type: %s File: %s", typeName, 
                    xsdFile.getAbsolutePath()));
        }
    }
    
    /**
     * Method that handles an XSD element that is a simple type.
     * 
     * @param element
     *      XML element that represents the simply type.
     * @param parentElement
     *      Parent element of the simple type.
     * @param xsdNamespace
     *      XSD namespace the simple type belongs to.
     * @param xsdFile
     *      XSD file the simple type is in.
     * @throws ClassNotFoundException
     *      Thrown if no JAXB class can be found for the simple type.
     */
    private void handleSimpleType(final Element element, final Element parentElement, final XsdNamespace xsdNamespace, 
            final File xsdFile) throws ClassNotFoundException
    {
        final String elementName = 
                element.getAttribute("name"); //NOCHECKSTYLE Duplicate string is more readable
        final String typeName = elementName.isEmpty() ? parentElement.getAttribute("name") : elementName;
        final Class<?> newJaxbClass = determineJaxbClass(xsdFile, WordUtils.capitalize(typeName));
        getOrCreateXsdType(xsdNamespace, xsdFile, newJaxbClass);
    }
    
    /**
     * Method that handles an XSD element that is an extension type.
     * 
     * @param element
     *      XML element that represents the extension type.
     * @param type
     *      The XSD type the extension belongs to.
     * @param index
     *      The current index value.
     * @return
     *      The current index value.
     * @throws ClassNotFoundException
     *      Thrown if no JAXB class can be found.
     * @throws JaxbProtoConvertException
     *      Thrown if a duplicate overridden ID is encountered.
     */
    private int handleExtensionType(final Element element, final XsdType type, final int index) 
            throws ClassNotFoundException, JaxbProtoConvertException
    {
        int indexInt = index;
        final String baseType = element.getAttribute(BASE_ATTRIBUTE);
        if ("xs:double".equals(baseType) || "xs:base64Binary".equals(baseType))
        {
            indexInt = handleFieldType(type, element, "value", indexInt);
        }
        else if (baseType.startsWith(XS_PREFIX))
        {
            throw new IllegalStateException(String.format("%s is a built in XSD type and should be "
                    + "handled", baseType));
        }
        else
        {
            handleBaseAttribute(element, type);
        }
        return indexInt;
    }
    
    /**
     * Method that handles an XSD element that is a restriction type.
     * 
     * @param element
     *      XML element that represents the restriction type.
     * @param parentElement
     *      Parent element of the restriction element.
     * @param type
     *      The XSD type the restriction belongs to.
     * @throws ClassNotFoundException
     *      Thrown if no JAXB class can be found.
     */
    private void handleRestrictionType(final Element element, final Element parentElement, final XsdType type)
            throws ClassNotFoundException
    {   
        final String baseType = element.getAttribute(BASE_ATTRIBUTE);
        if (!parentElement.getNodeName().equals(XS_SIMPLE_TPYE) && !baseType.startsWith(XS_PREFIX))
        {
            type.setComplexRestriction(true);
            handleBaseAttribute(element, type);
        }
    }
    
    /**
     * Method that handles an XSD element that contains a base attribute.
     * 
     * @param element
     *      XML element that has a base attribute.
     * @param type
     *      The XSD type the XML element with the attribute belongs to.
     * @throws ClassNotFoundException
     *      Thrown if no JAXB class can be found for the base type.
     */
    private void handleBaseAttribute(final Element element, final XsdType type) 
            throws ClassNotFoundException
    {
        final String base = element.getAttribute(BASE_ATTRIBUTE);
        final String[] splitBase = base.split(":");
        final String baseTypeName;
        if (splitBase.length == 2)
        {
            baseTypeName = splitBase[1];
        }
        else
        {
            baseTypeName = splitBase[0];
        }
        final XsdType baseType = getOrCreateBaseXsdType(baseTypeName);
        type.setBaseType(baseType);
    }

    /**
     * Method that retrieves the specified XSD type or creates it if one cannot be found.
     * 
     * @param namespace
     *      XSD namespace the type belongs to.
     * @param xsdFile
     *      XSD file the type is in.
     * @param jaxbClass
     *      JAXB class the XSD type represents.
     * @return
     *      The found or newly created XSD type.
     */
    private XsdType getOrCreateXsdType(final XsdNamespace namespace, final File xsdFile, final Class<?> jaxbClass)
    {
        XsdType type = namespace.getTypesMap().get(jaxbClass);
        if (type == null)
        {
            type = m_DiscoveredTypes.remove(jaxbClass.getSimpleName());
            type = type == null ? new XsdType() : type;
            type.setXsdNamespace(namespace);
            type.setJaxbType(jaxbClass);
            type.setXsdFile(xsdFile);
            namespace.getTypesMap().put(jaxbClass, type);
        }
        return type;
    }
    
    /**
     * Method that retrieves the XSD base type with specified type name or creates it if one cannot be found.
     * 
     * @param typeName
     *      The simple name of the base type to be retrieved.
     * @return
     *      The found or created base type.
     */
    private XsdType getOrCreateBaseXsdType(final String typeName)
    {
        for (String namespace: m_Model.getNamespacesMap().keySet())
        {
            final XsdNamespace namespaceModel = m_Model.getNamespacesMap().get(namespace);
            for (Class<?> type: namespaceModel.getTypesMap().keySet())
            {
                if (type.getSimpleName().equals(typeName))
                {
                    return namespaceModel.getTypesMap().get(type);
                }
            }
        }
        
        XsdType discoveredType = m_DiscoveredTypes.get(typeName);
        if (discoveredType == null)
        {
            discoveredType = new XsdType();
            m_DiscoveredTypes.put(typeName, discoveredType);
        }
        return discoveredType;
    }
    
    /**
     * Method used to check if a list of nodes contains an XSD enumeration type.
     * 
     * @param nodes
     *      List of XML nodes to be checked.
     * @return
     *      True if an XSD enumeration type is found and false otherwise.
     */
    private boolean containsEnumElements(final NodeList nodes)
    {
        for (int i = 0; i < nodes.getLength(); i++)
        {
            final Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element element = (Element)node;
                final String nodeName = element.getNodeName();
                if ("xs:enumeration".equals(nodeName))
                {
                    return true;
                }
                final boolean childrenContainEnum = containsEnumElements(node.getChildNodes());
                if (childrenContainEnum)
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * Handles xs tag that represents a field within a complex type.
     * 
     * @param type
     *      The complex type the field belongs to.
     * @param element
     *      The element that represents the field.
     * @param name
     *      The name of the field.
     * @param index
     *      The index of the field.
     * @return
     *      The incremented index after adding a field to the complex type.
     * @throws JaxbProtoConvertException
     *      Thrown if there is an issue handling the field.
     */
    private int handleFieldType(final XsdType type, final Element element, final String name, final int index) 
            throws JaxbProtoConvertException
    {
        int currentIndex = index;
        final Integer overriddenId = getIndexOverride(element.getChildNodes());
        if (overriddenId == null)
        {
            while (type.getOverriddenIds().contains(currentIndex))
            {
                currentIndex++;
            }
            type.getFieldsMap().put(name, new XsdField(type, currentIndex, false));
            currentIndex++;
        }
        else
        {
            if (type.getOverriddenIds().contains(overriddenId))
            {
                throw new JaxbProtoConvertException(
                        String.format("Duplicate overridden ID found in: %s", type.getXsdFile()));
            }
            type.getFieldsMap().put(name, new XsdField(type, overriddenId, true));
            type.getOverriddenIds().add(overriddenId);
        }
        return currentIndex;
    }
    
    /**
     * Determines if there is an appinfo with the index property and returns the index if there is.
     * 
     * @param nodes
     *      Nodes to be checked for an appinfo with an index property.
     * @return
     *      Integer that represents the index property if one is found.
     * @throws JaxbProtoConvertException 
     *      Thrown if the contents of the appinfo tag cannot be read.
     */
    private Integer getIndexOverride(final NodeList nodes) throws JaxbProtoConvertException
    {
        for (int i = 0; i < nodes.getLength(); i++)
        {
            final Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element element = (Element)node;
                final String nodeName = element.getNodeName();
                if ("xs:annotation".equals(nodeName))
                {
                    final Element appInfo = searchAnnotationForAppInfo(element.getChildNodes());
                    return getIndexFromAppInfo(appInfo);
                }
            }
        }
        return null;
    }
    
    /**
     * Searches the specified xs:annotation children for xs:appinfo nodes.
     * 
     * @param annotationNodes
     *      Node list that contains the children of an xs:annotation node.
     * @return
     *      Returns an xs:appinfo node if found. Otherwise returns null.
     */
    private Element searchAnnotationForAppInfo(final NodeList annotationNodes)
    {
        for (int i = 0; i < annotationNodes.getLength(); i++)
        {
            final Node annotationNode = annotationNodes.item(i);
            if (annotationNode.getNodeType() == Node.ELEMENT_NODE)
            {
                final Element annotationElement = (Element)annotationNode;
                final String annotationNodeName = annotationElement.getNodeName();
                if ("xs:appinfo".equals(annotationNodeName))
                {
                    return annotationElement;
                }
            }
        }
        return null;
    }
    
    /**
     * Checks for and returns the index property to be in the text content of an appinfo node.
     * 
     * @param appInfo
     *      Appinfo node to be checked for an index property.
     * @return
     *      Returns the index contained within the appinfo.
     * @throws JaxbProtoConvertException
     *      Thrown if the contents of the appinfo tag cannot be read.
     */
    private Integer getIndexFromAppInfo(final Element appInfo) throws JaxbProtoConvertException
    {
        if (appInfo != null)
        {
            final String content = appInfo.getTextContent();
            final Properties props = new Properties();
            try
            {
                props.load(new StringReader(content));
            }
            catch (final IOException ex)
            {
                throw new JaxbProtoConvertException("Invalid properties specified within appinfo tag.", ex);
            }

            final String index = props.getProperty("index");
            if (index != null)
            {
                return Integer.parseInt(index);
            }
        }
        return null;
    }
    
    /**
     * Determines the JAXB class associated with the XSD type.
     * 
     * @param xsdFile
     *      XSD file that contains the type.
     * @param xsdTypeName
     *      Name of the XSD type to determine the associated JAXB class for.
     * @return
     *      Class that represents the JAXB class associated with the XSD type.
     * @throws ClassNotFoundException
     *      Thrown if no associate JAXB class can be found for the XSD type.
     */
    private Class<?> determineJaxbClass(final File xsdFile, final String xsdTypeName) throws ClassNotFoundException
    {
        final Path relativePath = PathUtils.getRelativePath(xsdFile.getParentFile(), m_XsdFilesDir);
        
        final String packageSeparator = ".";
        final String partialPackageName = relativePath.toString().replace(File.separator, 
                packageSeparator);

        final String className = "mil.dod.th.core." + partialPackageName + packageSeparator
                + xsdTypeName;
        
        return Class.forName(className);
    }
}
