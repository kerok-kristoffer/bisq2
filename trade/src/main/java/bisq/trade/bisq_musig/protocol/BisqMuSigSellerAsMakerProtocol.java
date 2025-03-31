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
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEventHandler;
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

        // Transition from the Deposit Module's final state (e.g. DEPOSIT_CONFIRMED)
        // to the start of the Fiat Confirmation Module.
        from(DEPOSIT_BROADCAST)
                .on(NextPhaseTriggerEvent.class) // Generic Transition Event and Handler should be replaced with specific ones when needed.
                .run(NextPhaseTriggerEventHandler.class)
                .to(AWAITING_FIAT_CONFIRMATION);

        /*Fiat Confirmation Module transitions are here in the protocol sequence*/
        fiatConfirmationModule.from(AWAITING_FIAT_CONFIRMATION)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(FIAT_CONFIRMED);

        // Transition to the Key Exchange Module.
        from(FIAT_CONFIRMED)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(KEY_EXCHANGE_INIT);

        /*Key Exchange Module transitions are here in the protocol sequence*/
        keyExchangeModule.from(KEY_EXCHANGE_INIT)
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(KEY_EXCHANGE_COMPLETE);

        // Transition to the Finalization Module.
        from(KEY_EXCHANGE_COMPLETE)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(FINALIZATION_INIT);

        /*Finalization Module transitions are here in the protocol sequence*/
        finalizationModule.from(FINALIZATION_INIT) // SwapTx handled here?
                .on(BisqMuSigDummyEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(FINALIZATION_COMPLETE);

        from(FINALIZATION_COMPLETE)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(BTC_CONFIRMED);

        // Transition to the Conflict Module.
        conflictModule.fromAny() //fromAny() for now, is currently guarded by phase in BisqMuSigProtocol
                .on(WarningTxEvent.class) // guard from WarningTx after key exchange specifically here or in BisqMuSigProtocol?
                .run(WarningTxEventHandler.class)
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
                            .to(REDIRECT_TX_CONFIRMED) // Should roles be handled on a protocol level? ie RedirectTxSeller | RedirectTxBuyer
                        .then()
                        .from(AWAITING_REDIRECT_TX_OR_T2EXPIRED)
                            .on(T2ExpiredEvent.class)
                            .run(BisqMuSigDummyEventHandler.class)
                            .to(AWAITING_REDIRECT_TX_OR_CLAIM_TX)
                            .then()
                                .from(AWAITING_REDIRECT_TX_OR_CLAIM_TX)
                                    .on(RedirectTxEvent.class)
                                    .run(BisqMuSigDummyEventHandler.class)
                                    .to(REDIRECT_TX_CONFIRMED)
                                .then()
                                .from(AWAITING_REDIRECT_TX_OR_CLAIM_TX)
                                    .on(ClaimTxEvent.class)
                                    .run(BisqMuSigDummyEventHandler.class)
                                    .to(CLAIM_TX_CONFIRMED)

                .then()
                    .from(REDIRECT_TX_CONFIRMED)
                    .on(BisqMuSigDummyEvent.class)
                    .run(BisqMuSigDummyEventHandler.class)
                    .to(CONFLICT_RESOLVED_REDIRECT)
                .then()
                    .from(CLAIM_TX_CONFIRMED)
                    .on(BisqMuSigDummyEvent.class)
                    .run(BisqMuSigDummyEventHandler.class)
                    .to(CONFLICT_RESOLVED_CLAIM);

        fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigRejectTradeEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(REJECTED);

        fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigRejectTradeMessage.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(PEER_REJECTED);

        fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigCancelTradeEvent.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(CANCELLED);

        fromStates(DEPOSIT_INIT, PRE_SIGNED_TRANSACTIONS_RECEIVED, KEY_AGGREGATED) // add proper states here
                .on(BisqMuSigCancelTradeMessage.class)
                .run(BisqMuSigDummyEventHandler.class)
                .to(PEER_CANCELLED);
    }
}