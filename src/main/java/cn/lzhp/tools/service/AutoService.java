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
	 * 服务名
	 */
	private String serviceName = "";

	/**
	 * 缓冲队列
	 */
	private BlockingQueue<Object> queues = null;
	private int queueSize = 10;

	/**
	 * 提供的锁id
	 */
	private String lockID = "";

	/**
	 * 访问器
	 */
	private int accessorIdleSeconds = 2;
	private int accessorThreadCounts = 1;
	private String accessorParams = "";
	private String accessorName = "";

	/**
	 * 处理器
	 */
	private int processorIdleSeconds = 2;
	private int processorThreadCounts = 5;
	private String processorParams = "";
	private String processorName = "";

	/**
	 * 解锁，清理
	 */
	private String releaserName = "";
	private String releaserParams = "";
	private Releaser releaser = null;

	public AutoService() {
		//do nothing
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
			tryToSleep(2);

			for (int i = 0; i < processorThreadCounts; i++) {
				consumer[i].setStop(true);
			}

			LogHelper.debug("wait all threads shutdown...");

			for (int i = 0; i < accessorThreadCounts; i++) {
				threadProducer[i].join();
			}
			LogHelper.info("producer shutdown ok!");

			for (int i = 0; i < processorThreadCounts; i++) {
				threadConsumer[i].join();
			}
			LogHelper.info("consumer shutdown ok!");

			releaseItemsInQueue();
			LogHelper.info("shutdown ok!");

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
		queues = new LinkedBlockingQueue<>(queueSize);

		producer = new Producer[accessorThreadCounts];
		consumer = new Consumer[processorThreadCounts];

		threadProducer = new Thread[accessorThreadCounts];
		threadConsumer = new Thread[processorThreadCounts];

		releaser = new Releaser();
	}

	/**
	 * 释放队列里没有处理的内容
	 */
	private void releaseItemsInQueue() {
		Object workItem = null;
		while (!queues.isEmpty()) {
			try {
				workItem = queues.poll(processorIdleSeconds, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// 没取到，继续取
				LogHelper.debug("Can't poll from queue");
			}
			if (workItem != null) {
				releaser.release(workItem, releaserParams);
			}
		}
	}

	@Override
	public String toString() {
		return "AutoService [serviceName=" + serviceName + ",queueSize=" + queueSize + ", accessorIdleSeconds="
				+ accessorIdleSeconds + ", processorIdleSeconds=" + processorIdleSeconds + ", accessorThreadCounts="
				+ accessorThreadCounts + ", processorThreadCounts=" + processorThreadCounts + ", accessorParams="
				+ accessorParams + ", processorParams=" + processorParams + ", accessorName=" + accessorName
				+ ", processorName=" + processorName + ",lockID=" + lockID + "]";
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
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

	private void attemptToRelease(Object workItem) {
		try {
			releaser.release(workItem, releaserParams);
		} catch (Throwable t) {
			// 出错不管，吃掉错误
			LogHelper.debug("error in attemptToRelease", t);
		}
	}

	private void tryToSleep(int seconds) {
		try {
			Thread.sleep(seconds * 1000L);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LogHelper.error("error when sleep", e);
		}
	}

	class Producer implements Runnable {

		private boolean stop = false;

		public void setStop(boolean stop) {
			this.stop = stop;
		}

		@Override
		public void run() {
			LogHelper.info("producer begin!");
			try {
				produceData();
				LogHelper.info("producer quit!");
			} catch (Throwable t) {
				LogHelper.error("producer quit with ERROR!" + AutoService.this.toString(), t);
			}
		}

		private void produceData() {
			IAccessor accessor = (IAccessor) ReflectHelper.getClassInstance(accessorName);

			List<Object> workItems = null;
			while (!stop) {
				try {
					workItems = accessor.access(lockID, accessorParams);
				} catch (Throwable t) {
					LogHelper.error("error in access!" + AutoService.this.toString(), t);
				}

				if (workItems != null) {
					for (Object o : workItems) {
						try {
							// 超时，放不进去，直接解锁，退出
							if (!queues.offer(o, accessorIdleSeconds, TimeUnit.SECONDS)) {
								LogHelper.debug("queues.offer time out:" + o.toString());
								attemptToRelease(o);
							}
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							LogHelper.error("error when put to queue:" + o.toString(), e);
							attemptToRelease(o);
						}
					}
				} else {
					tryToSleep(accessorIdleSeconds);
				}
			}
		}
	}

	class Consumer implements Runnable {
		private boolean stop = false;

		@Override
		public void run() {
			LogHelper.info("consumer begin!");
			try {
				consumeData();
				LogHelper.info("consumer quit!");
			} catch (Throwable t) {
				LogHelper.error("consumer quit with ERROR!" + AutoService.this.toString(), t);
			}

		}

		private void consumeData() {
			IProcessor process = (IProcessor) ReflectHelper.getClassInstance(processorName);

			Object workItem = null;
			while (!stop) {
				try {
					workItem = queues.poll(processorIdleSeconds, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					// 这里出错不处理即可，没取到，不干活
					LogHelper.debug("error when poll", e);
				}
				if (workItem != null) {
					try {
						process.process(workItem, processorParams);
					} catch (Throwable t) {
						LogHelper.error("error when process:" + workItem.toString(), t);
						// 出错了，尝试解锁
						attemptToRelease(workItem);
					}
				}
			}

		}

		public void setStop(boolean stop) {
			this.stop = stop;
		}

	}

	class Releaser {
		private IReleaser myReleaser = null;

		public void release(Object workItem, String params) {
			if (myReleaser != null) {
				myReleaser.release(workItem, params);
			}
		}

		public void releaseAll(String lockID, String params) {
			if (myReleaser != null) {
				myReleaser.releaseAll(lockID, params);
			}
		}

		public Releaser(){
			// 需要初始化
			if (!Strings.isNullOrEmpty(releaserName) && myReleaser == null) {
				myReleaser = (IReleaser) ReflectHelper.getClassInstance(releaserName);
			}
		}
	}
}
