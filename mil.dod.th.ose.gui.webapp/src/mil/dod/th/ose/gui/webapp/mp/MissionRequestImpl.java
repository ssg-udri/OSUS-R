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
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.mp.model.MissionVariableMetaData;
import mil.dod.th.core.types.DigitalMedia;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.mp.MissionModel.MissionTemplateLocation;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;

import org.glassfish.osgicdi.OSGiService;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Implementation of the {@link MissionRequest} interface.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "missionRequest")
@RequestScoped
public class MissionRequestImpl implements MissionRequest
{
    /**
     * The currently selected active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Reference to the {@link MissionProgramMgr} service.
     */
    @ManagedProperty(value = "#{missionProgMgr}")
    private MissionProgramMgr missionProgMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Reference to the context utility used to get the faces and request context.
     */
    @Inject
    private FacesContextUtil m_FacesContextUtil;
    
    /**
     * Reference to the injected {@link TemplateProgramManager} service.
     */
    @Inject @OSGiService
    private TemplateProgramManager m_TemplateProgramManager;

    /**
     * Method used to set the {@link FacesContextUtil} used to get the FacesContext and RequestContext.
     * 
     * @param facesContextUtil
     *          The {@link FacesContextUtil} to be set.
     */
    public void setFacesContextUtil(final FacesContextUtil facesContextUtil)
    {
        m_FacesContextUtil = facesContextUtil;
    }
    
    /**
     * Method for setting the {@link MissionProgramTemplate} service. This setter method is used only for testing.
     * 
     * @param templateManager
     *          The {@link MissionProgramTemplate} service to be set.
     */
    public void setTemplateProgramManager(final TemplateProgramManager templateManager)
    {
        m_TemplateProgramManager = templateManager;
    }

    /**
     * Method for setting the Mission Program Manager (bean) service.
     * 
     * @param missionManager
     *          The mission program manager bean service to be set.
     */
    public void setMissionProgMgr(final MissionProgramMgr missionManager)
    {
        missionProgMgr = missionManager;
    }

    /**
     * Set the value of the currently active controller.
     * @param controller
     *     the controller that is currently the active controller
     */
    public void setActiveController(final ActiveController controller)
    {
        activeController = controller;
    }

    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionRequest#getMissions()
     */
    @SuppressWarnings("nestedfordepth") // TD: should simplify this method
    @Override
    public List<MissionModel> getMissions()
    {
        final List<MissionModel> missions = new ArrayList<MissionModel>();
        
        final List<String> namesOfRemoteTemplates;
        if (activeController.isActiveControllerSet())
        {
            //get a list of template names from the currently active controller
            namesOfRemoteTemplates = missionProgMgr.getRemoteTemplateNames(
                activeController.getActiveController().getId());
        }
        else
        {
            namesOfRemoteTemplates = new ArrayList<String>();
        }

        for (MissionProgramTemplate missionTemplate: m_TemplateProgramManager.getTemplates())
        {
            //Pulls all fields from the mission template and adds them to a mission model.
            final MissionModel mission = new MissionModel();
            mission.setName(missionTemplate.getName());
            mission.setDescription(missionTemplate.getDescription());
            mission.setSource(missionTemplate.getSource());
            mission.setWithImageCapture(missionTemplate.isWithImageCapture());
            mission.setWithInterval(missionTemplate.isWithInterval());
            mission.setWithSensorTrigger(missionTemplate.isWithSensorTrigger());
            mission.setWithTimerTrigger(missionTemplate.isWithTimerTrigger());
            
            //if the name of the template matches a name in the list of templates names on the active controller
            //then the template is synced
            if (namesOfRemoteTemplates.contains(mission.getName()))
            {
                mission.setLocation(MissionTemplateLocation.SYNCED);
            }
            else
            {
                mission.setLocation(MissionTemplateLocation.LOCAL);
            }
            
            //Pulls all secondary images from the mission template and adds them to the map in the mission model.
            //Each image is also given an integer as the key in the map. This will later be used when
            //the getStreamSecondaryImage method is called so that the appropriate image can be retrieved.
            for (int secondaryImageIndex = 0; secondaryImageIndex < missionTemplate.getSecondaryImages().size(); 
                    secondaryImageIndex++)
            {
                mission.getSecondaryImageIds().add(secondaryImageIndex);
            }
            
            //Pulls all variable metadata and creates a mission argument model for each variable.
            for (MissionVariableMetaData variableMetaData: missionTemplate.getVariableMetaData())
            {
                final MissionArgumentModel argument = new MissionArgumentModel();
                argument.setName(variableMetaData.getName());
                argument.setDescription(variableMetaData.getDescription());
                argument.setType(variableMetaData.getType());
                argument.setCurrentValue(variableMetaData.getDefaultValue());
                argument.setMinValue(variableMetaData.getMinValue());
                argument.setMaxValue(variableMetaData.getMaxValue());
                if (variableMetaData.getOptionValues().size() > 0)
                {
                    for (String value: variableMetaData.getOptionValues())
                    {
                        argument.getOptionValues().add(value);
                    }
                }
                mission.getArguments().add(argument);
            }
            missions.add(mission);
        }
        return missions;
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionRequest#getStreamPrimaryImage()
     */
    @Override
    public StreamedContent getStreamPrimaryImage()
    {
        //Gets the name of the mission the primary image belongs to.
        final FacesContext context = m_FacesContextUtil.getFacesContext();
        final String missionName = context.getExternalContext().getRequestParameterMap().get("primeImageMissionName");
        
        //Send an empty stream back if name is null. This is used to handle the render call the page makes.
        if (missionName == null)
        {
            return new DefaultStreamedContent();
        }
        final MissionProgramTemplate missionTemp = m_TemplateProgramManager.getTemplate(missionName);
        final byte[] primeImage = missionTemp.getPrimaryImage().getValue();
        final String encoding = missionTemp.getPrimaryImage().getEncoding();

        return new DefaultStreamedContent(new ByteArrayInputStream(primeImage), encoding);
    }
    
    /* (non-Javadoc)
     * @see mil.dod.th.ose.gui.webapp.mp.MissionRequest#getStreamSecondaryImage()
     */
    @Override
    public StreamedContent getStreamSecondaryImage()
    {
        //Get the name of mission the secondary image belongs to.
        final FacesContext context = m_FacesContextUtil.getFacesContext();
        final String missionName = context.getExternalContext().getRequestParameterMap().get("secondImageMissionName");
        
        //Send an empty stream back if name is null. This is used to handle the render call the page makes.
        if (missionName == null)
        {
            return new DefaultStreamedContent();
        }
        
        //Get the id that is associated with the desired secondary image.
        final String imageIdStr = context.getExternalContext().getRequestParameterMap().get("imageId");
        final int imageId = Integer.parseInt(imageIdStr);
        final MissionProgramTemplate missionTemp = m_TemplateProgramManager.getTemplate(missionName);
        final DigitalMedia image = missionTemp.getSecondaryImages().get(imageId);

        return new DefaultStreamedContent(new ByteArrayInputStream(image.getValue()), image.getEncoding());
    }
}
