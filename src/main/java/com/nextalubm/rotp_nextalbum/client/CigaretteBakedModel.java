package com.nextalubm.rotp_nextalbum.client;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.nextalubm.rotp_nextalbum.item.SpiritCigaretteItem;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;

public class CigaretteBakedModel implements IBakedModel {
    private final IBakedModel gui;
    private final IBakedModel handheld;
    private final ItemOverrideList overrides;
    private CigaretteBakedModel litModel = this;

    public CigaretteBakedModel(IBakedModel gui, IBakedModel handheld) {
        this.gui = gui;
        this.handheld = handheld;
        this.overrides = new ItemOverrideList() {
            @Override
            public IBakedModel resolve(IBakedModel model, ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {
                return SpiritCigaretteItem.isLit(stack) ? litModel : CigaretteBakedModel.this;
            }
        };
    }

    public void setLitModel(CigaretteBakedModel litModel) {
        this.litModel = litModel == null ? this : litModel;
    }

    private IBakedModel choose(ItemCameraTransforms.TransformType type) {
        return CigaretteModels.isGuiPerspective(type) ? gui : handheld;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
        return handheld.getQuads(state, side, rand);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return gui.getParticleIcon();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return overrides;
    }

    @Override
    public ItemCameraTransforms getTransforms() {
        return handheld.getTransforms();
    }

    @Override
    public IBakedModel handlePerspective(ItemCameraTransforms.TransformType type, MatrixStack pose) {
        choose(type).handlePerspective(type, pose);
        return this;
    }
}
