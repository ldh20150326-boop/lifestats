package com.lifestats.data;

import com.lifestats.LifeStatsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/lifestats 폴더의 파일 읽기/쓰기. 파일이 없으면 모드에 들어 있는 기본값을 복사해 준다. */
public final class ConfigFiles {
    private ConfigFiles() {
    }

    public static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("lifestats");
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            LifeStatsClient.LOG.warn("설정 폴더를 만들 수 없습니다: {}", p, e);
        }
        return p;
    }

    public static Path file(String name) {
        return dir().resolve(name);
    }

    /** 사용자 파일이 있으면 그것을, 없으면 기본값을 복사한 뒤 읽는다. */
    public static String readWithDefault(String name) throws IOException {
        Path f = file(name);
        if (!Files.exists(f)) {
            try (InputStream in = ConfigFiles.class.getResourceAsStream("/assets/lifestats/defaults/" + name)) {
                if (in == null) throw new IOException("기본 파일이 없습니다: " + name);
                Files.write(f, in.readAllBytes());
            }
        }
        return Files.readString(f, StandardCharsets.UTF_8);
    }

    public static String readIfExists(String name) throws IOException {
        Path f = file(name);
        return Files.exists(f) ? Files.readString(f, StandardCharsets.UTF_8) : null;
    }

    public static void write(String name, String content) {
        try {
            Files.writeString(file(name), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LifeStatsClient.LOG.warn("저장 실패: {}", name, e);
        }
    }
}
