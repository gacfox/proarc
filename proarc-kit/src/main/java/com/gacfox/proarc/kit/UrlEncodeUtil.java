package com.gacfox.proarc.kit;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * 与前端行为一致的URL编解码工具类
 */
public class UrlEncodeUtil {

    private static final String ENC = "UTF-8";

    /**
     * 判断一个URL是否经过URL编码
     *
     * @param s URL地址
     * @return 判断结果
     */
    public static boolean hasEncoded(String s) {
        try {
            String decodedUrl = decodeUriComponent(s);
            return !s.equals(decodedUrl);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 编码整个字符串
     *
     * @param s 输入字符串
     * @return 编码结果
     */
    public static String encodeUriComponent(String s) {
        try {
            String encodedUrl = URLEncoder.encode(s, ENC);
            encodedUrl = encodedUrl.replaceAll("\\+", "%20");
            return encodedUrl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 编码一个完整URL中的参数
     *
     * @param s 输入URL
     * @return 编码结果
     */
    public static String encodeUri(String s) {
        try {
            s = s.replaceAll(" ", "%20");
            s = s.replaceAll("\u00A0", "%C2%A0");
            URI uri = new URI(s);
            s = new URI(uri.getScheme(),
                    uri.getUserInfo(),
                    uri.getHost(),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment()).toASCIIString();
            return s;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解码字符串
     *
     * @param s 已经编码的字符串
     * @return 输出结果
     */
    public static String decodeUriComponent(String s) {
        try {
            s = s.replaceAll("\\+", "%2B");
            return URLDecoder.decode(s, ENC);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
