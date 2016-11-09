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
package mil.dod.th.ose.remote.observation;

import aQute.bnd.annotation.metatype.Meta;

/**
 * This interface assists with configuration of the {@link RemoteObservationListener}.
 * @author callen
 *
 */
public interface RemoteObservationListenerConfig 
{
   /**
    * Property for representing whether the remote observation store will listen for observations from remote
    * systems.
    * 
    * @return
    *    flag value representing if the remote observation store is enabled
    */
    @Meta.AD(required = false,
        description = "If true the service is enabled, else the service is disabled.")
    boolean enabled();
}
