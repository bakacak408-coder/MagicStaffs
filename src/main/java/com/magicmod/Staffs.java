package com.magicmod;

import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntitySmallFireball;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class Staffs {

    // ================= FIRE =================
    public static class Fire extends ItemStaff {
        public Fire() { super("fire_staff", 300, 10); }

        @Override
        protected boolean cast(ItemStack stack, World w, EntityPlayer p, Vec3 start, Vec3 look) {
            if (p.isSneaking()) { // flame nova
                List list = w.getEntitiesWithinAABBExcludingEntity(p, p.boundingBox.expand(6, 3, 6));
                for (Object o : list) {
                    if (o instanceof EntityLivingBase && ((EntityLivingBase) o).getDistanceToEntity(p) <= 6) {
                        EntityLivingBase e = (EntityLivingBase) o;
                        e.setFire(8);
                        e.attackEntityFrom(magic(p), 6.0F);
                    }
                }
                w.playSoundAtEntity(p, "fire.ignite", 1.0F, 0.8F);
                w.playSoundAtEntity(p, "mob.ghast.fireball", 1.0F, 1.0F);
                return true;
            }
            EntitySmallFireball fb = new EntitySmallFireball(w, p, look.xCoord, look.yCoord, look.zCoord);
            fb.setPosition(start.xCoord + look.xCoord * 1.2, start.yCoord + look.yCoord * 1.2 - 0.1, start.zCoord + look.zCoord * 1.2);
            w.spawnEntityInWorld(fb);
            w.playSoundAtEntity(p, "mob.ghast.fireball", 1.0F, 1.2F);
            return true;
        }

        @Override
        protected void clientFx(World w, EntityPlayer p, Vec3 start, Vec3 look) {
            if (p.isSneaking()) {
                for (int i = 0; i < 80; i++) {
                    double a = w.rand.nextDouble() * Math.PI * 2, r = 1 + w.rand.nextDouble() * 5;
                    w.spawnParticle("flame", p.posX + Math.cos(a) * r, p.posY - 1.2, p.posZ + Math.sin(a) * r, 0, 0.05, 0);
                }
            } else {
                for (int i = 0; i < 12; i++)
                    w.spawnParticle("flame", start.xCoord + look.xCoord * 1.2, start.yCoord + look.yCoord * 1.2 - 0.1,
                            start.zCoord + look.zCoord * 1.2, look.xCoord * 0.3 + (w.rand.nextDouble() - 0.5) * 0.1,
                            look.yCoord * 0.3 + (w.rand.nextDouble() - 0.5) * 0.1, look.zCoord * 0.3 + (w.rand.nextDouble() - 0.5) * 0.1);
            }
        }
    }

    // ================= ICE =================
    public static class Ice extends ItemStaff {
        private static final double RANGE = 24;
        public Ice() { super("ice_staff", 300, 20); }

        @Override
        protected boolean cast(ItemStack stack, World w, EntityPlayer p, Vec3 start, Vec3 look) {
            MovingObjectPosition m = trace(w, p, start, look, RANGE);
            if (m == null) return false;
            int cx, cy, cz;
            if (m.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                cx = MathHelper.floor_double(m.entityHit.posX);
                cy = MathHelper.floor_double(m.entityHit.posY);
                cz = MathHelper.floor_double(m.entityHit.posZ);
            } else { cx = m.blockX; cy = m.blockY; cz = m.blockZ; }

            AxisAlignedBB bb = AxisAlignedBB.getBoundingBox(cx - 3, cy - 3, cz - 3, cx + 4, cy + 4, cz + 4);
            for (Object o : w.getEntitiesWithinAABB(EntityLivingBase.class, bb)) {
                EntityLivingBase e = (EntityLivingBase) o;
                if (e == p) continue;
                e.attackEntityFrom(magic(p), 5.0F);
                e.addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 140, 3));
                e.addPotionEffect(new PotionEffect(Potion.digSlowdown.id, 140, 1));
                e.extinguish();
            }
            for (int dx = -3; dx <= 3; dx++)
                for (int dy = -2; dy <= 2; dy++)
                    for (int dz = -3; dz <= 3; dz++) {
                        if (dx * dx + dy * dy + dz * dz > 10) continue;
                        int x = cx + dx, y = cy + dy, z = cz + dz;
                        Block b = w.getBlock(x, y, z);
                        if (b == Blocks.water || b == Blocks.flowing_water) {
                            w.setBlock(x, y, z, Blocks.ice);
                        } else if (b == Blocks.air && w.getBlock(x, y - 1, z).isOpaqueCube() && w.rand.nextInt(2) == 0) {
                            w.setBlock(x, y, z, Blocks.snow_layer);
                        }
                    }
            w.playSoundEffect(cx + 0.5, cy + 0.5, cz + 0.5, "random.glass", 1.0F, 1.6F);
            return true;
        }

        @Override
        protected void clientFx(World w, EntityPlayer p, Vec3 start, Vec3 look) {
            MovingObjectPosition m = trace(w, p, start, look, RANGE);
            double d = hitDistance(m, start, RANGE);
            beam(w, "snowshovel", start, look, d, 0.3);
            if (m != null) burst(w, "snowballpoof", m.hitVec.xCoord, m.hitVec.yCoord, m.hitVec.zCoord, 40, 4);
        }
    }

    // ================= LIGHTNING =================
    public static class Lightning extends ItemStaff {
        private static final double RANGE = 40;
        public Lightning() { super("lightning_staff", 200, 30); }

        @Override
        protected boolean cast(ItemStack stack, World w, EntityPlayer p, Vec3 start, Vec3 look) {
            MovingObjectPosition m = trace(w, p, start, look, RANGE);
            if (m == null) return false;
            double x = m.hitVec.xCoord, y = m.hitVec.yCoord, z = m.hitVec.zCoord;
            if (p.getDistance(x, y, z) < 4) return false; // do not zap yourself
            w.addWeatherEffect(new EntityLightningBolt(w, x, y, z));
            for (int i = 0; i < 2; i++) { // little chain strikes
                double ox = x + (w.rand.nextDouble() - 0.5) * 6, oz = z + (w.rand.nextDouble() - 0.5) * 6;
                int top = w.getHeightValue(MathHelper.floor_double(ox), MathHelper.floor_double(oz));
                w.addWeatherEffect(new EntityLightningBolt(w, ox, top, oz));
            }
            return true;
        }

        @Override
        protected void clientFx(World w, EntityPlayer p, Vec3 start, Vec3 look) {
            MovingObjectPosition m = trace(w, p, start, look, RANGE);
            beam(w, "fireworksSpark", start, look, hitDistance(m, start, RANGE), 0.8);
        }
    }

    // ================= BLINK =================
    public static class Blink extends ItemStaff {
        private static final double RANGE = 14;
        public Blink() { super("blink_staff", 400, 15); }

        @Override
        protected boolean cast(ItemStack stack, World w, EntityPlayer p, Vec3 start, Vec3 look) {
            Vec3 end = start.addVector(look.xCoord * RANGE, look.yCoord * RANGE, look.zCoord * RANGE);
            MovingObjectPosition m = w.rayTraceBlocks(start, end);
            double d = m == null ? RANGE : Math.max(0, start.distanceTo(m.hitVec) - 1.0);
            double x = start.xCoord + look.xCoord * d;
            double z = start.zCoord + look.zCoord * d;
            double fy = start.yCoord + look.yCoord * d - 1.62; // feet position
            int tries = 0;
            while (tries < 4 && !w.getCollidingBoundingBoxes(p, p.boundingBox.getOffsetBoundingBox(x - p.posX, fy - p.posY, z - p.posZ)).isEmpty()) {
                fy += 1.0;
                tries++;
            }
            if (tries >= 4 || d < 1) return false;
            w.playSoundAtEntity(p, "mob.endermen.portal", 1.0F, 1.0F);
            if (p instanceof EntityPlayerMP) {
                ((EntityPlayerMP) p).playerNetServerHandler.setPlayerLocation(x, fy, z, p.rotationYaw, p.rotationPitch);
            }
            p.fallDistance = 0;
            w.playSoundEffect(x, fy, z, "mob.endermen.portal", 1.0F, 1.0F);
            return true;
        }

        @Override
        protected void clientFx(World w, EntityPlayer p, Vec3 start, Vec3 look) {
            for (int i = 0; i < 40; i++)
                w.spawnParticle("portal", p.posX + (w.rand.nextDouble() - 0.5) * 1.5, p.posY - 1.6 + w.rand.nextDouble() * 2,
                        p.posZ + (w.rand.nextDouble() - 0.5) * 1.5, (w.rand.nextDouble() - 0.5), -w.rand.nextDouble(), (w.rand.nextDouble() - 0.5));
        }
    }

    // ================= HEAL =================
    public static class Heal extends ItemStaff {
        public Heal() { super("heal_staff", 250, 100); }

        @Override
        protected boolean cast(ItemStack stack, World w, EntityPlayer p, Vec3 start, Vec3 look) {
            List list = w.getEntitiesWithinAABB(EntityPlayer.class, p.boundingBox.expand(5, 3, 5));
            for (Object o : list) {
                EntityPlayer ep = (EntityPlayer) o;
                ep.heal(6.0F);
                ep.addPotionEffect(new PotionEffect(Potion.regeneration.id, 120, 1));
                ep.removePotionEffect(Potion.poison.id);
            }
            w.playSoundAtEntity(p, "random.levelup", 1.0F, 1.5F);
            return true;
        }

        @Override
        protected void clientFx(World w, EntityPlayer p, Vec3 start, Vec3 look) {
            for (int i = 0; i < 10; i++)
                w.spawnParticle("heart", p.posX + (w.rand.nextDouble() - 0.5) * 2, p.posY - 1 + w.rand.nextDouble() * 2,
                        p.posZ + (w.rand.nextDouble() - 0.5) * 2, 0, 0.1, 0);
            for (int i = 0; i < 30; i++) {
                double a = w.rand.nextDouble() * Math.PI * 2, r = w.rand.nextDouble() * 5;
                w.spawnParticle("happyVillager", p.posX + Math.cos(a) * r, p.posY - 1.5 + w.rand.nextDouble() * 2, p.posZ + Math.sin(a) * r, 0, 0, 0);
            }
        }
    }
}
