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

		mForwardBtn = new Button("ADBת���˿�");
		mForwardBtn.setFont(Constants.DEFAULT_FONT);
		mConnBtn = new Button("������");
		mConnBtn.setFont(Constants.DEFAULT_FONT);
		mSendBtn = new Button("����");
		mSendBtn.setFont(Constants.DEFAULT_FONT);
		Label localPortLabel = new Label("���ض˿ڣ�",Label.CENTER );
		localPortLabel.setFont(Constants.DEFAULT_FONT);
		Label remotePortLabel = new Label("�������˿ڣ�",Label.CENTER);
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
		mInfo.setText("������...");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		//ʹ��JScrollPane����TextArea
		JScrollPane jsp1 = new JScrollPane(mInfo);

		mClearBtn = new Button("�������");
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

		//����״̬��ʾ����
		setConstraints(gbl, gbc, mStatus, GridBagConstraints.BOTH, 4, 1, 0, 0, 1, 0, null);

		//�˿���Ϣ
		setConstraints(gbl, gbc, localPortLabel, GridBagConstraints.BOTH, 1, 1, 0, 1, 1, 0, null);
		setConstraints(gbl, gbc, mLocalPortTF, GridBagConstraints.BOTH, 1, 1, 1, 1, 1, 0, null);
		setConstraints(gbl, gbc, remotePortLabel, GridBagConstraints.BOTH, 1, 1, 2, 1, 1, 0, null);
		setConstraints(gbl, gbc, mRemotePortTF, GridBagConstraints.BOTH, 1, 1, 3, 1, 1, 0, null);
		setConstraints(gbl, gbc, mForwardBtn, GridBagConstraints.BOTH, 1, 1, 4, 1, 1, 0, null);
		
		//���Ӱ�ť
		setConstraints(gbl, gbc, mConnBtn, GridBagConstraints.BOTH, 1, 1, 4, 2, 1, 0, null);

		//����ָ������
		setConstraints(gbl, gbc, mCmdTF, GridBagConstraints.BOTH, 4, 2, 0, 2, 1, 0, null);
		//���Ͱ�ť
		setConstraints(gbl, gbc, mSendBtn, GridBagConstraints.BOTH, 1, 1, 4, 3, 1, 0, null);
		//�����ť
		setConstraints(gbl, gbc, mClearBtn, GridBagConstraints.HORIZONTAL, 1, 1, 4, 4, 1, 0, null);
		//չʾ��Ϣ
		setConstraints(gbl, gbc, jsp1, GridBagConstraints.BOTH, 4, 1, 0, 4, 2, 1, null);
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

	private void start() {
		if (!setupAdbForward()) {
			mConnectStatus = false;
			printPanel("���ö˿�ת��ʧ�ܣ�");
			mStatus.setText("DISCONNECTED");
			mStatus.setForeground(Color.RED);
			mConnBtn.setLabel("������");
			return;
		}
		mConnectStatus = true;
		printPanel("���ö˿�ת���ɹ���");
		mStatus.setText("CONNECTED");
		mStatus.setForeground(Color.GREEN);
		mConnBtn.setLabel("�Ͽ�����");
		listenServer();
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
			// PC������8000�˿�ͨ�����ݽ����ض����ֻ���9000�˿�server��
			Runtime.getRuntime().exec("adb forward tcp:8000 tcp:9000");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * �ж��Ƿ�Ͽ����ӣ��Ͽ�����true,û�з���false
	 * @param socket
	 * @return
	 */
	public Boolean isServerConnected(Socket socket) {
		if (socket == null)
			return false;
		return false;
		/*try {
			// ����1���ֽڵĽ������ݣ�Ĭ������£���������û�п����������ݴ�����Ӱ������ͨ��
			socket.sendUrgentData(0);
			return true;
		} catch (Exception se) {
			return false;
		}*/
	}

	public void sendMsg(String msg) {
		// ����socket���󣬱���IP��8000�˿�
		try {
			printPanel("sever connction state:" + isServerConnected(mSocket));
			if (mSocket == null) {
				mSocket = new Socket("127.0.0.1", 8000);
			}
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
						mConnBtn.setLabel("������");
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
