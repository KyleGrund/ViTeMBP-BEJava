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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Kyle
 */
public class UuidStringStoreDynamoDBTest {
    /**
     * An array containing the names of the mock sensors we are generating data
     * for.
     */
    private static final String[] SENSOR_NAMES =
            new String[] { "Sensor One", "Sensor Two" };
    
    /**
     * A UUID representing the type of the mock sensor.
     */
    private static final UUID SENSOR_TYPE_UUID = UUID.fromString("3906c164-82c8-48f8-a154-a39a9d0269fa");
    
    public UuidStringStoreDynamoDBTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of instantiation of class UuidStringStoreDynamoDB.
     */
    @Test
    public void testInstantiate() throws InstantiationException {
        System.out.println("instantiate");

        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);
    }
    
    /**
     * Test of read method, of class UuidStringStoreDynamoDB.
     */
    @Test
    public void testRead() throws IOException, InstantiationException {
        System.out.println("read");

        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);

        UUID key = UUID.randomUUID();
        String expResult = "A test string.";
        instance.write(key, expResult);
        String result = instance.read(key);
        assertEquals(expResult, result);
    }
    
    /**
     * Test of read method on empty location, of class UuidStringStoreDynamoDB.
     */
    @Test
    public void testReadEmpty() throws IOException, InstantiationException {
        System.out.println("read empty");

        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);

        UUID key = UUID.randomUUID();
        String result = instance.read(key);
        assertNull(result);
    }

    /**
     * Test of write method, of class UuidStringStoreDynamoDB.
     * @throws java.lang.InstantiationException
     * @throws java.io.IOException
     */
    @Test
    public void testWrite() throws InstantiationException, IOException {
        System.out.println("write");
        UUID key = UUID.randomUUID();
        String value = "A test string.";
        
        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);

        instance.write(key, value);
        
        key = UUID.randomUUID();
        value = "";

        instance.write(key, value);
    }
    
    /**
     * Test of read method, of class UuidStringStoreDynamoDB.
     * @throws java.lang.InstantiationException
     * @throws java.io.IOException
     */
    @Test
    public void testDelete() throws InstantiationException, IOException {
        System.out.println("delete");
        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);

        UUID key = UUID.randomUUID();
        String expResult = "A test string.";
        instance.write(key, expResult);
        String result = instance.read(key);
        assertEquals(expResult, result);
        instance.delete(key);
        result = instance.read(key);
        assertNull(result);
    }
    
    /**
     * Test of write method, of class UuidStringStoreDynamoDB.
     */
    @Test
    public void testUpdate() throws InstantiationException, IOException {
        System.out.println("write");
        UUID key = UUID.randomUUID();
        
        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);

        String expected = "A test string.";
        instance.write(key, expected);
        assertTrue(expected.equals(instance.read(key)));

        expected = "A different string.";
        instance.write(key, expected);
        assertTrue(expected.equals(instance.read(key)));
    }
    
    /**
     * Test of class UuidStringStoreDynamoDB through capture interface.
     * @throws java.lang.Exception If an Exception occurs during test.
     */
    @Test
    public void testGetCapture() throws Exception {
        // test using 
        Map<String, UUID> nameToIds = new HashMap<>();
        nameToIds.put(SENSOR_NAMES[0], SENSOR_TYPE_UUID);
        nameToIds.put(SENSOR_NAMES[1], SENSOR_TYPE_UUID);
        Capture toTest = CaptureFactory.buildCapture(CaptureTypes.AmazonDynamoDB, 29.9, nameToIds);
        CaptureTests.testWriteTo(toTest);
    }
    
    /**
     * Test of getKeys method, of class UuidStringStoreDynamoDB.
     */
    @Test
    public void testGetKeys() throws InstantiationException, IOException {
        System.out.println("read");        

        // instantiate the connector
        UuidStringStore instance = UuidStringStoreFactory.build(CaptureTypes.AmazonDynamoDB);

        UUID key = UUID.randomUUID();
        String expResult = "A test string for ID scans.";
        instance.write(key, expResult);

        long keyCount = instance.getKeys().count();
        assertTrue(keyCount > 0);

        long keysCount = instance.getKeys()
                .filter((id) -> 
                {
                    return key.equals(id);
                })
                .count();

        assertEquals(1, keysCount);
    }
}
