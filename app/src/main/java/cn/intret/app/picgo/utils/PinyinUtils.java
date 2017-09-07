package cn.intret.app.picgo.utils;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.HashMap;
import java.util.Map;


public class PinyinUtils {

    static Map<Character, String> characterStringMap = new HashMap<>();

    static {
        characterStringMap.put('1', "1");
        characterStringMap.put('2', "2abc");
        characterStringMap.put('3', "3def");
        characterStringMap.put('4', "4ghi");
        characterStringMap.put('5', "5jkl");
        characterStringMap.put('6', "6mno");
        characterStringMap.put('7', "7pqrs");
        characterStringMap.put('8', "8tuv");
        characterStringMap.put('9', "9wxyz");
    }

    String getT9KeyboardCharacters(char number) {
        return characterStringMap.get(number);
    }

    boolean isStringMatchT9Input(String target, String t9input) {
        if (target == null) {
            throw new IllegalArgumentException("target is null/empty");
        }

        if (t9input == null || "".equals(t9input)) {
            return false;
        }

        String targetl = target.toLowerCase();
        for (int i = 0; i < targetl.length(); i++) {
            char c = targetl.charAt(i);
            String[] pinyin = PinyinHelper.toHanyuPinyinStringArray(c);
        }

        return false;
    }
}
