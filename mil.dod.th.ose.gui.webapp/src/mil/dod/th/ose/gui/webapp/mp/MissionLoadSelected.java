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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;

import mil.dod.th.core.mp.TemplateProgramManager;
import mil.dod.th.core.mp.model.MissionProgramParameters;
import mil.dod.th.core.mp.model.MissionProgramSchedule;
import mil.dod.th.core.mp.model.MissionProgramTemplate;
import mil.dod.th.core.remote.objectconverter.JaxbProtoObjectConverter;
import mil.dod.th.core.remote.objectconverter.ObjectConverterException;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadParametersRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.LoadTemplateRequestData;
import mil.dod.th.core.remote.proto.MissionProgramMessages.MissionProgrammingNamespace.MissionProgrammingMessageType;
import mil.dod.th.core.types.MapEntry;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.mp.MissionModel.MissionTemplateLocation;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramParametersGen;
import mil.dod.th.remote.lexicon.mp.model.MissionProgramTemplateGen;

import org.glassfish.osgicdi.OSGiService;


/**
 * Loads the 'current' mission reference from the {@link MissionSetUpMgr}.
 * @author callen
 *
 */
@ManagedBean //NOCHECKSTYLE: high fan out. Many of the import are for the support of the proto messages
@RequestScoped
public class MissionLoadSelected 
{
    /**
     * String to use as description for messages and logging.
     */
    private static final String LOG_MESSAGE_DESCRIPTOR = "Mission Info:";
    
    /**
     * Mission manager is used to get the reference to the current mission being configured.
     */
    @ManagedProperty(value = "#{missionSetUpMgr}")
    private MissionSetUpMgr missionSetUpMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * The currently selected active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * The mission program manager.
     */
    @ManagedProperty(value = "#{missionProgMgr}")
    private MissionProgramMgr missionProgMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Template manager used to get templates from the local system.
     */
    @Inject @OSGiService
    private TemplateProgramManager m_TemplateManager;
    
    /**
     * The {@link JaxbProtoObjectConverter} responsible for converting between JAXB and protocol buffer objects. 
     */
    @Inject @OSGiService
    private JaxbProtoObjectConverter m_Converter;
    
    /**
     * Growl message utility for creating growl messages.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
    /**
     * Set the {@link JaxbProtoObjectConverter}.
     * 
     * @param converter
     *     the service responsible for converting between JAXB and protocol buffer objects.
     */
    public void setConverter(final JaxbProtoObjectConverter converter)
    {
        m_Converter = converter;
    }
    
    /**
     * Set the {@link MissionSetUpMgr} service.
     * @param missionManager
     *     the service object to use
     */
    public void setMissionSetUpMgr(final MissionSetUpMgr missionManager)
    {
        this.missionSetUpMgr = missionManager;
    }

    /**
     * Set the template program manager.
     * @param templateManager
     *     the template program manager service to use
     */
    public void setTemplateProgramManager(final TemplateProgramManager templateManager)
    {
        m_TemplateManager = templateManager;
    }
    
    /**
     * Set the mission program manager.
     * @param programManager
     *  the mission program manager service to use
     */
    public void setMissionProgMgr(final MissionProgramMgr programManager)
    {
        this.missionProgMgr = programManager;
    }
    
    /**
     * Set the growl message utility service.
     * @param growlUtil
     *      the growl message utility service to use.
     */
    public void setGrowlMessageUtility(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    /**
     * Set the value of the currently active controller.
     * @param controller
     *     the controller that is currently the active controller
     */
    public void setActiveController(final ActiveController controller)
    {
        this.activeController = controller;
    }
    
    /**
     * Load the currently referenced mission from the {@link MissionSetUpMgr}.
     * @return
     *     string representing the next destination to be rendered to the browser
     * @throws ObjectConverterException
     *     throw in the event that the template or parameters cannot be converted to proto messages 
     * @throws IllegalArgumentException
     *     thrown in the event that the template selected to load does not exist 
     */
    public String loadMission() throws IllegalArgumentException, ObjectConverterException
    {
        if (!this.activeController.isActiveControllerSet()) 
        {
            //returning null will prevent navigation
            return null;
        }

        //get the current values from the mission manager
        final MissionModel model = this.missionSetUpMgr.getMission();
        final MissionProgramSchedule schedule = this.missionSetUpMgr.getSchedule();
        final int controllerId = this.activeController.getActiveController().getId();
        if (model.getLocation() == MissionTemplateLocation.LOCAL)
        {
            sendTemplateToActiveController(model.getName());
        }
        //transform arguments and send to controller
        //list to store arguments
        final List<MapEntry> parameters = new ArrayList<MapEntry>();
        //iterate over argument models to retrieve values for parameters list
        for (MissionArgumentModel argument : model.getArguments())
        {
            //make a map entry and fill with values from arguments
            final MapEntry entry = new MapEntry();
            entry.setKey(argument.getName());
            entry.setValue(argument.getCurrentValue());
            parameters.add(entry);
        }
        //transform mission arguments to mission program parameters, set schedule as active
        schedule.setActive(true);

        //check if a name was set for the created mission, if empty append a generated UUID to the template name
        String name = this.missionSetUpMgr.getProgramName();
        if (name == null || name.isEmpty())
        {
            name = this.missionSetUpMgr.getMission().getName().concat(UUID.randomUUID().toString());
            
            //post growl of assigned name
            m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "Mission Program ",
                String.format("A mission program loaded for controller 0x%08x was not named, assigned name is %s", 
                    controllerId, name));
        }

        //construct params
        final MissionProgramParameters params = new MissionProgramParameters().withProgramName(name).
            withSchedule(schedule).withParameters(parameters).withTemplateName(model.getName());
        //send the parameters to the controller
        sendParametersToActiveController(params);
        
        //notify user of success
        m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, LOG_MESSAGE_DESCRIPTOR, 
            String.format("Mission %s using template %s was sent to 0x%08x", name, model.getName(), controllerId));
        
        this.missionSetUpMgr.setMission(null);
        return "";
    }
    
    /**
     * Send a template to the currently 'active' system.
     * @param templateName
     *     the name of the template to send 
     * @throws ObjectConverterException 
     *     thrown if the template cannot be converted to a proto message
     * @throws IllegalArgumentException
     *     thrown if the template is not found locally     
     */
    private void sendTemplateToActiveController(final String templateName) throws ObjectConverterException, 
            IllegalArgumentException
    {
        //retrieve the template from the template manager
        final MissionProgramTemplate template = m_TemplateManager.getTemplate(templateName);
        if (template == null)
        {
            //throw exception representing that the template is not known locally
            throw new IllegalArgumentException("The template was not found locally, despite the template being flagged"
                 + " as being available locally!");
        }
        //convert to proto template
        final MissionProgramTemplateGen.MissionProgramTemplate templateGen = 
                (MissionProgramTemplateGen.MissionProgramTemplate)m_Converter.convertToProto(template);
        //construct load template request
        final LoadTemplateRequestData loadMessage = LoadTemplateRequestData.newBuilder().setMission(templateGen).
            build();
        
        //send message
        this.missionProgMgr.queueMessage(this.activeController.getActiveController().getId(), loadMessage,
                MissionProgrammingMessageType.LoadTemplateRequest);
    }   
    
    /**
     * Send the mission program parameters to the controller via a terra harvest message.
     * 
     * @param params
     *     the actual mission program parameters to send
     * @throws ObjectConverterException 
     *     thrown if the parameters cannot be converted to a proto message 
     */
    private void sendParametersToActiveController(final MissionProgramParameters params) throws ObjectConverterException
    {
        //create proto message parameters
        final MissionProgramParametersGen.MissionProgramParameters paramsGen = 
            (MissionProgramParametersGen.MissionProgramParameters)m_Converter.convertToProto(params);
        //construct load template request
        final LoadParametersRequestData loadMessage = LoadParametersRequestData.newBuilder().
            setParameters(paramsGen).build();
        //send the parameters
        this.missionProgMgr.queueMessage(this.activeController.getActiveController().getId(), 
                loadMessage, MissionProgrammingMessageType.LoadParametersRequest);
    }
    
}
