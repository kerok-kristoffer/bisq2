package bisq.trade.bisq_musig.protocol.modules;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;

public class FinalizationModuleFSM extends ModuleFsm {


    public FinalizationModuleFSM(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void configErrorHandling() {

    }

    @Override
    public void configTransitions() {


    }
}
