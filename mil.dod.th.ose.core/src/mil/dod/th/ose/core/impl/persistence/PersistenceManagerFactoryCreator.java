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
package mil.dod.th.ose.core.impl.persistence;

import javax.jdo.PersistenceManagerFactory;

/**
 * Interface class for creating persistence manager factories.
 * 
 * @author jconn
 */
public interface PersistenceManagerFactoryCreator
{
    /**
     * Creates a Persistence Manager Factory.
     * 
     * @param extentClass
     *          core class that this manager will persist
     * @param url
     *          connection URL to use for persisting
     * @return a new PersistenceManagerFactory instance for the given properties
     */
    PersistenceManagerFactory createPersistenceManagerFactory(Class<?> extentClass, String url);

}
