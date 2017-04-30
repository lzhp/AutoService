package cn.customs.tools.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Strings;

import cn.customs.tools.service.util.LogHelper;
import cn.customs.tools.service.util.ReflectHelper;

/**
 * @author lizhipeng
 * @version 1.0
 */
public class AutoService {

	/**
	 * 缓冲队列
	 */
	BlockingQueue<Object> queues = null;
	private int queueSize = 10;

	/**
	 * 提供的锁id
	 */
	private String lockID = "";

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
	private String accessorParams = "";
	private String processorParams = "";

	/**
	 * 访问器和处理器
	 */
	private String accessorName = "";
	private String processorName = "";

	/**
	 * 解锁器
	 */
	private String releaserName = "";
	private String releaserParams = "";
	private Unlock unlock = new Unlock();

	public AutoService() {

	}

	Producer[] producer;
	Consumer[] consumer;

	Thread[] threadProducer;
	Thread[] threadConsumer;

	public void start() {
		init();
		LogHelper.info(Thread.currentThread().getName() + " service begin:" + this.toString());

		LogHelper.info(Thread.currentThread().getName() + " unlock now: lockID=[" + lockID + "];releaserParams=["
				+ releaserParams + "]");
		
		// 先将以前的解锁
		unlock.release(lockID, releaserParams);

		for (int i = 0; i < accessorThreadCounts; i++) {
			producer[i] = new Producer();
			producer[i].setStop(false);
			threadProducer[i] = new Thread(producer[i]);
			threadProducer[i].start();
		}

		for (int i = 0; i < processorThreadCounts; i++) {
			consumer[i] = new Consumer();
			consumer[i].setStop(false);
			threadConsumer[i] = new Thread(consumer[i]);
			threadConsumer[i].start();
		}
	}

	public void stop() {
		
		//先停生产者
		for (int i = 0; i < accessorThreadCounts; i++) {
			producer[i].setStop(true);
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			LogHelper.error("Sleep error", e);
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
		LogHelper.debug("producer shutdown ok!");
		for (int i = 0; i < processorThreadCounts; i++) {
			try {
				threadConsumer[i].join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		LogHelper.debug("consumer shutdown ok!");

		release();
		LogHelper.debug("shutdown ok!");
	}

	private void init() {

		// 暂时用机器名地址做锁
		try {
			lockID = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		queues = new LinkedBlockingQueue<Object>(queueSize);

		producer = new Producer[accessorThreadCounts];
		consumer = new Consumer[processorThreadCounts];

		threadProducer = new Thread[accessorThreadCounts];
		threadConsumer = new Thread[processorThreadCounts];
	}

	/**
	 * 释放队列里没有处理的内容
	 */
	private void release() {
		Object workItem = null;
		while (queues.size() > 0) {
			try {
				workItem = queues.poll(processorIdleSeconds, TimeUnit.SECONDS);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (workItem != null) {
				unlock.release(workItem, releaserParams);
			}
		}
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

	public void setQueueSize(int queueSize) {
		this.queueSize = queueSize;
	}

	public void setReleaserName(String releaserName) {
		this.releaserName = releaserName;
	}

	public void setReleaserParams(String releaserParams) {
		this.releaserParams = releaserParams;
	}

	class Producer implements Runnable {

		private boolean stop = false;

		public void setStop(boolean stop) {
			this.stop = stop;
		}

		@Override
		public void run() {
			IAccessor accessor = (IAccessor) ReflectHelper.getClassInstance(accessorName);

			LogHelper.debug(Thread.currentThread().getName() + "producer begin!");
			List<Object> workItems = null;
			while (!stop) {
				workItems = accessor.access(lockID, accessorParams);

				if (workItems != null) {
					for (Object o : workItems) {
						try {
							// release if can't add in
							if (!queues.offer(o, accessorIdleSeconds, TimeUnit.SECONDS)) {
								unlock.release(o, releaserParams);
							}
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
	}

	class Consumer implements Runnable {
		private boolean stop = false;

		@Override
		public void run() {
			IProcessor process = (IProcessor) ReflectHelper.getClassInstance(processorName);

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

		public void setStop(boolean stop) {
			this.stop = stop;
		}

	}

	class Unlock {

		public void release(Object workItem, String params) {
			IReleaser releaser = getReleaser();
			if (releaser!=null){
			releaser.release(workItem, params);
			}
		}

		public void release(String lockID, String params) {
			IReleaser releaser = getReleaser();
			if (releaser!=null){
				releaser.release(lockID, params);
			}
		}

		private IReleaser getReleaser() {
			if (Strings.isNullOrEmpty(releaserName)){
				return null;
			}
			IReleaser releaser = (IReleaser) ReflectHelper.getClassInstance(releaserName);
			return releaser;
		}
	}

}
