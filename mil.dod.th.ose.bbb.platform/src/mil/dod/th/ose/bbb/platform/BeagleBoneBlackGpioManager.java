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
package mil.dod.th.ose.bbb.platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import mil.dod.th.ose.utils.FileService;

/**
 * Service used to manage the state of the BeagleBone Black's GPIO pins.
 * 
 * @author cweisenborn
 */
@Component(provide = BeagleBoneBlackGpioManager.class)
public class BeagleBoneBlackGpioManager
{
    private static final String GPIO_PREFIX = "gpio";
    private static final String GPIO_PATH = "/sys/class/" + GPIO_PREFIX;
    
    private FileService m_FileService;
    
    @Reference
    public void setFileService(final FileService fileService)
    {
        m_FileService = fileService;
    }
    
    /**
     * Method that sets the specified GPIO pin to either the {@link GpioState#HIGH} or {@link GpioState#LOW}.
     * 
     * @param pin
     *      The integer that represents the pin whose state should be set.
     * @param state
     *      The state the specified pin should be set to.
     * @throws IllegalStateException
     *      Thrown if the specified pin's state cannot be set.
     */
    public void setGpioState(final int pin, final GpioState state)
    {
        final String gpioStr = GPIO_PREFIX + pin;
        final File gpioValue = m_FileService.getFile(GPIO_PATH, gpioStr + "/value");
        
        try
        {
            if (!gpioValue.exists())
            {
                enablePinAsOutput(pin);
            }
            
            try (final FileOutputStream fos = m_FileService.createFileOutputStream(gpioValue, false);
                    final PrintStream printStream = m_FileService.createPrintStream(fos))
            {
                printStream.print(state.toValue());
            }
        }
        catch (final IOException ex)
        {
            throw new IllegalStateException(String.format("Cannot set GPIO [%s] to state [%s] as it does not exists.", 
                    pin, state), ex);
        }
    }
    
    /**
     * Method that enables and sets the specified GPIO pin as output.
     * 
     * @param pin
     *      The pin to be enabled and set as output.
     * @throws IOException
     *      Thrown if the pin cannot be enabled or setup to output.
     */
    private void enablePinAsOutput(final int pin) throws IOException
    {
        final File exportGpio = m_FileService.getFile(GPIO_PATH, "export");
        try (final FileOutputStream fos = m_FileService.createFileOutputStream(exportGpio, false);
                final PrintStream printStream = m_FileService.createPrintStream(fos))
        {
            printStream.print(pin);
        }
        
        final String gpioStr = GPIO_PREFIX + pin + "/direction";
        final File gpioDirection = m_FileService.getFile(GPIO_PATH, gpioStr);
        try (final FileOutputStream fos = m_FileService.createFileOutputStream(gpioDirection, false);
                final PrintStream printStream = m_FileService.createPrintStream(fos))
        {
            printStream.print("out");
        }
    }

    /**
     * Enumeration used to represent the output state of a GPIO pin.
     */
    public enum GpioState
    {
        /**
         * Enum that represents the pin high state.
         */
        HIGH(1),
        
        /**
         * Enum that represents the pin low state.
         */
        LOW(0);
        
        private int m_State;
        
        /**
         * Private constructor used to set the integer value of the enumeration.
         * 
         * @param state
         *      The integer value the state enumeration represents.
         */
        GpioState(final int state)
        {
            m_State = state;
        }
        
        /**
         * Methods that returns the integer representation of the state enumeration.
         * 
         * @return
         *      Integer value that the state enumeration represents.
         */
        public int toValue()
        {
            return m_State;
        }
    }
}
