package com.example.reservation_service.web;

import com.example.reservation_service.exceptions.InventoryUnavailableException;
import com.example.reservation_service.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(InventoryUnavailableException.class)
    public ProblemDetail handleInventoryUnavailable(InventoryUnavailableException ex){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,ex.getMessage());
        problem.setTitle("Inventory service unavailable");
        problem.setProperty("code","INVENTORY_UNAVAILABLE");
        return problem;
    }


    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFoundException(NotFoundException ex){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setProperty("code","NOT_FOUND");
        return problem;
    }
}
