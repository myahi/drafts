package fr.lbp.lib.model.transco;

import lombok.Data;

@Data
public class Output {

	private String name;

	private String value;

	public Output() {
	}

	public Output(String name, String value) {
		this.name = name;
		this.value = value;
	}

}
