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
//    Common message type constants. These constants identify the type of data
//    that has been passed over the socket.
//==============================================================================

/*
 * GLOBALS
 *  All variables in this file are considered to be global variables because they
 *  are being assigned to the window object for the page.
 */
//thType prototype, all THOSE push-message identifiers should be properties of this.
window.thType || (window.thType = {});

//GROWL MESSAGE TYPE
window.thType.growlMessageType = "GROWL_MESSAGE";

//EVENT TOPIC MESSAGE TYPE
window.thType.eventMessageType = "EVENT";