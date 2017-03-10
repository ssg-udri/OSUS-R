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
package mil.dod.th.ose.gui.webapp.comms;

import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.log.Logging;
import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateLinkLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreatePhysicalLinkRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CreateTransportLayerRequestData;
import mil.dod.th.core.remote.proto.CustomCommsMessages.CustomCommsNamespace.CustomCommsMessageType;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.converter.PhysicalLinkTypeEnumConverter;

import org.glassfish.osgicdi.OSGiService;
import org.osgi.service.log.LogService;

/**
 * Implementation of the {@link AddCommsMessageController}.
 * @author allenchl
 *
 */
@ManagedBean(name = "addCommsController")
@ViewScoped
public class AddCommsMessageControllerImpl implements AddCommsMessageController 
{ 
    /**
     * Comms manager service to use.
     */
    @ManagedProperty(value = "#{commsMgr}")
    private CommsMgr commsMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
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
     * Set the comms manager service.
     * @param commsManager
     *      comms manager service to set.
     */
    public void setCommsMgr(final CommsMgr commsManager)
    {
        commsMgr = commsManager;
    }
    
    @Override
    public void submitNewCommsStackRequest(final int systemId, final CommsStackCreationModel model) 
    {
        if (model.isForceAdd())
        {
            final CreatePhysicalLinkRequestData physicalRequest = CreatePhysicalLinkRequestData.newBuilder().
                    setPhysicalLinkType(
                            PhysicalLinkTypeEnumConverter.convertJavaEnumToProto(model.getSelectedPhysicalType())).
                    setPhysicalLinkName(model.getNewPhysicalName()).build();
            
            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.CreatePhysicalLinkRequest, 
                    physicalRequest).queue(systemId, null); 
            Logging.log(LogService.LOG_DEBUG, "Requested to create physical link with the name: [%s] and of type: [%s]",
                    model.getNewPhysicalName(), model.getSelectedPhysicalType());
        }
        else
        {
            ResponseHandler handler = null;
            if (model.getSelectedTransportLayerType() != null)
            {                
                final CreateTransportLayerRequestData.Builder transportLayerRequest = CreateTransportLayerRequestData
                        .newBuilder()
                        .setTransportLayerName(model.getNewTransportName())
                        .setTransportLayerProductType(model.getSelectedTransportLayerType()); 
                handler = commsMgr.createLinkLayerHandler(transportLayerRequest);
                Logging.log(LogService.LOG_DEBUG, "Requested to create transport layer with the name: [%s] and of "
                        + "type: [%s]", model.getNewTransportName(), model.getSelectedTransportLayerType());
            }

            final CreateLinkLayerRequestData linkLayerRequest;
            if (model.getSelectedPhysicalLink() == null || model.getSelectedPhysicalLink().isEmpty())
            {
                linkLayerRequest = CreateLinkLayerRequestData.newBuilder().
                        setLinkLayerName(model.getNewLinkName()).
                        setLinkLayerProductType(model.getSelectedLinkLayerType()).build();
            }
            else
            {
                final UUID physUuid = commsMgr.getPhysicalUuidByName(model.getSelectedPhysicalLink(), systemId);
                linkLayerRequest = CreateLinkLayerRequestData.newBuilder().
                        setLinkLayerName(model.getNewLinkName()).
                        setLinkLayerProductType(model.getSelectedLinkLayerType()).
                        setPhysicalLinkUuid(SharedMessageUtils.convertUUIDToProtoUUID(physUuid)).build();
            }

            m_MessageFactory.createCustomCommsMessage(CustomCommsMessageType.CreateLinkLayerRequest, 
                    linkLayerRequest).queue(systemId, handler); 
            Logging.log(LogService.LOG_DEBUG, "Requested to create link layer with name: [%s] and of type: [%s]", 
                    model.getNewLinkName(), model.getSelectedLinkLayerType());
        } 
    }
}
