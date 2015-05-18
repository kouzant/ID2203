package se.kth.ict.id2203.sim;

import java.util.*;
import se.sics.kompics.*;

public class SimScheduler extends Scheduler
{
    public SimUniverse universe;
    private LinkedList<Component> readyComponents;
    
    public SimScheduler() {
        this.readyComponents = new LinkedList<Component>();
    }
    
    public void schedule(final Component c, final int w) {
        this.readyComponents.add(c);
    }
    
    public void proceed() {
        while (true) {
            if (this.readyComponents.isEmpty()) {
                if (!this.universe.advanceTime()) {
                    break;
                }
                continue;
            }
            else {
                final Component component = this.readyComponents.poll();
                this.executeComponent(component, 0);
            }
        }
    }
    
    public void shutdown() {
    }
    
    public void asyncShutdown() {
    }
}
