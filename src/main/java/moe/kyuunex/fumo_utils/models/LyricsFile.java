package moe.kyuunex.fumo_utils.models;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsFile {
    Pattern pattern = Pattern.compile("^\\[(\\d{2}):(\\d{2}\\.\\d{2})\\]\\s*(.*)$");
    private int lastLyric;

    private final Map<Integer, String> lyrics = new HashMap<>(64);

    public LyricsFile(File file) throws FileNotFoundException {
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String data = scanner.nextLine();
            ParseLine(data);
        }
        scanner.close();
    }

    private void ParseLine(String line){
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            int minutes = Integer.parseInt(matcher.group(1));
            double seconds = Double.parseDouble(matcher.group(2));
            String lyric = matcher.group(3).trim();

            double timestamp = (minutes * 60 + seconds) * 20;

            lyrics.put((int)timestamp, lyric);
            lastLyric = (int)timestamp;
        }
    }

    public String getAtTick(int tick){
        return lyrics.get(tick);
    }

    public int getMaxDuration(){
        return lastLyric;
    }
}
