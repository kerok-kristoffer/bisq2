package bisq.trade.bisq_musig.protocol.modules;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;

public class FiatConfirmationModuleFSM  extends ModuleFsm {

    public FiatConfirmationModuleFSM(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void configErrorHandling() {

    }

    @Override
    public void configTransitions() {

    }
}
