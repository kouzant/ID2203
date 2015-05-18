package se.kth.ict.id2203.components.multipaxos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.asc.AbortableSequenceConsensus;
import se.kth.ict.id2203.ports.asc.AscAbort;
import se.kth.ict.id2203.ports.asc.AscDecide;
import se.kth.ict.id2203.ports.asc.AscPropose;
import se.kth.ict.id2203.ports.fpl.FIFOPerfectPointToPointLink;
import se.kth.ict.id2203.ports.fpl.FplSend;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;

public class MultiPaxos extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(MultiPaxos.class);

	private Negative<AbortableSequenceConsensus> asc = provides(AbortableSequenceConsensus.class);
	private Positive<FIFOPerfectPointToPointLink> fpl = requires(FIFOPerfectPointToPointLink.class);

	private final Address self;
	private final Set<Address> nodes;
	private final Integer numOfNodes;
	private Integer t, prepts, ats, al, pts, pl, tsPrime, counter;
	private ArrayList<Values<?>> av, pv, proposedValues, vSufPrime;
	private HashMap<Address, ReadItem> readList;
	private HashMap<Address, Integer> accepted, decided;

	public MultiPaxos(MultiPaxosInit event) {
		this.self = event.getSelfAddress();
		this.nodes = new HashSet<Address>(event.getAllAddresses());
		this.numOfNodes = event.getAllAddresses().size();
		this.t = 0;
		this.prepts = 0;
		this.ats = 0;
		this.al = 0;
		this.pts = 0;
		this.pl = 0;
		this.tsPrime = 0;
		this.counter = 0;
		this.av = new ArrayList<Values<?>>();
		this.pv = new ArrayList<Values<?>>();
		this.proposedValues = new ArrayList<Values<?>>();
		this.readList = new HashMap<Address, ReadItem>();
		this.accepted = new HashMap<Address, Integer>();
		this.decided = new HashMap<Address, Integer>();

		subscribe(startHandler, control);

		subscribe(proposeHandler, asc);

		subscribe(prepareMsgHandler, fpl);
		subscribe(nAckMsgHandler, fpl);
		subscribe(prepAckMsgHandler, fpl);
		subscribe(accMsg, fpl);
		subscribe(accAckMsgHandler, fpl);
		subscribe(decMsgHandler, fpl);
	}

	private Handler<Start> startHandler = new Handler<Start>() {

		@Override
		public void handle(Start event) {
			logger.info("Multi-Paxos component started");
		}
	};

	private Handler<AscPropose> proposeHandler = new Handler<AscPropose>() {

		@Override
		public void handle(AscPropose event) {
			logger.info("proposeHandler - Received PROPOSE message");
			t++;

			if (pts.equals(0)) {
				pts = t * numOfNodes + self.getId();
				pv = new ArrayList<Values<?>>(av.subList(0, al));
				pl = 0;
				proposedValues.add(new Values<Object>(event.getValue()));
				readList.clear();
				accepted.clear();
				decided.clear();

				for (Address node : nodes) {
					PrepareMessage prepMsg = new PrepareMessage(self, pts, al,
							t);
					trigger(new FplSend(node, prepMsg), fpl);
				}
			} else if (readList.size() <= (numOfNodes / 2)) {
				proposedValues.add(new Values<Object>(event.getValue()));
			} else if (!pv.contains(event.getValue())) {
				pv.add(new Values<Object>(event.getValue()));

				for (Address node : nodes) {
					if ((readList.get(node) != null)) {
						ArrayList<Values<?>> tmp = new ArrayList<Values<?>>();
						tmp.add(new Values<Object>(event.getValue()));
						AcceptMessage accMsg = new AcceptMessage(self, pts,
								tmp, pv.size() - 1, t);
						trigger(new FplSend(node, accMsg), fpl);
					}
				}
			}
		}
	};

	//[PREPARE, pts, al, t]
	//[PREPARE, ts, l, t']
	private Handler<PrepareMessage> prepareMsgHandler = new Handler<PrepareMessage>() {

		@Override
		public void handle(PrepareMessage event) {
			logger.info("prepareMsgHandler - Received PREPARE message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getPts() < prepts) {
				NAckMessage nAckMsg = new NAckMessage(self, event.getPts(), t);
				trigger(new FplSend(event.getSource(), nAckMsg), fpl);
			} else {
				prepts = new Integer(event.getPts());
				ArrayList<Values<?>> suf = new ArrayList<Values<?>>(av.subList(
						event.getAl(), av.size()));
				PrepareAckMessage prepAckMsg = new PrepareAckMessage(self,
						event.getPts(), ats, suf, al, t);
				trigger(new FplSend(event.getSource(), prepAckMsg), fpl);
			}
		}
	};
	
	private Handler<NAckMessage> nAckMsgHandler = new Handler<NAckMessage>() {
		@Override
		public void handle(NAckMessage event) {
			t = Integer.max(t, event.getT()) + 1;
			if (event.getTs().equals(pts)) {
				pts = 0;
				trigger(new AscAbort(), asc);
			}
		}
	};
	
	//[PREPAREACK, ts, ats, suffix(av, l), al, t]
	//[PREPAREACK, pts', ts, vsuf, l, t']
	private Handler<PrepareAckMessage> prepAckMsgHandler = new Handler<PrepareAckMessage>() {
		@Override
		public void handle(PrepareAckMessage event) {
			logger.info("prepAckMsgHandler - Received PREPARE-ACK message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getTs().equals(pts)) {
				 
				readList.put(event.getSource(), new ReadItem(event.getAts(), event.getSeq()));
				decided.put(event.getSource(), event.getAl());
				
				if (readList.size() == ((numOfNodes / 2) + 1)) {
					tsPrime = new Integer(0);
					vSufPrime = new ArrayList<Values<?>>();
					
					System.err.println("Start printing");
					readList.forEach(new BiConsumer<Address, ReadItem>() {

						@Override
						public void accept(Address t, ReadItem u) {
							System.err.println("Key: " + t.getId() + " Value: " + u.getVsuf());
							if ( (tsPrime < u.getTs()) || ( (tsPrime.equals(u.getTs()) && (vSufPrime.size() < u.getVsuf().size()) ) ) ) {
								tsPrime = new Integer(u.getTs());
								vSufPrime = new ArrayList<Values<?>>(u.getVsuf());
							}
						}
					});
					
					pv.addAll(vSufPrime);
					
					for (Values<?> v : proposedValues) {
						if (!pv.contains(v)) {
							pv.add(v);
						}
					}
					
					for (Address node : nodes) {
						if (readList.get(node) != null) {
							Integer lPrime = new Integer(decided.get(node));
							ArrayList<Values<?>> suf = new ArrayList<Values<?>>(pv.subList(lPrime, pv.size()));
							AcceptMessage accMsg = new AcceptMessage(self, pts, suf, lPrime, t);
							trigger(new FplSend(node, accMsg), fpl);
						}
					}
				} else if (readList.size() > ((numOfNodes / 2) + 1)) {
					ArrayList<Values<?>> suf = new ArrayList<Values<?>>(pv.subList(event.getAl(), pv.size()));
					AcceptMessage accMsg = new AcceptMessage(self, pts, suf, event.getAl(), t);
					trigger(new FplSend(event.getSource(), accMsg), fpl);
					if (!pl.equals(0)) {
						DecideMessage decMsg = new DecideMessage(self, pts, pl, t);
						trigger(new FplSend(event.getSource(), decMsg), fpl);
					}
				}
			}
		}
	};
	
	//[ACCEPT, pts, suffix(), l, t]
	//[ACCEPT, ts, vsuf, offs, t']
	private Handler<AcceptMessage> accMsg = new Handler<AcceptMessage>() {
		
		@Override
		public void handle(AcceptMessage event) {
			logger.info("accMsg - Received ACCEPT message");
			t = Integer.max(t, event.getT()) + 1;
			if (!event.getPts().equals(prepts)) {
				NAckMessage nAckMsg = new NAckMessage(self, event.getPts(), t);
				trigger(new FplSend(event.getSource(), nAckMsg), fpl);
			} else {
				ats = new Integer(event.getPts());
				if (event.getPvSize() < av.size()) {
					//System.err.println("Truncate av!");
					av = new ArrayList<Values<?>>(av.subList(0, event.getPvSize()));
				}
				
				av.addAll(event.getV());
				//System.err.println("av size: " + av.size());
				AcceptAckMessage accAckMsg = new AcceptAckMessage(self, event.getPts(), av.size(), t);
				trigger(new FplSend(event.getSource(), accAckMsg), fpl);
			}
		}
	};
	
	//[ACCEPTACK, ts, #av, t]
	//[ACCEPTACK, pts', l, t']
	private Handler<AcceptAckMessage> accAckMsgHandler = new Handler<AcceptAckMessage>() {
		@Override
		public void handle(AcceptAckMessage event) {
			logger.info("accAckMsgHandler - Received ACCEPT-ACK message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getTs().equals(pts)) {
				accepted.put(event.getSource(), event.getAvSize());
				
				counter = new Integer(0);
				
				accepted.forEach(new BiConsumer<Address, Integer>() {
					@Override
					public void accept(Address t, Integer u) {
						if (u >= event.getAvSize()) {
							counter++;
						}
					}
				});
				
				if ( (pl < event.getAvSize()) && (counter > (numOfNodes / 2))) {
					pl = new Integer(event.getAvSize());
					for (Address node : nodes) {
						if ((readList.get(node) != null) ) {
							DecideMessage decMsg = new DecideMessage(self, pts, pl, t);
							trigger(new FplSend(node, decMsg), fpl);
						}
					}
				}
			}
		}
	};
	
	//[DECIDE, pts, pl, t]
	//[DECIDE, ts, l, t']
	private Handler<DecideMessage> decMsgHandler = new Handler<DecideMessage>() {
		@Override
		public void handle(DecideMessage event) {
			t = Integer.max(t, event.getT()) + 1;
			Integer ts = new Integer(event.getPts());
			Integer l = new Integer(event.getPl());
			
			if (ts.equals(prepts)) {
				while (al < l ) {
					//System.err.println("getPl: " + event.getPl());
					//System.err.println("al: " + al);
					AscDecide ascDecMsg = new AscDecide(av.get(al).getValue());
					trigger(ascDecMsg, asc);
					al++;
				}
			}
		}
	};
}
