package test.utils.lang;

import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.lang.CollectionTools;
import com.firefly.utils.lang.MultiReturnFunction;
import com.firefly.utils.lang.Pair;
import com.firefly.utils.lang.SingleReturnFunction;

public class TestCollectionTools {

	public static void main(String[] args) {
		new TestCollectionTools().transform();
	}

	@Test
	public void transform() {
		List<Foo> list = new ArrayList<Foo>();

		for (int i = 0; i < 5; i++) {
			Foo foo = new Foo();
			foo.id = i;
			foo.information = "test" + i;
			list.add(foo);
		}

		Map<Long, Foo> map = CollectionTools.transform(list,
				new MultiReturnFunction<Long, Foo, Foo>() {

					@Override
					public Pair<Long, Foo> apply(Foo input) {
						return new Pair<Long, Foo>(input.id, input);
					}
				});
		System.out.println(map);
		Assert.assertThat(map.get(1L).getInformation(), is("test1"));
		Assert.assertThat(map.get(2L).getInformation(), is("test2"));

		List<Bar> barList = CollectionTools.transform(list,
				new SingleReturnFunction<Bar, Foo>() {

					@Override
					public Bar apply(Foo input) {
						Bar bar = new Bar();
						bar.id = input.id;
						bar.title = input.information;
						return bar;
					}
				});

		for (int i = 0; i < 5; i++) {
			Assert.assertThat(barList.get(i).getTitle(), is("test" + i));
		}
	}

	public static class Bar {
		private long id;
		private String title;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		@Override
		public String toString() {
			return "Bar [id=" + id + ", title=" + title + "]";
		}

	}

	public static class Foo {
		private long id;
		private String information;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getInformation() {
			return information;
		}

		public void setInformation(String information) {
			this.information = information;
		}

		@Override
		public String toString() {
			return "Foo [id=" + id + ", information=" + information + "]";
		}

	}
}
