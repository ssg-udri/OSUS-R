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
import java.net.URL;
import java.net.URLConnection;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.ose.utils.CoverageIgnore;
import mil.dod.th.ose.utils.UrlService;

/**
 * Implementation of the {@link UrlService}.
 * @author allenchl
 *
 */
@Component
public class UrlServiceImpl implements UrlService
{
    @Override
    @CoverageIgnore
    public URLConnection constructUrlConnection(final String urlString) throws IOException
    {
        final URL url = new URL(urlString);
        return url.openConnection();
    }
}
