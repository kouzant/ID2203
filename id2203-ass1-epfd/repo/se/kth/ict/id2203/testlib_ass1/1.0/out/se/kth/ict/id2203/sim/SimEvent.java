package se.kth.ict.id2203.sim;

public class SimEvent implements Comparable<SimEvent>
{
    public final Type type;
    public final long time;
    public final int pid;
    public final Object event;
    
    public SimEvent(final Type type, final long time, final int pid, final Object event) {
        this.type = type;
        this.time = time;
        this.pid = pid;
        this.event = event;
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SimEvent)) {
            return false;
        }
        final SimEvent that = (SimEvent)obj;
        return this.type == that.type && this.time == that.time && this.pid == that.pid && (this.event == that.event || (this.event != null && this.event.equals(that.event)));
    }
    
    @Override
    public int compareTo(final SimEvent o) {
        int c = Long.compare(this.time, o.time);
        if (c == 0) {
            c = Integer.compare(this.type.ordinal(), o.type.ordinal());
        }
        return c;
    }
    
    @Override
    public String toString() {
        return String.format("SimEvent(%s, %d, %d, %s)", this.type, this.time, this.pid, this.event);
    }
    
    public enum Type
    {
        TERMINATE_SIMULATION("TERMINATE_SIMULATION", 0), 
        CRASH_PROCESS("CRASH_PROCESS", 1), 
        TIMER_TIMEOUT("TIMER_TIMEOUT", 2), 
        TIMER_PERIODIC_TIMEOUT("TIMER_PERIODIC_TIMEOUT", 3), 
        PP2P_MESSAGE("PP2P_MESSAGE", 4), 
        PAUSE_NETWORK("PAUSE_NETWORK", 5), 
        RESUME_NETWORK("RESUME_NETWORK", 6), 
        SUSPECT_PROCESS("SUSPECT_PROCESS", 7), 
        RESTORE_PROCESS("RESTORE_PROCESS", 8), 
        CRB_BROADCAST("CRB_BROADCAST", 9), 
        CRB_DELIVER("CRB_DELIVER", 10), 
        AR_READ_REQUEST("AR_READ_REQUEST", 11), 
        AR_WRITE_REQUEST("AR_WRITE_REQUEST", 12), 
        AR_READ_RESPONSE("AR_READ_RESPONSE", 13), 
        AR_WRITE_RESPONSE("AR_WRITE_RESPONSE", 14), 
        UNC_PROPOSE("UNC_PROPOSE", 15), 
        UNC_ABORT("UNC_ABORT", 16), 
        UNC_DECIDE("UNC_DECIDE", 17), 
        TOB_BROADCAST("TOB_BROADCAST", 18), 
        TOB_DELIVER("TOB_DELIVER", 19);
    }
}
