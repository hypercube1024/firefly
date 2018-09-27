package com.firefly.utils.image.component;

import com.firefly.utils.Assert;
import com.firefly.utils.CollectionUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractGraphicsComponent implements GraphicsComponent {

    protected String id;
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected List<GraphicsComponent> children = new ArrayList<>();
    protected GraphicsComponent parent;
    protected BufferedImage bufferedImage;

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public void addChild(GraphicsComponent component) {
        component.setParent(this);
        children.add(component);
    }

    @Override
    public void removeChild(String id) {
        children.removeIf(c -> Objects.equals(c.getId(), id));
    }

    @Override
    public void addChildren(List<GraphicsComponent> componentList) {
        componentList.forEach(c -> c.setParent(this));
        children.addAll(componentList);
    }

    @Override
    public List<GraphicsComponent> getChildren() {
        return children;
    }

    @Override
    public void setChildren(List<GraphicsComponent> children) {
        Assert.notNull(children);
        children.forEach(c -> c.setParent(this));
        this.children = children;
    }

    @Override
    public GraphicsComponent getParent() {
        return parent;
    }

    @Override
    public void setParent(GraphicsComponent parent) {
        this.parent = parent;
    }

    protected void drawChildren() {
        if (!CollectionUtils.isEmpty(children)) {
            Graphics2D g = bufferedImage.createGraphics();
            for (GraphicsComponent component : children) {
                BufferedImage img = component.draw();
                g.drawImage(img, component.getX(), component.getY(), img.getWidth(), img.getHeight(), null);
            }
            g.dispose();
        }
    }

    @Override
    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

}
