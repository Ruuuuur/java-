import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class Client {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;

    private static String userName = "";
    private static JTextArea textArea;
    private static JTextField textField;
    private static JButton sendButton;
    private static JList<String> userList;
    private static DefaultListModel<String> userListModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::showLoginDialog);
    }

    private static void showLoginDialog() {
        JDialog loginDialog = new JDialog();
        loginDialog.setTitle("登录");
        loginDialog.setSize(300, 150);
        loginDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        loginDialog.setLocationRelativeTo(null);

        JLabel nameLabel = new JLabel("请输入用户名：");
        JTextField nameField = new JTextField(15);
        JButton loginButton = new JButton("登录");

        JPanel panel = new JPanel();
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(loginButton);
        loginDialog.add(panel);
        loginDialog.setVisible(true);

        loginButton.addActionListener(e -> {
            userName = nameField.getText();
            if (!userName.isEmpty()) {
                loginDialog.setVisible(false);
                startChat();
            } else {
                JOptionPane.showMessageDialog(loginDialog, "用户名不能为空！");
            }
        });
    }

    private static void startChat() {
        JFrame chatFrame = new JFrame("聊天室 - 当前用户名：" + userName);
        chatFrame.setSize(600, 400);
        chatFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chatFrame.setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane textScrollPane = new JScrollPane(textArea);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListPane = new JScrollPane(userList);
        userListPane.setBorder(BorderFactory.createTitledBorder("在线用户"));

        // 使用 JSplitPane 分割聊天区域和在线用户区域
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScrollPane, userListPane);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.75);

        JPanel inputPanel = new JPanel();
        textField = new JTextField(30);
        sendButton = new JButton("发送");
        inputPanel.add(textField);
        inputPanel.add(sendButton);

        chatFrame.add(splitPane, BorderLayout.CENTER);
        chatFrame.add(inputPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> {
            String message = textField.getText();
            if (!message.isEmpty()) {
                sendMessage(message);
                textField.setText("");
            }
        });

        chatFrame.setVisible(true);
        connectToServer();
    }

    private static void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(userName);
            new Thread(new ReceiveMessageTask()).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(String message) {
        out.println(message);
        textArea.append("我(" + userName + "): " + message + "\n");
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    private static class ReceiveMessageTask implements Runnable {
        @Override
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("USERS:")) {
                        updateUserList(message.substring(6));
                    } else {
                        textArea.append(message + "\n");
                        textArea.setCaretPosition(textArea.getDocument().getLength());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateUserList(String users) {
        String[] userArray = users.split(",");
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            for (String user : userArray) {
                userListModel.addElement(user);
            }
        });
    }
}
