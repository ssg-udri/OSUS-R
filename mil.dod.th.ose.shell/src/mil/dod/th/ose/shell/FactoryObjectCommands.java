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
package mil.dod.th.ose.shell;

import java.io.PrintStream;
import java.util.Set;

import aQute.bnd.annotation.component.Component;

import mil.dod.th.core.factory.FactoryObject;

import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;

/**
 * General commands for all types of {@link FactoryObject}'s.
 * 
 * @author dlandoll
 */
@Component(provide = FactoryObjectCommands.class, properties = { "osgi.command.scope=thobj",
    "osgi.command.function=getExtension|getExtensions" })
public class FactoryObjectCommands
{
    /**
     * Get the {@link mil.dod.th.core.factory.Extension} object from the given factory object.
     * 
     * @param factoryObject
     *      Factory object
     * @param extensionTypeName
     *      {@link mil.dod.th.core.factory.Extension} type string in fully qualified class name format
     * @return
     *      {@link mil.dod.th.core.factory.Extension} object
     */
    @Descriptor("Returns the extension for the factory object")
    public Object getExtension(
            @Descriptor("Factory object")
            final FactoryObject factoryObject,
            @Descriptor("Unique extension type string in fully qualified class name format.")
            final String extensionTypeName)
    {
        final Set<Class<?>> extTypes = factoryObject.getExtensionTypes();
        for (Class<?> extType : extTypes)
        {
            if (extensionTypeName.equals(extType.getName()))
            {
                return factoryObject.getExtension(extType);
            }
        }

        throw new IllegalArgumentException(String.format("Extension type [%s] does not exist", extensionTypeName));
    }

    /**
     * Display all {@link mil.dod.th.core.factory.Extension} types provided by the factory object.
     * 
     * @param session
     *      Provides access to the Gogo shell session
     * @param object
     *      Factory object
     */
    @Descriptor("Displays all extension types provided by the factory object")
    public void getExtensions(
            final CommandSession session,
            @Descriptor("Factory object")
            final FactoryObject object)
    {
        final PrintStream out = session.getConsole();

        out.println(String.format("%s Extensions:", object.getName()));

        final Set<Class<?>> extTypes = object.getExtensionTypes();
        if (extTypes.isEmpty())
        {
            out.println("None");
        }
        else
        {
            for (Class<?> extType : extTypes)
            {
                out.println(extType.getName());
            }
        }
    }
}
