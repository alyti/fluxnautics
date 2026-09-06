package dev.alyti.fluxnautics.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.alyti.fluxnautics.Fluxnautics;
import dev.alyti.fluxnautics.content.entities.gear.EntityMekaRobit;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class MekaRobit extends HierarchicalModel<EntityMekaRobit> {
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(Fluxnautics.id("meka_robit"), "main");
	private final ModelPart root;
	public final ModelPart core;
	public final ModelPart shell;
	private final ModelPart shell_left_up;
	private final ModelPart shell_left_down;
	private final ModelPart shell_right_up;
	private final ModelPart shell_right_down;
	public final ModelPart wings_h;
	private final ModelPart down_left;
	private final ModelPart up_left;
	private final ModelPart down_right;
	private final ModelPart up_right;
	public final ModelPart wings_v;
	private final ModelPart left_up;
	private final ModelPart right_up;
	private final ModelPart left_down;
	private final ModelPart right_down;
	public final ModelPart thruster;
	private final ModelPart thrust_1;
	private final ModelPart thrust_2;
	private final ModelPart thrust_3;
	private final ModelPart thrust_4;

	public MekaRobit(ModelPart root) {
		this.root = root.getChild("root");
		this.core = this.root.getChild("core");
		this.shell = this.root.getChild("shell");
		this.shell_left_up = this.shell.getChild("shell_left_up");
		this.shell_left_down = this.shell.getChild("shell_left_down");
		this.shell_right_up = this.shell.getChild("shell_right_up");
		this.shell_right_down = this.shell.getChild("shell_right_down");
		this.wings_h = this.root.getChild("wings_h");
		this.down_left = this.wings_h.getChild("down_left");
		this.up_left = this.wings_h.getChild("up_left");
		this.down_right = this.wings_h.getChild("down_right");
		this.up_right = this.wings_h.getChild("up_right");
		this.wings_v = this.root.getChild("wings_v");
		this.left_up = this.wings_v.getChild("left_up");
		this.right_up = this.wings_v.getChild("right_up");
		this.left_down = this.wings_v.getChild("left_down");
		this.right_down = this.wings_v.getChild("right_down");
		this.thruster = this.root.getChild("thruster");
		this.thrust_1 = this.thruster.getChild("thrust_1");
		this.thrust_2 = this.thruster.getChild("thrust_2");
		this.thrust_3 = this.thruster.getChild("thrust_3");
		this.thrust_4 = this.thruster.getChild("thrust_4");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 18.2F, 0.0F));

		PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 48).addBox(-1.0F, -1.0F, 1.0F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, -0.2F, 0.0F, 0.0F, 3.1416F, 0.0F));

		PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -0.2F, 0.0F, -3.1416F, 0.0F, 0.0F));

		PartDefinition shell_left_up = shell.addOrReplaceChild("shell_left_up", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 0.0F));

		PartDefinition cube_r1 = shell_left_up.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(38, 10).addBox(-2.5F, -1.0F, -4.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, 0.1745F, 0.0262F));

		PartDefinition shell_left_down = shell.addOrReplaceChild("shell_left_down", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 0.0F));

		PartDefinition cube_r2 = shell_left_down.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(38, 40).addBox(-2.5F, -3.0F, -4.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, 0.1745F, -0.0262F));

		PartDefinition shell_right_up = shell.addOrReplaceChild("shell_right_up", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 0.0F));

		PartDefinition cube_r3 = shell_right_up.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(38, 20).mirror().addBox(0.5F, -1.0F, -4.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.1309F, -0.1745F, -0.0262F));

		PartDefinition shell_right_down = shell.addOrReplaceChild("shell_right_down", CubeListBuilder.create(), PartPose.offset(0.0F, 1.0F, 0.0F));

		PartDefinition cube_r4 = shell_right_down.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(38, 30).mirror().addBox(0.5F, -3.0F, -4.0F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -0.1309F, -0.1745F, 0.0262F));

		PartDefinition wings_h = root.addOrReplaceChild("wings_h", CubeListBuilder.create(), PartPose.offset(0.0F, -0.2F, 0.0F));

		PartDefinition down_left = wings_h.addOrReplaceChild("down_left", CubeListBuilder.create(), PartPose.offset(-6.0F, 1.0F, 0.0F));

		PartDefinition cube_r5 = down_left.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(19, 0).mirror().addBox(-1.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.1416F, 0.0F, -0.4363F));

		PartDefinition up_left = wings_h.addOrReplaceChild("up_left", CubeListBuilder.create(), PartPose.offset(-6.0F, -1.0F, 0.0F));

		PartDefinition cube_r6 = up_left.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(0, 12).mirror().addBox(-1.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition down_right = wings_h.addOrReplaceChild("down_right", CubeListBuilder.create(), PartPose.offset(6.0F, 1.0F, 0.0F));

		PartDefinition cube_r7 = down_right.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(19, 12).addBox(0.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.1416F, 0.0F, 0.4363F));

		PartDefinition up_right = wings_h.addOrReplaceChild("up_right", CubeListBuilder.create(), PartPose.offset(6.0F, -1.0F, 0.0F));

		PartDefinition cube_r8 = up_right.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));

		PartDefinition wings_v = root.addOrReplaceChild("wings_v", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -0.2F, 0.0F, 0.0F, 0.0F, 1.5708F));

		PartDefinition left_up = wings_v.addOrReplaceChild("left_up", CubeListBuilder.create(), PartPose.offset(-6.0F, 1.0F, 0.0F));

		PartDefinition cube_r9 = left_up.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 24).mirror().addBox(-1.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.1416F, 0.0F, -0.4363F));

		PartDefinition right_up = wings_v.addOrReplaceChild("right_up", CubeListBuilder.create(), PartPose.offset(-6.0F, -1.0F, 0.0F));

		PartDefinition cube_r10 = right_up.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(19, 24).mirror().addBox(-1.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4363F));

		PartDefinition left_down = wings_v.addOrReplaceChild("left_down", CubeListBuilder.create(), PartPose.offset(6.0F, 1.0F, 0.0F));

		PartDefinition cube_r11 = left_down.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 36).addBox(0.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 3.1416F, 0.0F, 0.4363F));

		PartDefinition right_down = wings_v.addOrReplaceChild("right_down", CubeListBuilder.create(), PartPose.offset(6.0F, -1.0F, 0.0F));

		PartDefinition cube_r12 = right_down.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(19, 36).addBox(0.0F, -3.0F, -4.0F, 1.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.4363F));

		PartDefinition thruster = root.addOrReplaceChild("thruster", CubeListBuilder.create(), PartPose.offset(0.0F, 5.8F, 0.0F));

		PartDefinition thrust_1 = thruster.addOrReplaceChild("thrust_1", CubeListBuilder.create().texOffs(14, 50).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, -0.5F));

		PartDefinition thrust_2 = thruster.addOrReplaceChild("thrust_2", CubeListBuilder.create().texOffs(14, 50).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 2.5F));

		PartDefinition thrust_3 = thruster.addOrReplaceChild("thrust_3", CubeListBuilder.create().texOffs(14, 50).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 5.5F));

		PartDefinition thrust_4 = thruster.addOrReplaceChild("thrust_4", CubeListBuilder.create().texOffs(14, 50).addBox(-1.0F, -1.0F, -0.5F, 2.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -6.0F, 8.5F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(@NotNull EntityMekaRobit e, float limbSwing, float limbSwingAmount,
	                      float ageInTicks, float netHeadYaw, float headPitch) {
		this.root().getAllParts().forEach(ModelPart::resetPose);

		this.animate(e.attackStartState, MekaRobitAnimation.ATTACK_START, ageInTicks, 1.0F);
		this.animate(e.attackOngoingState, MekaRobitAnimation.ATTACK_ONGOING, ageInTicks, 0.5F);
		this.animate(e.attackEndState, MekaRobitAnimation.ATTACK_END, ageInTicks, 1.0F);

		this.animate(e.idleAnimationState, MekaRobitAnimation.IDLE, ageInTicks, 1.0F);
		this.animate(e.patrolAnimationState, MekaRobitAnimation.IDLE, ageInTicks, 1.0F);
		this.animate(e.fullControlAnimationState, MekaRobitAnimation.IDLE, ageInTicks, 1.0F);

		float t = ageInTicks * 0.05F;
		float speed = (float) e.getDeltaMovement().length();

		core.zRot = t * 2.0F;
	}

	@Override
	public ModelPart root() {
		return root;
	}
}