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
	private static boolean mConnectStatus = false;//SOCKET����״̬
	private boolean mForwardSuccess = false; //adb�˿�ת��״̬
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
		mCmdTF = new JTextField();
		JScrollPane jsp2 = new JScrollPane(mCmdTF);
		mCmdTF.setFont(Constants.DEFAULT_FONT);
		mInfo = new JTextArea();
		mInfo.setLineWrap(true);	//�Զ�����
		mInfo.setWrapStyleWord(true);	//���в����֣�����ռ�����ֽڣ����Ի��м任��
		mInfo.setText("������...");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		//ʹ��JScrollPane����TextArea
		JScrollPane jsp1 = new JScrollPane(mInfo);

		mClearHistoryBtn = new Button("��ռ�¼");
		mClearHistoryBtn.setFont(Constants.DEFAULT_FONT);

		mClearCmdBtn = new Button("���ָ��");
		mClearCmdBtn.setFont(Constants.DEFAULT_FONT);

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
						if (JsonUtil.isJson(recevStr, 0) && JsonUtil.isJsonObj(recevStr)) {
							JSONObject jo = new JSONObject(recevStr);
							String eventType = jo.getString("EventType");
							jo.put("host", "client");
							if (eventType.equals("101")) { //101�Ƿ���������������
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
