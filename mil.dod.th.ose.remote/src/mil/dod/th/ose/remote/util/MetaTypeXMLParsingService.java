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

import java.io.IOException;
import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This interface assists with parsing XML documents. 
 * @author callen
 *
 */
@Component(provide = { MetaTypeXMLParsingService.class })
public class MetaTypeXMLParsingService
{
    /**
     * Document builder factory used to get an instance of a Document builder.
     */
    private DocumentBuilderFactoryService m_DocumentBuilderFactory;

    /**
     * Document builder used to parse XML documents.
     */
    private DocumentBuilder m_DocumentBuilder;

    /**
     * Set the Document builder service.
     * @param docBuilder
     *     the document builder to use.
     */
    @Reference
    public void setDocumentBuilderFactory(final DocumentBuilderFactoryService docBuilder)
    {
        m_DocumentBuilderFactory = docBuilder;
    }

    /**
     * Create an instance of a document builder.
     * @throws ParserConfigurationException 
     *     if a DocumentBuilder cannot be created which satisfies the configuration requested
     */
    @Activate
    public void activate() throws ParserConfigurationException
    {
        m_DocumentBuilder = m_DocumentBuilderFactory.getDocumentBuilder();
    }

    /**
     * Parses the XML document at the give URI. If the OCD element or ultimately the PID cannot be found this will
     * return null.
     * @param uri
     *    the URI pointing the the XML document
     * @return
     *    the PID attribute value
     * @throws IOException
     *    if the file cannot be opened
     * @throws SAXException
     *    if the document is unable to be parsed, also thrown in the event that the document is not found.
     */
    public synchronized String getPidAttribute(final URI uri) throws IOException, SAXException
    {
        //the builder creates an in memory document by parsing the file
        final Document doc = m_DocumentBuilder.parse(uri.toString());
        
        //Organizes the children nodes under the parent(outer) node 
        doc.getDocumentElement().normalize();
        
        //get the OCD, object class definition node from the xml document
        final NodeList children = doc.getElementsByTagName("OCD");
        //iterate through the nodes in the list, there could be more than one OCD
        for (int index = 0; index < children.getLength(); index++)
        {
            //get the node
            final Node nNode = children.item(index);

            // make sure this is an element node, the entire document is already in memory
            if (nNode.getNodeType() == Node.ELEMENT_NODE)
            {
                //cast the node to an element to utilize the element interface
                final Element eElement = (Element) nNode;

                //success the name attribute which is also the PID :-O
                return eElement.getAttribute("id");
            }
        }
        return null;
    }
}
