package com.therighthon.afc.client.render;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.therighthon.afc.AFC;
import com.therighthon.afc.client.render.colors.AFCSignBlockEntityRenderer;
import com.therighthon.afc.common.blocks.AFCBlocks;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.mixin.client.accessor.SignRendererAccessor;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;

public class AFCHangingSignBlockEntityRenderer extends HangingSignRenderer
{
    private static final Map<Block, AFCHangingSignBlockEntityRenderer.HangingSignModelData> RENDER_INFO;

    @Nullable
    public static AFCHangingSignBlockEntityRenderer.HangingSignModelData getData(Block block)
    {
        return RENDER_INFO.get(block);
    }

    private static AFCHangingSignBlockEntityRenderer.HangingSignModelData createModelData(Metal.Default metal, Supplier<? extends SignBlock> reg)
    {
        final WoodType type = reg.get().type();
        final ResourceLocation woodName = new ResourceLocation(type.name());
        final ResourceLocation metalName = Helpers.identifier(metal.getSerializedName());
        //TODO: Remove
        AFC.LOGGER.debug("Searchable");
        AFC.LOGGER.debug(type.name());
        return new AFCHangingSignBlockEntityRenderer.HangingSignModelData(
            new Material(Sheets.SIGN_SHEET, new ResourceLocation(woodName.getNamespace(), "entity/signs/hanging/" + metalName.getPath() + "/" + woodName.getPath())),
            new ResourceLocation(type.name() + ".png").withPrefix("textures/gui/hanging_signs/" + metalName.getPath() + "/")
        );
    }

    static
    {
        final ImmutableMap.Builder<Block, AFCHangingSignBlockEntityRenderer.HangingSignModelData> builder = ImmutableMap.builder();

        AFCBlocks.CEILING_HANGING_SIGNS.forEach((wood, map) -> map.forEach((metal, reg) -> builder.put(reg.get(), createModelData(metal, reg))));
        AFCBlocks.WALL_HANGING_SIGNS.forEach((wood, map) -> map.forEach((metal, reg) -> builder.put(reg.get(), createModelData(metal, reg))));

        RENDER_INFO = builder.build();
    }

    private final Map<WoodType, HangingSignRenderer.HangingSignModel> hangingSignModels;

    public AFCHangingSignBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {
        this(context, AFCBlocks.WOODS.keySet()
            .stream()
            .map(map -> new AFCSignBlockEntityRenderer.SignModelData(
                TerraFirmaCraft.MOD_ID,
                map.getSerializedName(),
                map.getVanillaWoodType()
            )));
    }

    public AFCHangingSignBlockEntityRenderer(BlockEntityRendererProvider.Context context, Stream<AFCSignBlockEntityRenderer.SignModelData> blocks)
    {
        super(context);

        ImmutableMap.Builder<WoodType, HangingSignRenderer.HangingSignModel> modelBuilder = ImmutableMap.builder();
        blocks.forEach(data -> {
            modelBuilder.put(data.type(), new HangingSignRenderer.HangingSignModel(context.bakeLayer(new ModelLayerLocation(new ResourceLocation(data.domain(), "hanging_sign/" + data.name()), "main"))));
        });
        this.hangingSignModels = modelBuilder.build();
    }

    @Override
    public void render(SignBlockEntity sign, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay)
    {
        final BlockState state = sign.getBlockState();
        final SignBlock signBlock = (SignBlock) state.getBlock();
        final WoodType woodType = SignBlock.getWoodType(signBlock);
        final HangingSignRenderer.HangingSignModel model = this.hangingSignModels.get(woodType);
        final AFCHangingSignBlockEntityRenderer.HangingSignModelData modelData = Objects.requireNonNull(getData(signBlock));

        model.evaluateVisibleParts(state);

        renderSignWithText(sign, poseStack, buffer, light, overlay, state, signBlock, modelData.modelMaterial(), model);
    }

    // behavior copied from SignRenderer#renderSignWithText
    void renderSignWithText(SignBlockEntity sign, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, BlockState blockstate, SignBlock signblock, Material modelMaterial, Model model)
    {
        poseStack.pushPose();
        ((SignRendererAccessor) this).invoke$translateSign(poseStack, -signblock.getYRotationDegrees(blockstate), blockstate);
        this.renderSign(poseStack, buffer, light, overlay, modelMaterial, model);
        ((SignRendererAccessor) this).invoke$renderSignText(sign.getBlockPos(), sign.getFrontText(), poseStack, buffer, light, sign.getTextLineHeight(), sign.getMaxTextLineWidth(), true);
        ((SignRendererAccessor) this).invoke$renderSignText(sign.getBlockPos(), sign.getBackText(), poseStack, buffer, light, sign.getTextLineHeight(), sign.getMaxTextLineWidth(), false);
        poseStack.popPose();
    }

    // behavior copied from SignRenderer#renderSign
    void renderSign(PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, Material modelMaterial, Model model)
    {
        poseStack.pushPose();
        float f = this.getSignModelRenderScale();
        poseStack.scale(f, -f, -f);

        VertexConsumer vertexconsumer = modelMaterial.buffer(buffer, model::renderType);
        ((SignRendererAccessor) this).invoke$renderSignModel(poseStack, light, overlay, model, vertexconsumer);
        poseStack.popPose();
    }

    public record HangingSignModelData(Material modelMaterial, ResourceLocation textureLocation) {}
}
