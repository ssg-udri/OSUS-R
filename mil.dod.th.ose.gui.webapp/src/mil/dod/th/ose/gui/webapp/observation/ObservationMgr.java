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
import java.util.List;
import java.util.UUID;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.validator.ValidatorException;

import mil.dod.th.core.observation.types.Observation;

import org.primefaces.model.LazyDataModel;

/**
 * This class takes care constructing the view model for the observation tab.
 * 
 * @author nickmarcucci
 *
 */
public interface ObservationMgr
{
    /** 
     * Event topic prefix for all {@link ObservationMgr} events. 
     */
    String TOPIC_PREFIX = "mil/dod/th/ose/gui/webapp/observation/ObservationMgr/";
    
    /**
     * Topic used for when the observation store was altered, either by retrieval or deletion requests.
     */
    String TOPIC_OBS_STORE_UPDATED = TOPIC_PREFIX + "OBS_STORE_UPDATED";
    
    /**
     * Get observation data model for the currently active controller.
     * 
     * @return
     *      a lazy data model that will only load observations needed for the page
     */
    LazyDataModel<GuiObservation> getObservations();
    
    /**
     * Return a list of target classifications in string form.
     * @param obs
     *  the observation for which to retrieve target classifications
     * @return
     * a list of target classifications that have been
     */
    List<String> getTargetClassifications(Observation obs);

    /**
     * Return the string representation of the sensing modality for an 
     * observation.
     * @param obs
     *  the observation from which to retrieve the sensing modality
     * @return
     *  the list of the string representation of the sensing modalities 
     *  for an observation
     */
    List<String> getModalities(Observation obs);
    
    /**
     * Method which retrieves the start date used to filter observations.
     * @return
     *      start date for filtering observations
     */
    Date getStartDate();
    
    /**
     * Method which retrieves the end date used to filter observations.
     * @return
     *      end date for filtering observations
     */
    Date getEndDate();
    
    /**
     * Method which sets the start date used to filter observations.
     * @param startDate
     *      start date for filtering observations
     */
    void setStartDate(Date startDate);
    
    /**
     * Method which sets the end date used to filter observations.
     * @param endDate
     *      end date for filtering observations
     */
    void setEndDate(Date endDate);
    
    /**
     * Sets value indicating whether or not observations should be filtered by date.
     * Value is <code>true</code> if observations should be filtered by date.
     * <code>false</code> otherwise. 
     * @param isFilterByDate
     *      <code>true</code> if observations should be filtered by date.
     *      <code>false</code> otherwise.
     */
    void setFilterByDate(boolean isFilterByDate);
    
    /**
     * Gets value indicating whether or not observations should be filtered by date.
     * Value is <code>true</code> if observations should be filtered by date.
     * <code>false</code> otherwise. 
     * @return
     *      <code>true</code> if observations should be filtered by date.
     *      <code>false</code> otherwise.
     */
    boolean isFilterByDate();
    
    /**
     * PostValidateEvent function which verifies that a given end date within the component comes
     * after the given start date.
     * @param event
     *  the event which has been triggered on the PostValidateEvent
     */
    void validateDates(ComponentSystemEvent event);

    /**
     * Returns the filter for the observation query.
     * @return
     *      filter currently set
     */
    String getFilter();
    
    /**
     * Sets the filter to use for observation queries.
     * @param filter
     *      filter to use
     */
    void setFilter(String filter);
    
    /**
     *  Gets the boolean value indicating whether filtered observations should be 
     * in relation to a filter string.
     * @return 
     *      <code>true</code> if filtered observations should be in relation to the filter string,
     *      <code>false</code> if not.
     */
    boolean isFilterByExpression();
    
    /**
     *  Sets the boolean value indicating whether filtered observations should be 
     * in relation to a filter string.
     * @param filterByExpression
     *      <code>true</code> if filtered observations should be in relation to the filter string,
     *      <code>false</code> if not.
     */
    void setFilterByExpression(boolean filterByExpression);
    
    /**
     * Check the current filter value is valid.
     * @param context
     *      context for the current value
     * @param component
     *      component that is being updated
     * @param value
     *      current value that needs to be validated
     * @throws ValidatorException
     *      if validation fails
     */
    void checkFilter(FacesContext context, UIComponent component, Object value) throws ValidatorException;
    
    /**
     * Handle the manual (button press) request to filter observations.
     */
    void handleManualFilterRequest();
    
    /**
     * Retrieve the observation model for the observation with the specified UUID.
     * 
     * @param observationUuid
     *      UUID of the observation to be retrieved.
     * @return
     *      Model that represents the observation with the specified UUID.
     */
    GuiObservation getObservation(UUID observationUuid);
}
