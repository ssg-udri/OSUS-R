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
/**
 * Most of the utility classes within this package are (javax.inject.Singleton)s. Singletons are used instead of 
 * OSGi declarative service components because this bundle is unable to provide declarative service components without 
 * a race condition happening at startup. The race condition happens when the service components cannot be registered 
 * before the managed beans request the services. 
 */
package mil.dod.th.ose.gui.webapp.utils;
