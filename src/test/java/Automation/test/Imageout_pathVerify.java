package Automation.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.testng.Assert;
import org.testng.annotations.Test;

public class Imageout_pathVerify {

   public static void main(String[] args) {
        Map<String, String> webSockets = new HashMap<>();
        webSockets.put("dev2mani.humanbrain.in WebSocket", "wss://dev2mani.humanbrain.in/aiAgentServer/ws/ai_agent");
        webSockets.put("apollo2.humanbrain.in WebSocket", "wss://apollo2.humanbrain.in/aiAgentServer/ws/ai_agent");

        for (Map.Entry<String, String> entry : webSockets.entrySet()) {
            String serverName = entry.getKey();
            String webSocketUrl = entry.getValue();

            try {
                testWebSocketConnection(serverName, webSocketUrl);
            } catch (Exception e) {
                System.out.println("❌ Exception while checking " + serverName + ": " + e.getMessage());
                sendAlertMail(serverName, e.getMessage(), "222 1000", "Divya D", 193, "Neurovoyager");
            }
        }
    }

    private static void testWebSocketConnection(String serverName, String webSocketUrl) throws InterruptedException {
        System.out.println("🔍 Connecting to " + serverName + " → " + webSocketUrl);

        CountDownLatch latch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(URI.create(webSocketUrl)) {

            private StringBuilder responseBuffer = new StringBuilder();
            private ScheduledExecutorService scheduler;
            private ScheduledFuture<?> timeoutFuture;

            @Override
            public void onOpen(ServerHandshake handshake) {
            	  String[] pagesToTest = {"Atlas Editor", "SomeOtherPage", "Neurovoyager"};
                String testMessage = "{"
                        + "\"query\": \"222 1000\","
                        + "\"user\": \"Divya D\","
                        + "\"userId\": 193,"
                        + "\"page\": \"Neurovoyager\","
                        + "\"page_context\": {}"
                        + "}";
                send(testMessage);
                System.out.println("📤 Sent: " + testMessage);

                scheduler = Executors.newSingleThreadScheduledExecutor();
                timeoutFuture = scheduler.schedule(() -> {
                    logFailureMessage(serverName, "Timeout (15 seconds)");
                    latch.countDown();
                    close();
                }, 15, TimeUnit.SECONDS);
            }

            @Override
            public void onMessage(String message) {
                System.out.println("📥 Received: " + message);
                responseBuffer.append(message);

                if (message.contains("###END")) {
                    if (timeoutFuture != null) timeoutFuture.cancel(true);
                    System.out.println("✅ Full AI Agent response received from " + serverName + ":");
                    System.out.println(responseBuffer.toString());
                    latch.countDown();
                    close();
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("❌ WebSocket Closed for " + serverName + ". Code: " + code + ", Reason: " + reason);
                if (timeoutFuture != null) timeoutFuture.cancel(true);
                if (scheduler != null) scheduler.shutdownNow();
                latch.countDown();
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("❌ Error in " + serverName + " WebSocket: " + ex.getMessage());
                logFailureMessage(serverName, ex.getMessage());
                if (timeoutFuture != null) timeoutFuture.cancel(true);
                if (scheduler != null) scheduler.shutdownNow();
                latch.countDown();
            }
        };

        try {
            client.connectBlocking();
        } catch (Exception e) {
            System.out.println("❌ Could not connect to " + serverName + " WebSocket: " + e.getMessage());
            sendAlertMail(serverName, e.getMessage(), "222 1000", "Divya D", 193, "Neurovoyager");
            return;
        }

        latch.await(); // Wait until message received or timeout
    }

    private static void logFailureMessage(String serverName, String reason) {
        String message = "❌ " + serverName + " WebSocket health check failed. Reason: " + reason;
        System.out.println(message);
        sendAlertMail(serverName, reason, "222 1000", "Divya D", 193, "Neurovoyager");
    }

    private static void sendAlertMail(String serverName, String reason, String query, String user, int userId, String page) {
        String[] to = {"sriramv@htic.iitm.ac.in"};
        String[] cc = {"venip@htic.iitm.ac.in", "divya.d@htic.iitm.ac.in", "gayathri@htic.iitm.ac.in"};
        String from = "gayathri@htic.iitm.ac.in";
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("automationsoftware25@gmail.com", "wjzcgaramsqvagxu");
            }
        });

        session.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));

            for (String recipient : to) {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            for (String ccRecipient : cc) {
                message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccRecipient));
            }

            message.setSubject("AI Agent - WebSocket Connection Issue Alert: " + serverName);

            String currentTime = java.time.LocalDateTime.now().toString();

            String content = "<div style='font-family: Arial, sans-serif; font-size: 14px; color: #333;'>"
                + "<h3 style='color: #D9534F;'>🚨 AI Agent WebSocket Connection Failure Alert</h3>"
                + "<p>Hi Team,</p>"
                + "<p><strong>WebSocket connection to <span style='color:#5bc0de;'>" + serverName + "</span> failed at <strong>" + currentTime + "</strong>.</strong></p>"
                + "<p><u><strong>Error Details:</strong></u></p>"
                + "<ul>"
                + "<li><strong>Page:</strong> " + page + "</li>"
                + "<li><strong>User:</strong> " + user + " (ID: " + userId + ")</li>"
                + "<li><strong>Query:</strong> " + query + "</li>"
                + "<li><strong>Reason:</strong> " + reason + "</li>"
                + "</ul>"
                + "<p><u><strong>Action:</strong></u></p>"
                + "<p>Please check WebSocket server status, nginx proxy configuration, and SSL certificate.</p>"
                + "<br><p style='color: #555;'>Regards,<br><b>Automated Monitoring</b></p>"
                + "</div>";

            message.setContent(content, "text/html");

            System.out.println("Sending reachability alert email...");
            Transport.send(message);
            System.out.println("Email sent successfully.");

        } catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}
