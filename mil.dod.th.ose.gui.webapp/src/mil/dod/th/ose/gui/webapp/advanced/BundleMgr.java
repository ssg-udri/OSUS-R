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
package mil.dod.th.ose.gui.webapp.advanced;

import java.util.List;

/**
 * This class handles keeping track of the bundles located on each controller. When a controller becomes the active 
 * controller and the bundles configuration tab is viewed the controller will be added to a list of controllers. Once 
 * added, that controller will be sent a request for information of each bundle located on that controller. Once  
 * information about the bundles located on the controller is received a list of those bundles is created and stored.
 * 
 * @author cweisenborn
 */
public interface BundleMgr
{
    /** Event topic prefix for all BundleMgr events. */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/advanced/BundleMgr/";
    
    /** Event topic used to receive all BundleMgr events. */
    String TOPIC_ALL_BUNDLE_MGR_EVENTS = TOPIC_PREFIX + "*";
    
    /** Property key used to retrieve the bundle location. */
    String EVENT_PROP_BUNDLE_LOCATION = "bundle.location";
    
    /** Topic used to specify information on a remote bundle was received and that the bundle manager has updated the
     * information stored about this bundle. Contains the following properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller the bundle is located on.
     * <li>{@link org.osgi.service.event.EventConstants#BUNDLE_ID} - ID of the bundle information was received about.
     * <li>{@link #EVENT_PROP_BUNDLE_LOCATION} - Location of the updated bundle.
     * </ul>
     */
    String TOPIC_BUNDLE_INFO_RECEIVED = TOPIC_PREFIX + "BUNDLE_INFO_RECEIVED";
    
    /**
     * Topic used to specify information on a remote bundle has been removed from the bundle manager. Contains the
     * following properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller the bundle is located on.
     * <li>{@link org.osgi.service.event.EventConstants#BUNDLE_ID} - ID of the bundle information was received about.
     * <li>{@link #EVENT_PROP_BUNDLE_LOCATION} - Location of the updated bundle.
     * </ul>
     */
    String TOPIC_BUNDLE_INFO_REMOVED = TOPIC_PREFIX + "BUNDLE_INFO_REMOVED";
    
    /**
     * Topic used when existing bundle status changes to either started or stopped. Contains the following properties:
     * <ul>
     * <li>{@link mil.dod.th.ose.gui.api.SharedPropertyConstants#EVENT_PROP_CONTROLLER_ID} - ID of the 
     * controller the bundle is located on.
     * <li>{@link org.osgi.service.event.EventConstants#BUNDLE_ID} - ID of the bundle information was received about.
     * <li>{@link #EVENT_PROP_BUNDLE_LOCATION} - Location of the updated bundle.
     * </ul>
     */
    String TOPIC_BUNDLE_STATUS_UPDATED = TOPIC_PREFIX + "BUNDLE_STATUS_UPDATED";
    
    /**
     * Method that returns a list of bundles for the specified controller. If the specified controller is unknown
     * then an entry for that controller is created and a request for bundle information is sent. The class will post
     * a {@link #TOPIC_BUNDLE_INFO_RECEIVED} event once the information has been retrieved from the controller.
     * 
     * @param controllerId
     *          ID of the controller with which to retrieve a list of bundles for.
     * @return
     *          List of bundles contained on the specified controller. The list returned will be empty if the controller
     *          is unknown and a message will be sent to retrieve bundle information from the controller. The 
     *          {@link #TOPIC_BUNDLE_INFO_RECEIVED} information is retrieved from the controller.
     */
    List<BundleModel> getBundlesAsync(int controllerId);
    
    /**
     * Method that retrieves the model that represents a specific bundle on the specified controller. This method may
     * return null if the controller does not contain a bundle with the specified location.
     * 
     * @param controllerId
     *          Controller to retrieve information on the bundle with the specified ID.
     * @param bundleLocation
     *          The unique bundle location of the bundle to retrieve information for.
     * @return
     *          Bundle model that represents the bundle with the corresponding ID on the specified controller. May 
     *          return null if the controller does not contain a bundle with the specified location.
     */
    BundleModel retrieveBundleByLocationAsync(int controllerId, String bundleLocation);
}
