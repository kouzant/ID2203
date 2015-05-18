package se.kth.ict.id2203.sim.tob;

import se.kth.ict.id2203.sim.*;
import se.kth.ict.id2203.components.beb.*;
import java.util.*;
import se.kth.ict.id2203.components.fpl.*;
import se.kth.ict.id2203.components.multipaxos.*;
import se.kth.ict.id2203.components.eld.*;
import se.kth.ict.id2203.components.tob.*;
import se.kth.ict.id2203.ports.pp2p.*;
import se.sics.kompics.timer.*;
import se.kth.ict.id2203.ports.fpl.*;
import se.kth.ict.id2203.ports.eld.*;
import se.kth.ict.id2203.ports.beb.*;
import se.kth.ict.id2203.ports.asc.*;
import se.kth.ict.id2203.ports.tob.*;
import se.sics.kompics.*;

public class Process extends SimProcess
{
    private Component timer;
    private Component pp2p;
    private Component beb;
    private Component fpl;
    private Component asc;
    private Component eld;
    private Component tob;
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
            this.beb = this.create((Class)BasicBroadcast.class, (Init)new BasicBroadcastInit(event.getSelfAddress(), (Set)event.getAllAddresses()));
            this.fpl = this.create((Class)SequenceNumberFIFOLink.class, (Init)new SequenceNumberFIFOLinkInit(this.address, (Set)event.getAllAddresses()));
            this.asc = this.create((Class)MultiPaxos.class, (Init)new MultiPaxosInit(this.address, (Set)event.getAllAddresses()));
            this.eld = this.create((Class)MonarchicalEld.class, (Init)new MonarchicalEldInit(this.address, (Set)event.getAllAddresses(), 3000, 500));
            this.tob = this.create((Class)Tob.class, (Init)new TobInit(this.address, (Set)event.getAllAddresses()));
            this.app = this.create((Class)Application.class, Init.NONE);
            this.simTimer = (SimTimer)this.timer.getComponent();
            this.simPp2p = (SimPp2p)this.pp2p.getComponent();
            this.simApp = (Application)this.app.getComponent();
            this.simTimer.setProcess(this);
            this.simPp2p.setProcess(this);
            this.simApp.setProcess(this);
            this.connect(this.eld.required((Class)PerfectPointToPointLink.class), this.pp2p.provided((Class)PerfectPointToPointLink.class));
            this.connect(this.eld.required((Class)Timer.class), this.timer.provided((Class)Timer.class));
            this.connect(this.beb.required((Class)PerfectPointToPointLink.class), this.pp2p.provided((Class)PerfectPointToPointLink.class));
            this.connect(this.fpl.required((Class)PerfectPointToPointLink.class), this.pp2p.provided((Class)PerfectPointToPointLink.class));
            this.connect(this.asc.required((Class)FIFOPerfectPointToPointLink.class), this.fpl.provided((Class)FIFOPerfectPointToPointLink.class));
            this.connect(this.tob.required((Class)EventualLeaderDetector.class), this.eld.provided((Class)EventualLeaderDetector.class));
            this.connect(this.tob.required((Class)BestEffortBroadcast.class), this.beb.provided((Class)BestEffortBroadcast.class));
            this.connect(this.tob.required((Class)AbortableSequenceConsensus.class), this.asc.provided((Class)AbortableSequenceConsensus.class));
            this.connect(this.app.required((Class)TotalOrderBroadcast.class), this.tob.provided((Class)TotalOrderBroadcast.class));
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
        this.simApp.triggerEvent(e);
    }
}
