package com.nextalubm.rotp_nextalbum.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.nextalubm.rotp_nextalbum.util.AgingBlockTracker;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;


@Mixin(Block.class)
public abstract class BlockDropResourcesMixin {

    @Inject(
            method = "dropResources(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true)
    private static void rotpNextAlbum$swapAgedCropDrops(BlockState state, World world, BlockPos pos,
                                                        TileEntity te, Entity entity, ItemStack tool,
                                                        CallbackInfo ci) {
        if (rotpNextAlbum$replaceAgedCropDrops(state, world, pos, te, entity, tool)) {
            ci.cancel();
        }
    }

    @Inject(
            method = "getDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void rotpNextAlbum$inheritAgingOnDrops(BlockState state, ServerWorld world, BlockPos pos,
                                                          TileEntity te,
                                                          CallbackInfoReturnable<List<ItemStack>> cir) {
        rotpNextAlbum$inheritAgingForSelfDrops(state, world, pos, cir.getReturnValue());
    }

    @Inject(
            method = "getDrops(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/tileentity/TileEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void rotpNextAlbum$inheritAgingOnToolDrops(BlockState state, ServerWorld world, BlockPos pos,
                                                              TileEntity te, Entity entity, ItemStack tool,
                                                              CallbackInfoReturnable<List<ItemStack>> cir) {
        rotpNextAlbum$inheritAgingForSelfDrops(state, world, pos, cir.getReturnValue());
    }

    @Unique
    private static boolean rotpNextAlbum$replaceAgedCropDrops(BlockState state, World world, BlockPos pos,
                                                              TileEntity te, Entity entity, ItemStack tool) {
        if (!(world instanceof ServerWorld)) {
            return false;
        }
        if (!(state.getBlock() instanceof CropsBlock)) {
            return false;
        }
        CropsBlock crop = (CropsBlock) state.getBlock();
        int age = state.getValue(crop.getAgeProperty());
        if (age < crop.getMaxAge()) {
            return false;
        }
        if (!AgingBlockTracker.hasAnyAgeing((ServerWorld) world, pos)) {
            return false;
        }
        BlockState reset = crop.getStateForAge(0);
        Block.dropResources(reset, world, pos, te, entity, tool);
        return true;
    }

    @Unique
    private static void rotpNextAlbum$inheritAgingForSelfDrops(BlockState state, ServerWorld world, BlockPos pos,
                                                               List<ItemStack> drops) {
        if (drops == null || drops.isEmpty()) {
            return;
        }
        for (ItemStack stack : drops) {
            AgingBlockTracker.applyAgingToDroppedSelfItem(world, pos, state, stack);
        }
    }
}
