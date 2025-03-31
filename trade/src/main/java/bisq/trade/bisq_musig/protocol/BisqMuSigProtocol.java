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

import bisq.common.fsm.Event;
import bisq.common.fsm.EventHandler;
import bisq.trade.ServiceProvider;
import bisq.trade.bisq_easy.protocol.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.BisqMuSigTakeOfferEvent;
import bisq.trade.bisq_musig.events.WarningTxEvent;
import bisq.trade.bisq_musig.protocol.modules.*;
import bisq.trade.protocol.TradeProtocol;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static bisq.trade.bisq_musig.protocol.BisqMuSigProtocol.Phase.*;
import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;

@Slf4j
@Getter
public abstract class BisqMuSigProtocol extends TradeProtocol<BisqMuSigTrade> {
    private static final String version = "1.0.0";
    private Phase phase = Phase.INIT;
    private static final List<Phase> allowedPhasesForWarningTxEvent = List.of(
                    AWAITING_FIAT_CONFIRMED,
                    AWAITING_KEY_EXCHANGE_CONFIRMED);

    enum Phase {
        INIT,
        AWAITING_DEPOSIT_BROADCAST,
        AWAITING_FIAT_CONFIRMED,
        AWAITING_KEY_EXCHANGE_CONFIRMED,
        CONFLICT,
        AWAITING_FINALIZATION,
        FINALIZED,
        ABORTED
        }

    protected DepositModuleFSM depositModule;
    protected FiatConfirmationModuleFSM fiatConfirmationModule;
    protected KeyExchangeModuleFSM keyExchangeModule;
    protected FinalizationModuleFSM finalizationModule;
    protected ConflictModuleFSM conflictModule;

    public BisqMuSigProtocol(ServiceProvider serviceProvider, BisqMuSigTrade model) {
        super(version, serviceProvider, model);
    }

    @Override
    protected EventHandler newEventHandlerFromClass(Class<? extends EventHandler> handlerClass) {
        try {
            return handlerClass.getDeclaredConstructor(ServiceProvider.class, BisqMuSigTrade.class).newInstance(serviceProvider, model);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void configErrorHandling() {
    }

    protected void modularHandle(Event event) {

        if (event instanceof WarningTxEvent) {
            if(allowedPhasesForWarningTxEvent.contains(this.phase)) {
                this.phase = Phase.CONFLICT;
                handleBasedOnPhase(event);
            }
            return;
        }

        if (event instanceof NextPhaseTriggerEvent) { // as phase trigger events are added, these can be made more specific
            switch (this.phase) {
                case AWAITING_DEPOSIT_BROADCAST:
                    if(DEPOSIT_BROADCAST.equals(model.getState())) { // ensures that Phase change is only triggered when the DepositModule is in its final State
                        this.phase = Phase.AWAITING_FIAT_CONFIRMED;
                        break;
                    }
                    return; // no need to queue up the next phase trigger event if received in wrong order
                case AWAITING_FIAT_CONFIRMED:
                    if (FIAT_CONFIRMED.equals(model.getState())) {
                        this.phase = AWAITING_KEY_EXCHANGE_CONFIRMED;
                        break;
                    }
                    return;
                case AWAITING_KEY_EXCHANGE_CONFIRMED:
                    if (KEY_EXCHANGE_COMPLETE.equals(model.getState())) { // multiple conditions can be added here, for example when a SwapTx is broadcast, which should be valid in different phases
                        this.phase = Phase.AWAITING_FINALIZATION;
                        break;
                    }
                    return;
                case AWAITING_FINALIZATION:
                    if (FINALIZATION_COMPLETE.equals(model.getState())) {
                        this.phase = Phase.FINALIZED;
                        break;
                    }
                    return;
            }
            log.info("Phase transition to: {}", this.phase);
            super.handle(event); // this ensures that the Phase switching Events are handled by the main protocol FSM
            return;
        }

        if (this.phase.equals(Phase.INIT) && event instanceof BisqMuSigTakeOfferEvent) { // example of specific Phase transition event
            this.phase = Phase.AWAITING_DEPOSIT_BROADCAST;
            super.handle(event);
            return;
        }

        handleBasedOnPhase(event);
    }

    private void handleBasedOnPhase(Event event) {

        switch (this.phase) {
            case AWAITING_DEPOSIT_BROADCAST:
                depositModule.handle(event);
                break;
            case AWAITING_FIAT_CONFIRMED:
                fiatConfirmationModule.handle(event);
                break;
            case AWAITING_KEY_EXCHANGE_CONFIRMED:
                keyExchangeModule.handle(event);
                break;
            case AWAITING_FINALIZATION:
                finalizationModule.handle(event);
                break;
            case CONFLICT:
                conflictModule.handle(event);
                break;
            default:
                super.handle(event);
        }
    }

}
