package io.hoggmania.dashboard.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ColorPaletteTest {

    @Test
    public void testGetInitiativeRagColor() {
        assertEquals(ColorPalette.RAG_RED, ColorPalette.getInitiativeRagColor('R'));
        assertEquals(ColorPalette.RAG_AMBER, ColorPalette.getInitiativeRagColor('A'));
        assertEquals(ColorPalette.RAG_GREEN, ColorPalette.getInitiativeRagColor('G'));
        assertEquals(ColorPalette.WHITE, ColorPalette.getInitiativeRagColor('X'));
        assertEquals(ColorPalette.WHITE, ColorPalette.getInitiativeRagColor(' '));
    }

    @Test
    public void testGetRagColor() {
        assertEquals(ColorPalette.RAG_RED, ColorPalette.getRagColor("red"));
        assertEquals(ColorPalette.RAG_RED, ColorPalette.getRagColor("RED"));
        assertEquals(ColorPalette.RAG_RED, ColorPalette.getRagColor(" red "));
        
        assertEquals(ColorPalette.RAG_AMBER, ColorPalette.getRagColor("amber"));
        assertEquals(ColorPalette.RAG_AMBER, ColorPalette.getRagColor("AMBER"));
        
        assertEquals(ColorPalette.RAG_YELLOW, ColorPalette.getRagColor("yellow"));
        assertEquals(ColorPalette.RAG_YELLOW, ColorPalette.getRagColor("YELLOW"));
        
        assertEquals(ColorPalette.RAG_GREEN, ColorPalette.getRagColor("green"));
        assertEquals(ColorPalette.RAG_GREEN, ColorPalette.getRagColor("GREEN"));
        
        assertEquals(ColorPalette.RAG_DEFAULT, ColorPalette.getRagColor("unknown"));
        assertEquals(ColorPalette.RAG_DEFAULT, ColorPalette.getRagColor(null));
        assertEquals(ColorPalette.RAG_DEFAULT, ColorPalette.getRagColor(""));
    }

    @Test
    public void testConstantValues() {
        // Verify hex color format
        assertTrue(ColorPalette.RAG_RED.matches("#[0-9A-F]{6}"));
        assertTrue(ColorPalette.RAG_AMBER.matches("#[0-9A-F]{6}"));
        assertTrue(ColorPalette.RAG_GREEN.matches("#[0-9A-F]{6}"));
        assertTrue(ColorPalette.PRIMARY_BLUE.matches("#[0-9A-F]{6}"));
    }
}

