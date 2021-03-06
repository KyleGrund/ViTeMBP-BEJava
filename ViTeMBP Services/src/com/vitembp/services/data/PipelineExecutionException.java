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
package com.vitembp.services.data;

/**
 * The exception which is thrown when an error occurs while executing a
 * pipeline.
 */
public class PipelineExecutionException extends RuntimeException {
    /**
     * Initializes a new instance of the PipelineExecutionException class.
     * @param message The message explaining why the exception occurred.
     * @param inner The Exception that caused this exception to be thrown.
     */
    public PipelineExecutionException(String message, Exception inner) {
        super(message, inner);
    }
    
    /**
     * Initializes a new instance of the PipelineExecutionException class.
     * @param message The message explaining why the exception occurred.
     */
    public PipelineExecutionException(String message) {
        super(message);
    }
}
