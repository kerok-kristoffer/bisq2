package bisq.trade.bisq_musig.protocol;

import bisq.contract.Party;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_musig.BisqMuSigOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.*;
import bisq.trade.bisq_musig.messages.BisqMuSigCancelTradeMessage;
import bisq.trade.bisq_musig.messages.BisqMuSigRejectTradeMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static bisq.trade.bisq_musig.protocol.BisqMuSigProtocol.Phase.*;
import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
public class BisqMuSigSellerAsMakerProtocolTest {

    private BisqMuSigTrade model;
    private BisqMuSigSellerAsMakerProtocol protocol;

    @BeforeEach
    public void setUp() {
        ServiceProvider serviceProvider = mock(ServiceProvider.class);

        BisqMuSigContract mockContract = mock(BisqMuSigContract.class);
        Party mockMakerParty = mock(Party.class);
        when(mockContract.getMaker()).thenReturn(mockMakerParty);
        when(mockContract.getOffer()).thenReturn(mock(BisqMuSigOffer.class));
        NetworkId maker = mock(NetworkId.class);
        NetworkId taker = mock(NetworkId.class);
        when(mockMakerParty.getNetworkId()).thenReturn(maker);
        model = new BisqMuSigTrade(mockContract, false, false,  mock(Identity.class), taker);
        protocol = new BisqMuSigSellerAsMakerProtocol(serviceProvider, model);
    }

    @Test
    void testProtocolHappyPathFlowWithoutKeyExchange() {
        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.modularHandle(new BisqMuSigKeyAggregationEvent());
        assertEquals(KEY_AGGREGATED, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(DEPOSIT_BROADCAST, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(FIAT_PAYMENT, protocol.getPhase());
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        // following sub-protocol transitions yet to be implemented

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(KEY_EXCHANGE, protocol.getPhase());
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.modularHandle(new SkipKeyEventForTesting()); // skipping keyExchange for testing
        assertEquals(KEY_EXCHANGE_COMPLETE, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(FINALIZATION, protocol.getPhase());
        assertEquals(FINALIZATION_INIT, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FINALIZATION_COMPLETE, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(COMPLETE, protocol.getPhase());
        assertEquals(BTC_CONFIRMED, model.getState());
    }

    @Test
    void testKeyExchange1A2A2B2C() {
        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(AWAITING_EVENT1A_OR_EVENT1B, model.getState());

        protocol.modularHandle(new DummyEvent1A());
        assertEquals(AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C, model.getState());

        protocol.modularHandle(new DummyEvent2A());
        assertEquals(AWAITING_EVENT2B_AND_EVENT2C, model.getState());
        protocol.modularHandle(new DummyEvent2B());
        assertEquals(AWAITING_EVENT2C, model.getState());
        protocol.modularHandle(new DummyEvent2C());
        assertEquals(EVENT1A_AND_2X_COMPLETE, model.getState());
    }

    @Test
    void testKeyExchange1A2B2C2A() {
        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(AWAITING_EVENT1A_OR_EVENT1B, model.getState());

        protocol.modularHandle(new DummyEvent1A());
        assertEquals(AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C, model.getState());

        protocol.modularHandle(new DummyEvent2B());
        assertEquals(AWAITING_EVENT2A_AND_EVENT2C, model.getState());
        protocol.modularHandle(new DummyEvent2C());
        assertEquals(AWAITING_EVENT2A, model.getState());
        protocol.modularHandle(new DummyEvent2A());
        assertEquals(EVENT1A_AND_2X_COMPLETE, model.getState());
    }

    @Test
    void testKeyExchange1B2A2B() {
        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(AWAITING_EVENT1A_OR_EVENT1B, model.getState());

        protocol.modularHandle(new DummyEvent1B());
        assertEquals(AWAITING_EVENT2A_AND_EVENT2B_FROM_1B, model.getState());

        protocol.modularHandle(new DummyEvent2A());
        assertEquals(AWAITING_EVENT2B_FROM_1B, model.getState());
        protocol.modularHandle(new DummyEvent2B());
        assertEquals(EVENT1B_AND_2X_COMPLETE, model.getState());
    }

    @Test
    void testOutOfOrderEventProcessing() {
        NextPhaseTriggerEvent phaseTriggerEvent = mock(NextPhaseTriggerEvent.class);
        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(DEPOSIT_INIT, model.getState(), "should not transition to next phase without the requisite State");

        protocol.modularHandle(new BisqMuSigKeyAggregationEvent());
        assertEquals(DEPOSIT_INIT, model.getState(), "should not transition to next phase without the requisite State");

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(KEY_AGGREGATED, model.getState()); // both construction and aggregation in queue, so transitions to Aggregated State

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(KEY_AGGREGATED, model.getState(), "should not transition to next phase without the requisite State");

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(KEY_AGGREGATED, model.getState(), "should not transition to next phase without the requisite State");

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(DEPOSIT_BROADCAST, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState(), "should not transition to next phase without the requisite State");

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(KEY_EXCHANGE_INIT, model.getState());
    }

    @Test
    void testWarningTxEventBranchWithRedirectTx() {

        assertEquals(INIT, model.getState());
        protocol.modularHandle(new WarningTxEvent()); // Phase protection swallows WarningTxEvent in INIT state
        assertEquals(INIT, model.getState());

        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new WarningTxEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState());
        assertEquals(BisqMuSigProtocol.Phase.CONFLICT, protocol.getPhase());

        protocol.modularHandle(new T1ExpiredEvent());
        assertEquals(AWAITING_REDIRECT_TX_OR_T2EXPIRED, model.getState());

        protocol.modularHandle(new RedirectTxEvent());
        assertEquals(REDIRECT_TX_CONFIRMED, model.getState());

        protocol.modularHandle(new ClaimTxEvent()); // ClaimTx has no effect when redirectTx is received, will be added to Event-queue and discarded at the end of the cycle
        assertEquals(REDIRECT_TX_CONFIRMED, model.getState());

        // Finalize the conflict resolution
        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED_REDIRECT, model.getState());
    }

    @Test
    void testWarningTxEventBranchNoRedirectTx() {
        assertEquals(INIT, model.getState());

        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.modularHandle(new WarningTxEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState());
        assertEquals(BisqMuSigProtocol.Phase.CONFLICT, protocol.getPhase());

        protocol.modularHandle(new T1ExpiredEvent());
        assertEquals(AWAITING_REDIRECT_TX_OR_T2EXPIRED, model.getState());

        protocol.modularHandle(new T2ExpiredEvent());
        assertEquals(AWAITING_REDIRECT_TX_OR_CLAIM_TX, model.getState());

        protocol.modularHandle(new ClaimTxEvent());
        assertEquals(CLAIM_TX_CONFIRMED, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED_CLAIM, model.getState());
    }

    @Test
    void testClaimTxEarlyShouldWaitForT2Expired() {
        assertEquals(INIT, model.getState());

        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new WarningTxEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState());

        protocol.modularHandle(new ClaimTxEvent()); // ClaimTxEvent needs to wait for T2ExpiredEvent to take effect
        assertEquals(AWAITING_T1EXPIRED, model.getState());

        protocol.modularHandle(new T1ExpiredEvent());
        assertEquals(AWAITING_REDIRECT_TX_OR_T2EXPIRED, model.getState());

        protocol.modularHandle(new T2ExpiredEvent());
        assertEquals(CLAIM_TX_CONFIRMED, model.getState()); // ClaimTxEvent already queued up, so it will be processed after T1ExpiredEvent

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED_CLAIM, model.getState());
    }

    @Test
    void testClaimTxAfterRedirectTxShouldBeQueuedAndDiscarded() { // Need to decide which Events should be queued and which should result in Exception
        assertEquals(INIT, model.getState());

        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new WarningTxEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState());

        protocol.modularHandle(new T1ExpiredEvent());
        assertEquals(AWAITING_REDIRECT_TX_OR_T2EXPIRED, model.getState());

        protocol.modularHandle(new RedirectTxEvent());
        assertEquals(REDIRECT_TX_CONFIRMED, model.getState());

        protocol.modularHandle(new ClaimTxEvent());
        assertEquals(REDIRECT_TX_CONFIRMED, model.getState(), "ClaimTxEvent should not be allowed after RedirectTxEvent");

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED_REDIRECT, model.getState()); // State is final, should we still check that ClaimTxEvent is actually discarded?
    }

    @Test
    void testHappyPathEventAfterWarningTxShouldFail() { // should it fail or just queue up?
        assertEquals(INIT, model.getState());

        fastForwardToAwaitingFiatConfirmation();
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        protocol.modularHandle(new WarningTxEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState()); // Happy path events should not be allowed after WarningTxEvent

        protocol.modularHandle(new NextPhaseTriggerEvent());
        assertEquals(AWAITING_T1EXPIRED, model.getState());

        protocol.modularHandle(new T1ExpiredEvent()); // conflict branch should continue as normal
        assertEquals(AWAITING_REDIRECT_TX_OR_T2EXPIRED, model.getState());

        protocol.modularHandle(new RedirectTxEvent());
        assertEquals(REDIRECT_TX_CONFIRMED, model.getState());
    }

    @Test
    void testTradeRejected() { // reject and cancel tests only check correct final State reached for now.
        assertEquals(INIT, model.getState());

        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.modularHandle(new BisqMuSigRejectTradeEvent());
        assertEquals(REJECTED, model.getState());
    }

    @Test
    void testTradePeerRejected() {
        assertEquals(INIT, model.getState());

        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.modularHandle(new BisqMuSigRejectTradeMessage());
        assertEquals(PEER_REJECTED, model.getState());
    }

    @Test
    void testTradeCancelled() {
        assertEquals(INIT, model.getState());

        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.modularHandle(new BisqMuSigCancelTradeEvent());
        assertEquals(CANCELLED, model.getState());
    }

    @Test
    void testTradePeerCancelled() {
        assertEquals(INIT, model.getState());

        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.modularHandle(new BisqMuSigCancelTradeMessage());
        assertEquals(PEER_CANCELLED, model.getState());
    }

    private void fastForwardToAwaitingFiatConfirmation() {
        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        protocol.modularHandle(new BisqMuSigKeyAggregationEvent());
        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(DEPOSIT_BROADCAST, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
    }
}
