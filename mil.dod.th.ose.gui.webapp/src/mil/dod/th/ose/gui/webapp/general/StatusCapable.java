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
package mil.dod.th.ose.gui.webapp.general;

import mil.dod.th.core.types.status.SummaryStatusEnum;

/**
 * This interface should be implemented by items which can possess a status.
 * @author allenchl
 *
 */
public interface StatusCapable
{
    /**
     * Get the status of the object.
     * @return
     *    the status of the object
     */
    SummaryStatusEnum getSummaryStatus();
}
