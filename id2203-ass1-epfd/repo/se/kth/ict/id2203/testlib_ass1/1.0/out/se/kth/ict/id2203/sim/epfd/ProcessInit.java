package se.kth.ict.id2203.sim.epfd;

import se.sics.kompics.*;
import se.sics.kompics.address.*;
import java.util.*;

class ProcessInit extends Init<Process>
{
    private final Address selfAddress;
    private final Set<Address> allAddresses;
    
    public ProcessInit(final Address selfAddress, final Set<Address> allAddresses) {
        this.selfAddress = selfAddress;
        this.allAddresses = allAddresses;
    }
    
    public Address getSelfAddress() {
        return this.selfAddress;
    }
    
    public Set<Address> getAllAddresses() {
        return this.allAddresses;
    }
}
