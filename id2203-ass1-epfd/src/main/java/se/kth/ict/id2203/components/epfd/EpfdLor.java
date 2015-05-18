package se.kth.ict.id2203.components.epfd;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.epfd.EventuallyPerfectFailureDetector;
import se.kth.ict.id2203.ports.epfd.Restore;
import se.kth.ict.id2203.ports.epfd.Suspect;
import se.kth.ict.id2203.ports.pp2p.PerfectPointToPointLink;
import se.kth.ict.id2203.ports.pp2p.Pp2pSend;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

public class EpfdLor extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(Epfd.class);
	
	private int seqnum;
	private long delay;
	
	private EpfdInit event;
	
	private Set<Address> suspected, alive, allAddresses;
	
	private Positive<Timer> timer = requires(Timer.class);
	private Positive<PerfectPointToPointLink> pptpl = requires(PerfectPointToPointLink.class);
	private Negative<EventuallyPerfectFailureDetector> epfd = provides(EventuallyPerfectFailureDetector.class);

	public EpfdLor(EpfdInit init) {
		logger.info("[Epfd] Constructor");
		event = init;
		
		delay = event.getInitialDelay();
		
		logger.info("[Epfd] Delay = " + event.getInitialDelay());
		logger.info("[Epfd] Delta = " + event.getDeltaDelay());
		
		alive = new HashSet<Address>(event.getAllAddresses());
		allAddresses = new HashSet<Address>(event.getAllAddresses());
		suspected = new HashSet<Address>();
		
		subscribe(handleStart, control);
		subscribe(hCheckTimeout, timer);
		subscribe(hHeartbeatRequestMessage, pptpl);
		subscribe(hHeartbeatReplyMessage, pptpl);
		
		logger.info("[Epfd] Timer initialized with initial delay.");
	}
	// Handle Start event
		private Handler<Start> handleStart = new Handler<Start>() {

			@Override
			public void handle(Start event) {
				setTimeout(delay);
			}
		};
	
	private Handler<CheckTimeout> hCheckTimeout= new Handler<CheckTimeout>() {
		public void handle(CheckTimeout msg) {
			//logger.info("[Epfd] Check Timeout");
			// Intersection of alive and suspected sets
						for (Address node : alive) {
							if (suspected.contains(node)) {
								delay += event.getDeltaDelay();
								break;
							}
						}
						
			logger.info("Delay: {}", delay);
			seqnum++;
			
			for(Address p : allAddresses) {
				logger.info("sdf");
				if(!alive.contains(p) && !suspected.contains(p)) {
					suspected.add(p);
					trigger(new Suspect(p), epfd);
				} else if(alive.contains(p) && suspected.contains(p)) {
					suspected.remove(p);
					trigger(new Restore(p), epfd);
				}
				
				trigger(new Pp2pSend(p, new HeartbeatRequestMsg(event.getSelfAddress(), seqnum)), pptpl);
			}
			
			alive.clear();
			setTimeout(delay);
		}
	};
	
	private Handler<HeartbeatRequestMsg> hHeartbeatRequestMessage = new Handler<HeartbeatRequestMsg>() {
		public void handle(HeartbeatRequestMsg msg) {
			//logger.info("Received a HearbeatRequestMessage!");
			trigger(new Pp2pSend(msg.getSource(), new HeartbeatReplyMsg(event.getSelfAddress(), msg.getSequenceNumber())), pptpl);
			//logger.info("Sent an HearbeatReplyMessage to the sender of the HeartbeatRequestMessage!");
		}
	};
	
	private Handler<HeartbeatReplyMsg> hHeartbeatReplyMessage = new Handler<HeartbeatReplyMsg>() {
		public void handle(HeartbeatReplyMsg msg) {
			//logger.info("Received a HearbeatReplyMessage!");
			if(msg.getSequenceNumber().equals(seqnum) || suspected.contains(msg.getSource())){
				alive.add(msg.getSource());
			}
		}
	};
	
	private void setTimeout(long interval) {
		ScheduleTimeout st = new ScheduleTimeout(interval);
		st.setTimeoutEvent(new CheckTimeout(st));
		trigger(st, timer);
	}
}