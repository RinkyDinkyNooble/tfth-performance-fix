package com.rinkynooble.tfthperformancefix.verify;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.rinkynooble.tfthperformancefix.SectionScan;
import com.rinkynooble.tfthperformancefix.SpreadAccess;
import com.rinkynooble.tfthperformancefix.TfthBlocks;
import net.mcreator.thefleshthathates.FEvents.FleshWorld;
import net.mcreator.thefleshthathates.FleshBlockSpread;
import net.mcreator.thefleshthathates.IncubatorMobSpawn;
import net.mcreator.thefleshthathates.TickMethods;
import net.mcreator.thefleshthathates.block.FleshBlocks;
import net.mcreator.thefleshthathates.entity.PlaqueincubatorstartEntity;
import net.mcreator.thefleshthathates.init.TheFleshThatHatesModBlocks;
import net.mcreator.thefleshthathates.init.TheFleshThatHatesModEntities;
import net.mcreator.thefleshthathates.init.TheFleshThatHatesModMobEffects;
import net.mcreator.thefleshthathates.potion.GOHMobEffect;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Dev-only checks for TFTH Performance Fix, run as server commands. Never shipped.
 * Each check compares the patched code with a plain re-implementation of what TFTH's code did,
 * restricted to loaded chunks, and prints PASS or FAIL.
 */
@Mod("tfthperformancefix_verify")
public class TfthVerify {
    public TfthVerify() {
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tfthverify")
                .then(Commands.literal("scatter")
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 200))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100000))
                                        .then(Commands.argument("seed", IntegerArgumentType.integer())
                                                .executes(safe(TfthVerify::scatter))))))
                .then(Commands.literal("scan").executes(safe(TfthVerify::scan)))
                .then(Commands.literal("goh").executes(safe(TfthVerify::goh)))
                .then(Commands.literal("brute").executes(safe(TfthVerify::brute)))
                .then(Commands.literal("hunter").executes(safe(TfthVerify::hunter)))
                .then(Commands.literal("evolve").executes(safe(TfthVerify::evolve)))
                .then(Commands.literal("spreads").executes(safe(TfthVerify::spreads)))
                .then(Commands.literal("chunks").executes(safe(TfthVerify::chunks)))
                .then(Commands.literal("placeincubator").executes(safe(TfthVerify::placeIncubator))));
    }

    @FunctionalInterface
    private interface Check {
        int run(CommandContext<CommandSourceStack> ctx) throws Exception;
    }

    /** Reports an exception as a failed check instead of a command error. */
    private static Command<CommandSourceStack> safe(Check check) {
        return ctx -> {
            try {
                return check.run(ctx);
            } catch (Exception e) {
                Throwable cause = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
                say(ctx, "ERROR " + cause);
                org.apache.logging.log4j.LogManager.getLogger("tfthverify").error("Check failed", cause);
                return 0;
            }
        };
    }

    private static void say(CommandContext<CommandSourceStack> ctx, String text) {
        ctx.getSource().sendSuccess(() -> Component.literal(text), false);
    }

    /** Places random TFTH blocks, fire and lava in loaded chunks around the source. */
    private static int scatter(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos center = BlockPos.containing(ctx.getSource().getPosition());
        int radius = IntegerArgumentType.getInteger(ctx, "radius");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        RandomSource random = RandomSource.create(IntegerArgumentType.getInteger(ctx, "seed"));
        List<Block> blocks = new ArrayList<>(FleshBlocks.ALL_BLOCKS);
        blocks.add(Blocks.FIRE);
        blocks.add(Blocks.LAVA);
        blocks.add(TheFleshThatHatesModBlocks.FLESH_BLOCK.get());
        blocks.add(TheFleshThatHatesModBlocks.FLESH_BLOCK.get());
        int placed = 0;
        for (int i = 0; i < count; i++) {
            BlockPos pos = center.offset(random.nextInt(2 * radius + 1) - radius, random.nextInt(41) - 20, random.nextInt(2 * radius + 1) - radius);
            if (SectionScan.getIfLoaded(level, pos) == null || level.isOutsideBuildHeight(pos)) {
                continue;
            }
            Block block = blocks.get(random.nextInt(blocks.size()));
            // Flag 2 without neighbour updates, so fire and lava stay where they are put.
            level.setBlock(pos, block.defaultBlockState(), 2 | 16);
            if (block == TheFleshThatHatesModBlocks.FLESH_BLOCK.get() && random.nextBoolean()) {
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2 | 16);
                for (BlockPos side : new BlockPos[]{pos.above().north(), pos.above().south(), pos.above().east(), pos.above().west()}) {
                    if (SectionScan.getIfLoaded(level, side) != null) {
                        level.setBlock(side, Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                }
            }
            placed++;
        }
        say(ctx, "Placed " + placed + " blocks around " + center.toShortString());
        return placed;
    }

    /** TFTH's way: read every position of the cube in x, y, z order (loaded chunks only). */
    private static List<BlockPos> naiveMatches(ServerLevel level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                                              Predicate<BlockState> match) {
        List<BlockPos> found = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    pos.set(x, y, z);
                    BlockState state = SectionScan.getIfLoaded(level, pos);
                    if (state != null && match.test(state)) {
                        found.add(pos.immutable());
                    }
                }
            }
        }
        return found;
    }

    private static boolean loadedAir(ServerLevel level, BlockPos pos) {
        BlockState state = SectionScan.getIfLoaded(level, pos);
        return state != null && state.isAir();
    }

    private static int scan(CommandContext<CommandSourceStack> ctx) throws Exception {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
        boolean allPass = true;

        // Fog and music: count and nearest distance of TFTH blocks within 30.
        List<BlockPos> naiveMod = naiveMatches(level, c.getX() - 30, c.getY() - 30, c.getZ() - 30, c.getX() + 30, c.getY() + 30, c.getZ() + 30, TfthBlocks.MOD_BLOCK);
        double naiveNearest = Double.MAX_VALUE;
        for (BlockPos pos : naiveMod) {
            naiveNearest = Math.min(naiveNearest, pos.distSqr(c));
        }
        SectionScan.CountResult fast = SectionScan.countInCube(level, c, 30, TfthBlocks.MOD_BLOCK);
        boolean fogPass = fast.count == naiveMod.size() && fast.nearestDistSqr == naiveNearest;
        allPass &= fogPass;
        say(ctx, (fogPass ? "PASS" : "FAIL") + " fog/music: count " + fast.count + " vs " + naiveMod.size()
                + ", nearest " + fast.nearestDistSqr + " vs " + naiveNearest);

        // Fire and lava within 30, in TFTH's order.
        Predicate<BlockState> fireOrLava = state -> state.is(Blocks.FIRE) || state.is(Blocks.LAVA);
        List<BlockPos> naiveFire = naiveMatches(level, c.getX() - 30, c.getY() - 30, c.getZ() - 30, c.getX() + 30, c.getY() + 30, c.getZ() + 30, fireOrLava);
        List<BlockPos> fastFire = new ArrayList<>();
        SectionScan.forEachMatch(level, c.getX() - 30, c.getY() - 30, c.getZ() - 30, c.getX() + 30, c.getY() + 30, c.getZ() + 30, fireOrLava,
                (x, y, z, state) -> fastFire.add(new BlockPos(x, y, z)));
        fastFire.sort(TfthBlocks.LOOP_ORDER);
        boolean firePass = fastFire.equals(naiveFire);
        allPass &= firePass;
        say(ctx, (firePass ? "PASS" : "FAIL") + " fire/lava: " + fastFire.size() + " vs " + naiveFire.size());

        // Incubator spawn positions, for both incubator sizes, through the patched TFTH method.
        Method pick = IncubatorMobSpawn.class.getDeclaredMethod("getRandomFleshBlockPosition", ServerLevel.class, LivingEntity.class);
        pick.setAccessible(true);
        for (EntityType<? extends Mob> type : List.of(TheFleshThatHatesModEntities.PLAQUEINCUBATORSTART.get(), TheFleshThatHatesModEntities.PLAQUEINCUBATORONE.get())) {
            Mob incubator = type.create(level);
            incubator.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
            int r = type == TheFleshThatHatesModEntities.PLAQUEINCUBATORSTART.get() ? 75 : 125;
            List<BlockPos> naive = new ArrayList<>();
            for (BlockPos pos : naiveMatches(level, c.getX() - r, c.getY() - 20, c.getZ() - r, c.getX() + r, c.getY() + 20, c.getZ() + r, TfthBlocks.FLESH_BLOCK)) {
                BlockPos above = pos.above();
                if (loadedAir(level, above) && loadedAir(level, above.north()) && loadedAir(level, above.south())
                        && loadedAir(level, above.east()) && loadedAir(level, above.west())) {
                    naive.add(pos);
                }
            }
            long seed = 12345L;
            level.random.setSeed(seed);
            BlockPos fastPick = (BlockPos) pick.invoke(null, level, incubator);
            level.random.setSeed(seed);
            BlockPos naivePick = naive.isEmpty() ? null : naive.get(level.random.nextInt(naive.size()));
            boolean pickPass = java.util.Objects.equals(fastPick, naivePick);
            allPass &= pickPass;
            say(ctx, (pickPass ? "PASS" : "FAIL") + " spawn pick r=" + r + ": " + naive.size() + " candidates, picked "
                    + (fastPick == null ? "none" : fastPick.toShortString()) + " vs " + (naivePick == null ? "none" : naivePick.toShortString()));
            incubator.discard();
        }

        say(ctx, allPass ? "SCAN ALL PASS" : "SCAN HAD FAILURES");
        return allPass ? 1 : 0;
    }

    /** Gives a zombie GOH's speed modifier without the effect, then applies GOH. TFTH crashed here. */
    private static int goh(CommandContext<CommandSourceStack> ctx) throws Exception {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
        Mob zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        level.addFreshEntity(zombie);
        Field idField = GOHMobEffect.class.getDeclaredField("MOVEMENT_SPEED_ID");
        idField.setAccessible(true);
        UUID id = (UUID) idField.get(null);
        zombie.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(
                new AttributeModifier(id, "GOH speed reduction", -0.2, AttributeModifier.Operation.MULTIPLY_TOTAL));
        try {
            zombie.addEffect(new MobEffectInstance(TheFleshThatHatesModMobEffects.GOH.get(), 100, 1));
            int modifiers = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(id) != null ? 1 : 0;
            double amount = zombie.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(id).getAmount();
            say(ctx, "PASS goh: no crash, modifier present " + modifiers + ", amount " + amount + " (expected -0.4)");
        } catch (IllegalArgumentException e) {
            say(ctx, "FAIL goh: " + e.getMessage());
        } finally {
            zombie.discard();
        }
        return 1;
    }

    /** Hurts a flesh human next to a brute at the Awareness stage, and compares the damage taken. */
    private static int brute(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
        if (FleshWorld.getPoints() < 3000) {
            FleshWorld.addPoints(3000 - FleshWorld.getPoints(), level);
        }
        Mob human = TheFleshThatHatesModEntities.FLESH_HUMAN.get().create(level);
        human.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        human.setNoAi(true);
        level.addFreshEntity(human);
        Mob brute = TheFleshThatHatesModEntities.BRUTEPLAQUECREATUREONE.get().create(level);
        brute.moveTo(c.getX() + 3.5, c.getY(), c.getZ() + 0.5);
        brute.setNoAi(true);
        level.addFreshEntity(brute);
        float before = human.getHealth();
        human.hurt(level.damageSources().magic(), 10.0F);
        float taken = before - human.getHealth();
        float reduction = net.mcreator.thefleshthathates.FConfig.TFTHConfiguration.getAwarenessStageDamageReduction();
        float expected = 10.0F * (1.0F - reduction) * 0.7F;
        boolean pass = Math.abs(taken - expected) < 0.01F;
        say(ctx, (pass ? "PASS" : "FAIL") + " brute: stage " + FleshWorld.getCurrentStage() + ", reduction " + reduction
                + ", took " + taken + ", expected " + expected + " (TFTH's: " + (10.0F * 0.7F) + ")");
        human.discard();
        brute.discard();
        return pass ? 1 : 0;
    }

    /**
     * Compares the Flesh Hunter III's registered size with its real one. Presence Footsteps
     * divides the second by the first: 1 is right, infinity is what TFTH's 0 by 0 gave.
     */
    private static int hunter(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        EntityType<?> type = TheFleshThatHatesModEntities.FLESH_HUNTER_THREE.get();
        Mob hunter = (Mob) type.create(level);
        BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
        hunter.moveTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
        float ratio = hunter.getBbWidth() / type.getDimensions().width;
        boolean pass = type.getDimensions().width == 1.5F && type.getDimensions().height == 3.3F && ratio == 1.0F;
        say(ctx, (pass ? "PASS" : "FAIL") + " hunter: registered " + type.getDimensions().width + " x " + type.getDimensions().height
                + ", real " + hunter.getBbWidth() + " x " + hunter.getBbHeight() + ", ratio " + ratio);
        hunter.discard();
        return pass ? 1 : 0;
    }

    /** Makes the nearest starting incubator evolve on its next tick. */
    private static int evolve(CommandContext<CommandSourceStack> ctx) throws Exception {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos c = BlockPos.containing(ctx.getSource().getPosition());
        List<PlaqueincubatorstartEntity> found = level.getEntitiesOfClass(PlaqueincubatorstartEntity.class, new AABB(c).inflate(32));
        if (found.isEmpty()) {
            say(ctx, "No starting incubator within 32 blocks");
            return 0;
        }
        if (FleshWorld.getPoints() < 3000) {
            FleshWorld.addPoints(3000 - FleshWorld.getPoints(), level);
        }
        Field time = PlaqueincubatorstartEntity.class.getDeclaredField("timeSinceSpawn");
        time.setAccessible(true);
        time.setInt(found.get(0), Integer.MAX_VALUE / 2);
        say(ctx, "Incubator at " + found.get(0).blockPosition().toShortString() + " evolves next tick");
        return 1;
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static Object call(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        return m.invoke(target);
    }

    private static int chunks(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        say(ctx, "Loaded chunks in " + level.dimension().location() + ": " + level.getChunkSource().getLoadedChunksCount());
        return 1;
    }

    /** Runs TFTH's incubator placement once for a fake player at the source position. */
    private static int placeIncubator(CommandContext<CommandSourceStack> ctx) throws Exception {
        ServerLevel level = ctx.getSource().getLevel();
        net.minecraftforge.common.util.FakePlayer player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(level);
        player.moveTo(ctx.getSource().getPosition());
        int chunksBefore = level.getChunkSource().getLoadedChunksCount();
        int before = level.getEntities(TheFleshThatHatesModEntities.PLAQUEINCUBATORSTART.get(), e -> true).size();
        Method place = net.mcreator.thefleshthathates.IncubatorSpawn.class.getDeclaredMethod("processFirstTickEvent",
                ServerLevel.class, net.minecraft.world.entity.player.Player.class, net.mcreator.thefleshthathates.FleshWorldData.class);
        place.setAccessible(true);
        long start = System.nanoTime();
        place.invoke(null, level, player, net.mcreator.thefleshthathates.FleshWorldData.get(level.getServer()));
        long micros = (System.nanoTime() - start) / 1000;
        int after = level.getEntities(TheFleshThatHatesModEntities.PLAQUEINCUBATORSTART.get(), e -> true).size();
        say(ctx, "placeincubator at " + BlockPos.containing(ctx.getSource().getPosition()).toShortString() + ": incubators " + before + " -> " + after
                + ", loaded chunks " + chunksBefore + " -> " + level.getChunkSource().getLoadedChunksCount() + ", took " + micros + " us");
        return 1;
    }

    private static int spreads(CommandContext<CommandSourceStack> ctx) throws Exception {
        Field visited = FleshBlockSpread.class.getField("visitedPositions");
        Field state = FleshBlockSpread.class.getDeclaredField("state");
        state.setAccessible(true);
        say(ctx, TickMethods.fleshBlockSpreads.size() + " spreads");
        for (FleshBlockSpread spread : TickMethods.fleshBlockSpreads) {
            ServerLevel level = ((SpreadAccess) spread).tfthperformancefix$getLevel();
            Object waiting = field(spread, "tfthperformancefix$waiting");
            int waitingCount = 0;
            for (Object list : ((java.util.Map<?, ?>) waiting).values()) {
                waitingCount += ((java.util.List<?>) list).size();
            }
            say(ctx, " - " + (level == null ? "null" : level.dimension().location()) + " " + spread.origin.toShortString()
                    + " waiting " + waitingCount + " in " + ((java.util.Map<?, ?>) waiting).size() + " chunks,"
                    + " state " + state.get(spread) + ", visited " + ((java.util.Set<?>) visited.get(spread)).size()
                    + " (" + visited.get(spread).getClass().getSimpleName() + ")"
                    + ", queue " + ((java.util.Collection<?>) field(spread, "queue")).size()
                    + ", cooldown " + field(spread, "isCooldown") + " " + field(spread, "cooldownCounter")
                    + ", spreadCounter " + field(spread, "spreadCounter") + ", rateCounter " + field(spread, "spreadRateCounter")
                    + "/" + field(spread, "INITIAL_SPREAD_RATE") + ", tickCooldown " + field(spread, "tickCooldown")
                    + ", originLoaded " + (level != null && level.getChunkSource().getChunkNow(spread.origin.getX() >> 4, spread.origin.getZ() >> 4) != null)
                    + ", sameLevelObject " + (level == ctx.getSource().getServer().getLevel(level.dimension()))
                    + ", checkRadius " + field(spread, "CHECK_RADIUS") + ", incubatorsNearby " + call(spread, "areIncubatorsNearby"));
        }
        return 1;
    }
}
