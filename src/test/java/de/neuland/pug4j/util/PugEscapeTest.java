package de.neuland.pug4j.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class PugEscapeTest {

    @Test
    public void testEscape() {
        assertEquals("\\\\s", PugEscape.escape("\\\\s"));
    }

    @Test
    public void testEscapePatterns() {
        assertEquals(
                "&lt;p style=&quot;color: black&quot;&gt;You &amp; I&lt;/p&gt;",
                PugEscape.escape("<p style=\"color: black\">You & I</p>")
        );
    }
}