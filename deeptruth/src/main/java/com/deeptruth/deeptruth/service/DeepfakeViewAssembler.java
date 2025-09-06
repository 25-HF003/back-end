package com.deeptruth.deeptruth.service;
import com.deeptruth.deeptruth.base.Enum.DeepfakeResult;
import com.deeptruth.deeptruth.base.dto.deepfake.BulletDTO;
import com.deeptruth.deeptruth.base.policy.BulletPolicyRegistry;
import com.deeptruth.deeptruth.entity.DeepfakeDetection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DeepfakeViewAssembler {

    private final BulletPolicyRegistry policy;

    /** 조회용: 안정성 불릿 */
    public List<BulletDTO> makeStabilityBullets(DeepfakeDetection e) {
        boolean isFake = e.getResult() == DeepfakeResult.FAKE;
        Map<String, float[][]> bands = policy.stabilityPolicy(isFake);

        List<BulletDTO> out = new ArrayList<>();
        out.add(build("temporal_delta_mean","Δ Mean (↓)", F(e.getTemporalDeltaMean()), bands.get("temporal_mean"),"lower", null));
        out.add(build("temporal_delta_std", "Δ Std (↓)",  F(e.getTemporalDeltaStd()),  bands.get("temporal_std"), "lower", null));
        out.add(build("tta_std",            "TTA Std (↓)", F(e.getTtaStd()),          bands.get("tta_std"),      "lower", null));

        String label = isFake ? "TTA Mean (FAKE↑)" : "TTA Mean (REAL↓)";
        String dir   = isFake ? "higher" : "lower";
        out.add(build("tta_mean", label, F(e.getTtaMean()), bands.get("tta_mean"), dir, null));
        return out;
    }

    /** 조회용: 속도 불릿 (엔티티에 저장된 SLA로 재계산) */
    public List<BulletDTO> makeSpeedBullets(DeepfakeDetection e) {
        float targetFps = Optional.ofNullable(e.getTargetFpsUsed()).map(Number::floatValue).orElse(1.0f);
        float maxLatMs  = Optional.ofNullable(e.getMaxLatencyMsUsed()).map(Number::floatValue).orElse(2000.0f);

        Map<String, float[][]> bands = policy.speedPolicy(targetFps, maxLatMs);

        List<BulletDTO> out = new ArrayList<>();
        out.add(build("fps_processed", "Throughput (fps ↑)", F(e.getFpsProcessed()), bands.get("fps"), "higher", "fps"));
        out.add(build("ms_per_sample", "Latency (ms/sample ↓)", F(e.getMsPerSample()), bands.get("lat"), "lower", "ms"));
        return out;
    }

    /** 여러 불릿 평균 점수 → 0~100 */
    public static Float meanScore(List<BulletDTO> bullets) {
        List<Float> scores = new ArrayList<>();
        for (BulletDTO b : bullets) {
            Float s = scoreFromBands(b.getValue(), toArr(b.getBands()), b.getDirection());
            if (s != null) scores.add(s);
        }
        if (scores.isEmpty()) return null;
        float sum = 0f;
        for (Float s : scores) sum += s;
        return (sum / scores.size()) * 100.0f;
    }

    /** 밴드 기반 점수 0~1 산출 (float) */
    public static Float scoreFromBands(Float value, float[][] bands, String direction) {
        if (value == null || bands == null) return null;
        float[] good = bands[0], warn = bands[1]; // bad는 0점

        if ("lower".equals(direction)) {
            Float gMax = finite(good[1]);
            Float wMax = finite(warn[1]);
            if (gMax != null && value <= gMax) return 1.0f;
            if (wMax != null && value <= wMax) {
                float gHi = (gMax != null) ? gMax : warn[1];
                float denom = Math.max(1e-9f, (wMax - gHi));
                return (wMax - value) / denom;
            }
            return 0.0f;
        } else { // "higher"
            Float gMin = finite(good[0]);
            Float wMin = finite(warn[0]);
            if (gMin != null && value >= gMin) return 1.0f;
            if (wMin != null && value >= wMin) {
                float denom = Math.max(1e-9f, (gMin - wMin));
                return (value - wMin) / denom;
            }
            return 0.0f;
        }
    }

    private static BulletDTO build(String key, String label, Float value,
                                   float[][] bandArr, String direction, String unit) {
        Map<String, float[]> m = new LinkedHashMap<>();
        if (bandArr != null) {
            m.put("good", bandArr[0]);
            m.put("warn", bandArr[1]);
            m.put("bad",  bandArr[2]);
        }
        BulletDTO dto = new BulletDTO();
        dto.setKey(key);
        dto.setLabel(label);
        dto.setValue(value);
        dto.setBands(m);
        dto.setDirection(direction);
        dto.setUnit(unit);
        return dto;
    }

    private static Float F(Number n) { return n == null ? null : n.floatValue(); }

    private static float[][] toArr(Map<String, float[]> m) {
        if (m == null) return null;
        return new float[][]{ m.get("good"), m.get("warn"), m.get("bad") };
    }

    private static Float finite(float v) {
        return Float.isNaN(v) ? null : v;
    }
}