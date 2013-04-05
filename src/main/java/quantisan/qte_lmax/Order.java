package quantisan.qte_lmax;

import us.bpsm.edn.parser.Parseable;
import us.bpsm.edn.parser.Parser;
import us.bpsm.edn.parser.Parsers;

import java.util.Map;

import static us.bpsm.edn.Keyword.newKeyword;
import static us.bpsm.edn.parser.Parsers.defaultConfiguration;

public class Order {
    private final long instrument;

    public Order(String message) {
        Parseable pbr = Parsers.newParseable(message);
        Parser p = Parsers.newParser(defaultConfiguration());
        Map<?, ?> m = (Map<?, ?>) p.nextValue(pbr);
        instrument = Instrument.toId(m.get(newKeyword("instrument")).toString());

    }

    public void execute() {
    }
}
