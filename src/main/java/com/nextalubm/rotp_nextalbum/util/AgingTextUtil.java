package com.nextalubm.rotp_nextalbum.util;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public final class AgingTextUtil {
    public static final int OBFUSCATED_NAME_LENGTH = 8;

    private AgingTextUtil() {
    }

    public static ITextComponent obfuscatedNameComponent(ITextComponent original) {
        return new StringTextComponent(normalizeVisibleName(original != null ? original.getString() : ""))
                .withStyle(TextFormatting.OBFUSCATED);
    }

    public static ITextComponent obfuscatedNameComponent(String original) {
        return new StringTextComponent(normalizeVisibleName(original)).withStyle(TextFormatting.OBFUSCATED);
    }

    public static ITextComponent obfuscatedMessageComponent(String original) {
        String stripped = TextFormatting.stripFormatting(original == null ? "" : original);
        if (stripped == null) {
            stripped = "";
        }
        return new StringTextComponent(stripped).withStyle(TextFormatting.OBFUSCATED);
    }

    private static String normalizeVisibleName(String original) {
        String stripped = TextFormatting.stripFormatting(original == null ? "" : original);
        if (stripped == null) {
            stripped = "";
        }
        StringBuilder builder = new StringBuilder(stripped);
        if (builder.length() > OBFUSCATED_NAME_LENGTH) {
            builder.setLength(OBFUSCATED_NAME_LENGTH);
        }
        while (builder.length() < OBFUSCATED_NAME_LENGTH) {
            builder.append('X');
        }
        return builder.toString();
    }
}
