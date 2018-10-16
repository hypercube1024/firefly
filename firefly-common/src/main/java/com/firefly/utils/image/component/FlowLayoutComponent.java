package com.firefly.utils.image.component;

import com.firefly.utils.CollectionUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author Pengtao Qiu
 */
public class FlowLayoutComponent extends AbstractGraphicsComponent {

    protected int marginWidth;
    protected int marginHeight;

    public int getMarginWidth() {
        return marginWidth;
    }

    public void setMarginWidth(int marginWidth) {
        this.marginWidth = marginWidth;
    }

    public int getMarginHeight() {
        return marginHeight;
    }

    public void setMarginHeight(int marginHeight) {
        this.marginHeight = marginHeight;
    }

    @Override
    public BufferedImage draw() {
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        if (!CollectionUtils.isEmpty(children)) {
            Graphics2D g = bufferedImage.createGraphics();
            int x = 0;
            int y = 0;
            for (GraphicsComponent component : children) {
                BufferedImage img = component.draw();
                int cw = img.getWidth() + marginWidth * 2;
                int ch = img.getHeight() + marginHeight * 2;
                int w = x + cw;
                if (w > width) {
                    x = 0;
                    y += ch;
                    g.drawImage(img, x, y, img.getWidth(), img.getHeight(), null);
                    x += cw;
                } else {
                    g.drawImage(img, x, y, img.getWidth(), img.getHeight(), null);
                    x += cw;
                }
            }
            g.dispose();
        }
        return bufferedImage;
    }
}
