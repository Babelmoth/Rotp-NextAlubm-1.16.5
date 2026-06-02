package com.nextalubm.rotp_nextalbum.mixin;

import com.nextalubm.rotp_nextalbum.util.SexPistolsItemAttachmentUtil;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "hasFoil", at = @At("HEAD"), cancellable = true)
    private void rotpNextalbum$sexPistolsAttachmentFoil(CallbackInfoReturnable<Boolean> ci) {
        if (SexPistolsItemAttachmentUtil.hasAttachment((ItemStack) (Object) this)) {
            ci.setReturnValue(true);
        }
    }
}
