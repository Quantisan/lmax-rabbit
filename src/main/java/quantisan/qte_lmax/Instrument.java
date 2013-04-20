package quantisan.qte_lmax;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class Instrument {
    private Instrument() {}

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

    public static String toName(long id) {
        return instrumentIdNameMap.get(id);
    }

    public static long toId(String name) {
        return instrumentNameIdMap.get(name.toUpperCase());
    }
}
