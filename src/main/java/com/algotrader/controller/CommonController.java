package com.algotrader.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.algotrader.service.PanicStrategyService;

@RestController
public class CommonController {

	@Autowired
    private  PanicStrategyService service;

    @RequestMapping(method = RequestMethod.POST, value = "/exit")
    public ResponseEntity<String> exit() {
        service.initiateAppShutDown();
        return ResponseEntity.ok("EXIT");
    }
}
