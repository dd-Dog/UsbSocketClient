package com.dddog.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bian on 2018/12/6.
 */

   /*
     * 娉ㄦ剰: format(Date date)杩欎釜鏂规硶鏉ヨ嚜浜嶴impleDateFormat鐨勭埗绫籇ateFormat
     * String str1 = sdf1.format(date1);
       System.out.println("瀛楃涓茬被鍨嬫椂闂�:" + str1);
       // 瀛楃涓茬被鍨嬫椂闂�-銆嬭浆鎹负瀹氫箟鏍煎紡-銆嬫棩鏈熺被鍨嬫椂闂�
       Date dateF1 = sdf1.parse(str1);
       System.out.println("鏃ユ湡绫诲瀷鏃堕棿:" + dateF1);
       // **************2.鍏充簬甯哥敤鏍煎紡鍒嗘瀽*************
       System.out.println("----------甯哥敤鏍煎紡鍒嗘瀽---------");

     * y : 骞�
     * M : 骞翠腑鐨勬湀浠�
     * D : 骞翠腑鐨勫ぉ鏁�
     * d : 鏈堜腑鐨勫ぉ鏁�
     * w : 骞翠腑鐨勫懆鏁�
     * W : 鏈堜腑鐨勫懆鏁�
     * a : 涓婁笅/涓嬪崍
     * H : 涓�澶╀腑鐨勫皬鏃舵暟(0-23)
     * h : 涓�澶╀腑鐨勫皬鏃舵暟(0-12)
     * m : 灏忔椂涓殑鍒嗛挓
     * s : 鍒嗛挓閽熺殑绉掓暟
     * S : 姣鏁�

      SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd,w,W,a,HH:mm:ss,SS");
      String str2 = sdf2.format(new Date());
      System.out.println("鏃ユ湡绫诲瀷鏃堕棿:" + str2);
      System.out.println("瀛楃涓茬被鍨嬫椂闂�:" + sdf2.parse(str2));
      // **************2.鍏充簬鏋勯�犲櫒浣跨敤鎶�宸у垎鏋�*************
      System.out.println("----------鏋勯�犲櫒浣跨敤鎶�宸у垎鏋�---------");

     * 鏋勯�犲櫒:
     * SimpleDateFormat();
     * SimpleDateFormat(String pattern);
     * SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols);
     * SimpleDateFormat(String pattern, Locale locale)
     */

public class DateFormatUtil {
    /**
     * @return 2018-12-06 14:52:48
     */
    public static String getTime1() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

    /**
     * @return 2018.12.06-14:52
     */
    public static String getTime2() {
        return new SimpleDateFormat("yyyy.MM.dd-HH:mm").format(new Date());
    }

    /**
     * @return 2018-12-06
     */
    public static String getTime3() {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
    
    /**
     * @return 2018-12-18 16:56:06,615
     */
    public static String getTime4() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SS").format(new Date());
    }
}
