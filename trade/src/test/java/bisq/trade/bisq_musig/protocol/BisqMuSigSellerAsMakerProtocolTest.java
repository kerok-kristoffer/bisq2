package bisq.trade.bisq_musig.protocol;

import bisq.common.fsm.Fsm;
import bisq.common.fsm.FsmException;
import bisq.contract.Party;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_musig.BisqMuSigOffer;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.BisqMuSigTradeService;
import bisq.trade.bisq_musig.events.*;
import bisq.trade.bisq_musig.protocol.modules.ConflictBranchProtocol;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
public class BisqMuSigSellerAsMakerProtocolTest {

    private TradeService serviceProvider;
    private BisqMuSigTrade model;
    private BisqMuSigSellerAsMakerProtocol protocol;
    private final Map<String, BisqMuSigProtocol> tradeProtocolById = new ConcurrentHashMap<>();

    @BeforeEach
    public void setUp() {
        serviceProvider = mock(TradeService.class); // not sure if TradeService is the appropriate ServiceProvider to use here, used for now to access tradeProtocolById without changing the ServiceProvider interface
        BisqMuSigTradeService bisqMuSigTradeService = mock(BisqMuSigTradeService.class);
        when(serviceProvider.getBisqMusigTradeService()).thenReturn(bisqMuSigTradeService);
        when(bisqMuSigTradeService.getTradeProtocolById()).thenReturn(tradeProtocolById);

        BisqMuSigContract mockContract = mock(BisqMuSigContract.class);
        Party mockMakerParty = mock(Party.class);
        when(mockContract.getMaker()).thenReturn(mockMakerParty);
        when(mockContract.getOffer()).thenReturn(mock(BisqMuSigOffer.class));
        NetworkId maker = mock(NetworkId.class);
        NetworkId taker = mock(NetworkId.class);
        when(mockMakerParty.getNetworkId()).thenReturn(maker);
        model = new BisqMuSigTrade(mockContract, false, false,  mock(Identity.class), taker);
        protocol = new BisqMuSigSellerAsMakerProtocol(serviceProvider, model);

        // orchestrating active Fsm likely to be handled in TradeService in actual implementation.
        tradeProtocolById.put(model.getId(), protocol);
    }

    @Test
    void testProtocolHappyPathFlow() {
        NextPhaseTriggerEvent phaseTriggerEvent = mock(NextPhaseTriggerEvent.class);
        protocol.handle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.handle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.handle(new BisqMuSigKeyAggregationEvent());
        assertEquals(KEY_AGGREGATED, model.getState());

        protocol.handle(new BisqMuSigDummyEvent()); // add steps for signing and broadcasting transactions and Atomic Swap step.
        assertEquals(DEPOSIT_COMPLETED, model.getState());

        protocol.handle(phaseTriggerEvent);
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        // following sub-protocol transitions yet to be implemented

        protocol.handle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.handle(phaseTriggerEvent);
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.handle(new BisqMuSigDummyEvent());
        assertEquals(KEY_EXCHANGE_COMPLETE, model.getState());

        protocol.handle(phaseTriggerEvent);
        assertEquals(FINALIZATION_INIT, model.getState());

        protocol.handle(new BisqMuSigDummyEvent());
        assertEquals(FINALIZATION_COMPLETE, model.getState());

        protocol.handle(phaseTriggerEvent);
        assertEquals(BTC_CONFIRMED, model.getState());
    }

    @Test
    void testWarningTxEventBranchWithRedirectTx() {

        assertEquals(INIT, model.getState());

        protocol.handle(new WarningTxEvent()); // allowing WarningTxEvent from any state for now, should be from specific states
        assertEquals(WARNING_TX_RECEIVED, model.getState());
        // orchestrating active Fsm likely to be handled in TradeService. Need to handle persistence as well
        Fsm<BisqMuSigTrade> currentFsm = tradeProtocolById.get(model.getId());
        assertInstanceOf(ConflictBranchProtocol.class, currentFsm);

        //call from main Fsm should result in an error
        try {
            currentFsm.handle(new NextPhaseTriggerEvent());
            fail("Should have thrown an exception");
        } catch (Exception e) {
            // assert that we get an IllegalArgumentException
            assertEquals(FsmException.class, e.getClass());
            assertEquals("java.lang.IllegalArgumentException: No transition found for given event FsmErrorEvent(fsmException=bisq.common.fsm.FsmException: java.lang.IllegalArgumentException: No transition found for given event NextPhaseTriggerEvent)", e.getMessage());
        }
        assertEquals(WARNING_TX_RECEIVED, model.getState());

        currentFsm.handle(new RedirectTxEvent());
        assertEquals(REDIRECT_TX_RECEIVED, model.getState());

        // T1 expired and ClaimTx has no effect when redirectTx is received, will be added to Event queue and discarded at the end of the cycle
        currentFsm.handle(new T1ExpiredEvent());
        assertEquals(REDIRECT_TX_RECEIVED, model.getState());
        currentFsm.handle(new ClaimTxEvent());
        assertEquals(REDIRECT_TX_RECEIVED, model.getState());

        // Finalize the conflict resolution
        currentFsm.handle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED, model.getState());
    }

    @Test
    void testWarningTxEventBranchNoRedirectTx() {
        assertEquals(INIT, model.getState());

        protocol.handle(new WarningTxEvent());
        assertEquals(WARNING_TX_RECEIVED, model.getState());

        Fsm<BisqMuSigTrade> currentFsm = tradeProtocolById.get(model.getId());
        assertInstanceOf(ConflictBranchProtocol.class, currentFsm);

        currentFsm.handle(new T1ExpiredEvent());
        assertEquals(WARNING_TX_RECEIVED_AND_T1_EXPIRED, model.getState());

        currentFsm.handle(new ClaimTxEvent());
        assertEquals(CLAIM_TX_RECEIVED, model.getState());

        // Finalize the conflict resolution
        currentFsm.handle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED, model.getState());
    }

    @Test
    void testClaimTxBeforeT1Expired() {
        assertEquals(INIT, model.getState());

        protocol.handle(new WarningTxEvent());
        assertEquals(WARNING_TX_RECEIVED, model.getState());

        Fsm<BisqMuSigTrade> currentFsm = tradeProtocolById.get(model.getId());
        assertInstanceOf(ConflictBranchProtocol.class, currentFsm);

        currentFsm.handle(new ClaimTxEvent()); // ClaimTxEvent needs to wait for T1ExpiredEvent to take effect
        assertEquals(WARNING_TX_RECEIVED, model.getState());

        currentFsm.handle(new T1ExpiredEvent()); // ClaimTxEvent already queued up, so it will be processed after T1ExpiredEvent
        assertEquals(CLAIM_TX_RECEIVED, model.getState());

        // Finalize the conflict resolution
        currentFsm.handle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED, model.getState());
    }
}
