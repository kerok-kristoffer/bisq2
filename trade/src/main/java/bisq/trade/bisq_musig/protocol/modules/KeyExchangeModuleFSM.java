package bisq.trade.bisq_musig.protocol.modules;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.BisqMuSigDummyEvent;
import bisq.trade.bisq_musig.events.BisqMuSigDummyEventHandler;
import bisq.trade.bisq_musig.protocol.BisqMuSigProtocol;

import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;

public class KeyExchangeModuleFSM extends BisqMuSigProtocol {
    public KeyExchangeModuleFSM(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void configTransitions() {
        from(KEY_EXCHANGE_INIT)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(KEY_EXCHANGE_COMPLETE);
    }
}
