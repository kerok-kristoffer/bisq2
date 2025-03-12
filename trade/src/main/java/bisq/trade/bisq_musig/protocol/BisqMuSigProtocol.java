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
import bisq.trade.bisq_musig.events.NextPhaseTriggerEvent;
import bisq.trade.bisq_musig.BisqMuSigTrade;
import bisq.trade.bisq_musig.events.BisqMuSigTakeOfferEvent;
import bisq.trade.bisq_musig.events.WarningTxEvent;
import bisq.trade.bisq_musig.protocol.modules.*;
import bisq.trade.protocol.TradeProtocol;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import static bisq.trade.bisq_musig.protocol.BisqMuSigProtocol.Phase.*;
import static bisq.trade.bisq_musig.protocol.BisqMuSigTradeState.*;

@Slf4j
@Getter
public abstract class BisqMuSigProtocol extends TradeProtocol<BisqMuSigTrade> {
    private static final String version = "1.0.0";
    private Phase phase = Phase.START;
    private static final Set<Phase> allowedPhasesForWarningTxEvent = Set.of(
            FIAT_PAYMENT,
            KEY_EXCHANGE);

    enum Phase {
        START,
        DEPOSIT,
        FIAT_PAYMENT,
        KEY_EXCHANGE,
        CONFLICT,
        FINALIZATION,
        COMPLETE,
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
        // modularHandle likely needs some refactoring when decisions are made on the final structure of the protocol
        if (event instanceof WarningTxEvent) {
            if(allowedPhasesForWarningTxEvent.contains(this.phase)) {
                this.phase = Phase.CONFLICT;
                handleBasedOnPhase(event);
            }
            return;
        }

        if (event instanceof NextPhaseTriggerEvent) { // as specific phase trigger events are added, these can be removed and Phase switching can be handled automatically
            switch (this.phase) {
                case DEPOSIT:
                    if(DEPOSIT_BROADCAST.equals(model.getState())) { // ensures that Phase change is only triggered when the DepositModule is in its final State
                        this.phase = Phase.FIAT_PAYMENT;
                        break;
                    }
                    return; // no need to queue up generic next phase trigger event if received in wrong order
                case FIAT_PAYMENT:
                    if (FIAT_CONFIRMED.equals(model.getState())) {
                        this.phase = KEY_EXCHANGE;
                        break;
                    }
                    return;
                case KEY_EXCHANGE:
                    if (KEY_EXCHANGE_COMPLETE.equals(model.getState())) { // multiple conditions can be added here, for example when a SwapTx is broadcast, which could be valid from different phases
                        this.phase = Phase.FINALIZATION;
                        break;
                    }
                    return;
                case FINALIZATION:
                    if (FINALIZATION_COMPLETE.equals(model.getState())) {
                        this.phase = Phase.COMPLETE;
                        break;
                    }
                    return;
            }
            log.info("Phase transition to: {}", this.phase);
            super.handle(event); // this ensures that the Phase switching Events are handled by the main protocol FSM
            return;
        }

        if (this.phase.equals(Phase.START) && event instanceof BisqMuSigTakeOfferEvent) { // example of specific Phase transition event
            this.phase = Phase.DEPOSIT;
            super.handle(event);
            return;
        }

        handleBasedOnPhase(event);
    }

    private void handleBasedOnPhase(Event event) {

        switch (this.phase) {
            case DEPOSIT:
                depositModule.handle(event);
                break;
            case FIAT_PAYMENT:
                fiatConfirmationModule.handle(event);
                break;
            case KEY_EXCHANGE:
                keyExchangeModule.handle(event);
                break;
            case FINALIZATION:
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
