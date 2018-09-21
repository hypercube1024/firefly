package com.firefly.utils.image;

import com.firefly.utils.Assert;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * @author Pengtao Qiu
 */
abstract public class ImageUtils {

    public static BufferedImage resize(File file, int width, int height) {
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return resize(image, width, height);
    }

    public static BufferedImage resize(BufferedImage image, int width, int height) {
        Assert.notNull(image, "The image must be not null");
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = tag.getGraphics();
        g.drawImage(scaledImage, 0, 0, null); // 绘制缩小后的图
        g.dispose();
        return tag;
    }

    public static void mergeImage(BufferedImage backgroundImage, BufferedImage frontImage, int x, int y) {
        Graphics2D graph = backgroundImage.createGraphics();
        graph.drawImage(frontImage, x, y, frontImage.getWidth(), frontImage.getHeight(), null);
        graph.dispose();
    }

    public static BufferedImage makeRoundedCornerImg(BufferedImage image, int arc) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

        g2.setComposite(AlphaComposite.SrcAtop);
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return output;
    }

    public static TextLayout getTextLayout(String text, Font font) {
        // create temporary text image and get the text layout
        final int w = font.getSize() * text.length() + 20;
        final int h = font.getSize() + 20;
        BufferedImage buffImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffImg.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setFont(font);
        FontRenderContext ctx = g.getFontRenderContext();
        TextLayout layout = new TextLayout(text, font, ctx);
        g.dispose();
        return layout;
    }

    public static TextLayout getFontMaxHeight(Font font) {
        return getTextLayout("lg拥抱", font);
    }

    public static BufferedImage drawTextInRect(String text, Font font, Color textColor,
                                               int paddingWidth, int paddingHeight,
                                               Color backgroundColor, int arc) {
        TextLayout layout = getTextLayout(text, font);
        Rectangle2D bounds = layout.getBounds();
        TextLayout maxHeightLayout = getFontMaxHeight(font);

        int textWidth = (int) Math.round(bounds.getWidth());
        int textHeight = (int) Math.round(maxHeightLayout.getBounds().getHeight());
        int width = textWidth + paddingWidth * 2;
        int height = textHeight + paddingHeight * 2;
        BufferedImage buffImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = buffImg.createGraphics();
        g.setComposite(AlphaComposite.Src);

        if (backgroundColor != null) {
            // draw background round rect
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(backgroundColor);
            g.fillRoundRect(0, 0, width, height, arc, arc);
            g.drawImage(buffImg, 0, 0, null);
        }

        // draw text
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.setFont(font);
        g.setColor(textColor);
        g.drawString(text, width / 2 - textWidth / 2, height / 2 + textHeight / 2);
        g.dispose();
        return buffImg;
    }
}
