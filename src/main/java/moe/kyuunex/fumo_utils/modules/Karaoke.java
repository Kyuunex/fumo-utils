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
import java.util.Scanner;


public class Karaoke extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private int duration = 0;
    private boolean isReady = false;
    private Scanner scanner;
    private LyricsFile lyricsFile;

    private final Setting<String> lyricsFilePath = sgGeneral.add(new StringSetting.Builder()
        .name("lyrics-file-path")
        .description("Full path to the lyrics file")
        .defaultValue("")
        .build()
    );

    private final Setting<Boolean> isUntimed = sgGeneral.add(new BoolSetting.Builder()
        .name("is-untimed-file")
        .description("Enable for a text file with lyrics each line, with no timestamping. Disable for .lrc")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> timeBetweenLines = sgGeneral.add(new IntSetting.Builder()
        .name("time-between-lines")
        .description("Time between lines (in ticks) (for untimed only!)")
        .defaultValue(74)
        .build()
    );

    private final Setting<Boolean> simulate = sgGeneral.add(new BoolSetting.Builder()
        .name("simulate")
        .description("Enable to send client side only for preview, disable to send messages to the server")
        .defaultValue(false)
        .build()
    );

    public Karaoke() {
        super(FumoUtils.CATEGORY, "karaoke", "Sing along with your friends on the server.");
    }

    @Override
    public void onActivate() {
        try {
            scanner = new Scanner(new File(lyricsFilePath.get()));
            if(!isUntimed.get()) lyricsFile = new LyricsFile(scanner);
            isReady = true;
            info("Karaoke started!");
        } catch (FileNotFoundException e) {
            toggle();
            isReady = false;
        }
    }

    @Override
    public void onDeactivate() {
        scanner.close();
        isReady = false;
        duration = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if(!isReady) return;

        if(isUntimed.get()) {
            if(duration < timeBetweenLines.get()) {
                duration += 1;
                return;
            }
            duration = 0;
            if(scanner.hasNextLine()) send(scanner.nextLine());
        } else {
            if(duration > lyricsFile.lastLyric) {
                toggle();
                return;
            }
            try {
                String lyric = lyricsFile.lyrics.get(duration);
                if(lyric != null) send(lyric);
            }
            catch (IndexOutOfBoundsException ignored){}
            duration += 1;
        }
    }

    private void send(String text){
        if(simulate.get())
            info(text);
        else
            ChatUtils.sendPlayerMsg(text);
    }
}
