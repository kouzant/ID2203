package se.kth.ict.id2203.components.paxos;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.ac.AbortableConsensus;
import se.kth.ict.id2203.ports.ac.AcAbort;
import se.kth.ict.id2203.ports.ac.AcDecide;
import se.kth.ict.id2203.ports.ac.AcPropose;
import se.kth.ict.id2203.ports.beb.BebBroadcast;
import se.kth.ict.id2203.ports.beb.BestEffortBroadcast;
import se.kth.ict.id2203.ports.pp2p.PerfectPointToPointLink;
import se.kth.ict.id2203.ports.pp2p.Pp2pSend;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;

public class Paxos extends ComponentDefinition {

	private static final Logger logger = LoggerFactory.getLogger(Paxos.class);

	private Negative<AbortableConsensus> ac = provides(AbortableConsensus.class);
	private Positive<BestEffortBroadcast> beb = requires(BestEffortBroadcast.class);
	private Positive<PerfectPointToPointLink> pp2p = requires(PerfectPointToPointLink.class);

	private final Address self;
	private final Set<Address> nodes;
	private final Integer numberOfNodes;
	private Integer t, prepTs, ats, av, pts, pv, acks;
	private List<ReadList> readList;

	public Paxos(PaxosInit init) {
		this.self = init.getSelfAddress();
		this.nodes = init.getAllAddresses();
		this.t = 0;
		this.prepTs = 0;
		this.ats = 0;
		this.av = 0;
		this.pts = 0;
		this.pv = null;
		this.acks = 0;
		this.numberOfNodes = nodes.size();
		this.readList = new LinkedList<ReadList>();

		subscribe(startHandler, control);
		
		subscribe(proposeHandler, ac);
		
		subscribe(prepareReceive, beb);
		subscribe(acceptHandler, beb);
		
		subscribe(nAckHandler, pp2p);
		subscribe(prepAckHandler, pp2p);
		subscribe(accAckHandler, pp2p);
	}

	private Handler<Start> startHandler = new Handler<Start>() {

		@Override
		public void handle(Start event) {
			logger.info("Paxos component started!");
		}
	};

	private Handler<AcPropose> proposeHandler = new Handler<AcPropose>() {

		@Override
		public void handle(AcPropose event) {
			//logger.info("Received proposal event from App");
			t++;
			pts = t * numberOfNodes + self.getId();
			pv = event.getValue();
			readList.clear();
			acks = 0;
			PrepareMessage prepMsg = new PrepareMessage(self, pts, t);

			trigger(new BebBroadcast(prepMsg), beb);
		}
	};

	private Handler<PrepareMessage> prepareReceive = new Handler<PrepareMessage>() {

		@Override
		public void handle(PrepareMessage event) {
			//logger.info("Received PREPARE message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getPts() < prepTs) {
				NAckMessage nack = new NAckMessage(self, event.getPts(), t);
				//logger.info("Sending N-ACK");
				trigger(new Pp2pSend(event.getSource(), nack), pp2p);
			} else {
				prepTs = event.getPts();
				PrepareAckMessage prepAckMsg = new PrepareAckMessage(self, ats,
						av, event.getPts(), t);
				//logger.info("Sending ACK");
				trigger(new Pp2pSend(event.getSource(), prepAckMsg), pp2p);
			}
		}
	};
	
	private Handler<NAckMessage> nAckHandler = new Handler<NAckMessage>() {

		@Override
		public void handle(NAckMessage event) {
			//logger.info("Received N-ACK");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getTs().equals(pts)) {
				pts = 0;
				//logger.info("Aborting!");
				trigger(new AcAbort(), ac);
			}
		}
	};
	
	private Handler<PrepareAckMessage> prepAckHandler = new Handler<PrepareAckMessage>() {

		@Override
		public void handle(PrepareAckMessage event) {
			//logger.info("Received ACK message");
			t = Integer.max(t, event.getT()) + 1;
			
			if (event.getTs().equals(pts)) {
				readList.add(new ReadList(event.getAts(), event.getAv()));

				
				if (readList.size() > (numberOfNodes / 2)) {
					//Sort list by timestamp
					Collections.sort(readList, new Comparator<ReadList>() {
						@Override
						public int compare(ReadList obj0, ReadList obj1) {
							return obj0.getTs() < obj1.getTs() ? -1 : 1;
						}
					});
					
					ReadList highest = readList.get(readList.size() - 1);
					Integer ts = new Integer(highest.getTs());
					Integer v = new Integer(highest.getV());
					
					if (ts != 0) {
						pv = v;
					}
					readList.clear();
					
					AcceptMessage accMsg = new AcceptMessage(self, pts, pv, t);
					trigger(new BebBroadcast(accMsg), beb);
				}
			}
		}
	};
	
	private Handler<AcceptMessage> acceptHandler = new Handler<AcceptMessage>() {

		@Override
		public void handle(AcceptMessage event) {
			//logger.info("Received ACCEPT message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getPts() < prepTs) {
				NAckMessage nAckMsg = new NAckMessage(self, event.getPts(), t);
				//logger.info("Sending N-ACK ");
				trigger(new Pp2pSend(event.getSource(), nAckMsg), pp2p);
			} else {
				ats = event.getPts();
				prepTs = event.getPts();
				av = event.getPv();
				AcceptAckMessage accAck = new AcceptAckMessage(self, event.getPts(), t);
				//logger.info("Sending ACCEPT ACK message");
				trigger(new Pp2pSend(event.getSource(), accAck), pp2p);
			}
		}
	};
	
	private Handler<AcceptAckMessage> accAckHandler = new Handler<AcceptAckMessage>() {

		@Override
		public void handle(AcceptAckMessage event) {
			//logger.info("Received ACCEPT ACK message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getTs().equals(pts)) {
				acks++;
				if (acks > (numberOfNodes / 2)) {
					pts = 0;
					AcDecide decision = new AcDecide(pv);
					trigger(decision, ac);
				}
			}
		}
	};
}
