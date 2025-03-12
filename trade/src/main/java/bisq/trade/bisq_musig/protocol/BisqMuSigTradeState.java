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

import bisq.common.fsm.State;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum BisqMuSigTradeState implements State {
    // Deposit & Key Aggregation Phase
    INIT,
    DEPOSIT_INIT,
    PRE_SIGNED_TRANSACTIONS_RECEIVED,
    KEY_AGGREGATED,
    DEPOSIT_COMPLETED,

    // Fiat Payment Confirmation Phase
    AWAITING_FIAT_CONFIRMATION,
    // Potential intermediate States to be added
    FIAT_CONFIRMED,

    // Key Exchange / Swap Phase
    KEY_EXCHANGE_INIT,
    // Potential intermediate States to be added
    KEY_EXCHANGE_COMPLETE,

    // Finalization Phase
    FINALIZATION_INIT,
    // Potential intermediate States to be added
    FINALIZATION_COMPLETE,

    WARNING_TX_RECEIVED,
    WARNING_TX_CONTINUED,
    REDIRECT_TX_RECEIVED,
    WARNING_TX_RECEIVED_AND_T1_EXPIRED,
    CLAIM_TX_RECEIVED,
    CONFLICT_RESOLVED(true),

    BTC_CONFIRMED(true),
    // Global Unhappy/Error States
    REJECTED(true),
    CANCELLED(true),
    FAILED(true),
    FAILED_AT_PEER(true);

    private final boolean isFinalState;
    private final int ordinal;

    BisqMuSigTradeState() {
        this(false);
    }

    BisqMuSigTradeState(boolean isFinalState) {
        this.isFinalState = isFinalState;
        ordinal = ordinal();
    }
}