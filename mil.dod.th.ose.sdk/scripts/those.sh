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
# DESCRIPTION:
#   Script to start the SDK command line tool in a Linux environment.
#
#==============================================================================

if [ `type -p java` ]
then
    PROGRAM="java"
else
    if [ -e $JAVA_HOME/bin/java ]
    then
        PROGRAM="$JAVA_HOME/bin/java"
    else
        echo "Cannot find java, add to PATH or define JAVA_HOME env variable."
        exit 1
    fi
fi

SDKDIR=`dirname $0`

# Execute the application
$PROGRAM -cp "$SDKDIR/lib/mil.dod.th.ose.sdk.jar":\
"$SDKDIR/api/mil.dod.th.core.api.jar":\
"$SDKDIR/lib/biz.aQute.bndlib-3.1.0.jar" \
mil.dod.th.ose.sdk.those.ThoseMain $@ 

