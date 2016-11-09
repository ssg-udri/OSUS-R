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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import aQute.bnd.annotation.component.Component;

/**
 * Service that returns instances of document builders.
 * @author callen
 *
 */
@Component(provide = { DocumentBuilderFactoryService.class })
public class DocumentBuilderFactoryService
{
    /**
     * Document builder factory used to create instances document builders.
     */
    private final DocumentBuilderFactory m_DocumentFactory = DocumentBuilderFactory.newInstance();

    /**
     * Get an instance of a document builder.
     * @return
     *    a document builder instance
     * @throws ParserConfigurationException
     *    if a DocumentBuilder cannot be created which satisfies the configuration requested
     */
    public DocumentBuilder getDocumentBuilder() throws ParserConfigurationException
    {
        return m_DocumentFactory.newDocumentBuilder();
    }
}
