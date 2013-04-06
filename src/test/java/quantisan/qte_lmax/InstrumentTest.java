package quantisan.qte_lmax;

import org.junit.Test;
import static org.junit.Assert.*;

public class InstrumentTest {
    @Test
    public void testToName() throws Exception {
        assertEquals("EURUSD", Instrument.toName(4001L));
    }

    @Test
    public void testToId() throws Exception {
        assertEquals(4001L, Instrument.toId("EURUSD"));
    }
}
