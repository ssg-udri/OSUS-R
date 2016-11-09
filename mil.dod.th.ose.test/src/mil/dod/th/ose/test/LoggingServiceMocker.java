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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Date;

import mil.dod.th.core.log.LoggingService;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.service.log.LogService;

/**
 * @author dhumeniuk
 *
 */
public class LoggingServiceMocker
{
    public static LoggingService createMock()
    {
        final LoggingService loggingService = mock(LoggingService.class);
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final int level = (Integer)args[0];
                final String formatStr = (String)args[1];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 2, args.length);
                String msg = formatStr;
                if (msg_args.length != 0)
                {
                    msg = String.format(formatStr, msg_args);
                }
                 
                System.err.println(fromLogEntry(level, msg, null));
                return null;
            }
        }).when(loggingService).log(anyInt(), anyString(), anyVararg());
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final int level = (Integer)args[0];
                final Throwable cause = (Throwable)args[1];
                final String formatStr = (String)args[2];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 3, args.length);
                String msg = formatStr;
                if (msg_args.length != 0)
                {
                    msg = String.format(formatStr, msg_args);
                }
                System.err.println(fromLogEntry(level, msg, cause));
                return null;
            }

        }).when(loggingService).log(anyInt(), (Throwable)anyObject(), anyString(), anyVararg());
        
        // debug
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final String formatStr = (String)args[0];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 1, args.length);
                final String msg = String.format(formatStr, msg_args);
                loggingService.log(LogService.LOG_DEBUG, msg);
                return null;
            }
        }).when(loggingService).debug(anyString(), anyVararg());
        
        // info
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final String formatStr = (String)args[0];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 1, args.length);
                final String msg = String.format(formatStr, msg_args);
                loggingService.log(LogService.LOG_INFO, msg);
                return null;
            }
        }).when(loggingService).info(anyString(), anyVararg());
        
        // warning
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final String formatStr = (String)args[0];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 1, args.length);
                final String msg = String.format(formatStr, msg_args);
                loggingService.log(LogService.LOG_WARNING, msg);
                return null;
            }
        }).when(loggingService).warning(anyString(), anyVararg());
        
        // error
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final String formatStr = (String)args[0];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 1, args.length);
                final String msg = String.format(formatStr, msg_args);
                loggingService.log(LogService.LOG_ERROR, msg);
                return null;
            }
        }).when(loggingService).error(anyString(), anyVararg());
        
        // error + throwable
        doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                Object[] args = invocation.getArguments();
                final Throwable cause = (Throwable)args[0];
                final String formatStr = (String)args[1];
                // get varargs from log method call and create message string
                final Object[] msg_args = Arrays.copyOfRange(args, 2, args.length);
                final String msg = String.format(formatStr, msg_args);
                loggingService.log(LogService.LOG_ERROR, cause, msg);
                return null;
            }
        }).when(loggingService).error((Throwable)anyObject(), anyString(), anyVararg());
        return loggingService;
    }
    
    private static String fromLogEntry(int level, String msg, Throwable cause)
    {
        String logLevel;
        switch (level)
        {
            case LogService.LOG_DEBUG:
                logLevel = "DEBUG";
                break;
            case LogService.LOG_INFO:
                logLevel = "INFO ";
                break;
            case LogService.LOG_WARNING:
                logLevel = "WARN ";
                break;
            case LogService.LOG_ERROR:
                logLevel = "ERROR";
                break;
            default:
                throw new IllegalArgumentException(String.format("Level %d is not a valid log level", level));
        }
        
        if (cause == null)
        {
            return String.format("%s %s %s", new Date(), logLevel, msg);
        }
        else
        {
            ByteArrayOutputStream array = new ByteArrayOutputStream();
            PrintStream stream = new PrintStream(array);
            cause.printStackTrace(stream);
            return String.format("%s %s %s%n%s", new Date(), logLevel, msg, array.toString());
        }
    }
}
