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

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.xml.bind.UnmarshalException;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.persistence.PersistenceFailedException;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.glassfish.osgicdi.OSGiService;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

/**
 * This class handles the import of mission templates on the mission setup page.
 * 
 * @author cweisenborn
 */
@ManagedBean
@RequestScoped
public class MissionImport
{
    /**
     * Reference to the growl utility used to create growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlMessageUtil;
    
    /**
     * Reference to the injected {@link TemplateProgramManager} service.
     */
    @Inject @OSGiService
    private TemplateProgramManager m_TemplateProgramManager;
       
    /**
     * Method used to set the growl message utility used to create growl messages.
     * 
     * @param growlMessageUtil
     *          The {@link GrowlMessageUtil} to be set.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlMessageUtil)
    {
        m_GrowlMessageUtil = growlMessageUtil;
    }
    
    /**
     * Method for setting the MissionProgramTemplate service.
     * 
     * @param templateManager
     *          The MissionProgramTemplate service to be set.
     */
    public void setTemplateProgramManager(final TemplateProgramManager templateManager)
    {
        m_TemplateProgramManager = templateManager;
    }
    
    /**
     * Method called to handle importing an XML mission file to the {@link TemplateProgramManager}.
     * 
     * @param event 
     *        event that is thrown with each file being uploaded
     * @throws IOException
     *         If the file being imported is unable to read from.
     */
    public void handleImportMission(final FileUploadEvent event) throws IOException
    {   
        final UploadedFile missionFile = event.getFile();
        try
        {
            m_TemplateProgramManager.loadFromStream(missionFile.getInputstream(), false);
            
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO,
                    "Mission template successfully imported!",
                    missionFile.getFileName() + " was successfully imported.");
        }
        catch (final IllegalArgumentException exception)
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, "Mission template already exists!", 
                    String.format("%s. The name of the template will need to be changed before it can be imported.", 
                    exception.getMessage()), exception);
            
        }
        catch (final UnmarshalException | IllegalStateException exception)
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid XML file!", "The XML file "
                    + "was unable to be converted to a mission template. Be sure that the mission template follows"
                    + " the schema for mission templates.", exception);
        }
        catch (final PersistenceFailedException exception)
        {
            m_GrowlMessageUtil.createLocalFacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Mission template was unable to be " 
                    + "stored!", "The mission template being imported was unable to be saved in the local data store.", 
                    exception);
        }
    }
}
