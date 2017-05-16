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
package mil.dod.th.ose.utils;

import java.io.IOException;
import java.net.URLConnection;

/**
 * This service should be used when components must create {@link java.net.URLConnection}s.
 * 
 * @author allenchl
 */
public interface UrlService
{
    /**
     * Construct a URL from the given string, and get the {@link java.net.URLConnection}. 
     * @param urlString
     *      the string containing the information to create a valid URL
     * @return
     *      the URL connection created
     * @throws IOException
     *      if the data is not complete, contains illegal values, or the connection cannot be opened
     */
    URLConnection constructUrlConnection(String urlString) throws IOException;
}
