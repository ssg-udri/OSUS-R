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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This service allows components access to ImageIO methods.
 */
public interface ImageIOService
{
    /**
     * Read a stream containing image information to return a buffered image.
     * 
     * @param stream
     *      The stream containing the image data.
     * @return
     *      A buffered image deciphered from the stream data.
     * @throws IOException 
     *      When the input stream contains invalid data.
     */
    BufferedImage read(InputStream stream) throws IOException;

    /**
     * Read an image file and return it as a buffered image.
     * 
     * @param file
     *      The file that represents an image.
     * @return
     *      A buffered image construct from the image file.
     * @throws IOException
     *      Thrown when the file is not a valid image.
     */
    BufferedImage read(File file) throws IOException;

    /**
     * Write an image to the specified output stream.
     * 
     * @param image
     *      The image to be written to the output stream.
     * @param format
     *      Format of the image to be written to the output stream.
     * @param outStream
     *      The output stream the image is to be written to.
     * @throws IOException
     *      Thrown if an error occurs writing the image to the output stream.
     */
    void write(RenderedImage image, String format, OutputStream outStream) throws IOException;
    
    /**
     * Write an image to the specified output stream.
     * 
     * @param image
     *      The image to be written to the output stream.
     * @param format
     *      Format of the image to be written to the output stream.
     * @param file
     *      The file the image is to be written to.
     * @throws IOException
     *      Thrown if an error occurs writing the image to the output stream.
     */
    void write(RenderedImage image, String format, File file) throws IOException;
}