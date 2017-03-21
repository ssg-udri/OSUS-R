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
package mil.dod.th.ose.gui.webapp.asset;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.AssetMessages.ActivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.AssetNamespace.AssetMessageType;
import mil.dod.th.core.remote.proto.AssetMessages.CaptureDataRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeactivateRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.DeleteRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.PerformBitRequestData;
import mil.dod.th.core.remote.proto.AssetMessages.SetNameRequestData;
import mil.dod.th.ose.gui.webapp.utils.FacesContextUtil;
import mil.dod.th.ose.shared.SharedMessageUtils;
import mil.dod.th.remote.lexicon.types.remote.RemoteTypesGen;

import org.glassfish.osgicdi.OSGiService;
import org.primefaces.context.RequestContext;

/**
 * Implementation of {@link SelectedAsset}.
 * @author callen
 *
 */
@ManagedBean(name = "selectedAsset")
@ViewScoped
public class SelectedAssetImpl implements SelectedAsset
{
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * AssetManager service.
     */
    @ManagedProperty(value = "#{assetMgr}")
    private AssetMgrImpl assetMgr; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    @ManagedProperty(value = "#{assetDisplay}")
    private AssetDisplayHelper assetDisplay; //NOCHECKSTYLE - Name must match pattern / Breaks ManagedProperty
    
    /**
     * Reference to the faces context utility.
     */
    @Inject
    private FacesContextUtil m_FacesUtil;
    
    /**
     * Store the selected asset model, this is used to determine which asset model to show when displaying 
     * in a dialog.
     */
    private AssetModel m_SelectedAssetForDialog;   
    
    /**
     * Set the asset manager service to use. 
     * @param assetManager
     *     the asset manager service
     */
    public void setAssetMgr(final AssetMgrImpl assetManager)
    {
        assetMgr = assetManager;
    }
    
    /**
     * Set the asset display helper service to use.
     * @param assetDisp
     *      the asset display service
     */
    public void setAssetDisplay(final AssetDisplayHelper assetDisp)
    {
        assetDisplay = assetDisp;
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
     * Sets the faces context utility to use.
     * @param facesUtil
     *  the faces context utility to be set.
     */
    public void setFacesContextUtil(final FacesContextUtil facesUtil)
    {
        m_FacesUtil = facesUtil;
    }
    
    @Override
    public void setSelectedAssetForDialog(final AssetModel assetModel)
    {
        m_SelectedAssetForDialog = assetModel;
    }
    
    @Override
    public AssetModel getSelectedAssetForDialog()
    {
        return m_SelectedAssetForDialog;
    }

    @Override
    public void requestActivation(final AssetModel assetModel)
    {
        final int systemId = assetModel.getControllerId();
        final ActivateRequestData activateRequest = ActivateRequestData.newBuilder().
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetModel.getUuid())).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.ActivateRequest, 
                activateRequest).queue(systemId, null);
    }

    @Override
    public void requestDeactivation(final AssetModel assetModel)
    {
        final int systemId = assetModel.getControllerId();
        final DeactivateRequestData deactivateRequest = DeactivateRequestData.newBuilder().
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetModel.getUuid())).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.DeactivateRequest, 
                deactivateRequest).queue(systemId, null);
    }

    @Override
    public void requestBIT(final AssetModel assetModel)
    {
        final int systemId = assetModel.getControllerId();
        final PerformBitRequestData performBITRequest = PerformBitRequestData.newBuilder().
             setUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetModel.getUuid())).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.PerformBitRequest, performBITRequest).
            queue(systemId, null);
    }

    @Override
    public void requestCaptureData(final AssetModel assetModel)
    {
        final int systemId = assetModel.getControllerId();
        final String sensorId = assetModel.getSensorId();

        final CaptureDataRequestData.Builder performDataCapRequest = CaptureDataRequestData.newBuilder()
                .setUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetModel.getUuid()))
                .setObservationFormat(RemoteTypesGen.LexiconFormat.Enum.UUID_ONLY);
        if (sensorId != null && !sensorId.isEmpty())
        {
            performDataCapRequest.setSensorId(assetModel.getSensorId());
        }

        m_MessageFactory.createAssetMessage(AssetMessageType.CaptureDataRequest, performDataCapRequest.build()).
            queue(systemId, null);
    }

    @Override
    public void requestRemoval(final AssetModel assetModel)
    {
        // Unregister callbacks on page
        final RequestContext context = m_FacesUtil.getRequestContext();
        final String functionCall = "unregisterComponents('" + assetModel.getUuid() + "')";
        context.execute(functionCall);
        
        final int systemId = assetModel.getControllerId();
        final DeleteRequestData removeRequest = DeleteRequestData.newBuilder().
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetModel.getUuid())).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.DeleteRequest, 
            removeRequest).queue(systemId, null);
        
        //Set selected asset to null. This avoids a request with the wrong asset being sent if for some reason the
        //correct selected asset cannot be set.
        setSelectedAssetForDialog(null);
        
        assetDisplay.setSelectedFactoryObject(null);
    }
    
    @Override
    public void requestNameUpdate(final AssetModel assetModel)
    {
        final int systemId = assetModel.getControllerId();
        final SetNameRequestData nameRequest = SetNameRequestData.newBuilder().
            setAssetName(assetModel.getWorkingName()).
            setUuid(SharedMessageUtils.convertUUIDToProtoUUID(assetModel.getUuid())).build();
        m_MessageFactory.createAssetMessage(AssetMessageType.SetNameRequest, 
                nameRequest).queue(systemId, assetMgr.createRemoteAssetNameHandler(assetModel.getUuid()));
    }
}
