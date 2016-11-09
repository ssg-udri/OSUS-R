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
package mil.dod.th.ose.archiver.vlc;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the methods of the helper class {@link mil.dod.th.ose.archiver.vlc.VlcUtils}.
 * 
 * @author jmiller
 *
 */
public class TestVlcUtils
{
    
    private String m_ArchiveFile;
    
    @Before
    public void setUp()
    {
        m_ArchiveFile = "top" + File.separator + "level" + File.separator + "dir" + File.separator + 
                "profileId" + File.separator + "timestamp";
    }
    
    @Test
    public void testCreateOptionsString()
    {        
        assertThat(VlcUtils.createOptionsString(m_ArchiveFile), is(":sout=file/ts:top" + File.separator + "level" +
                File.separator + "dir" + File.separator + "profileId" + File.separator + "timestamp"));
    }

}
