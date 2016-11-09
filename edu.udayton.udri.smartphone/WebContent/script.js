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

var sensorCheck = false;
var webSocket;

var gpsCheck = false;
var gpsTimeUpdateCheck = false;
var gpsTimeUpdateMinutes = 1;
var gpsTimedLoopInterval;
var gpsLastTimeUpdate = 1;
var gpsPositionUpdateCheck = false;
var gpsPositionUpdateMeters = 1;
var gpsWatch;

var batteryCheck = false;
var batteryTimeUpdateCheck = false;
var batteryTimeUpdateMinutes = 1;
var batteryTimedLoopInterval;
var batteryStatusUpdateCheck = false;
var batteryStatusUpdatePercent = 20;
var batteryStatusListener;

/////////////////////////////////////////////////////////
//Functions to perform when a switch has changed state.// 
/////////////////////////////////////////////////////////

/**
 * Function to be performed when the sensor switch has changed state. It either creates the websocket and connects to 
 * the websocket server or disconnects from the websocket server, and then determines whether or not any data retrieval
 * methods should be started.
 */
function setSensorCheck()
{
    sensorCheck = document.getElementById("sensorSwitch").checked;
    
    if (sensorCheck)
    {
        webSocket = new WebSocket("ws://" + location.hostname + ":9090");
        
        setGpsTimeUpdateCheck();
        setGpsPositionUpdateCheck();
        setBatteryTimeUpdateCheck();
        setBatteryStatusUpdateCheck();
    }
    else
    {
        webSocket.close();
    }
}

/**
 * Function to be performed when the GPS switch has changed state. It updates the GPS switch value and checks the 
 * value of the GPS sub-switches to determine whether or not the data retrieval methods should be started.
 */
function setGpsCheck()
{
    gpsCheck = document.getElementById("gpsSwitch").checked;
    
    setGpsTimeUpdateCheck();
    setGpsPositionUpdateCheck();
}

/**
 * Function to be performed when the battery switch has changed state. It updates the battery switch value and checks
 * the value of the battery sub-switches to determine whether or not the data retrieval methods should be started.
 */
function setBatteryCheck()
{
    batteryCheck = document.getElementById("batterySwitch").checked;

    setBatteryTimeUpdateCheck();
    setBatteryStatusUpdateCheck();
}

/**
 * Function to be performed when the GPS timed update switch has changed state. Determines if the required switches
 * are set to true and will either start or stop retrieving GPS data accordingly. This method retrieves the GPS
 * location based on a time interval.
 */
function setGpsTimeUpdateCheck()
{
    gpsTimeUpdateCheck = document.getElementById("gpsUpdateTimeBox").checked;
    
    if (gpsTimeUpdateCheck && gpsCheck && sensorCheck)
    {
        getGpsCoords();
        gpsTimedLoopInterval = setInterval(getGpsCoords, gpsTimeUpdateMinutes * 60000);
    }
    else
    {
        clearInterval(gpsTimedLoopInterval);
    }
}

/**
 * Function to be performed when the GPS position-based update switch has changed state. Determines if the required
 * switches are set to true and will either start or stop retrieving GPS data accordingly. This method retrieves GPS
 * location based on the change in position.
 */
function setGpsPositionUpdateCheck()
{
    gpsPositionUpdateCheck = document.getElementById("gpsUpdateChangeBox").checked;
        
    if (gpsPositionUpdateCheck && gpsCheck && sensorCheck)
    {
        if(navigator.geolocation)
        {    
            var lastPos;
            gpsWatch = navigator.geolocation.watchPosition(function(pos) {
                if (typeof lastPos === 'undefined' || getPositionDelta(pos, lastPos) > gpsPositionUpdateMeters)
                {
                    lastPos = pos;
                    
                    webSocket.send(JSON.stringify({
                        "dataType": "coordinates", 
                        "latitude": pos.coords.latitude, 
                        "longitude": pos.coords.longitude, 
                        "timestamp": Date.now()}));
                }
            });
        }
    }
    else
    {
        navigator.geolocation.clearWatch(gpsWatch);
    }
}

/**
 * Function to be performed when the battery timed update switch has changed state. Determines if the required switches
 * are set to true and will either start or stop retrieving battery data accordingly. This method retrieves battery
 * status based on a time interval.
 */
function setBatteryTimeUpdateCheck()
{
    batteryTimeUpdateCheck = document.getElementById("batteryTimeUpdateBox").checked;
        
    if (batteryTimeUpdateCheck && batteryCheck && sensorCheck)
    {
        getBatteryLevel();
        batteryTimedLoopInterval = setInterval(getBatteryLevel, batteryTimeUpdateMinutes * 60000)
    }
    else
    {
        clearInterval(batteryTimedLoopInterval);
    }
}

/**
 * Function to be performed when the battery status-based update switch has changed state. Determines if the required 
 * switches are set to true and will either start or stop retrieving battery data accordingly. This method retrieves
 * battery status when the phone's battery level has dropped below the specified threshold.
 */
function setBatteryStatusUpdateCheck()
{
    batteryStatusUpdateCheck = document.getElementById("batteryStatusUpdateBox").checked;
    
    if (batteryStatusUpdateCheck && batteryCheck && sensorCheck)
    {
        navigator.getBattery().then(function(battery)
        {
            batteryStatusListener = battery;
            
            batteryStatusListener.onlevelchange = batteryListener;
        });
    }
    else if (batteryStatusListener != undefined)
    {
        
        batteryStatusListener.onlevelchange = null;
    }
}

/////////////////////////////////////////////////////////////////////////////////
//Functions to perform when specific settings have changed i.e. time intervals.//
/////////////////////////////////////////////////////////////////////////////////

/**
 * Update the minutes setting of the GPS timed update.
 */
function setGpsTimeUpdateMinutes()
{
    var newVal = parseInt(document.getElementById("gpsTimeUpdateMinutes").value);
    
    if (newVal > 0)
    {
        gpsTimeUpdateMinutes = newVal;
    }
}

/**
 * Update the change in meters setting of the GPS position-change update.
 */
function setGpsPositionUpdateMeters()
{
    var newVal = parseInt(document.getElementById("gpsPositionUpdateMeters").value);
    
    if (newVal > 0)
    {
        gpsPositionUpdateMeters = newVal;
    }
}

/**
 * Update the minutes setting of the battery timed update.
 */
function setBatteryTimeUpdateMinutes()
{
    var newVal = parseInt(document.getElementById("batteryTimeUpdateMinutes").value);
    
    if (newVal > 0)
    {
        batteryTimeUpdateMinutes = newVal;
    }
}

/**
 * Update the percentage setting of the battery status-based update.
 */
function setBatteryStatusUpdatePercent()
{
    var newVal = parseInt(document.getElementById("batteryStatusUpdatePercent").value);
    if (newVal > 0)
    {
        batteryStatusUpdatePercent = newVal;
    }
}

/////////////////////////////////////////////////
//Functions for retrieving data from the phone.//
/////////////////////////////////////////////////

/**
 * Returns the GPS coordinates of the phone.
 */
function getGpsCoords()
{
    if (navigator.geolocation) 
    {
        navigator.geolocation.getCurrentPosition(function (pos) {
            webSocket.send(JSON.stringify({
                "dataType": "coordinates", 
                "latitude": pos.coords.latitude, 
                "longitude": pos.coords.longitude, 
                "timestamp": Date.now()}));
        });
    }
    else
    {
        console.log("Geolocation is not supported or was not given permission!");
    }
}

/**
 * Returns the battery level of the phone.
 */
function getBatteryLevel()
{
    
    navigator.getBattery().then(function(battery) 
    {
        webSocket.send(JSON.stringify({
            "dataType": "batteryStatus",
            "batteryLevel": battery.level,
            "timestamp": Date.now()}));
    });
}

/**
 * Listens for changes in the battery level of the phone and returns it when the current level is lower than the
 * specified threshold.
 */
function batteryListener()
{
    if (batteryStatusListener.level * 100 < batteryStatusUpdatePercent)
    {
        webSocket.send(JSON.stringify({
            "dataType": "batteryStatus",
            "batteryLevel": batteryStatusListener.level,
            "timestamp": Date.now()}));
    }
}

///////////////////
//Math functions.//
///////////////////

/**
 * Determines the change in meters by using the previous returned location and the current location. This algebra
 * comes from the Great-circle distance.
 */
function getPositionDelta(newPos, lastPos)
{
    var R = 6378.137;
    var dLat = (newPos.coords.latitude - lastPos.coords.latitude) * Math.PI / 180;
    var dLon = (newPos.coords.longitude - lastPos.coords.longitude) * Math.PI / 180;
    var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lastPos.coords.latitude * Math.PI / 180) * Math.cos(newPos.coords.latitude * Math.PI / 180) *
    Math.sin(dLon/2) * Math.sin(dLon/2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    var d = R * c;
    return d * 1000;
}
