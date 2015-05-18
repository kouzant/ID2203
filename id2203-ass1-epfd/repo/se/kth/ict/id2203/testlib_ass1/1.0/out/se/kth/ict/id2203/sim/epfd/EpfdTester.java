package se.kth.ict.id2203.sim.epfd;

import org.apache.log4j.*;
import se.sics.kompics.address.*;
import se.sics.kompics.*;
import se.kth.ict.id2203.sim.*;
import se.kth.ict.id2203.ports.epfd.*;
import se.sics.kompics.launch.*;
import java.util.*;

public class EpfdTester extends SimUniverse
{
    private final Topology topology;
    private static final long STABILIZATION_TIME_S = 100L;
    
    static {
        final Properties props = new Properties();
        props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.A1.layout.ConversionPattern", "%5r %-5p {%c{1}} %m%n");
        props.setProperty("log4j.rootLogger", "WARN, A1");
        PropertyConfigurator.configure(props);
    }
    
    public static void main(final String[] args) {
        System.out.println("Log passed: " + test());
    }
    
    public static void correctAndSubmit(final String email, final String password) {
        if (test()) {
            System.out.println("Tests passed, submitting...");
            final String response = Submit.submit(email, password, "epfd");
            if (response.equals("Success")) {
                System.out.println("Submission successful.");
            }
            else {
                System.out.println("Submission failed: " + response);
            }
        }
        else {
            System.out.println("Tests failed.");
        }
    }
    
    public static boolean test() {
        Kompics.setScheduler((Scheduler)new SimScheduler());
        Kompics.createAndStart((Class)EpfdTester.class);
        final boolean passed = EpfdTester.universe.validate();
        Kompics.forceShutdown();
        return passed;
    }
    
    public EpfdTester() {
        this.topology = new Topology() {
            {
                this.node(1, "127.0.0.1", 22031);
                this.node(2, "127.0.0.1", 22032);
                this.node(3, "127.0.0.1", 22033);
                this.defaultLinks(1000L, 0.0);
            }
        };
        EpfdTester.universe = this;
        ((SimScheduler)Kompics.getScheduler()).universe = this;
        final Set<Address> addresses = (Set<Address>)this.topology.getAllAddresses();
        for (final Address address : addresses) {
            final Component c = this.create((Class)Process.class, (Init)new ProcessInit(address, addresses));
            this.processes.put(address.getId(), (Process)c.getComponent());
        }
        this.createInputEvents();
    }
    
    @Override
    public void handleProcessEvent(final SimProcess p, final Event event) {
        if (!p.crashed) {
            if (event instanceof Suspect) {
                this.logEvent(new SimEvent(SimEvent.Type.SUSPECT_PROCESS, this.currentTime, p.pid, event));
            }
            else if (event instanceof Restore) {
                this.logEvent(new SimEvent(SimEvent.Type.RESTORE_PROCESS, this.currentTime, p.pid, event));
            }
            else {
                super.handleProcessEvent(p, event);
            }
        }
    }
    
    @Override
    public long getLatencyMs(final Address src, final Address dst) throws NoLinkException {
        return this.topology.getLatencyMs(src, dst);
    }
    
    private long s(final long t) {
        return (t + 100L) * 1000L;
    }
    
    private void createInputEvents() {
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(0L), 1, null);
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(10L), 2, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(20L), 1, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(30L), 2, null);
        this.addFutureEvent(SimEvent.Type.CRASH_PROCESS, this.s(40L), 3, null);
        this.addFutureEvent(SimEvent.Type.CRASH_PROCESS, this.s(50L), 2, null);
        this.addFutureEvent(SimEvent.Type.TERMINATE_SIMULATION, this.s(70L), 0, null);
    }
    
    @Override
    public boolean validate() {
        ArrayList<SimEvent> evs = this.filterTypeTime(this.eventLog, SimEvent.Type.SUSPECT_PROCESS, this.s(0L), this.s(70L));
        if (this.matchAndRemoveSuspicion(evs, this.s(0L), this.s(10L), 1, 2) && this.matchAndRemoveSuspicion(evs, this.s(0L), this.s(10L), 1, 3) && this.matchAndRemoveSuspicion(evs, this.s(0L), this.s(10L), 3, 1) && this.matchAndRemoveSuspicion(evs, this.s(0L), this.s(10L), 2, 1) && this.matchAndRemoveSuspicion(evs, this.s(10L), this.s(20L), 2, 3) && this.matchAndRemoveSuspicion(evs, this.s(10L), this.s(20L), 3, 2) && this.matchAndRemoveSuspicion(evs, this.s(40L), this.s(50L), 1, 3) && this.matchAndRemoveSuspicion(evs, this.s(40L), this.s(50L), 2, 3) && this.matchAndRemoveSuspicion(evs, this.s(50L), this.s(100L), 1, 2) && evs.size() == 0) {
            evs = this.filterTypeTime(this.eventLog, SimEvent.Type.RESTORE_PROCESS, this.s(0L), this.s(70L));
            if (this.matchAndRemoveRestore(evs, this.s(20L), this.s(30L), 1, 3) && this.matchAndRemoveRestore(evs, this.s(20L), this.s(30L), 3, 1) && this.matchAndRemoveRestore(evs, this.s(30L), this.s(40L), 1, 2) && this.matchAndRemoveRestore(evs, this.s(30L), this.s(40L), 2, 1) && this.matchAndRemoveRestore(evs, this.s(30L), this.s(40L), 2, 3) && this.matchAndRemoveRestore(evs, this.s(30L), this.s(40L), 3, 2) && evs.size() == 0) {
                return true;
            }
        }
        return false;
    }
    
    private SimEvent matchSuspicion(final ArrayList<SimEvent> evs, final long first, final long last, final int pid, final int suspectsPid) {
        for (final SimEvent se : evs) {
            if (se.type == SimEvent.Type.SUSPECT_PROCESS && se.time >= first && se.time <= last && se.pid == pid) {
                final Suspect sd = (Suspect)se.event;
                if (sd.getSource().getId() == suspectsPid) {
                    return se;
                }
                continue;
            }
        }
        return null;
    }
    
    private boolean matchAndRemoveSuspicion(final ArrayList<SimEvent> evs, final long first, final long last, final int pid, final int suspectsPid) {
        final SimEvent se = this.matchSuspicion(evs, first, last, pid, suspectsPid);
        if (se == null) {
            return false;
        }
        evs.remove(se);
        return true;
    }
    
    private SimEvent matchRestore(final ArrayList<SimEvent> evs, final long first, final long last, final int pid, final int restorePid) {
        for (final SimEvent se : evs) {
            if (se.type == SimEvent.Type.RESTORE_PROCESS && se.time >= first && se.time <= last && se.pid == pid) {
                final Restore sd = (Restore)se.event;
                if (sd.getSource().getId() == restorePid) {
                    return se;
                }
                continue;
            }
        }
        return null;
    }
    
    private boolean matchAndRemoveRestore(final ArrayList<SimEvent> evs, final long first, final long last, final int pid, final int restorePid) {
        final SimEvent se = this.matchRestore(evs, first, last, pid, restorePid);
        if (se == null) {
            return false;
        }
        evs.remove(se);
        return true;
    }
}
