package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.orderbook.Instrument;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.PricePoint;

import java.util.Date;
import java.util.List;

public class Tick {
    private final long timestamp;
    private long instrumentId;      // TODO use instrument name
    private final FixedPointNumber bidPrice;
    private final FixedPointNumber bidVolume;
    private final FixedPointNumber askPrice;
    private final FixedPointNumber askVolume;

    private final boolean isMarketHour;

    public Tick(long timestamp, long instrumentId, FixedPointNumber bidPrice, FixedPointNumber bidVolume, FixedPointNumber askPrice, FixedPointNumber askVolume, boolean isMarketHour) {
        this.timestamp = timestamp;
        this.instrumentId = instrumentId;
        this.bidPrice = bidPrice;
        this.bidVolume = bidVolume;
        this.askPrice = askPrice;
        this.askVolume = askVolume;
        this.isMarketHour = isMarketHour;
    }

    public Tick(OrderBookEvent o) {
        this.timestamp = o.getTimeStamp();
        this.isMarketHour = this.timestamp <= o.getMarketClosePriceTimeStamp();
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

    public boolean isValid() {
        return(isMarketHour() || getBidVolume() != 0 || getAskVolume() != 0);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Date getDate() {
        return new Date(timestamp);
    }

    public long getInstrumentId() {
        return instrumentId;
    }

    public long getBidPrice() {
        return bidPrice.longValue();
    }

    public long getBidVolume() {
        return bidVolume.longValue();
    }

    public long getAskPrice() {
        return askPrice.longValue();
    }

    public long getAskVolume() {
        return askVolume.longValue();
    }

    public boolean isMarketHour() {
        return isMarketHour;
    }

    @Override
    public String toString() {
        return(getTimestamp() + "," + getInstrumentId() + "," + getBidPrice() + "/" + getAskPrice() +
                "," + getBidVolume() + "/" + getAskVolume());
    }
}
