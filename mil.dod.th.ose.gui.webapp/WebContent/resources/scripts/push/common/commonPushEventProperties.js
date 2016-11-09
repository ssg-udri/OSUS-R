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
//    Common variables to be available/used by scripts which implement push 
//    capabilities.
//==============================================================================

/*
 * GLOBALS
 *  All variables in this file are considered to be global variables because they
 *  are being assigned to the window object for the page.
 */

window.thEventProps || (window.thEventProps = {});

//EVENT_PROP_CONTROLLER_ID
window.thEventProps.ControllerId = "controller.id";

//ASSET_UUID
window.thEventProps.AssetUuid = "asset.uuid";