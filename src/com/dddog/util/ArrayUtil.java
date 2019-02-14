package com.dddog.util;

/**
 * Created by bian on 2019/2/14.
 */

public class ArrayUtil {

    /**
     * byte鏁扮粍杞瑂hort鏁扮粍 姣忎袱涓猙yte杞负涓�涓猻hort绫诲瀷
     * 澶х妯″紡锛氭暟鎹殑楂樺瓧鑺備繚瀛樺湪鍐呭瓨鐨勪綆鍦板潃涓紝鑰屾暟鎹殑浣庡瓧鑺備繚瀛樺湪鍐呭瓨鐨勯珮鍦板潃涓�
     * @param src
     * @return
     */
    public static short[] toShortArray(byte[] src) {

        int count = src.length >> 1;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {
            dest[i] = (short) (src[i * 2] << 8 | src[2 * i + 1] & 0xff);
        }
        return dest;
    }

    /**
     *short鏁扮粍杞负byte鏁扮粍
     * 澶х妯″紡锛氭暟鎹殑楂樺瓧鑺備繚瀛樺湪鍐呭瓨鐨勪綆鍦板潃涓紝鑰屾暟鎹殑浣庡瓧鑺備繚瀛樺湪鍐呭瓨鐨勯珮鍦板潃涓�
     * @param src
     * @return
     */
    public static byte[] toByteArray(short[] src) {
        int count = src.length;
        byte[] dest = new byte[count << 1];
        for (int i = 0; i < count; i++) {
            dest[i * 2] = (byte) (src[i] >> 8);
            dest[i * 2 + 1] = (byte) (src[i] >> 0);
        }

        return dest;
    }
}
