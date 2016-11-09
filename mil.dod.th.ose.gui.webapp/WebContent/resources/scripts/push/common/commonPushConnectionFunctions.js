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
//    Common push connection functions to be used by all scripts which perform 
//    PrimeFaces push updates to specific pages. 
//==============================================================================

/**
 * Function to decrement the request count.
 * It is a work around an issue with long-polling in Atmosphere.   
 * Each poll is treated as a separate connection. And in fact it is. And 
 * there is a requests counter that when it reaches certain value, will stop 
 * further requests.
 * 
 * @param socket
 *  the PrimeFaces push socket that needs the request count decremented.
 */
function decrementRequestCount(socket)
{
    /*this section is to avoid the problem of having the maximum
    number of requests being reached.
    The last bit is a work around an issue with long-polling in Atmosphere.   
    Each poll is treated as a separate connection. And in fact it is. And 
    there is a requests counter that when it reaches certain value, will stop 
    further requests. So if you receive a message and the status is 200 - that 
    magically means you successfully received a message, you decrease the 
    counter by 1 in order to prevent disconnects. 
    */ 
    if(socket.connection.response == 200)
    {
        socket.connection.response.request.requestCount--;
    }
    
    return;
}

