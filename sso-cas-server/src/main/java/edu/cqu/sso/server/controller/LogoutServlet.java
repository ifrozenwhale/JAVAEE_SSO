package edu.cqu.sso.server.controller;

import edu.cqu.sso.server.model.TicketGrangtingTicket;
import edu.cqu.sso.server.storage.JVMCache;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;

import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.Map;

public class LogoutServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        HttpSession session = request.getSession();
        String logoutUrl = request.getParameter("service");
        // 主动退出
        // 向认证中心发送注销请求
        System.out.println("------------------------");
        System.out.println("server start to logout");
        System.out.println(request.getRequestURL());
        String logoutInterfaceUrl = "/logout";
        String TGTId = "";
        Cookie[] cookies = request.getCookies();
        if (null != cookies) {
            for (Cookie cookie : cookies) {
                System.out.println(cookie.getName());
                if ("CAS-TGC".equals(cookie.getName())) {
                    TGTId = cookie.getValue();

                    break;
                }
            }
        }
        System.out.println("server get TGC: " + TGTId);
        TicketGrangtingTicket TGT = JVMCache.TGT_CACHE.get(TGTId);
        for(Map.Entry<String, String> entry : TGT.serviceMap.entrySet()){
            String ST = entry.getKey();
            String service = entry.getValue();
            System.out.println("server request ST: " + ST);
            // 通知service logout
            PostMethod postMethod = new PostMethod(service + logoutInterfaceUrl);
            System.out.println("server post logout to: " + service + logoutInterfaceUrl);
            postMethod.addParameter("CAS-ST", ST);
            HttpClient httpClient = new HttpClient();
            try {
                httpClient.executeMethod(postMethod);
                postMethod.releaseConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 注销后重定向


        // 注销本地会话
        TicketGrangtingTicket removedTGT = JVMCache.TGT_CACHE.remove(TGTId);
        if (removedTGT != null) {
            session.removeAttribute("CAS-TGC");
        }
        response.sendRedirect(logoutUrl);

    }
}
