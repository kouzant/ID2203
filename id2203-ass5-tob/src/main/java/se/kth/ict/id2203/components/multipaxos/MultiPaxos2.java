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

public class MultiPaxos2 extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(MultiPaxos.class);

	private Negative<AbortableSequenceConsensus> asc = provides(AbortableSequenceConsensus.class);
	private Positive<FIFOPerfectPointToPointLink> fpl = requires(FIFOPerfectPointToPointLink.class);

	private final Address self;
	private final Set<Address> nodes;
	private final Integer numberOfNodes;
	private Integer t, prepts, ats, al, pts, pl, tsPrime, counter;
	private ArrayList<Object> av, pv, proposedValues, vsufPrime;
	private HashMap<Address, ReadItem> readList;
	private HashMap<Address, Integer> accepted;
	private HashMap<Address, Integer> decided;

	public MultiPaxos2(MultiPaxosInit event) {
		self = event.getSelfAddress();
		nodes = new HashSet<Address>(event.getAllAddresses());
		numberOfNodes = nodes.size();
		t = 0;
		prepts = 0;
		ats = 0;
		al = 0;
		pts = 0;
		pl = 0;
		counter = 0;
		av = new ArrayList<Object>();
		pv = new ArrayList<Object>();
		proposedValues = new ArrayList<Object>();
		readList = new HashMap<Address, ReadItem>();
		accepted = new HashMap<Address, Integer>();
		decided = new HashMap<Address, Integer>();

		subscribe(startHandler, control);

		subscribe(proposeHandler, asc);

		subscribe(prepareMsgHandler, fpl);
		subscribe(nAckMsgHandler, fpl);
		subscribe(prepAckMsgHandler, fpl);
		subscribe(acceptHandler, fpl);
		subscribe(accAckMessage, fpl);
		subscribe(decideHandler, fpl);
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
			logger.info("proposeHandler - Received a PROPOSAL command!");
			t++;

			if (pts.equals(0)) {
				logger.info("pts equals 0");
				pts = t * numberOfNodes + self.getId();
				pv = new ArrayList<Object>(av.subList(0, al));
				pl = 0;
				ArrayList<Object> tmp = new ArrayList<Object>();
				tmp.add(event.getValue());
				proposedValues.addAll(tmp);
				readList.clear();
				accepted.clear();
				decided.clear();

				for (Address node : nodes) {
					PrepareMessage prepMsg = new PrepareMessage(self, pts, al,
							t);
					trigger(new FplSend(node, prepMsg), fpl);
				}
			} else if (readList.size() <= (numberOfNodes / 2)) {
				logger.info("proposeHandler - readList <= N/2");
				ArrayList<Object> tmp = new ArrayList<Object>();
				tmp.add(event.getValue());
				proposedValues.addAll(tmp);
				//proposedValues.add(new Values<Object>(event.getValue()));
			} else if (!pv.contains(event.getValue())) {
				logger.info("proposeHandler - pv does not contain proposed value");
				//pv.add(new Values<Object>(event.getValue()));
				ArrayList<Object> tmp = new ArrayList<Object>();
				tmp.add(event.getValue());
				pv.addAll(tmp);

				for (Address node : nodes) {
					if (readList.get(node) != null) {
						ArrayList<Object> valSeq = new ArrayList<Object>();
						valSeq.add(event.getValue());

						AcceptMessage accMsg = new AcceptMessage(self, pts,
								valSeq, pv.size() - 1, t);
						logger.info("proposeHandler - sending AcceptMessage");
						trigger(new FplSend(node, accMsg), fpl);
					}
				}
			}
		}
	};

	private Handler<PrepareMessage> prepareMsgHandler = new Handler<PrepareMessage>() {

		@Override
		public void handle(PrepareMessage event) {
			logger.info("prepareMsgHandler - Received a PREPARE event!");
			t = Integer.max(t, event.getT()) + 1;

			if (event.getPts() < prepts) {
				logger.info("prepareMsgHandler - getPts < prepts");
				NAckMessage nAckMsg = new NAckMessage(self, event.getPts(), t);
				logger.info("prepareMsgHandler - Sending N-ACK message");
				trigger(new FplSend(event.getSource(), nAckMsg), fpl);
			} else {
				logger.info("prepareMsgHandler - getPts >= prepts");
				prepts = new Integer(event.getPts());
				
				ArrayList<Object> suf = new ArrayList<Object>(av.subList(event.getAl(), av.size()));
				logger.info("prepareMsgHanlder - Sending Prepare-ACK message");
				PrepareAckMessage prepAckMsg = new PrepareAckMessage(self,
						event.getPts(), ats, suf, al, t);
				trigger(new FplSend(event.getSource(), prepAckMsg), fpl);
			}
		}
	};

	private Handler<NAckMessage> nAckMsgHandler = new Handler<NAckMessage>() {

		@Override
		public void handle(NAckMessage event) {
			logger.info("nAckMsgHandler - Received N-ACK message!");
			t = Integer.max(t, event.getT()) + 1;
			if (pts.equals(event.getTs())) {
				pts = 0;
				logger.info("nAckMsgHandler - Sending ABORT to app");
				trigger(new AscAbort(), asc);
			}
		}
	};

	private Handler<PrepareAckMessage> prepAckMsgHandler = new Handler<PrepareAckMessage>() {

		@Override
		public void handle(PrepareAckMessage event) {
			logger.info("prepAckMsgHandler - Received a PREPARE-ACK message");
			t = Integer.max(t, event.getT()) + 1;
			
			if (event.getTs().equals(pts)) {
				logger.info("prepAckMsgHandler - pts' == pts");
				readList.put(event.getSource(), new ReadItem(event.getAts(),
						event.getSeq()));
				decided.put(event.getSource(), event.getAl());

				if (readList.size() == ((numberOfNodes / 2) + 1)) {
					logger.info("prepAckMsgHandler - readlist == N/2 + 1");
					tsPrime = new Integer(0);
					vsufPrime = new ArrayList<Object>();

					readList.forEach(new BiConsumer<Address, ReadItem>() {

						@Override
						public void accept(Address t, ReadItem u) {
							if ( (tsPrime < u.getTs())
									|| ( (tsPrime.equals(u.getTs())) && (vsufPrime
											.size() < u.getVsuf().size()) ) ) {
								logger.info("prepAckMsgHandler - inside big if statement");
								tsPrime = new Integer(u.getTs());
								vsufPrime = new ArrayList<Object>(u
										.getVsuf());
							}
						}
					});

					pv.addAll(vsufPrime);

					for (Object v : proposedValues) {
						if (!pv.contains(v)) {
							logger.info("prepAckMsgHandler - pv does not contain v");
							//pv.add(v);
							
							ArrayList<Object> tmp = new ArrayList<Object>();
							tmp.add(v);
							pv.addAll(tmp);
						}
					}

					for (Address node : nodes) {
						if (readList.get(node) != null) {
							logger.info("prepAckMsgHandler - readList node not null");
							Integer lPrime = decided.get(node);
							ArrayList<Object> suf = new ArrayList<Object>(
									pv.subList(lPrime, pv.size()));
							AcceptMessage accMsg = new AcceptMessage(self, pts,
									suf, lPrime, t);
							logger.info("prepAxkMsgHandler - Sending ACCEPT message");
							trigger(new FplSend(node, accMsg), fpl);
						}
					}
				} else if (readList.size() > ((numberOfNodes / 2) + 1)) {
					logger.info("prepAckMsgHandler - readlist > N/2+1");
					ArrayList<Object> suf = new ArrayList<Object>(
							pv.subList(event.getAl(), pv.size()));
					AcceptMessage accMsg = new AcceptMessage(self, pts, suf,
							event.getAl(), t);
					logger.info("prepAckMsgHandler - sending ACCEPT message");
					trigger(new FplSend(event.getSource(), accMsg), fpl);
					if (!pl.equals(0)) {
						logger.info("Sending DECIDE message");
						DecideMessage decMsg = new DecideMessage(self, pts, pl,
								t);
						trigger(new FplSend(event.getSource(), decMsg), fpl);
					}
				}
			}
		}
	};

	private Handler<AcceptMessage> acceptHandler = new Handler<AcceptMessage>() {

		@Override
		public void handle(AcceptMessage event) {
			logger.info("acceptHandler - Received ACCEPT message");
			t = Integer.max(t, event.getT()) + 1;
			if (!event.getPts().equals(prepts)) {
				logger.info("acceptHandler - Sending N-ACK message");
				NAckMessage nackMsg = new NAckMessage(self, event.getPts(), t);
				trigger(new FplSend(event.getSource(), nackMsg), fpl);
			} else {
				ats = new Integer(event.getPts());
				if (event.getPvSize() < av.size()) {
					logger.info("acceptHandler - PvSize < av.size");
					av.subList(0, event.getPvSize()).clear();
				}
				av.addAll(event.getV());
				logger.info("acceptHandler - Sending ACCEPT-ACK message");
				AcceptAckMessage accAckMsg = new AcceptAckMessage(self,
						event.getPts(), av.size(), t);
				trigger(new FplSend(event.getSource(), accAckMsg), fpl);
			}
		}
	};
	
	private Handler<AcceptAckMessage> accAckMessage = new Handler<AcceptAckMessage>() {

		@Override
		public void handle(AcceptAckMessage event) {
			logger.info("accAckMessage - Received ACCEPT-ACK message");
			t = Integer.max(t, event.getT()) + 1;
			if (pts.equals(event.getTs())) {
				logger.info("accAckMessage - pts == getTs");
				accepted.put(event.getSource(), event.getAvSize());
				
				counter = new Integer(0);
				
				accepted.forEach(new BiConsumer<Address, Integer>() {
					@Override
					public void accept(Address t, Integer u) {
						if (u >= event.getAvSize()) {
							logger.info("accAckMessage - counter");
							counter++;
						}
					}
				});
				
				if ( (pl < event.getAvSize()) && (counter > (numberOfNodes / 2)) ){
					pl = new Integer(event.getAvSize());
					
					for (Address node : nodes) {
						if (readList.get(node) != null) {
							logger.info("accAckMessage - Sending DECIDE message");
							DecideMessage decMsg = new DecideMessage(self, pts, pl, t);
							trigger(new FplSend(node, decMsg), fpl);
						}
					}
				}
			}
		}
	};
	
	private Handler<DecideMessage> decideHandler = new Handler<DecideMessage>() {

		@Override
		public void handle(DecideMessage event) {
			logger.info("decideHandler - Received DECIDE message");
			t = Integer.max(t, event.getT()) + 1;
			if (event.getPts().equals(prepts)) {
				logger.info("decideHandler - eventPl: {}", event.getPl());
				System.err.println("decideHandler - eventPl: " + event.getPl());
				System.err.println("av size: " + av.size());
				while (al < event.getPl()) {
					System.err.println("al: " + al);
					AscDecide ascDecMsg = new AscDecide(av.get(al));
					logger.info("Sending DECIDE to app");
					al++;
					trigger(ascDecMsg, asc);
				}
				/*for (int i = al; i < event.getPl(); i++){
					logger.info("al: {}", al);
					AscDecide ascDecMsg = new AscDecide(av.get(i).getValue());
					logger.info("Sending DECIDE to app");
					trigger(ascDecMsg, asc);
				}*/
			}
		}
	};
}
