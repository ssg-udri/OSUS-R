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
// SWIG interface file to generate the FileOperations class.  Contains GNU C  
// ported functions focused on file operations.
//
//==============================================================================

%module FileOperations
%{
#include <sys/types.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <unistd.h>

#include "Types.h"
#include "FileOperations.h"
%}

%include "arrays_java.i"
%include "typemaps.i"

%include "Types.h"
%include "FileOperations.h"

// these functions are defined by the GNU C library, exact types my be slightly 
// different but should be compatible (int vs. ssize_t, uint8[] vs void*)
int open(const char* pathname, int flags);
int close(int fd);

%constant int O_CREAT = O_CREAT;
%constant int O_RDWR = O_RDWR;
%constant int O_RDONLY = O_RDONLY;
%constant int O_WRONLY = O_WRONLY;
%constant int O_NOCTTY = O_NOCTTY;