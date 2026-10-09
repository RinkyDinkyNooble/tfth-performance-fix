package com.rinkynooble.tfthperformancefix.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.mcreator.thefleshthathates.block.FleshSpikesBlock;
import net.mcreator.thefleshthathates.block.entity.FleshSpikesTileEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws idle flesh spikes from a recording instead of through GeckoLib.
 *
 * <p>A flesh spike only animates while its block state's {@code animation} property is set
 * (a one-shot animation when something steps on it). The rest of the time GeckoLib still ran its
 * animation code and rebuilt all 16 cubes every frame, for every spike in view, and infected
 * ground is covered in them. An idle spike always has the same shape, so GeckoLib's output for
 * it is recorded once per block state and replayed: the same vertices, colours, texture
 * coordinates and render type, moved to the spike's position and given its light.
 */
public final class StaticSpikeRenderer {
    private static final Logger LOGGER = LogManager.getLogger("TFTH Performance Fix");
    private static final Map<BlockState, List<Recording>> RECORDINGS = new IdentityHashMap<>();
    /**
     * After an animation, GeckoLib eases the spike back to its resting shape over 5 ticks
     * ({@code GeoAnimatable.getBoneResetTime}), so a spike keeps going through GeckoLib for a
     * little longer than that after it was last seen animating.
     */
    private static final int SETTLE_TICKS = 10;
    private static final Map<FleshSpikesTileEntity, Long> LAST_ANIMATING = new WeakHashMap<>();
    private static boolean recording;
    /** Set if recording ever fails; GeckoLib then draws every spike, as without this mod. */
    private static boolean disabled;

    private StaticSpikeRenderer() {
    }

    /** Called when resources reload, since GeckoLib reloads the model then. */
    public static void clear() {
        RECORDINGS.clear();
        LAST_ANIMATING.clear();
    }

    /** Draws the spike if it's idle, and returns whether it did. GeckoLib draws it otherwise. */
    public static boolean render(GeoBlockRenderer<FleshSpikesTileEntity> renderer, FleshSpikesTileEntity spike, float partialTick,
                                 PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (recording || disabled) {
            return false;
        }
        List<Recording> recordings;
        try {
            if (spike.getLevel() == null) {
                return false;
            }
            long time = spike.getLevel().getGameTime();
            if (!isIdle(renderer, spike)) {
                LAST_ANIMATING.put(spike, time);
                return false;
            }
            Long lastAnimating = LAST_ANIMATING.get(spike);
            if (lastAnimating != null) {
                if (time - lastAnimating < SETTLE_TICKS) {
                    return false;
                }
                LAST_ANIMATING.remove(spike);
            }
            BlockState state = spike.getBlockState();
            recordings = RECORDINGS.get(state);
            if (recordings == null) {
                recordings = record(renderer, spike, partialTick, packedLight, packedOverlay);
                RECORDINGS.put(state, recordings);
            }
        } catch (RuntimeException e) {
            disabled = true;
            LOGGER.error("Drawing idle flesh spikes from a recording failed, so GeckoLib draws them all from now on.", e);
            return false;
        }
        PoseStack.Pose pose = poseStack.last();
        Matrix4f position = pose.pose();
        Matrix3f normal = pose.normal();
        Vector4f pos = new Vector4f();
        Vector3f norm = new Vector3f();
        for (Recording rec : recordings) {
            VertexConsumer buffer = bufferSource.getBuffer(rec.renderType);
            float[] v = rec.vertices;
            int[] overlays = rec.overlays;
            for (int i = 0, n = 0; i < v.length; i += Recording.FLOATS, n++) {
                position.transform(pos.set(v[i], v[i + 1], v[i + 2], 1.0F));
                normal.transform(norm.set(v[i + 9], v[i + 10], v[i + 11]));
                buffer.vertex(pos.x(), pos.y(), pos.z(), v[i + 3], v[i + 4], v[i + 5], v[i + 6], v[i + 7], v[i + 8],
                        overlays[n], packedLight, norm.x(), norm.y(), norm.z());
            }
        }
        return true;
    }

    /**
     * Idle means the block state doesn't ask for the animation and no controller is still
     * playing one (an animation finishing after the state was reset still goes through GeckoLib).
     */
    private static boolean isIdle(GeoBlockRenderer<FleshSpikesTileEntity> renderer, FleshSpikesTileEntity spike) {
        BlockState state = spike.getBlockState();
        if (!state.hasProperty(FleshSpikesBlock.ANIMATION) || state.getValue(FleshSpikesBlock.ANIMATION) != 0) {
            return false;
        }
        AnimatableInstanceCache cache = spike.getAnimatableInstanceCache();
        AnimatableManager<?> manager = cache.getManagerForId(renderer.getInstanceId(spike));
        for (AnimationController<?> controller : manager.getAnimationControllers().values()) {
            if (controller.getCurrentAnimation() != null && controller.getAnimationState() != AnimationController.State.STOPPED) {
                return false;
            }
        }
        return true;
    }

    /** Runs GeckoLib's own render once, at the origin, into buffers that keep what it writes. */
    private static List<Recording> record(GeoBlockRenderer<FleshSpikesTileEntity> renderer, FleshSpikesTileEntity spike, float partialTick,
                                          int packedLight, int packedOverlay) {
        Map<RenderType, Recorder> recorders = new LinkedHashMap<>();
        MultiBufferSource source = type -> recorders.computeIfAbsent(type, t -> new Recorder());
        recording = true;
        try {
            renderer.render(spike, partialTick, new PoseStack(), source, packedLight, packedOverlay);
        } finally {
            recording = false;
        }
        List<Recording> result = new ArrayList<>();
        recorders.forEach((type, recorder) -> result.add(recorder.finish(type)));
        return result;
    }

    private record Recording(RenderType renderType, float[] vertices, int[] overlays) {
        /** x, y, z, red, green, blue, alpha, u, v, normal x, y, z. */
        static final int FLOATS = 12;
    }

    /** Keeps every vertex written to it. Light is dropped: each spike's own light is used when replaying. */
    private static final class Recorder implements VertexConsumer {
        private final List<float[]> vertices = new ArrayList<>();
        private final List<Integer> overlays = new ArrayList<>();
        private float[] current = new float[Recording.FLOATS];
        private int currentOverlay;
        private boolean defaultColor;
        private final float[] defaultRgba = new float[4];

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v,
                           int overlay, int light, float normalX, float normalY, float normalZ) {
            this.vertices.add(new float[]{x, y, z, red, green, blue, alpha, u, v, normalX, normalY, normalZ});
            this.overlays.add(overlay);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.current[0] = (float) x;
            this.current[1] = (float) y;
            this.current[2] = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            this.current[3] = red / 255.0F;
            this.current[4] = green / 255.0F;
            this.current[5] = blue / 255.0F;
            this.current[6] = alpha / 255.0F;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.current[7] = u;
            this.current[8] = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            this.currentOverlay = (u & 0xFFFF) | (v << 16);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            this.current[9] = x;
            this.current[10] = y;
            this.current[11] = z;
            return this;
        }

        @Override
        public void endVertex() {
            if (this.defaultColor) {
                System.arraycopy(this.defaultRgba, 0, this.current, 3, 4);
            }
            this.vertices.add(this.current);
            this.overlays.add(this.currentOverlay);
            this.current = new float[Recording.FLOATS];
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            this.defaultColor = true;
            this.defaultRgba[0] = red / 255.0F;
            this.defaultRgba[1] = green / 255.0F;
            this.defaultRgba[2] = blue / 255.0F;
            this.defaultRgba[3] = alpha / 255.0F;
        }

        @Override
        public void unsetDefaultColor() {
            this.defaultColor = false;
        }

        Recording finish(RenderType type) {
            float[] flat = new float[this.vertices.size() * Recording.FLOATS];
            int[] overlayArray = new int[this.overlays.size()];
            for (int i = 0; i < this.vertices.size(); i++) {
                System.arraycopy(this.vertices.get(i), 0, flat, i * Recording.FLOATS, Recording.FLOATS);
                overlayArray[i] = this.overlays.get(i);
            }
            return new Recording(type, flat, overlayArray);
        }
    }
}
