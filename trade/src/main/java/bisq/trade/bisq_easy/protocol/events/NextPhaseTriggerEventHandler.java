package bisq.trade.bisq_easy.protocol.events;

import bisq.common.fsm.Event;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.protocol.events.SendTradeMessageHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NextPhaseTriggerEventHandler extends SendTradeMessageHandler<BisqMuSigTrade> { // TODO need to create actual TransitionHandler class
    public NextPhaseTriggerEventHandler(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void handle(Event event) {
        log.info("Transition to next phase module triggered by event: {}", event.toString());
    }
}
