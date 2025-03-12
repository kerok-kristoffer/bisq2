/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.bisq_musig.protocol;

import bisq.trade.ServiceProvider;
import bisq.trade.bisq_musig.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_musig.events.NextPhaseTriggerEventHandler;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.*;
import bisq.trade.bisq_musig.messages.BisqMuSigCancelTradeMessage;
import bisq.trade.bisq_musig.messages.BisqMuSigRejectTradeMessage;
import bisq.trade.bisq_musig.protocol.modules.*;
import lombok.extern.slf4j.Slf4j;

import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;


@Slf4j
public class BisqMuSigSellerAsMakerProtocol extends BisqMuSigProtocol {

    public BisqMuSigSellerAsMakerProtocol(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
    }

    public void configTransitions() {
        // submodules in BisqMuSigProtocol
        depositModule = new DepositModuleFSM(serviceProvider, model);
        fiatConfirmationModule = new FiatConfirmationModuleFSM(serviceProvider, model);
        keyExchangeModule = new KeyExchangeModuleFSM(serviceProvider, model);
        finalizationModule = new FinalizationModuleFSM(serviceProvider, model);
        conflictModule = new ConflictModuleFSM(serviceProvider, model);

        from(INIT)
                .on(BisqMuSigTakeOfferEvent.class)
                .run(BisqMuSigTakeOfferEventHandler.class)
                .to(DEPOSIT_INIT);

        depositModule.from(DEPOSIT_INIT)
                .on(BisqMusigConstructPresignedTransactionEvent.class)
                .run(BisqMusigConstructPresignedTransactionEventHandler.class)
                .to(PRE_SIGNED_TRANSACTIONS_RECEIVED) // add transaction for each role here
                .then()
                .from(PRE_SIGNED_TRANSACTIONS_RECEIVED)
                .on(BisqMuSigKeyAggregationEvent.class)
                .run(BisqMuSigKeyAggregationEventHandler.class)
                .to(KEY_AGGREGATED)
                .then()
                .from(KEY_AGGREGATED)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(DEPOSIT_BROADCAST);

        from(DEPOSIT_BROADCAST)
                .on(NextPhaseTriggerEvent.class) // Generic Transition Event and Handler should be replaced with specific ones when needed.
                .run(NextPhaseTriggerEventHandler.class)
                .to(AWAITING_FIAT_CONFIRMATION);

        fiatConfirmationModule.from(AWAITING_FIAT_CONFIRMATION)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(FIAT_CONFIRMED);

        from(FIAT_CONFIRMED)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(KEY_EXCHANGE_INIT);

        keyExchangeModule.from(KEY_EXCHANGE_INIT) // long transition chains could be moved to the module FSMs depending on preference
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(AWAITING_EVENT1A_OR_EVENT1B). // example of branching Events using _OR_
                then() // could be made more obvious with a branch() method if needed
                    .from(AWAITING_EVENT1A_OR_EVENT1B) // from State contains names of expected Events for better readability
                        .on(DummyEvent1A.class)
                        .run(BisqMuSigDummyEventHandler.class)
                        .to(AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C) // branches for Events in undetermined order highlighted with _AND_
                        .then()
                            .from(AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C) // State could be shortened to AWAITING_ALL_2_EVENTS with descriptive documentation depending on preference
                                .on(DummyEvent2A.class)
                                .run(BisqMuSigDummyEventHandler.class)
                                .to(AWAITING_EVENT2B_AND_EVENT2C)
                            .then()
                                .from(AWAITING_EVENT2B_AND_EVENT2C)
                                    .on(DummyEvent2B.class)
                                    .run(BisqMuSigDummyEventHandler.class)
                                    .to(AWAITING_EVENT2C)
                                    .then()
                                    .from(AWAITING_EVENT2C)
                                        .on(DummyEvent2C.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(EVENT1A_AND_2X_COMPLETE) // branch leaves marked with COMPLETE
                                .then()
                                .from(AWAITING_EVENT2B_AND_EVENT2C)
                                    .on(DummyEvent2C.class)
                                    .run(BisqMuSigDummyEventHandler.class)
                                    .to(AWAITING_EVENT2B)
                                    .then()
                                    .from(AWAITING_EVENT2B)
                                        .on(DummyEvent2B.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(EVENT1A_AND_2X_COMPLETE)

                            .then()
                            .from(AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C)
                                .on(DummyEvent2B.class)
                                .run(BisqMuSigDummyEventHandler.class)
                                .to(AWAITING_EVENT2A_AND_EVENT2C)
                                .then()
                                    .from(AWAITING_EVENT2A_AND_EVENT2C)
                                        .on(DummyEvent2C.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(AWAITING_EVENT2A)
                                            .then()
                                            .from(AWAITING_EVENT2A)
                                                .on(DummyEvent2A.class)
                                                .run(BisqMuSigDummyEventHandler.class)
                                                .to(EVENT1A_AND_2X_COMPLETE)
                                    .then()
                                    .from(AWAITING_EVENT2A_AND_EVENT2C)
                                        .on(DummyEvent2A.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(AWAITING_EVENT2C) // transition from this state is already defined above in the 2A -> 2B -> 2C path.
                            .then()
                            .from(AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C)
                                .on(DummyEvent2C.class)
                                .run(BisqMuSigDummyEventHandler.class)
                                .to(AWAITING_EVENT2A_AND_EVENT2B)
                                .then()
                                    .from(AWAITING_EVENT2A_AND_EVENT2B)
                                        .on(DummyEvent2A.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(AWAITING_EVENT2B_FROM_2C_2A_PATH) // _FROM_ if we want to differentiate between paths instead of reusing Transition from above
                                            .then()
                                            .from(AWAITING_EVENT2B_FROM_2C_2A_PATH)
                                                .on(DummyEvent2B.class)
                                                .run(BisqMuSigDummyEventHandler.class)
                                                .to(EVENT1A_AND_2X_COMPLETE)
                                    .then()
                                    .from(AWAITING_EVENT2A_AND_EVENT2B)
                                        .on(DummyEvent2B.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(AWAITING_EVENT2A) // transition from this state is already defined above in the 2C -> 2B -> 2A path.
                    .then()
                    .from(AWAITING_EVENT1A_OR_EVENT1B)
                        .on(DummyEvent1B.class)
                        .run(BisqMuSigDummyEventHandler.class)
                        .to(AWAITING_EVENT2A_AND_EVENT2B_FROM_1B) // When we receive Event1B, we can differentiate between the two paths if necessary
                        .then()
                            .from(AWAITING_EVENT2A_AND_EVENT2B_FROM_1B)
                                .on(DummyEvent2A.class)
                                .run(BisqMuSigDummyEventHandler.class)
                                .to(AWAITING_EVENT2B_FROM_1B)
                                .then()
                                    .from(AWAITING_EVENT2B_FROM_1B)
                                        .on(DummyEvent2B.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(EVENT1B_AND_2X_COMPLETE)
                            .then()
                            .from(AWAITING_EVENT2A_AND_EVENT2B_FROM_1B)
                                .on(DummyEvent2B.class)
                                .run(BisqMuSigDummyEventHandler.class)
                                .to(AWAITING_EVENT2A_FROM_1B)
                                .then()
                                    .from(AWAITING_EVENT2A_FROM_1B)
                                        .on(DummyEvent2A.class)
                                        .run(BisqMuSigDummyEventHandler.class)
                                        .to(EVENT1B_AND_2X_COMPLETE)
                .then()
                .fromStates(EVENT1A_AND_2X_COMPLETE, EVENT1B_AND_2X_COMPLETE) // continuation of the protocol after the branching events leaf States
                        .on(BisqMuSigDummyEvent.class)
                        .run(BisqMuSigDummyEventHandler.class)
                        .to(KEY_EXCHANGE_COMPLETE)

            .then()
                .from(KEY_EXCHANGE_INIT)
                .on(SkipKeyEventForTesting.class) // Event added to test flow separately - this should be removed when the protocol is fully implemented
                .run(BisqMuSigDummyEventHandler.class)
                .to(KEY_EXCHANGE_COMPLETE);

        from(KEY_EXCHANGE_COMPLETE)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(FINALIZATION_INIT);

        finalizationModule.from(FINALIZATION_INIT) // SwapTx handled here?
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(FINALIZATION_COMPLETE);

        from(FINALIZATION_COMPLETE)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(BTC_CONFIRMED);

        // Transition to the Conflict Module
        conflictModule.fromAny() //fromAny() for now. Is currently guarded by phase in BisqMuSigProtocol.modularHandle()
                .on(WarningTxEvent.class)  // explicit fromStates() would obviate the need for phase check in BisqMuSigProtocol.modularHandle()
                .run(WarningTxEventHandler.class) // branch for WarningTx after key exchange needs implementation
                .to(AWAITING_T1EXPIRED)
                .then()
                .from(AWAITING_T1EXPIRED)
                    .on(T1ExpiredEvent.class)
                    .run(BisqMuSigDummyEventHandler.class)
                    .to(AWAITING_REDIRECT_TX_OR_T2EXPIRED)
                    .then()
                        .from(AWAITING_REDIRECT_TX_OR_T2EXPIRED)
                            .on(RedirectTxEvent.class)
                            .run(BisqMuSigDummyEventHandler.class)
                            .to(REDIRECT_TX_CONFIRMED) // implement roles? ie RedirectTxSeller | RedirectTxBuyer
                        .then()
                        .from(AWAITING_REDIRECT_TX_OR_T2EXPIRED)
                            .on(T2ExpiredEvent.class)
                            .run(BisqMuSigDummyEventHandler.class)
                            .to(AWAITING_REDIRECT_TX_OR_CLAIM_TX)
                            .then()
                                .from(AWAITING_REDIRECT_TX_OR_CLAIM_TX)
                                    .on(RedirectTxEvent.class)
                                    .run(BisqMuSigDummyEventHandler.class)
                                    .to(REDIRECT_TX_CONFIRMED) // leaves of branches distinguished with CONFIRMED and indentation
                                .then()
                                .from(AWAITING_REDIRECT_TX_OR_CLAIM_TX)
                                    .on(ClaimTxEvent.class)
                                    .run(BisqMuSigDummyEventHandler.class)
                                    .to(CLAIM_TX_CONFIRMED)

                .then()
                    .from(REDIRECT_TX_CONFIRMED) // protocol continues depending on which leaf was reached
                    .on(BisqMuSigDummyEvent.class)
                    .run(BisqMuSigDummyEventHandler.class)
                    .to(CONFLICT_RESOLVED_REDIRECT)
                .then()
                    .from(CLAIM_TX_CONFIRMED)
                    .on(BisqMuSigDummyEvent.class)
                    .run(BisqMuSigDummyEventHandler.class)
                    .to(CONFLICT_RESOLVED_CLAIM);

        // reject and cancel trades - these could be moved to main protocol FSM if needed
        depositModule.fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigRejectTradeEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(REJECTED);

        depositModule.fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigRejectTradeMessage.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(PEER_REJECTED);

        depositModule.fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigCancelTradeEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(CANCELLED);

        depositModule.fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigCancelTradeMessage.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(PEER_CANCELLED);
    }
}
