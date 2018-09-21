package test.utils.image;

import com.firefly.utils.image.ImageUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Pengtao Qiu
 */
public class ImageDemo {

    public static void main(String[] args) throws Exception {
        int width = 550;
        int height = 305;
        int padding = 20;
        java.util.List<String> texts = Arrays.asList("sun", "apple", "orange", "mistake", "Tomorrow is always fresh",
                "project", "release", "outline", "A light weight or user mode thread");
        texts.sort(Comparator.comparingInt(String::length));

        Font font = new Font("微软雅黑", Font.PLAIN, 24);
        Color textColor = new Color(151, 75, 0);

        BufferedImage wordCard = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int x = 0;
        int y = 0;
        int margin = 5;
        for (String text : texts) {
            BufferedImage fontImage = ImageUtils.drawTextInRect(text, font, textColor, padding, padding, Color.WHITE, 18);
            System.out.println(fontImage.getWidth() + ", " + fontImage.getHeight());

            int w = x + fontImage.getWidth() + margin * 2;
            if (w > width) {
                x = 0;
                y += fontImage.getHeight() + margin * 2;
                ImageUtils.mergeImage(wordCard, fontImage, x, y);
                x += fontImage.getWidth() + margin * 2;
            } else {
                ImageUtils.mergeImage(wordCard, fontImage, x, y);
                x += fontImage.getWidth() + margin * 2;
            }
        }

        Font scoreFont = new Font("微软雅黑", Font.BOLD, 140);
        BufferedImage score = ImageUtils.drawTextInRect("79", scoreFont, Color.WHITE, 20, 20, null, 0);

        BufferedImage poster = ImageIO.read(new File("/Users/bjhl/Downloads/score_card.png"));
        ImageUtils.mergeImage(poster, wordCard, 100, 733);
        ImageUtils.mergeImage(poster, score, 269, 238);

        try (OutputStream out = new FileOutputStream("/Users/bjhl/Downloads/test.png")) {
            ImageIO.write(poster, "png", out);
        }
    }
}
