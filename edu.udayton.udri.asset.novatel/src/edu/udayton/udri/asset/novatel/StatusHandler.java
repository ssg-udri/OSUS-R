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

import mil.dod.th.core.types.status.ComponentStatus;

/**
 * This interface describes how components can update their status with their parent {@link NovatelAsset}.
 * @author allenchl
 *
 */
public interface StatusHandler
{

    /**
     * Handle updating the status of a component belonging to a 
     * {@link edu.udayton.udri.asset.novatel.NovatelAsset}.
     * @param status
     *      the new status for one of the asset's components
     */
    void handleStatusUpdate(ComponentStatus status);

}
