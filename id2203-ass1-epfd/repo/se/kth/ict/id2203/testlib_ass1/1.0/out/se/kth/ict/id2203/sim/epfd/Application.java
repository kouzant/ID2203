package se.kth.ict.id2203.sim.epfd;

import se.kth.ict.id2203.ports.epfd.*;
import se.kth.ict.id2203.sim.*;
import se.sics.kompics.*;

public class Application extends ComponentDefinition
{
    private Positive<EventuallyPerfectFailureDetector> epfd;
    private Process process;
    private Handler<Suspect> handleSuspectDeliver;
    private Handler<Restore> handleRestoreDeliver;
    
    public Application() {
        this.epfd = (Positive<EventuallyPerfectFailureDetector>)this.requires((Class)EventuallyPerfectFailureDetector.class);
        this.handleSuspectDeliver = new Handler<Suspect>() {
            public void handle(final Suspect event) {
                EpfdTester.universe.handleProcessEvent(Application.this.process, (Event)event);
            }
        };
        this.handleRestoreDeliver = new Handler<Restore>() {
            public void handle(final Restore event) {
                EpfdTester.universe.handleProcessEvent(Application.this.process, (Event)event);
            }
        };
        this.subscribe((Handler)this.handleSuspectDeliver, (Port)this.epfd);
        this.subscribe((Handler)this.handleRestoreDeliver, (Port)this.epfd);
    }
    
    void setProcess(final Process process) {
        this.process = process;
    }
}
