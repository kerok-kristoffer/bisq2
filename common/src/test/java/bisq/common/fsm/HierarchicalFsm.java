package bisq.common.fsm;

import bisq.common.data.Pair;
import bisq.common.fsm.modules.SubFsmA;
import bisq.common.fsm.modules.SubFsmB;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
public class HierarchicalFsm<M extends FsmModel> extends Fsm<M> {
    private final List<Fsm<M>> subFsms = new LinkedList<>();


    public final SubFsmA<M> subFsmA;
    public final SubFsmB<M> subFsmB;

    protected HierarchicalFsm(M model) {
        super(model);

        subFsmA = new SubFsmA<>(model);
        subFsmB = new SubFsmB<>(model);
        subFsms.add(subFsmA);
        subFsms.add(subFsmB);
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

    public void registerSubTransitions(Map<Pair<State, Class<? extends Event>>, Transition> subTransitions) {
        getTransitionMap().putAll(subTransitions);
    }

    public void registerAllSubTransitions() {
        subFsms.forEach(subFsm -> getTransitionMap().putAll(subFsm.getTransitionMap()));
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return handlerClass.getDeclaredConstructor().newInstance();
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
