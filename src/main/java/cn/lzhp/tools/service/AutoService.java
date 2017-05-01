package cn.lzhp.tools.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import cn.lzhp.tools.service.util.LogHelper;
import cn.lzhp.tools.service.util.ReflectHelper;

/**
 * @author lizhipeng
 * @version 1.0
 */
public class AutoService {

	/**
	 * 缓冲队列
	 */
	private BlockingQueue<Object> queues = null;
	/**
	 * 提供的锁id
	 */
	private String lockID = "";

	/**
	 * 缓冲队列的长度
	 */
	private int queueSize = 10;

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
	 * 解锁，清理
	 */
	private String releaserName = "";
	private String releaserParams = "";
	private Releaser releaser = new Releaser();

	public AutoService() {

	}

	Producer[] producer;
	Consumer[] consumer;

	Thread[] threadProducer;
	Thread[] threadConsumer;

	public void start() {

		try {
			LogHelper.info(" service init params...");

			init();
			LogHelper.info(" service init params ok:" + this.toString());

			// 先将以前的解锁
			releaser.releaseAll(lockID, releaserParams);

			for (int i = 0; i < accessorThreadCounts; i++) {
				producer[i] = new Producer();
				producer[i].setStop(false);
				threadProducer[i] = new Thread(producer[i]);
				threadProducer[i].start();
			}
			LogHelper.info(" Producer init ok:[" + accessorThreadCounts + "] threads");

			for (int i = 0; i < processorThreadCounts; i++) {
				consumer[i] = new Consumer();
				consumer[i].setStop(false);
				threadConsumer[i] = new Thread(consumer[i]);
				threadConsumer[i].start();
			}
			LogHelper.info(" Consumer init ok:[" + processorThreadCounts + "] threads");
		} catch (Throwable t) {
			LogHelper.error("fetal error when startup:" + this.toString(), t);
		}
	}

	public void stop() {

		try {
			LogHelper.info(" begin to shutdown...");

			// 先停生产者
			for (int i = 0; i < accessorThreadCounts; i++) {
				producer[i].setStop(true);
			}

			LogHelper.info(" wait a moment for consumer..");
			Thread.sleep(1000);

			for (int i = 0; i < processorThreadCounts; i++) {
				consumer[i].setStop(true);
			}

			LogHelper.debug("wait all threads shutdown...");

			for (int i = 0; i < accessorThreadCounts; i++) {
				threadProducer[i].join();
			}
			LogHelper.debug("producer shutdown ok!");

			for (int i = 0; i < processorThreadCounts; i++) {
				threadConsumer[i].join();
			}
			LogHelper.debug("consumer shutdown ok!");

			release();
			LogHelper.debug("shutdown ok!");

		} catch (Throwable t) {
			LogHelper.error("fetal error when shutdown:" + this.toString(), t);
		}
	}

	private void init() {

		// 必须初始化的检查
		Preconditions.checkArgument(!Strings.isNullOrEmpty(accessorName), "accessorName is empty");
		Preconditions.checkArgument(accessorThreadCounts > 0,
				"accessorThreadCounts must bigger than 0, now is [" + accessorThreadCounts + "]");

		// 暂时用机器名地址做锁
		try {
			lockID = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		// 如果没有配置处理器，那就不做
		if (Strings.isNullOrEmpty(processorName)) {
			processorThreadCounts = 0;
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
				releaser.release(workItem, releaserParams);
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

			LogHelper.debug("producer begin!");
			List<Object> workItems = null;
			while (!stop) {
				workItems = accessor.access(lockID, accessorParams);

				if (workItems != null) {
					for (Object o : workItems) {
						try {
							// release if can't add in
							if (!queues.offer(o, accessorIdleSeconds, TimeUnit.SECONDS)) {
								releaser.release(o, releaserParams);
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
			LogHelper.debug("producer quit!");
		}
	}

	class Consumer implements Runnable {
		private boolean stop = false;

		@Override
		public void run() {
			IProcessor process = (IProcessor) ReflectHelper.getClassInstance(processorName);

			LogHelper.debug("consumer begin!");
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
			LogHelper.info("consumer quit!");
		}

		public void setStop(boolean stop) {
			this.stop = stop;
		}

	}

	class Releaser {

		private IReleaser releaser = null;

		public void release(Object workItem, String params) {
			if (releaser != null) {
				releaser.release(workItem, params);
			}
		}

		public void releaseAll(String lockID, String params) {
			if (releaser != null) {
				releaser.releaseAll(lockID, params);
			}
		}

		public Releaser() {
			if (Strings.isNullOrEmpty(releaserName)) {
				return;
			}

			if (releaser == null) {
				releaser = (IReleaser) ReflectHelper.getClassInstance(releaserName);
			}
		}
	}

}
