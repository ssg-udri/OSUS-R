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
package mil.dod.th.ose.gui.webapp.mp;

import java.io.ByteArrayInputStream;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.xml.bind.MarshalException;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.xml.XmlMarshalService;

import org.glassfish.osgicdi.OSGiService;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * This class handles exporting mission templates from the mission setup page.
 * 
 * @author cweisenborn
 */
@ManagedBean
@RequestScoped
public class MissionExport
{   
    /**
     * Reference to the injected {@link TemplateProgramManager} service.
     */
    @Inject @OSGiService
    private TemplateProgramManager m_TemplateProgramManager;
    
    /**
     * Service for creating and validating JAXB objects into XML files.
     */
    @Inject @OSGiService
    private XmlMarshalService m_MarshalService;
    
    /**
     * Reference to the mission being exported.
     */
    private StreamedContent m_DownloadFile;
    
    /**
     * Method for setting the {@link MissionProgramTemplate} service.
     * 
     * @param templateManager
     *          The {@link MissionProgramTemplate} service to be set.
     */
    public void setTemplateProgramManager(final TemplateProgramManager templateManager)
    {
        m_TemplateProgramManager = templateManager;
    }
    
    /**
     * Set the XMLMarshal service.
     * 
     * @param xmlMarshallService
     *          the service used to marshal incoming JAXB objects.
     */
    public void setXMLMarshalService(final XmlMarshalService xmlMarshallService)
    {
        m_MarshalService = xmlMarshallService;
    }
    
    /**
     * Retrieves the file to be downloaded.
     * 
     * @return
     *          The file to be downloaded.
     */
    public StreamedContent getDownloadFile()
    {
        return m_DownloadFile;
    }
    
    /**
     * Method called to handle exporting a mission script in the {@link TemplateProgramManager} to an XML file.
     * 
     * @param missionName
     *          The name of the mission template to be exported as an XML file.
     * @throws MarshalException 
     *          thrown if the mission template specified is unable to be converted to XML
     *          or thrown if the XML created from the mission template is unable to be read.                  
     */
    public void handleExportMission(final String missionName) throws MarshalException
    {
        final MissionProgramTemplate template = m_TemplateProgramManager.getTemplate(missionName);
        final byte[] fileByteArray = m_MarshalService.createXmlByteArray(template, true);
        m_DownloadFile = new DefaultStreamedContent(new ByteArrayInputStream(fileByteArray), "application/xml", 
                missionName + ".xml");
    }
}
