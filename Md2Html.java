package md2html;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class Md2Html {
    private static int lineIndex;
    private static int index;
    private static ArrayList<String> markdown;
    private static final Map<Character, String> htmlChars = Map.of(
            '>', "&gt;",
            '<', "&lt;",
            '&', "&amp;"
    );

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new FileWriter(args[1], StandardCharsets.UTF_8))) {
            markdown = new ArrayList<>();
            ArrayList<String> html = new ArrayList<>();
            while (true) {
                String buf = reader.readLine();
                if (buf != null) {
                    markdown.add(buf);
                } else {
                    break;
                }
            }
            lineIndex = 0;
            while (lineIndex < markdown.size()) {
                if (!isEmpty(lineIndex)) {
                    if (isHeading()) {
                        html.add(parseHeading());
                    } else {
                        html.add("<p>" + parseSequence("\n", true) + "</p>");
                    }
                }
                lineIndex++;
            }
            for (String s : html) {
                writer.write(s);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static String parseSequence(String end, boolean isNeedMarkUp) {
        StringBuilder sb = new StringBuilder();
        while (!isEmpty(lineIndex) && !isNow(end)) {
            if (isNeedMarkUp) {
                if (isNow("**")) {
                    sb.append(tryToParse("**", "strong", "<em></em>"));
                } else if (isNow("__")) {
                    sb.append(tryToParse("__", "strong", "<em></em>"));
                } else if (isNow("*")) {
                    sb.append(tryToParse("*", "em", "*"));
                } else if (isNow("_")) {
                    sb.append(tryToParse("_", "em", "_"));
                } else if (isNow("--")) {
                    sb.append(tryToParse("--", "s", "--"));
                } else if (isNow("`")) {
                    sb.append(tryToParse("`", "code", "`"));
                } else if (isNowImage()) {
                    isNow("![");
                    sb.append("<img alt='").append(parseSequence("](", false))
                            .append("' src='").append(parseSequence(")", false)).append("'>");
                } else {
                    if (lineIndex < markdown.size()) {
                        String currentLine = markdown.get(lineIndex);
                        if (index < currentLine.length()) {
                            char c = currentLine.charAt(index++);
                            if (c == '\\') {
                                c = currentLine.charAt(index++);
                            }
                            sb.append(htmlChars.getOrDefault(c, String.valueOf(c)));
                        } else {
                            sb.append('\n');
                            lineIndex++;
                            index = 0;
                        }
                    }
                }
            } else {
                sb.append(markdown.get(lineIndex).charAt(index));
                if (nextChar()) {
                    sb.append('\n');
                }
            }
        }
        if (sb.charAt(sb.length() - 1) == '\n' && isNeedMarkUp) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    private static boolean nextChar() {
        if (index >= markdown.get(lineIndex).length() - 1) {
            index = 0;
            lineIndex++;
            return true;
        } else {
            index++;
            return false;
        }
    }

    private static boolean isNowImage() {
        int in = index;
        int line = lineIndex;
        if (isNow("!") && isNow("[") && !isEmpty(lineIndex)) {
            while (!isNow("](") && !isEmpty(lineIndex)) {
                nextChar();
            }
            while (!isNow(")") && !isEmpty(lineIndex)) {
                nextChar();
            }
            if (!isEmpty(lineIndex) && markdown.get(lineIndex).charAt(index - 1) == ')') {
                index = in;
                lineIndex = line;
                return true;
            }
        }
        return false;
    }

    private static String tryToParse(String mark, String tag, String def) {
        if (hasNextInParagraph(index, mark)) {
            return "<" + tag + ">" + parseSequence(mark, true) + "</" + tag + ">";
        } else {
            return def;
        }
    }

    private static boolean isNow(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (i + index >= markdown.get(lineIndex).length() ||
                    s.charAt(i) != markdown.get(lineIndex).charAt(i + index)) {
                return false;
            }
        }
        index += s.length();
        return true;
    }

    private static boolean hasNextInParagraph(int in, String seq) {
        int line = lineIndex;
        while (!isEmpty(lineIndex)) {
            if (isNow("\\")) {
                index += 2;
            } else {
                if (isNow(seq)) {
                    lineIndex = line;
                    index = in;
                    return true;
                }
                index++;
            }
            if (index >= markdown.get(lineIndex).length()) {
                lineIndex++;
                index = 0;
            }
        }
        lineIndex = line;
        index = in;
        return false;
    }

    private static String parseHeading() {
        index = 0;
        StringBuilder sb = new StringBuilder();
        while (markdown.get(lineIndex).charAt(index) == '#') {
            index++;
        }
        int number = index;
        String data = parseSequence("\n", true).substring(1);
        sb.append("<h").append(number).append(">").append(data);
        sb.append("</h").append(number).append(">");
        return sb.toString();
    }

    private static boolean isEmpty(int lineIndex) {
        return lineIndex >= markdown.size() || markdown.get(lineIndex).length() == 0;
    }

    private static boolean isHeading() {
        index = 0;
        while (markdown.get(lineIndex).charAt(index) == '#') {
            index++;
        }
        if (index != 0 && Character.isWhitespace(markdown.get(lineIndex).charAt(index))) {
            return true;
        } else {
            index = 0;
            return false;
        }
    }
}