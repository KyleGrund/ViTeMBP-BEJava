/*
 * Video Telemetry for Mountain Bike Platform back-end services.
 * Copyright (C) 2017 Kyle Grund
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.vitembp.embedded.controller;

import org.apache.logging.log4j.LogManager;

/**
 * The class containing the implementation for the WaitForEnd state.
 */
class WaitForEnd implements ControllerState {
    /**
     * Class logger instance.
     */
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    @Override
    public Class execute(ExecutionContext state) {
        // wait for a signal event
        Signal signal = null;
        try {
            signal = StateMachine.getSingleton().getSignal();
        } catch (InterruptedException ex) {
            LOGGER.error("Interrupted waiting for event.", ex);
        }
        
        // process the signal by type
        if (signal instanceof SignalEndCapture) {
            state.setSignal(signal);
            return EndCapture.class;
        }
        
        if (signal != null) {
            signal.returnResult("Error, capture running.");
        }
        
        // no valid signal received, return to this state
        return this.getClass();
    }
}
