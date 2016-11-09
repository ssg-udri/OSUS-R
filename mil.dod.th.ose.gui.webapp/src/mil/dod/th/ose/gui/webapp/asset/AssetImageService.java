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

import java.util.UUID;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;

import mil.dod.th.ose.gui.webapp.controller.ActiveController;
import mil.dod.th.ose.gui.webapp.general.ImageDisplayInterface;

/**
 * Request scoped bean for serving up the image representing an asset.
 * @author allenchl
 *
 */
@ManagedBean(name = "assetImage")
@RequestScoped
public class AssetImageService
{
    /**
     * AssetManager service.
     */
    @ManagedProperty(value = "#{assetMgr}")
    private AssetMgr assetMgr; //NOCHECKSTYLE must match exactly with the bean name.
    
    /**
     * The active controller.
     */
    @ManagedProperty(value = "#{activeController}")
    private ActiveController activeController; //NOCHECKSTYLE must match exactly with the bean name.
    
    /**
     * Set the asset manager service to use. 
     * @param assetManager
     *     the asset manager service
     */
    public void setAssetMgr(final AssetMgr assetManager)
    {
        assetMgr = assetManager;
    }
    
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
     * Get the image belonging to the given asset. from the system with the given
     * Id.
     * @param assetUuid
     *      the string representation of an asset's UUID
     * @return
     *      string representation of the image of the asset
     */
    public String tryGetAssetImage(final UUID assetUuid)
    {
        if (activeController.isActiveControllerSet()) //need to ensure that an active controller is set
        {
            final AssetModel model = 
                assetMgr.getAssetModelByUuid(assetUuid, activeController.getActiveController().getId());
            if (model != null)
            {
                return model.getImage();
            }
        }
        return ImageDisplayInterface.DEFAULT_IMAGE;
    }
}
