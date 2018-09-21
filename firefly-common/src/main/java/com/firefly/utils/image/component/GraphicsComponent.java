package com.firefly.utils.image.component;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Pengtao Qiu
 */
public interface GraphicsComponent {

    String getId();

    void setId(String id);

    int getX();

    void setX(int x);

    int getY();

    void setY(int y);

    int getWidth();

    void setWidth(int width);

    int getHeight();

    void setHeight(int height);

    void addChild(GraphicsComponent component);

    void removeChild(String id);

    void addChildren(List<GraphicsComponent> componentList);

    void setParent(GraphicsComponent parent);

    GraphicsComponent getParent();

    void setChildren(List<GraphicsComponent> children);

    List<GraphicsComponent> getChildren();

    BufferedImage draw();

    BufferedImage getBufferedImage();

}
