package org.logx.compatibility.jsp.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 测试Servlet，用于生成各种级别的日志消息
 */
@WebServlet("/test-log")
public class TestLogServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(TestLogServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // 生成不同级别的日志消息
        logger.trace("这是一条TRACE级别的日志消息");
        logger.debug("这是一条DEBUG级别的日志消息");
        logger.info("这是一条INFO级别的日志消息");
        logger.warn("这是一条WARN级别的日志消息");
        logger.error("这是一条ERROR级别的日志消息");

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<html><body>");
        out.println("<h1>日志消息已生成</h1>");
        out.println("</body></html>");
    }
}