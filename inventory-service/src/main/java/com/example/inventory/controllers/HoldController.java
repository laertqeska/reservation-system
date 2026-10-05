package com.example.inventory.controllers;

import com.example.inventory.dto.CreateHoldRequest;
import com.example.inventory.dto.HoldResponse;
import com.example.inventory.services.HoldService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/holds")
public class HoldController {
    private final HoldService holdService;

    public HoldController(HoldService holdService) {
        this.holdService = holdService;
    }

    @PostMapping
    public ResponseEntity<HoldResponse> createHold(@RequestBody @Valid CreateHoldRequest request){
        HoldResponse response = holdService.createHold(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/{id}/commit")
    public ResponseEntity<HoldResponse> commitHold(@PathVariable Long id){
        HoldResponse response = holdService.commitHold(id);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<HoldResponse> releaseHold(@PathVariable Long id){
        HoldResponse response = holdService.releaseHold(id);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HoldResponse> getHold(@PathVariable Long id){
        HoldResponse response = holdService.getHold(id);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }
}
