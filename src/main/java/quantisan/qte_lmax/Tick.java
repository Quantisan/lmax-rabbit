package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.orderbook.Instrument;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.PricePoint;

import java.util.List;

public class Tick {
    private final long timestamp;
    private long instrumentId;      // TODO use instrument name
    private final FixedPointNumber bidPrice;
    private final FixedPointNumber bidVolume;
    private final FixedPointNumber askPrice;
    private final FixedPointNumber askVolume;
    public static final Tick ZERO = new Tick(0, 0, FixedPointNumber.ZERO, FixedPointNumber.ZERO, FixedPointNumber.ZERO, FixedPointNumber.ZERO);

    public Tick(long timestamp, long instrumentId, FixedPointNumber bidPrice, FixedPointNumber bidVolume, FixedPointNumber askPrice, FixedPointNumber askVolume) {
        this.timestamp = timestamp;
        this.instrumentId = instrumentId;
        this.bidPrice = bidPrice;
        this.bidVolume = bidVolume;
        this.askPrice = askPrice;
        this.askVolume = askVolume;
    }

    public Tick(OrderBookEvent o) {
        this.timestamp = o.getTimeStamp();
        this.instrumentId = o.getInstrumentId();
        this.bidPrice = getBestPrice(o.getBidPrices());
        this.bidVolume = getBestVolume(o.getBidPrices());
        this.askPrice = getBestPrice(o.getAskPrices());
        this.askVolume = getBestVolume(o.getAskPrices());
    }

    private FixedPointNumber getBestPrice(List<PricePoint> prices)
    {
        return prices.size() != 0 ? prices.get(0).getPrice() : FixedPointNumber.ZERO;
    }

    private FixedPointNumber getBestVolume(List<PricePoint> prices)
    {
        return prices.size() != 0 ? prices.get(0).getQuantity() : FixedPointNumber.ZERO;
    }

    public boolean isZero() {
        return(this.bidVolume.longValue() == 0 && this.askVolume.longValue() == 0);
    }

    @Override
    public String toString() {
        return(timestamp + "," + instrumentId + "," + bidPrice + "/" + askPrice + "," + bidVolume + "/" + askVolume);
    }
}
