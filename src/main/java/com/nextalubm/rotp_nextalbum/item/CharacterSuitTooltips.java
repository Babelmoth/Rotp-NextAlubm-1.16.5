package com.nextalubm.rotp_nextalbum.item;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;

public final class CharacterSuitTooltips {
    private CharacterSuitTooltips() {
    }

    public static ITextComponent effectLine(int themeColor, String nameKey, int worn, int required, String descKey) {
        Style theme = Style.EMPTY.withColor(Color.fromRgb(themeColor));
        ITextComponent name = new TranslationTextComponent(nameKey).withStyle(theme);
        ITextComponent desc = new TranslationTextComponent(descKey);
        if (required <= 1) {
            return new TranslationTextComponent("item.nextalbum.suit.effect.line.simple", name, desc);
        }
        return new TranslationTextComponent("item.nextalbum.suit.effect.line",
                name,
                new StringTextComponent(String.valueOf(worn)).withStyle(theme),
                new StringTextComponent(String.valueOf(required)).withStyle(theme),
                desc);
    }

    public static void applyUnbreakable(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            stack.getOrCreateTag().putBoolean("Unbreakable", true);
        }
    }
}
