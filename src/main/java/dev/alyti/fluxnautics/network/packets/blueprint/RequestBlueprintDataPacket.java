package dev.alyti.fluxnautics.network.packets.blueprint;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.blocks.assembly_terminal.BlueprintTelemetry;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public record RequestBlueprintDataPacket(UUID subLevel)implements IMekanismPacket {

    private static final int REQUEST_COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long> LAST_REQUESTS = new ConcurrentHashMap<>();
    private static final double MAX_REQUEST_DISTANCE_SQR = 64.0 * 64.0;

    public static Type<RequestBlueprintDataPacket> TYPE = new Type<>(Fluxnautics.id("request_blueprint_data"));

    public static StreamCodec<RegistryFriendlyByteBuf, RequestBlueprintDataPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RequestBlueprintDataPacket::subLevel,
            RequestBlueprintDataPacket::new);

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final ServerPlayer player = (ServerPlayer) context.player();
            final Level level = player.level();

            final SubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container == null) {
                return;
            }

            final long now = level.getGameTime();
            final Long last = LAST_REQUESTS.get(player.getUUID());
            if (last != null && now - last < REQUEST_COOLDOWN_TICKS) {
                return;
            }
            LAST_REQUESTS.put(player.getUUID(), now);

            final SubLevel subLevel = container.getSubLevel(this.subLevel);

            if (subLevel instanceof final ServerSubLevel serverSubLevel) {
                var bb = serverSubLevel.boundingBox();
                double px = net.minecraft.util.Mth.clamp(player.getX(), bb.minX(), bb.maxX());
                double py = net.minecraft.util.Mth.clamp(player.getY(), bb.minY(), bb.maxY());
                double pz = net.minecraft.util.Mth.clamp(player.getZ(), bb.minZ(), bb.maxZ());
                if (player.distanceToSqr(px, py, pz) > MAX_REQUEST_DISTANCE_SQR) {
                    return;
                }
                BlueprintTelemetry.queueBlueprintDataFor(serverSubLevel, player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}