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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;

/**
 * A factory for Platform instances.
 */
class PlatformFactory {
    /**
     * Class logger instance.
     */
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();
    
    /**
     * Builds an appropriate Platform object for the supplied SystemBoard.
     * @param board The SystemBoard to build a platform for.
     * @return The Platform object for the System board.
     */
    public static Platform build(SystemBoard board) {
        try {
            // reflect on this class to find a create method that takes the board type
            Method buildMethod = PlatformFactory.class.getMethod("create", board.getClass());
            
            
            // check that the return type is a Platform object
            if (Platform.class.isAssignableFrom(buildMethod.getReturnType())) {            
                // build the platform using the method found
                try {
                    LOGGER.info("Building Platform for: " + board.getClass().getTypeName() + ".");
                    return (Platform)buildMethod.invoke(null, board);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                    LOGGER.error("Exception invoking the create Platform method for SystemBoard type: " + board.getClass().getTypeName() + ".", ex);
                }
            } else {
                LOGGER.error("The return type for the create function for the SystemBoard type: " + board.getClass().getTypeName() + " is not a Platform.");
            }
        } catch (NoSuchMethodException ex) {
            LOGGER.error("Could not find a Platform creator for SystemBoard type: " + board.getClass().getTypeName() + ".", ex);
        } catch (SecurityException ex) {
            LOGGER.error("Could not reflecct on SystemBoard to create Platform for type: " + board.getClass().getTypeName() + ".", ex);
        }
        
        throw new IllegalArgumentException("Could not build a Platoform for: " + board.getClass().getTypeName() + ".");
    }
    
    /**
     * Builds a Platform for a SystemBoardMock instance.
     * @param board The system board to build a Platform for.
     * @return A Platform for a SystemBoardMock instance.
     */
    public static Platform create(SystemBoardMock board){
        return new PlatformFunctor(
                (Boolean t) -> {
                    if (t) {
                        LOGGER.info("Enabled synchronization light.");
                    } else {
                        LOGGER.info("Disabled synchronization light.");
                    }   
                },
                (Consumer<Character> cb) -> {
                    LOGGER.info("Set keypad callback.");
                },
                () -> {
                    Set<Sensor> toReturn = new HashSet<>();
                    toReturn.add(new AccelerometerMock("Accelerometer"));
                    return toReturn;
                });
    }
    
    /**
     * Builds a Platform for a SystemBoardUdooNeo instance.
     * @param board The system board to build a Platform for.
     * @return A Platform for a SystemBoardUdooNeo instance.
     */
    public static Platform create(SystemBoardUdooNeo board){
        // get gpio4 which controls the sync light
        GPIOPort lightPort = board.getGPIOPorts().stream().filter((p) -> p.getName().equals("gpio4")).findFirst().get();
        
        return new PlatformFunctor(
                lightPort::setValue,
                (Consumer<Character> cb) -> {
                    LOGGER.info("Set keypad callback.");
                },
                () -> {
                    Set<Sensor> toReturn = new HashSet<>();
                    toReturn.add(new AccelerometerMock("Accelerometer"));
                    return toReturn;
                });
    }
}