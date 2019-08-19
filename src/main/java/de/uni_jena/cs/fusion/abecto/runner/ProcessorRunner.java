package de.uni_jena.cs.fusion.abecto.runner;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Future;

import javax.transaction.Transactional;

import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import de.uni_jena.cs.fusion.abecto.model.ModelRepository;
import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor;
import de.uni_jena.cs.fusion.abecto.processor.api.Processor.Status;
import de.uni_jena.cs.fusion.abecto.processor.api.RefinementProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.SourceProcessor;
import de.uni_jena.cs.fusion.abecto.processor.api.UploadSourceProcessor;

@Component
public class ProcessorRunner {
	private final static Logger log = LoggerFactory.getLogger(ProcessorRunner.class);

	private final Map<Processing, Processor<?>> processors = Collections
			.synchronizedMap(new WeakHashMap<Processing, Processor<?>>());

	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ModelRepository modelRepository;

	/**
	 * Asynchronously execute a {@link Processing} without {@link InputStream}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute.
	 * @throws NoSuchElementException   if the {@link Processing} was not found.
	 * @throws IllegalStateException    if the {@link Processing} was already
	 *                                  started.
	 * @throws IllegalArgumentException if the {@link Processor} requires an input
	 *                                  stream.
	 */
	@Async
	@Transactional
	public Future<Processing> asyncExecute(UUID processingId)
			throws NoSuchElementException, IllegalStateException {
		Processing processing = processingRepository.findById(processingId).orElseThrow();
		return new AsyncResult<Processing>(syncExecute(processing));
	}

	/**
	 * Synchronously execute a {@link Processing} with {@link InputStream}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute.
	 * @throws IllegalStateException    if the {@link Processing} was already
	 *                                  started.
	 * @throws IllegalArgumentException if the {@link Processor} does not accept an
	 *                                  input stream.
	 * @throws IOException              if the {@link Processor} failed to read the
	 *                                  {@link InputStream}.
	 */
	public void syncExecute(Processing processing, InputStream inputStream)
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
			syncExecute(processing, processor);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			processingRepository.save(processing.setStateFail(e));
		}
	}

	/**
	 * Synchronously execute a {@link Processing} without {@link InputStream}.
	 * 
	 * @param processingId {@link UUID} of the {@link Processing} to execute.
	 * @throws IllegalStateException    if the {@link Processing} was already
	 *                                  started.
	 * @throws IllegalArgumentException if the {@link Processor} requires an input
	 *                                  stream.
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
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
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
			log.info("Running processor " + processor);
			processingRepository.save(processing.setStateStart());
			Model model = processor.call();
			String modelHash = modelRepository.save(model);
			log.info("Processor " + processor + " succeded");
			return processingRepository.save(processing.setStateSuccess(modelHash));
		} catch (Exception e) {
			log.error("Processor " + processor + " failed", e);
			processor.fail();
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
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	private Processor<?> getProcessor(Processing processing) throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
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
				if (!this.processors.containsKey(inputProcessing)) {
					this.processors.put(inputProcessing, getProcessor(inputProcessing));
				}
				Processor<?> inputProcessor = this.processors.get(inputProcessing);
				((RefinementProcessor<?>) processor).addInputProcessor(inputProcessor);
			}
		}
		return processor;
	}

}
