package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.protocol.events.TradeEventHandler;

public class BisqMuSigDummyEventHandler extends TradeEventHandler<BisqMuSigTrade> {
    public BisqMuSigDummyEventHandler(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {

    }
}
