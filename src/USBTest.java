
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class USBTest {
	public static void main(String[] args) throws IOException {
		if (!setupAdbForward()) {
			System.out.println("���ö˿�ת��ʧ��");
			return;
		}
		System.out.println("�����ַ�, �س�������Toast");
		//ͨ��Scanner��ȡ����̨����
		Scanner scanner = new Scanner(System.in);
		while (true) {
			String msg = scanner.next();
			sendToast(msg);
		}
	}

	private static boolean setupAdbForward() {
		try {
			//PC������8000�˿�ͨ�����ݽ����ض����ֻ���9000�˿�server��
			Runtime.getRuntime().exec("adb forward tcp:8000 tcp:9000");
			return true;
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		return false;
	}

	public static void sendToast(String msg) throws IOException {
		//����socket���󣬱���IP��8000�˿�
		Socket socket = new Socket("127.0.0.1", 8000);
		byte[] buffer = new byte[256];
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		//��������
		dos.writeUTF(msg);
		dos.flush();
		//��������
		int len = dis.read(buffer);

		if (len > 0) {
			System.out.println("\n���յ���" + new String(buffer, 0, len, "UTF-8"));
		}
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		socket.close();
	}
}
