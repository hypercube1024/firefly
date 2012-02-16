package com.firefly.utils.dom;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public interface Dom {

	/**
	 * 根据文件读取文档对象
	 * @Date 2011-3-3
	 * @param file
	 * @return 文档对象
	 */
	public abstract Document getDocument(String file);
	
	/**
	 * 取得根节点
	 * @param doc
	 * @return
	 */
	public abstract Element getRoot(Document doc);
	
	/**
	 * 取得所有子元素
	 * @param e
	 * @return
	 */
	public abstract List<Element> elements(Element e);
	
	/**
	 * 根据元素名取得子元素列表
	 * @param e
	 * @param name
	 * @return
	 */
	public abstract List<Element> elements(Element e, String name);
	
	/**
	 * 获取元素
	 * @param e
	 * @param name
	 * @return
	 */
	public abstract Element element(Element e, String name);
	
	/**
	 * 获取元素值
	 * @param valueEle
	 * @return
	 */
	public abstract String getTextValue(Element valueEle);
}
