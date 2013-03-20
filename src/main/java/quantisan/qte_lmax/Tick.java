package quantisan.qte_lmax;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.lmax.api.FixedPointNumber;
import com.lmax.api.orderbook.OrderBookEvent;
import com.lmax.api.orderbook.PricePoint;

import java.util.*;

public class Tick {
    private final long timestamp;
    private long instrumentId;      // TODO use instrument name
    private final FixedPointNumber bidPrice;
    private final FixedPointNumber bidVolume;
    private final FixedPointNumber askPrice;
    private final FixedPointNumber askVolume;
    private final boolean isMarketHour;

    public static final BiMap<Long, String> instrumentIdNameMap = ImmutableBiMap.<Long, String>builder()
            .put(4008L,"AUDJPY")
            .put(4007L,"AUDUSD")
            .put(4009L,"CHFJPY")
            .put(4016L,"EURAUD")
            .put(4015L,"EURCAD")
            .put(4011L,"EURCHF")
            .put(4003L,"EURGBP")
            .put(4006L,"EURJPY")
            .put(4001L,"EURUSD")
            .put(4017L,"GBPAUD")
            .put(4014L,"GBPCAD")
            .put(4012L,"GBPCHF")
            .put(4005L,"GBPJPY")
            .put(4002L,"GBPUSD")
            .put(4013L,"USDCAD")
            .put(4010L,"USDCHF")
            .put(4004L,"USDJPY")
            .put(100637L,"XAUUSD")
            .put(100639L,"XAGUSD").build();
    public static final BiMap<String, Long> instrumentNameIdMap = instrumentIdNameMap.inverse();

    public static String toInstrumentName(long id) {
        return (instrumentIdNameMap.get(id));
    }

    public static long toInstrumentId(String name) {
        return (instrumentNameIdMap.get(name));
    }

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
        return(getTimestamp() + "," + toInstrumentName(getInstrumentId()) + "," + getBidPrice() + "," + getAskPrice() +
                "," + getBidVolume() + "," + getAskVolume());
    }
}
