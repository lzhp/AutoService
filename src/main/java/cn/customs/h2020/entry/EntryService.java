package cn.customs.h2020.entry;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;

import cn.customs.tools.service.IAccessor;
import cn.customs.tools.service.IProcessor;
import cn.customs.tools.service.IReleaser;
import cn.customs.tools.service.util.LogHelper;

public class EntryService implements IAccessor, IProcessor, IReleaser {

	private static AtomicInteger count = new AtomicInteger();

	@Override
	public void process(Object workItem, String params) {
		LogHelper.debug(Thread.currentThread().getName()+ "开始消费：" + workItem);
	}

	@Override
	public List<Object> access(String lockID, String params) {
		LogHelper.debug(Thread.currentThread().getName()+ "开始生产：" + count);
		List<Object> result = Lists.newArrayList();
		for (int i = 0; i < 5; i++) {
			result.add(count.incrementAndGet());
		}
		LogHelper.debug(Thread.currentThread().getName()+ "生产完毕：" + count);
		return result;
	}

	@Override
	public void release(String lockID, String params) {
		// TODO Auto-generated method stub

	}

	@Override
	public void release(Object workItem, String params) {
		// TODO Auto-generated method stub

	}
}
