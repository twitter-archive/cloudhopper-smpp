package com.cloudhopper.smpp.demo.persist;

import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduResponse;

public class DummySmppClientMessageService implements SmppClientMessageService {

	/** delivery receipt, or MO */
	@Override
	public PduResponse received(OutboundClient client, DeliverSm deliverSm) {
		return deliverSm.createResponse();
	}

}
