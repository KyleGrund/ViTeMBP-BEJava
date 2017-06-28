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
package com.vitembp.embedded.data;

import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.apache.logging.log4j.LogManager;

/**
 * An implementation of Capture using in-memory collection classes.
 */
class UuidStringStoreCapture extends Capture {
    /**
     * Class logger instance.
     */
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();
    
    /**
     * The names of the sensors whose data is represented by this sample.
     */
    private final Set<String> names;
    
    /**
     * The map of sensor names to their UUID type.
     */
    private final Map<String, UUID> types;
    
    /**
     * The set of samples.
     */
    private final ArrayList<Sample> samples;
    
    /**
     * The persistent storage this instance uses.
     */
    private final UuidStringStore store;
    
    /**
     * The UUID location to save  this capture at.
     */
    private final UUID captureLocation;
    
    /**
     * Initializes a new instance of the InMemoryCapture class and stores it to
     * the persistent storage.
     * @param frequency The frequency at which samples were taken.
     * @param store The persistent storage this instance uses.
     * @param nameToIds A map of sensor names to type UUIDs.
     */
    public UuidStringStoreCapture(double frequency, UuidStringStore store, Map<String, UUID> nameToIds) {
        super(frequency);
        
        // save refrences to parameters        
        this.store = store;
        this.names = new HashSet<>(nameToIds.keySet());
        this.types = new HashMap<>(nameToIds);
        
        // create store for any added samples
        this.samples = new ArrayList<>();
        
        captureLocation = UUID.randomUUID();
    }
    
    @Override
    public Iterable<Sample> getSamples() {
        return Collections.unmodifiableList(this.samples);
    }

    @Override
    public void addSample(Map<String, String> data) {
        // create a new sample and add it to the samples array list
        this.samples.add(new Sample(this.samples.size(), Instant.now(), data));
    }
    
    @Override
    public void save() throws IOException {
        this.store.write(this.captureLocation, this.toXml());
    }

    @Override
    public void load() throws IOException {
        // create a string  reader from capture entry in store
        StringReader sr = new StringReader(this.store.read(this.captureLocation));
        
        try {
            // load data from a newly created XML stream
            this.readFrom(XMLInputFactory.newFactory().createXMLStreamReader(sr));
        } catch (XMLStreamException ex) {
            LOGGER.error("Exception while loading Capture from UUID String store.", ex);
            throw new IOException("Exception while loading Capture from UUID String store.", ex);
        }
    }

    @Override
    public Set<String> getSensorNames() {
        return Collections.unmodifiableSet(this.names);
    }

    @Override
    public Map<String, UUID> getSensorTypes() {
        return Collections.unmodifiableMap(this.types);
    }

    @Override
    protected void addSample(Sample toAdd) {
        this.samples.add(toAdd);
    }

    @Override
    protected void readSamplesFrom(XMLStreamReader toReadFrom) throws XMLStreamException {
        while ("sample".equals(toReadFrom.getLocalName())) {
            int index = this.samples.size();
            Instant time = this.startTime.plusNanos(this.nanoSecondInterval * index);
            this.samples.add(new Sample(this.samples.size(), time, toReadFrom));
        }
    }

    @Override
    protected int getSampleCount() {
        return this.samples.size();
    }
}
