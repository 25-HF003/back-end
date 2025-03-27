package com.deeptruth.deeptruth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Hello API", description = "테스트용 API입니다.")
public class HelloController {

    @Operation(summary = "Hello 메시지 반환", description = "입력한 이름과 함께 Hello 메시지를 반환합니다.")
    @GetMapping("/hello")
    public String hello(@RequestParam String name) {
        return "Hello, " + name + "!";
    }
}
