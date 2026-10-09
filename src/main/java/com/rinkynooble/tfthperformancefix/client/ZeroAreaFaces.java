package com.rinkynooble.tfthperformancefix.client;

import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.cache.object.GeoCube;
import software.bernie.geckolib.cache.object.GeoQuad;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Removes the faces with no area from TFTH's models.
 *
 * <p>Many of TFTH's models use flat cubes (planes): a cube with zero size on one axis still gets
 * six faces from GeckoLib, four of which have no area. They draw no pixels but still cost a
 * transform and four vertices each, every frame (72 of the Flesh Vindicator's 462 faces).
 * GeckoLib skips faces that are null, so those faces are set to null, once per baked model.
 */
public final class ZeroAreaFaces {
    private static final Set<BakedGeoModel> DONE = Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<>()));

    private ZeroAreaFaces() {
    }

    public static void removeFrom(BakedGeoModel model) {
        if (DONE.add(model)) {
            for (GeoBone bone : model.topLevelBones()) {
                removeFrom(bone);
            }
        }
    }

    private static void removeFrom(GeoBone bone) {
        for (GeoCube cube : bone.getCubes()) {
            GeoQuad[] quads = cube.quads();
            if (quads == null) {
                continue;
            }
            for (int i = 0; i < quads.length; i++) {
                if (quads[i] != null && hasNoArea(quads[i])) {
                    quads[i] = null;
                }
            }
        }
        for (GeoBone child : bone.getChildBones()) {
            removeFrom(child);
        }
    }

    /**
     * The cross product of a quad's diagonals has twice its area as length. On a flat cube's
     * edge faces two coordinates are equal in all four corners, so it comes out exactly zero;
     * very thin faces that still have some area are kept.
     */
    private static boolean hasNoArea(GeoQuad quad) {
        if (quad.vertices().length != 4) {
            return false;
        }
        Vector3f a = quad.vertices()[0].position();
        Vector3f b = quad.vertices()[1].position();
        Vector3f c = quad.vertices()[2].position();
        Vector3f d = quad.vertices()[3].position();
        Vector3f diagonal1 = new Vector3f(c).sub(a);
        Vector3f diagonal2 = new Vector3f(d).sub(b);
        return diagonal1.cross(diagonal2).lengthSquared() == 0.0F;
    }
}
