package cn.lzhp.test;

import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Test;

import cn.lzhp.tools.service.AutoService;

public class TestAutoService {

	@Test
	public void test() throws InterruptedException {
		AutoService service = new AutoService();

		service.setServiceName("EntryService");
		service.setAccessorIdleSeconds(5);
		service.setAccessorName("cn.lzhp.test.EntryService");
		service.setAccessorParams("");
		service.setAccessorThreadCounts(2);

		service.setProcessorIdleSeconds(1);
		service.setProcessorName("cn.lzhp.test.EntryService");
		service.setProcessorParams("");
		service.setProcessorThreadCounts(4);

		service.setReleaserName("cn.lzhp.test.EntryService");
		service.setReleaserParams("");

		service.start();

		Thread.sleep(10 * 1000);
		//Awaitility.await().atLeast(10, TimeUnit.SECONDS);
		

		service.stop();
	}

}
