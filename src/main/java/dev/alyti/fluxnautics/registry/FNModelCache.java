package dev.alyti.fluxnautics.registry;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.thruster.IonThrusterBakedModel;
import mekanism.client.model.BaseModelCache;

public class FNModelCache extends BaseModelCache {

    public static final FNModelCache INSTANCE = new FNModelCache();

    public final JSONModelData[] THRUSTER_PARTS = new JSONModelData[IonThrusterBakedModel.PARTIAL_MODELS.length];

    public final JSONModelData CONDUIT_WIRE = registerJSON("block/conduit/wire");
    public final JSONModelData CONDUIT_NODE = registerJSON("block/conduit/node");

    private FNModelCache() {
        super(Fluxnautics.MODID);
        for (IonThrusterBakedModel.ThrusterPart status : IonThrusterBakedModel.PARTIAL_MODELS) {
            THRUSTER_PARTS[status.ordinal()] = registerJSON(status.getModel());
        }
    }
}