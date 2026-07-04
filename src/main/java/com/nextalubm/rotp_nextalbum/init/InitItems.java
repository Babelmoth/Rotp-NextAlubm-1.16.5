package com.nextalubm.rotp_nextalbum.init;

import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.item.LuckPluckItem;
import com.nextalubm.rotp_nextalbum.item.MistaSuitArmorItem;
import com.nextalubm.rotp_nextalbum.item.NextAlbumArmorMaterials;
import com.nextalubm.rotp_nextalbum.item.RevolverAmmoItem;
import com.nextalubm.rotp_nextalbum.item.RevolverCasingItem;
import com.nextalubm.rotp_nextalbum.item.RevolverItem;
import com.nextalubm.rotp_nextalbum.item.AjaStoneNecklaceItem;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NextAlubm.MOD_ID);

    public static final RegistryObject<Item> REVOLVER = ITEMS.register("revolver", RevolverItem::new);
    public static final RegistryObject<Item> REVOLVER_AMMO = ITEMS.register("revolver_ammo", RevolverAmmoItem::new);
    public static final RegistryObject<Item> REVOLVER_CASING = ITEMS.register("revolver_casing", RevolverCasingItem::new);
    public static final RegistryObject<Item> LUCK_PLUCK = ITEMS.register("luck_pluck", () -> new LuckPluckItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PLANT_AGING_DUST = ITEMS.register("plant_aging_dust", () -> new Item(new Item.Properties().tab(ModItems.MAIN_TAB)));
    public static final RegistryObject<Item> FOUR_ATTACK_ICON = ITEMS.register("four_attack_icon", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MISTA_SUIT_HAT = ITEMS.register("mista_suit_hat", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.MISTA_SUIT, EquipmentSlotType.HEAD, "geo/mista_suit_hat.geo.json", "textures/models/armor/mista_suit_hat.png", true, false, false, false));
    public static final RegistryObject<Item> MISTA_SUIT_CLOTH = ITEMS.register("mista_suit_cloth", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.MISTA_SUIT, EquipmentSlotType.CHEST, "geo/mista_suit_cloth.geo.json", "textures/models/armor/mista_suit_cloth.png", "geo/mista_suit_cloth_slim.geo.json", "textures/models/armor/mista_suit_cloth_slim.png", false, true, true, false));
    public static final RegistryObject<Item> MISTA_SUIT_PANTS = ITEMS.register("mista_suit_pants", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.MISTA_SUIT, EquipmentSlotType.LEGS, "geo/mista_suit_pants.geo.json", "textures/models/armor/mista_suit_pants.png", false, false, false, true));
    public static final RegistryObject<Item> MISTA_SUIT_BOOTS = ITEMS.register("mista_suit_boots", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.MISTA_SUIT, EquipmentSlotType.FEET, "geo/mista_suit_boots.geo.json", "textures/models/armor/mista_suit_boots.png", false, false, false, true));
    public static final RegistryObject<Item> AJA_STONE_NECKLACE = ITEMS.register("aja_stone_necklace", () -> new AjaStoneNecklaceItem(new Item.Properties().tab(ModItems.MAIN_TAB).rarity(Rarity.RARE)));
}