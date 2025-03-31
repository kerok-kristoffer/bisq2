package bisq.trade.bisq_musig.protocol.modules;

import bisq.common.fsm.EventHandler;
import bisq.common.fsm.Fsm;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.BisqMuSigTrade;

import java.lang.reflect.InvocationTargetException;

public class ConflictModuleFSM extends Fsm<BisqMuSigTrade> {

    private final ServiceProvider serviceProvider;

    public ConflictModuleFSM(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(model);
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected void configErrorHandling() {

    }

    @Override
    protected void configTransitions() {
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        try {
            return handlerClass.getDeclaredConstructor(ServiceProvider.class, BisqMuSigTrade.class).newInstance(serviceProvider, model);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
