package quantisan.qte_lmax;

import org.junit.Test;
import static org.junit.Assert.*;

public class InstrumentTest {
    @Test
    public void testToName() throws Exception {
        assertEquals(Instrument.toName(4001L), "EURUSD");
    }

    @Test
    public void testToId() throws Exception {
        assertEquals(Instrument.toId("EURUSD"), 4001L);
    }
}
