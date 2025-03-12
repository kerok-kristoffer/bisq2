package bisq.trade.bisq_musig.protocol.modules;

import bisq.common.fsm.EventHandler;
import bisq.common.fsm.Fsm;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;

import java.lang.reflect.InvocationTargetException;

public abstract class ModuleFsm extends Fsm<BisqMuSigTrade> {
    private final ServiceProvider serviceProvider;

    protected ModuleFsm(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(model);
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        try {
            return handlerClass.getDeclaredConstructor(ServiceProvider.class, BisqMuSigTrade.class).newInstance(serviceProvider, model);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
