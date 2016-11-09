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
package mil.dod.th.ose.test;

import java.util.concurrent.Callable;

/**
 * Callable that throws an exception
 * 
 * @author cweisenborn
 */
public class ExceptionThrower implements Callable<String>
{
    /**
     * Exception to throw.
     */
    private Exception m_Throwable;

    /**
     * Construct a new Exception Thrower
     * @param throwable
     *     the object to throw
     */
    public ExceptionThrower(final Exception throwable)
    {
        m_Throwable = throwable;
    }
    
    @Override
    public String call() throws Exception
    {
        throw m_Throwable;
    }
}
