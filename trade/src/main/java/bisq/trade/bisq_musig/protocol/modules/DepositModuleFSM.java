package bisq.trade.bisq_musig.protocol.modules;

import bisq.common.fsm.EventHandler;
import bisq.common.fsm.Fsm;
import bisq.common.fsm.FsmErrorEvent;
import bisq.trade.bisq_easy.protocol.events.BisqEasyFsmErrorEventHandler;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.*;
import bisq.trade.bisq_musig.protocol.BisqMuSigTradeState;

import java.lang.reflect.InvocationTargetException;

import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;

public class DepositModuleFSM extends Fsm<BisqMuSigTrade> {

    public DepositModuleFSM(BisqMuSigTrade model) {
        super(model);
        // todo config Transitions here or keep it in the parent class?
            // keeping it in parent class gives a clearer overview of the whole protocol
    }

    @Override
    public void configTransitions() {
        from(DEPOSIT_INIT)
                .on(BisqMusigConstructPresignedTransactionEvent.class)
                .run(BisqMusigConstructPresignedTransactionEventHandler.class)
                .to(PRE_SIGNED_TRANSACTIONS_RECEIVED);

        from(PRE_SIGNED_TRANSACTIONS_RECEIVED)
                .on(BisqMuSigKeyAggregationEvent.class)
                .run(BisqMuSigKeyAggregationEventHandler.class)
                .to(KEY_AGGREGATED);

        // add steps for signing and broadcasting transactions and Atomic Swap step.
        from(KEY_AGGREGATED)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(DEPOSIT_COMPLETED);
    }

    @Override
    protected void configErrorHandling() {
        // Define error handling transitions specific to the deposit phase.
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return null;
    }
}
