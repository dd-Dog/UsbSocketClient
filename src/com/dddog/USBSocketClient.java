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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.json.JSONObject;
import org.omg.CORBA_2_3.portable.InputStream;

import com.dddog.packet.FramePacket;
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
	Button mReceiveStreamBtn;
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
	//接收的数据缓冲区
	private static LinkedList<FramePacket> mPacketList;
	private static int mBufferSize;
	private static final int BUFFER_SIZE = 50 * 100;

	public static void main(String[] args) throws IOException {

		mClient = new USBSocketClient();
		mClient.createFrame();
		mPacketList = new LinkedList<FramePacket>();
		mBufferSize = BUFFER_SIZE;
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
		mCmdTF = new JTextField("{\"CallNumber\":\"15033262664\",\"EventType\":\"1\"}");
		JScrollPane jsp2 = new JScrollPane(mCmdTF);
		mCmdTF.setFont(Constants.DEFAULT_FONT);
		mInfo = new JTextArea();
		mInfo.setLineWrap(true); //自动换行
		mInfo.setWrapStyleWord(true); //换行不断字，中文占两个字节，可以会中间换行
		mInfo.setText("请连接...");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		//使用JScrollPane包裹TextArea
		JScrollPane jsp1 = new JScrollPane(mInfo);

		mClearHistoryBtn = new Button("清空记录");
		mClearHistoryBtn.setFont(Constants.DEFAULT_FONT);

		mClearCmdBtn = new Button("清空指令");
		mClearCmdBtn.setFont(Constants.DEFAULT_FONT);

		mReceiveStreamBtn = new Button("接收PCM");
		mReceiveStreamBtn.setFont(Constants.DEFAULT_FONT);

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
		mReceiveStreamBtn.addActionListener(this);

		panel.add(mForwardStatus);
		panel.add(mConnStatus);
		panel.add(mConnBtn);
		panel.add(jsp2);
		panel.add(mSendBtn);
		panel.add(jsp1);
		panel.add(mClearHistoryBtn);
		panel.add(mClearCmdBtn);

		panel.add(mReceiveStreamBtn);
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

		setConstraints(gbl, gbc, mReceiveStreamBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 6, 1, 0, null);
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
	 * 收到客户端传过来的数据并写入到缓冲区
	 * @param packet
	 */

	public void addPacketToBuffer(FramePacket packet) {
		//如果缓冲区存储数据已经超过缓冲最大限度,删除最旧的FramePacket
		if (mPacketList.size() > mBufferSize) {
			takeAwayFirstPacket();
		}
		mPacketList.addLast(packet);
	}

	/**
	 * 获取缓存中第一个帧数据
	 * @return
	 */
	public byte[] takeAwayFirstFrame() {
		FramePacket packet = takeAwayFirstPacket();
		if (packet == null) {
			return null;
		}
		return packet.getFrame();
	}

	/**
	 * 删除第一个FramePacket
	 * @return
	 */
	private synchronized FramePacket takeAwayFirstPacket() {
		if (mPacketList.size() <= 0) {
			return null;
		}
		FramePacket fp = mPacketList.getFirst();
		if (fp == null) {
			return null;
		}
		FramePacket packet = new FramePacket(fp);
		mPacketList.removeFirst();
		return packet;
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
						System.out.println("isJson=" + JsonUtil.isJson(recevStr, 0) + ",isJsonObj="
								+ JsonUtil.isJsonObj(recevStr));
						if (JsonUtil.isJson(recevStr, 0) && JsonUtil.isJsonObj(recevStr)) {
							System.out.println("parse json");
							JSONObject jo = new JSONObject(recevStr);
							String eventType = jo.getString("EventType");
							if (eventType.equals("101")) { //101是服务器的心跳连接
								jo.put("host", "client");
								sendMsg(jo.toString());
								printPanel("Client:" + jo.toString());
							} else if (eventType.equals("102")) {
								receiveFile(null);
							}
						}
					} else {

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
		} else if (event.getSource() == mReceiveStreamBtn) {
			preapreReceivePCM();
		}
	}

	private void preapreReceivePCM() {
		setupAdbForward("9000", "9000");
		final String localPath = "C:\\Users\\bian\\Desktop";
		final String waveFiflePath = "C:\\Users\\bian\\Desktop\\test.wav";
		final String rawFilePath = "C:\\Users\\bian\\Desktop\\test.raw";
		final String fileName = "test.raw";
		final byte[] END_FLAY = new byte[640];
		new Thread() {
			public void run() {
				try {
					//建立socket连接
					Socket socket = new Socket("127.0.0.1", Constants.DOWNLOAD_PORT);

					java.io.InputStream is = socket.getInputStream();
					//获取文件输入流
					File file = new File(localPath, fileName);
					if (file.exists()) {
						file.delete();
					} else {
						file.createNewFile();
					}

					FileOutputStream fos = new FileOutputStream(file);
					// 开始接收文件
					byte[] bytes = new byte[640];
					int num = 0;
					boolean end = false;
					while ((num = is.read(bytes, 0, bytes.length)) != -1) {
						printPanel("receive data,size=" + num);
						if(bytes[0] == 0x7f && bytes[1] == 0x7f && bytes[2] == 0x7f && bytes[3] == 0x7f) {
							end = true;
							System.out.println(" receive end");
						}else {
							end = false;
						}
						if(end) {
							break;
						}
						fos.write(bytes, 0, num);
						fos.flush();
						
						
					}
					copyWaveFile(rawFilePath, waveFiflePath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	// 这里得到可播放的音频文件
	private static final int SAMPLE_RATE = 16000;

	private void copyWaveFile(String inFilename, String outFilename) {
		System.out.println("copyWaveFile");
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = SAMPLE_RATE;
		int channels = 1;
		long byteRate = 16 * SAMPLE_RATE * channels / 8;
		int minBufferSize = 2560;
		byte[] data = new byte[minBufferSize];
		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			writeWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);
			while (in.read(data) != -1) {
				out.write(data);
			}
			in.close();
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	* 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
	*/
	private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate,
			int channels, long byteRate) throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = 16; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}

	/**
	 * 接收 服务器传来的文件
	 * @param path
	 */
	private void receiveFile(String path) {
		System.out.println("receiveFile");
		final String path2 = "C:\\Users\\bian\\Desktop";
		//		if(true)return;
		new Thread() {
			@Override
			public void run() {
				try {
					//建立新的端口转发
					Runtime.getRuntime()
							.exec("adb forward tcp:" + Constants.DOWNLOAD_PORT + " tcp:" + Constants.SERVER_PORT);
					//建立socket连接
					Socket socket = new Socket("127.0.0.1", Constants.DOWNLOAD_PORT);

					//获取文件名称
					DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
					System.out.println("start read");
					int flag = dataInputStream.readInt();
					System.out.println("flag=" + flag);
					String fileName = dataInputStream.readUTF();
					System.out.println("fileName=" + fileName);
					//获取文件长度
					long readLong = dataInputStream.readLong();
					System.out.println("file length" + readLong);
					//获取文件输入流
					File file = new File(path2, fileName);
					if (file.exists()) {
						file.delete();
					} else {
						file.createNewFile();
					}

					FileOutputStream fos = new FileOutputStream(file);
					// 开始接收文件
					byte[] bytes = new byte[1024];
					int num = 0;
					while ((num = dataInputStream.read(bytes, 0, bytes.length)) != -1) {
						fos.write(bytes, 0, num);
						fos.flush();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

	public static void printPanel(String msg) {
		if (mInfo != null) {
			String textBefore = mInfo.getText();
			mInfo.setText(textBefore + "\r\n" + (DateFormatUtil.getTime4() + "---- " + msg));
			mInfo.setCaretPosition(mInfo.getText().length());
		}
	}
}
