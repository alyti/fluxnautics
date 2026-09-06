package dev.alyti.fluxnautics.network.packets.blueprint;

import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.client.gui.GuiAssemblyTerminalScreen;
import dev.alyti.fluxnautics.util.FNCodecUtil;
import dev.alyti.fluxnautics.util.FNDistUtil;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import mekanism.common.network.IMekanismPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3dc;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Map;


public record BlueprintDataPacket(Map<ForceGroup, List<QueuedForceGroup.PointForce>> forces, double mass, Matrix3dc inertiaTensor, Vector3dc linearVelocity, Vector3dc angularVelocity) implements IMekanismPacket {
    public static final CustomPacketPayload.Type<BlueprintDataPacket> TYPE = new CustomPacketPayload.Type<>(Fluxnautics.id("blueprint_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlueprintDataPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(Object2ObjectOpenHashMap::new, FNCodecUtil.STREAM_FORCE_GROUP, FNCodecUtil.STREAM_POINT_FORCE.apply(ByteBufCodecs.list())), BlueprintDataPacket::forces,
        ByteBufCodecs.DOUBLE, BlueprintDataPacket::mass,
        FNCodecUtil.STREAM_MATRIX3D, BlueprintDataPacket::inertiaTensor,
        FNCodecUtil.STREAM_VECTOR3DC, BlueprintDataPacket::linearVelocity,
        FNCodecUtil.STREAM_VECTOR3DC, BlueprintDataPacket::angularVelocity,
        BlueprintDataPacket::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<BlueprintDataPacket> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            final GuiAssemblyTerminalScreen screen = FNDistUtil.getAssemblyTerminalScreen();

            if (screen != null) {
                screen.updateBlueprintData(this);
            }
        });
    }
}
