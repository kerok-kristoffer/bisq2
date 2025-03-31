package bisq.trade.bisq_musig.protocol;

import bisq.contract.Party;
import bisq.contract.bisq_musig.BisqMuSigContract;
import bisq.identity.Identity;
import bisq.network.identity.NetworkId;
import bisq.offer.bisq_musig.BisqMuSigOffer;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void testProtocolHappyPathFlow() {
        NextPhaseTriggerEvent phaseTriggerEvent = mock(NextPhaseTriggerEvent.class);
        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        assertEquals(DEPOSIT_INIT, model.getState());

        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        assertEquals(PRE_SIGNED_TRANSACTIONS_RECEIVED, model.getState());

        protocol.modularHandle(new BisqMuSigKeyAggregationEvent());
        assertEquals(KEY_AGGREGATED, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(DEPOSIT_BROADCAST, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(AWAITING_FIAT_CONFIRMATION, model.getState());

        // following sub-protocol transitions yet to be implemented

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FIAT_CONFIRMED, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(KEY_EXCHANGE_INIT, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(KEY_EXCHANGE_COMPLETE, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(FINALIZATION_INIT, model.getState());

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(FINALIZATION_COMPLETE, model.getState());

        protocol.modularHandle(phaseTriggerEvent);
        assertEquals(BTC_CONFIRMED, model.getState());
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
        assertEquals(REDIRECT_TX_CONFIRMED, model.getState()); // ClaimTxEvent should not be allowed after RedirectTxEvent

        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(CONFLICT_RESOLVED_REDIRECT, model.getState());

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

    private void fastForwardToAwaitingFiatConfirmation() {
        protocol.modularHandle(new BisqMuSigTakeOfferEvent());
        protocol.modularHandle(new BisqMusigConstructPresignedTransactionEvent());
        protocol.modularHandle(new BisqMuSigKeyAggregationEvent());
        protocol.modularHandle(new BisqMuSigDummyEvent());
        assertEquals(DEPOSIT_BROADCAST, model.getState());

        protocol.modularHandle(new NextPhaseTriggerEvent());
    }
}
