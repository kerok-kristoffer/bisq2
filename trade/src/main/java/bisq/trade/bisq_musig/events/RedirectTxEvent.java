package bisq.trade.bisq_musig.events;

import bisq.common.fsm.Event;

public class RedirectTxEvent implements Event {

    @Override
    public String toString() {
        return "RedirectTxEvent";
    }
}
