import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

public class Server {
    private static final int PORT = 12345;
    private static final List<PrintWriter> clientWriters = new ArrayList<>();
    private static final List<String> clientUserNames = new ArrayList<>(); // 存储所有客户端的用户名
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    static {
        try {
            // 设置日志记录器
            FileHandler fileHandler = new FileHandler("server.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("聊天室服务器启动中...");
        logger.info("聊天室服务器启动中...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("服务器已启动，正在监听端口 " + PORT + "...");
            logger.info("服务器已启动，正在监听端口 " + PORT + "...");

            while (true) {
                // 接受客户端的连接请求并启动新线程处理
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            logger.severe("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String userName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // 初始化输入和输出流
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // 获取用户名
                userName = in.readLine();
                if (userName == null || userName.isEmpty()) {
                    out.println("用户名不能为空。连接中断。");
                    socket.close();
                    return;
                }
                synchronized (clientUserNames) {
                    clientUserNames.add(userName);
                }

                // 通知客户端用户名设置成功
                out.println("用户名 " + userName + " 设置成功，您可以开始聊天了");

                // 广播新用户加入的消息
                broadcastMessage(userName + " 加入了聊天室！");
                logger.info(userName + " 已连接到聊天室。");

                // 广播最新的在线用户列表
                broadcastUserList();

                // 接收并广播消息
                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(userName + ": " + message);
                }
            } catch (IOException e) {
                logger.warning("连接出现异常: " + e.getMessage());
            } finally {
                // 用户离开，清理资源
                cleanupConnection();
            }
        }

        private void cleanupConnection() {
            try {
                socket.close();
            } catch (IOException e) {
                logger.severe("关闭连接时出现异常: " + e.getMessage());
            }

            synchronized (clientWriters) {
                clientWriters.remove(out);
            }
            synchronized (clientUserNames) {
                clientUserNames.remove(userName);
            }

            // 广播用户离开消息并更新在线用户列表
            broadcastMessage(userName + " 离开了聊天室。");
            broadcastUserList();

            logger.info(userName + " 已退出聊天室。");
        }

        private void broadcastMessage(String message) {
            String formattedMessage = formatMessage(message);
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(formattedMessage);
                }
            }
        }

        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("USERS:");
            synchronized (clientUserNames) {
                for (String user : clientUserNames) {
                    userList.append(user).append(",");
                }
            }
            // 移除最后一个多余的逗号
            if (userList.length() > 6) {
                userList.setLength(userList.length() - 1);
            }
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(userList.toString());
                }
            }
        }


        private String formatMessage(String message) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date()) + " " + message;
        }
    }
}
