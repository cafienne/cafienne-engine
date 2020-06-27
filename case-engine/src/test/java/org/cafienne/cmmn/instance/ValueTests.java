package org.cafienne.cmmn.instance;

import org.cafienne.cmmn.instance.casefile.ValueList;
import org.cafienne.cmmn.instance.casefile.ValueMap;
import org.cafienne.cmmn.test.TestScript;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValueTests {

    @Test
    public void testValueMap() {
        TestScript.debugMessage("Running ValueTests");

        ValueMap m = new ValueMap("x", 20, "y", "30");

        try {
            m = new ValueMap("x");
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().equals("Must provide sufficient input data to the ValueMap construction, of pattern String, Object, String, Object ...")) {
                throw new AssertionError("Hmmmm, something breaks nastily");
            }
        }
        try {
            m = new ValueMap(0, "13");
        } catch (IllegalArgumentException e) {
            if (!e.getMessage().contains("is not of type String or Enum, but it must be; found type")) {
                throw new AssertionError("Hmmmm, something breaks nastily", e);
            }
        }

        // Test conversion of array of int; it must lead to ValueList of LongValue, with Long's inside.
        ValueMap arrayConversionOfNumbersToLong = new ValueMap("SpecialOutput", new ValueMap("Multi", new int[]{0, 1, 2, 3, 4}));
        ValueMap output = arrayConversionOfNumbersToLong.with("SpecialOutput");
        ValueList list = output.withArray("Multi");
        for (int i = 0; i<list.size(); i++) {
            Object ith = list.get(i).getValue();
            if (!ith.equals(Long.valueOf(i))) { // Note: every number is converted to Long...
                throw new AssertionError("Member "+i+" in list is not " + i + ", but "+list.get(i).getValue());
            }
        }

        // Test conversion of string array
        ValueMap arrayConversionOfString = new ValueMap("SpecialOutput", new ValueMap("Multi", new String[]{"1", "2", "3", "4"}));

        // Test list conversion
        List<String> stringList = new ArrayList();
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");
        ValueMap objectWithStringList = new ValueMap("stringList", stringList);
        ValueList convertedList = objectWithStringList.withArray("stringList");
        for (int i=0; i<stringList.size(); i++) {
            String ithString = stringList.get(i);
            Object object = convertedList.get(i).getValue();
            if (! ithString.equals(object)) {
                throw new AssertionError("Expected string list to contain value "+ithString+" at position "+i+" but we found "+object);
            }
        }

        // Test basic map conversion
        Map<String, String> map = new HashMap();
        map.put("a", "a");
        map.put("b", "b");
        ValueMap mappedMap = new ValueMap("map", map);
        ValueMap convertedMap = mappedMap.with("map");
        if (!convertedMap.get("a").getValue().equals("a")) {
            throw new AssertionError("ValueMap is expected to contain a value 'a'");
        }
        if (!convertedMap.get("b").getValue().equals("b")) {
            throw new AssertionError("ValueMap is expected to contain a value 'b'");
        }
    }

}
