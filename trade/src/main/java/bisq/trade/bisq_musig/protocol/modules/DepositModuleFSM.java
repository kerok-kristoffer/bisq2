package bisq.trade.bisq_musig.protocol.modules;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;

public class DepositModuleFSM extends ModuleFsm {

    public DepositModuleFSM(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    public void configTransitions() {

    }

    @Override
    protected void configErrorHandling() {
        // Define error handling transitions specific to the deposit phase.
    }
}
