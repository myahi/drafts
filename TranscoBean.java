package fr.lbp.lib.service;

import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import fr.lbp.lib.model.transco.Transcos;

@SuppressWarnings("unchecked")
@Component
public class TranscoBean {

	
	TranscoService transcoService;
	
	public TranscoBean(TranscoService transcoService) {
		this.transcoService = transcoService;
	}
	
	@org.apache.camel.Handler
	public void loadTransco(Exchange exchange){
		List<String> transcoCategories = exchange.getIn().getHeader("transcoCategories",List.class);
		Map<String, Transcos> transcosByCategory = transcoService.loadByCategories(transcoCategories);
		exchange.setProperty("transcoByCategory", transcosByCategory);
	}
}
