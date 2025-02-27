package bisq.common.fsm.modules;

import bisq.common.fsm.*;

import java.lang.reflect.InvocationTargetException;

public class SubFsmB<M extends FsmModel> extends Fsm<M> {
    public SubFsmB(M model) {
        super(model);
    }

    @Override
    protected void configErrorHandling() {
        fromAny()
                .on(FsmErrorEvent.class)
                .to(State.FsmState.ERROR);
    }

    @Override
    protected void configTransitions() {

    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return null;
    }

    @Override
    public void handle(Event event) {
        try {
            super.handle(event);
        } catch (FsmException fsmException) {
            // We swallow the exception

        }
    }
}
