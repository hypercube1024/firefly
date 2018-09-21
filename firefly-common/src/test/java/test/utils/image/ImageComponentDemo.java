package test.utils.image;

import com.firefly.utils.image.component.FlowLayoutComponent;
import com.firefly.utils.image.component.ImageComponent;
import com.firefly.utils.image.component.OvalComponent;
import com.firefly.utils.image.component.TextComponent;
import com.firefly.utils.time.TimeUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author Pengtao Qiu
 */
public class ImageComponentDemo {

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        ImageComponent poster = new ImageComponent();
        poster.setSrc(ImageComponentDemo.class.getResource("/score_card.png"));

        OvalComponent avatarOval = new OvalComponent();
        avatarOval.setColor(Color.WHITE);
        avatarOval.setX(34);
        avatarOval.setY(34);
        avatarOval.setWidth(110);
        avatarOval.setHeight(110);
        poster.addChild(avatarOval);

        ImageComponent avatar = new ImageComponent();
        avatar.setSrc(new URL("http://thirdwx.qlogo.cn/mmopen/vi_32/DYAIOgq83eqpswuRZ6cKo7AMHm6mHCVCOjfeiaml69vAPwSsHP0jkfzNkKm7j1dsDgKj0XTmGt1nAsicpxp9GUpg/132"));
        avatar.setWidth(98);
        avatar.setHeight(98);
        avatar.setX(40);
        avatar.setY(40);
        avatar.setArc(180);
        poster.addChild(avatar);

        TextComponent title = new TextComponent();
        title.setText("世界原来广阔辽远");
        title.setFont(new Font("微软雅黑", Font.BOLD, 28));
        title.setColor(Color.WHITE);
        title.setX(157);
        title.setY(42);
        title.setPaddingWidth(10);
        title.setPaddingHeight(5);
        poster.addChild(title);

        TextComponent lesson = new TextComponent();
        lesson.setText("Lesson 1 - How much is that doggie in the window".substring(0, 34) + " ... ");
        lesson.setFont(new Font("微软雅黑", Font.PLAIN, 28));
        lesson.setColor(new Color(207, 140, 12));
        lesson.setX(157);
        lesson.setY(88);
        lesson.setPaddingWidth(10);
        lesson.setPaddingHeight(10);
        poster.addChild(lesson);

        TextComponent date = new TextComponent();
        date.setText(LocalDate.now().format(TimeUtils.DEFAULT_LOCAL_DATE));
        date.setFont(new Font("微软雅黑", Font.BOLD, 24));
        date.setColor(Color.WHITE);
        date.setX(555);
        date.setY(42);
        date.setPaddingHeight(5);
        poster.addChild(date);

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

        long end = System.currentTimeMillis();
        System.out.println("complete. " + (end - start));
    }
}
