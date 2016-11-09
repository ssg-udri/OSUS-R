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
package mil.dod.th.ose.core.factory.api;

import java.util.List;

import mil.dod.th.core.factory.FactoryException;

import org.osgi.service.cm.ConfigurationException;

/**
 * Interface which defines callback methods to be used when the {@link FactoryRegistry} performs actions.
 * 
 * @author nickmarcucci
 * 
 * @param <T> 
 *      type should be an internal type such as {@link mil.dod.th.ose.core.impl.asset.AssetInternal}, 
 *      which is used internally by the core to control objects.
 *
 */
public interface FactoryRegistryCallback<T extends FactoryObjectInternal>
{
    /**
     * This method will be called just prior to the initialize method for each {@link 
     * mil.dod.th.core.factory.FactoryObjectProxy} base type (like {@link 
     * mil.dod.th.core.asset.AssetProxy#initialize(mil.dod.th.core.asset.AssetContext, java.util.Map)}).
     * 
     * @param object
     *      object that its proxy is about to be initialized
     * @throws FactoryException
     *      if there is an error while performing the callback
     * @throws ConfigurationException
     *      if there was an error with the configuration
     */
    void preObjectInitialize(T object) throws FactoryException, ConfigurationException;
    
    /**
     * This method will be called just after the initialize method for each {@link 
     * mil.dod.th.core.factory.FactoryObjectProxy} base type (like {@link 
     * mil.dod.th.core.asset.AssetProxy#initialize(mil.dod.th.core.asset.AssetContext, java.util.Map)}).
     * 
     * @param object
     *      object that its proxy was just initialized
     * @throws FactoryException
     *      if there is an error while performing the callback
     */
    void postObjectInitialize(T object) throws FactoryException;
    
    /**
     * This method will be called just prior to {@link 
     * mil.dod.th.core.factory.FactoryObjectProxy#updated(java.util.Map)} being called.
     * 
     * @param object
     *      object that its proxy is about to be notified of an update
     * @throws FactoryException
     *      if there is an error while performing the callback
     * @throws ConfigurationException
     *      if there was an error with the configuration
     */
    void preObjectUpdated(T object) throws FactoryException, ConfigurationException;
    
    /**
     * This method notifies that an object has just been removed from circulation.
     * 
     * @param object
     *      object that was removed
     *      
     */
    void onRemovedObject(T object);
    
    /**
     * This method is called to retrieve the {@link RegistryDependency}s required for the specific internal
     * type object. For example {@link mil.dod.th.core.ccomm.link.LinkLayer}s depend on a 
     * {@link mil.dod.th.core.ccomm.physical.PhysicalLink}.
     * 
     * @return
     *      list of dependencies required for this services specific type of objects
     */
    List<RegistryDependency> retrieveRegistryDependencies();
}
