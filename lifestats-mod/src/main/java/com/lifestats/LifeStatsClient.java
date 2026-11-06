package com.lifestats;

import com.lifestats.buff.BuffManager;
import com.lifestats.data.ItemRegistry;
import com.lifestats.data.PriceManager;
import com.lifestats.data.StatsStore;
import com.lifestats.hud.BuffHud;
import com.lifestats.screen.StatsScreen;
import com.lifestats.track.ExpTracker;
import com.lifestats.track.InventoryTracker;
import com.lifestats.track.PlayTimer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifeStatsClient implements ClientModInitializer {
    public static final String MOD_ID = "lifestats";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openKey;
    private static KeyBinding hudKey;
    private static KeyBinding resetExpKey;
    private static int saveTimer;

    @Override
    public void onInitializeClient() {
        ItemRegistry.load();
        PriceManager.load();
        StatsStore.load();
        BuffManager.load();

        // 1.21.8 은 카테고리를 문자열로 받는다. (1.21.9 이상으로 올리면 KeyBinding.Category 로 바뀜)
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lifestats.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.lifestats"));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lifestats.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.lifestats"));
        resetExpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lifestats.reset_exp", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.lifestats"));

        ClientTickEvents.END_CLIENT_TICK.register(LifeStatsClient::onTick);
        HudRenderCallback.EVENT.register(BuffHud::render);

        // 아이템 사용으로 버프 발동 감지 (내 클라이언트 플레이어만)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (player == mc.player) {
                BuffManager.onItemUsed(InventoryTracker.nameOf(player.getStackInHand(hand)));
            }
            return ActionResult.PASS;
        });

        // 서버가 보내는 시스템/액션바 메시지로 버프 발동 감지
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> BuffManager.onMessage(message.getString()));

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveAll());
    }

    private static void onTick(MinecraftClient mc) {
        while (openKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new StatsScreen());
        }
        while (hudKey.wasPressed()) {
            BuffHud.toggle();
        }
        while (resetExpKey.wasPressed()) {
            ExpTracker.reset(mc);
        }

        InventoryTracker.onTick(mc);
        PlayTimer.onTick();

        if (++saveTimer >= 600) { // 약 30초마다 저장
            saveTimer = 0;
            saveAll();
        }
    }

    private static void saveAll() {
        StatsStore.save();
        PriceManager.save();
        BuffManager.saveState();
    }
}
