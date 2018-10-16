package com.firefly.utils.image.component;

import com.firefly.utils.Assert;
import com.firefly.utils.image.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Pengtao Qiu
 */
public class TextComponent extends AbstractGraphicsComponent {

    protected String text;
    protected Font font;
    protected Color color;
    protected int paddingWidth;
    protected int paddingHeight;
    protected Color backgroundColor;
    protected int arc;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getPaddingWidth() {
        return paddingWidth;
    }

    public void setPaddingWidth(int paddingWidth) {
        this.paddingWidth = paddingWidth;
    }

    public int getPaddingHeight() {
        return paddingHeight;
    }

    public void setPaddingHeight(int paddingHeight) {
        this.paddingHeight = paddingHeight;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public int getArc() {
        return arc;
    }

    public void setArc(int arc) {
        this.arc = arc;
    }

    @Override
    public BufferedImage draw() {
        Assert.hasText(text, "The text must be not empty");
        Assert.notNull(font, "The text font must be not null");
        Assert.notNull(color, "The text color must be not null");
        bufferedImage = ImageUtils.drawTextInRect(text, font, color, paddingWidth, paddingHeight, backgroundColor, arc);
        width = bufferedImage.getWidth();
        height = bufferedImage.getHeight();
        drawChildren();
        return bufferedImage;
    }
}
