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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import aQute.bnd.annotation.component.Component;
import mil.dod.th.ose.utils.ImageIOService;

/**
 * Implementation of {@link ImageIOService}.
 */
@Component
public class ImageIOServiceImpl implements ImageIOService
{
    @Override
    public BufferedImage read(final InputStream stream) throws IOException
    {
        return ImageIO.read(stream);
    }
    
    @Override
    public BufferedImage read(final File file) throws IOException
    {
        return ImageIO.read(file);
    }
    
    @Override
    public void write(final RenderedImage image, final String format, final OutputStream outStream) throws IOException
    {
        ImageIO.write(image, format, outStream);
    }

    @Override
    public void write(final RenderedImage image, final String format, final File file) throws IOException
    {
        ImageIO.write(image, format, file);
    }
}
