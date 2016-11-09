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
package mil.dod.th.ose.bbb.platform;

/**
 * Interface used to define a listener that gets called when a wake lock is activated.
 * 
 * @author cweisenborn
 */
public interface BeagleBoneBlackLockListener
{
    
    /**
     * Method that gets called when an active wake lock is added.
     */
    void activeWakeLockAdded();
}
