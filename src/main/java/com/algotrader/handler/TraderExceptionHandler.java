package com.algotrader.handler;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.algotrader.dto.BaseErrorDetailsDto;
import com.algotrader.dto.BaseErrorResponseDto;
import com.algotrader.exception.ApiException;
import com.algotrader.exception.CacheException;
import com.algotrader.exception.NoCredentialsException;

@ControllerAdvice
public class TraderExceptionHandler {

	@ExceptionHandler(NoCredentialsException.class)
	protected ResponseEntity<BaseErrorResponseDto> handleNoCredentialsException(NoCredentialsException e) {
	    var details = new BaseErrorDetailsDto("reason", "no-credentials");
	    var responseDto = buildBaseErrorResponseDto("VALIDATION_ERROR", "title", List.of(details));
	    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
	            .contentType(MediaType.APPLICATION_JSON)
	            .body(responseDto);
	}

	@ExceptionHandler(ApiException.class)
	protected ResponseEntity<BaseErrorResponseDto> handleInvalidCredentials(ApiException e) {
	    var details = new BaseErrorDetailsDto("reason", "bad-credentials");
	    var responseDto = buildBaseErrorResponseDto("VALIDATION_ERROR", "title", List.of(details));
	    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	            .contentType(MediaType.APPLICATION_JSON)
	            .body(responseDto);
	}

	@ExceptionHandler(CacheException.class)
	protected ResponseEntity<BaseErrorResponseDto> handleCacheException(CacheException e) {
	    var details = new BaseErrorDetailsDto("reason", "cache-exception");
	    var responseDto = buildBaseErrorResponseDto("CACHE_ERROR", "title", List.of(details));
	    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
	            .contentType(MediaType.APPLICATION_JSON)
	            .body(responseDto);
	}

	private BaseErrorResponseDto buildBaseErrorResponseDto(String type,
	                                                       String title,
	                                                       List<BaseErrorDetailsDto> details) {
	    return new BaseErrorResponseDto(type, title, details);
	}

}
