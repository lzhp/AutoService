package cn.customs.tools.service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import cn.customs.tools.service.util.LogHelper;

/**
 * @author lizhipeng
 * @version 1.0
 */
public class AutoService implements IAutoService {

	/**
	 * 缓冲队列
	 */
	BlockingQueue<Object> queues = null;
	private int queueSize = 10;

	/**
	 * 提供的锁id
	 */
	private String lockID = null;

	/**
	 * 等待时间
	 */
	private int accessorIdleSeconds = 2;
	private int processorIdleSeconds = 2;

	/**
	 * 启动的线程数量
	 */
	private int accessorThreadCounts = 1;
	private int processorThreadCounts = 5;

	/**
	 * 访问器和处理器参数
	 */
	private String accessorParams = null;
	private String processorParams = null;

	/**
	 * 访问器和处理器
	 */
	private String accessorName = null;
	private String processorName = null;

	/**
	 * 解锁器
	 */
	private String releaserName = null;

	public AutoService() {

	}

	Producer[] producer;
	Consumer[] consumer;

	Thread[] threadProducer;
	Thread[] threadConsumer;

	public void start() {
		init();

		LogHelper.info(Thread.currentThread().getName() + " service begin:" + this.toString());
		for (int i = 0; i < accessorThreadCounts; i++) {
			producer[i] = new Producer();
			producer[i].setAccessorIdleSeconds(accessorIdleSeconds);
			producer[i].setAccessorName(accessorName);
			producer[i].setAccessorParams(accessorParams);
			producer[i].setLockID(lockID);
			producer[i].setQueues(queues);
			producer[i].setStop(false);
			threadProducer[i] = new Thread(producer[i]);
			threadProducer[i].start();
		}

		for (int i = 0; i < processorThreadCounts; i++) {
			consumer[i] = new Consumer();
			consumer[i].setProcessorIdleSeconds(processorIdleSeconds);
			consumer[i].setProcessor(processorName);
			consumer[i].setProcessorParams(processorParams);
			consumer[i].setQueues(queues);
			consumer[i].setStop(false);
			threadConsumer[i] = new Thread(consumer[i]);
			threadConsumer[i].start();
		}

	}

	public void stop() {
		for (int i = 0; i < accessorThreadCounts; i++) {
			producer[i].setStop(true);
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < processorThreadCounts; i++) {
			consumer[i].setStop(true);
		}

		LogHelper.debug("begin shutdown");

		for (int i = 0; i < accessorThreadCounts; i++) {
			try {
				threadProducer[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		for (int i = 0; i < processorThreadCounts; i++) {
			try {
				threadConsumer[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		LogHelper.debug("shutdown ok!");
	}

	private void init() {

		queues = new LinkedBlockingQueue<Object>(10);

		producer = new Producer[accessorThreadCounts];
		consumer = new Consumer[processorThreadCounts];

		threadProducer = new Thread[accessorThreadCounts];
		threadConsumer = new Thread[processorThreadCounts];
	}

	/**
	 * 释放队列里没有处理的内容
	 */
	private void release() {

	}

	@Override
	public String toString() {
		return "AutoService [queueSize=" + queueSize + ", accessorIdleSeconds=" + accessorIdleSeconds
				+ ", processorIdleSeconds=" + processorIdleSeconds + ", accessorThreadCounts=" + accessorThreadCounts
				+ ", processorThreadCounts=" + processorThreadCounts + ", accessorParams=" + accessorParams
				+ ", processorParams=" + processorParams + ", accessorName=" + accessorName + ", processorName="
				+ processorName + ",lockID=" + lockID + "]";
	}

	public void setAccessorIdleSeconds(int accessorIdleSeconds) {
		this.accessorIdleSeconds = accessorIdleSeconds;
	}

	public void setProcessorIdleSeconds(int processorIdleSeconds) {
		this.processorIdleSeconds = processorIdleSeconds;
	}

	public void setAccessorThreadCounts(int accessorThreadCounts) {
		this.accessorThreadCounts = accessorThreadCounts;
	}

	public void setProcessorThreadCounts(int processorThreadCounts) {
		this.processorThreadCounts = processorThreadCounts;
	}

	public void setAccessorParams(String accessorParams) {
		this.accessorParams = accessorParams;
	}

	public void setProcessorParams(String processorParams) {
		this.processorParams = processorParams;
	}

	public void setAccessorName(String accessorName) {
		this.accessorName = accessorName;
	}

	public void setProcessorName(String processorName) {
		this.processorName = processorName;
	}

}

class Producer implements Runnable {

	private boolean stop = false;
	private int accessorIdleSeconds = 50;
	private String accessorName = null;
	private String accessorParams = null;
	private BlockingQueue<Object> queues = null;
	private String lockID = null;

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	@Override
	public void run() {
		IAccessor accessor = null;
		try {
			Class<?> c = Class.forName(accessorName);
			accessor = (IAccessor) c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LogHelper.error("创建类失败：" + accessorName, e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		LogHelper.debug(Thread.currentThread().getName() + "producer begin!");
		List<Object> workItems = null;
		while (!stop) {
			workItems = accessor.access(lockID, accessorParams);

			if (workItems != null) {
				for (Object o : workItems) {
					try {
						queues.put(o);
					} catch (InterruptedException e) {
						LogHelper.error("error when put to queue:" + o.toString(), e);
					}
				}
			} else {
				try {
					Thread.sleep(accessorIdleSeconds * 1000);
				} catch (InterruptedException e) {
					LogHelper.error("error when sleep", e);
				}
			}
		}
		LogHelper.debug(Thread.currentThread().getName() + "producer quit!");
	}

	public void setAccessorIdleSeconds(int accessorIdleSeconds) {
		this.accessorIdleSeconds = accessorIdleSeconds;
	}

	public void setAccessorName(String accessorName) {
		this.accessorName = accessorName;
	}

	public void setAccessorParams(String accessorParams) {
		this.accessorParams = accessorParams;
	}

	public void setQueues(BlockingQueue<Object> queues) {
		this.queues = queues;
	}

	public void setLockID(String lockID) {
		this.lockID = lockID;
	}

}

class Consumer implements Runnable {

	private BlockingQueue<Object> queues = null;
	private boolean stop = false;
	private String processorParams = null;
	private int processorIdleSeconds = 50;
	private String processorName = null;

	@Override
	public void run() {
		IProcessor process = null;
		try {
			Class<?> c = Class.forName(processorName);
			process = (IProcessor) c.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			LogHelper.error("创建类失败：" + processorName, e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		LogHelper.debug(Thread.currentThread().getName() + "consumer begin!");
		Object workItem = null;
		while (!stop) {
			try {
				workItem = queues.poll(processorIdleSeconds, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (workItem != null) {
				process.process(workItem, processorParams);
			}
		}
		LogHelper.debug(Thread.currentThread().getName() + "consumer quit!");
	}

	public void setProcessorIdleSeconds(int processorIdleSeconds) {
		this.processorIdleSeconds = processorIdleSeconds;
	}

	public void setProcessorParams(String processorParams) {
		this.processorParams = processorParams;
	}

	public void setProcessor(String processorName) {
		this.processorName = processorName;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public void setQueues(BlockingQueue<Object> queues) {
		this.queues = queues;
	}
}

class Unlock {

	private String releaserName = null;
	
	public void release(Object workItem, String params){
		IReleaser releaser = getReleaser();
		releaser.release(workItem, params); 
	}
	
	public void release(String lockID, String params){
		IReleaser releaser = getReleaser();
		releaser.release(lockID, params);		
	}

	private IReleaser getReleaser() {
		IReleaser releaser = null;
		try {
			Class<?> c = Class.forName(releaserName);
			releaser = (IReleaser) c.newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | ClassNotFoundException
				| SecurityException e) {
			LogHelper.error("create class failure：" + releaserName, e);
		}
		return releaser;
	}
}