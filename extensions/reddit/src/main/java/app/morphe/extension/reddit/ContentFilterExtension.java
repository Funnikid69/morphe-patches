package app.morphe.extension.reddit;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ContentFilterExtension {
    // Regex matching target keywords and rainbow/trans flag Unicode sequences
    private static final Pattern BLOCKED_TEXT = Pattern.compile(
        "(?i)\\b(lgbtq\\+?|transgender|trans|gay|lesbian|pride)\\b|\\uD83C\\uDFF3\\uFE0F?\\u200D\\uD83C\\uDF08|\\uD83C\\uDFF3\\uFE0F?\\u200D\\u26A7\\uFE0F?"
    );

    private static final List<String> BLOCKED_ASSET_TOKENS = Arrays.asList(
        "pride", "rainbow", "trans_flag", "lgbt", "progress_flag"
    );

    // Called via bytecode hook on TextView/Spannable rendering
    public static CharSequence filterText(CharSequence input) {
        if (input == null) return null;
        String raw = input.toString();
        if (BLOCKED_TEXT.matcher(raw).find()) {
            return BLOCKED_TEXT.matcher(raw).replaceAll("[filtered]");
        }
        return input;
    }

    // Called via OkHttp / Snoo avatar asset URL interceptor
    public static boolean shouldBlockAssetUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        for (String token : BLOCKED_ASSET_TOKENS) {
            if (lower.contains(token)) return true;
        }
        return false;
    }

    // Replaces flagged bitmaps (pfp, banners, icons, images) with a neutral gray image
    public static Bitmap replaceWithDefaultIfFlag(Bitmap source) {
        if (source == null || source.getWidth() < 16 || source.getHeight() < 16) return source;
        if (matchesHorizontalFlagBands(source)) {
            Bitmap fallback = Bitmap.createBitmap(source.getWidth(), source.getHeight(), Bitmap.Config.ARGB_8888);
            fallback.eraseColor(Color.DKGRAY);
            return fallback;
        }
        return source;
    }

    // Samples vertical center column for horizontal multi-band color stripes
    private static boolean matchesHorizontalFlagBands(Bitmap bmp) {
        int x = bmp.getWidth() / 2;
        int h = bmp.getHeight();
        int[] samples = new int[6];
        for (int i = 0; i < 6; i++) {
            samples[i] = bmp.getPixel(x, (int) ((i + 0.5) * (h / 6.0)));
        }
        // Check for classic 6-stripe rainbow progression (Red -> Orange -> Yellow -> Green -> Blue -> Violet)
        return isHueNear(samples[0], 0f) && isHueNear(samples[1], 30f)
            && isHueNear(samples[2], 60f) && isHueNear(samples[3], 120f)
            && isHueNear(samples[4], 220f) && isHueNear(samples[5], 280f);
    }

    private static boolean isHueNear(int color, float targetHue) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        if (hsv[1] < 0.45f || hsv[2] < 0.45f) return false;
        float diff = Math.abs(hsv[0] - targetHue);
        return Math.min(diff, 360f - diff) < 25f;
    }
}
