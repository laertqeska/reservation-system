package com.example.inventory.web;

import com.example.inventory.exceptions.InsufficientInventoryException;
import com.example.inventory.exceptions.InventoryItemAlreadyExistsException;
import com.example.inventory.exceptions.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,ex.getMessage());
        problem.setTitle("Resource not found");
        problem.setProperty("code","NOT_FOUND");
        return problem;
    }


    @ExceptionHandler(InventoryItemAlreadyExistsException.class)
    public ProblemDetail handleDuplicateSku(InventoryItemAlreadyExistsException ex){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Duplicate sku");
        problem.setProperty("code","SKU_ALREADY_EXISTS");

        return problem;
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ProblemDetail handleInsufficientInventory(InsufficientInventoryException ex){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );
        problem.setTitle("Insufficient inventory");
        problem.setProperty("code","INSUFFICIENT_INVENTORY");

        return problem;
    }
}
