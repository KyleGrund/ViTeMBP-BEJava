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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.DeleteItemRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.vitembp.embedded.configuration.SystemConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;

/**
     * Creates a UuidStringStore for the H2 embedded database.
 */
class UuidStringStoreDynamoDB implements UuidStringStore {
    /**
     * Class logger instance.
     */
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();
    
    /**
     * The locations in the store where a list of captures are stored.
     */
    private static final UUID CAPTURE_LOCATIONS = UUID.fromString("b4522adf-5581-4e5a-a2e8-6ea94d25c0b3");
    
    /**
     * The connection to the database.
     */
    private final AmazonDynamoDB client;
    
    /**
     * Initializes a new instance of the UuidStringStoreH2 class.
     * @param dataFile The file to store the database to.
     */
    UuidStringStoreDynamoDB() {        
        // builds a client with credentials
        this.client = AmazonDynamoDBClient.builder().build();
    }
    
    @Override
    public Iterable<UUID> getCaptureLocations() throws IOException {
        return Arrays.asList(Arrays.asList(this.read(CAPTURE_LOCATIONS).split(","))
                .stream()
                .map(UUID::fromString)
                .toArray(UUID[]::new));
    }
    
    @Override
    public void addCapture(Capture toAdd, UUID locationID) throws IOException { 
        // get the current list of capture locations
        String captures = this.read(CAPTURE_LOCATIONS);
        
        // if there are no captures just store the single UUID, otherwise
        // append the list with a comma and then the UUID.
        if (captures == null || "".equals(captures)) {
            this.write(CAPTURE_LOCATIONS, locationID.toString());
        } else {
            this.write(CAPTURE_LOCATIONS, captures.concat(",").concat(locationID.toString()));
        }
        
        // add capture data: location, system, start, frequency to captures table
        Map<String, AttributeValue> attrs = new HashMap<>();
        attrs.put("LOCATION", new AttributeValue(locationID.toString()));
        attrs.put("SYSTEM", new AttributeValue(SystemConfig.getConfig().getSystemUUID().toString()));
        attrs.put("CREATEDTIME", new AttributeValue(toAdd.getCreatedTime().toString()));
        attrs.put("FREQUENCY", new AttributeValue(Double.toString(toAdd.getSampleFrequency())));
        
        try {
            this.client.putItem("CAPTURES", attrs);
        } catch (ResourceNotFoundException e) {
            LOGGER.error("The database does not contain the captures index table.", e);
        } catch (AmazonServiceException e) {
            LOGGER.error("Exception occurred writing to database.", e);
        }
    }
    
    @Override
    public String read(UUID key) throws IOException {
        Map<String, AttributeValue> reqkey = new HashMap<>();
        reqkey.put("ID", new AttributeValue().withS(key.toString()));
        
        GetItemRequest request = new GetItemRequest().withTableName("DATA").withKey(reqkey);
        
        GetItemResult result = client.getItem(request);
        if (result != null && result.getItem() != null) {
            AttributeValue val = result.getItem().get("VALUE");
            return val.getS();
        } else {
            // no items returned, so return null
            return null;
        }
    }

    @Override
    public void write(UUID key, String value) throws IOException {
        Map<String, AttributeValue> toAdd = new HashMap<>();
        toAdd.put("ID", new AttributeValue(key.toString()));
        toAdd.put("VALUE", new AttributeValue(value));
        
        try {
            this.client.putItem("DATA", toAdd);
        } catch (ResourceNotFoundException e) {
            LOGGER.error("The database does not contain the data table.", e);
        } catch (AmazonServiceException e) {
            LOGGER.error("Exception occurred writing to database.", e);
        }
    }

    @Override
    public void delete(UUID key) throws IOException {
        Map<String, AttributeValue> reqkey = new HashMap<>();
        reqkey.put("ID", new AttributeValue().withS(key.toString()));
        
        DeleteItemRequest request = new DeleteItemRequest().withTableName("DATA").withKey(reqkey);
        
        DeleteItemResult result = client.deleteItem(request);
    }
    
    @Override
    public Stream<UUID> getKeys() throws IOException {
        List<ScanResult> keys = new ArrayList<>();
        ScanResult res = this.client.scan("DATA", Arrays.asList(new String[] { "ID" }));
        keys.add(res);
        Map<String,AttributeValue> lastRes = res.getLastEvaluatedKey();
        while (lastRes != null) {
            ScanRequest sr = new ScanRequest();
            sr.setTableName("DATA");
            sr.setAttributesToGet(Arrays.asList(new String[] { "ID" }));
            sr.setExclusiveStartKey(lastRes);
            res = this.client.scan(sr);
            keys.add(res);
            lastRes = res.getLastEvaluatedKey();
        }
        return res.getItems().stream().map((item) -> UUID.fromString(item.get("ID").getS()));
    }
    
    @Override
    public Map<UUID, String> getHashes(List<UUID> locations) throws IOException {
        Map<UUID, String> hashes = new HashMap<>();
        for (UUID loc : locations) {
            // try to get any data
            String toHash = this.read(loc);
            if (toHash == null) {
                // put in an empty string as this entry is blank
                hashes.put(loc, "");
            } else {
                // had data, add the standard 32bit string hash
                hashes.put(loc, Integer.toString(toHash.hashCode()));
            }
        }
        return hashes;
    }
}
