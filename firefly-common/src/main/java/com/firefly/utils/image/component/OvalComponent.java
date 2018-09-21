package com.firefly.utils.image.component;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Pengtao Qiu
 */
public class OvalComponent extends AbstractGraphicsComponent {

    protected Color color;

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public BufferedImage draw() {
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();
        g.setColor(color);
        g.fillOval(0, 0, width, height);
        g.dispose();
        drawChildren();
        return bufferedImage;
    }
}
