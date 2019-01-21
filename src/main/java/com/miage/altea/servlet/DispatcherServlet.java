package com.miage.altea.servlet;

import annotation.Controller;
import annotation.RequestMapping;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miage.altea.controller.HelloController;
import com.miage.altea.controller.PokemonTypeController;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = "/*", loadOnStartup = 1)
public class DispatcherServlet extends HttpServlet {

    private Map<String, Method> uriMappings = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        System.out.println("Getting request for " + req.getRequestURI());
        ObjectMapper mapper = new ObjectMapper();
        if (this.uriMappings.get(req.getRequestURI()) != null) {
            Method m = this.uriMappings.get(req.getRequestURI());
            Object responseToClient;
            try {
                if(m.getParameterCount() != 0) {
                    responseToClient = m.invoke(m.getDeclaringClass().getDeclaredConstructor().newInstance(),
                            req.getParameterMap());
                }else{
                    responseToClient = m.invoke(m.getDeclaringClass().getDeclaredConstructor().newInstance());
                }
                try {
                    resp.getWriter().print(mapper.writeValueAsString(responseToClient));
                    resp.getWriter().flush();
                    resp.getWriter().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                try {
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "exception when calling method someThrowingMethod : some exception message");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }


        } else {
            try {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "no mapping found for request uri /test");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        // on enregistre notre controller au démarrage de la servlet
        this.registerController(HelloController.class);
        this.registerController(PokemonTypeController.class);
    }

    protected void registerController(Class controllerClass) {
        System.out.println("Analysing class " + controllerClass.getName());
        if (controllerClass.getAnnotation(Controller.class) != null) {
            for (Method m : controllerClass.getMethods()) {
                registerMethod(m);
            }
        } else {
            throw new IllegalArgumentException();
        }
    }

    protected void registerMethod(Method method) {
        System.out.println("Registering method " + method.getName());
        RequestMapping r = method.getAnnotation(RequestMapping.class);
        if (r != null && method.getReturnType() != void.class) {
            this.uriMappings.put(r.uri(), method);
        }
    }

    protected Map<String, Method> getMappings() {
        return this.uriMappings;
    }

    protected Method getMappingForUri(String uri) {
        return this.uriMappings.get(uri);
    }
}
