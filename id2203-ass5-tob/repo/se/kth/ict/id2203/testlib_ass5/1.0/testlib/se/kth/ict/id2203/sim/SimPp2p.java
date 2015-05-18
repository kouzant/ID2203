package se.kth.ict.id2203.sim;

import se.kth.ict.id2203.ports.pp2p.*;
import se.sics.kompics.*;

public class SimPp2p extends ComponentDefinition
{
    private Negative<PerfectPointToPointLink> pp2p;
    private SimProcess process;
    private Handler<Pp2pSend> handlePp2pSend;
    
    public SimPp2p() {
        this.pp2p = (Negative<PerfectPointToPointLink>)this.provides((Class)PerfectPointToPointLink.class);
        this.subscribe((Handler)(this.handlePp2pSend = new Handler<Pp2pSend>() {
            public void handle(final Pp2pSend event) {
                SimUniverse.universe.handleProcessEvent(SimPp2p.this.process, (Event)event);
            }
        }), (Port)this.pp2p);
    }
    
    public void setProcess(final SimProcess process) {
        this.process = process;
    }
    
    public void triggerEvent(final Event e) {
        this.trigger(e, (Port)this.pp2p);
    }
}
