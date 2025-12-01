package com.example.dashboard.resource;

import java.net.URI;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/")
@RequestScoped
public class RootResource {

    @GET
    public Response root() {
        return Response.seeOther(URI.create("/ui")).build();
    }

    @GET
    @Path("favicon.ico")
    public Response favicon() {
        return Response.seeOther(URI.create("/favicon.svg")).build();
    }
}
