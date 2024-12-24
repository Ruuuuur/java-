import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 客户端程序
 * 用于连接服务器，发送和接收消息，并显示聊天内容和在线用户列表
 */
public class Client {
    // 服务器地址和端口号
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    // 套接字及流，用于与服务器通信
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    // 用户名和聊天界面组件
    private static String userName = "";
    private static JTextArea textArea; // 显示聊天内容的文本区域
    private static JTextField textField; // 输入消息的文本框
    private static JButton sendButton; // 发送消息的按钮
    private static JList<String> userList; // 在线用户列表
    private static DefaultListModel<String> userListModel; // 用户列表的数据模型

    public static void main(String[] args) {
        // 启动登录界面
        SwingUtilities.invokeLater(Client::showLoginDialog);
    }

    /**
     * 显示登录对话框，用户输入用户名
     */
    private static void showLoginDialog() {
        JDialog loginDialog = new JDialog(); // 创建对话框
        loginDialog.setTitle("登录");
        loginDialog.setSize(300, 150);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setLocationRelativeTo(null);

        // 登录对话框组件
        JLabel nameLabel = new JLabel("请输入用户名：");
        JTextField nameField = new JTextField(15);
        JButton loginButton = new JButton("登录");

        // 将组件添加到面板
        JPanel panel = new JPanel();
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(loginButton);
        loginDialog.add(panel);

        loginDialog.setVisible(true);

        // 登录按钮点击事件
        loginButton.addActionListener(e -> {
            userName = nameField.getText(); // 获取输入的用户名
            if (!userName.isEmpty()) {
                loginDialog.setVisible(false); // 隐藏登录对话框
                startChat(); // 启动聊天界面
            } else {
                JOptionPane.showMessageDialog(loginDialog, "用户名不能为空！");
            }
        });
    }

    /**
     * 启动聊天界面
     */
    private static void startChat() {
        JFrame chatFrame = new JFrame("欢迎使用聊天室应用 - 当前用户名：" + userName); // 主聊天窗口
        chatFrame.setSize(600, 400);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setLocationRelativeTo(null);

        // 聊天内容显示区域
        textArea = new JTextArea();
        textArea.setEditable(false); // 禁止用户编辑
        textArea.setLineWrap(true); // 自动换行
        JScrollPane textScrollPane = new JScrollPane(textArea); // 添加滚动条

        // 在线用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListPane = new JScrollPane(userList);
        userListPane.setBorder(BorderFactory.createTitledBorder("在线用户"));

        // 聊天内容和在线用户分割布局
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, userListPane);
        splitPane.setDividerLocation(400); // 设置分隔线初始位置
        splitPane.setResizeWeight(0.75); // 设置左侧区域占初始宽度的比例

        // 消息输入和发送区域
        JPanel inputPanel = new JPanel();
        textField = new JTextField(30); // 文本输入框
        sendButton = new JButton("发送"); // 发送按钮
        inputPanel.add(textField);
        inputPanel.add(sendButton);

        // 将组件添加到主窗口
        chatFrame.add(splitPane, BorderLayout.CENTER);
        chatFrame.add(inputPanel, BorderLayout.SOUTH);

        // 发送按钮点击事件
        sendButton.addActionListener(e -> {
            String message = textField.getText(); // 获取用户输入
            if (!message.isEmpty()) {
                sendMessage(message); // 发送消息到服务器
                textField.setText(""); // 清空输入框
            }
        });

        chatFrame.setVisible(true);

        // 连接服务器
        connectToServer();
    }

    /**
     * 连接到服务器并初始化通信流
     */
    private static void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT); // 连接到服务器
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // 输入流
            out = new PrintWriter(socket.getOutputStream(), true); // 输出流

            out.println(userName); // 将用户名发送到服务器
            new Thread(new ReceiveMessageTask()).start(); // 启动线程处理接收消息
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息到服务器
     */
    private static void sendMessage(String message) {
        out.println(message); // 将消息发送到服务器
        textArea.append(formatMessage("我(" + userName + "): " + message + "\n")); // 显示到聊天区域
        textArea.setCaretPosition(textArea.getDocument().getLength()); // 滚动到最新消息
    }

    /**
     * 接收服务器消息的任务
     */
    private static class ReceiveMessageTask implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("USERS:")) { // 如果消息是用户列表更新
                        updateUserList(message.substring(6)); // 更新用户列表
                    } else { // 普通消息

                        //提取时间戳后面的信息
                        String Tem = message.substring(19);
                        //提取当前发送消息的用户名
                        // 找到冒号的位置
                        int colonIndex = Tem.indexOf(":");
                        // 检查冒号是否存在
                        if (colonIndex != -1) {
                            // 提取冒号前的字符串,也就是用户名
                            String user_now = Tem.substring(1, colonIndex);
                            if(user_now.equals(userName)){
                                continue;
                            }
                        }
                        textArea.append(message + "\n");
                        textArea.setCaretPosition(textArea.getDocument().getLength()); // 滚动到最新消息
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 格式化消息，添加时间戳
     * @param message 原始消息
     * @return 格式化后的消息
     */
    private static String formatMessage(String message) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date()) + " " + message; // 添加时间戳
    }

    /**
     * 更新在线用户列表
     */
    private static void updateUserList(String users) {
        String[] userArray = users.split(","); // 解析用户列表
        SwingUtilities.invokeLater(() -> {
            userListModel.clear(); // 清空当前列表
            for (String user : userArray) {
                userListModel.addElement(user); // 添加用户到列表
            }
        });
    }
}
