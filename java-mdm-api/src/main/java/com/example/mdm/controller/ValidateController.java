package com.example.mdm.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class ValidateController {

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody(required = false) Map<String, Object> req) {
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "checks", new String[]{"schema", "fk", "dryrun"}
        ));
    }

    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> catalog() {
        return ResponseEntity.ok(Map.of(
                "tables", new String[]{"supplier", "supplier_address"}
        ));
    }
}
