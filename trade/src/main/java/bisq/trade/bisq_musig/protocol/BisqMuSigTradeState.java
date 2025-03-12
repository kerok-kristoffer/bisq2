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
    DEPOSIT_BROADCAST,

    // Fiat Payment Confirmation Phase
    AWAITING_FIAT_CONFIRMATION,
    // Potential intermediate States to be added
    FIAT_CONFIRMED,

    // Key Exchange / Swap Phase
    KEY_EXCHANGE_INIT,
    // Example branching States
    AWAITING_EVENT1A_OR_EVENT1B,
    AWAITING_EVENT2A_AND_EVENT2B_AND_EVENT2C,
        AWAITING_EVENT2B_AND_EVENT2C,
        AWAITING_EVENT2A_AND_EVENT2C,
        AWAITING_EVENT2A_AND_EVENT2B,
        AWAITING_EVENT2A_AND_EVENT2B_FROM_1B,
            AWAITING_EVENT2A,
            AWAITING_EVENT2B,
            AWAITING_EVENT2C,
        AWAITING_EVENT2B_FROM_1B,
        AWAITING_EVENT2A_FROM_1B,
        AWAITING_EVENT2B_FROM_2C_2A_PATH,
                EVENT1A_AND_2X_COMPLETE,
                EVENT1B_AND_2X_COMPLETE,
    KEY_EXCHANGE_COMPLETE,

    // Finalization Phase
    FINALIZATION_INIT,
    // Potential intermediate States to be added
    FINALIZATION_COMPLETE,

    // Conflict Resolution Phase
    AWAITING_T1EXPIRED,
    AWAITING_REDIRECT_TX_OR_T2EXPIRED,
    AWAITING_REDIRECT_TX_OR_CLAIM_TX,
    CLAIM_TX_CONFIRMED,
    REDIRECT_TX_CONFIRMED,

    // Final States,
    BTC_CONFIRMED(true),
    REJECTED(true),
    PEER_REJECTED(true),
    CANCELLED(true),
    PEER_CANCELLED(true),
    FAILED(true),
    FAILED_AT_PEER(true),
    CONFLICT_RESOLVED_REDIRECT(true),
    CONFLICT_RESOLVED_CLAIM(true);

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