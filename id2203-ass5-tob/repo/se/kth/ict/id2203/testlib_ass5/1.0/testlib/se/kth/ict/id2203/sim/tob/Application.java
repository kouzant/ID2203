package se.kth.ict.id2203.sim.tob;

import se.kth.ict.id2203.ports.tob.*;
import se.kth.ict.id2203.sim.*;
import se.sics.kompics.*;

public class Application extends ComponentDefinition
{
    private Positive<TotalOrderBroadcast> tob;
    private Process process;
    private Handler<TobDeliver> handleTobDeliver;
    
    public Application() {
        this.tob = (Positive<TotalOrderBroadcast>)this.requires((Class)TotalOrderBroadcast.class);
        this.subscribe((Handler)(this.handleTobDeliver = new Handler<TobDeliver>() {
            public void handle(final TobDeliver event) {
                TobTester.universe.handleProcessEvent(Application.this.process, (Event)event);
            }
        }), (Port)this.tob);
    }
    
    void setProcess(final Process process) {
        this.process = process;
    }
    
    void triggerEvent(final Event e) {
        this.trigger(e, (Port)this.tob);
    }
}
