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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import mil.dod.th.core.types.DigitalMedia;

import org.junit.Test;

/**
 * Test class to test the ProjectBaseCapabilities class.
 * 
 * @author m.elmo
 * 
 */
public class TestProjectBaseCapabilities
{
    /**
     * Tests that {@link ProjectBaseCapabilities#makePrimaryImage()} returns
     * {@link ProjectBaseCapabilities#makeDigitalMedia(String)}.
     */
    @Test
    public void testMakePrimaryImage()
    {
        assertThat(ProjectBaseCapabilities.makePrimaryImage(),
                is(ProjectBaseCapabilities.makeDigitalMedia(ProjectBaseCapabilities.MIME_IMAGE)));
    }

    /**
     * Tests that {@link ProjectBaseCapabilities#makeSecondaryImages()} returns a list containing
     * {@link ProjectBaseCapabilities#makeDigitalMedia(String)} and
     * {@link ProjectBaseCapabilities#makeDigitalMedia(String)}.
     */
    @Test
    public void testMakeSecondaryImages()
    {
        assertThat(
                ProjectBaseCapabilities.makeSecondaryImages(),
                hasItems(ProjectBaseCapabilities.makeDigitalMedia(ProjectBaseCapabilities.MIME_IMAGE),
                        ProjectBaseCapabilities.makeDigitalMedia(ProjectBaseCapabilities.MIME_UNKNOWN)));
    }

    /**
     * Tests that {@link ProjectBaseCapabilities#makeDigitalMedia(String)} returns a new DigitalMedia object that has
     * the correct byte array and encoding.
     */
    @Test
    public void testMakeDigitalMedia()
    {
        assertThat(ProjectBaseCapabilities.makeDigitalMedia("hello"),
                is(new DigitalMedia(new byte[] {0, 1, 2}, "hello")));
    }

    /**
     * Tests that {@link ProjectBaseCapabilities#getProductName()} returns the string "Enter product name here".
     */
    @Test
    public void testGetProductName()
    {
        assertThat(ProjectBaseCapabilities.getProductName(), is("Enter product name here"));
    }

    /**
     * Tests that {@link ProjectBaseCapabilities#getDescription()} returns the string "Enter product description here".
     */
    @Test
    public void testGetDescription()
    {
        assertThat(ProjectBaseCapabilities.getDescription(), is("Enter product description here"));
    }

    /**
     * Tests that {@link ProjectBaseCapabilities#getManufacturer()} returns the string
     * "Enter product manufacturer here if known".
     */
    @Test
    public void testGetManufacturer()
    {
        assertThat(ProjectBaseCapabilities.getManufacturer(), is("Enter product manufacturer here if known"));
    }
}
