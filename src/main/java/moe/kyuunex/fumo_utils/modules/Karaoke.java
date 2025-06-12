package moe.kyuunex.fumo_utils.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import moe.kyuunex.fumo_utils.FumoUtils;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import moe.kyuunex.fumo_utils.models.LyricsFile;

import java.io.File;
import java.io.FileNotFoundException;


public class Karaoke extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();

    private final Setting<String> lyricsFilePath = sgGeneral.add(new StringSetting.Builder()
        .name("lyrics-file-path")
        .description("Full path to the .lrc file. Use `syrics` from pypi to get the spotify lyrics.")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> simulate = sgGeneral.add(new BoolSetting.Builder()
        .name("simulate")
        .description("Enable to send client side only for preview, disable to send messages to the server")
        .defaultValue(false)
        .build()
    );

    private final Setting<TextColor> textColor = sgGeneral.add(new EnumSetting.Builder<TextColor>()
        .name("text-color")
        .description("Green text? or Blue text? or none?")
        .defaultValue(TextColor.Default)
        .build()
    );

    public final Setting<Boolean> convertToCaps = sgGeneral.add(new BoolSetting.Builder()
        .name("caps")
        .description("Convert the string to caps")
        .defaultValue(false)
        .build()
    );

    public Karaoke() {
        super(FumoUtils.CATEGORY, "karaoke", "Sing along with your friends on the server.");
    }

    private int duration = 0;
    private boolean isReady = false;
    private LyricsFile lyricsFile;

    @Override
    public void onActivate() {
        try {
            lyricsFile = new LyricsFile(new File(lyricsFilePath.get()));
            isReady = true;
            info("Karaoke started!");
        } catch (FileNotFoundException e) {
            info("File not found! Disabling!");
            isReady = false;
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
        isReady = false;
        duration = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isReady) return;

        if (duration > lyricsFile.getMaxDuration()) {
            toggle();
            return;
        }
        String lyric = lyricsFile.getAtTick(duration);
        if (lyric != null) send(lyric);
        duration += 1;
    }

    private void send(String text){
        if (convertToCaps.get())
            text = text.toUpperCase();
        if (simulate.get())
            info(decorate(text));
        else
            ChatUtils.sendPlayerMsg(decorate(text));
    }

    private String decorate(String text){
        if (textColor.get() == TextColor.Green)
            return "> %s".formatted(text);
        else if (textColor.get() == TextColor.Blue)
            return "`%s".formatted(text);
        else
            return text;
    }

    private enum TextColor {
        Default,
        Green,
        Blue
    }
}
