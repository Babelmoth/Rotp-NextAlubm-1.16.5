package com.nextalubm.rotp_nextalbum.mixin;

import com.github.standobyte.jojo.util.mc.MCUtil;
import com.nextalubm.rotp_nextalbum.item.LuckPluckItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MCUtil.class, remap = false)
public class MCUtilMixin {

    @Inject(method = "itemHandFree", at = @At("HEAD"), cancellable = true)
    private static void allowLuckPluckHandFree(ItemStack item, CallbackInfoReturnable<Boolean> cir) {
        if (!item.isEmpty() && item.getItem() instanceof LuckPluckItem) {
            if (((LuckPluckItem) item.getItem()).openFingers()) {
                cir.setReturnValue(true);
            }
        }
    }
}