package com.nextalubm.rotp_nextalbum.init;

import com.github.standobyte.jojo.init.ModItems;
import com.nextalubm.rotp_nextalbum.NextAlubm;
import com.nextalubm.rotp_nextalbum.client.render.RevolverGeoRenderer;
import com.nextalubm.rotp_nextalbum.item.*;

import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.Rarity;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class InitItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, NextAlubm.MOD_ID);

    public static final RegistryObject<Item> REVOLVER_AMMO = ITEMS.register("revolver_ammo", RevolverAmmoItem::new);
    public static final RegistryObject<Item> REVOLVER_CASING = ITEMS.register("revolver_casing", RevolverCasingItem::new);
    public static final RegistryObject<Item> LUCK_PLUCK = ITEMS.register("luck_pluck", () -> new LuckPluckItem(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> PLANT_AGING_DUST = ITEMS.register("plant_aging_dust", () -> new Item(new Item.Properties().tab(ModItems.MAIN_TAB)));
    public static final RegistryObject<Item> ICE_CUBE = ITEMS.register("ice_cube", IceCubeItem::new);
    public static final RegistryObject<Item> SPIRIT_CIGARETTE = ITEMS.register("spirit_cigarette", SpiritCigaretteItem::new);
    public static final RegistryObject<Item> FOUR_ATTACK_ICON = ITEMS.register("four_attack_icon", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> MISTA_SUIT_HAT = ITEMS.register("mista_suit_hat", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.HEAD, "geo/general_suit_hat.geo.json", "textures/models/armor/mista_suit_hat.png", true, false, false, false));
    public static final RegistryObject<Item> MISTA_SUIT_CLOTH = ITEMS.register("mista_suit_cloth", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.CHEST, "geo/general_suit_cloth.geo.json", "textures/models/armor/mista_suit_cloth.png", "geo/general_suit_cloth_slim.geo.json", "textures/models/armor/mista_suit_cloth_slim.png", false, true, true, false));
    public static final RegistryObject<Item> MISTA_SUIT_PANTS = ITEMS.register("mista_suit_pants", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.LEGS, "geo/general_suit_pants.geo.json", "textures/models/armor/mista_suit_pants.png", false, false, false, true));
    public static final RegistryObject<Item> MISTA_SUIT_BOOTS = ITEMS.register("mista_suit_boots", () -> new MistaSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.FEET, "geo/general_suit_boots.geo.json", "textures/models/armor/mista_suit_boots.png", false, false, false, true));
    public static final RegistryObject<Item> PROSCIUTTO_SUIT_CLOTH = ITEMS.register("prosciutto_suit_cloth", () -> new ProsciuttoSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.CHEST, "geo/general_suit_cloth.geo.json", "textures/models/armor/prosciutto_suit_cloth.png", "geo/general_suit_cloth_slim.geo.json", "textures/models/armor/prosciutto_suit_cloth_slim.png", false, true, true, false));
    public static final RegistryObject<Item> PROSCIUTTO_SUIT_PANTS = ITEMS.register("prosciutto_suit_pants", () -> new ProsciuttoSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.LEGS, "geo/general_suit_pants.geo.json", "textures/models/armor/prosciutto_suit_pants.png", false, false, false, true));
    public static final RegistryObject<Item> PROSCIUTTO_SUIT_BOOTS = ITEMS.register("prosciutto_suit_boots", () -> new ProsciuttoSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.FEET, "geo/general_suit_boots.geo.json", "textures/models/armor/prosciutto_suit_boots.png", false, false, false, true));
    public static final RegistryObject<Item> PESCI_SUIT_CLOTH = ITEMS.register("pesci_suit_cloth", () -> new PesciSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.CHEST, "geo/general_suit_cloth.geo.json", "textures/models/armor/pesci_suit_cloth.png", "geo/general_suit_cloth_slim.geo.json", "textures/models/armor/pesci_suit_cloth_slim.png", false, true, true, false));
    public static final RegistryObject<Item> PESCI_SUIT_PANTS = ITEMS.register("pesci_suit_pants", () -> new PesciSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.LEGS, "geo/general_suit_pants.geo.json", "textures/models/armor/pesci_suit_pants.png", false, false, false, true));
    public static final RegistryObject<Item> PESCI_SUIT_BOOTS = ITEMS.register("pesci_suit_boots", () -> new PesciSuitArmorItem(NextAlbumArmorMaterials.CHARACTER_SUIT, EquipmentSlotType.FEET, "geo/general_suit_boots.geo.json", "textures/models/armor/pesci_suit_boots.png", false, false, false, true));
    public static final RegistryObject<Item> AJA_STONE_NECKLACE = ITEMS.register("aja_stone_necklace", () -> new AjaStoneNecklaceItem(new Item.Properties().tab(ModItems.MAIN_TAB).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> BEACH_BOY_ROD = ITEMS.register("beach_boy_rod",
            () -> new BeachBoyRodItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> REVOLVER = ITEMS.register("revolver",
            () -> new RevolverItem(
                    new Item.Properties().stacksTo(1).tab(ModItems.MAIN_TAB).setISTER(() -> RevolverGeoRenderer::new),
                    new RevolverStats.Builder()
                            .damage(12.0F, 6.5F, 9.0F)
                            .specialDamage(0.75F, 3.0F, 3.0F)
                            .range(8.0F, 72.0F, 2.0F)
                            .farDropRate(0.16F)
                            .speed(6.8F)
                            .inaccuracy(1.15F, 0.38F)
                            .recoil(11.8F, 4.0F)
                            .adsRecoil(0.90F, 0.80F)
                            .visualRecoil(0.18F, 14.0F)
                            .cooldowns(14, 18)
                            .melee(1.25F, 2.9D, 0.58D)
                            .adsZoom(1.70F)
                            .sway(1.18F)
                            .heavyFireSound()
                            .visuals("revolver")
                            .build()
            )
    );

    public static final RegistryObject<Item> MISTA_REVOLVER = ITEMS.register("mista_revolver",
            () -> new RevolverItem(
                    new Item.Properties().stacksTo(1).tab(ModItems.MAIN_TAB).setISTER(() -> RevolverGeoRenderer::new),
                    new RevolverStats.Builder()
                            .damage(10.0F, 5.0F, 8.0F)
                            .specialDamage(0.75F, 3.0F, 3.0F)
                            .range(5.0F, 50.0F, 1.0F)
                            .speed(6.1F)
                            .inaccuracy(0.78F, 0.24F)
                            .recoil(4.85F, 1.45F)
                            .adsRecoil(0.68F, 0.48F)
                            .visualRecoil(0.072F, 5.6F)
                            .cooldowns(5, 8)
                            .melee(1.15F, 2.85D, 0.50D)
                            .adsZoom(1.92F)
                            .sway(0.88F)
                            .criticalBonus(0.20F)
                            .visuals("mista_revolver")
                            .build()
            )
    );

}