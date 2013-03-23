package quantisan.qte_lmax;

import com.lmax.api.FixedPointNumber;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.PricePoint;

import java.util.*;

public class Tick {
    private final long timestamp;
    private long instrumentId;
    private final FixedPointNumber bidPrice;
    private final FixedPointNumber bidVolume;
    private final FixedPointNumber askPrice;
    private final FixedPointNumber askVolume;
    private final boolean isMarketHour;

    public Tick(OrderBookEvent o) {
        this.timestamp = o.getTimeStamp();
        this.isMarketHour = this.timestamp < o.getMarketClosePriceTimeStamp();
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
        return isMarketHour() || getBidVolume() != 0 || getAskVolume() != 0;
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

    public String getInstrumentName() {
        return Instrument.toName(instrumentId);
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
        return(getTimestamp() + "," + getInstrumentName() + "," + getBidPrice() + "," + getAskPrice() +
                "," + getBidVolume() + "," + getAskVolume());
    }
}
