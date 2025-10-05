package com.deeptruth.deeptruth.base.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskEvent<T> {
    private String type;
    private String taskId;
    private Integer progress;
    private T payload;

    public static TaskEvent<Void> progress(String id, int p) { return TaskEvent.<Void>builder().type("PROGRESS").taskId(id).progress(p).build(); }
    public static <T> TaskEvent<T> done(String id, T payload) { return TaskEvent.<T>builder().type("DONE").taskId(id).progress(100).payload(payload).build(); }
    public static TaskEvent<String> error(String id, String code) { return TaskEvent.<String>builder().type("ERROR").taskId(id).payload(code).build(); }
}