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
import javax.swing.JTextField;

import org.json.JSONObject;

import com.dddog.util.DDLog;
import com.dddog.util.DateFormatUtil;
import com.dddog.util.JsonUtil;

public class USBSocketClient implements ActionListener {
	private Frame mFrame;
	private static USBSocketClient mClient;
	Button mConnBtn;
	Button mSendBtn;
	Button mClearHistoryBtn;
	Button mClearCmdBtn;
	Button mForwardBtn;
	JTextField mCmdTF;
	TextField mLocalPortTF;
	TextField mRemotePortTF;
	static JTextArea mInfo;
	Label mForwardStatus;
	Label mConnStatus;
	Socket mSocket;
	private static boolean mConnectStatus = false;//SOCKET连接状态
	private boolean mForwardSuccess = false; //adb端口转发状态
	ListenServerThread mListenServerThread;
	String mLocalPort;
	String mServerPort;

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

		mForwardBtn = new Button("转发端口");
		mForwardBtn.setFont(Constants.DEFAULT_FONT);
		mConnBtn = new Button("打开连接");
		mConnBtn.setFont(Constants.DEFAULT_FONT);
		mSendBtn = new Button("发送");
		mSendBtn.setFont(Constants.DEFAULT_FONT);
		Label localPortLabel = new Label("本地端口：", Label.CENTER);
		localPortLabel.setFont(Constants.DEFAULT_FONT);
		Label remotePortLabel = new Label("服务器端口：", Label.CENTER);
		remotePortLabel.setFont(Constants.DEFAULT_FONT);
		mLocalPortTF = new TextField(Constants.LOCAL_PORT);
		mLocalPortTF.setFont(Constants.DEFAULT_FONT);
		mRemotePortTF = new TextField(Constants.SERVER_PORT);
		mRemotePortTF.setFont(Constants.DEFAULT_FONT);
		mCmdTF = new JTextField();
		JScrollPane jsp2 = new JScrollPane(mCmdTF);
		mCmdTF.setFont(Constants.DEFAULT_FONT);
		mInfo = new JTextArea();
		mInfo.setLineWrap(true);	//自动换行
		mInfo.setWrapStyleWord(true);	//换行不断字，中文占两个字节，可以会中间换行
		mInfo.setText("请连接...");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		//使用JScrollPane包裹TextArea
		JScrollPane jsp1 = new JScrollPane(mInfo);

		mClearHistoryBtn = new Button("清空记录");
		mClearHistoryBtn.setFont(Constants.DEFAULT_FONT);

		mClearCmdBtn = new Button("清空指令");
		mClearCmdBtn.setFont(Constants.DEFAULT_FONT);

		mForwardStatus = new Label("ADB未转发", Label.CENTER);
		mForwardStatus.setFont(new Font("", Font.BOLD, 20));
		mForwardStatus.setForeground(Constants.DARK_RED);

		mConnStatus = new Label("SOCKET未连接", Label.CENTER);
		mConnStatus.setFont(new Font("", Font.BOLD, 20));
		mConnStatus.setForeground(Constants.DARK_RED);

		mForwardBtn.addActionListener(this);
		mConnBtn.addActionListener(this);
		mSendBtn.addActionListener(this);
		mClearHistoryBtn.addActionListener(this);
		mClearCmdBtn.addActionListener(this);

		panel.add(mForwardStatus);
		panel.add(mConnStatus);
		panel.add(mConnBtn);
		panel.add(jsp2);
		panel.add(mSendBtn);
		panel.add(jsp1);
		panel.add(mClearHistoryBtn);
		panel.add(mClearCmdBtn);

		panel.add(mForwardBtn);
		panel.add(localPortLabel);
		panel.add(remotePortLabel);
		panel.add(mLocalPortTF);
		panel.add(mRemotePortTF);

		//连接状态显示区域
		setConstraints(gbl, gbc, mForwardStatus, GridBagConstraints.BOTH, 2, 1, 0, 0, 1, 0, null);
		setConstraints(gbl, gbc, mConnStatus, GridBagConstraints.BOTH, 2, 1, 2, 0, 1, 0, null);

		//端口信息
		setConstraints(gbl, gbc, localPortLabel, GridBagConstraints.BOTH, 1, 1, 0, 1, 1, 0, null);
		setConstraints(gbl, gbc, mLocalPortTF, GridBagConstraints.BOTH, 1, 1, 1, 1, 1, 0, null);
		setConstraints(gbl, gbc, remotePortLabel, GridBagConstraints.BOTH, 1, 1, 2, 1, 1, 0, null);
		setConstraints(gbl, gbc, mRemotePortTF, GridBagConstraints.BOTH, 1, 1, 3, 1, 1, 0, null);
		setConstraints(gbl, gbc, mForwardBtn, GridBagConstraints.BOTH, 1, 1, 4, 1, 1, 0, null);

		//连接按钮
		setConstraints(gbl, gbc, mConnBtn, GridBagConstraints.BOTH, 1, 1, 4, 2, 1, 0, null);

		//输入指令区域
		setConstraints(gbl, gbc, jsp2, GridBagConstraints.BOTH, 4, 2, 0, 2, 1, 0, null);
		//发送按钮
		setConstraints(gbl, gbc, mSendBtn, GridBagConstraints.BOTH, 1, 1, 4, 3, 1, 0, null);
		//清除按钮
		setConstraints(gbl, gbc, mClearCmdBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 4, 1, 0, null);
		setConstraints(gbl, gbc, mClearHistoryBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 5, 1, 0, null);
		//展示信息
		setConstraints(gbl, gbc, jsp1, GridBagConstraints.BOTH, 4, 10, 0, 4, 2, 1, null);
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

	/**
	 * 连接到服务器
	 */
	private void connect2Server() {
		if (!mForwardSuccess) {
			printPanel("connect2Server failed, no create forward!");
			return;
		}
		new Thread() {
			public void run() {

				try {
					if (mSocket == null || (mSocket != null && mSocket.isClosed())) {
						if (mLocalPort != null && !mLocalPort.equals("")) {
							int localPort = Integer.parseInt(mLocalPort);
							mSocket = new Socket("127.0.0.1", localPort);
						}
					}
					byte[] buffer = new byte[256];
					DataInputStream dis = new DataInputStream(mSocket.getInputStream());
					DataOutputStream dos = new DataOutputStream(mSocket.getOutputStream());

					String msg = "Hello,this is client!";
					// 发送数据
					printPanel("Client:" + msg);
					dos.write(msg.getBytes("UTF-8"));
					dos.flush();

					printPanel("waiting response...");
					//读取回应
					int len = dis.read(buffer);
					if (len < 0) {
						printPanel("Server not response!");
						mConnectStatus = false;
						if (!mSocket.isClosed())
							mSocket.close();
						updateConnectStatus();
					} else {
						mConnectStatus = true;
						String ack = new String(buffer, 0, len);
						printPanel("Server:" + ack);
						printPanel("Server connected!");
						listenServer();
					}
				} catch (Exception e) {
					e.printStackTrace();
					mConnectStatus = false;
					if (mSocket != null) {
						try {
							mSocket.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				} finally {
					updateConnectStatus();
				}
			}
		}.start();
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
					if (mSocket == null || (mSocket != null && mSocket.isClosed())) {
						if (mLocalPort != null && !mLocalPort.equals("")) {
							int localPort = Integer.parseInt(mLocalPort);
							mSocket = new Socket("127.0.0.1", localPort);
						}
					}
					int buffSize = 256;
					byte[] buffer = new byte[buffSize];
					DataInputStream dis = new DataInputStream(mSocket.getInputStream());
					int sumLen = 0;//接收 的总长度
					int len = -1;
					StringBuilder sb = new StringBuilder();
					/*开始读取并拼接字符串到sb中*/
					while ((len = dis.read(buffer)) >= buffSize) {
						String str = new String(buffer, 0, len, "UTF-8");
						sb.append(str);
						sumLen += len;
					}
					sumLen += len;
					if (sumLen > 0) {
						String str = new String(buffer, 0, len, "UTF-8");
	                    sb.append(str);
						String recevStr = sb.toString();
						/*拼接字符串完成*/
						printPanel("Server:" + recevStr);
						if (JsonUtil.isJson(recevStr, 0) && JsonUtil.isJsonObj(recevStr)) {
							JSONObject jo = new JSONObject(recevStr);
							String eventType = jo.getString("EventType");
							jo.put("host", "client");
							if (eventType.equals("101")) { //101是服务器的心跳连接
								sendMsg(jo.toString());
								printPanel("Client:" + jo.toString());
							}
						}
					}else {
						
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void updateConnectStatus() {
		if (!mForwardSuccess) {
			mConnectStatus = false;
		}
		printPanel(mConnectStatus ? "SOCKET连接成功！" : "SOCKET未连接！");
		mConnStatus.setText(mConnectStatus ? "SOCKET连接成功" : "SOCKET未连接");
		mConnStatus.setForeground(mConnectStatus ? Constants.DARK_GREEN : Constants.DARK_RED);
		mConnBtn.setLabel(mConnectStatus ? "断开连接" : "开始连接");
	}

	private void updateForwardStatus() {
		printPanel(mForwardSuccess ? "端口转发成功！" : "未转发！");
		mForwardStatus.setText(mForwardSuccess ? "ADB转发成功" : "ADB未转发");
		mForwardStatus.setForeground(mForwardSuccess ? Constants.DARK_GREEN : Constants.DARK_RED);
		mForwardBtn.setLabel(mForwardSuccess ? "移除转发" : "转发端口");
		updateConnectStatus();
	}

	/**
	 * 移除转发
	 * @param localPort
	 */
	private void removeAdbForward(String localPort) {
		try {
			Runtime.getRuntime().exec("adb forward --remove tcp:" + localPort);
			mForwardSuccess = false;
			printPanel("adb forward remove port" + localPort);
			updateForwardStatus();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 建立转发
	 * @param localPort	本地端口
	 * @param serverPort	服务器端口
	 * @return
	 */
	private boolean setupAdbForward(String localPort, String serverPort) {
		try {
			mForwardSuccess = false;
			// PC机localPort端口通信数据将被重定向到手机端serverPort端口server上
			Runtime.getRuntime().exec("adb forward tcp:" + localPort + " tcp:" + serverPort);
			//读取结果，判断是否转发成功
			Process process = Runtime.getRuntime().exec("adb forward --list");
			DataInputStream dis = new DataInputStream(process.getInputStream());
			byte[] buf = new byte[8];
			int len = -1;
			StringBuilder sb = new StringBuilder();
			while ((len = dis.read(buf)) != -1) {
				String str = new String(buf, 0, len);
				sb.append(str);
			}
			String adbList = sb.toString().toString();
			String[] forwardArr = adbList.split("\n");
			for (String forward : forwardArr) {
				if (forward.contains(localPort) && forward.contains(serverPort)) {
					printPanel("forward=" + forward);
					mForwardSuccess = true;
					break;
				}
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			updateForwardStatus();

		}
		return false;
	}

	/**
	 * 发送消息
	 * @param msg
	 */
	public void sendMsg(String msg) {
		// 建立socket对象，本机IP，8000端口
		if (!mForwardSuccess || !mConnectStatus) {
			printPanel("请先建立连接！");
			return;
		}
		try {
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
		if (event.getSource() == mForwardBtn) {
			if (mForwardSuccess) {
				if (mConnectStatus) {
					printPanel("请先断开连接 ！");
				} else {
					removeAdbForward(mLocalPort);
				}
			} else {
				mLocalPort = mLocalPortTF.getText().trim();
				mServerPort = mRemotePortTF.getText().trim();
				setupAdbForward(mLocalPort, mServerPort);
			}

		} else if (event.getSource() == mConnBtn) {
			if (!mForwardSuccess) {
				printPanel("请先建立转发！");
				return;
			}
			System.out.println("1111");
			if (!mConnectStatus) {
				System.out.println("2222");
				printPanel("start to connect server...");
				connect2Server();
			} else {
				printPanel("disconnect link to server...");
				if (mSocket != null) {
					try {
						mConnectStatus = false;
						mListenServerThread.stop();
						if (!mSocket.isClosed())
							mSocket.close();
						updateConnectStatus();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else if (event.getSource() == mSendBtn) {
			if (!mForwardSuccess) {
				printPanel("请先建立转发！");
				return;
			}
			if (!mConnectStatus) {
				printPanel("请先建立连接！");
				return;
			}
			printPanel("send msg to server");
			String cmdStr = mCmdTF.getText();
			if (!cmdStr.isEmpty()) {
				sendMsg(cmdStr);
			} else {
				printPanel("do not send empty body!");
			}
		} else if (event.getSource() == mClearHistoryBtn) {
			mInfo.setText("");
		} else if (event.getSource() == mClearCmdBtn) {
			//TextFiled不允许设置为空
			mCmdTF.setText(" ");
			JTextField jtf;
		}
	}

	public static void printPanel(String msg) {
		if (mInfo != null) {
			String textBefore = mInfo.getText();
			mInfo.setText(textBefore + "\r\n" + (DateFormatUtil.getTime4() + "---- "+msg));
			mInfo.setCaretPosition(mInfo.getText().length());
		}
	}
}
