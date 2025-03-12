package bisq.trade.bisq_musig.protocol.modules;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.*;
import bisq.trade.bisq_musig.protocol.BisqMuSigProtocol;

import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;

public class ConflictBranchProtocol extends BisqMuSigProtocol {

    public ConflictBranchProtocol(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void configErrorHandling() {

    }

    @Override
    protected void configTransitions() {

        from(WARNING_TX_RECEIVED)
                .on(T1ExpiredEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(WARNING_TX_RECEIVED_AND_T1_EXPIRED);

        // Branch 1 - if RedirectTx occurs before ClaimTx
        fromStates(WARNING_TX_RECEIVED, WARNING_TX_RECEIVED_AND_T1_EXPIRED)
                .on(RedirectTxEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(REDIRECT_TX_RECEIVED);

        // Branch 2 - if no redirect tx is received and t1 has expired, the claimTX can be received
        from(WARNING_TX_RECEIVED_AND_T1_EXPIRED)
                .on(ClaimTxEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(CLAIM_TX_RECEIVED);

        // some step to finalize the conflict resolution from either branch, should perhaps be split
        fromStates(REDIRECT_TX_RECEIVED, CLAIM_TX_RECEIVED)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(CONFLICT_RESOLVED);
    }

}
