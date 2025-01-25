package com.algotrader.dto;


import java.util.List;

public class BaseErrorResponseDto {

	
	
    
	private String type;
    private String title;
    private List<BaseErrorDetailsDto> details;
	
    
    public BaseErrorResponseDto(String type, String title, List<BaseErrorDetailsDto> details) {
		this.type = type;
		this.title = title;
		this.details = details;
	}
    
    
    public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public List<BaseErrorDetailsDto> getDetails() {
		return details;
	}
	public void setDetails(List<BaseErrorDetailsDto> details) {
		this.details = details;
	}
}
