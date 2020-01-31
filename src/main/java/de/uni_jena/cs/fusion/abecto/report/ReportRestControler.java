package de.uni_jena.cs.fusion.abecto.report;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.uni_jena.cs.fusion.abecto.processing.Processing;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRepository;
import de.uni_jena.cs.fusion.abecto.processing.ProcessingRunner;
import de.uni_jena.cs.fusion.abecto.processor.Processor;

@RestController
public class ReportRestControler {
	@Autowired
	ProcessingRepository processingRepository;
	@Autowired
	ProcessingRunner processingRunner;

	/**
	 * Reports based on all data known after the given {@link Processing}.
	 * 
	 * @throws Exception
	 */
	@GetMapping("/processing/{uuid}/report/{report}")
	public Object getResult(@PathVariable("uuid") UUID processingId, @PathVariable("report") String reportClassName)
			throws Exception {
		Processing processing = processingRepository.findById(processingId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Processing not found."));
		Processor<?> processor = processingRunner.getProcessor(processing);
		Report report = this.getReportClass(reportClassName).getDeclaredConstructor().newInstance();
		return report.of(processor);
	}

	@SuppressWarnings("unchecked")
	private Class<Report> getReportClass(String reportClassName) throws ResponseStatusException {
		try {
			if (!reportClassName.contains(".")) {
				reportClassName = "de.uni_jena.cs.fusion.abecto.report.implementation." + reportClassName;
			}
			return (Class<Report>) Class.forName(reportClassName);
		} catch (ClassNotFoundException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Report class unknown.");
		}
	}

}
