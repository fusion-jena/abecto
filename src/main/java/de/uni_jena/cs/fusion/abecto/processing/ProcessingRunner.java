package de.uni_jena.cs.fusion.abecto.processing;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import de.uni_jena.cs.fusion.abecto.model.ModelRepository;
import de.uni_jena.cs.fusion.abecto.processor.Processor;
import de.uni_jena.cs.fusion.abecto.processor.Processor.Status;
import de.uni_jena.cs.fusion.abecto.processor.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.UploadSourceProcessor;

@Component
public class ProcessingRunner {
	private final static Logger log = LoggerFactory.getLogger(ProcessingRunner.class);

	private final Map<Processing, Processor<?>> processors = Collections
			.synchronizedMap(new WeakHashMap<Processing, Processor<?>>());

	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ModelRepository modelRepository;

	/**
	 * Awaits the termination of the given {@link Processing}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to await.
	 * @throws ReflectiveOperationException if instantiation of the
	 *                                      {@link Processor} belonging to the
	 *                                      {@link Processing} fails.
	 * @throws NoSuchElementException       if the {@link Processing} was not found.
	 * @throws InterruptedException         if the current thread was interrupted
	 *                                      while waiting for termination.
	 */
	@Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
	public void await(UUID processingId)
			throws ReflectiveOperationException, NoSuchElementException, InterruptedException {
		this.getProcessor(processingRepository.findById(processingId).orElseThrow()).await();
	}

	/**
	 * Asynchronously execute a {@link Processing} without {@link InputStream}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute.
	 * @throws NoSuchElementException if the {@link Processing} was not found.
	 * @throws IllegalStateException  if the {@link Processing} was already started.
	 */
	@Async
	@Transactional
	public Future<Processing> asyncExecute(UUID processingId) throws NoSuchElementException, IllegalStateException {
		Processing processing = processingRepository.findById(processingId).orElseThrow();
		return new AsyncResult<Processing>(syncExecute(processing));
	}

	/**
	 * Synchronously execute a {@link Processing} with {@link InputStream}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute.
	 * @throws IllegalStateException if the {@link Processing} was already started.
	 * @throws IOException           if the {@link Processor} failed to read the
	 *                               {@link InputStream}.
	 */
	public Processing syncExecute(Processing processing, InputStream inputStream)
			throws IllegalStateException, IllegalArgumentException, IOException {
		ensureNotStartetd(processing);
		try {
			Processor<?> processor = getProcessor(processing);
			if (processor instanceof UploadSourceProcessor<?>) {
				((UploadSourceProcessor<?>) processor).setUploadStream(inputStream);
			} else {
				throw new IllegalArgumentException(String.format(
						"Failed to execute Processor %s: Unexpected input streams.", processor.getClass().getName()));
			}
			return syncExecute(processing, processor);
		} catch (ReflectiveOperationException e) {
			return processingRepository.save(processing.setStateFail(e));
		}
	}

	/**
	 * Synchronously execute a {@link Processing} without {@link InputStream}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute.
	 * @throws IllegalStateException if the {@link Processing} was already started.
	 */
	public Processing syncExecute(Processing processing) throws IllegalStateException {
		ensureNotStartetd(processing);
		try {
			Processor<?> processor = getProcessor(processing);

			if (processor instanceof UploadSourceProcessor<?>) {
				throw new IllegalArgumentException(String.format(
						"Failed to execute Processor %s: Missing input streams.", processor.getClass().getName()));
			}

			return syncExecute(processing, processor);
		} catch (ReflectiveOperationException | IllegalArgumentException e) {
			return processingRepository.save(processing.setStateFail(e));
		}
	}

	/**
	 * Synchronously execute a prepared {@link Processing}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute
	 * @param processor    {@link Processor} instance to use
	 * @throws IllegalStateException if the {@link Processing} was already started.
	 */
	private Processing syncExecute(Processing processing, Processor<?> processor) throws IllegalStateException {
		ensureNotStartetd(processing);
		try {
			log.debug(String.format("Processor for %s started.", processing));
			processing = processingRepository.save(processing.setStateStart());
			Model model = processor.call();
			String modelHash = modelRepository.save(model);
			log.debug(String.format("Processor for %s succeded.", processing));
			return processingRepository.save(processing.setStateSuccess(modelHash));
		} catch (Exception e) {
			log.debug(String.format("Processor for %s failed.", processing));
			try {
				processor.fail(e);
			} catch (ExecutionException e1) {
				// do nothing
			}
			return processingRepository.save(processing.setStateFail(e));
		}
	}

	/**
	 * Check if a {@link Processing} was not already started.
	 * 
	 * @param processing {@link Processing} to check
	 * @throws IllegalStateException if the {@link Processing} was already started.
	 */
	private void ensureNotStartetd(Processing processing) throws IllegalStateException {
		if (!processing.isNotStarted()) {
			throw new IllegalStateException(
					String.format("Processing was already started. Status: %s", processing.getStatus()));
		}
	}

	/**
	 * Returns a {@link Processor} with the same {@link Status} and result
	 * {@link Model} (if applicable) as the given {@link Processing}.
	 * 
	 * @return {@link Processor} for the given {@link Processing}
	 * @throws ReflectiveOperationException if the {@link Processor} instantiation
	 *                                      failed for a variety of reasons
	 */
	public synchronized Processor<?> getProcessor(Processing processing) throws ReflectiveOperationException {
		if (!this.processors.containsKey(processing)) {
			Processor<?> processor = processing.getProcessorClass().getDeclaredConstructor().newInstance();
			processor.setParameters(processing.getParameter().getParameters());
			processor.setStatus(processing.getStatus());
			if (processing.isSucceeded()) {
				processor.setResultModel(modelRepository.get(processing.getModelHash()));
			}
			if (processor instanceof SourceProcessor) {
				((SourceProcessor<?>) processor).setKnowledgBase(processing.getStep().getKnowledgeBase().getId());
			} else if (processor instanceof RefinementProcessor) {
				for (Processing inputProcessing : processing.getInputProcessings()) {
					// recursive retrieval of input models
					Processor<?> inputProcessor = this.getProcessor(inputProcessing);
					((RefinementProcessor<?>) processor).addInputProcessor(inputProcessor);
				}
			}
			this.processors.put(processing, processor);
		}
		return this.processors.get(processing);
	}

}
