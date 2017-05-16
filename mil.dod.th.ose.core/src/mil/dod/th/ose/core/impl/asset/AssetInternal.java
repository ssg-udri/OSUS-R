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
package mil.dod.th.ose.core.impl.asset;

import mil.dod.th.core.asset.AssetContext;
import mil.dod.th.core.asset.AssetException;
import mil.dod.th.ose.core.factory.api.FactoryInternal;
import mil.dod.th.ose.core.factory.api.FactoryObjectInternal;

/**
 * Contains internal functions available to the {@link mil.dod.th.core.asset.Asset} implementation. 
 * Includes functions that should not be part of the {@link mil.dod.th.core.asset.Asset} interface 
 * available to outside consumers, but is needed internally. 
 * consumers.
 * 
 * @author dhumeniuk
 *
 */
public interface AssetInternal extends AssetContext, FactoryObjectInternal
{
    /**
     * ID of the component factory for this class.
     */
    String COMPONENT_FACTORY_REG_ID = "mil.dod.th.ose.core.impl.asset.AssetInternal";
    
    /**
     * Key value pair to denote an asset service type component.
     */
    String SERVICE_TYPE_PAIR = FactoryObjectInternal.SERVICE_TYPE_KEY + "=Asset";
    
    /**
     * Setter method for the Asset Active Status.
     * 
     * @param status
     *            The status of the Asset
     */
    void setActiveStatus(AssetActiveStatus status);
    
    /**
     * Method used to internally invoke activation of an asset.
     * 
     * @throws AssetException
     *      thrown if asset cannot be activated
     */
    void onActivate() throws AssetException;
    
    /**
     * Method used to internally invoke deactivation of an asset.
     * 
     * @throws AssetException
     *      thrown if asset cannot be deactivated
     */
    void onDeactivate() throws AssetException;
    
    @Override
    FactoryInternal getFactory();
    
    /**
     * Same as {@link #deactivateAsync()} but returns the thread that is executing the deactivation.
     * 
     * @param listeners
     *      Listeners for the deactivation
     * @return
     *      Thread that has the {@link Deactivator} running
     */
    Thread internalDeactivate(AssetActivationListener[] listeners);
}
