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

import static org.mockito.Mockito.*;

import mil.dod.th.core.controller.TerraHarvestController;
import mil.dod.th.core.controller.TerraHarvestController.OperationMode;

/**
 * Mocker utility for {@link TerraHarvestController}
 * @author nickmarcucci
 *
 */
public class TerraHarvestControllerMocker
{
    public static TerraHarvestController mockIt(Integer systemId, OperationMode mode, 
            String name)
    {
        TerraHarvestController mockController = mock(TerraHarvestController.class);
        
        when(mockController.getId()).thenReturn(systemId);
        when(mockController.getOperationMode()).thenReturn(mode);
        when(mockController.getName()).thenReturn(name);
        
        return mockController;
    }
}
