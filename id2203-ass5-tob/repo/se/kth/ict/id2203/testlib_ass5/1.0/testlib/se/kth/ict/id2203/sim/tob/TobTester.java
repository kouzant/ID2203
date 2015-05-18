package se.kth.ict.id2203.sim.tob;

import org.apache.log4j.*;
import se.sics.kompics.address.*;
import se.sics.kompics.*;
import se.kth.ict.id2203.sim.*;
import se.sics.kompics.launch.*;
import se.kth.ict.id2203.ports.tob.*;
import java.util.*;

public class TobTester extends SimUniverse
{
    private final Random random;
    private final Topology topology;
    private static final long STABILIZATION_TIME_S = 100L;
    
    static {
        final Properties props = new Properties();
        props.setProperty("log4j.appender.A1", "org.apache.log4j.ConsoleAppender");
        props.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        props.setProperty("log4j.appender.A1.layout.ConversionPattern", "%5r %-5p {%c{1}} %m%n");
        props.setProperty("log4j.rootLogger", "INFO, A1");
        props.setProperty("log4j.logger.se.kth.ict.id2203.components", "WARN");
        PropertyConfigurator.configure(props);
    }
    
    public static void main(final String[] args) {
        System.out.println("Log passed: " + test());
    }
    
    public static void correctAndSubmit(final String email, final String password) {
        if (test()) {
            System.out.println("Tests passed, submitting...");
            final String response = Submit.submit(email, password, "tob", TobTester.universe.getSerialVersionUIDs());
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
        Kompics.createAndStart((Class)TobTester.class);
        final boolean passed = TobTester.universe.validate();
        Kompics.forceShutdown();
        return passed;
    }
    
    public TobTester() {
        this.topology = new Topology() {
            {
                this.node(1, "127.0.0.1", 22031);
                this.node(2, "127.0.0.1", 22032);
                this.node(3, "127.0.0.1", 22033);
                this.node(4, "127.0.0.1", 22034);
                this.node(5, "127.0.0.1", 22035);
                this.defaultLinks(2000L, 0.0);
            }
        };
        TobTester.universe = this;
        this.random = new Random(0L);
        ((SimScheduler)Kompics.getScheduler()).universe = this;
        final Set<Address> addresses = (Set<Address>)this.topology.getAllAddresses();
        for (final Address address : addresses) {
            final ProcessInit init = new ProcessInit(address, new HashSet<Address>(addresses));
            final Component c = this.create((Class)Process.class, (Init)init);
            this.processes.put(address.getId(), (Process)c.getComponent());
        }
        this.createInputEvents();
    }
    
    @Override
    public void processSimEvent(final SimEvent se) {
        final Process p = this.processes.get(se.pid);
        if (p == null || p.crashed) {
            return;
        }
        if (se.type == SimEvent.Type.TOB_BROADCAST) {
            this.logEvent(se);
            p.triggerApplicationEvent((Event)se.event);
        }
        else {
            super.processSimEvent(se);
        }
    }
    
    @Override
    public void handleProcessEvent(final SimProcess p, final Event event) {
        if (!p.crashed) {
            if (event instanceof TobDeliver) {
                this.logEvent(new SimEvent(SimEvent.Type.TOB_DELIVER, this.currentTime, p.pid, event));
            }
            else {
                super.handleProcessEvent(p, event);
            }
        }
    }
    
    @Override
    public long getLatencyMs(final Address src, final Address dst) throws NoLinkException {
        return Math.max(10L, this.topology.getLatencyMs(src, dst) + (long)(this.random.nextGaussian() * 500.0));
    }
    
    private Address getAddress(final int pid) {
        return this.processes.get(pid).address;
    }
    
    private long s(final long t) {
        return (t + 100L) * 1000L;
    }
    
    private void createInputEvents() {
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(0L), 1, new TobBroadcast((TobDeliver)new Msg(this.getAddress(1), "1")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(0L), 2, new TobBroadcast((TobDeliver)new Msg(this.getAddress(2), "2")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(0L), 3, new TobBroadcast((TobDeliver)new Msg(this.getAddress(3), "3")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(0L), 4, new TobBroadcast((TobDeliver)new Msg(this.getAddress(4), "4")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(0L), 5, new TobBroadcast((TobDeliver)new Msg(this.getAddress(5), "5")));
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(2L), 1, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(30L), 1, null);
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(50L), 2, new TobBroadcast((TobDeliver)new Msg(this.getAddress(2), "6")));
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(80L), 1, null);
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(80L), 2, null);
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(80L), 3, null);
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(90L), 1, new TobBroadcast((TobDeliver)new Msg(this.getAddress(1), "7")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(90L), 2, new TobBroadcast((TobDeliver)new Msg(this.getAddress(2), "8")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(90L), 3, new TobBroadcast((TobDeliver)new Msg(this.getAddress(3), "9")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(90L), 4, new TobBroadcast((TobDeliver)new Msg(this.getAddress(4), "10")));
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(90L), 5, new TobBroadcast((TobDeliver)new Msg(this.getAddress(5), "11")));
        this.addFutureEvent(SimEvent.Type.CRASH_PROCESS, this.s(100L), 4, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(120L), 3, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(120L), 2, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(120L), 1, null);
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(150L), 2, null);
        this.addFutureEvent(SimEvent.Type.PAUSE_NETWORK, this.s(150L), 3, null);
        this.addFutureEvent(SimEvent.Type.TOB_BROADCAST, this.s(160L), 5, new TobBroadcast((TobDeliver)new Msg(this.getAddress(5), "12")));
        this.addFutureEvent(SimEvent.Type.CRASH_PROCESS, this.s(170L), 5, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(190L), 3, null);
        this.addFutureEvent(SimEvent.Type.RESUME_NETWORK, this.s(190L), 2, null);
        this.addFutureEvent(SimEvent.Type.TERMINATE_SIMULATION, this.s(500L), 0, null);
    }
    
    @Override
    public boolean validate() {
        final TreeSet<String> broadcasted = new TreeSet<String>();
        final ArrayList<String> ordered = new ArrayList<String>();
        final TreeMap<Integer, TreeSet<String>> broadcastedBy = new TreeMap<Integer, TreeSet<String>>();
        final TreeMap<Integer, Integer> delivered = new TreeMap<Integer, Integer>();
        final TreeMap<Integer, Boolean> crashed = new TreeMap<Integer, Boolean>();
        for (final Address a : this.topology.getAllAddresses()) {
            final int pid = a.getId();
            broadcastedBy.put(pid, new TreeSet<String>());
            delivered.put(pid, 0);
            crashed.put(pid, false);
        }
        for (final SimEvent se : this.eventLog) {
            if (se.type == SimEvent.Type.TOB_BROADCAST) {
                final TobBroadcast e = (TobBroadcast)se.event;
                final String s = ((Msg)e.getDeliverEvent()).s;
                broadcasted.add(s);
                broadcastedBy.get(se.pid).add(s);
            }
            else if (se.type == SimEvent.Type.TOB_DELIVER) {
                final String s2 = ((Msg)se.event).s;
                if (!broadcasted.contains(s2)) {
                    return false;
                }
                final int delCount = delivered.get(se.pid);
                if (ordered.size() < delCount) {
                    return false;
                }
                if (ordered.size() == delCount) {
                    ordered.add(s2);
                }
                else if (!s2.equals(ordered.get(delCount))) {
                    return false;
                }
                delivered.put(se.pid, delCount + 1);
            }
            else {
                if (se.type != SimEvent.Type.CRASH_PROCESS) {
                    continue;
                }
                crashed.put(se.pid, true);
            }
        }
        final TreeSet<String> broadcastedCorrect = new TreeSet<String>();
        for (final Address a2 : this.topology.getAllAddresses()) {
            final int pid2 = a2.getId();
            if (!crashed.get(pid2)) {
                broadcastedCorrect.addAll(broadcastedBy.get(pid2));
            }
        }
        broadcastedCorrect.removeAll(ordered);
        if (!broadcastedCorrect.isEmpty()) {
            return false;
        }
        for (final Address a2 : this.topology.getAllAddresses()) {
            final int pid2 = a2.getId();
            if (!crashed.get(pid2) && delivered.get(pid2) != ordered.size()) {
                return false;
            }
        }
        return true;
    }
    
    class Msg extends TobDeliver
    {
        private static final long serialVersionUID = -6860853846309555000L;
        private final String s;
        
        public Msg(final Address source, final String s) {
            super(source);
            this.s = s;
        }
        
        public String toString() {
            return "Msg(" + this.s + ")";
        }
    }
}
