package com.algotrader.dto;


public class BaseErrorDetailsDto {

    private String name;
    private String reason;
    
    
    
	public BaseErrorDetailsDto(String name, String reason) {
		this.name = name;
		this.reason = reason;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
}
