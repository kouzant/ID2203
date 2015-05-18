package se.kth.ict.id2203.sim;

import se.sics.kompics.address.*;
import se.sics.kompics.*;

public abstract class SimProcess extends ComponentDefinition
{
    public boolean crashed;
    public boolean pausedNetwork;
    public Address address;
    public int pid;
    
    public SimProcess() {
        this.crashed = false;
        this.pausedNetwork = false;
    }
    
    public abstract void triggerTimerEvent(final Event p0);
    
    public abstract void triggerPp2pEvent(final Event p0);
    
    public abstract void triggerApplicationEvent(final Event p0);
}
