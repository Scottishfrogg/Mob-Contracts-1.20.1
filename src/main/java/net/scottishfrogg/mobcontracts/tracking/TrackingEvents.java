package net.scottishfrogg.mobcontracts.tracking;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.scottishfrogg.mobcontracts.ContractMod;
import net.scottishfrogg.mobcontracts.contract.ContractObjective;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingConversionEvent;
import java.util.UUID;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.scottishfrogg.mobcontracts.contract.ContractConditions;

@Mod.EventBusSubscriber(modid = ContractMod.MOD_ID)
public class TrackingEvents {
    private static final String SCARED_TAG = "contractmod_scared";
    private static final String WOLVES_TAG = "contractmod_wolves";
    private static final String BLAST_TICK_TAG = "contractmod_blast_tick";
    private static final String BLAST_KILLS_TAG = "contractmod_blast_kills";
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            ContractTracker.onKill(player, event.getEntity(), event.getSource());
        }
    }
    @SubscribeEvent
    public static void onArrowJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof ServerPlayer shooter) {
            if (shooter.getUseItem().getItem() instanceof BowItem) {
                arrow.getPersistentData().putBoolean(ContractConditions.BOW_TAG, true);
            } else if (shooter.getMainHandItem().getItem() instanceof CrossbowItem
                    || shooter.getOffhandItem().getItem() instanceof CrossbowItem) {
                arrow.getPersistentData().putBoolean(ContractConditions.CROSSBOW_TAG, true);
            }
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            ContractTracker.onPlayerDamaged(player, event.getSource());
        }
    }
    @SubscribeEvent
    public static void onSkeletonSunDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || victim.getType() != EntityType.SKELETON) return;
        DamageSource source = event.getSource();
        if (!(source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.IN_FIRE))) return;
        if (!victim.level().isDay() || !victim.level().canSeeSky(victim.blockPosition())) return;
        Skeleton skeleton = (Skeleton) victim;
        ServerPlayer player = null;
        if (skeleton.getTarget() instanceof ServerPlayer targeted) {
            player = targeted;
        } else if (skeleton.getLastHurtByMob() instanceof ServerPlayer attacker
                && skeleton.tickCount - skeleton.getLastHurtByMobTimestamp() < 200) {
            player = attacker;
        }
        if (player != null) ContractTracker.fireEvent(player, "skeleton_sun_kill");
    }
    @SubscribeEvent
    public static void onInteractEntity(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getTarget() instanceof Creeper creeper && !creeper.isIgnited()
                && event.getItemStack().is(Items.FLINT_AND_STEEL)) {
            ContractTracker.fireEvent(player, "creeper_ignited");
        }
    }
    @SubscribeEvent
    public static void onZombieVillagerCured(LivingConversionEvent.Post event) {
        if (!(event.getEntity() instanceof ZombieVillager zombie) || !(event.getOutcome() instanceof Villager)) return;
        if (zombie.level().isClientSide) return;
        CompoundTag data = zombie.saveWithoutId(new CompoundTag());
        if (!data.hasUUID("ConversionPlayer")) return;
        UUID starter = data.getUUID("ConversionPlayer");
        if (zombie.level().getPlayerByUUID(starter) instanceof ServerPlayer player) {
            ContractTracker.fireEvent(player, "zombie_villager_cured");
        }
    }
    @SubscribeEvent
    public static void onCreeperBlastDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (victim.level().isClientSide || !(victim instanceof Enemy)
                || !source.is(DamageTypeTags.IS_EXPLOSION)
                || !(source.getDirectEntity() instanceof Creeper creeper) || creeper == victim) return;
        CompoundTag tag = creeper.getPersistentData();
        long now = victim.level().getGameTime();
        int kills = tag.getLong(BLAST_TICK_TAG) == now ? tag.getInt(BLAST_KILLS_TAG) + 1 : 1;
        tag.putLong(BLAST_TICK_TAG, now);
        tag.putInt(BLAST_KILLS_TAG, kills);
        ServerPlayer player = null;
        if (creeper.getTarget() instanceof ServerPlayer targeted) {
            player = targeted;
        } else if (victim.level().getNearestPlayer(creeper, 32.0) instanceof ServerPlayer nearest) {
            player = nearest;
        }
        if (player != null) ContractTracker.fireEventAtLeast(player, "creeper_blast_kills", kills);
    }
    @SubscribeEvent
    public static void onFireballJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof LargeFireball ball && ball.getOwner() instanceof Ghast ghast) {
            ball.getPersistentData().putUUID(ContractConditions.FIREBALL_ORIGIN_TAG, ghast.getUUID());
        }
    }
    @SubscribeEvent
    public static void onShieldBlock(ShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ContractTracker.onResetTrigger(player, ContractObjective.RESET_SHIELD);
        }
    }
    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getFrom() == Level.NETHER) {
            ContractTracker.onResetTrigger(player, ContractObjective.RESET_LEAVE_NETHER);
        }
    }
    @SubscribeEvent
    public static void onRespawnReset(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level().dimension() != Level.NETHER) {
            ContractTracker.onResetTrigger(player, ContractObjective.RESET_LEAVE_NETHER);
        }
    }
    @SubscribeEvent
    public static void onBottleRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)
                || !event.getItemStack().is(Items.GLASS_BOTTLE)) return;
        boolean nearCloud = !player.level().getEntitiesOfClass(AreaEffectCloud.class,
                player.getBoundingBox().inflate(2.0),
                cloud -> cloud.isAlive() && cloud.getOwner() instanceof EnderDragon).isEmpty();
        if (nearCloud) ContractTracker.fireEvent(player, "dragon_breath_collected");
    }
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Creeper creeper)
                || creeper.level().isClientSide || creeper.tickCount % 10 != 0) return;
        CompoundTag tag = creeper.getPersistentData();
        if (tag.getBoolean(SCARED_TAG)) return;
        if (creeper.goalSelector.getRunningGoals().noneMatch(g -> g.getGoal() instanceof AvoidEntityGoal<?>)) return;
        for (Cat cat : creeper.level().getEntitiesOfClass(Cat.class, creeper.getBoundingBox().inflate(8.0), Cat::isTame)) {
            if (cat.getOwner() instanceof ServerPlayer owner) {
                tag.putBoolean(SCARED_TAG, true);
                ContractTracker.fireEvent(owner, "creeper_scared_by_cat");
                break;
            }
        }
    }
    @SubscribeEvent
    public static void onPearl(EntityTeleportEvent.EnderPearl event) {
        ServerPlayer player = event.getPlayer();
        if (!ContractTracker.hasEventObjective(player, "ender_pearl_while_chased")) return;
        boolean chased = !player.level().getEntitiesOfClass(EnderMan.class,
                player.getBoundingBox().inflate(32.0), e -> e.getTarget() == player).isEmpty();
        if (chased) ContractTracker.fireEvent(player, "ender_pearl_while_chased");
    }
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        if (event.getEntity() instanceof ServerPlayer player
                && effect == MobEffects.LEVITATION
                && event.getEffectSource() instanceof Shulker) {
            ContractTracker.fireEvent(player, "shulker_levitation");
        } else if (event.getEntity() instanceof ServerPlayer withered && effect == MobEffects.WITHER) {
            ContractTracker.onResetTrigger(withered, ContractObjective.RESET_WITHER);
        } else if (event.getEntity() instanceof Stray
                && effect == MobEffects.MOVEMENT_SLOWDOWN
                && event.getEffectSource() instanceof ServerPlayer source) {
            ContractTracker.fireEvent(source, "slowness_on_stray");
        }
    }
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Wolf wolf) || !wolf.isTame()
                || !(wolf.getOwner() instanceof ServerPlayer owner)
                || event.getEntity().getType() != EntityType.ZOMBIE
                || !ContractTracker.hasEventObjective(owner, "wolves_attack_zombie")) return;
        CompoundTag tag = event.getEntity().getPersistentData();
        ListTag wolves = tag.getList(WOLVES_TAG, Tag.TAG_STRING);
        String id = wolf.getStringUUID();
        for (int i = 0; i < wolves.size(); i++) {
            if (wolves.getString(i).equals(id)) return;
        }
        wolves.add(StringTag.valueOf(id));
        tag.put(WOLVES_TAG, wolves);
        ContractTracker.fireEventAtLeast(owner, "wolves_attack_zombie", wolves.size());
    }
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) return;
        if (ContractTracker.hasEventObjective(player, "has_wither_skull")
                && player.getInventory().contains(new ItemStack(Items.WITHER_SKELETON_SKULL))) {
            ContractTracker.fireEvent(player, "has_wither_skull");
        }
        if (ContractTracker.hasEventObjective(player, "wither_skulls_held")) {
            int skulls = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (stack.is(Items.WITHER_SKELETON_SKULL)) skulls += stack.getCount();
            }
            for (ItemStack stack : player.getInventory().offhand) {
                if (stack.is(Items.WITHER_SKELETON_SKULL)) skulls += stack.getCount();
            }
            if (skulls > 0) ContractTracker.fireEventAtLeast(player, "wither_skulls_held", skulls);
        }
    }
    @SubscribeEvent
    public static void onSonicBoom(LivingAttackEvent event) {
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.SONIC_BOOM) && source.getEntity() instanceof Warden warden) {
            warden.getPersistentData().putBoolean(ContractConditions.SONIC_BOOM_TAG, true);
        }
    }
    @SubscribeEvent
    public static void onEndCrystalRemoved(EntityLeaveLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof EndCrystal crystal)
                || crystal.getRemovalReason() != Entity.RemovalReason.KILLED) return;
        for (EnderDragon dragon : level.getDragons()) {
            dragon.getPersistentData().putBoolean(ContractConditions.CRYSTAL_TAG, true);
        }
    }
}
