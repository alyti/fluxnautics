package dev.alyti.fluxnautics.content.blocks.conduit;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import mekanism.api.RelativeSide;
import mekanism.common.util.EnumUtils;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Mekanism model loader that properly loads models in a specific to energy cube format
 */
public class ConduitPortModelLoader implements IGeometryLoader<ConduitPortGeometry> {

    public static final ConduitPortModelLoader INSTANCE = new ConduitPortModelLoader();

    private ConduitPortModelLoader() {
    }

    @NotNull
    @Override
    public ConduitPortGeometry read(@NotNull JsonObject jsonObject, @NotNull JsonDeserializationContext ctx) {
        List<BlockElement> frame = readElements(jsonObject, ctx, "frame");
        Map<RelativeSide, List<BlockElement>> leds = new EnumMap<>(RelativeSide.class);
        Map<RelativeSide, List<BlockElement>> ports = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : EnumUtils.SIDES) {
            String name = side.name().toLowerCase(Locale.ROOT);
//            leds.put(side, readElements(jsonObject, ctx, name + "LEDs"));
            ports.put(side, readElements(jsonObject, ctx, name + "Port"));
        }
        return new ConduitPortGeometry(frame, leds, ports);
    }

    private List<BlockElement> readElements(JsonObject jsonObject, JsonDeserializationContext ctx, String key) {
        List<BlockElement> elements = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(jsonObject, key)) {
            elements.add(ctx.deserialize(element, BlockElement.class));
        }
        if (elements.isEmpty()) {
            throw new JsonParseException("Energy cube models requires a \"" + key + "\" element with at least one element.");
        }
        return elements;
    }
}