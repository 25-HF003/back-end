package com.deeptruth.deeptruth.service;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class ActiveTaskService {

    // 동시성 문제를 피하기 위해 ConcurrentHashMap 사용
    // Key: loginId, Value: taskId
    private final Map<String, String> activeTasks = new ConcurrentHashMap<>();


    public void registerTask(String loginId, String taskId) {
        activeTasks.put(loginId, taskId);
    }

    public void deregisterTask(String loginId) {
        String taskId = activeTasks.remove(loginId);
    }

    public String getActiveTask(String loginId) {
        return activeTasks.get(loginId);
    }
}
