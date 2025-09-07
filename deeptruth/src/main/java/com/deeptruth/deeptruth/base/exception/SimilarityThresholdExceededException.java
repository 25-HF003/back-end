package com.deeptruth.deeptruth.base.exception;

public class SimilarityThresholdExceededException extends RuntimeException {
    public SimilarityThresholdExceededException(int dist, int th) {
        super("유사도가 임계값을 초과했습니다. dist=" + dist + ", threshold=" + th);
    }
}