package cn.lzhp.test;

import cn.lzhp.tools.service.AutoService;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		AutoService service = new AutoService();
		service.setAccessorIdleSeconds(5);
		service.setAccessorName("cn.lzhp.test.EntryService");
		service.setAccessorParams("");
		service.setAccessorThreadCounts(1);
		
		service.setProcessorIdleSeconds(1);
		service.setProcessorName("cn.lzhp.test.EntryService");
		service.setProcessorParams("");
		service.setProcessorThreadCounts(4);
		
		service.setReleaserName("cn.lzhp.test.EntryService");
		service.setReleaserParams("");
		
		service.start();
		
		Thread.sleep(2*1000);
		
		service.stop();
		
		System.exit(0);
	}

}
