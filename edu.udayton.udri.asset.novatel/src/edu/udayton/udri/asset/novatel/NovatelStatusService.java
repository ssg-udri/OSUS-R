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
package edu.udayton.udri.asset.novatel;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.observation.types.Observation;
import mil.dod.th.core.observation.types.Status;
import mil.dod.th.core.types.ComponentType;
import mil.dod.th.core.types.status.ComponentStatus;
import mil.dod.th.core.types.status.OperatingStatus;
import mil.dod.th.core.types.status.SummaryStatusEnum;

/**
 * This class handles translating a Novatel asset's component statuses.
 * @author allenchl
 *
 */
@Component(servicefactory = true, provide = NovatelStatusService.class)
public class NovatelStatusService
{
    /**
     * Map which holds the statuses of known components that make up 
     * this asset.
     */
    private final Map<ComponentType, ComponentStatus> m_ComponentStatusMap = 
            Collections.synchronizedMap(new HashMap<ComponentType, ComponentStatus>());
    
    /**
     * Evaluate if the given status is necessitates updating the entire asset's status. 
     * <br>
     * <strong>NOTE: Once a component status is added of a particular type, that {@link ComponentType}
     * is expected to be relevant for the life span of the asset. Thus the status for that component
     * will always be represented in the asset's overall status.</strong>
     * 
     * @param statusObs
     *      the observation which contains the last known status, may be null if no such observation exists 
     * @param compStatus
     *      the new component statuses
     * @return
     *      constructed status to post, or null if the status should not be updated
     */
    public synchronized Status getStatus(final Observation statusObs, final ComponentStatus compStatus)
    {
        m_ComponentStatusMap.put(compStatus.getComponent(), compStatus);
        if (shouldUpdateEntireStatus(statusObs))
        {
            return getCompleteStatus();
        }
        return null;
    }

    /**
     * Evaluate if the given status is actually worth updating the entire asset's status.
     * @param statusObs
     *      the observation that holds the last known status
     * @return
     *      <code> true </code> if the entire status should be updated, <code> false </code> otherwise
     */
    private boolean shouldUpdateEntireStatus(final Observation statusObs)
    {
        //no former status exists, so status should be updated
        if (statusObs == null)
        {
            return true;
        }
        
        //grab list of the last component statuses
        final List<ComponentStatus> lastComponentStatuses = 
                statusObs.getStatus().getComponentStatuses();
        
        if (lastComponentStatuses.size() < m_ComponentStatusMap.size()) // must be a new status
        {
            return true;
        }
        
        //go through the last known statuses
        for (ComponentStatus formerComponentStatus : lastComponentStatuses)
        {
            //the new status if available
            final ComponentStatus newStatus = m_ComponentStatusMap.get(formerComponentStatus.getComponent());
            //if the components match descriptions then check if the old status and the new are different
            //if they are different we need a new status to be created.
            if (newStatus.getStatus().getDescription().equals(formerComponentStatus.getStatus().getDescription()))
            {
                if (newStatus.getStatus().getSummary() != formerComponentStatus.getStatus().getSummary())
                {
                    return true;
                }
            }
            else
            {
                return true;
            }
        }
        
        //went through the list, no need for a new status.
        return false;
    }
    
    /**
     * Sort out final status for the asset with respect to the given statuses of individual components.
     * @return
     *      the new status with the determined overall status based on the components' statuses
     */
    private Status getCompleteStatus() 
    { 
        final Set<SummaryStatusEnum> statSet = new HashSet<>();

        //iterate through the current component statuses and 'flip' the flag corresponding to the status found
        for (ComponentStatus compStatus : m_ComponentStatusMap.values())
        {
            statSet.add(compStatus.getStatus().getSummary());
        }
        //final status holder
        SummaryStatusEnum finalStatus = SummaryStatusEnum.UNKNOWN;
        if (statSet.contains(SummaryStatusEnum.UNKNOWN))
        {
            //any unknown makes the status unknown
            finalStatus = SummaryStatusEnum.UNKNOWN;
        }
        else if (statSet.contains(SummaryStatusEnum.BAD))
        {
            //any bad makes the status bad
            finalStatus = SummaryStatusEnum.BAD;
        }
        else if (statSet.contains(SummaryStatusEnum.DEGRADED))
        {
            //any degraded makes the status degraded
            finalStatus = SummaryStatusEnum.DEGRADED;
        }
        //An off component status should only
        else if (statSet.contains(SummaryStatusEnum.OFF) && !statSet.contains(SummaryStatusEnum.GOOD)) 
        { //set the overall status to off if there is not another status too.
            finalStatus = SummaryStatusEnum.OFF;
        }
        else
        {
            finalStatus = SummaryStatusEnum.GOOD;
        }
        //use the overall summary status to render the appropriate description for the status
        final String description = getDescriptionForOverallStatus(finalStatus);
        
        return new Status().withSummaryStatus(
                new OperatingStatus(finalStatus, description)).withComponentStatuses(m_ComponentStatusMap.values());
    }
    
    
    /**
     * Retrieve the description for the given status.
     * @param status
     *      the status for which to retrieve the description
     * @return
     *      the string description that applies for the given status
     */
    private String getDescriptionForOverallStatus(final SummaryStatusEnum status)
    {
        switch (status)
        {
            case BAD: 
                return "One or more supporting components are not functioning properly.";
            case DEGRADED:
                return "Not all supporting components are functioning as expected.";
            case GOOD:
                return "Novatel asset is processing data as expected.";
            case OFF:
                return "Novatel asset is not actively processing data.";
            case UNKNOWN:
                return "Not all components have a known status.";
            default:
                throw new IllegalArgumentException(String.format(
                        "Unknown SummaryStatusEnum %s. A summary description could not be found for this status.", 
                            status));
        }
    }
}
