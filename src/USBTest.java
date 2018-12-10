
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Scanner;

public class USBTest {
	public static void main(String[] args) throws IOException {
		if (!setupAdbForward()) {
			System.out.println("设置端口转发失败");
			return;
		}
		System.out.println("任意字符, 回车键发送Toast");
		//通过Scanner获取控制台输入
		Scanner scanner = new Scanner(System.in);
		while (true) {
			String msg = scanner.next();
			sendToast(msg);
		}
	}

	private static boolean setupAdbForward() {
		try {
			//PC上所有8000端口通信数据将被重定向到手机端9000端口server上
			Runtime.getRuntime().exec("adb forward tcp:8000 tcp:9000");
			return true;
		} catch (IOException e3) {
			e3.printStackTrace();
		}
		return false;
	}

	public static void sendToast(String msg) throws IOException {
		//建立socket对象，本机IP，8000端口
		Socket socket = new Socket("127.0.0.1", 8000);
		byte[] buffer = new byte[256];
		DataInputStream dis = new DataInputStream(socket.getInputStream());
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		//发送数据
		dos.writeUTF(msg);
		dos.flush();
		//接收数据
		int len = dis.read(buffer);

		if (len > 0) {
			System.out.println("\n接收到：" + new String(buffer, 0, len, "UTF-8"));
		}
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		socket.close();
	}
}
