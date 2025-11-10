package de.neuland.pug4j.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class RecordWrapperTest {

    record Person(String name, int age) {}
    record Address(String city, String zip) {}
    record PersonWithAddress(String name, int age, Address address) {}

    @Test
    public void testWrapSimpleRecord() {
        Person person = new Person("Alice", 42);
        RecordWrapper wrapper = new RecordWrapper(person);

        assertEquals(2, wrapper.size());
        assertTrue(wrapper.containsKey("name"));
        assertTrue(wrapper.containsKey("age"));
        assertEquals("Alice", wrapper.get("name"));
        assertEquals(42, wrapper.get("age"));
    }

    @Test
    public void testWrapNestedRecord() {
        Address address = new Address("Berlin", "10115");
        PersonWithAddress person = new PersonWithAddress("Bob", 30, address);
        RecordWrapper wrapper = new RecordWrapper(person);

        assertEquals(3, wrapper.size());
        assertEquals("Bob", wrapper.get("name"));
        assertEquals(30, wrapper.get("age"));

        // Address should be automatically wrapped
        Object addressValue = wrapper.get("address");
        assertNotNull(addressValue);
        assertTrue(addressValue instanceof RecordWrapper);

        RecordWrapper addressWrapper = (RecordWrapper) addressValue;
        assertEquals("Berlin", addressWrapper.get("city"));
        assertEquals("10115", addressWrapper.get("zip"));
    }

    @Test
    public void testWrapIfRecord() {
        Person person = new Person("Charlie", 25);
        Object wrapped = RecordWrapper.wrapIfRecord(person);
        assertTrue(wrapped instanceof RecordWrapper);

        String str = "not a record";
        Object notWrapped = RecordWrapper.wrapIfRecord(str);
        assertEquals(str, notWrapped);
        assertFalse(notWrapped instanceof RecordWrapper);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapNonRecord() {
        new RecordWrapper("not a record");
    }
}
