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
package example.platform.controller;

import java.util.Map;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.core.controller.TerraHarvestControllerProxy;

/**
 * Just an example proxy implementation that has no customizations.
 * 
 * @author dhumeniuk
 *
 */
@Component
public class ExampleControllerProxy implements TerraHarvestControllerProxy
{
    @Override
    public String getVersion() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getBuildInfo() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setName(String name) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getId() throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setId(int id) throws UnsupportedOperationException
    {
        throw new UnsupportedOperationException();
    }
}
