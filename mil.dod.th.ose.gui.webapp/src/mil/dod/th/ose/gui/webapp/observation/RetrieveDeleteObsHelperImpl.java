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

import java.util.Date;
import java.util.UUID;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Inject;

import com.google.protobuf.Message;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.persistence.ObservationQuery;
import mil.dod.th.core.persistence.ObservationStore;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.GetObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.ObservationStoreNamespace.ObservationStoreMessageType;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.Query;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationByUUIDRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.RemoveObservationRequestData;
import mil.dod.th.core.remote.proto.ObservationStoreMessages.TimeConstraintData;
import mil.dod.th.ose.gui.webapp.asset.AssetModel;
import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.utils.DateTimeConverterUtil;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;

import org.osgi.service.log.LogService;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link RetrieveDeleteObsHelper}.
 * @author allenchl
 *
 */
@ManagedBean(name = "retieveDeleteObs")
@ViewScoped 
public class RetrieveDeleteObsHelperImpl implements RetrieveDeleteObsHelper
{
    /**
     * The active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty

    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;

    /**
     * The observation store instance.
     */
    @Inject @OSGiService
    private ObservationStore m_ObservationStore;
    
    /**
     * Reference to the faces context utility.
     */
    @Inject
    private FacesContextUtil m_FacesUtil;
    
    /**
     * Flag indicating whether or not observations should be retrieved/deleted by date.
     * Value is <code>true</code> if observations should be retrieved/deleted by date.
     */
    private boolean m_IsRetrieveDeleteByDate;

    /**
     * Flag indicating whether or not retrieved observations should be limited to a specific number.
     * Value is <code>true</code> if observations should be limited by number.
     */
    private boolean m_IsRetrieveByMaxObsNum;

    /**
     * Holds the value of the start date used to retrieve observations.
     */
    private Date m_StartDate;

    /**
     * Holds the value of the end date used to retrieve observations. 
     */
    private Date m_EndDate;
    
    /**
     * Holds the value of the number of observations to return.
     */
    private int m_ObservationNumber;
    
    /**
     * Holds the currently selected observation's UUID.
     */
    private UUID m_SelectedUUID;
    
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
     * Method that sets the MessageFactory service to use.
     * 
     * @param messageFactory
     *          MessageFactory service to set.
     */
    public void setMessageFactory(final MessageFactory messageFactory)
    {
        m_MessageFactory = messageFactory;
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
     * Sets the faces context utility to use.
     * @param facesUtil
     *  the faces context utility to be set.
     */
    public void setFacesContextUtil(final FacesContextUtil facesUtil)
    {
        m_FacesUtil = facesUtil;
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
    public void setRetrieveDeleteByDate(final boolean isRetrieveDeleteByDate)
    {
        m_IsRetrieveDeleteByDate = isRetrieveDeleteByDate;
    }

    @Override
    public boolean isRetrieveDeleteByDate()
    {
        return m_IsRetrieveDeleteByDate;
    }

    @Override
    public int getMaxObservationNumber()
    {
        return m_ObservationNumber;
    }

    @Override
    public void setMaxObservationNumber(final int obsNumber)
    {
        m_ObservationNumber = obsNumber;
    }
    
    @Override
    public boolean isRetrieveByMaxObsNum()
    {
        return m_IsRetrieveByMaxObsNum;
    }

    @Override
    public void setRetrieveByMaxObsNum(final boolean isFilterByNum)
    {
        m_IsRetrieveByMaxObsNum = isFilterByNum;        
    }
   
    @Override
    public void setSelectedObsUuid(final UUID uuid)
    {
        m_SelectedUUID = uuid;
    }

    @Override
    public UUID getSelectedObsUuid()
    {
        return m_SelectedUUID;
    }
    
    @Override
    public void deleteObservation()
    {
        if (m_SelectedUUID == null)
        {
            Logging.log(LogService.LOG_ERROR, "Delete request recieved, but the UUID is null.");
        }
        else
        {
            //remove locally
            m_ObservationStore.remove(m_SelectedUUID);

            //call to remove from remote controller 
            final RemoveObservationByUUIDRequestData removeData = RemoveObservationByUUIDRequestData.newBuilder().
                addUuidOfObservation(SharedMessageUtils.convertUUIDToProtoUUID(m_SelectedUUID)).build();

            m_MessageFactory.createObservationStoreMessage(ObservationStoreMessageType.RemoveObservationByUUIDRequest,
                removeData).queue(activeController.getActiveController().getId(), null);
        }
    }

    @Override
    public void submitRetrieveObservationsRequest(final AssetModel model)
    {
        final Query.Builder queryBuilder = Query.newBuilder();
        if (model != null)
        {       
            queryBuilder.setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid()));
        }
        m_MessageFactory.createObservationStoreMessage(ObservationStoreMessageType.
                GetObservationRequest, requestBuilderHelper(false, queryBuilder)).
                    queue(activeController.getActiveController().getId(), null);
    }

    @Override
    public void submitDeleteObservationsRequest(final AssetModel model)
    {
        final Query.Builder builder = Query.newBuilder();
        if (model == null) //indicates that ALL asset models need to be removed
        {
            removeLocalObservations(null);
        }
        else
        {
            removeLocalObservations(model.getUuid());
            builder.setAssetUuid(SharedMessageUtils.convertUUIDToProtoUUID(model.getUuid()));
            
        }
        m_MessageFactory.createObservationStoreMessage(ObservationStoreMessageType.RemoveObservationRequest,
            requestBuilderHelper(true, builder)).queue(activeController.getActiveController().getId(), null);
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
        final UIInput startDateComponent = (UIInput)components.findComponent("startDateRetrieve");
        final UIInput endDateComponent = (UIInput)components.findComponent("endDateRetrieve");
        
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
    
    /**
     * Helper method which removes an observation if it exists. 
     * @param assetUuid
     *      UUID of the asset containing the observations
     */
    private void removeLocalObservations(final UUID assetUuid)
    {
        final ObservationQuery query = m_ObservationStore.newQuery();
        if (assetUuid != null)
        {
            query.withAssetUuid(assetUuid);
        }
        if (m_IsRetrieveDeleteByDate)
        {
            final Date endDate = DateTimeConverterUtil.roundToEndOfSecond(m_EndDate);
            query.withTimeCreatedRange(m_StartDate, endDate);
        }
        query.remove();
    }
    
    /**
     * Helper that builds either a remove observation request or a get observation request with or without time
     * constraints based on the given input.
     * 
     * @param isRemoveRequest
     *            <code>true</code> if the method should build a remove observation request. <code>false</code> if the
     *            method should build a get observation request.
     * @param builder
     *            builder used to create a query 
     * @return a remove observation request or a get observation request based on the given input
     */
    private Message requestBuilderHelper(final boolean isRemoveRequest, final Query.Builder builder)
    {
        if (m_IsRetrieveDeleteByDate && m_EndDate != null && m_StartDate != null)
        {
            final Date endDate = DateTimeConverterUtil.roundToEndOfSecond(m_EndDate);
            final TimeConstraintData dates = TimeConstraintData.newBuilder().
                    setStartTime(m_StartDate.getTime()).
                    setStopTime(endDate.getTime()).build();
            
            builder.setCreatedTimeRange(dates);
        }
        //removal
        if (isRemoveRequest)
        {
            return RemoveObservationRequestData.newBuilder().setObsQuery(builder.build()).build();
        }
        //if max obs is needed too
        if (m_IsRetrieveByMaxObsNum && m_ObservationNumber > 0)
        {
            builder.setMaxNumberOfObs(m_ObservationNumber);
        }
        return GetObservationRequestData.newBuilder().setObsQuery(builder.build()).build();
    }
}
