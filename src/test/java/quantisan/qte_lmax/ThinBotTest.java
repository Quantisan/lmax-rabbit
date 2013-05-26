package quantisan.qte_lmax;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static quantisan.qte_lmax.ThinBot.isSaturday;

public class ThinBotTest {
    @Test @Ignore
    public void testIsWeekendNow() throws Exception {
        assertTrue(isSaturday());
    }
}
