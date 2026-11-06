package com.lifestats.buff;

import java.util.regex.Pattern;

/** buffs.json 한 줄. match=사용한 아이템 이름에 들어 있는 글자, chat=발동 메시지 정규식(비우면 사용 안 함). */
public class BuffDef {
    public String id;
    public String name;
    public String match = "";
    public String chat = "";
    public int effectSeconds;
    public int cooldownSeconds;

    transient Pattern chatPattern;

    void compile() {
        if (chat != null && !chat.isBlank()) {
            try {
                chatPattern = Pattern.compile(chat);
            } catch (Exception e) {
                chatPattern = null;
            }
        }
    }
}
