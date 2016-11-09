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
package mil.dod.th.ose.sdk.those;

import java.io.File;
import java.io.IOException;

import aQute.bnd.osgi.Jar;

/**
 * Basic Bnd library operations.
 * 
 * @author dhumeniuk
 *
 */
public class BndService
{
    /**
     * Instantiate a new {@link Jar} using {@link Jar#Jar(java.io.File)}.
     * 
     * @param file
     *      file to pass to constructor
     * @return
     *      created Jar object
     * @throws IOException
     *      if instantiation fails
     */
    Jar newJar(final File file) throws IOException
    {
        return new Jar(file);
    }
}
