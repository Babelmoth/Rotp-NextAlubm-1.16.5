package com.nextalubm.rotp_nextalbum.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public final class BeachBoyLinePath {
    private BeachBoyLinePath() {
    }

    public static double polylineLength(List<Vector3d> points) {
        double length = 0.0D;
        for (int i = 1; i < points.size(); i++) {
            length += points.get(i - 1).distanceTo(points.get(i));
        }
        return length;
    }

    public static List<Vector3d> sagCurve(Vector3d start, Vector3d end, double sag) {
        Vector3d delta = end.subtract(start);
        double span = delta.length();
        if (span < 1.0E-4D) {
            List<Vector3d> points = new ArrayList<Vector3d>(2);
            points.add(start);
            points.add(end);
            return points;
        }
        int samples = MathHelper.clamp((int) Math.max(span * 2.0D, 4.0D), 4, 24);
        List<Vector3d> points = new ArrayList<Vector3d>(samples + 1);
        for (int i = 0; i <= samples; i++) {
            double t = i / (double) samples;
            points.add(new Vector3d(
                    start.x + delta.x * t,
                    start.y + delta.y * t - 4.0D * sag * t * (1.0D - t),
                    start.z + delta.z * t));
        }
        return points;
    }

    public static List<Vector3d> buildPath(World world, List<Vector3d> anchors, double maxLength,
            boolean pathMode, @Nullable Entity tracer) {
        if (anchors == null || anchors.size() < 2) {
            return anchors == null ? Collections.emptyList() : new ArrayList<Vector3d>(anchors);
        }
        if (pathMode) {
            return new ArrayList<Vector3d>(anchors);
        }
        Vector3d start = anchors.get(0);
        Vector3d end = anchors.get(anchors.size() - 1);
        double span = start.distanceTo(end);
        double sag = Math.min(0.12D, span * 0.01D);
        return sagCurve(start, end, sag);
    }
}
