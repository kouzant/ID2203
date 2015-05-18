package se.kth.ict.id2203.sim.epfd;

import se.kth.ict.id2203.sim.*;
import se.kth.ict.id2203.components.epfd.*;
import java.util.*;
import se.sics.kompics.timer.*;
import se.kth.ict.id2203.ports.pp2p.*;
import se.kth.ict.id2203.ports.epfd.*;
import se.sics.kompics.*;

public class Process extends SimProcess
{
    private Component timer;
    private Component pp2p;
    private Component pfd;
    private Component app;
    private SimTimer simTimer;
    private SimPp2p simPp2p;
    private Application simApp;
    
    public Process(final ProcessInit event) {
        this.address = event.getSelfAddress();
        this.pid = event.getSelfAddress().getId();
        try {
            this.timer = this.create((Class)SimTimer.class, Init.NONE);
            this.pp2p = this.create((Class)SimPp2p.class, Init.NONE);
            this.pfd = this.create((Class)Epfd.class, (Init)new EpfdInit(event.getSelfAddress(), (Set)event.getAllAddresses(), 500L, 500L));
            this.app = this.create((Class)Application.class, Init.NONE);
            this.simTimer = (SimTimer)this.timer.getComponent();
            this.simPp2p = (SimPp2p)this.pp2p.getComponent();
            this.simApp = (Application)this.app.getComponent();
            this.simTimer.setProcess(this);
            this.simPp2p.setProcess(this);
            this.simApp.setProcess(this);
            this.connect(this.pfd.required((Class)Timer.class), this.timer.provided((Class)Timer.class));
            this.connect(this.pfd.required((Class)PerfectPointToPointLink.class), this.pp2p.provided((Class)PerfectPointToPointLink.class));
            this.connect(this.app.required((Class)EventuallyPerfectFailureDetector.class), this.pfd.provided((Class)EventuallyPerfectFailureDetector.class));
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }
    
    @Override
    public void triggerTimerEvent(final Event e) {
        this.simTimer.triggerEvent(e);
    }
    
    @Override
    public void triggerPp2pEvent(final Event e) {
        this.simPp2p.triggerEvent(e);
    }
    
    @Override
    public void triggerApplicationEvent(final Event e) {
    }
}
