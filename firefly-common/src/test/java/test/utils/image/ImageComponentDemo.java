package test.utils.image;

import com.firefly.utils.image.component.FlowLayoutComponent;
import com.firefly.utils.image.component.ImageComponent;
import com.firefly.utils.image.component.TextComponent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Pengtao Qiu
 */
public class ImageComponentDemo {

    public static void main(String[] args) throws Exception {
        ImageComponent poster = new ImageComponent();
        poster.setSrc(new File("/Users/bjhl/Downloads/score_card.png"));

        TextComponent score = new TextComponent();
        score.setText("79");
        score.setFont(new Font("微软雅黑", Font.BOLD, 140));
        score.setColor(Color.WHITE);
        score.setPaddingWidth(20);
        score.setPaddingHeight(20);
        score.setX(269);
        score.setY(238);
        poster.addChild(score);

        FlowLayoutComponent layout = new FlowLayoutComponent();
        layout.setWidth(550);
        layout.setHeight(305);
        layout.setMarginWidth(5);
        layout.setMarginHeight(5);
        layout.setX(100);
        layout.setY(733);

        java.util.List<String> texts = Arrays.asList("sun", "apple", "orange", "mistake", "Tomorrow is always fresh",
                "project", "release", "outline", "A light weight or user mode thread");
        texts.sort(Comparator.comparingInt(String::length));
        Font font = new Font("微软雅黑", Font.PLAIN, 24);
        Color textColor = new Color(151, 75, 0);
        int padding = 20;
        texts.forEach(text -> {
            TextComponent word = new TextComponent();
            word.setText(text);
            word.setFont(font);
            word.setColor(textColor);
            word.setPaddingWidth(padding);
            word.setPaddingHeight(padding);
            word.setBackgroundColor(Color.WHITE);
            word.setArc(18);
            layout.addChild(word);
        });
        poster.addChild(layout);

        try (OutputStream out = new FileOutputStream("/Users/bjhl/Downloads/test_component.png")) {
            ImageIO.write(poster.draw(), "png", out);
        }
    }
}
