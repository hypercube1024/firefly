package test.utils.log;

import com.firefly.utils.collection.TreeTrie;
import com.firefly.utils.collection.Trie;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogConfigParser;
import com.firefly.utils.log.PropertiesLogConfigParser;
import com.firefly.utils.log.XmlLogConfigParser;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.is;

public class LogParserTest {

	@Test
	public void test() {
		Trie<Log> xmlLogTree = new TreeTrie<>();
		Trie<Log> propertiesLogTree = new TreeTrie<>();

		LogConfigParser parser = new XmlLogConfigParser();
		boolean success = parser.parse((fileLog) -> {
			xmlLogTree.put(fileLog.getName(), fileLog);
		});
		Assert.assertThat(success, is(true));

		parser = new PropertiesLogConfigParser();
		success = parser.parse((fileLog) -> {
			propertiesLogTree.put(fileLog.getName(), fileLog);
		});
		Assert.assertThat(success, is(true));

		for (String name : xmlLogTree.keySet()) {
			Log xml = xmlLogTree.get(name);
			Log p = propertiesLogTree.get(name);
			if (p != null) {
				Assert.assertThat(xml, is(p));
			}
		}
	}
}
