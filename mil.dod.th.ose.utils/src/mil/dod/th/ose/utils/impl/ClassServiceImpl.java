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

import java.net.URL;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.ose.utils.ClassService;

/**
 * Implements {@link ClassService}.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class ClassServiceImpl implements ClassService
{
    @Override
    public URL getResource(final Class<?> clazz, final String name)
    {
        return clazz.getResource(name);
    }

    @Override
    public ClassLoader getClassLoader(final Class<?> clazz)
    {
        return clazz.getClassLoader();
    }
}
