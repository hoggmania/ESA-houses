package com.example.dashboard.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof ValidationException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Validation Error", exception.getMessage()))
                    .build();
        }
        
        if (exception instanceof JsonMappingException || exception instanceof JsonProcessingException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("JSON Parsing Error", "Invalid JSON format: " + exception.getMessage()))
                    .build();
        }
        
        if (exception instanceof IllegalArgumentException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid Argument", exception.getMessage()))
                    .build();
        }
        
        // Generic server error for unexpected exceptions
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Internal Server Error", "An unexpected error occurred"))
                .build();
    }
    
    public static class ErrorResponse {
        public String error;
        public String message;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }
}
