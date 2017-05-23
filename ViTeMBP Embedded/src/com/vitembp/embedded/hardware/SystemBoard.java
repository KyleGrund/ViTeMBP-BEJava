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
package com.vitembp.embedded.hardware;

/**
 * Class providing an interface to embedded system boards.
 */
public abstract class SystemBoard {
    /**
     * Detects the current board the system is operating on and creates the
     * appropriate singleton instance.
     * @return The board instance for the system that the program is currently
     * executing on. If the system is executing on an unknown board a mock
     * simulation will be returned.
     */
    public static SystemBoard getBoard() {
        throw new UnsupportedOperationException();
    }
}
