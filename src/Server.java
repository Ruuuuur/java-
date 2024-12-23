import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;

/**
 * 服务器端程序
 * 用于实现多人聊天室功能，负责管理客户端的连接、消息的广播和在线用户列表的维护
 */
public class Server {
    private static final int PORT = 12345; // 服务器监听的端口号
    private static final List<PrintWriter> clientWriters = new ArrayList<>(); // 存储所有客户端的输出流
    private static final List<String> clientUserNames = new ArrayList<>(); // 存储所有客户端的用户名
    private static final Logger logger = Logger.getLogger(Server.class.getName()); // 日志记录器

    // 初始化日志记录器
    static {
        try {
            FileHandler fileHandler = new FileHandler("server.log", true); // 创建日志文件并追加内容
            fileHandler.setFormatter(new SimpleFormatter()); // 设置日志格式
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

            // 不断接受客户端的连接请求
            while (true) {
                new ClientHandler(serverSocket.accept()).start(); // 为每个客户端启动一个新线程
            }
        } catch (IOException e) {
            logger.severe("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 客户端处理线程
     * 每个客户端连接都会分配一个独立的线程来处理通信
     */
    private static class ClientHandler extends Thread {
        private final Socket socket; // 当前客户端的套接字
        private PrintWriter out; // 当前客户端的输出流
        private BufferedReader in; // 当前客户端的输入流
        private String userName; // 当前客户端的用户名

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                // 初始化输入和输出流
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // 将客户端输出流添加到全局列表中，便于消息广播
                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                // 获取客户端发送的用户名
                userName = in.readLine();
                if (userName == null || userName.isEmpty()) { // 如果用户名为空，拒绝连接
                    out.println("用户名不能为空。连接中断。");
                    socket.close();
                    return;
                }

                // 将用户名添加到全局列表
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

                // 持续接收客户端发送的消息并广播
                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(userName + ": " + message); // 广播消息
                }
            } catch (IOException e) {
                logger.warning("连接出现异常: " + e.getMessage());
            } finally {
                // 用户断开连接时清理资源
                cleanupConnection();
            }
        }

        /**
         * 清理连接资源，移除用户并通知其他客户端
         */
        private void cleanupConnection() {
            try {
                socket.close(); // 关闭套接字
            } catch (IOException e) {
                logger.severe("关闭连接时出现异常: " + e.getMessage());
            }

            synchronized (clientWriters) {
                clientWriters.remove(out); // 从输出流列表中移除
            }
            synchronized (clientUserNames) {
                clientUserNames.remove(userName); // 从用户名列表中移除
            }

            // 广播用户离开聊天室的消息并更新在线用户列表
            broadcastMessage(userName + " 离开了聊天室。");
            broadcastUserList();

            logger.info(userName + " 已退出聊天室。");
        }

        /**
         * 向所有客户端广播消息
         * @param message 要广播的消息
         */
        private void broadcastMessage(String message) {
            String formattedMessage = formatMessage(message); // 格式化消息（添加时间戳）
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(formattedMessage); // 向所有客户端发送消息
                }
            }
        }

        /**
         * 广播在线用户列表到所有客户端
         */
        private void broadcastUserList() {
            StringBuilder userList = new StringBuilder("USERS:"); // 用户列表前缀
            synchronized (clientUserNames) {
                for (String user : clientUserNames) {
                    userList.append(user).append(","); // 将用户名拼接成列表
                }
            }
            // 移除最后一个多余的逗号
            if (userList.length() > 6) {
                userList.setLength(userList.length() - 1);
            }
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(userList.toString()); // 向所有客户端发送在线用户列表
                }
            }
        }

        /**
         * 格式化消息，添加时间戳
         * @param message 原始消息
         * @return 格式化后的消息
         */
        private String formatMessage(String message) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return sdf.format(new Date()) + " " + message; // 添加时间戳
        }
    }
}
