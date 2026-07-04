package com.nextalubm.rotp_nextalbum.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import java.util.Optional;

public class CuriosIntegration {

    public static boolean hasCurioEquipped(LivingEntity entity, Item item) {
        try {
            Class<?> curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");

            Object curiosHelper = curiosApiClass.getMethod("getCuriosHelper").invoke(null);

            java.lang.reflect.Method findMethod = curiosHelper.getClass().getMethod("findEquippedCurio", Item.class, LivingEntity.class);

            Optional<?> optional = (Optional<?>) findMethod.invoke(curiosHelper, item, entity);

            return optional != null && optional.isPresent();
        } catch (Exception e) {
            return false;
        }
    }
}