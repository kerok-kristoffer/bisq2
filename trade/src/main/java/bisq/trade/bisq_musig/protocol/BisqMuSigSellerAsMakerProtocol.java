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

import bisq.common.fsm.EventHandler;
import bisq.common.fsm.Fsm;
import bisq.trade.ServiceProvider;
import bisq.trade.TradeService;
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEventHandler;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.BisqMuSigTakeOfferEvent;
import bisq.trade.bisq_musig.events.BisqMuSigTakeOfferEventHandler;
import bisq.trade.bisq_musig.events.WarningTxEvent;
import bisq.trade.bisq_musig.events.WarningTxEventHandler;
import bisq.trade.bisq_musig.protocol.modules.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;


@Slf4j
public class BisqMuSigSellerAsMakerProtocol extends BisqMuSigProtocol {

    private final List<Fsm<BisqMuSigTrade>> subProtocols;
    private final TradeService tradeService;
    private Class<? extends EventHandler> handlerClass;

    // temporary base constructor while testing WarningTx events utilizing TradeService to access tradeProtocolById
    public BisqMuSigSellerAsMakerProtocol(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
        subProtocols = List.of();
        tradeService = null;
    }

    // temporary constructor for testing WarningTx events utilizing TradeService to access tradeProtocolById
    public
    BisqMuSigSellerAsMakerProtocol(TradeService serviceProvider, BisqMuSigTrade model) {
        super(serviceProvider, model);
        this.tradeService = serviceProvider;

        // sub-protocols that define their own transitions.
            // in this prototype the transitions gets flattened into the main protocol.
        // the warningTx transition explores the case with a completely separate module.
        DepositModuleFSM depositModule = new DepositModuleFSM(model);
        FiatConfirmationModuleFSM fiatConfirmationModule = new FiatConfirmationModuleFSM(serviceProvider, model);
        KeyExchangeModuleFSM keyExchangeModule = new KeyExchangeModuleFSM(serviceProvider, model);
        FinalizationModuleFSM finalizationModule = new FinalizationModuleFSM(serviceProvider, model);

        subProtocols = List.of(depositModule, fiatConfirmationModule, keyExchangeModule, finalizationModule);
        registerAllSubTransitions();

    }

    public void configTransitions() {

        // Transitions between modules are not functionally required on this level, but offer an overview of the protocol sequence.
        // The Start and End States of each module can probably be refined to clarify the protocol sequence further.

        from(INIT)
                .on(BisqMuSigTakeOfferEvent.class)
                .run(BisqMuSigTakeOfferEventHandler.class)
                .to(DEPOSIT_INIT);

        /*Deposit Module transitions are here in the protocol sequence*/

        // Transition from the Deposit Module's final state (e.g. DEPOSIT_CONFIRMED)
        // to the start of the Fiat Confirmation Module.
        from(DEPOSIT_COMPLETED)
                .on(NextPhaseTriggerEvent.class) // Generic Transition Event and Handler should be replaced with specific ones when needed.
                .run(NextPhaseTriggerEventHandler.class)
                .to(AWAITING_FIAT_CONFIRMATION);

        /*Fiat Confirmation Module transitions are here in the protocol sequence*/

        // Transition to the Key Exchange Module.
        from(FIAT_CONFIRMED)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(KEY_EXCHANGE_INIT);

        /*Key Exchange Module transitions are here in the protocol sequence*/

        // Transition to the Finalization Module.
        from(KEY_EXCHANGE_COMPLETE)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(FINALIZATION_INIT);

        /*Finalization Module transitions are here in the protocol sequence*/

        from(FINALIZATION_COMPLETE)
            .on(NextPhaseTriggerEvent.class)
            .run(NextPhaseTriggerEventHandler.class)
            .to(BTC_CONFIRMED);

        // subFsm ConflictBranchProtocol initiated in WarningTxEventHandler
        fromAny() //fromAny() for now, needs to be replaced with appropriate fromStates(...)
                .on(WarningTxEvent.class)
                .run(WarningTxEventHandler.class)
                .to(WARNING_TX_RECEIVED);

    }

    private void registerAllSubTransitions() {
        subProtocols.forEach( module -> getTransitionMap().putAll(module.getTransitionMap()));
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        if(handlerClass == WarningTxEventHandler.class) { // temporary for testing WarningTx events, requires tradeService to access tradeProtocolById to ensure correct Fsm is called.
            return new WarningTxEventHandler(tradeService, model);
        }
        return super.newEventHandlerFromClass(handlerClass);
    }
}