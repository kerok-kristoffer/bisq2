package bisq.trade.bisq_musig.protocol.modules;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;

public class KeyExchangeModuleFSM extends ModuleFsm {

    public KeyExchangeModuleFSM(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    @Override
    protected void configErrorHandling() {

    }

    @Override
    public void configTransitions() {

    }
}
