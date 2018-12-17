package com.dddog;

import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.json.JSONObject;

import com.dddog.util.DDLog;
import com.dddog.util.JsonUtil;

public class USBSocketClient implements ActionListener {
	private Frame mFrame;
	private static USBSocketClient mClient;
	Button mConnBtn;
	Button mSendBtn;
	Button mClearBtn;
	Button mForwardBtn;
	TextField mCmdTF;
	TextField mLocalPortTF;
	TextField mRemotePortTF;
	static JTextArea mInfo;
	Label mStatus;
	Socket mSocket;
	private static boolean mConnectStatus = false;
	ListenServerThread mListenServerThread;

	public static void main(String[] args) throws IOException {

		mClient = new USBSocketClient();
		mClient.createFrame();
	}

	private void createFrame() {
		mFrame = new Frame("USBSocketClient");
		mFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				System.exit(0);
			}
		});
		// get screen size
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) screensize.getHeight();
		int screenWidth = (int) screensize.getWidth();
		// set frame layout center in screen
		mFrame.setBounds(new Rectangle(screenWidth / 2 - Constants.FRAME_WIDHT / 2,
				screenHeight / 2 - Constants.FRAME_HEIGHT / 2, Constants.FRAME_WIDHT, Constants.FRAME_HEIGHT));

		mFrame.add(createPanel());
		mFrame.setVisible(true);
	}

	private Panel createPanel() {
		Panel panel = new Panel();
		panel.setBackground(Color.LIGHT_GRAY);
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints gbc = new GridBagConstraints();
		panel.setLayout(gbl);

		mForwardBtn = new Button("ADB转发端口");
		mForwardBtn.setFont(Constants.DEFAULT_FONT);
		mConnBtn = new Button("打开连接");
		mConnBtn.setFont(Constants.DEFAULT_FONT);
		mSendBtn = new Button("发送");
		mSendBtn.setFont(Constants.DEFAULT_FONT);
		Label localPortLabel = new Label("本地端口：",Label.CENTER );
		localPortLabel.setFont(Constants.DEFAULT_FONT);
		Label remotePortLabel = new Label("服务器端口：",Label.CENTER);
		remotePortLabel.setFont(Constants.DEFAULT_FONT);
		mLocalPortTF = new TextField();
		mLocalPortTF.setFont(Constants.DEFAULT_FONT);
		mRemotePortTF = new TextField();
		mRemotePortTF.setFont(Constants.DEFAULT_FONT);
		mCmdTF = new TextField();
		mCmdTF.setFont(Constants.DEFAULT_FONT);
		mInfo = new JTextArea();
		mInfo.setLineWrap(true);
		mInfo.setWrapStyleWord(true);
		mInfo.setText("请连接...");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		//使用JScrollPane包裹TextArea
		JScrollPane jsp1 = new JScrollPane(mInfo);

		mClearBtn = new Button("清除窗口");
		mClearBtn.setFont(Constants.DEFAULT_FONT);

		mStatus = new Label("DISCONNECTED", Label.CENTER);
		mStatus.setFont(new Font("", Font.BOLD, 20));
		mStatus.setForeground(Color.RED);

		mConnBtn.addActionListener(this);
		mSendBtn.addActionListener(this);
		mClearBtn.addActionListener(this);

		panel.add(mStatus);
		panel.add(mConnBtn);
		panel.add(mCmdTF);
		panel.add(mSendBtn);
		panel.add(jsp1);
		panel.add(mClearBtn);
		
		panel.add(mForwardBtn);
		panel.add(localPortLabel);
		panel.add(remotePortLabel);
		panel.add(mLocalPortTF);
		panel.add(mRemotePortTF);

		//连接状态显示区域
		setConstraints(gbl, gbc, mStatus, GridBagConstraints.BOTH, 4, 1, 0, 0, 1, 0, null);

		//端口信息
		setConstraints(gbl, gbc, localPortLabel, GridBagConstraints.BOTH, 1, 1, 0, 1, 1, 0, null);
		setConstraints(gbl, gbc, mLocalPortTF, GridBagConstraints.BOTH, 1, 1, 1, 1, 1, 0, null);
		setConstraints(gbl, gbc, remotePortLabel, GridBagConstraints.BOTH, 1, 1, 2, 1, 1, 0, null);
		setConstraints(gbl, gbc, mRemotePortTF, GridBagConstraints.BOTH, 1, 1, 3, 1, 1, 0, null);
		setConstraints(gbl, gbc, mForwardBtn, GridBagConstraints.BOTH, 1, 1, 4, 1, 1, 0, null);
		
		//连接按钮
		setConstraints(gbl, gbc, mConnBtn, GridBagConstraints.BOTH, 1, 1, 4, 2, 1, 0, null);

		//输入指令区域
		setConstraints(gbl, gbc, mCmdTF, GridBagConstraints.BOTH, 4, 2, 0, 2, 1, 0, null);
		//发送按钮
		setConstraints(gbl, gbc, mSendBtn, GridBagConstraints.BOTH, 1, 1, 4, 3, 1, 0, null);
		//清除按钮
		setConstraints(gbl, gbc, mClearBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 4, 1, 0, null);
		//展示信息
		setConstraints(gbl, gbc, jsp1, GridBagConstraints.BOTH, 4, 1, 0, 4, 2, 1, null);
		return panel;
	}

	/**
	 * 设置布局
	 * @param gbl 布局
	 * @param gbc	布局容器
	 * @param comp	添加到容器中的组件
	 * @param gridwidth	占用宽度格数
	 * @param gridheight	占用高度植格数
	 * @param gridx	x坐标
	 * @param gridy y坐标
	 * @param weighx 窗口缩放时组件的缩放比例
	 * @param weighty 窗口缩放时组件的缩放比例
	 * @param insets	间隙
	 */
	public void setConstraints(GridBagLayout gbl, GridBagConstraints gbc, Component comp, int fill, int gridwidth,
			int gridheight, int gridx, int gridy, double weighx, double weighty, Insets insets) {
		gbc.fill = fill;
		gbc.gridwidth = gridwidth;
		gbc.gridheight = gridheight;
		gbc.gridx = gridx;
		gbc.gridy = gridy;
		gbc.weightx = weighx;
		gbc.weighty = weighty;
		if (insets == null) {
			insets = new Insets(5, 5, 5, 5);
		}
		gbc.insets = insets;
		gbl.setConstraints(comp, gbc);
	}

	private void start() {
		if (!setupAdbForward()) {
			mConnectStatus = false;
			printPanel("设置端口转发失败！");
			mStatus.setText("DISCONNECTED");
			mStatus.setForeground(Color.RED);
			mConnBtn.setLabel("打开连接");
			return;
		}
		mConnectStatus = true;
		printPanel("设置端口转发成功！");
		mStatus.setText("CONNECTED");
		mStatus.setForeground(Color.GREEN);
		mConnBtn.setLabel("断开连接");
		listenServer();
	}

	/**
	 * 监听服务端返回消息
	 */
	private void listenServer() {
		mListenServerThread = new ListenServerThread();
		mListenServerThread.start();
	}

	private class ListenServerThread extends Thread {
		@Override
		public void run() {
			try {
				while (mConnectStatus) {
					if (mSocket == null) {
						mSocket = new Socket("127.0.0.1", 8000);
					}
					byte[] buffer = new byte[256];
					DataInputStream dis = new DataInputStream(mSocket.getInputStream());
					int len = dis.read(buffer);
					if (len > 0) {
						String recevStr = new String(buffer, 0, len, "UTF-8");
						printPanel("Server:" + recevStr);
						if (JsonUtil.isJson(recevStr, 0)) {
							JSONObject jo = new JSONObject(recevStr);
							String eventType = jo.getString("EventType");
							jo.put("host", "client");
							if (eventType.equals("101")) {
								sendMsg(jo.toString());
								printPanel("Client:" + jo.toString());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private boolean setupAdbForward() {
		try {
			// PC上所有8000端口通信数据将被重定向到手机端9000端口server上
			Runtime.getRuntime().exec("adb forward tcp:8000 tcp:9000");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * 判断是否断开连接，断开返回true,没有返回false
	 * @param socket
	 * @return
	 */
	public Boolean isServerConnected(Socket socket) {
		if (socket == null)
			return false;
		return false;
		/*try {
			// 发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信
			socket.sendUrgentData(0);
			return true;
		} catch (Exception se) {
			return false;
		}*/
	}

	public void sendMsg(String msg) {
		// 建立socket对象，本机IP，8000端口
		try {
			printPanel("sever connction state:" + isServerConnected(mSocket));
			if (mSocket == null) {
				mSocket = new Socket("127.0.0.1", 8000);
			}
			byte[] buffer = new byte[256];
			DataInputStream dis = new DataInputStream(mSocket.getInputStream());
			DataOutputStream dos = new DataOutputStream(mSocket.getOutputStream());

			// 发送数据
			printPanel("Client:" + msg);
			// dos.writeUTF(msg);//该方法对方接收到有乱码
			dos.write(msg.getBytes("UTF-8"));
			dos.flush();
			Thread.sleep(500L);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == mConnBtn) {
			if (!mConnectStatus) {
				printPanel("start to connect server...");
				start();
			} else {
				printPanel("disconnect link to server...");
				if (mSocket != null) {
					try {
						mConnectStatus = false;
						mListenServerThread.stop();
						if (!mSocket.isClosed())
							mSocket.close();
						mStatus.setForeground(Color.RED);
						mStatus.setText("DISCONNECTED");
						mConnBtn.setLabel("打开连接");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else if (event.getSource() == mSendBtn) {
			printPanel("send msg to server");
			String cmdStr = mCmdTF.getText();
			if (!cmdStr.isEmpty()) {
				sendMsg(cmdStr);
			} else {
				printPanel("do not send empty body!");
			}
		} else if (event.getSource() == mClearBtn) {
			mInfo.setText("");
		}
	}

	public static void printPanel(String msg) {
		if (mInfo != null) {
			String textBefore = mInfo.getText();
			mInfo.setText(textBefore + "\r\n" + msg);
		}
	}
}
