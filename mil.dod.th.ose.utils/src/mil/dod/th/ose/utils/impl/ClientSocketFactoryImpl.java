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
package mil.dod.th.ose.utils.impl;

import java.io.IOException;
import java.net.Socket;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.ose.utils.ClientSocketFactory;


/**
 * Implementation of the {@link ClientSocketFactory}.
 * @author callen
 *
 */
@Component
public class ClientSocketFactoryImpl implements ClientSocketFactory 
{
    @Override
    public Socket createClientSocket(final String host, final int port) throws IOException
    {
        return new Socket(host, port);
    }
}
