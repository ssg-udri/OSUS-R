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

import javax.faces.event.ComponentSystemEvent;

import mil.dod.th.ose.gui.webapp.asset.AssetModel;

/**
 * This interface describes a view scoped bean which assists with the retrieval and deletion of remote observations.
 * @author allenchl
 *
 */
public interface RetrieveDeleteObsHelper
{
    /**
     * Method which retrieves the start date used to retrieve observations.
     * @return
     *      start date for retrieving observations
     */
    Date getStartDate();
    
    /**
     * Method which retrieves the end date used to retrieve observations.
     * @return
     *      end date for retrieving observations
     */
    Date getEndDate();
    
    /**
     * Method which sets the start date used to retrieve observations.
     * @param startDate
     *      start date for retrieving observations
     */
    void setStartDate(Date startDate);
    
    /**
     * Method which sets the end date used to retrieve observations.
     * @param endDate
     *      end date for retrieving observations
     */
    void setEndDate(Date endDate);
    
    /**
     * Sets value indicating whether or not observations should be retrieved/deleted by date.
     * Value is <code>true</code> if observations should be retrieved/deleted by date.
     * <code>false</code> otherwise. 
     * @param isRetrieveDeleteByDate
     *      <code>true</code> if observations should be retrieved/deleted by date.
     *      <code>false</code> otherwise.
     */
    void setRetrieveDeleteByDate(boolean isRetrieveDeleteByDate);
    
    /**
     * Gets value indicating whether or not observations should be retrieved/deleted by date.
     * Value is <code>true</code> if observations should be retrieved/deleted by date.
     * <code>false</code> otherwise. 
     * @return
     *      <code>true</code> if observations should be retrieved/deleted by date.
     *      <code>false</code> otherwise.
     */
    boolean isRetrieveDeleteByDate();

    /**
     * Returns the maximum number of observations wanted to be retrieved.
     * @return
     *      max number of observations wanted to be retrieved
     */
    int getMaxObservationNumber();

    /**
     * Sets the maximum number of observations wanted to be retrieved.
     * @param obsNumber
     *      max number of observations wanted to be retrieved
     */
    void setMaxObservationNumber(int obsNumber);
    
    /**
     * Gets the boolean value indicating whether or not retrieved observations should be 
     * limited to a specific number.
     * @return 
     *      <code>true</code> if the retrieval of observations is limited to a specific
     *      number. <code>false</code> if all observations should be retrieved.
     */
    boolean isRetrieveByMaxObsNum();
    
    /**
     * Sets the value indicating whether or not retrieved observations should be 
     * limited to a specific number.
     * @param isFilterByNum
     *      Parameter is <code>true</code> if the retrieval of observations is limited to a specific
     *      number. <code>false</code> if all observations should be retrieved.
     */
    void setRetrieveByMaxObsNum(boolean isFilterByNum);
    
    /**
     * Sends request for retrieving the observations within a certain date range.
     * @param model
     *      the currently selected base model
     */
    void submitRetrieveObservationsRequest(AssetModel model);

    /**
     * Sends request for deleting the observations. 
     * @param model
     *      the currently selected base model
     */
    void submitDeleteObservationsRequest(AssetModel model);

    /**
     * Removes the observation associated with the UUID of the observation set as the selected observation.
     */
    void deleteObservation();

    /**
     * Sets the currently selected observation's UUID.
     * @param uuid
     *      the UUID of the currently selected observation
     */
    void setSelectedObsUuid(UUID uuid);

    /**
     * Returns the currently selected observation's UUID.
     * @return
     * the UUID of the currently selected observation.
     */
    UUID getSelectedObsUuid();
    
    /**
     * PostValidateEvent function which verifies that a given end date within the component comes
     * after the given start date.
     * @param event
     *  the event which has been triggered on the PostValidateEvent
     */
    void validateDates(ComponentSystemEvent event);
}
