package com.dddog;

import java.awt.Button;
import java.awt.Color;
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

import javax.swing.JTextArea;

public class USBSocketClient implements ActionListener {
	private Frame mFrame;
	private static USBSocketClient mClient;
	Button mConnBtn;
	Button mSendBtn;
	TextField mCmdTF;
	static JTextArea mInfo;
	Label mStatus;
	Socket mSocket;
	private static boolean mConnectStatus = false;;

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
		//get screen size
		Dimension screensize = Toolkit.getDefaultToolkit().getScreenSize();
		int screenHeight = (int) screensize.getHeight();
		int screenWidth = (int) screensize.getWidth();
		//set frame layout center in screen
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

		mConnBtn = new Button("������");
		mConnBtn.setFont(Constants.DEFAULT_FONT);
		mSendBtn = new Button("����");
		mSendBtn.setFont(Constants.DEFAULT_FONT);
		mCmdTF = new TextField();
		mCmdTF.setFont(Constants.DEFAULT_FONT);
		mInfo = new JTextArea();
		mInfo.setLineWrap(true);
		mInfo.setWrapStyleWord(true);
		mInfo.setText("Test for JLabel");
		mInfo.setFont(new Font("", Font.PLAIN, 18));
		
		mStatus = new Label("DISCONNECTED", Label.CENTER);
		mStatus.setFont(new Font("",Font.BOLD, 20));
		mStatus.setForeground(Color.RED);

		mConnBtn.addActionListener(this);
		mSendBtn.addActionListener(this);

		panel.add(mStatus);
		panel.add(mConnBtn);
		panel.add(mCmdTF);
		panel.add(mSendBtn);
		panel.add(mInfo);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbl.setConstraints(mInfo, gbc);

		gbc.fill = GridBagConstraints.BOTH;
		//���ռ��������
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		//λ������
		gbc.gridx = 1;
		gbc.gridy = 0;
		//�����ʽ
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		//����Լ��ָ���ؼ���Ӧ�õ����ֹ�����
		gbl.setConstraints(mConnBtn, gbc);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbl.setConstraints(mCmdTF, gbc);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbl.setConstraints(mSendBtn, gbc);

		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.weightx = 2;
		gbc.weighty = 1;
		gbc.insets = new Insets(5, 5, 5, 5);
		gbl.setConstraints(mInfo, gbc);
		return panel;
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
		new Thread() {
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
							printPanel("Server:" + new String(buffer, 0, len, "UTF-8"));
						}
					}
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();

	}

	private boolean setupAdbForward() {
		try {
			//PC������8000�˿�ͨ�����ݽ����ض����ֻ���9000�˿�server��
			Runtime.getRuntime().exec("adb forward tcp:8000 tcp:9000");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void sendMsg(String msg) {
		//����socket���󣬱���IP��8000�˿�
		try {
			if (mSocket == null) {
				mSocket = new Socket("127.0.0.1", 8000);
			}
			byte[] buffer = new byte[256];
			DataInputStream dis = new DataInputStream(mSocket.getInputStream());
			DataOutputStream dos = new DataOutputStream(mSocket.getOutputStream());
			//��������
			printPanel("Client:" + msg);
			//			dos.writeUTF(msg);//�÷����Է����յ�������
			dos.write(msg.getBytes());
			dos.flush();
			//��������
//			int len = dis.read(buffer);
//
//			if (len > 0) {
//				printPanel("Server:" + new String(buffer, 0, len, "UTF-8"));
//			}
			Thread.sleep(1000L);
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
						mSocket.close();
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
		}
	}

	public static void printPanel(String msg) {
		if (mInfo != null) {
			String textBefore = mInfo.getText();
			mInfo.setText(textBefore + "\r\n" + msg);
		}
	}
}
