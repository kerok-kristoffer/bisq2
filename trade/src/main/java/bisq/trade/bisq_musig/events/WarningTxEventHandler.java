package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.protocol.events.TradeEventHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WarningTxEventHandler extends TradeEventHandler<BisqMuSigTrade> {


    public WarningTxEventHandler(ServiceProvider serviceProvider,
                                 BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        log.info("Warning Tx Event triggered {}", event.toString());
    }
}
