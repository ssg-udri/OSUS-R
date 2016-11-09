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
package mil.dod.th.ose.gui.webapp.utils.push;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;

/**
 * Service that retrieves the PushContext for Primefaces Push capability.
 * This class should never be invoked directly. Instead, use 
 * {@link PushChannelMessageManager}
 * 
 * @author nickmarcucci
 *
 */
@Startup
@Singleton
public class PushContextUtil
{
    /**
     * Method returns a push context to disseminate data to users.
     * @return
     *  the current push context
     */
    public PushContext getPushContext()
    {
        return PushContextFactory.getDefault().getPushContext();
    }
}
