package com.magicmod;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@Mod(modid = MagicMod.MODID, name = "Magic Staffs", version = "1.0")
public class MagicMod {
    public static final String MODID = "magicmod";

    public static Item fireStaff, iceStaff, lightningStaff, blinkStaff, healStaff;

    public static final CreativeTabs tab = new CreativeTabs("magicmod") {
        @Override
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem() {
            return MagicMod.fireStaff;
        }
    };

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        fireStaff = new Staffs.Fire();
        iceStaff = new Staffs.Ice();
        lightningStaff = new Staffs.Lightning();
        blinkStaff = new Staffs.Blink();
        healStaff = new Staffs.Heal();
        GameRegistry.registerItem(fireStaff, "fire_staff");
        GameRegistry.registerItem(iceStaff, "ice_staff");
        GameRegistry.registerItem(lightningStaff, "lightning_staff");
        GameRegistry.registerItem(blinkStaff, "blink_staff");
        GameRegistry.registerItem(healStaff, "heal_staff");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        // Shape:  ..X / .S. / S..   (S = stick)
        addStaff(fireStaff, Items.blaze_rod);
        addStaff(iceStaff, Blocks.packed_ice);
        addStaff(lightningStaff, Items.diamond);
        addStaff(blinkStaff, Items.ender_pearl);
        addStaff(healStaff, Items.ghast_tear);
    }

    private void addStaff(Item staff, Object gem) {
        GameRegistry.addRecipe(new ItemStack(staff), "  X", " S ", "S  ", 'X', gem, 'S', Items.stick);
    }
}
