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
package example.platform.power;

import java.io.Serializable;

/**
 * Stores relevant method parameter values for {@link ExamplePlatformPowerManager} method calls.
 * 
 * @author jlatham
 */
public class ExamplePowerManagerMethodLog implements Serializable
{
    private final static long serialVersionUID = 1L;
    
    private String wakeLockId;
    
    private long startTimeMs;
    
    private long endTimeMs;
    
    private MethodCalled calledMethod;
    
    /** Sets which method was called for logging purposes */
    public enum MethodCalled
    {
        Activate,
        Cancel
    }
    
    public void setWakeLockId(String id)
    {
        this.wakeLockId = id;
    }
    
    public void setStartTimeMs(long startTime)
    {
        this.startTimeMs = startTime;
    }
    
    public void setEndtimeMs(long endTime)
    {
        this.endTimeMs = endTime;
    }
    
    public void setCalledMethod(MethodCalled name)
    {
        this.calledMethod = name;
    }
    
    public String getWakeLockId()
    {
        return this.wakeLockId;
    }
    
    public long getStartTimeMs()
    {
        return this.startTimeMs;
    }
    
    public long getEndTimeMs()
    {
        return this.endTimeMs;
    }
    
    public MethodCalled getCalledMethod()
    {
        return this.calledMethod;
    }
}
