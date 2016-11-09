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

import com.google.inject.AbstractModule;

import mil.dod.th.ose.utils.impl.ClassServiceImpl;
import mil.dod.th.ose.utils.impl.ClientSocketFactoryImpl;
import mil.dod.th.ose.utils.impl.FileServiceImpl;
import mil.dod.th.ose.utils.impl.ImageIOServiceImpl;
import mil.dod.th.ose.utils.impl.PropertyRetrieverImpl;
import mil.dod.th.ose.utils.impl.ServerSocketFactoryImpl;
import mil.dod.th.ose.utils.impl.UrlServiceImpl;

/**
 * Guice injection module for non-OSGi code to use dependency injection.
 * 
 * @author dhumeniuk
 *
 */
public class UtilInjectionModule extends AbstractModule
{
    @Override
    protected void configure()
    {
        bind(ClassService.class).to(ClassServiceImpl.class);
        bind(ClientSocketFactory.class).to(ClientSocketFactoryImpl.class);
        bind(FileService.class).to(FileServiceImpl.class);
        bind(ImageIOService.class).to(ImageIOServiceImpl.class);
        bind(PropertyRetriever.class).to(PropertyRetrieverImpl.class);
        bind(ServerSocketFactory.class).to(ServerSocketFactoryImpl.class);
        bind(UrlService.class).to(UrlServiceImpl.class);
    }
}
