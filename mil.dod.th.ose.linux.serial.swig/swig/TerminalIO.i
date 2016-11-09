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
//
// DESCRIPTION:
// SWIG interface file to generate the TerminalIO class.  Contains GNU C ported 
// functions focused on terminal I/O operations.
//
//==============================================================================

%module TerminalIO
%{
#include <sys/ioctl.h>
#include "TerminalIO.h"
%}

%include "arrays_java.i"
%include "typemaps.i"

%apply int *OUTPUT { int *value };
%include "TerminalIO.h"

%inline
{
typedef unsigned char   cc_t;
typedef unsigned int    tcflag_t;
}

// these constants are only a subset, see system include termios.h
%constant int NCCS = NCCS;
%constant int CLOCAL = CLOCAL;
%constant int CREAD = CREAD;
%constant int CS7 = CS7;
%constant int CS8 = CS8;
%constant int CSTOPB = CSTOPB;
%constant int PARENB = PARENB;
%constant int PARODD = PARODD;
%constant int CRTSCTS = CRTSCTS;
%constant int IXON = IXON;
%constant int IXOFF = IXOFF;
%constant int VMIN = VMIN;

// baud rate constants
%constant int B1200 = B1200; 
%constant int B1800 = B1800;
%constant int B2400 = B2400;
%constant int B4800 = B4800;
%constant int B9600 = B9600;
%constant int B19200 = B19200;
%constant int B38400 = B38400;
%constant int B57600 = B57600;
%constant int B115200 = B115200;
%constant int B230400 = B230400; 
%constant int B460800 = B460800;
%constant int B500000 = B500000;
%constant int B576000 = B576000;
%constant int B921600 = B921600;
%constant int B1000000 = B1000000;
%constant int B1152000 = B1152000;
%constant int B1500000 = B1500000;

// constants for working with individual modem signals
%constant int TIOCMBIC = TIOCMBIC;
%constant int TIOCMBIS = TIOCMBIS;
%constant int TIOCM_DTR = TIOCM_DTR;
