package net.scottishfrogg.mobcontracts.covenant;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.registries.ForgeRegistries;
import net.scottishfrogg.mobcontracts.contract.ContractDefinition;
import net.scottishfrogg.mobcontracts.contract.ContractType;
import net.scottishfrogg.mobcontracts.contract.EliteTarget;
import net.scottishfrogg.mobcontracts.data.PlayerContractData;
import net.scottishfrogg.mobcontracts.team.ContractTeam;
import net.scottishfrogg.mobcontracts.tracking.ContractTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CovenantManager {
    public static final String OWNER_TAG = "contractmod_elite_owner";
    public static final String BEHAVIORS_TAG = "contractmod_elite_behaviors";
    public static final String KILL_EVENT = "elite_target_killed";
    private static final double SPAWN_RADIUS = 96.0;
    private static final ChatFormatting[] GLOW_COLORS = {
            ChatFormatting.RED, ChatFormatting.GOLD, ChatFormatting.YELLOW, ChatFormatting.GREEN,
            ChatFormatting.AQUA, ChatFormatting.BLUE, ChatFormatting.LIGHT_PURPLE, ChatFormatting.DARK_PURPLE,
            ChatFormatting.WHITE, ChatFormatting.DARK_AQUA, ChatFormatting.DARK_GREEN, ChatFormatting.DARK_RED,
            ChatFormatting.DARK_BLUE, ChatFormatting.GRAY};

    private CovenantManager() {}
    public static boolean hasOtherOnlinePlayer(ServerPlayer player) {
        return player.server.getPlayerList().getPlayers().stream().anyMatch(p -> p != player);
    }
    public static boolean canBegin(ServerPlayer player, ContractDefinition def) {
        if (def.targetsPlayer() && !hasOtherOnlinePlayer(player)) {
            player.displayClientMessage(
                    Component.literal("No other player is online to hunt").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }
    public static void begin(ServerPlayer player, ContractDefinition def) {
        PlayerContractData.clearCovenantData(player);
        CompoundTag cov = PlayerContractData.getCovenantData(player);
        if (def.targetsPlayer()) {
            List<ServerPlayer> others = new ArrayList<>(player.server.getPlayerList().getPlayers());
            others.remove(player);
            ServerPlayer target = others.get(player.getRandom().nextInt(others.size()));
            cov.putUUID("TargetPlayer", target.getUUID());
            cov.putString("TargetName", target.getName().getString());
            storeLastPosition(cov, target.blockPosition());
            cov.putString("Dim", target.level().dimension().location().toString());
            player.sendSystemMessage(Component.literal("Covenant started: hunt " + target.getName().getString()
                    + ". Follow the compass.").withStyle(ChatFormatting.GOLD));
        } else if (def.elite() != null) {
            plan(player, def.elite(), cov);
            player.sendSystemMessage(Component.literal("Covenant started: " + def.elite().name() + " is hiding in "
                    + dimensionName(def.elite().dimension()) + ". Follow the compass.").withStyle(ChatFormatting.GOLD));
        }
    }
    private static String dimensionName(ResourceLocation id) {
        return switch (id.getPath()) {
            case "overworld" -> "the Overworld";
            case "the_nether" -> "the Nether";
            case "the_end" -> "the End";
            default -> id.toString();
        };
    }
    private static void plan(ServerPlayer player, EliteTarget elite, CompoundTag cov) {
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, elite.dimension());
        ServerLevel level = player.server.getLevel(dimKey);
        if (level == null) {
            level = player.server.overworld();
            dimKey = Level.OVERWORLD;
        }
        double originX, originZ;
        if (elite.anchorAtWorldOrigin()) {
            originX = 0;
            originZ = 0;
        } else if (player.level().dimension() == dimKey) {
            originX = player.getX();
            originZ = player.getZ();
        } else {
            double ratio = player.level().dimensionType().coordinateScale() / level.dimensionType().coordinateScale();
            originX = player.getX() * ratio;
            originZ = player.getZ() * ratio;
        }
        RandomSource random = player.getRandom();
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = elite.minDistance() + random.nextDouble() * Math.max(0, elite.maxDistance() - elite.minDistance());
        BlockPos target = level.getWorldBorder().clampToBounds(
                originX + Math.cos(angle) * dist, 64, originZ + Math.sin(angle) * dist);
        cov.putString("Dim", dimKey.location().toString());
        cov.putInt("X", target.getX());
        cov.putInt("Z", target.getZ());
        cov.putBoolean("Spawned", false);
        cov.remove("EliteId");
    }
    public static void end(ServerPlayer player) {
        CompoundTag cov = PlayerContractData.getCovenantData(player);
        discardElite(player.server, cov);
        PlayerContractData.clearCovenantData(player);
        CovenantCompass.removeAll(player);
    }
    private static void discardElite(MinecraftServer server, CompoundTag cov) {
        if (!cov.hasUUID("EliteId")) return;
        UUID id = cov.getUUID("EliteId");
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null && e.isAlive()) e.discard();
        }
    }
    public static void tick(ServerPlayer player) {
        ContractDefinition def = PlayerContractData.getActive(player);
        ServerPlayer owner = player;
        if (def == null || def.type() != ContractType.COVENANT) {
            owner = null;
            for (ServerPlayer host : ContractTeam.hostsOf(player)) {
                ContractDefinition hostDef = PlayerContractData.getActive(host);
                if (hostDef != null && hostDef.type() == ContractType.COVENANT) {
                    owner = host;
                    def = hostDef;
                    break;
                }
            }
            if (owner == null) {
                if (CovenantCompass.has(player)) CovenantCompass.removeAll(player);
                return;
            }
        }
        CovenantCompass.sweep(player);
        CompoundTag cov = PlayerContractData.getCovenantData(owner);
        if (owner == player) {
            if (def.targetsPlayer()) {
                trackPlayerTarget(owner, cov);
            } else if (def.elite() != null) {
                if (!cov.contains("X")) plan(owner, def.elite(), cov);
                if (!cov.getBoolean("Spawned")) {
                    trySpawn(owner, def, def.elite(), cov);
                } else {
                    verifyElite(owner, cov);
                }
            }
        }
        BlockPos pointAt;
        if ((def.targetsPlayer() || cov.getBoolean("Spawned")) && cov.contains("LastX")) {
            pointAt = new BlockPos(cov.getInt("LastX"), cov.getInt("LastY"), cov.getInt("LastZ"));
        } else {
            pointAt = new BlockPos(cov.getInt("X"), 64, cov.getInt("Z"));
        }
        CovenantCompass.ensureAndPoint(player, dimension(cov), pointAt);
    }
    private static ResourceKey<Level> dimension(CompoundTag cov) {
        ResourceLocation id = ResourceLocation.tryParse(cov.getString("Dim"));
        return ResourceKey.create(Registries.DIMENSION, id != null ? id : Level.OVERWORLD.location());
    }
    private static void storeLastPosition(CompoundTag cov, BlockPos pos) {
        cov.putInt("LastX", pos.getX());
        cov.putInt("LastY", pos.getY());
        cov.putInt("LastZ", pos.getZ());
    }
    private static void trackPlayerTarget(ServerPlayer owner, CompoundTag cov) {
        if (!cov.hasUUID("TargetPlayer")) return;
        ServerPlayer target = owner.server.getPlayerList().getPlayer(cov.getUUID("TargetPlayer"));
        if (target == null) return;
        cov.putString("Dim", target.level().dimension().location().toString());
        storeLastPosition(cov, target.blockPosition());
    }
    private static void trySpawn(ServerPlayer owner, ContractDefinition def, EliteTarget elite, CompoundTag cov) {
        ResourceKey<Level> dimKey = dimension(cov);
        int x = cov.getInt("X"), z = cov.getInt("Z");
        ServerPlayer nearby = null;
        for (ServerPlayer p : ContractTeam.participants(owner)) {
            if (p.level().dimension() != dimKey) continue;
            double dx = p.getX() - x, dz = p.getZ() - z;
            if (dx * dx + dz * dz <= SPAWN_RADIUS * SPAWN_RADIUS) {
                nearby = p;
                break;
            }
        }
        if (nearby == null) return;
        ServerLevel level = nearby.serverLevel();
        RandomSource random = level.random;
        for (int attempt = 0; attempt < 20; attempt++) {
            int sx = x + (attempt == 0 ? 0 : random.nextInt(81) - 40);
            int sz = z + (attempt == 0 ? 0 : random.nextInt(81) - 40);
            BlockPos pos = findSpawnPos(level, sx, sz);
            if (pos != null && spawnElite(level, def, elite, pos, owner, cov) != null) return;
        }
        cov.putInt("X", x + random.nextInt(97) - 48);
        cov.putInt("Z", z + random.nextInt(97) - 48);
    }
    private static BlockPos findSpawnPos(ServerLevel level, int x, int z) {
        if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight() + 1, z))) return null;
        if (level.dimension() == Level.NETHER) {
            for (int y = 110; y > 33; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockPos below = pos.below();
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP)
                        && level.getFluidState(below).isEmpty() && level.getFluidState(pos).isEmpty()) {
                    return pos;
                }
            }
            return null;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        if (y <= level.getMinBuildHeight() + 1 || level.getBlockState(below).isAir()) return null;
        if (!level.getFluidState(below).isEmpty() || !level.getFluidState(pos).isEmpty()) return null;
        if (level.getBlockState(below).is(BlockTags.LOGS) || level.getBlockState(below).is(BlockTags.LEAVES)) return null;
        return pos;
    }
    private static Mob spawnElite(ServerLevel level, ContractDefinition def, EliteTarget elite, BlockPos pos,
                                  ServerPlayer owner, CompoundTag cov) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(elite.entity());
        if (type == null) return null;
        Entity entity = type.create(level);
        if (!(entity instanceof Mob mob)) return null;
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null, null);
        for (Entity passenger : new ArrayList<>(mob.getPassengers())) passenger.discard();
        Entity vehicle = mob.getVehicle();
        if (vehicle != null) {
            mob.stopRiding();
            vehicle.discard();
        }
        mob.setBaby(false);
        mob.removeAllEffects();
        mob.setCanPickUpLoot(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
            mob.setDropChance(slot, 0.0F);
        }
        elite.equipment().forEach((slot, item) -> mob.setItemSlot(slot, item.template()));
        for (EliteTarget.EliteEffect fx : elite.effects()) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(fx.effect());
            if (effect != null) {
                mob.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, fx.amplifier(), false, false));
            }
        }
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, MobEffectInstance.INFINITE_DURATION, 0, false, false));
        mob.setHealth(mob.getMaxHealth());
        mob.setCustomName(Component.literal(elite.name()));
        mob.setCustomNameVisible(true);
        mob.setPersistenceRequired();
        mob.setGlowingTag(true);
        assignRandomGlowColor(level, mob);
        CompoundTag data = mob.getPersistentData();
        data.putUUID(OWNER_TAG, owner.getUUID());
        data.putString(BEHAVIORS_TAG, String.join(",", elite.behaviors()));
        cov.putBoolean("Spawned", true);
        cov.putUUID("EliteId", mob.getUUID());
        storeLastPosition(cov, pos);
        if (!level.addFreshEntity(mob)) {
            cov.putBoolean("Spawned", false);
            cov.remove("EliteId");
            return null;
        }
        return mob;
    }
    private static void assignRandomGlowColor(ServerLevel level, Mob mob) {
        ChatFormatting color = GLOW_COLORS[level.random.nextInt(GLOW_COLORS.length)];
        Scoreboard scoreboard = level.getScoreboard();
        String teamName = "cm_elite_" + color.getName();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setColor(color);
        }
        scoreboard.addPlayerToTeam(mob.getStringUUID(), team);
    }
    private static void verifyElite(ServerPlayer player, CompoundTag cov) {
        UUID id = cov.hasUUID("EliteId") ? cov.getUUID("EliteId") : null;
        ServerLevel level = player.server.getLevel(dimension(cov));
        Entity entity = (id == null || level == null) ? null : level.getEntity(id);
        if (entity instanceof Mob elite && elite.isAlive()) {
            storeLastPosition(cov, elite.blockPosition());
            if (elite.getPersistentData().getString(BEHAVIORS_TAG).contains("hostile_to_owner")) {
                makeHostile(elite, nearestParticipant(player, elite));
            }
            return;
        }
        if (entity != null) return;
        BlockPos last = new BlockPos(cov.getInt("LastX"), cov.getInt("LastY"), cov.getInt("LastZ"));
        if (level != null && level.hasChunkAt(last)) {
            cov.putBoolean("Spawned", false);
            cov.remove("EliteId");
            cov.putInt("X", last.getX());
            cov.putInt("Z", last.getZ());
        }
    }
    private static ServerPlayer nearestParticipant(ServerPlayer owner, Mob elite) {
        ServerPlayer best = owner;
        double bestDist = Double.MAX_VALUE;
        for (ServerPlayer p : ContractTeam.participants(owner)) {
            if (p.level() != elite.level()) continue;
            double d = p.distanceToSqr(elite);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }
    private static void makeHostile(Mob elite, ServerPlayer player) {
        elite.setTarget(player);
        elite.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, player.getUUID(), 600L);
        elite.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, player);
    }
    public static boolean isOrphanedElite(ServerLevel level, Mob mob) {
        CompoundTag data = mob.getPersistentData();
        if (!data.hasUUID(OWNER_TAG)) return false;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.getUUID(OWNER_TAG));
        if (owner == null) return false; // owner offline: keep it until they are back
        ContractDefinition def = PlayerContractData.getActive(owner);
        if (def == null || def.type() != ContractType.COVENANT) return true;
        CompoundTag cov = PlayerContractData.getCovenantData(owner);
        return !(cov.hasUUID("EliteId") && cov.getUUID("EliteId").equals(mob.getUUID()));
    }
    private static boolean creditsKill(ServerPlayer owner, DamageSource source) {
        Entity killer = source.getEntity();
        return killer == owner
                || (killer instanceof ServerPlayer p && ContractTeam.isHelperOf(owner, p.getUUID()));
    }
    public static void onLivingDeath(LivingEntity victim, DamageSource source) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        MinecraftServer server = level.getServer();
        if (victim instanceof ServerPlayer dead) {
            for (ServerPlayer hunter : server.getPlayerList().getPlayers()) {
                ContractDefinition def = PlayerContractData.getActive(hunter);
                if (def == null || !def.targetsPlayer() || !creditsKill(hunter, source)) continue;
                CompoundTag cov = PlayerContractData.getCovenantData(hunter);
                if (cov.hasUUID("TargetPlayer") && cov.getUUID("TargetPlayer").equals(dead.getUUID())) {
                    ContractTracker.fireOwnEvent(hunter, KILL_EVENT);
                }
            }
            return;
        }
        CompoundTag data = victim.getPersistentData();
        if (!data.hasUUID(OWNER_TAG)) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(data.getUUID(OWNER_TAG));
        if (owner == null || !PlayerContractData.hasActive(owner)) return;
        CompoundTag cov = PlayerContractData.getCovenantData(owner);
        if (!cov.hasUUID("EliteId") || !cov.getUUID("EliteId").equals(victim.getUUID())) return;
        if (creditsKill(owner, source)) {
            ContractTracker.fireOwnEvent(owner, KILL_EVENT);
        } else {
            cov.putBoolean("Spawned", false);
            cov.remove("EliteId");
            cov.putInt("X", victim.getBlockX());
            cov.putInt("Z", victim.getBlockZ());
            owner.displayClientMessage(Component.literal("The elite target fell to something else. Another will take its place.")
                    .withStyle(ChatFormatting.YELLOW), true);
        }
    }
    public static void onExplosion(ServerLevel level, Entity exploder) {
        if (!(exploder instanceof Mob creeper)) return;
        CompoundTag data = creeper.getPersistentData();
        if (!data.hasUUID(OWNER_TAG) || !data.getString(BEHAVIORS_TAG).contains("respawn_on_explosion")) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.getUUID(OWNER_TAG));
        if (owner == null) return;
        ContractDefinition def = PlayerContractData.getActive(owner);
        if (def == null || def.type() != ContractType.COVENANT || def.elite() == null) return;
        CompoundTag cov = PlayerContractData.getCovenantData(owner);
        if (!cov.hasUUID("EliteId") || !cov.getUUID("EliteId").equals(creeper.getUUID())) return;
        cov.putBoolean("Spawned", false);
        cov.remove("EliteId");
        ServerLevel ownerLevel = owner.serverLevel();
        if (ownerLevel.dimension() == level.dimension()) {
            for (int attempt = 0; attempt < 12; attempt++) {
                double angle = ownerLevel.random.nextDouble() * Math.PI * 2;
                int sx = (int) Math.floor(owner.getX() + Math.cos(angle) * 20);
                int sz = (int) Math.floor(owner.getZ() + Math.sin(angle) * 20);
                BlockPos pos = findSpawnPos(ownerLevel, sx, sz);
                if (pos != null && spawnElite(ownerLevel, def, def.elite(), pos, owner, cov) != null) {
                    owner.displayClientMessage(Component.literal(def.elite().name() + " has returned...")
                            .withStyle(ChatFormatting.RED), true);
                    return;
                }
            }
        }
        cov.putInt("X", creeper.getBlockX());
        cov.putInt("Z", creeper.getBlockZ());
    }
    public static boolean debugMoveNear(ServerPlayer player) {
        ContractDefinition def = PlayerContractData.getActive(player);
        if (def == null || def.type() != ContractType.COVENANT || def.elite() == null) return false;
        CompoundTag cov = PlayerContractData.getCovenantData(player);
        if (player.level().dimension() != dimension(cov)) return false;
        discardElite(player.server, cov);
        cov.putInt("X", player.getBlockX() + 40);
        cov.putInt("Z", player.getBlockZ());
        cov.putBoolean("Spawned", false);
        cov.remove("EliteId");
        return true;
    }
}
