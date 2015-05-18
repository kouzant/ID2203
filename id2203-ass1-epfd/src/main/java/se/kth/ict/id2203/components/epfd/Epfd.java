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

public class Epfd extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(Epfd.class);

	private Positive<PerfectPointToPointLink> pp2p = requires(PerfectPointToPointLink.class);
	private Positive<Timer> timer = requires(Timer.class);
	private Negative<EventuallyPerfectFailureDetector> epfd = provides(EventuallyPerfectFailureDetector.class);

	private long delay;
	private final long delta;
	private Integer sequenceNumber;
	private Set<Address> alive;
	private Set<Address> suspected;
	private final Address self;
	private ScheduleTimeout timeout;
	private final Set<Address> nodes;

	public Epfd(EpfdInit init) {
		delay = init.getInitialDelay();
		delta = init.getDeltaDelay();
		sequenceNumber = 0;
		alive = new HashSet<Address>(init.getAllAddresses());
		suspected = new HashSet<Address>();
		self = init.getSelfAddress();
		nodes = init.getAllAddresses();
		
		subscribe(handleStart, control);
		subscribe(handleCheckTimeout, timer);
		subscribe(handleHbRequests, pp2p);
		subscribe(handleHbReplies, pp2p);
	}

	private void setTimer(long delay) {
		timeout = new ScheduleTimeout(delay);
		timeout.setTimeoutEvent(new CheckTimeout(timeout));
		trigger(timeout, timer);
	}
	
	// Handle Start event
	private Handler<Start> handleStart = new Handler<Start>() {

		@Override
		public void handle(Start event) {
			logger.info("Component Epfd created at node: " + self.getId() + "!");
			setTimer(delay);
		}
	};

	// Handle CheckTimeout event
	private Handler<CheckTimeout> handleCheckTimeout = new Handler<CheckTimeout>() {

		@Override
		public void handle(CheckTimeout event) {
			//logger.info("Node: " + self.getId() + " CheckTimeout");
			// Intersection of alive and suspected sets
			for (Address node : alive) {
				if (suspected.contains(node)) {
					//logger.info("Suspected set contains alive nodes");
					delay += delta;
					break;
				}
			}
			sequenceNumber++;
			logger.info("Sequence number is: {}", sequenceNumber);
			logger.info("New timeout delay is: {}", delay);

			for (Address node : nodes) {
				if ((!alive.contains(node)) && (!suspected.contains(node))) {
					suspected.add(node);
					Suspect suspectEvent = new Suspect(node);
					trigger(suspectEvent, epfd);
				} else if (alive.contains(node) && suspected.contains(node)) {
					suspected.remove(node);
					Restore restoreEvent = new Restore(node);
					trigger(restoreEvent, epfd);
				}

				// Send Heartbeat request to all nodes
				HeartbeatRequestMsg hbRequestMsg = new HeartbeatRequestMsg(
						self, sequenceNumber);
				trigger(new Pp2pSend(node, hbRequestMsg), pp2p);
			}

			alive.clear();

			// Reschedule timer
			setTimer(delay);
		}
	};

	// Handle Heartbeat requests
	private Handler<HeartbeatRequestMsg> handleHbRequests = new Handler<HeartbeatRequestMsg>() {

		@Override
		public void handle(HeartbeatRequestMsg event) {
			//logger.info("Received HB Request from node: " + event.getSource());
			HeartbeatReplyMsg hbReplyMsg = new HeartbeatReplyMsg(
					self, event.getSequenceNumber());
			trigger(new Pp2pSend(event.getSource(), hbReplyMsg), pp2p);
		}
	};

	// Handle Heartbeat replies
	private Handler<HeartbeatReplyMsg> handleHbReplies = new Handler<HeartbeatReplyMsg>() {

		@Override
		public void handle(HeartbeatReplyMsg event) {
			//logger.info("Received HB Reply from node: " + event.getSource());
			if (event.getSequenceNumber().equals(sequenceNumber)
					|| suspected.contains(event.getSource())) {
				alive.add(event.getSource());
			}
		}
	};
}