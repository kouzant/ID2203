package se.kth.ict.id2203.components.riwcm;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.kth.ict.id2203.ports.ar.ArReadRequest;
import se.kth.ict.id2203.ports.ar.ArReadResponse;
import se.kth.ict.id2203.ports.ar.ArWriteRequest;
import se.kth.ict.id2203.ports.ar.ArWriteResponse;
import se.kth.ict.id2203.ports.ar.AtomicRegister;
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

public class ReadImposeWriteConsultMajority extends ComponentDefinition {

	private static final Logger logger = LoggerFactory
			.getLogger(ReadImposeWriteConsultMajority.class);

	private Positive<BestEffortBroadcast> beb = requires(BestEffortBroadcast.class);
	private Positive<PerfectPointToPointLink> pp2p = requires(PerfectPointToPointLink.class);
	private Negative<AtomicRegister> ar = provides(AtomicRegister.class);

	private final Address self;
	private final Set<Address> nodes;
	private final Integer numberOfNodes;
	private List<ReadObject> readList;
	private Integer rid;
	private Integer acks;
	private Integer wr, val, writeVal, readVal;
	private Integer ts;
	private Boolean reading;
	private WriteBebDataMessage wBebMsg;
	private Integer rr;
	private Integer maxts;

	public ReadImposeWriteConsultMajority(
			ReadImposeWriteConsultMajorityInit event) {
		this.self = event.getSelfAddress();
		this.nodes = new HashSet<Address>(event.getAllAddresses());
		this.readList = new LinkedList<ReadObject>();
		this.numberOfNodes = this.nodes.size();
		this.rid = 0;
		this.acks = 0;
		this.reading = false;
		this.ts = 0;
		this.wr = 0;
		this.val = 0;
		this.writeVal = null;
		this.readVal = null;
		this.rr = 0;
		this.maxts = -1;
		
		subscribe(startHandler, control);
		
		subscribe(readRequest, ar);
		subscribe(writeRequest, ar);
		
		subscribe(bebDeliver, beb);
		subscribe(writeValue, beb);
		
		subscribe(arDeliver, pp2p);
		subscribe(ackHandler, pp2p);
	}

	private Handler<Start> startHandler = new Handler<Start>() {

		@Override
		public void handle(Start event) {
			logger.info("Atomic Register component started");
		}
	};

	private Handler<ArReadRequest> readRequest = new Handler<ArReadRequest>() {

		@Override
		public void handle(ArReadRequest event) {
			//logger.info("Got read request from Application");
			rid++;
			acks = 0;
			readList.clear();
			reading = true;
			ReadBebDataMessage bebMessage = new ReadBebDataMessage(self, rid);
			trigger(new BebBroadcast(bebMessage), beb);
		}
	};

	private Handler<ReadBebDataMessage> bebDeliver = new Handler<ReadBebDataMessage>() {

		@Override
		public void handle(ReadBebDataMessage event) {
			//logger.info("Delivered BEB message {}", event.getR());
			ArDataMessage arMessage = new ArDataMessage(self, event.getR(),
					ts, wr, val);
			trigger(new Pp2pSend(event.getSource(), arMessage), pp2p);
		}
	};

	private Handler<ArDataMessage> arDeliver = new Handler<ArDataMessage>() {

		@Override
		public void handle(ArDataMessage event) {
			if (event.getR().equals(rid)) {
				readList.add(new ReadObject(event.getTs(), event.getWr(), event.getVal(), event.getSource().getId()));
				
				if (readList.size() > (numberOfNodes / 2)) {
					// Sort of readList based on 'ts' or on 'wr'
					Collections.sort(readList, new Comparator<ReadObject>() {
						@Override
						public int compare(ReadObject obj0, ReadObject obj1) {
							if (obj0.getTs().equals(obj1.getTs())) {
								return obj0.getNodeId() < obj1.getNodeId() ? -1 : 1;
							} else if (obj0.getTs() < obj1.getTs()) {
								return -1;
							} else {
								return 1;
							}
						}
					});

					ReadObject highest = readList.get(readList.size() - 1);

					rr = new Integer(highest.getWr());
					readVal = new Integer(highest.getVal());
					maxts = new Integer(highest.getTs());

					readList.clear();

					if (reading) {
						// trigger <beb, Broadcast | [Write, rid, maxts, rr,
						// readval]>;
						wBebMsg = new WriteBebDataMessage(
								self, rid, maxts, rr, readVal);
						trigger(new BebBroadcast(wBebMsg), beb);
					} else {
						// trigger <beb, Broadcast | [Write, rid, maxts + 1,
						// rank(self ), writeval]>;
						wBebMsg = new WriteBebDataMessage(
								self, rid, maxts + 1, self.getId(),
								writeVal);
						trigger(new BebBroadcast(wBebMsg), beb);
					}
				}
			}
		}
	};

	private Handler<ArWriteRequest> writeRequest = new Handler<ArWriteRequest>() {

		@Override
		public void handle(ArWriteRequest event) {
			rid++;
			writeVal = event.getValue();
			acks = 0;
			readList.clear();
			ReadBebDataMessage bebMessage = new ReadBebDataMessage(self, rid);
			trigger(new BebBroadcast(bebMessage), beb);
		}
	};

	private Handler<WriteBebDataMessage> writeValue = new Handler<WriteBebDataMessage>() {

		@Override
		public void handle(WriteBebDataMessage event) {
			//How da fuck Java evaluate expressions
			/*if ((event.getTs() > ts) || (event.getWr() > wr)) {
				ts = event.getTs();
				wr = event.getWr();
				val = event.getVal();
			}*/
			if (event.getTs().equals(ts)) {
				if (event.getWr() > wr) {
					ts = event.getTs();
					wr = event.getWr();
					val = event.getVal();
				}
			} else if (event.getTs() > ts) {
				ts = event.getTs();
				wr = event.getWr();
				val = event.getVal();
			}
			// trigger ACK
			//logger.info("Sending ACK to: {}", event.getSource());
			trigger(new Pp2pSend(event.getSource(), new AckMessage(self,
					event.getR())), pp2p);
		}
	};
	
	private Handler<AckMessage> ackHandler = new Handler<AckMessage>() {

		@Override
		public void handle(AckMessage event) {
			if (event.getR().equals(rid)) {
				acks++;
				//logger.info("Number of ACKs received: {}", acks);
				if (acks > (numberOfNodes / 2)) {
					//logger.info("Gathered the required ACKs!");
					acks = 0;
					
					if (reading) {
						reading = false;
						ArReadResponse resp = new ArReadResponse(readVal);
						//logger.info("Sending ReadResponse with value: {}", readVal);
						trigger(resp, ar);
					} else {
						//logger.info("Sending WriteResponse with value");
						trigger(new ArWriteResponse(), ar);
					}
				}
			}
		}
	};
}
