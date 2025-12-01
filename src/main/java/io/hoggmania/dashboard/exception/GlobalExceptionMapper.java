package io.hoggmania.dashboard.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Context;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.logging.Log;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    UriInfo uriInfo;

    @Context
    HttpHeaders headers;

    @Override
    public Response toResponse(Exception exception) {
        // Log the full exception for diagnostics during tests
        Log.error("Unhandled exception in request processing", exception);
        boolean wantsHtml = false;
        String path = uriInfo != null && uriInfo.getPath() != null ? uriInfo.getPath() : "";
        if (path.startsWith("ui") || path.startsWith("/ui")) {
            wantsHtml = true;
        }
        if (headers != null && headers.getAcceptableMediaTypes() != null) {
            for (var mt : headers.getAcceptableMediaTypes()) {
                if (MediaType.TEXT_HTML_TYPE.isCompatible(mt)) {
                    wantsHtml = true;
                    break;
                }
            }
        }

        Response.ResponseBuilder builder;
        String title;
        String msg;
        if (exception instanceof ValidationException) {
            builder = Response.status(Response.Status.BAD_REQUEST);
            title = "Validation Error";
            msg = exception.getMessage();
            return wantsHtml ? builder.entity(buildHtml(title, msg)).type(MediaType.TEXT_HTML).build()
                    : builder.entity(new ErrorResponse(title, msg)).build();
        }
        
        if (exception instanceof JsonMappingException || exception instanceof JsonProcessingException) {
            builder = Response.status(Response.Status.BAD_REQUEST);
            title = "JSON Parsing Error";
            msg = "Invalid JSON format: " + exception.getMessage();
            return wantsHtml ? builder.entity(buildHtml(title, msg)).type(MediaType.TEXT_HTML).build()
                    : builder.entity(new ErrorResponse(title, msg)).build();
        }
        
        if (exception instanceof IllegalArgumentException) {
            builder = Response.status(Response.Status.BAD_REQUEST);
            title = "Invalid Argument";
            msg = exception.getMessage();
            return wantsHtml ? builder.entity(buildHtml(title, msg)).type(MediaType.TEXT_HTML).build()
                    : builder.entity(new ErrorResponse(title, msg)).build();
        }
        
        // Generic server error for unexpected exceptions
        builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR);
        title = "Internal Server Error";
        msg = "An unexpected error occurred";
        return wantsHtml ? builder.entity(buildHtml(title, msg)).type(MediaType.TEXT_HTML).build()
                : builder.entity(new ErrorResponse(title, msg)).build();
    }
    
    public static class ErrorResponse {
        public String error;
        public String message;
        
        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }
    }

    private String buildHtml(String title, String message) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head><meta charset=\"UTF-8\"><title>" + escape(title) + "</title>" +
                "<style>body{font-family:system-ui,Segoe UI,Roboto,Arial,sans-serif;margin:24px;color:#222}" +
                ".msg{padding:12px 14px;border-radius:6px;margin-bottom:16px;background:#fee;border:1px solid #e99;color:#900;white-space:pre-wrap}</style>" +
                "</head>\n" +
                "<body>\n" +
                "<h1>" + escape(title) + "</h1>\n" +
                "<div class=\"msg\">" + escape(message) + "</div>\n" +
                "<a href=\"/ui\">Back</a>\n" +
                "</body></html>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
