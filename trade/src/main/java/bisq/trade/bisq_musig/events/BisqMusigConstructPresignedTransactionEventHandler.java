package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.protocol.events.TradeEventHandler;

public class BisqMusigConstructPresignedTransactionEventHandler extends TradeEventHandler<BisqMuSigTrade> {
    public BisqMusigConstructPresignedTransactionEventHandler(ServiceProvider serviceProvider,
                                                                 BisqMuSigTrade trade) {
        super(serviceProvider, trade);
    }

    @Override
    public void handle(Event event) {

    }
}
