package org.fentanylsolutions.nimblespiders.common;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

public class ConfigTest {

    @Test
    public void speedEntriesHandleClassNamesNamespacedIdsWhitespaceAndDuplicates() {
        Map<String, Double> values = Config.parseSpeeds(
            new String[] { "Spider:0.8", "Spider:1.2", " mod:Spider : 0.6 ", " net.example.MySpider : 0.9 ",
                "CaveSpider:0" });
        assertEquals(1.2, values.get("Spider"), 0);
        assertEquals(0.6, values.get("mod:Spider"), 0);
        assertEquals(0.9, values.get("net.example.MySpider"), 0);
        assertEquals(0, values.get("CaveSpider"), 0);
    }

    @Test
    public void invalidEntriesAreIgnoredWithoutDiscardingValidValues() {
        Map<String, Double> values = Config.parseSpeeds(
            new String[] { "Spider:0.8", "Spider:bad", "NoColon", ":0.5", " :1", "Negative:-1", "Nan:NaN",
                "Infinite:Infinity", "Huge:1e300", "Empty:", "CaveSpider:0.8" });
        assertEquals(2, values.size());
        assertEquals(0.8, values.get("Spider"), 0);
        assertEquals(0.8, values.get("CaveSpider"), 0);
    }
}
