package se.kth.ict.id2203.sim;

import se.sics.kompics.timer.*;
import se.sics.kompics.*;

public class SimTimer extends ComponentDefinition
{
    private Negative<Timer> timer;
    private SimProcess process;
    private Handler<ScheduleTimeout> handleScheduleTimeout;
    private Handler<SchedulePeriodicTimeout> handleSchedulePeriodicTimeout;
    private Handler<CancelTimeout> handleCancelTimeout;
    private Handler<CancelPeriodicTimeout> handleCancelPeriodicTimeout;
    
    public SimTimer() {
        this.timer = (Negative<Timer>)this.provides((Class)Timer.class);
        this.handleScheduleTimeout = new Handler<ScheduleTimeout>() {
            public void handle(final ScheduleTimeout event) {
                SimUniverse.universe.handleProcessEvent(SimTimer.this.process, (Event)event);
            }
        };
        this.handleSchedulePeriodicTimeout = new Handler<SchedulePeriodicTimeout>() {
            public void handle(final SchedulePeriodicTimeout event) {
                SimUniverse.universe.handleProcessEvent(SimTimer.this.process, (Event)event);
            }
        };
        this.handleCancelTimeout = new Handler<CancelTimeout>() {
            public void handle(final CancelTimeout event) {
                SimUniverse.universe.handleProcessEvent(SimTimer.this.process, (Event)event);
            }
        };
        this.handleCancelPeriodicTimeout = new Handler<CancelPeriodicTimeout>() {
            public void handle(final CancelPeriodicTimeout event) {
                SimUniverse.universe.handleProcessEvent(SimTimer.this.process, (Event)event);
            }
        };
        this.subscribe((Handler)this.handleScheduleTimeout, (Port)this.timer);
        this.subscribe((Handler)this.handleSchedulePeriodicTimeout, (Port)this.timer);
        this.subscribe((Handler)this.handleCancelTimeout, (Port)this.timer);
        this.subscribe((Handler)this.handleCancelPeriodicTimeout, (Port)this.timer);
    }
    
    public void setProcess(final SimProcess process) {
        this.process = process;
    }
    
    public void triggerEvent(final Event e) {
        this.trigger(e, (Port)this.timer);
    }
}
