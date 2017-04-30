package cn.customs.h2020.entry;

import cn.customs.tools.service.AutoService;

public class Test {

	public static void main(String[] args) throws InterruptedException {
		AutoService service = new AutoService();
		service.setAccessorIdleSeconds(10);
		service.setAccessorName("cn.customs.h2020.entry.EntryService");
		service.setAccessorParams("");
		service.setAccessorThreadCounts(1);
		service.setProcessorIdleSeconds(1);
		service.setProcessorName("cn.customs.h2020.entry.EntryService");
		service.setProcessorParams("");
		service.setProcessorThreadCounts(3);
		
		service.start();
		
		Thread.sleep(2*1000);
		
		service.stop();
		
		Thread.sleep(2*1000);
	}

}
