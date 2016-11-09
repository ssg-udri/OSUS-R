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

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;

import com.google.protobuf.Message;

import mil.dod.th.core.remote.ResponseHandler;
import mil.dod.th.core.remote.messaging.MessageFactory;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.AssetDirectoryServiceNamespace
    .AssetDirectoryServiceMessageType;
import mil.dod.th.core.remote.proto.AssetDirectoryServiceMessages.ScanForNewAssetsRequestData;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestMessage;
import mil.dod.th.core.remote.proto.RemoteBase.TerraHarvestPayload;
import mil.dod.th.ose.gui.webapp.utils.GrowlMessageUtil;

import org.glassfish.osgicdi.OSGiService;

/**
 * Implementation of the {@link ScanForAssetsHelper} interface.
 * 
 * @author cweisenborn
 */
@ManagedBean(name = "scanForAssetsHelper")
@ViewScoped
public class ScanForAssetsHelperImpl implements ScanForAssetsHelper, ResponseHandler
{
    /**
     * Inject the MessageFactory service for creating remote messages.
     */
    @Inject @OSGiService
    private MessageFactory m_MessageFactory;
    
    /**
     * Reference to the growl message utility.
     */
    @Inject
    private GrowlMessageUtil m_GrowlUtil;
    
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
     * Sets the growl message utility to be used.
     * @param growlUtil
     *      {@link GrowlMessageUtil} to be set.
     */
    public void setGrowlMessageUtil(final GrowlMessageUtil growlUtil)
    {
        m_GrowlUtil = growlUtil;
    }
    
    @Override
    public void requestScanForAssets(final int controllerId)
    {
        //Send a request for asset types first. This is done to make sure any assets created from scanner are of types
        //the GUI knows about.
        m_MessageFactory.createAssetDirectoryServiceMessage(AssetDirectoryServiceMessageType.GetAssetTypesRequest, 
                null).queue(controllerId, this);
    }

    @Override
    public void handleResponse(final TerraHarvestMessage message, final TerraHarvestPayload payload, 
            final Message namespaceMessage, final Message dataMessage)
    {
        if (namespaceMessage instanceof AssetDirectoryServiceNamespace)
        {
            final AssetDirectoryServiceNamespace assetDirNamespace = (AssetDirectoryServiceNamespace)namespaceMessage;
            if (assetDirNamespace.getType().equals(AssetDirectoryServiceMessageType.GetAssetTypesResponse))
            {
                final ScanForNewAssetsRequestData scanRequest = ScanForNewAssetsRequestData.newBuilder().build();
                m_MessageFactory.createAssetDirectoryServiceMessage(
                        AssetDirectoryServiceMessageType.ScanForNewAssetsRequest, 
                        scanRequest).queue(message.getSourceId(), this);
            }
            else if (assetDirNamespace.getType().equals(AssetDirectoryServiceMessageType.ScanForNewAssetsResponse))
            {
                m_GrowlUtil.createLocalFacesMessage(FacesMessage.SEVERITY_INFO, "Asset Scan Complete", 
                        "Scan for new assets has completed.");
            }
        }
    }
}
