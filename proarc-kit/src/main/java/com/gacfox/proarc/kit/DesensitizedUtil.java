package com.gacfox.proarc.kit;

/**
 * 敏感信息脱敏工具类
 */
public class DesensitizedUtil {
    /**
     * 邮箱脱敏，输入格式xxx@xx.xx，脱敏结果x**@xx.xx
     *
     * @param email 邮箱字符串
     * @return 结果字符串
     */
    public static String hideEmail(String email) {
        if (email != null && email.contains("@")) {
            String[] emailSplit = new String[2];
            emailSplit[0] = email.substring(0, email.indexOf("@"));
            emailSplit[1] = email.substring(email.indexOf("@") + 1);
            return hideCommonStr(emailSplit[0], 1, Integer.MAX_VALUE) + "@" + emailSplit[1];
        }
        return email;
    }

    /**
     * 中文名脱敏
     *
     * @param name 中文名
     * @return 结果字符串
     */
    public static String hideChineseName(String name) {
        return hideCommonStr(name, 1, Integer.MAX_VALUE);
    }

    /**
     * 11位手机号脱敏
     *
     * @param phoneNum 11位手机号
     * @return 结果字符串
     */
    public static String hidePhoneNum11(String phoneNum) {
        return hideCommonStr(phoneNum, 3, 4);
    }

    /**
     * 身份证号脱敏，隐藏身份证号第11-16位
     *
     * @param idNum 身份证号
     * @return 结果字符串
     */
    public static String hideIdentityCardId(String idNum) {
        return hideCommonStr(idNum, 10, 6);
    }

    /**
     * 中国企业统一社会信用代码脱敏，隐藏第3-6位
     *
     * @param code 统一社会信用代码
     * @return 结果字符串
     */
    public static String hideUnifiedSocialCreditCode(String code) {
        return hideCommonStr(code, 2, 4);
    }

    /**
     * 使用星号进行脱敏，越过常见连接字符
     *
     * @param str        原字符串
     * @param beginIndex 开始序号
     * @param maskLength 遮罩长度
     * @return 结果字符串
     */
    public static String hideCommonStr(String str, int beginIndex, int maskLength) {
        return hideStr(str, '*', new char[]{' ', '-'}, beginIndex, maskLength);
    }

    /**
     * 字符串脱敏，使用遮罩字符替换原字符串中指定位置的字符，非遮罩字符不会被替换，非遮罩字符不影响遮罩长度的计算
     *
     * @param str        原字符串
     * @param maskChar   遮罩字符
     * @param unmaskChar 非遮罩字符
     * @param beginIndex 开始序号
     * @param maskLength 遮罩长度
     * @return 结果字符串
     */
    public static String hideStr(String str, char maskChar, char[] unmaskChar, int beginIndex, int maskLength) {
        if (beginIndex < 0) {
            throw new IllegalArgumentException("Desensitization param [beginIndex] can not be negative.");
        }
        if (maskLength <= 0) {
            throw new IllegalArgumentException("Desensitization param [maskLength] can not be less than or equal to zero.");
        }
        if (str == null || str.isEmpty() || beginIndex >= str.length()) {
            return str;
        }
        int strLength = str.length();
        for (int i = 0; i < Math.min(maskLength, strLength - beginIndex); i++) {
            boolean isUnmaskChar = false;
            if (unmaskChar != null) {
                for (char c : unmaskChar) {
                    if (str.charAt(beginIndex + i) == c) {
                        isUnmaskChar = true;
                        break;
                    }
                }
            }
            if (!isUnmaskChar) {
                str = str.substring(0, beginIndex + i) + maskChar + str.substring(beginIndex + i + 1);
            }
        }
        return str;
    }
}
