package com.magicmod;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public abstract class ItemStaff extends Item {
    protected final int cooldown;

    public ItemStaff(String name, int durability, int cooldown) {
        this.cooldown = cooldown;
        setUnlocalizedName(name);
        setTextureName(MagicMod.MODID + ":" + name);
        setCreativeTab(MagicMod.tab);
        setMaxStackSize(1);
        setMaxDamage(durability);
        setFull3D();
    }

    /** Server side: do the real magic. Return true if the spell was cast (uses durability). */
    protected abstract boolean cast(ItemStack stack, World w, EntityPlayer p, Vec3 start, Vec3 look);

    /** Client side: particles only. */
    protected void clientFx(World w, EntityPlayer p, Vec3 start, Vec3 look) {}

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World w, EntityPlayer p) {
        NBTTagCompound data = p.getEntityData();
        String key = "cd_" + getUnlocalizedName();
        long now = w.getTotalWorldTime();
        long last = data.getLong(key);
        if (last <= now && now - last < cooldown) return stack;
        data.setLong(key, now);

        double ey = p.posY + (w.isRemote ? p.getEyeHeight() - p.getDefaultEyeHeight() : p.getEyeHeight());
        Vec3 start = Vec3.createVectorHelper(p.posX, ey, p.posZ);
        Vec3 look = p.getLookVec();

        p.swingItem();
        if (w.isRemote) {
            clientFx(w, p, start, look);
        } else if (cast(stack, w, p, start, look)) {
            stack.damageItem(1, p);
        }
        return stack;
    }

    // ---------- helpers ----------

    protected static DamageSource magic(EntityPlayer p) {
        return new EntityDamageSource("indirectMagic", p).setMagicDamage();
    }

    /** Ray trace that hits blocks AND living entities. May return null. */
    protected static MovingObjectPosition trace(World w, EntityPlayer p, Vec3 start, Vec3 look, double range) {
        Vec3 end = start.addVector(look.xCoord * range, look.yCoord * range, look.zCoord * range);
        MovingObjectPosition best = w.rayTraceBlocks(start, end);
        double bestDist = best != null ? start.distanceTo(best.hitVec) : range;

        AxisAlignedBB box = p.boundingBox.addCoord(look.xCoord * range, look.yCoord * range, look.zCoord * range).expand(1, 1, 1);
        List list = w.getEntitiesWithinAABBExcludingEntity(p, box);
        for (Object o : list) {
            if (!(o instanceof EntityLivingBase)) continue;
            Entity e = (Entity) o;
            if (!e.canBeCollidedWith()) continue;
            float s = e.getCollisionBorderSize() + 0.3F;
            MovingObjectPosition i = e.boundingBox.expand(s, s, s).calculateIntercept(start, end);
            if (i != null) {
                double d = start.distanceTo(i.hitVec);
                if (d < bestDist) {
                    bestDist = d;
                    best = new MovingObjectPosition(e);
                    best.hitVec = i.hitVec;
                }
            }
        }
        return best;
    }

    protected static double hitDistance(MovingObjectPosition m, Vec3 start, double range) {
        return m == null ? range : start.distanceTo(m.hitVec);
    }

    protected static void beam(World w, String particle, Vec3 start, Vec3 look, double len, double step) {
        for (double d = 1.0; d < len; d += step) {
            w.spawnParticle(particle, start.xCoord + look.xCoord * d, start.yCoord + look.yCoord * d - 0.15,
                    start.zCoord + look.zCoord * d, 0, 0, 0);
        }
    }

    protected static void burst(World w, String particle, double x, double y, double z, int count, double spread) {
        for (int i = 0; i < count; i++) {
            w.spawnParticle(particle,
                    x + (w.rand.nextDouble() - 0.5) * spread,
                    y + (w.rand.nextDouble() - 0.5) * spread,
                    z + (w.rand.nextDouble() - 0.5) * spread,
                    (w.rand.nextDouble() - 0.5) * 0.2, w.rand.nextDouble() * 0.2, (w.rand.nextDouble() - 0.5) * 0.2);
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return true; // enchant glint
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        return EnumRarity.rare;
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer p, List list, boolean adv) {
        list.add(StatCollector.translateToLocal(getUnlocalizedName() + ".desc"));
    }
}
