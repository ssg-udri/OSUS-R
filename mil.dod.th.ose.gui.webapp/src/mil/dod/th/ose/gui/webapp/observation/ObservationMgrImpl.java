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
package mil.dod.th.ose.gui.webapp.observation; 

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.ObservationRef;
import mil.dod.th.core.observation.types.TargetClassification;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.types.SensingModality;
import mil.dod.th.core.types.observation.ObservationSubTypeEnum;
import mil.dod.th.ose.gui.webapp.asset.AssetDisplayHelper;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.controller.ObservationCountMgr;
import mil.dod.th.ose.gui.webapp.factory.FactoryBaseModel;
import mil.dod.th.ose.gui.webapp.utils.DateTimeConverterUtil;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.shared.JdoDataStore;

import org.osgi.service.log.LogService;

import org.glassfish.osgicdi.OSGiService;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 * Implementation of {@link ObservationMgr} interface.
 * @author nickmarcucci
 *
 */
@ManagedBean(name = "obsMgr")
@ViewScoped 
public class ObservationMgrImpl implements ObservationMgr
{
    /**
     * String to be used for adding the date information to a filter string.
     */
    private static final String DATE_STRING = " && createdTimestamp >= %d && createdTimestamp <= %d";
    
    /**
     * String to be used for adding the selected asset information to a filter string.
     */
    private static final String SELECTED_ASSET_STRING = " && assetUuid == '%s'";
    
    /**
     * The asset display helper.
     */
    @ManagedProperty(value = "#{assetDisplay}")
    private AssetDisplayHelper assetDisplay; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * The active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * The observation count manager.
     */
    @ManagedProperty(value = "#{observationCountMgr}")
    private ObservationCountMgr observationCountMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Reference to the faces context utility.
     */
    @Inject
    private FacesContextUtil m_FacesUtil;
    
    /**
     * The observation store jdo query instance.
     */
    @SuppressWarnings("rawtypes")
    @Inject @OSGiService(serviceCriteria = "(" + JdoDataStore.PROP_KEY_DATASTORE_TYPE + "="
            + JdoDataStore.PROP_OBSERVATION_STORE + ")")
    private JdoDataStore m_JdoObservationStore;
    
    /**
     * The observation store instance.
     */
    @Inject @OSGiService
    private ObservationStore m_ObservationStore;
    
    /**
     * Flag indicating if observations should be filtered by 'filter expression'.
     */
    private boolean m_IsFilterByExpression;

    /**
     * Filter string that is passed to store to refine search.
     */
    private String m_Filter;
    
    /**
     * Flag indicating whether or not observations should be filtered by date.
     * Value is <code>true</code> if observations should be filtered by date.
     */
    private boolean m_IsFilterByDate;
    
    /**
     * Holds the value of the start date used to filter observations.
     */
    private Date m_StartDate;

    /**
     * Holds the value of the end date used to filter observations. 
     */
    private Date m_EndDate;
    
    /**
     * Model containing the observations that are lazy loaded.
     */
    private LazyDataModel<GuiObservation> m_ObservationModel;
    
    /**
     * Sets the {@link ActiveController} instance.
     * @param activeCntrller
     *      the current instance.
     */
    public void setActiveController(final ActiveController activeCntrller)
    {
        activeController = activeCntrller;
    }

    /**
     * Sets the faces context utility to use.
     * @param facesUtil
     *  the faces context utility to be set.
     */
    public void setFacesContextUtil(final FacesContextUtil facesUtil)
    {
        m_FacesUtil = facesUtil;
    }
    
    /**
     * Sets the {@link ObservationCountMgr} instance.
     * @param obsCountMgr
     *  The current instance.
     */
    public void setObservationCountMgr(final ObservationCountMgr obsCountMgr)
    {
        observationCountMgr = obsCountMgr;
    }

    /**
     * Sets the ObservationStore's JDO object query instance.
     * @param obsJdo
     *  The current ObservationStore instance.
     */
    @SuppressWarnings("rawtypes")
    public void setJdoDataStore(final JdoDataStore obsJdo)
    {
        m_JdoObservationStore = obsJdo;
    }
    
    /**
     * Sets the ObservationStore instance.
     * @param obsStore
     *  The current ObservationStore instance.
     */
    public void setObservationStore(final ObservationStore obsStore)
    {
        m_ObservationStore = obsStore;
    }
    
    /**
     * Sets the AssetDisplayHelper instance.
     * @param displayHelper
     *  the current AssetDisplayHelper instance.
     */
    public void setAssetDisplay(final AssetDisplayHelper displayHelper)
    {
        assetDisplay = displayHelper;
    }
    
    /**
     * Post construct method. This method is used to clear the observation count for the active controller when
     * the observation tab is visited.
     */
    @PostConstruct
    public void postConstruct()
    {
        final Calendar date = Calendar.getInstance();
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        m_EndDate = date.getTime();
        date.add(Calendar.DAY_OF_MONTH, -1);
        m_StartDate = date.getTime();        
        
        m_ObservationModel = new ObservationDataLazyModel();
    }
      
    @Override
    public LazyDataModel<GuiObservation> getObservations()
    {
        return m_ObservationModel;
    }

    @Override
    public List<String> getTargetClassifications(final Observation obs)
    {
        final List<String> classifications = new ArrayList<String>();

        final List<TargetClassification> tClass = obs.getDetection().getTargetClassifications();

        for (TargetClassification classification : tClass)
        {
            classifications.add(classification.getType().getValue().toString().toLowerCase());
        }
        return classifications;
    }

    @Override
    public List<String> getModalities(final Observation obs)
    {
        final List<String> modalities = new ArrayList<String>();

        final List<SensingModality> sensings = obs.getModalities();

        for (SensingModality modality : sensings)
        {
            modalities.add(modality.getValue().toString().toLowerCase());
        }

        return modalities;
    }
    
    @Override
    public Date getStartDate()
    {
        return m_StartDate;
    }

    @Override
    public Date getEndDate()
    {
        return m_EndDate;
    }

    @Override
    public void setStartDate(final Date startDate)
    {
        m_StartDate = startDate;        
    }

    @Override
    public void setEndDate(final Date endDate)
    {
        m_EndDate = endDate;        
    }
    
    @Override
    public void setFilterByDate(final boolean isFilter)
    {
        m_IsFilterByDate = isFilter; 
    }

    @Override
    public boolean isFilterByDate()
    {
        return m_IsFilterByDate;
    }

    @Override
    public void validateDates(final ComponentSystemEvent event)
    {
        //the component returned represents the outputPanel containing both start
        //and end calendar components.
        //that is why both the startDate and endDate components can be found from the 
        //'components' UIComponent
        final UIComponent components = event.getComponent(); 
       
        //reused by two components, so make sure that the appropriate paired values are validated
        final UIInput startDateComponent = (UIInput)components.findComponent("startDateFilter");
        final UIInput endDateComponent = (UIInput)components.findComponent("endDateFilter");

        final Date startDate = (Date)startDateComponent.getLocalValue();
        final Date endDate = (Date)endDateComponent.getLocalValue();
        
        
        if (startDate != null && endDate != null && endDate.before(startDate))
        {
            final FacesContext context = m_FacesUtil.getFacesContext();

            final FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR, "",
                    "'Start Date' must come before 'End Date'.");

            context.addMessage(startDateComponent.getClientId(), message);
            context.validationFailed();
            context.renderResponse();
        }   
    }

    @Override
    public String getFilter()
    {
        return m_Filter;
    }

    @Override
    public void setFilter(final String filter)
    {
        m_Filter = filter;
    }

    @Override
    public boolean isFilterByExpression()
    {
        return m_IsFilterByExpression;
    }

    @Override
    public void setFilterByExpression(final boolean filterByExpression)
    {
        m_IsFilterByExpression = filterByExpression;
    }

    @Override
    public void checkFilter(final FacesContext context, final UIComponent component, final Object value)
    {
        final String filter = (String) value;
        final javax.jdo.Query query = m_JdoObservationStore.newJdoQuery();
        query.setFilter(filter);
        
        try
        {
            query.compile();
        }
        catch (final Exception e)
        {
            final FacesMessage message = new FacesMessage("Filter is not valid", e.getMessage());
            throw new ValidatorException(message, e);
        }
    }
    
    @Override
    public void handleManualFilterRequest()
    {
        final FacesContext context = m_FacesUtil.getFacesContext();

        if (!context.isValidationFailed()) 
        {
            m_FacesUtil.getRequestContext().update(":obsTable");
        }
    }
    
    @Override
    public GuiObservation getObservation(final UUID observationUuid)
    {
        final Observation observation = m_ObservationStore.find(observationUuid);
        //If the observation was not found then return null.
        if (observation == null)
        {
            return null;
        }
        return compileSingleGuiObservation(observation);
    }
    
    /**
     * Method which returns a {@link ObservationQuery} based on the given query criteria.
     
     * @return 
     *  the {@link ObservationQuery} object based on the set filters or null if the filters given are not properly set
     */
    private ObservationQuery createObservationQuery()
    {
        final FactoryBaseModel model = assetDisplay.getSelectedFactoryObject();
        final ObservationQuery query = m_ObservationStore.newQuery();

        if (model != null)
        {
            query.withAssetUuid(model.getUuid());
        }

        if (m_IsFilterByDate)
        {
            //missing date will be notified on filter tab
            if (m_StartDate == null || m_EndDate == null || m_EndDate.before(m_StartDate))
            {
                return null;
            }
            
            final Date endDate = DateTimeConverterUtil.roundToEndOfSecond(m_EndDate);
            query.withTimeCreatedRange(m_StartDate, endDate);
        }
        return query;
    }
    
    /**
     * Create a JDO query for {@link Observation} retrieval.
     * 
     * @return
     *      a JDO query based on the set query criteria or null if criteria
     *      set is not valid.
     */
    private javax.jdo.Query createJdoQuery()
    {
        final FactoryBaseModel model = assetDisplay.getSelectedFactoryObject();
        final javax.jdo.Query query = m_JdoObservationStore.newJdoQuery();
        String formattedFilter = m_Filter;
        if (model != null)
        {
            formattedFilter = String.format(formattedFilter + SELECTED_ASSET_STRING, model.getUuid().toString());
        }
        
        if (m_IsFilterByDate)
        {
            //missing date will be notified on filter tab
            if (m_StartDate == null || m_EndDate == null || m_EndDate.before(m_StartDate))
            {
                return null;
            }
            final Date endDate = DateTimeConverterUtil.roundToEndOfSecond(m_EndDate);
            formattedFilter = String.format(formattedFilter + DATE_STRING, m_StartDate.getTime(), endDate.getTime());
        }
        
        query.setFilter(formattedFilter);

        return query;
    }
    
    /**
     * Method to determine the observation type of the given observation.
     * @param observation
     *  the observation for which the type is to be determined
     * @return
     *  the observation type that describes the information that this observation holds.
     *  if the observation does not satisfy any of the known types then null will be returned.
     */
    private ObservationSubTypeEnum determineObservationSubType(final Observation observation)
    {
        ObservationSubTypeEnum type = null;
        
        if (observation.isSetDetection())
        {
            type = ObservationSubTypeEnum.DETECTION;
        }
        else if (observation.isSetStatus())
        {
            type = ObservationSubTypeEnum.STATUS;
        }
        else if (observation.isSetWeather())
        {
            type = ObservationSubTypeEnum.WEATHER;
        }
        else if (observation.isSetDigitalMedia())
        {
            type = determineDigitalMediaSubType(observation);
        }
        else if (observation.isSetBiological())
        {
            type = ObservationSubTypeEnum.BIOLOGICAL;
        }
        else if (observation.isSetChemical())
        {
            type = ObservationSubTypeEnum.CHEMICAL;
        }
        else if (observation.isSetCbrneTrigger())
        {
            type = ObservationSubTypeEnum.CBRNE_TRIGGER;
        }
        else if (observation.isSetWaterQuality())
        {
            type = ObservationSubTypeEnum.WATER_QUALITY;
        }
        else if (observation.isSetPower())
        {
            type = ObservationSubTypeEnum.POWER;
        }
        
        return type;
    }
    
    /**
     * Method used to determine the sub type of a digital media observation.
     * 
     * @param observation
     *      Observation to determine the digital media sub type of.
     * @return
     *      The observation sub type of the digital media observation.
     */
    private ObservationSubTypeEnum determineDigitalMediaSubType(final Observation observation)
    {
        if (observation.isSetAudioMetadata())
        {
            return ObservationSubTypeEnum.AUDIO_METADATA;
        }
        else if (observation.isSetImageMetadata())
        {
            return ObservationSubTypeEnum.IMAGE_METADATA;
        }
        else if (observation.isSetVideoMetadata())
        {
            return ObservationSubTypeEnum.VIDEO_METADATA;
        }
        else if (observation.isSetChannelMetadata())
        {
            return ObservationSubTypeEnum.CHANNEL_METADATA;
        }
        return ObservationSubTypeEnum.NONE;
    }

    /**
     * Method accepts a list of observations and compiles a list of gui displayable observations.
     * @param observations
     *  the list of observations that are to be converted
     * @return
     *  a list of gui displayable observations; empty list if empty list of observations is passed in
     */
    private List<GuiObservation> compileGuiObservations(final List<Observation> observations)
    {
        final List<GuiObservation> guiObservations = new ArrayList<>();
        
        for (Observation observation : observations)
        {
            guiObservations.add(compileSingleGuiObservation(observation));
        }
        
        return guiObservations;
    }
    
    /**
     * Method that accepts an observation and compiles it as a gui displayable observation.
     * 
     * @param observation
     *  the observation to be converted.
     * @return
     *  the converted observation.
     */
    private GuiObservation compileSingleGuiObservation(final Observation observation)
    {
        final List<RelatedObservationIdentity> types = new ArrayList<>();
        for (ObservationRef obsRef : observation.getRelatedObservations())
        {
            final Observation referencedObs = m_ObservationStore.find(obsRef.getUuid());
            
            if (referencedObs != null)
            {
                types.add(new RelatedObservationIdentity(true, determineObservationSubType(referencedObs)));
            }
            else
            {
                types.add(new RelatedObservationIdentity(false, null));
                
                Logging.log(LogService.LOG_ERROR, "Could not find related observation with uuid %s " 
                        + "for observation with uuid %s. ", obsRef.getUuid(), observation.getUuid());
            }
        }
        
        return new GuiObservation(observation, types);
    }
    
    /**
     * Class used for implementation of lazy loading for observation table.
     */
    private class ObservationDataLazyModel extends LazyDataModel<GuiObservation>
    {
        /** Serial ID. */
        private static final long serialVersionUID = 283482009482L;
        
        @SuppressWarnings("unchecked")
        @Override
        public List<GuiObservation> load(final int first, final int pageSize, 
                final String sortField, final SortOrder sortOrder,
                final Map<String, String> filters)
        {
            final List<Observation> observations = new ArrayList<Observation>();
            // check if the request is a filter string action, then analyze if the string filter expression should 
            // be evaluated... lastly make sure that if the filter expression should be evaluated that it's value is
            // not null
            if (m_IsFilterByExpression && m_Filter != null && !m_Filter.isEmpty())
            {
                final javax.jdo.Query queryObs = createJdoQuery();
                final javax.jdo.Query queryCount = createJdoQuery();
                
                if (queryObs == null)
                {
                    m_ObservationModel.setRowCount(0);
                }
                else
                {
                    m_ObservationModel.setRowCount((int)m_JdoObservationStore.executeGetCount(queryCount));
                    
                    queryObs.setRange(first, first + pageSize);
                    queryObs.setOrdering("createdTimestamp descending");
                    observations.addAll(m_JdoObservationStore.executeJdoQuery(queryObs));
                }
            }
            else
            {
                final ObservationQuery queryObs = createObservationQuery();
                final ObservationQuery queryCount = createObservationQuery();
                
                if (queryObs == null)
                {
                    m_ObservationModel.setRowCount(0);
                }
                else
                {
                    m_ObservationModel.setRowCount((int)queryCount.getCount());
                    
                    queryObs.withRange(first, first + pageSize);
                    observations.addAll(queryObs.execute());
                }
            }
            
            observationCountMgr.clearObsCount(activeController.getActiveController().getId());
            
            return compileGuiObservations(observations);
        }
    }
}
