package com.selimhorri.app.business.dashboard.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(value = {"/api/beta/dashboard"})
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "features.beta-dashboard", havingValue = "true")
public class BetaDashboardController {
	
	@GetMapping
	public ResponseEntity<String> betaDashboard() {
		log.info("**** Beta Dashboard Feature ****");
		return ResponseEntity.ok("Beta dashboard with real-time analytics and interactive visualizations.");
	}
	
	@GetMapping("/metrics")
	public ResponseEntity<String> advancedMetrics() {
		log.info("**** Advanced Metrics Feature ****");
		return ResponseEntity.ok("Advanced metrics dashboard with custom KPIs and business intelligence.");
	}
	
}
