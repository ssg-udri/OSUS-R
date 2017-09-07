#!/usr/bin/env bash
#==============================================================================
# This software is part of the Open Standard for Unattended Sensors (OSUS)
# reference implementation (OSUS-R).
#
# To the extent possible under law, the author(s) have dedicated all copyright
# and related and neighboring rights to this software to the public domain
# worldwide. This software is distributed without any warranty.
#
# You should have received a copy of the CC0 Public Domain Dedication along
# with this software. If not, see
# <http://creativecommons.org/publicdomain/zero/1.0/>.
#==============================================================================

#
# Notes:
# This script will generate C++ code in the _gen/cpp directory. Copy the Makefile.example file
# to _gen/cpp/Makefile and run "make" in that directory to build a shared C++ library.
#

# Change this path to the directory where protobufs 2.6.1 is installed
PROTOC_2_6_1=/usr/local/protoc/2.6.1

set -e
mkdir -p ./_gen/cpp

for f in `find . -name "*.proto"`
do
    echo "Generate code for $f..."
    $PROTOC_2_6_1/bin/protoc --cpp_out=./_gen/cpp -I=. $f
done
