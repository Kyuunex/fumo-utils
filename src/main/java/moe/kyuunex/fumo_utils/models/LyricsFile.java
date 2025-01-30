package moe.kyuunex.fumo_utils.models;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricsFile {
    Pattern pattern = Pattern.compile("^\\[(\\d{2}):(\\d{2}\\.\\d{2})\\]\\s*(.*)$");
    public int lastLyric;

    public Dictionary<Integer, String> lyrics = new Hashtable<>();

    public LyricsFile(Scanner object){
        while (object.hasNextLine()) {
            String data = object.nextLine();
            ParseLine(data);
        }
    }

    void ParseLine(String line){
        Matcher matcher = pattern.matcher(line);

        if (matcher.matches()) {
            int minutes = Integer.parseInt(matcher.group(1));
            double seconds = Double.parseDouble(matcher.group(2));
            String lyric = matcher.group(3).trim();

            // Calculate the total timestamp in seconds
            double timestamp = (minutes * 60 + seconds) * 20;

            // Add the parsed lyric to the list=
            lyrics.put((int)timestamp, lyric);
            lastLyric = (int)timestamp;
        }
    }
}
