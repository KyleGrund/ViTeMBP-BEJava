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

import com.vitembp.embedded.data.ConsumerIOException;
import com.vitembp.embedded.configuration.SystemConfig;
import com.vitembp.embedded.controller.SignalEndCapture;
import com.vitembp.embedded.controller.SignalStartCapture;
import com.vitembp.embedded.controller.StateMachine;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;

/**
 * This class provides an abstraction for peripheral interface board.
 */
public class HardwareInterface {
    /**
     * Class logger instance.
     */
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();
    
    /**
     * Singleton instance of this class.
     */
    private static HardwareInterface singleton = null;
    
    /**
     * The platform interface to hardware.
     */
    private Platform platform;
    
    /**
     * Mapping of sensor system names to sensor control objects.
     */
    private Map<String, Sensor> sensors;
    
    /**
     * Initializes a new instance of the HardwareInterface class.
     */
    private HardwareInterface() {
        this.sensors  = new HashMap<>();
        this.initializeResources();
        
        // updates the sensor collection when configuration updates occur
        SystemConfig config = SystemConfig.getConfig();
        config.addConfigChangedListener(this::updateSensorBindings);
        config.addConfigChangedListener(this::updateInterfaceMetrics);
    }
    
    /**
     * Gets the bound sensors to the system.
     * @return A map of sensor names to the sensor bound to it.
     */
    public Map<String, Sensor> getSensors() {
        return this.sensors;
    }
    
    /**
     * Sets the state of the synchronization light.
     * @param state Boolean value indicating whether to illuminate sync light.
     * @throws IOException If there is an IOException while setting sync light.
     */
    public void setSyncLight(boolean state) throws IOException {
        this.platform.getSetSyncLightTarget().accept(state);
    }
    
    /**
     * Flashes the sync light with the list of integers indicating the durations.
     * @param durations The delays between turning the sync light on and off.
     * @throws java.io.IOException If an error occurs accessing sync light IO.
     */
    public void flashSyncLight(List<Integer> durations) throws IOException {
        ConsumerIOException<Boolean> light = this.platform.getSetSyncLightTarget();
        Runnable lightTask = () -> {
            try {
                // initially disable light
                boolean lightState = false;
                light.accept(false);
                
                // flip light state and wait for duration
                for (int wait : durations) {
                    lightState = !lightState;
                    light.accept(lightState);
                    Thread.sleep(wait);
                }
                
                // always disable light
                light.accept(false);
            } catch (InterruptedException ex) {
                LOGGER.error("Thread sleep interrupted flashing sync light.", ex);
            } catch (IOException ex) {
                LOGGER.error("IOException while flashing sync light.", ex);
            }
        };
        
        new Thread(lightTask, "syncLight").start();
    }
    
    /**
     * Sounds the buzzer for the number of milliseconds provided.
     * @param duration The number of milliseconds to sound the buzzer for.
     * @throws java.io.IOException If an error occurs accessing buzzer IO.
     */
    public void soundBuzzer(int duration) throws IOException {
        ConsumerIOException<Boolean> buzzer = this.platform.getBuzzerTarget();
        Runnable buzzTask = () -> {
            try {
                buzzer.accept(true);
                Thread.sleep(duration);
                buzzer.accept(false);
            } catch (InterruptedException ex) {
                LOGGER.error("Thread sleep interrupted sounding buzzer.", ex);
            }   catch (IOException ex) {
                LOGGER.error("IO exception occured sounding buzzer.", ex);
            }
        };
        new Thread(buzzTask, "Buzzer").start();
    }
    
    /**
     * Shuts down and halts the system.
     * @throws IOException if the shutdown process cannot be started.
     */
    public void shutDownSystem() throws IOException {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(1000);
                ProcessBuilder pb = new ProcessBuilder(new String[] {"sudo", "shutdown", "-h", "now"});
                LOGGER.info("Executing command: " + Arrays.toString(pb.command().toArray()));
                // execute the command
                Process proc = pb.start();
                
                LOGGER.info("Sytem shutting down.");
            } catch (Exception ex) {
            } finally {
                System.exit(0);
            }
        });
    }
    
    /**
     * Shuts down and restarts the system.
     * @throws IOException if the shutdown process cannot be started.
     */
    public void restartSystem() throws IOException {
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(1000);
                ProcessBuilder pb = new ProcessBuilder(new String[] {"sudo", "shutdown", "-r", "now"});
                LOGGER.info("Executing command: " + Arrays.toString(pb.command().toArray()));
                // execute the command
                Process proc = pb.start();
                
                LOGGER.info("Sytem shutting down.");
            } catch (Exception ex) {
            } finally {
                System.exit(0);
            }
        });
    }
    
    /**
     * Initializes resources such as resolving sensor bindings for system
     * interface.
     */
    private void initializeResources() {
        LOGGER.info("Initializing hardware resoureces.");
        this.platform = Platform.getPlatform();
        SystemConfig config = SystemConfig.getConfig();
        
        // if the configuration was not loaded from disk,
        // attempt to load defaults for the platform
        if (!config.initializedFromFile()) {
            try {
                config.createDefaultConfigFrom(this.platform.getDefaultConfigPath());
            } catch (IOException ex) {
                LOGGER.error("Could not create platform specific default configuration.", ex);
            }
        }
        
        // register sensors with the system configuration
        this.platform.getSensors().forEach((s) -> {
            SystemConfig.getConfig().registerSensorUUID(s.getSerial());
            LOGGER.info(
                "Registered sensor, " + s.getSerial().toString() + ", of type, " +
                s.getType().toString() + ".");
            });
        
        // names to sensor bindings
        updateSensorBindings();
        
        // register key press listener to store presses into a queue
        this.platform.setKeypadCallback(this::keyPressListener);
        
        // update the interface metrics
        this.updateInterfaceMetrics();
    }
    
    /**
     * Gets an instance of the HardwareInterface class used to access the
     * hardware the program is currently executing on.
     * @return An instance of the HardwareInterface class for the hardware the
     * program is currently executing on.
     */
    public synchronized static HardwareInterface getInterface() {
        // build the singleton instance if it has not been built already
        if (HardwareInterface.singleton == null) {
            HardwareInterface.singleton = new HardwareInterface();
        }
        
        return HardwareInterface.singleton;
    }
    
    /**
     * Handles key-press events.
     * @param key The key that was pressed.
     */
    private void keyPressListener(char key) {
        if (key == '1') {
            StateMachine.getSingleton().enqueueSignal(new SignalStartCapture((s) -> {
                LOGGER.debug("Result of \"" + key + "\" key pressed: " + s);
            }));
        } else if (key == '4') {
            StateMachine.getSingleton().enqueueSignal(new SignalEndCapture((s) -> {
                LOGGER.debug("Result of \"" + key + "\" key pressed: " + s);
            }));
        }
    }
    
    /**
     * Creates a new hash map of sensor name to instance bindings.
     * @return A hash map of sensor name to instance bindings.
     */
    private void updateSensorBindings() {
        Map<String, Sensor> bindings = new HashMap<>();
        SystemConfig config = SystemConfig.getConfig();
        
        // for each name
        config.getSensorNames().forEach((name) -> {
            // get binding
            UUID bindingAddress = config.getSensorBindings().get(name);

            // get matching sensor if one is available, otherwise gets null
            Sensor match = this.platform.getSensors().stream()
                    .filter((d) -> d.getSerial().equals(bindingAddress))
                    .findFirst()
                    .orElse(null);
            
            if (match == null) {
                LOGGER.info("Could not bind sensor \"" + name + "\" to \"" + bindingAddress + "\".");
            } else {
                LOGGER.info("Sensor \"" + name + "\" bound to \"" + bindingAddress + "\"");
            }
            
            // add sensor to bindings
            bindings.put(name, match);
        });
        
        // update the local bindings collection
        this.sensors = bindings;
    }

    /**
     * Updates the network interface metrics to the values set in the configuration.
     */
    private void updateInterfaceMetrics() {
        SystemConfig config = SystemConfig.getConfig();
        
        try {
            this.platform.setWiredEthernetMetric(config.getWiredEthernetMetric());
        } catch (IOException ex) {
            LOGGER.error("Exception setting wired Ethernet interface metric.", ex);
        }
        try {
            this.platform.setWirelessEthernetMetric(config.getWirelessEthernetMetric());
        } catch (IOException ex) {
            LOGGER.error("Exception setting wireless Ethernet interface metric.", ex);
        }
        try {
            this.platform.setBluetoothMetric(config.getBluetoothMetric());
        } catch (IOException ex) {
            LOGGER.error("Exception setting Bluetooth interface metric.", ex);
        }
    }
}
