package dev.alyti.fluxnautics.content.blocks.conduit.strand.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public record ConduitAttachment(ConduitAttachmentPoint point, @Nullable UUID subLevelID, BlockPos blockAttachment) {
    private static final Codec<ConduitAttachmentPoint> ATTACHMENT_POINT_CODEC = Codec.STRING.xmap(
        ConduitAttachmentPoint::valueOf,
        ConduitAttachmentPoint::name
    );

    private ConduitAttachment(final ConduitAttachmentPoint point, final Optional<UUID> subLevelID, final BlockPos blockAttachment) {
        this(point, subLevelID.orElse(null), blockAttachment);
    }

    public static final Codec<ConduitAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ATTACHMENT_POINT_CODEC.fieldOf("point").forGetter(ConduitAttachment::point),
        Codec.STRING.optionalFieldOf("subLevelID")
            .xmap(opt -> opt.map(UUID::fromString),
                uuid -> uuid.map(UUID::toString))
            .forGetter(x -> Optional.ofNullable(x.subLevelID())),
        BlockPos.CODEC.fieldOf("blockAttachment").forGetter(ConduitAttachment::blockAttachment)
    ).apply(instance, ConduitAttachment::new));

}