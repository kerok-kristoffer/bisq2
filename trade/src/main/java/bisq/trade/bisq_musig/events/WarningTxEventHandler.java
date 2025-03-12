package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;
import bisq.trade.TradeService;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.protocol.modules.ConflictBranchProtocol;
import bisq.trade.protocol.events.SendTradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WarningTxEventHandler extends SendTradeMessageHandler<BisqMuSigTrade> {
    private final TradeService tradeService;


    public WarningTxEventHandler(TradeService serviceProvider,
                                 BisqMuSigTrade model) {
        super(serviceProvider, model);
        this.tradeService = serviceProvider;
    }

    @Override
    public void handle(Event event) {
        log.info("Warning Tx Event triggered {}", event.toString());
        ConflictBranchProtocol conflictBranchProtocol = new ConflictBranchProtocol(serviceProvider, trade);

        // utilizing TradeService to access the tradeProtocolById map for prototyping purposes
            // accessing from ServiceProvider might need a change in the interface
        tradeService.getBisqMusigTradeService().getTradeProtocolById().put(trade.getId(), conflictBranchProtocol);
    }
}
