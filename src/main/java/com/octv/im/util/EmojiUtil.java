package com.octv.im.util;

import com.github.binarywang.java.emoji.EmojiConverter;
import org.springframework.stereotype.Component;

@Component
public class EmojiUtil {

    private static EmojiConverter emojiConverter = EmojiConverter.getInstance();

    /**
     * 将emojiStr转为 带有表情的字符
     *
     * @param emojiStr
     * @return
     */
    public static String emojiConverterUnicodeStr(String emojiStr) {
        String result = emojiConverter.toUnicode(emojiStr);
        return result;
    }

    /**
     * 带有表情的字符串转换为编码
     *
     * @param str
     * @return
     */
    public static String emojiConverterToAlias(String str) {
        String result = emojiConverter.toAlias(str);
        return result;

    }

    public static void main(String[] args) {
        String emo = "\uD83D\uDE05\uD83E\uDD23\uD83D\uDE04\uD83D\uDE02";
        System.out.println(emojiConverterUnicodeStr(emo));
        System.out.println(emojiConverterToAlias(emojiConverterUnicodeStr(emo)));
    }
}
