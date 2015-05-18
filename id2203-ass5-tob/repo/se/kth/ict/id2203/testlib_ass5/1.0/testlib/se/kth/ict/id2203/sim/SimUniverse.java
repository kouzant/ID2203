package se.kth.ict.id2203.sim;

import se.sics.kompics.launch.*;
import se.sics.kompics.*;
import se.kth.ict.id2203.ports.pp2p.*;
import se.sics.kompics.timer.*;
import java.lang.reflect.*;
import se.sics.kompics.address.*;
import java.util.*;
import java.io.*;

public abstract class SimUniverse extends ComponentDefinition
{
    public static SimUniverse universe;
    protected long currentTime;
    private PriorityQueue<SimEvent> futureEvents;
    public TreeMap<Integer, SimProcess> processes;
    public ArrayList<SimEvent> eventLog;
    public HashSet<Long> serialVersionUIDs;
    ArrayList<InNetworkMsg> inNetworkMsgs;
    
    public SimUniverse() {
        this.currentTime = 0L;
        this.futureEvents = new PriorityQueue<SimEvent>();
        this.processes = new TreeMap<Integer, SimProcess>();
        this.eventLog = new ArrayList<SimEvent>();
        this.serialVersionUIDs = new HashSet<Long>();
        this.inNetworkMsgs = new ArrayList<InNetworkMsg>();
    }
    
    public boolean advanceTime() {
        SimEvent se = this.futureEvents.peek();
        if (se == null) {
            return false;
        }
        this.currentTime = se.time;
        while (se != null && se.time == this.currentTime) {
            se = this.futureEvents.poll();
            if (se.type == SimEvent.Type.TERMINATE_SIMULATION) {
                return false;
            }
            this.processSimEvent(se);
            se = this.futureEvents.peek();
        }
        return true;
    }
    
    public void logEvent(final SimEvent se) {
        this.eventLog.add(se);
    }
    
    public void processSimEvent(final SimEvent se) {
        final SimProcess p = this.processes.get(se.pid);
        if (p.crashed) {
            return;
        }
        if (se.type == SimEvent.Type.CRASH_PROCESS) {
            p.crashed = true;
            this.crashProcess(p.pid);
            this.logEvent(se);
        }
        else if (se.type == SimEvent.Type.PAUSE_NETWORK) {
            assert !p.pausedNetwork;
            p.pausedNetwork = true;
            this.logEvent(se);
        }
        else if (se.type == SimEvent.Type.RESUME_NETWORK) {
            assert p.pausedNetwork;
            p.pausedNetwork = false;
            this.logEvent(se);
            for (final InNetworkMsg msg : this.inNetworkMsgs) {
                if (!msg.sent) {
                    if (msg.sender != p) {
                        if (!msg.event.getDestination().equals((Object)p.address) || msg.sender.pausedNetwork) {
                            continue;
                        }
                    }
                    try {
                        final long latency = this.getLatencyMs(msg.sender.address, msg.event.getDestination());
                        this.addFutureEvent(SimEvent.Type.PP2P_MESSAGE, latency, msg.event.getDestination().getId(), msg);
                        msg.sent = true;
                    }
                    catch (NoLinkException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        else if (se.type == SimEvent.Type.PP2P_MESSAGE) {
            final InNetworkMsg msg = (InNetworkMsg)se.event;
            if (p.pausedNetwork) {
                if (msg.sender.crashed) {
                    this.inNetworkMsgs.remove(msg);
                }
                else {
                    msg.sent = false;
                }
            }
            else {
                this.inNetworkMsgs.remove(msg);
                final Pp2pDeliver de = msg.event.getDeliverEvent();
                this.addSerialVersionUID(de);
                p.triggerPp2pEvent((Event)de);
            }
        }
        else if (se.type == SimEvent.Type.TIMER_TIMEOUT) {
            final ScheduleTimeout e2 = (ScheduleTimeout)se.event;
            p.triggerTimerEvent((Event)e2.getTimeoutEvent());
        }
        else if (se.type == SimEvent.Type.TIMER_PERIODIC_TIMEOUT) {
            final SchedulePeriodicTimeout e3 = (SchedulePeriodicTimeout)se.event;
            p.triggerTimerEvent((Event)e3.getTimeoutEvent());
            this.addFutureEvent(SimEvent.Type.TIMER_PERIODIC_TIMEOUT, e3.getPeriod(), se.pid, e3);
        }
    }
    
    public void handleProcessEvent(final SimProcess p, final Event event) {
        if (!p.crashed) {
            if (event instanceof Pp2pSend) {
                final Pp2pSend ev = (Pp2pSend)event;
                this.handlePp2pSendEvent(p, ev);
            }
            else if (event instanceof ScheduleTimeout) {
                final ScheduleTimeout ev2 = (ScheduleTimeout)event;
                this.addFutureEvent(SimEvent.Type.TIMER_TIMEOUT, ev2.getDelay(), p.pid, event);
            }
            else if (event instanceof SchedulePeriodicTimeout) {
                final SchedulePeriodicTimeout ev3 = (SchedulePeriodicTimeout)event;
                this.addFutureEvent(SimEvent.Type.TIMER_PERIODIC_TIMEOUT, ev3.getDelay(), p.pid, event);
            }
            else if (event instanceof CancelTimeout) {
                final CancelTimeout ev4 = (CancelTimeout)event;
                this.removeScheduledTimeout(ev4.getTimeoutId());
            }
            else if (event instanceof CancelPeriodicTimeout) {
                final CancelPeriodicTimeout ev5 = (CancelPeriodicTimeout)event;
                this.removeScheduledTimeout(ev5.getTimeoutId());
            }
        }
    }
    
    Pp2pDeliver clonePp2pDeliver(final Pp2pDeliver e) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(e);
            final byte[] arr = bos.toByteArray();
            out.close();
            bos.close();
            final ByteArrayInputStream bis = new ByteArrayInputStream(arr);
            final ObjectInputStream in = new ObjectInputStream(bis);
            final Object o = in.readObject();
            in.close();
            bis.close();
            return (Pp2pDeliver)o;
        }
        catch (Exception ex) {
            return e;
        }
    }
    
    public String getSerialVersionUIDs() {
        if (this.serialVersionUIDs.isEmpty()) {
            return "NA";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Long s : this.serialVersionUIDs) {
            if (sb.length() != 0) {
                sb.append(':');
            }
            sb.append(s.toString());
        }
        return sb.toString();
    }
    
    private void addSerialVersionUID(final Pp2pDeliver de) {
        try {
            final Field f = de.getClass().getDeclaredField("serialVersionUID");
            f.setAccessible(true);
            this.serialVersionUIDs.add((Long)f.get(null));
        }
        catch (Exception ex) {}
    }
    
    private void handlePp2pSendEvent(final SimProcess p, Pp2pSend e) {
        e = new Pp2pSend(e.getDestination(), this.clonePp2pDeliver(e.getDeliverEvent()));
        final Address src = p.address;
        final Address dst = e.getDestination();
        final SimProcess q = this.processes.get(dst.getId());
        if (q == null || q.crashed) {
            return;
        }
        if (src.equals((Object)dst)) {
            final Pp2pDeliver de = e.getDeliverEvent();
            this.addSerialVersionUID(de);
            p.triggerPp2pEvent((Event)de);
        }
        else if (p.pausedNetwork) {
            final InNetworkMsg msg = new InNetworkMsg(p, e, false);
            this.inNetworkMsgs.add(msg);
        }
        else {
            try {
                final long latency = this.getLatencyMs(src, dst);
                final InNetworkMsg msg2 = new InNetworkMsg(p, e, true);
                this.inNetworkMsgs.add(msg2);
                this.addFutureEvent(SimEvent.Type.PP2P_MESSAGE, latency, e.getDestination().getId(), msg2);
            }
            catch (NoLinkException e2) {
                e2.printStackTrace();
            }
        }
    }
    
    public abstract long getLatencyMs(final Address p0, final Address p1) throws NoLinkException;
    
    public void addFutureEvent(final SimEvent.Type type, final long delay, final int pid, final Object event) {
        final SimEvent se = new SimEvent(type, this.currentTime + delay, pid, event);
        this.futureEvents.add(se);
    }
    
    public void removeScheduledTimeout(final UUID timeoutId) {
        for (final SimEvent se : this.futureEvents) {
            if (se.type == SimEvent.Type.TIMER_TIMEOUT) {
                final ScheduleTimeout st = (ScheduleTimeout)se.event;
                if (st.getTimeoutEvent().getTimeoutId().equals(timeoutId)) {
                    this.futureEvents.remove(se);
                    return;
                }
                continue;
            }
            else {
                if (se.type != SimEvent.Type.TIMER_PERIODIC_TIMEOUT) {
                    continue;
                }
                final SchedulePeriodicTimeout e = (SchedulePeriodicTimeout)se.event;
                if (e.getTimeoutEvent().getTimeoutId().equals(timeoutId)) {
                    this.futureEvents.remove(se);
                    return;
                }
                continue;
            }
        }
    }
    
    public void crashProcess(final int pid) {
        final Iterator<SimEvent> it = this.futureEvents.iterator();
        while (it.hasNext()) {
            final SimEvent se = it.next();
            if (pid == se.pid) {
                it.remove();
            }
        }
        final Iterator<InNetworkMsg> it2 = this.inNetworkMsgs.iterator();
        while (it2.hasNext()) {
            final InNetworkMsg msg = it2.next();
            if ((msg.sender.pid == pid && !msg.sent) || msg.event.getDestination().getId() == pid) {
                it2.remove();
            }
        }
    }
    
    public abstract boolean validate();
    
    protected ArrayList<SimEvent> filterType(final ArrayList<SimEvent> evs, final SimEvent.Type type) {
        final ArrayList<SimEvent> res = new ArrayList<SimEvent>();
        for (final SimEvent se : evs) {
            if (se.type == type) {
                res.add(se);
            }
        }
        return res;
    }
    
    protected ArrayList<SimEvent> filterTime(final ArrayList<SimEvent> evs, final long first, final long last) {
        final ArrayList<SimEvent> res = new ArrayList<SimEvent>();
        for (final SimEvent se : evs) {
            if (se.time >= first && se.time <= last) {
                res.add(se);
            }
        }
        return res;
    }
    
    protected ArrayList<SimEvent> filterTypeTime(final ArrayList<SimEvent> evs, final SimEvent.Type type, final long first, final long last) {
        final ArrayList<SimEvent> res = new ArrayList<SimEvent>();
        for (final SimEvent se : evs) {
            if (se.type == type && se.time >= first && se.time <= last) {
                res.add(se);
            }
        }
        return res;
    }
    
    protected ArrayList<SimEvent> filterTypePid(final ArrayList<SimEvent> evs, final SimEvent.Type type, final int pid) {
        final ArrayList<SimEvent> res = new ArrayList<SimEvent>();
        for (final SimEvent se : evs) {
            if (se.type == type && se.pid == pid) {
                res.add(se);
            }
        }
        return res;
    }
    
    class InNetworkMsg
    {
        SimProcess sender;
        Pp2pSend event;
        boolean sent;
        
        InNetworkMsg(final SimProcess sender, final Pp2pSend event, final boolean sent) {
            this.sender = sender;
            this.event = event;
            this.sent = sent;
        }
    }
}
