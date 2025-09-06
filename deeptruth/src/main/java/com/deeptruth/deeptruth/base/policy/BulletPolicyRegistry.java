package com.deeptruth.deeptruth.base.policy;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class BulletPolicyRegistry {
    /** 안정성 밴드 정의 (good/warn/bad 각 [min,max], max==NaN이면 상한 없음) */
    public Map<String, float[][]> stabilityPolicy(boolean isFake) {
        Map<String, float[][]> m = new HashMap<>();

        m.put("temporal_mean", bands(0.0f, 0.03f, 0.03f, 0.06f, 0.06f, Float.NaN));
        m.put("temporal_std",  bands(0.0f, 0.02f, 0.02f, 0.05f, 0.05f, Float.NaN));
        m.put("tta_std",       bands(0.0f, 0.03f, 0.03f, 0.05f, 0.05f, Float.NaN));

        if (isFake) {
            m.put("tta_mean", bands(0.6f, Float.NaN, 0.4f, 0.6f, 0.0f, 0.4f));
        } else {
            m.put("tta_mean", bands(0.0f, 0.4f, 0.4f, 0.6f, 0.6f, Float.NaN));
        }
        return m;
    }

    /** 속도 밴드 정의 (SLA 반영) */
    public Map<String, float[][]> speedPolicy(float targetFps, float maxLatencyMs) {
        Map<String, float[][]> m = new HashMap<>();
        m.put("fps", bands(targetFps, Float.NaN, 0.5f * targetFps, targetFps, 0.0f, 0.5f * targetFps));
        m.put("lat", bands(0.0f, maxLatencyMs, maxLatencyMs, 2.0f * maxLatencyMs, 2.0f * maxLatencyMs, Float.NaN));
        return m;
    }

    /** helper: good/warn/bad 3구간 */
    private static float[][] bands(float gMin, float gMax, float wMin, float wMax, float bMin, float bMax) {
        return new float[][]{
                new float[]{gMin, norm(gMax)},
                new float[]{wMin, norm(wMax)},
                new float[]{bMin, norm(bMax)}
        };
    }

    private static float norm(float v) {
        return Float.isNaN(v) ? Float.NaN : v;
    }
}