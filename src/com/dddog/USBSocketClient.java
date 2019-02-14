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
	private static boolean mConnectStatus = false;//SOCKET����״̬
	private boolean mForwardSuccess = false; //adb�˿�ת��״̬
	ListenServerThread mListenServerThread;
	String mLocalPort;
	String mServerPort;
	//���յ����ݻ�����
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

		mForwardBtn = new Button("ת���˿�");
		mForwardBtn.setFont(Constants.DEFAULT_FONT);
		mConnBtn = new Button("������");
		mConnBtn.setFont(Constants.DEFAULT_FONT);
		mSendBtn = new Button("����");
		mSendBtn.setFont(Constants.DEFAULT_FONT);
		Label localPortLabel = new Label("���ض˿ڣ�", Label.CENTER);
		localPortLabel.setFont(Constants.DEFAULT_FONT);
		Label remotePortLabel = new Label("�������˿ڣ�", Label.CENTER);
		remotePortLabel.setFont(Constants.DEFAULT_FONT);
		mLocalPortTF = new TextField(Constants.LOCAL_PORT);
		mLocalPortTF.setFont(Constants.DEFAULT_FONT);
		mRemotePortTF = new TextField(Constants.SERVER_PORT);
		mRemotePortTF.setFont(Constants.DEFAULT_FONT);
		mCmdTF = new JTextField("{\"CallNumber\":\"15033262664\",\"EventType\":\"1\"}");
		JScrollPane jsp2 = new JScrollPane(mCmdTF);
		mCmdTF.setFont(Constants.DEFAULT_FONT);
		mInfo = new JTextArea();
		mInfo.setLineWrap(true); //�Զ�����
		mInfo.setWrapStyleWord(true); //���в����֣�����ռ�����ֽڣ����Ի��м任��
		mInfo.setText("������...");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		//ʹ��JScrollPane����TextArea
		JScrollPane jsp1 = new JScrollPane(mInfo);

		mClearHistoryBtn = new Button("��ռ�¼");
		mClearHistoryBtn.setFont(Constants.DEFAULT_FONT);

		mClearCmdBtn = new Button("���ָ��");
		mClearCmdBtn.setFont(Constants.DEFAULT_FONT);

		mReceiveStreamBtn = new Button("����PCM");
		mReceiveStreamBtn.setFont(Constants.DEFAULT_FONT);

		mForwardStatus = new Label("ADBδת��", Label.CENTER);
		mForwardStatus.setFont(new Font("", Font.BOLD, 20));
		mForwardStatus.setForeground(Constants.DARK_RED);

		mConnStatus = new Label("SOCKETδ����", Label.CENTER);
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

		//����״̬��ʾ����
		setConstraints(gbl, gbc, mForwardStatus, GridBagConstraints.BOTH, 2, 1, 0, 0, 1, 0, null);
		setConstraints(gbl, gbc, mConnStatus, GridBagConstraints.BOTH, 2, 1, 2, 0, 1, 0, null);

		//�˿���Ϣ
		setConstraints(gbl, gbc, localPortLabel, GridBagConstraints.BOTH, 1, 1, 0, 1, 1, 0, null);
		setConstraints(gbl, gbc, mLocalPortTF, GridBagConstraints.BOTH, 1, 1, 1, 1, 1, 0, null);
		setConstraints(gbl, gbc, remotePortLabel, GridBagConstraints.BOTH, 1, 1, 2, 1, 1, 0, null);
		setConstraints(gbl, gbc, mRemotePortTF, GridBagConstraints.BOTH, 1, 1, 3, 1, 1, 0, null);
		setConstraints(gbl, gbc, mForwardBtn, GridBagConstraints.BOTH, 1, 1, 4, 1, 1, 0, null);

		//���Ӱ�ť
		setConstraints(gbl, gbc, mConnBtn, GridBagConstraints.BOTH, 1, 1, 4, 2, 1, 0, null);

		//����ָ������
		setConstraints(gbl, gbc, jsp2, GridBagConstraints.BOTH, 4, 2, 0, 2, 1, 0, null);
		//���Ͱ�ť
		setConstraints(gbl, gbc, mSendBtn, GridBagConstraints.BOTH, 1, 1, 4, 3, 1, 0, null);
		//�����ť
		setConstraints(gbl, gbc, mClearCmdBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 4, 1, 0, null);
		setConstraints(gbl, gbc, mClearHistoryBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 5, 1, 0, null);

		setConstraints(gbl, gbc, mReceiveStreamBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 6, 1, 0, null);
		//չʾ��Ϣ
		setConstraints(gbl, gbc, jsp1, GridBagConstraints.BOTH, 4, 10, 0, 4, 2, 1, null);
		return panel;
	}

	/**
	 * ���ò���
	 * @param gbl ����
	 * @param gbc	��������
	 * @param comp	��ӵ������е����
	 * @param gridwidth	ռ�ÿ�ȸ���
	 * @param gridheight	ռ�ø߶�ֲ����
	 * @param gridx	x����
	 * @param gridy y����
	 * @param weighx ��������ʱ��������ű���
	 * @param weighty ��������ʱ��������ű���
	 * @param insets	��϶
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
	 * �յ��ͻ��˴����������ݲ�д�뵽������
	 * @param packet
	 */

	public void addPacketToBuffer(FramePacket packet) {
		//����������洢�����Ѿ�������������޶�,ɾ����ɵ�FramePacket
		if (mPacketList.size() > mBufferSize) {
			takeAwayFirstPacket();
		}
		mPacketList.addLast(packet);
	}

	/**
	 * ��ȡ�����е�һ��֡����
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
	 * ɾ����һ��FramePacket
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
	 * ���ӵ�������
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
					// ��������
					printPanel("Client:" + msg);
					dos.write(msg.getBytes("UTF-8"));
					dos.flush();

					printPanel("waiting response...");
					//��ȡ��Ӧ
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
	 * ��������˷�����Ϣ
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
					int sumLen = 0;//���� ���ܳ���
					int len = -1;
					StringBuilder sb = new StringBuilder();
					/*��ʼ��ȡ��ƴ���ַ�����sb��*/
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
						/*ƴ���ַ������*/
						printPanel("Server:" + recevStr);
						System.out.println("isJson=" + JsonUtil.isJson(recevStr, 0) + ",isJsonObj="
								+ JsonUtil.isJsonObj(recevStr));
						if (JsonUtil.isJson(recevStr, 0) && JsonUtil.isJsonObj(recevStr)) {
							System.out.println("parse json");
							JSONObject jo = new JSONObject(recevStr);
							String eventType = jo.getString("EventType");
							if (eventType.equals("101")) { //101�Ƿ���������������
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
		printPanel(mConnectStatus ? "SOCKET���ӳɹ���" : "SOCKETδ���ӣ�");
		mConnStatus.setText(mConnectStatus ? "SOCKET���ӳɹ�" : "SOCKETδ����");
		mConnStatus.setForeground(mConnectStatus ? Constants.DARK_GREEN : Constants.DARK_RED);
		mConnBtn.setLabel(mConnectStatus ? "�Ͽ�����" : "��ʼ����");
	}

	private void updateForwardStatus() {
		printPanel(mForwardSuccess ? "�˿�ת���ɹ���" : "δת����");
		mForwardStatus.setText(mForwardSuccess ? "ADBת���ɹ�" : "ADBδת��");
		mForwardStatus.setForeground(mForwardSuccess ? Constants.DARK_GREEN : Constants.DARK_RED);
		mForwardBtn.setLabel(mForwardSuccess ? "�Ƴ�ת��" : "ת���˿�");
		updateConnectStatus();
	}

	/**
	 * �Ƴ�ת��
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
	 * ����ת��
	 * @param localPort	���ض˿�
	 * @param serverPort	�������˿�
	 * @return
	 */
	private boolean setupAdbForward(String localPort, String serverPort) {
		try {
			mForwardSuccess = false;
			// PC��localPort�˿�ͨ�����ݽ����ض����ֻ���serverPort�˿�server��
			Runtime.getRuntime().exec("adb forward tcp:" + localPort + " tcp:" + serverPort);
			//��ȡ������ж��Ƿ�ת���ɹ�
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
	 * ������Ϣ
	 * @param msg
	 */
	public void sendMsg(String msg) {
		// ����socket���󣬱���IP��8000�˿�
		if (!mForwardSuccess || !mConnectStatus) {
			printPanel("���Ƚ������ӣ�");
			return;
		}
		try {
			byte[] buffer = new byte[256];
			DataInputStream dis = new DataInputStream(mSocket.getInputStream());
			DataOutputStream dos = new DataOutputStream(mSocket.getOutputStream());

			// ��������
			printPanel("Client:" + msg);
			// dos.writeUTF(msg);//�÷����Է����յ�������
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
					printPanel("���ȶϿ����� ��");
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
				printPanel("���Ƚ���ת����");
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
				printPanel("���Ƚ���ת����");
				return;
			}
			if (!mConnectStatus) {
				printPanel("���Ƚ������ӣ�");
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
			//TextFiled����������Ϊ��
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
					//����socket����
					Socket socket = new Socket("127.0.0.1", Constants.DOWNLOAD_PORT);

					java.io.InputStream is = socket.getInputStream();
					//��ȡ�ļ�������
					File file = new File(localPath, fileName);
					if (file.exists()) {
						file.delete();
					} else {
						file.createNewFile();
					}

					FileOutputStream fos = new FileOutputStream(file);
					// ��ʼ�����ļ�
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

	// ����õ��ɲ��ŵ���Ƶ�ļ�
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
	* �����ṩһ��ͷ��Ϣ��������Щ��Ϣ�Ϳ��Եõ����Բ��ŵ��ļ���
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
	 * ���� �������������ļ�
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
					//�����µĶ˿�ת��
					Runtime.getRuntime()
							.exec("adb forward tcp:" + Constants.DOWNLOAD_PORT + " tcp:" + Constants.SERVER_PORT);
					//����socket����
					Socket socket = new Socket("127.0.0.1", Constants.DOWNLOAD_PORT);

					//��ȡ�ļ�����
					DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
					System.out.println("start read");
					int flag = dataInputStream.readInt();
					System.out.println("flag=" + flag);
					String fileName = dataInputStream.readUTF();
					System.out.println("fileName=" + fileName);
					//��ȡ�ļ�����
					long readLong = dataInputStream.readLong();
					System.out.println("file length" + readLong);
					//��ȡ�ļ�������
					File file = new File(path2, fileName);
					if (file.exists()) {
						file.delete();
					} else {
						file.createNewFile();
					}

					FileOutputStream fos = new FileOutputStream(file);
					// ��ʼ�����ļ�
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
