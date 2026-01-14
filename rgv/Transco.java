package fr.lbp.lib.model.transco;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Transco {

	public Transco() {

	}

	public Transco(String category) {
		this.category = category;
	}

	private String project;

	private String category;

	private String srcApp;

	private String srcVl;

	private List<Output> output = new ArrayList<>();

}
