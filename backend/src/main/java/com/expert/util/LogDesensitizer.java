package com.expert.util;

/**
 * 日志脱敏工具
 * 防止敏感信息泄露到日志
 */
public class LogDesensitizer {

    /**
     * 脱敏手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 脱敏身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return "***";
        }
        return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 脱敏姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return "***";
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /**
     * 脱敏地址
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() < 10) {
            return "***";
        }
        return address.substring(0, 6) + "******";
    }

    /**
     * 脱敏病历内容
     */
    public static String maskMedicalRecord(String content) {
        if (content == null || content.length() < 20) {
            return "***";
        }
        return content.substring(0, 10) + "......" + content.substring(content.length() - 10);
    }

    /**
     * 脱敏密码（完全隐藏）
     */
    public static String maskPassword(String password) {
        return "******";
    }

    /**
     * 脱敏Token（只显示前10位）
     */
    public static String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 10) + "......";
    }

    /**
     * 脱敏银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return "***";
        }
        return "**** **** **** " + bankCard.substring(bankCard.length() - 4);
    }
}
