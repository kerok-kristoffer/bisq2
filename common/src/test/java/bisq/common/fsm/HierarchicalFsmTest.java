package bisq.common.fsm;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class HierarchicalFsmTest {

    @Test
    void testTransitions() {
        // reusing MockModel and Events from FsmTest for now, until (if) we need more complex model
        FsmTest.MockModel model = new FsmTest.MockModel(FsmTest.MockState.INIT);
        HierarchicalFsm<FsmTest.MockModel> fsm = new HierarchicalFsm<>(model);
        fsm.subFsmA.addTransition()
                .from(FsmTest.MockState.INIT)
                .on(FsmTest.MockEvent1.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S1);
        fsm.addTransition()
                .from(FsmTest.MockState.S1)
                .on(FsmTest.MockEvent1.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S2);
        fsm.subFsmB.addTransition()
                .from(FsmTest.MockState.S2)
                .on(FsmTest.MockEvent1.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S3);
        fsm.addTransition()
                .from(FsmTest.MockState.S3)
                .on(FsmTest.MockEvent1.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.COMPLETED);

        // todo - this needs to be handled more elegantly in the actual prototype - could be done in constructor, which requires subModule Transitions to be defined before parent Instance is created
        // this is one of two(?) options of handling subFsm transitions
            // this is a simple implementation that just registers all subFsm transitions in the parent Fsm - separation is only in creating the protocols
            // the other option is to let the Fsm handle() method look for subFsm transitions before throwing an exception and setting State to Error
                // this would require a more complex implementation of the Fsm.handle() method
                // pros are more separation of concerns, cons are more complexity and potential impact on BisqEasy cases
        fsm.registerAllSubTransitions();

        fsm.handle(new FsmTest.MockEvent1(model, "test1"));
        assertEquals(FsmTest.MockState.S1, model.getState());
        assertEquals("test1", model.data);

        fsm.handle(new FsmTest.MockEvent1(model, "test2"));
        assertEquals(FsmTest.MockState.S2, model.getState());
        assertEquals("test2", model.data);

        fsm.handle(new FsmTest.MockEvent1(model, "test3"));
        assertEquals(FsmTest.MockState.S3, fsm.getModel().getState());
        assertEquals("test3", model.data);

        fsm.handle(new FsmTest.MockEvent1(model, "test4"));
        assertEquals(FsmTest.MockState.COMPLETED, fsm.getModel().getState());
        assertEquals("test4", model.data);


        model = new FsmTest.MockModel(FsmTest.MockState.INIT);
        fsm = new HierarchicalFsm<>(model);

        // No change in data as no handler was defined
        fsm.subFsmA.addTransition()
                .from(FsmTest.MockState.INIT)
                .on(FsmTest.MockEvent1.class)
                .to(FsmTest.MockState.S1);

        fsm.subFsmA.addTransition() // defining all transitions ahead of time to avoid duplicates when running registerSubTransitions
                .from(FsmTest.MockState.S1)
                .on(FsmTest.MockEvent2.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S2);

        fsm.subFsmA.addTransition()
                .from(FsmTest.MockState.S2)
                .on(FsmTest.MockEvent2.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S3);
        fsm.registerAllSubTransitions();

        assertEquals(FsmTest.MockState.INIT, model.getState());
        assertNull(model.data);
        fsm.handle(new FsmTest.MockEvent1(model, "test1"));
        assertEquals(FsmTest.MockState.S1, model.getState());
        assertNull(model.data);

        // Transit with event handler called
        fsm.handle(new FsmTest.MockEvent2(model, "test2"));
        assertEquals(FsmTest.MockState.S2, model.getState());
        assertEquals("test2", model.data);

        // Different source, same event
        fsm.handle(new FsmTest.MockEvent2(model, "test3"));
        assertEquals(FsmTest.MockState.S3, model.getState());
        assertEquals("test3", model.data);

    }

    @Test
    void testUnregisteredSubFsmTransitions() {
        FsmTest.MockModel model = new FsmTest.MockModel(FsmTest.MockState.INIT);
        HierarchicalFsm<FsmTest.MockModel> fsm = new HierarchicalFsm<>(model);
        fsm.subFsmA.addTransition()
                .from(FsmTest.MockState.INIT)
                .on(FsmTest.MockEvent1.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S1);

        try {
            fsm.handle(new FsmTest.MockEvent1(model, "test1"));
        } catch (FsmException e) {
            assertEquals(State.FsmState.ERROR, model.getState());
            assertEquals("No transition found for event class bisq.common.fsm.FsmTest$MockEvent1 from state INIT", e.getMessage());
            log.info("Expected exception: {}", e.getMessage());
        }
        assertEquals(State.FsmState.ERROR, model.getState());
        assertNull(model.data);
        assertEquals(0, model.getEventQueue().size());
        assertEquals(0, model.getProcessedEvents().size());

    }

    @Test
    void testOutOfOrderEvents() {
        FsmTest.MockModel model = new FsmTest.MockModel(FsmTest.MockState.INIT);
        HierarchicalFsm<FsmTest.MockModel> fsm = new HierarchicalFsm<>(model);

        fsm.subFsmA.addTransition()
                .from(FsmTest.MockState.INIT)
                .on(FsmTest.MockEvent1.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S1);
        fsm.addTransition()
                .from(FsmTest.MockState.S1)
                .on(FsmTest.MockEvent2.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S2);
        fsm.subFsmB.addTransition()
                .from(FsmTest.MockState.S2)
                .on(FsmTest.MockEvent3.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.S3);
        fsm.addTransition()
                .from(FsmTest.MockState.S3)
                .on(FsmTest.MockEvent4.class)
                .run(FsmTest.MockEventHandler.class)
                .to(FsmTest.MockState.COMPLETED);

        fsm.registerAllSubTransitions();

        fsm.handle(new FsmTest.MockEvent3(model, "test3"));
        assertEquals(FsmTest.MockState.INIT, model.getState());
        assertNull(model.data);
        assertEquals(1, model.getEventQueue().size());
        assertEquals(0, model.getProcessedEvents().size());

        fsm.handle(new FsmTest.MockEvent2(model, "test2"));
        assertEquals(FsmTest.MockState.INIT, model.getState());
        assertNull(model.data);
        assertEquals(2, model.getEventQueue().size());
        assertEquals(0, model.getProcessedEvents().size());

        fsm.handle(new FsmTest.MockEvent1(model, "test1"));
        assertEquals(FsmTest.MockState.S3, model.getState());
        assertEquals("test3", model.data);
        assertEquals(0, model.getEventQueue().size());
        assertEquals(3, model.getProcessedEvents().size());

        fsm.handle(new FsmTest.MockEvent4(model, "test_comp"));
        assertEquals(FsmTest.MockState.COMPLETED, model.getState());
        assertEquals("test_comp", model.data);
        assertEquals(0, model.getEventQueue().size());
        assertEquals(0, model.getProcessedEvents().size());
    }

}
