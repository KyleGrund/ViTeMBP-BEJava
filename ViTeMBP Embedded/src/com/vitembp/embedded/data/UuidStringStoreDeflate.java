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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.DeflaterOutputStream;

/**
 * This class wraps another UuidStringStore implementing class and zips the data
 * in the store.
 */
class UuidStringStoreDeflate implements UuidStringStore {
    private final UuidStringStore store;
    
    UuidStringStoreDeflate(final UuidStringStore toWrap) {
        store = toWrap;
    }

    @Override
    public String read(UUID key) throws IOException {
        return this.decompress(this.store.read(key));
    }

    @Override
    public void write(UUID key, String value) throws IOException {
        this.store.write(key, this.compress(value));
    }

    @Override
    public Stream<CaptureDescription> getCaptureLocations() throws IOException {
        return this.store.getCaptureLocations();
    }

    @Override
    public void addCaptureDescription(CaptureDescription toAdd) throws IOException {
        this.store.addCaptureDescription(toAdd);
    }
    
    @Override
    public CaptureDescription getCaptureDescription(UUID location) throws IOException {
        return this.store.getCaptureDescription(location);
    }

    @Override
    public void delete(UUID key) throws IOException {
        this.store.delete(key);
    }

    @Override
    public Stream<UUID> getKeys() throws IOException {
        return this.store.getKeys();
    }

    @Override
    public Map<UUID, String> getHashes(List<UUID> locations) throws IOException {
        return this.store.getHashes(locations);
    }
    
    private String decompress(String data) throws IOException {
        // read data from the store
        char[] toDecomp = data.toCharArray();
        byte[] compressedData = new byte[toDecomp.length];
        for (int i = 0; i < toDecomp.length; i++) {
            compressedData[i] = (byte)toDecomp[i];
        }
        ByteArrayInputStream bytes = new ByteArrayInputStream(compressedData);
        DeflaterInputStream outStream = new DeflaterInputStream(bytes);
        bytes.close();
        StringBuilder sb = new StringBuilder();
        while (outStream.available() == 1) {
            sb.append((char)outStream.read());
        }

        return sb.toString();
    }
    
    private String compress(String value) throws IOException {
        // compress the data
        byte[] originalBytes = value.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DeflaterOutputStream outStream = new DeflaterOutputStream(bytes)) {
            outStream.write(originalBytes);
        }
        byte[] bytesToWrite = bytes.toByteArray();
        StringBuilder toWrite = new StringBuilder();
        for (int i = 0; i < bytesToWrite.length; i++) {
            toWrite.append((char)bytesToWrite[i]);
        }
        return toWrite.toString();
    }

    @Override
    public void removeCaptureDescription(UUID toRemove) throws IOException {
        this.store.removeCaptureDescription(toRemove);
    }
}
